// Loads an image and classifies it as belonging into one of a finite number of classes
// If the image is similar to the training set it should do a good job.  Where good job
// is defined as correct 60% to 95% of the time.  If not similar in any way to the training
// images then the resumts are likely to be humorous

import boofcv.processing.*;
import java.util.*;

PImage input;
SimpleImageClassification classifier;

void setup() {

  input = loadImage("cat01.jpg");

  // Supported classifiers:  VGG, NIN
  classifier = Boof.imageClassification("VGG");

  // Download and load the model from the default location
  // It can take several minutes to download a model and depends on the speed of your internet connection
  classifier.loadModel(null,true);

  // Classify the image
  classifier.classify(input);

  surface.setSize(input.width, input.height);
}

void draw() {
  image(input, 0, 0);

  textFont(createFont("Arial", 20, true));
  textAlign(LEFT);
  fill(0xFF, 0, 0);

  List<SimpleImageClassification.Score> scores = classifier.getAllScores();
  int N = Math.min(5,scores.size());

  for( int i = 0; i < N; i++ ) {
    SimpleImageClassification.Score s = scores.get(i);
    text(String.format("%s %f",s.category,s.score), 30, (i+1)*30);
  }
}