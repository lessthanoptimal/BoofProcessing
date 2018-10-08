package boofcv.processing;

import boofcv.alg.feature.detect.template.TemplateMatching;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import processing.core.PImage;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class SimpleTemplateMatching {

    TemplateMatching<GrayU8> matcher;

    public SimpleTemplateMatching(TemplateMatching<GrayU8> matcher) {
        this.matcher = matcher;
    }

    public List<Point2D_I32> detect(PImage input , PImage template ) {
        return null;
    }

    public List<Point2D_I32> detect(PImage input , PImage template , SimpleBinary mask ) {
        return null;
    }
}
