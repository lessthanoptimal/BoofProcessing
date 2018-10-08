// Renders a QR Code and displays it in a window

import boofcv.processing.*;


void draw() {
  // Give it the string you wish to encode in the QR Code and how large each square should be
  // in pxiels
  PImage output = Boof.renderQR("BoofCV on Processing is FUN!!!",10);

  // Let's see what it looks like
  surface.setSize(output.width, output.height);
  image(output,0,0);
}