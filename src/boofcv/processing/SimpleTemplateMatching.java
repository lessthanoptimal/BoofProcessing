package boofcv.processing;

import boofcv.alg.feature.detect.template.TemplateMatching;
import boofcv.struct.feature.Match;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified interface for performing template matching on an image
 *
 * @author Peter Abeles
 */
public class SimpleTemplateMatching {

    TemplateMatching<GrayU8> matcher;

    GrayU8 ginput = new GrayU8(1,1);
    GrayU8 gtemplate = new GrayU8(1,1);

    public SimpleTemplateMatching(TemplateMatching<GrayU8> matcher) {
        this.matcher = matcher;
    }

    public void setInput( PImage input ) {
        ConvertProcessing.convertFromRGB(input,ginput);
        matcher.setImage(ginput);
    }

    public List<Match> detect( PImage template , int maxMatches ) {
        ConvertProcessing.convertFromRGB(template,gtemplate);

//        System.out.println("width="+ginput.width+"x"+ginput.height+"  width="+gtemplate.width+"x"+gtemplate.height);
//
//        System.out.println("sum="+ ImageStatistics.sum(ginput));
//        System.out.println("sum="+ ImageStatistics.sum(gtemplate));
        matcher.setTemplate(gtemplate,null, maxMatches);

        return extractResults();
    }

    public List<Match> detect(PImage template , SimpleBinary mask , int maxMatches ) {
        ConvertProcessing.convert_RGB_U8(template,gtemplate);
        matcher.setTemplate(gtemplate,mask.image, maxMatches);

//        System.out.println("width="+ginput.width+"x"+ginput.height+"  width="+gtemplate.width+"x"+gtemplate.height+
//                "  width="+mask.image.width+"x"+mask.image.height);
//
//        System.out.println("sum="+ ImageStatistics.sum(ginput));
//        System.out.println("sum="+ ImageStatistics.sum(gtemplate));
//        System.out.println("sum="+ ImageStatistics.sum(mask.image));
        return extractResults();
    }

    private List<Match> extractResults() {
        matcher.process();
        List<Match> matches = matcher.getResults().toList();

//        System.out.println("total found "+matches.size());

        List<Match> output = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            Match orig = matches.get(i);
            Match copy = new Match();
            copy.set(orig);
            copy.score = orig.score;
            output.add(copy);
        }

        return output;
    }
}
