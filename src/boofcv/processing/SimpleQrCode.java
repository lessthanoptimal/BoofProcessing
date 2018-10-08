package boofcv.processing;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import processing.core.PImage;

import java.util.List;

/**
 * Detecting and rendering QR Codes
 *
 * @author Peter Abeles
 */
public class SimpleQrCode {
    GrayU8 gray = new GrayU8(1,1);
    QrCodeDetector<GrayU8> detector;

    /**
     * Renders a QR Code
     *
     * @param message The message
     * @param pixelsPerModule Number of pixels wide each square is. Try 20
     * @return Rendered QR Code
     */
    public static PImage generate( String message , int pixelsPerModule ) {

        QrCode qr = new QrCodeEncoder().
                setError(QrCode.ErrorLevel.M).
                addAutomatic(message).fixate();

        QrCodeGeneratorImage render = new QrCodeGeneratorImage(pixelsPerModule);

        render.render(qr);

        GrayU8 rendered = render.getGray();
        PImage output = new PImage(rendered.width,rendered.height);

        ConvertProcessing.convert_U8_RGB(rendered,output);

        return output;
    }

    public SimpleQrCode( ConfigQrCode config ) {
        detector = FactoryFiducial.qrcode(config,GrayU8.class);
    }

    public SimpleQrCode() {
        this(null);
    }

    public List<QrCode>  detect( PImage input ) {
        gray.reshape(input.width,input.height);
        ConvertProcessing.convertFromRGB(input,gray);
        detector.process(gray);
        return detector.getDetections();
    }
}
