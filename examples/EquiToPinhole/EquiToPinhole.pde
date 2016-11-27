// Loads an equirectangular image an renders an arbitrary pinhole camera
// This example will render a pinhole camera that's rotating around
import boofcv.processing.*;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.*;

PImage imgContour;
PImage imgBlobs;

int tick = 0;
EquirectangularToPinhole equiToPinhole;

void setup() {
  // load the equirectangular image which encodes a 360 view
  PImage equi = loadImage("equirectangular_half_dome_01.jpg");

  equiToPinhole = Boof.equirectangularToPinhole();

  // Set the image
  equiToPinhole.setEquirectangular(equi);

  // Render into a 340x300 camera with a 120 degree FOV
  equiToPinhole.setIntrinsic(340,300,120);

  surface.setSize(340, 300);
}

void draw() {
  // rotate the view around horizontally
  double angle = 2.0*Math.PI*(tick++%180)/180;

  // rotation which will align the camera to be visually pleasing
  DenseMatrix64F alignR = Boof.eulerXYZ(Math.PI/2.0,0,Math.PI/2.0);
  // Rotate around the Z axis
  DenseMatrix64F rotZ = Boof.eulerXYZ(0,0,angle);
  DenseMatrix64F R = new DenseMatrix64F(3,3);
  CommonOps.mult(rotZ,alignR,R);
  // Tell the projection to use this rotation matrix
  equiToPinhole.setOrientation(R);

  // Render the results
  SimpleColor pinhole = equiToPinhole.render();

  // Display it
  background(0);
  image(pinhole.convert(), 0, 0);
}