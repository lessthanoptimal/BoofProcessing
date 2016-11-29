package boofcv.processing;

import boofcv.abst.scene.ImageClassifier;
import boofcv.factory.scene.ClassifierAndSource;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import deepboof.io.DeepBoofDataBaseOps;
import processing.core.PImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simplified interface for image classification.
 *
 * @author Peter Abeles
 */
public class SimpleImageClassification {

    ImageClassifier<Planar<GrayF32>> classifier;
    List<String> sources;
    File path;

    Planar<GrayF32> boofImage = new Planar<>(GrayF32.class,1,1,3);

    boolean modelLoaded = false;

    public SimpleImageClassification(ClassifierAndSource cs) {
        this.classifier = cs.getClassifier();
        sources = cs.getSource();
    }

    /**
     * Loads the file at the specified location.  if download flag is true it will download the model to that location
     * and load it.
     * @param location Location of model or location to download model too
     * @param download if true it will attempt to download the model if it's not there already
     */
    public void loadModel( String location , boolean download ) {
        if( location == null ) {
            location = "download_data";
        }
        try {
            if( download ) {
                location = DeepBoofDataBaseOps.downloadModel(sources,new File(location)).getPath();
            }
            classifier.loadModel(new File(location));
            modelLoaded = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Classifies the image and returns the best class
     * @param image Input image to be classified
     * @return best result
     */
    public String classify(PImage image ) {
        if( !modelLoaded )
            throw new RuntimeException("Need to download and load the model");
        boofImage.reshape(image.width,image.height);
        ConvertProcessing.convertFromRGB(image,boofImage);

        classifier.classify(boofImage);

        List<String> categories = classifier.getCategories();
        return categories.get(classifier.getBestResult());
    }

    /**
     * List of all the classes and their scores
     */
    public List<Score> getAllScores() {
        List<String> categories = classifier.getCategories();
        List<Score> scores = new ArrayList<>();
        for ( ImageClassifier.Score s : classifier.getAllResults() ) {
            Score a = new Score();
            a.score = s.score;
            a.category = categories.get(s.category);
            scores.add(a);
        }
        return scores;
    }

    public List<String> getCategories() {
        return classifier.getCategories();
    }

    public static class Score {
        public String category;
        public double score;
    }
}
