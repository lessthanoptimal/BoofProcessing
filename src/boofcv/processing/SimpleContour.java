/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.FitData;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.struct.PointIndex_I32;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_I32;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage for a the contours from a binary image and the labeled blobs inside the image.
 *
 * @author Peter Abeles
 */
public class SimpleContour {
	Contour contour;

	public SimpleContour(Contour contour) {
		this.contour = contour;
	}

	/**
	 * Fits a polygon to the specified contour.
	 *
	 * @see ShapeFittingOps#fitPolygon(List, boolean, int, double)
	 *
	 * @param external true for the external contour or false for all the internal contours
	 * @param minimumSideLength The minimum allowed side length in pixels. Try 10
	 * @param cornerPenalty How much a corner is penalized. Try 0.25
	 * @return List of polygons described by their vertexes
	 */
	public List<List<Point2D_I32>> fitPolygon( boolean external , int minimumSideLength , double cornerPenalty ) {
		List<List<Point2D_I32>> polygons = new ArrayList<List<Point2D_I32>>();

		if( external ) {
			List<PointIndex_I32> output = ShapeFittingOps.
					fitPolygon(contour.external, true,minimumSideLength,cornerPenalty);

			List<Point2D_I32> poly = new ArrayList<Point2D_I32>();
			for( PointIndex_I32 p : output ) {
				poly.add( new Point2D_I32(p.x,p.y));
			}
			polygons.add(poly);
		} else {
			for( List<Point2D_I32> i : contour.internal ) {
				List<PointIndex_I32> output = ShapeFittingOps.
						fitPolygon(i, true, minimumSideLength,cornerPenalty);

				List<Point2D_I32> poly = new ArrayList<Point2D_I32>();
				for (PointIndex_I32 p : output) {
					poly.add(new Point2D_I32(p.x, p.y));
				}
				polygons.add(poly);
			}
		}

		return polygons;
	}

	/**
	 * Fits ellipse(s) to the specified contour
	 *
	 * @see boofcv.alg.shapes.ShapeFittingOps#fitEllipse_I32
	 *
	 * @param external true for the external contour or false for all the internal contours
	 * @return List of found ellipses
	 */
	public List<EllipseRotated_F64> fitEllipses(boolean external ) {
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		if( external ) {
			FitData<EllipseRotated_F64> found = ShapeFittingOps.fitEllipse_I32(contour.external,0,false,null);
			ellipses.add(found.shape);
		} else {
			for( List<Point2D_I32> i : contour.internal ) {
				FitData<EllipseRotated_F64> found = ShapeFittingOps.fitEllipse_I32(i,0,false,null);
				ellipses.add(found.shape);
			}
		}

		return ellipses;
	}

	public void visualize( PImage image , int colorExternal , int colorInternal )  {
		for( Point2D_I32 p : contour.external ) {
			int index = p.y * image.width + p.x;
			image.pixels[index] = colorExternal;
		}

		for( List<Point2D_I32> i : contour.internal ) {
			for( Point2D_I32 p : i ) {
				int index = p.y * image.width + p.x;
				image.pixels[index] = colorInternal;
			}
		}
	}

	public Contour getContour() {
		return contour;
	}
}
