package boofcv.processing;

import boofcv.abst.fiducial.MicroQrCodeDetector;
import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.microqr.MicroQrCode;
import boofcv.alg.fiducial.microqr.MicroQrCodeEncoder;
import boofcv.alg.fiducial.microqr.MicroQrCodeGenerator;
import boofcv.factory.fiducial.ConfigMicroQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import processing.core.PImage;

import java.util.List;

/**
 * Detecting and rendering Micro QR Codes
 *
 * @author Peter Abeles
 */
public class SimpleMicroQrCode {
    GrayU8 gray = new GrayU8(1, 1);
    MicroQrCodeDetector<GrayU8> detector;

    /**
     * Renders a Micro QR Code
     *
     * @param message         The message
     * @param pixelsPerModule Number of pixels wide each square is. Try 20
     * @return Rendered QR Code
     */
    public static PImage generate(String message, int pixelsPerModule) {

        MicroQrCode qr = new MicroQrCodeEncoder().addAutomatic(message).fixate();

        var render = new FiducialImageEngine();
        var g = new MicroQrCodeGenerator();
        g.setRender(render);

        int pixels = pixelsPerModule * qr.getNumberOfModules();
        render.configure(pixelsPerModule, pixels);
        g.markerWidth = pixels;
        g.render(qr);

        GrayU8 rendered = render.getGray();
        PImage output = new PImage(rendered.width, rendered.height);

        ConvertProcessing.convert_U8_RGB(rendered, output);

        return output;
    }

    public SimpleMicroQrCode(ConfigMicroQrCode config) {
        detector = FactoryFiducial.microqr(config, GrayU8.class);
    }

    public SimpleMicroQrCode() {
        this(null);
    }

    public List<MicroQrCode> detect(PImage input) {
        gray.reshape(input.width, input.height);
        ConvertProcessing.convertFromRGB(input, gray);
        detector.process(gray);
        return detector.getDetections();
    }
}
