// Loads an image and searches for objects in the image using template matching

import boofcv.processing.*;
import boofcv.struct.image.*;
import georegression.struct.point.*;
import java.util.*;
import boofcv.factory.feature.detect.template.TemplateScoreType;

PImage input;
List<Point2D_I32> found;
int regionWidth;
int regionHeight;

void setup() {
  input = loadImage("desktop.png");
  PImage template = loadImage("cursor.png");
  PImage mask = loadImage("cursor_mask.png");

  // Convert the mask into a usable format
  SimpleBinary bmask = Boof.gray(mask,ImageDataType.U8).threshold(125,false);

  // Create the template matching class
  SimpleTemplateMatching matching = Boof.templateMatching(TemplateScoreType.SUM_SQUARE_ERROR);

  // Tell it which image to search inside of
  matching.setInput(input);

  // search for the template with a mask
  // The number indicates how many matches it should return. 3 cursors can be found in the image
  found = matching.detect(template,bmask,3);
  // you can also search for it without a mask
  //found = matching.detect(template,3);

  System.out.println("Total Matches Found: "+found.size());

  // save the region's size for visualizing
  regionWidth = template.width;
  regionHeight = template.height;

  surface.setSize(input.width, input.height);
}

void draw() {
  image(input, 0, 0);

  // Configure the line's appearance
  noFill();
  strokeWeight(5);
  stroke(255, 0, 0);

  // Draw the solutions
  int w = regionWidth;
  int h = regionHeight;
  for ( Point2D_I32 p : found ) {
    beginShape();
    vertex( p.x, p.y );
    vertex( p.x+w, p.y );
    vertex( p.x+w, p.y+h );
    vertex( p.x, p.y+h );
    vertex( p.x, p.y );
    endShape();
  }
}