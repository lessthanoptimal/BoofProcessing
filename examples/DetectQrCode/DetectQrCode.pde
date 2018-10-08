// Launches a webcam and searches for square-binary fiducials.  Draws a cube over the feducials when it
// finds them and their ID number

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

    for( QrCode f : found ) {
      println("message             "+f.message);
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