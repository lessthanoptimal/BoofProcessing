// Launches a webcam and searches for QR Codes, prints their message and draws their outline

import processing.video.*;
import boofcv.processing.*;
import java.util.*;
import boofcv.alg.fiducial.qrcode.QrCode;

Capture cam;
SimpleQrCode detector;

void setup() {
  // Open up the camera so that it has a video feed to process
  initializeCamera(640, 480);
  surface.setSize(cam.width, cam.height);


  detector = Boof.detectQR();
}

void draw() {
  if (cam.available() == true) {
    cam.read();

    List<QrCode> found = detector.detect(cam);

    image(cam, 0, 0);

    // Configure the line's appearance
    noFill();
    strokeWeight(5);
    stroke(255, 0, 0);

    for( QrCode qr : found ) {
      println("message             "+qr.message);

        // Draw a line around each detected QR Code
        for ( List<Polygon2D_F64> outline : qr ) {
          beginShape();
          for ( int i = 0; i < outline.size(); i++ ) {
            Point2D_F64 p = outline.get(i);
            vertex( p.x, p.y );
          }
          // close the loop
          Point2D_F64 p = poly.get(0);
          vertex( p.x, p.y );
          endShape();
        }
    }
  }
}

void initializeCamera( int desiredWidth, int desiredHeight ) {
  String[] cameras = Capture.list();

  if (cameras.length == 0) {
    println("There are no cameras available for capture.");
    exit();
  } else {
    cam = new Capture(this, desiredWidth, desiredHeight);
    cam.start();
  }
}