package org.cloudgraph.spatial.parser;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudgraph.spatial.indexing.Cell;
import org.cloudgraph.spatial.indexing.CellAddress;
import org.cloudgraph.spatial.indexing.CellCounter;
import org.cloudgraph.spatial.indexing.CellPath;
import org.cloudgraph.spatial.indexing.CellVisitor;
import org.cloudgraph.spatial.indexing.Grid;
import org.cloudgraph.spatial.indexing.CellAddressCollector;
import org.cloudgraph.spatial.indexing.GridPrinter;
import org.cloudgraph.spatial.indexing.GridWktWriter;
import org.junit.Test;
import org.spatial.parser.wktLexer;
import org.spatial.parser.wktParser;
import org.spatial.parser.wktParser.GeometryContext;
import org.spatial.parser.wktParser.LineStringContext;
import org.spatial.parser.wktParser.MultiPointGeometryContext;
import org.spatial.parser.wktParser.MultiPolygonGeometryContext;
import org.spatial.parser.wktParser.PointContext;
import org.spatial.parser.wktParser.PointOrClosedPointContext;
import org.spatial.parser.wktParser.PolygonContext;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.OGCStructure;
import com.esri.core.geometry.Operator.Type;
import com.esri.core.geometry.OperatorBoundary;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.OperatorIntersects;
import com.esri.core.geometry.OperatorSimplifyOGC;
import com.esri.core.geometry.OperatorWithin;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.ProgressTracker;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.WktImportFlags;
import com.esri.core.geometry.ogc.OGCGeometry;
import com.google.common.collect.Iterables;

public class ShapefileTest {
	private static Log log = LogFactory.getLog(ShapefileTest.class);

	@Test
	public void testRead() throws Exception {
		CsvLineParser csvParser = new CsvLineParser('\t');
		// File f = new File("src/test/resources/2018_us_zcta510.csv");
		File f = new File("src/test/resources/us_states_4087.csv");

		BufferedReader reader = new BufferedReader(new FileReader(f));

		String line = "";

		int i = 1;
		while ((line = reader.readLine()) != null) {
			CSVRecord rec = csvParser.parse(line);
			// log.info(rec.toString());
			String wkt = rec.get(0);
			String field = rec.get(1);
			if (!("WV".equals(field)))
				continue;
 		    BufferedWriter stateWriter = new BufferedWriter(
 		    		new FileWriter("target/" + field + ".wkt"));
 		    stateWriter.write(wkt);		     
 		    stateWriter.close();
			
			log.info("line: " + String.valueOf(i) + " field: " + field);
			Geometry shape = OperatorImportFromWkt.local().execute(0, Geometry.Type.Unknown, wkt, null);
			log.info(field + " : " + shape.getType() + 
					" " + ((MultiPath)shape).getPathCount() + " rings" + " " + ((MultiPath)shape).getPointCount() + " vertices");
			//if (((MultiPath)shape).getPathCount() > 1)
			//	continue;
			//log.info(wkt);
 			boolean isSimple = OperatorSimplifyOGC.local().isSimpleOGC(shape,  null,  true, null, null);
 			Geometry simpleShape = OperatorSimplifyOGC.local().execute(shape, null, true, null);
  			isSimple = OperatorSimplifyOGC.local().isSimpleOGC(simpleShape,  null,  true, null, null);
			assertTrue(isSimple);
			Envelope2D boundry = new Envelope2D();
			simpleShape.queryEnvelope2D(boundry);
 			boolean boundryContainsShape = GeometryEngine.contains(new Envelope(boundry), simpleShape, null);
 			assertTrue(boundryContainsShape);
			
            // create an index grid  	
			long start = System.currentTimeMillis();
			Grid grid = new Grid(10000, 4, 6);
			Cell rootCell = grid.tesselate(simpleShape);
			log.info(grid);
			log.info("tesselated "+grid.getCellCount()+" cell shape "+field+" in " + String.valueOf(System.currentTimeMillis()-start));
//			CellVisitor printer = new GridPrinter();
//			rootCell.accept(printer);
			GridWktWriter writer = new GridWktWriter();
			rootCell.accept(writer);
 		    BufferedWriter gridWriter = new BufferedWriter(
 		    		new FileWriter("target/" + field + "_grid.wkt"));
 		    gridWriter.write(writer.getResult());		     
 		    gridWriter.close();
 		    
 		    CellAddressCollector indexCollector = new CellAddressCollector();
 		    rootCell.accept(indexCollector);
 		    for (CellAddress idx : indexCollector.getResult()) {
 		    	//log.info(idx);
 		    }
 		    
 		    Point2D point2d = boundry.getCenter();
 		    Point point = new Point(point2d);
 		    CellPath cellPath = grid.getPath(point);
 		    BufferedWriter centerPathWriter = new BufferedWriter(
 		    		new FileWriter("target/" + field + "_grid_center_path.wkt"));
 		    centerPathWriter.write(cellPath.asWkt());		     
 		    centerPathWriter.close();
   			
// 			boolean indexRectContainsBoundry = GeometryEngine.contains(rootCellRect, new Envelope(boundry), null);
//			log.info("indexRectContainsBoundry: " + indexRectContainsBoundry);
//			assertTrue(indexRectContainsBoundry);
//
// 			boolean indexRectContainsShape = GeometryEngine.contains(rootCellRect, simpleShape, null);
//			log.info("indexRectContainsShape: " + indexRectContainsShape);
//			assertTrue(indexRectContainsShape);
			
			
			
 			//GeometryContext geom = read(wkt, i);
			i++;
		}
		reader.close();
	}
	
