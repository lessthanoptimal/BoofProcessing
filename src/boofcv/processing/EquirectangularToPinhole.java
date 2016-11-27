package boofcv.processing;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.spherical.EquirectangularToPinhole_F32;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import org.ejml.data.DenseMatrix64F;
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

    EquirectangularToPinhole_F32 equiToPinhole = new EquirectangularToPinhole_F32();

    boolean pinholeChanged = false;

    public EquirectangularToPinhole() {
        ImageType<Planar<GrayU8>> imageType = ImageType.pl(3,GrayU8.class);

        InterpolatePixel<Planar<GrayU8>> interp = FactoryInterpolation.
                createPixel(0, 255, TypeInterpolate.BILINEAR, BorderType.EXTENDED, imageType);
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
        setIntrinsic(PerspectiveOps.createIntrinsic(width,height,hfov));
    }

    public void setIntrinsic( CameraPinhole intrinsic ) {
        this.intrinsic = intrinsic;

        pinholeImage.reshape(intrinsic.width,intrinsic.height);
        pinholeChanged = true;
    }

    public void setOrientation(DenseMatrix64F R ) {
        equiToPinhole.getRotation().set(R);
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
            equiToPinhole.setPinhole(intrinsic);
            distorter.setModel(equiToPinhole);
        }

        distorter.apply(equiImage,pinholeImage);
        return new SimpleColor(pinholeImage);
    }
}
