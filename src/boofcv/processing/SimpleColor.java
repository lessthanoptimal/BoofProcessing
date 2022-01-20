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
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.ArrayList;

/**
 * Simplified interface for handling color images
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class SimpleColor<Gray extends ImageGray<Gray>> extends SimpleImage<Planar<Gray>>{

	public SimpleColor(Planar<Gray> image) {
		super(image);
	}

	public SimpleColor blurMean( int radius ) {
		return new SimpleColor((Planar<Gray>)GBlurImageOps.mean(image, null, radius,null, null));
	}

	public SimpleColor blurMedian( int radius ) {
		return new SimpleColor((Planar<Gray>)GBlurImageOps.median(image, null, radius,null));
	}

	/**
	 * Removes perspective distortion.  4 points must be in 'this' image must be in clockwise order.
	 *
	 * @param outWidth Width of output image
	 * @param outHeight Height of output image
	 * @return Image with perspective distortion removed
	 */
	public SimpleColor removePerspective( int outWidth , int outHeight,
										 double x0, double y0,
										 double x1, double y1,
										 double x2, double y2,
										 double x3, double y3 )
	{
		Planar<Gray> output = image.createNew(outWidth,outHeight);

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
		new FDistort(image,output).transform(new PointToPixelTransform_F32(homography)).apply();

		return new SimpleColor(output);
	}

	/**
	 * @see boofcv.alg.filter.blur.GBlurImageOps#gaussian
	 */
	public SimpleColor blurGaussian( double sigma, int radius ) {
		return new SimpleColor((Planar<Gray>)GBlurImageOps.gaussian(image, null, sigma, radius, null));
	}

	/**
	 * Converts the color image into a gray scale image by averaged each pixel across the bands
	 */
	public SimpleGray grayMean() {
		ImageGray out =
				GeneralizedImageOps.createSingleBand(image.imageType.getDataType(),image.width,image.height);

		GConvertImage.average(image, out);

		return new SimpleGray(out);
	}

	public SimpleGray getBand( int band ) {
		return new SimpleGray(image.getBand(band));
	}

	public int getNumberOfBands() {
		return image.getNumBands();
	}

	public PImage convert() {
		PImage out = new PImage(image.width,image.height, PConstants.RGB);
		if( image.getBandType() == GrayF32.class) {
			ConvertProcessing.convert_PF32_RGB((Planar<GrayF32>)image, out);
		} else if( image.getBandType() == GrayU8.class ) {
			ConvertProcessing.convert_PU8_RGB((Planar<GrayU8>)image,out);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return out;
	}
}
