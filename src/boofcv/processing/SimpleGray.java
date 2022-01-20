/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.processing;

import boofcv.abst.distort.FDistort;
import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.enhance.GEnhanceImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.GConvertImage;
import boofcv.factory.feature.detect.line.*;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.ConfigLength;
import boofcv.struct.border.BorderType;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * High level interface for handling gray scale images
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class SimpleGray<Gray extends ImageGray<Gray>> extends SimpleImage<Gray>{

	public SimpleGray(Gray image) {
		super(image);
	}

	public SimpleGray blurMean( int radius ) {
		return new SimpleGray((Gray)GBlurImageOps.mean(image, null, radius, null, null));
	}

	public SimpleGray blurMedian( int radius ) {
		return new SimpleGray((Gray)GBlurImageOps.median(image, null, radius, null));
	}

	/**
	 * Equalizes the histogram across the entire image
	 *
	 * @see EnhanceImageOps
	 * @return New SimpleGray after equalize histogram has been applied
	 */
	public SimpleGray histogramEqualize() {
		if (!(image instanceof GrayU8))
			throw new RuntimeException("Image must be of type GrayU8 to adjust its histogram");

		GrayU8 adjusted = new GrayU8(image.width, image.height);

		int histogram[] = new int[256];
		int transform[] = new int[256];

		ImageStatistics.histogram((GrayU8) image, 0,histogram);
		EnhanceImageOps.equalize(histogram, transform);
		EnhanceImageOps.applyTransform((GrayU8) image, transform, adjusted);

		return new SimpleGray(adjusted);
	}

	/**
	 * Equalizes the local image histogram
	 * @see EnhanceImageOps
	 * @param radius Radius of the region used to localize
	 * @return New SimpleGray after equalize histogram has been applied
	 */
	public SimpleGray histogramEqualizeLocal( int radius ) {
		if (!(image instanceof GrayU8))
			throw new RuntimeException("Image must be of type GrayU8 to adjust its histogram");

		GrayU8 adjusted = new GrayU8(image.width, image.height);
		EnhanceImageOps.equalizeLocal((GrayU8) image, radius, adjusted, 256, null);

		return new SimpleGray(adjusted);
	}

	/**
	 * Applies a sharpen with a connect-4 rule
	 *
	 * @see EnhanceImageOps
	 *
	 * @return New SimpleGray
	 */
	public SimpleGray enhanceSharpen4() {
		if (!(image instanceof GrayU8))
			throw new RuntimeException("Image must be of type GrayU8 to adjust its histogram");

		Gray adjusted = image.createNew(image.width, image.height);
		GEnhanceImageOps.sharpen4(image, adjusted);

		return new SimpleGray(adjusted);
	}

	/**
	 * Applies a sharpen with a connect-8 rule
	 *
	 * @see EnhanceImageOps
	 *
	 * @return New SimpleGray
	 */
	public SimpleGray enhanceSharpen8() {
		if (!(image instanceof GrayU8))
			throw new RuntimeException("Image must be of type GrayU8 to adjust its histogram");

		Gray adjusted = image.createNew(image.width, image.height);
		GEnhanceImageOps.sharpen8(image, adjusted);

		return new SimpleGray(adjusted);
	}

	public List<LineParametric2D_F32> linesHoughPolar(ConfigHoughGradient configHough, ConfigParamPolar configPolar) {
		Class inputType = image.getClass();
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		DetectLine detector = FactoryDetectLine.houghLinePolar(configHough,configPolar,derivType);
		return detector.detect(image);
	}

	public List<LineParametric2D_F32> linesHoughFoot(ConfigHoughGradient configHough, ConfigParamFoot configFoot) {
		Class inputType = image.getClass();
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		DetectLine detector = FactoryDetectLine.houghLineFoot(configHough, configFoot, derivType);
		return detector.detect(image);
	}

	public List<LineParametric2D_F32> linesHoughFootSub(ConfigHoughFootSubimage configFoot ) {
		Class inputType = image.getClass();
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		DetectLine detector = FactoryDetectLine.houghLineFootSub(configFoot,derivType);
		return detector.detect(image);
	}

	/**
	 * Removes perspective distortion.  4 points must be in 'this' image must be in clockwise order.
	 *
	 * @param outWidth Width of output image
	 * @param outHeight Height of output image
	 * @return Image with perspective distortion removed
	 */
	public SimpleGray removePerspective( int outWidth , int outHeight,
										 double x0, double y0,
										 double x1, double y1,
										 double x2, double y2,
										 double x3, double y3 )
	{
		Gray output = image.createNew(outWidth,outHeight);

		// Homography estimation algorithm.  Requires a minimum of 4 points
		Estimate1ofEpipolar computeHomography = FactoryMultiView.homographyDLT(true);

		// Specify the pixel coordinates from destination to target
		ArrayList<AssociatedPair> associatedPairs = new ArrayList<AssociatedPair>();
		associatedPairs.add(new AssociatedPair(new Point2D_F64(0,0),new Point2D_F64(x0,y0)));
		associatedPairs.add(new AssociatedPair(new Point2D_F64(outWidth-1,0),new Point2D_F64(x1,y1)));
		associatedPairs.add(new AssociatedPair(new Point2D_F64(outWidth-1,outHeight-1),new Point2D_F64(x2,y2)));
		associatedPairs.add(new AssociatedPair(new Point2D_F64(0,outHeight-1),new Point2D_F64(x3,y3)));

		// Compute the homography
		DMatrixRMaj H = new DMatrixRMaj(3,3);
		computeHomography.process(associatedPairs, H);
		FMatrixRMaj H32 = new FMatrixRMaj(3,3);
		ConvertMatrixData.convert(H,H32);

		// Create the transform for distorting the image
		PointTransformHomography_F32 homography = new PointTransformHomography_F32(H32);

		// Apply distortion and show the results
		new FDistort(image,output).transform(new PointToPixelTransform_F32(homography)).border(BorderType.SKIP).apply();

		return new SimpleGray(output);
	}

	/**
	 * @see GBlurImageOps#gaussian
	 */
	public SimpleGray blurGaussian( double sigma, int radius ) {
		return new SimpleGray((Gray)GBlurImageOps.gaussian(image, null, sigma, radius, null));
	}

	/**
	 * @see GThresholdImageOps#threshold
	 */
	public SimpleBinary threshold(double threshold, boolean down ) {
		return new SimpleBinary(GThresholdImageOps.threshold(image, null, threshold, down));
	}

	/**
	 * @see GThresholdImageOps#computeOtsu
	 */
	public SimpleBinary thresholdOtsu(boolean down ) {
		double threshold = GThresholdImageOps.computeOtsu(image,0,255);
		return new SimpleBinary(GThresholdImageOps.threshold(image, null, threshold, down));
	}

	/**
	 * @see GThresholdImageOps#computeEntropy
	 */
	public SimpleBinary thresholdEntropy(boolean down ) {
		double threshold = GThresholdImageOps.computeEntropy(image,0,255);
		return new SimpleBinary(GThresholdImageOps.threshold(image, null, threshold, down));
	}

	/**
	 * @see GThresholdImageOps#localMean
	 */
	public SimpleBinary thresholdMean( int width, double bias, boolean down ) {
		ConfigLength config = new ConfigLength();
		config.length = width;
		return new SimpleBinary(GThresholdImageOps.localMean(image, null, config, bias, down, null,null, null));
	}

	/**
	 * @see GThresholdImageOps#localGaussian
	 */
	public SimpleBinary thresholdGaussian( int Width, double bias, boolean down ) {
		ConfigLength config = new ConfigLength();
		config.length = Width;
		return new SimpleBinary(GThresholdImageOps.localGaussian(image, null, config, bias, down, null, null));
	}

	/**
	 * @see GThresholdImageOps#localSauvola
	 *
	 * @param width Width of the adaptive region
	 * @param k Positive parameter used to tune threshold.  Try 0.3
	 */
	public SimpleBinary thresholdSauvola( int width, double k , boolean down ) {
		ConfigLength config = new ConfigLength();
		config.length = width;
		return new SimpleBinary(GThresholdImageOps.localSauvola(image, null, config, (float) k, down));
	}

	/**
	 * @see GThresholdImageOps#localNick
	 *
	 * @param width Width of the adaptive region
	 * @param k Positive parameter used to tune threshold.  Try -0.1 to -0.2
	 */
	public SimpleBinary thresholdNick( int width, double k , boolean down ) {
		ConfigLength config = new ConfigLength();
		config.length = width;
		return new SimpleBinary(GThresholdImageOps.localNick(image, null, config, (float) k, down));
	}

	/**
	 *
	 * @see GThresholdImageOps#blockMean
	 *
	 * @param width Width of square region.
	 * @param scale Scale factor adjust for threshold.  1.0 means no change.
	 * @param down Should it threshold up or down.
	 */
	public SimpleBinary thresholdBlockMean( int width, double scale , boolean down ) {
		ConfigLength config = new ConfigLength();
		config.length = width;
		return new SimpleBinary(GThresholdImageOps.blockMean(image, null, config, scale, down));
	}

	/**
	 *
	 * @see GThresholdImageOps#blockMinMax
	 *
	 * @param width Width of square region.
	 * @param scale Scale factor adjust for threshold.  1.0 means no change.
	 * @param down Should it threshold up or down.
	 * @param textureThreshold If the min and max values are within this threshold the pixel will be set to 1.
	 */
	public SimpleBinary thresholdBlockMinMax( int width, double scale , boolean down , double textureThreshold ) {
		ConfigLength config = new ConfigLength();
		config.length = width;
		return new SimpleBinary(GThresholdImageOps.blockMinMax(image, null, config, scale, down, textureThreshold));
	}

	/**
	 *
	 * @see GThresholdImageOps#blockOtsu
	 *
	 * @param width Width of square region.
	 * @param scale Scale factor adjust for threshold.  1.0 means no change.
	 * @param down Should it threshold up or down.
	 */
	public SimpleBinary thresholdBlockOtsu( int width, double scale , boolean down ) {
		ConfigLength config = new ConfigLength();
		config.length = width;
		return new SimpleBinary(GThresholdImageOps.blockOtsu(image, null, false,config,0, scale, down));
	}

	public SimpleGradient gradientSobel() {
		return gradient(FactoryDerivative.sobel(image.getClass(), null));
	}

	public SimpleGradient gradientPrewitt() {
		return gradient(FactoryDerivative.prewitt(image.getClass(), null));
	}

	public SimpleGradient gradientThree() {
		return gradient(FactoryDerivative.three(image.getClass(), null));
	}

	public SimpleGradient gradientScharr() {
		return gradient(FactoryDerivative.scharr(image.getClass(), null));
	}

	public SimpleGradient gradientTwo0() {
		return gradient(FactoryDerivative.two0(image.getClass(), null));
	}

	public SimpleGradient gradientTwo1() {
		return gradient(FactoryDerivative.two1(image.getClass(), null));
	}

	/**
	 * @see GImageStatistics#mean
	 */
	public double mean() {
		return GImageStatistics.mean(image);
	}

	/**
	 * @see GImageStatistics#max
	 */
	public double max() {
		return GImageStatistics.max(image);
	}

	/**
	 * @see GImageStatistics#maxAbs
	 */
	public double maxAbs() {
		return GImageStatistics.maxAbs(image);
	}

	/**
	 * @see GImageStatistics#sum
	 */
	public double sum() {
		return GImageStatistics.sum(image);
	}

	private SimpleGradient gradient(ImageGradient gradient) {
		SimpleGradient ret = new SimpleGradient(gradient.getDerivativeType(),image.width,image.height);
		gradient.process(image,ret.dx,ret.dy);

		return ret;
	}

	public PImage visualizeSign() {
		if( image instanceof GrayF32) {
			float max = ImageStatistics.maxAbs((GrayF32) image);
			return VisualizeProcessing.colorizeSign((GrayF32)image,max);
		} else if( image instanceof GrayI) {
			int max = (int)GImageStatistics.maxAbs(image);
			return VisualizeProcessing.colorizeSign((GrayI) image, max);
		} else {
			throw new RuntimeException("Unknown image type");
		}
	}

	public PImage convert() {
		PImage out = new PImage(image.width,image.height, PConstants.RGB);
		if( image instanceof GrayF32) {
			ConvertProcessing.convert_F32_RGB((GrayF32)image,out);
		} else if( image instanceof GrayU8 ) {
			ConvertProcessing.convert_U8_RGB((GrayU8) image, out);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return out;
	}

	/**
	 * Converts the internal image type into {@link GrayF32}.
	 */
	public void convertToF32() {
		if( image instanceof GrayF32 )
			return;

		GrayF32 a = new GrayF32(image.width,image.height);
		GConvertImage.convert(image,a);
		image = (Gray)a;
	}

	/**
	 * Converts the internal image type into {@link GrayU8}.
	 */
	public void convertToU8() {
		if( image instanceof GrayU8 )
			return;

		GrayU8 a = new GrayU8(image.width,image.height);
		GConvertImage.convert(image,a);
		image = (Gray)a;
	}
}
