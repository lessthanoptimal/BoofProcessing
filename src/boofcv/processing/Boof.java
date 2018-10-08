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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.ConfigCompleteSift;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.abst.segmentation.ImageSuperpixels;
import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.abst.tracker.ConfigComaniciu2003;
import boofcv.abst.tracker.ConfigTld;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.flow.ConfigBroxWarping;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.flow.ConfigHornSchunck;
import boofcv.factory.flow.ConfigHornSchunckPyramid;
import boofcv.factory.flow.ConfigOpticalFlowBlockPyramid;
import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.factory.scene.ClassifierAndSource;
import boofcv.factory.scene.FactoryImageClassifier;
import boofcv.factory.segmentation.*;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.*;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.so.Rodrigues_F64;
import org.ejml.data.DMatrixRMaj;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * Main class for invoking processing functions.
 *
 * @author Peter Abeles
 */
public class Boof {

	/**
	 * Convert PImage into a {@link SimpleGray} of the specified data type
	 * @param image Original input image.
	 * @param type Data type of image pixel.  F32 or U8
	 * @return Converted image
	 */
	public static SimpleGray gray(PImage image, ImageDataType type) {
		if (type == ImageDataType.F32) {
			GrayF32 out = new GrayF32(image.width, image.height);

			switch (image.format) {
				case PConstants.RGB:
				case PConstants.ARGB:
					ConvertProcessing.convert_RGB_F32(image, out);
					break;

				default:
					throw new RuntimeException("Unsupported image type");
			}

			return new SimpleGray(out);
		} else if (type == ImageDataType.U8) {
			GrayU8 out = new GrayU8(image.width, image.height);

			switch (image.format) {
				case PConstants.RGB:
				case PConstants.ARGB:
					ConvertProcessing.convert_RGB_U8(image, out);
					break;

				default:
					throw new RuntimeException("Unsupported image type");
			}

			return new SimpleGray(out);
		} else {
			throw new RuntimeException("Unsupport type: " + type);
		}
	}

	/**
	 * Convert PImage into a {@link SimpleColor} of the specified data type
	 * @param image Original input image.
	 * @param type Data type of image pixel.  F32 or U8
	 * @return Converted image
	 */
	public static SimpleColor colorMS(PImage image, ImageDataType type) {
		if (type == ImageDataType.F32) {
			Planar<GrayF32> out =
					new Planar<GrayF32>(GrayF32.class, image.width, image.height, 3);

			switch (image.format) {
				case PConstants.RGB:
				case PConstants.ARGB:
					ConvertProcessing.convert_RGB_PF32(image, out);
					break;

				default:
					throw new RuntimeException("Unsupported image type");
			}

			return new SimpleColor(out);
		} else if (type == ImageDataType.U8) {
			Planar<GrayU8> out =
					new Planar<GrayU8>(GrayU8.class, image.width, image.height, 3);

			switch (image.format) {
				case PConstants.RGB:
				case PConstants.ARGB:
					ConvertProcessing.convert_RGB_PU8(image, out);
					break;

				default:
					throw new RuntimeException("Unsupported image type");
			}

			return new SimpleColor(out);
		} else {
			throw new RuntimeException("Unsupport type: " + type);
		}
	}

