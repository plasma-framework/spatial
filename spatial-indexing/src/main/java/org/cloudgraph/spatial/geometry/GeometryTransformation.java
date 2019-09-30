package org.cloudgraph.spatial.geometry;

import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Line;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;

public class GeometryTransformation {
	private CoordinateReferenceSystem srcCrs;
	private CoordinateReferenceSystem targetCrs;
	private CoordinateTransformFactory ctFactory;

	@SuppressWarnings("unused")
	private GeometryTransformation(){}
	public GeometryTransformation(CoordinateReferenceSystem srcCrs,
			CoordinateReferenceSystem targetCrs) {
		super();
		this.srcCrs = srcCrs;
		this.targetCrs = targetCrs;
		this.ctFactory = new CoordinateTransformFactory();
	}

	public Geometry transform(Geometry srcGeom) {
		CoordinateTransform trans = ctFactory.createTransform(this.srcCrs,
				this.targetCrs);
		switch (srcGeom.getType()) {
		case Polygon:
			Polygon poly = (Polygon) srcGeom;
			for (int i = 0; i < poly.getPointCount(); i++) {
				transformPoint(poly.getPoint(i), trans);
			}
			break;
		case Line:
			Line line = (Line) srcGeom;
			Point2D start = line.getStartXY();
			Point2D end = line.getEndXY();
			transformPoint(start, trans);
			transformPoint(end, trans);
			line.setStartXY(start);
			line.setEndXY(end);
			break;
 		case MultiPoint:
 			MultiPoint multiPoint = (MultiPoint) srcGeom;
			for (int i = 0; i < multiPoint.getPointCount(); i++) {
				transformPoint(multiPoint.getPoint(i), trans);
			}
			break;
		case Point:
			Point point = (Point) srcGeom;
			transformPoint(point, trans);
			break;
		case Polyline:
			Polyline polyline = (Polyline) srcGeom;
			for (int i = 0; i < polyline.getPointCount(); i++) {
				transformPoint(polyline.getPoint(i), trans);
			}
		case Envelope:
		case Unknown:
		default:
			throw new IllegalStateException("unknown geom type: "
					+ srcGeom.getType());
		}
		return srcGeom;
	}
	
	private void transformPoint(Point point, CoordinateTransform trans) {
		ProjCoordinate in = new ProjCoordinate(point.getX(),
				point.getY());
		ProjCoordinate out = new ProjCoordinate();
		trans.transform(in, out);
		point.setX(out.x);
		point.setY(out.y);		
		// log.info("transformed: " + in + " to " + point);
	}
	
	private void transformPoint(Point2D point, CoordinateTransform trans) {
		ProjCoordinate in = new ProjCoordinate(point.x,
				point.y);
		ProjCoordinate out = new ProjCoordinate();
		trans.transform(in, out);
		point.x = out.x;
		point.y = out.y;		
		// log.info("transformed: " + in + " to " + point);
	}

}
