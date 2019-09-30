package org.cloudgraph.spatial.indexing;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.spatial.parser.wktLexer;
import org.spatial.parser.wktParser;
import org.spatial.parser.wktParser.GeometryContext;
import org.spatial.parser.wktParser.LineStringContext;
import org.spatial.parser.wktParser.MultiPolygonGeometryContext;
import org.spatial.parser.wktParser.PointContext;
import org.spatial.parser.wktParser.PolygonContext;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.OperatorSimplifyOGC;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Point2D;

public class ShapefileIndexTest {
	private static Log log = LogFactory.getLog(ShapefileIndexTest.class);

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
			Grid grid = new Grid(250000, 4, 12);
			grid.accellerate(simpleShape);
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
 		    
// 		    CellAddressCollector indexCollector = new CellAddressCollector();
// 		    rootCell.accept(indexCollector);
// 		    for (CellAddress idx : indexCollector.getResult()) {
// 		    	log.info(idx);
// 		    }
 		    
 		    Point2D point2d = boundry.getCenter();
 		    Point point = new Point(point2d);
 		    CellPath cellPath = grid.getPath(point);
 		    BufferedWriter centerPathWriter = new BufferedWriter(
 		    		new FileWriter("target/" + field + "_grid_center_path.wkt"));
 		    centerPathWriter.write(cellPath.asWkt());		     
 		    centerPathWriter.close();
   			
  			//GeometryContext geom = read(wkt, i);
			i++;
		}
		reader.close();
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
}