	private String toTruncString(Envelope rect) {
		return "RECT: xmin: " + (long)rect.getXMin() + " ymin: " + 
			    (long)rect.getYMin() + " xmax: " + (long)rect.getXMax() + " ymax: " + (long)rect.getYMax();		
	}
	
//	{
//		OperatorIntersects.local().execute(geometryA, geometryB, sr, null);
//	}

	private Envelope[] divide(Envelope rect) {
		int width = (int)rect.getWidth();
		int height = (int)rect.getHeight(); 		 
 		List<Envelope> list = new ArrayList<>();
		int currX = (int)rect.getXMin();
		int currY = (int)rect.getYMax();
		int div = 1000;
		int xincr = width / div;
		int yincr = height / div;
		for (int i = 0; i < div; i++) {
		    for (int j = 0; j < div; j++) {
 				Envelope envelope = new Envelope(currX, currY-yincr, currX+xincr, currY);
 				list.add(envelope);
 				currX = currX + xincr;
 			}
 			currY = currY - yincr;
 		}
 		Envelope[] result = new Envelope[list.size()];
 		list.toArray(result);
		return result;
	}
	
	private Envelope[] quarter(Envelope src) {
		Envelope[] result = new Envelope[4];
		double xhalf = src.getXMin() + ((src.getXMax() - src.getXMin()) / 2);
		double yhalf = src.getYMin() + ((src.getYMax() - src.getYMin()) / 2);
		result[0] = new Envelope(src.getXMin(), yhalf, xhalf, src.getYMax()); // 1
		result[1] = new Envelope(xhalf, yhalf, src.getYMax(), src.getYMax()); // 2
		result[2] = new Envelope(src.getXMin(), src.getYMin(), xhalf, yhalf); // 3 
		result[3] = new Envelope(xhalf, src.getYMin(), src.getXMax(), yhalf); // 4
		return result;
	}
	
	private Envelope snapToGrid(Envelope2D src) {
		Envelope result = new Envelope(
			snapRectMinMaxLong(src.xmin, true), 
			snapRectMinMaxLong(src.ymin, true), 
			snapRectMinMaxLong(src.xmax, false), 
			snapRectMinMaxLong(src.ymax, false));
		return result;
	}
	
	private double snapRectMinMaxLong(double src, boolean minValue)
	{
		long longValue = (long)src;
		long result = longValue;
		if (minValue)
			result--;
		else
			result++;
		return Long.valueOf(result).doubleValue();
	}	
 	
	private GeometryContext read(String wkt, int line) {
		GeometryContext geom = parse(wkt);
		List<MultiPolygonGeometryContext> multiList = geom
				.multiPolygonGeometry();
		for (MultiPolygonGeometryContext multiPolygonGeom : multiList) {
			List<PolygonContext> polygonList = multiPolygonGeom.polygon();
			int poly = 1;
			for (PolygonContext polygon : polygonList) {
				log.info("line: " + String.valueOf(line) + " polygon: " + poly + " children: " + polygon.getChildCount());
				List<LineStringContext> lscList = polygon.lineString();
				for (LineStringContext lsc : lscList) {
					List<PointContext> points = lsc.point();
					for (PointContext point : points) {
						for (TerminalNode node : point.DECIMAL()) {
							log.info("decimal: " + node.toString());
						}
					}

				}
				poly++;
			}
		}
		return geom;
	}

	private GeometryContext parse(String sql) {
		ANTLRInputStream antlrStream = new ANTLRInputStream(sql);
		// SyntaxErrorListener errorListener = new SyntaxErrorListener();
		Lexer lexer = new wktLexer(antlrStream);
		// lexer.addErrorListener(errorListener);
		final CommonTokenStream tokens = new CommonTokenStream(lexer);
		wktParser parser = new wktParser(tokens);
		// parser.removeErrorListeners(); // gets rid of console listener
		// parser.addErrorListener(errorListener);
		GeometryContext root = (GeometryContext) parser.geometry();
		return root;
	}

	static class CsvLineParser {

		private final CSVFormat csvFormat;

		CsvLineParser(char fieldDelimiter) {
			this.csvFormat = CSVFormat.newFormat(fieldDelimiter);
		}

		public CSVRecord parse(String input) throws IOException {
			CSVParser csvParser = new CSVParser(new StringReader(input),
					csvFormat);
			return Iterables.getFirst(csvParser, null);
		}
	}
}
