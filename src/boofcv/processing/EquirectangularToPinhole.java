package boofcv.processing;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.spherical.CameraToEquirectangular_F32;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;
import processing.core.PImage;

/**
 * Simplified interface for converting an equirectangular image into a pinhole camera view
 *
 * @author Peter Abeles
 */
public class EquirectangularToPinhole {

    Planar<GrayU8> equiImage = new Planar<>(GrayU8.class,1,1,3);
    Planar<GrayU8> pinholeImage = new Planar<>(GrayU8.class,1,1,3);

    ImageDistort<Planar<GrayU8>,Planar<GrayU8>> distorter;
    CameraPinhole intrinsic;

    CameraToEquirectangular_F32 equiToPinhole = new CameraToEquirectangular_F32();

    boolean pinholeChanged = false;

    public EquirectangularToPinhole() {
        ImageType<Planar<GrayU8>> imageType = ImageType.pl(3,GrayU8.class);

        InterpolatePixel<Planar<GrayU8>> interp = FactoryInterpolation.
                createPixel(0, 255, InterpolationType.BILINEAR, BorderType.EXTENDED, imageType);
       distorter = FactoryDistort.distort(false,interp,imageType);
    }

    /**
     * Configures pinhole camera with no distortion
     *
     * @param width image width
     * @param height image height
     * @param hfov horizontal FOV in degrees
     */
    public void setIntrinsic( int width , int height , double hfov ) {
        setIntrinsic(PerspectiveOps.createIntrinsic(width,height,hfov, null));
    }

    public void setIntrinsic( CameraPinhole intrinsic ) {
        this.intrinsic = intrinsic;

        pinholeImage.reshape(intrinsic.width,intrinsic.height);
        pinholeChanged = true;
    }

    public void setOrientation(DMatrixRMaj R ) {
        FMatrixRMaj R32 = new FMatrixRMaj(3,3);
        ConvertMatrixData.convert(R,R32);
        equiToPinhole.getRotation().setTo(R32);
    }

    public void setEquirectangular(PImage image ) {
        if( equiImage.width != image.width || equiImage.height != image.height ) {
            equiImage.reshape(image.width, image.height);
            equiToPinhole.setEquirectangularShape(equiImage.width, equiImage.height);
        }

        ConvertProcessing.convertFromRGB(image, equiImage);
    }

    public SimpleColor render() {
        if( intrinsic == null )
            throw new IllegalArgumentException("Must call setIntrinsic() first");

        if( pinholeChanged ) {
            pinholeChanged = false;
            equiToPinhole.setCameraModel(intrinsic);
            distorter.setModel(equiToPinhole);
        }

        distorter.apply(equiImage,pinholeImage);
        return new SimpleColor(pinholeImage);
    }
}