	/**
	 * Creates a KLT point tracker
	 *
	 * @see PointTracker
	 * @see boofcv.alg.tracker.klt.PyramidKltTracker
	 *
	 * @param config Configuration for KLT tracker.  If null defaults will be used.
	 * @param configExtract Configuration for corner detector.  If null defaults will be used.
	 * @param imageType Image type which is processed.  F32 or U8
	 * @return Point tracker
	 */
	public static SimpleTrackerPoints trackerKlt(PkltConfig config,
												ConfigGeneralDetector configExtract,
												ImageDataType imageType) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);

		PointTracker tracker = FactoryPointTracker.klt(config, configExtract, inputType, derivType);

		return new SimpleTrackerPoints(tracker, inputType);
	}

	public static SimpleTrackerObject trackerTld(ConfigTld config, ImageDataType imageType) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		TrackerObjectQuad tracker = FactoryTrackerObjectQuad.tld(config,inputType);
		return new SimpleTrackerObject(tracker);
	}

	public static SimpleTrackerObject trackerMeanShiftComaniciu(ConfigComaniciu2003 config, ImageType imageType) {
		TrackerObjectQuad tracker = FactoryTrackerObjectQuad.meanShiftComaniciu2003(config, imageType);
		return new SimpleTrackerObject(tracker);
	}

	public static SimpleTrackerObject trackerCirculant(ConfigCirculantTracker config, ImageDataType imageType) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		TrackerObjectQuad tracker = FactoryTrackerObjectQuad.circulant(config, inputType);
		return new SimpleTrackerObject(tracker);
	}

	public static SimpleTrackerObject trackerSparseFlow(SfotConfig config, ImageDataType imageType) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		TrackerObjectQuad tracker = FactoryTrackerObjectQuad.sparseFlow(config, inputType, derivType);
		return new SimpleTrackerObject(tracker);
	}

	public static SimpleImageSegmentation segmentMeanShift( ConfigSegmentMeanShift config , ImageType imageType ) {
		ImageSuperpixels alg = FactoryImageSegmentation.meanShift(config,imageType);
		return new SimpleImageSegmentation(alg);
	}

	public static SimpleImageSegmentation segmentSlic( ConfigSlic config , ImageType imageType ) {
		ImageSuperpixels alg = FactoryImageSegmentation.slic(config, imageType);
		return new SimpleImageSegmentation(alg);
	}

	public static SimpleImageSegmentation segmentFH04( ConfigFh04 config , ImageType imageType ) {
		ImageSuperpixels alg = FactoryImageSegmentation.fh04(config, imageType);
		return new SimpleImageSegmentation(alg);
	}

	public static SimpleImageSegmentation segmentWatershed( ConfigWatershed config , ImageType imageType ) {
		ImageSuperpixels alg = FactoryImageSegmentation.watershed(config, imageType);
		return new SimpleImageSegmentation(alg);
	}

	public static SimpleDenseOpticalFlow flowKlt( PkltConfig configKlt, int radius , ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		Class derivType = GImageDerivativeOps.getDerivativeType(inputType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.flowKlt(configKlt,radius,inputType,derivType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDenseOpticalFlow flowRegion( ConfigOpticalFlowBlockPyramid config, ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.region(config, inputType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDenseOpticalFlow flowHornSchunck( ConfigHornSchunck config, ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.hornSchunck(config, inputType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDenseOpticalFlow flowHornSchunckPyramid( ConfigHornSchunckPyramid config, ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.hornSchunckPyramid(config, inputType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDenseOpticalFlow flowBroxWarping( ConfigBroxWarping configKlt, ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);
		DenseOpticalFlow flow = FactoryDenseOpticalFlow.broxWarping(configKlt,inputType);
		return new SimpleDenseOpticalFlow(flow);
	}

	public static SimpleDetectDescribePoint detectSurf( boolean stable , ImageDataType imageType ) {
		Class inputType = ImageDataType.typeToSingleClass(imageType);

		DetectDescribePoint ddp;
		if( stable )
			ddp = FactoryDetectDescribe.surfStable(null,null,null,inputType);
		else
			ddp = FactoryDetectDescribe.surfFast(null,null,null,inputType);

		return new SimpleDetectDescribePoint(ddp, ImageType.single(inputType));
	}

	public static SimpleDetectDescribePoint detectSift( ImageDataType imageType ) {
		if( imageType != ImageDataType.F32 )
			throw new IllegalArgumentException("Only GrayF32 is supported, e.g. ImageDataType.F32");

		DetectDescribePoint ddp = FactoryDetectDescribe.sift(new ConfigCompleteSift());

		return new SimpleDetectDescribePoint(ddp, ImageType.single(GrayF32.class));
	}

	public static SimpleAssociateDescription associateGreedy( SimpleDetectDescribePoint detector ,
															  boolean backwardsValidation ) {

		ScoreAssociation score = FactoryAssociation.defaultScore(detector.detectDescribe.getDescriptionType());

		AssociateDescription assoc = FactoryAssociation.greedy(score,Double.MAX_VALUE,backwardsValidation);

		return new SimpleAssociateDescription(assoc);
	}

	/**
	 * Creates a square-binary fiducial detector which is light invariant.
	 *
	 * @param width Width of square in world units
	 */
	public static SimpleFiducial fiducialSquareBinaryRobust( double width  ) {
		return new SimpleFiducial(FactoryFiducial.squareBinary(new ConfigFiducialBinary(width),
				ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 15), GrayU8.class));
	}

	/**
	 * Creates a square-binary fiducial detector which is light invariant.
	 */
	public static SimpleFiducialSquareImage fiducialSquareImageRobust() {
		return new SimpleFiducialSquareImage(FactoryFiducial.squareImage(new ConfigFiducialImage(),
				ConfigThreshold.local(ThresholdType.LOCAL_MEAN,15), GrayU8.class));
	}

	/**
	 * Creates a square-binary fiducial detector
	 *
	 * @param width Width of square in world units
	 * @param threshold Binary threshold
	 */
	public static SimpleFiducial fiducialSquareBinary( double width , int threshold ) {
		return new SimpleFiducial(FactoryFiducial.squareBinary(new ConfigFiducialBinary(width),
				ConfigThreshold.fixed(threshold), GrayU8.class));
	}

	/**
	 * Creates a square-binary fiducial detector
	 *
	 * @param threshold Binary threshold
	 */
	public static SimpleFiducialSquareImage fiducialSquareImage( int threshold ) {
		return new SimpleFiducialSquareImage(FactoryFiducial.squareImage(new ConfigFiducialImage(),
				ConfigThreshold.fixed(threshold), GrayU8.class));
	}

	/**
	 * Returns a class for converting equirectangular images into pinhole images
	 */
	public static EquirectangularToPinhole equirectangularToPinhole() {
		return new EquirectangularToPinhole();
	}

	public static DMatrixRMaj eulerXYZ(double rotX , double rotY , double rotZ ) {
		return ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX,rotY,rotZ,null);
	}

	public static DMatrixRMaj rodrigues( double angle , double axisX , double axisY , double axisZ ) {
		return ConvertRotation3D_F64.rodriguesToMatrix(new Rodrigues_F64(angle,axisX,axisY,axisZ),null);
	}

	public static DMatrixRMaj quaternion( double w , double x , double y , double z ) {
		return ConvertRotation3D_F64.quaternionToMatrix(w,x,y,z,null);
	}

	public static SimpleImageClassification imageClassification( String which ) {
		ClassifierAndSource cs;
		if( which.compareTo("VGG") == 0 ) {
			cs = FactoryImageClassifier.vgg_cifar10();
		} else if( which.compareTo("NIN") == 0 ) {
			cs = FactoryImageClassifier.nin_imagenet();
		} else {
			throw new IllegalArgumentException("Unknown model.  Valid options.  VGG, NIN");
		}

		return new SimpleImageClassification(cs);
	}

	public static SimpleQrCode detectQR() {
		return new SimpleQrCode();
	}

	public static PImage renderQR( String message , int pixelPerModule ) {
		return SimpleQrCode.generate(message,pixelPerModule);
	}

}
