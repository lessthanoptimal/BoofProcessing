package boofcv.processing;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGmm;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import processing.core.PImage;

/**
 * Detections motion in the input video stream. Output is a binary image where pixels marked
 * 0 are not moving and 1 are moving.
 *
 * @author Peter Abeles
 */
public class SimpleMotionDetection {

    GrayU8 gray = new GrayU8(1,1);
    SimpleBinary segmented = new SimpleBinary(new GrayU8(1,1));
    BackgroundModelStationary<GrayU8> background;

    public SimpleMotionDetection(ConfigBackgroundGmm config ) {
        background = FactoryBackgroundModel.stationaryGmm(config,
                ImageType.single(GrayU8.class));
    }

    public SimpleMotionDetection(ConfigBackgroundBasic config ) {
        background = FactoryBackgroundModel.stationaryBasic(config,
                ImageType.single(GrayU8.class));
    }

    public SimpleBinary segment(PImage input ) {
        gray.reshape(input.width,input.height);
        segmented.image.reshape(input.width,input.height);

        ConvertProcessing.convertFromRGB(input,gray);

        background.updateBackground(gray);

        background.segment(gray,segmented.image);

        return segmented;
    }
}
