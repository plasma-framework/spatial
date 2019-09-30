package org.cloudgraph.spatial.indexing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudgraph.spatial.geometry.GeometryTransformation;
import org.junit.Test;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.spatial.parser.wktBaseListener;
import org.spatial.parser.wktLexer;
import org.spatial.parser.wktParser;
import org.spatial.parser.wktParser.GeometryContext;
import org.spatial.parser.wktParser.LineStringContext;
import org.spatial.parser.wktParser.MultiPolygonGeometryContext;
import org.spatial.parser.wktParser.PointContext;
import org.spatial.parser.wktParser.PolygonContext;
import org.spatial.parser.wktParser.PolygonGeometryContext;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.OperatorExportToWkt;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SimpleGeometryCursor;
import com.google.common.collect.Iterables;

public class ShapeFromWGS84IndexGridTest {
	private static Log log = LogFactory.getLog(ShapeFromWGS84IndexGridTest.class);
	private CRSFactory crsFactory;
	private CoordinateReferenceSystem WGS84;
	private CsvLineParser csvParser;
	private File inputFile;
	
	public ShapeFromWGS84IndexGridTest() {
	    this.crsFactory = new CRSFactory();

	    String WGS84_PARAM = "+title=long/lat:WGS84 +proj=longlat +datum=WGS84 +units=degrees";
	    this.WGS84 = crsFactory.createFromParameters("WGS84", WGS84_PARAM); // 4326		
	    this.csvParser = new CsvLineParser('\t');
	    this.inputFile = new File("src/test/resources/us_states_4326.csv"); //WSG84
	}
	
	@Test
	public void testTo3005() throws Exception {		
	    int targetSrs = 3005; //NAD83 / BC Albers 
	    CoordinateReferenceSystem targetCrs = this.crsFactory.createFromName("EPSG:"+targetSrs+"");
	    File targetDir = new File("target/"+targetSrs+"/");
	    targetDir.mkdirs();
	    process(this.WGS84, targetCrs, targetSrs, targetDir);
	}
	
	@Test
	public void testTo4087() throws Exception {		
	    int targetSrs = 4087; // WGS 84 / World Equidistant Cylindrical 
	    CoordinateReferenceSystem targetCrs = this.crsFactory.createFromName("EPSG:"+targetSrs+"");
	    File targetDir = new File("target/"+targetSrs+"/");
	    targetDir.mkdirs();
	    process(this.WGS84, targetCrs, targetSrs, targetDir);
	}
	
	@Test
	public void testTo3857() throws Exception {		
	    int targetSrs = 3857; //  WGS 84 / Pseudo-Mercator 
	    CoordinateReferenceSystem targetCrs = this.crsFactory.createFromName("EPSG:"+targetSrs+"");
	    File targetDir = new File("target/"+targetSrs+"/");
	    targetDir.mkdirs();
	    process(this.WGS84, targetCrs, targetSrs, targetDir);
	}
	
 
	@Test
	public void testTo2163() throws Exception {		
	    int targetSrs = 2163; //  US National Atlas Equal Area 
	    CoordinateReferenceSystem targetCrs = this.crsFactory.createFromName("EPSG:"+targetSrs+"");
	    File targetDir = new File("target/"+targetSrs+"/");
	    targetDir.mkdirs();
	    process(this.WGS84, targetCrs, targetSrs, targetDir);
	}

	private void process(CoordinateReferenceSystem srcCrs, CoordinateReferenceSystem targetCrs, 
			int targetSrsId, File targetDir) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(this.inputFile));

		String line = "";

 		while ((line = reader.readLine()) != null) {
			CSVRecord rec = csvParser.parse(line);
			// log.info(rec.toString());
			String wkt = rec.get(0);
			String field = rec.get(1);
			//if (!("WV".equals(field)))
			//	continue;
			Geometry shape = OperatorImportFromWkt.local().execute(0, 
					Geometry.Type.Unknown, wkt, null);
			GeometryTransformation xform = new GeometryTransformation(srcCrs, targetCrs);
			shape = xform.transform(shape);
			
			String targetWkt = OperatorExportToWkt.local().execute(0, shape, null);
 		    BufferedWriter wktWriter = new BufferedWriter(
 		    		new FileWriter(new File(targetDir, field + "_"+targetSrsId+".wkt")));
 		    wktWriter.write(targetWkt);		     
 		    wktWriter.close();
			
			Grid grid = new Grid(100000, 4, 6);
			grid.accellerate(shape);
			Cell rootCell = grid.tesselate(shape);
			log.info(grid);
 			GridWktWriter writer = new GridWktWriter();
			rootCell.accept(writer);
 		    BufferedWriter gridWriter = new BufferedWriter(
 		    		new FileWriter(new File(targetDir, field + "_"+targetSrsId+"_grid.wkt")));
 		    gridWriter.write(writer.getResult());		     
 		    gridWriter.close();
 		    
 		    CellAddressCollector indexCollector = new CellAddressCollector();
 		    rootCell.accept(indexCollector);
 		    for (CellAddress idx : indexCollector.getResult()) {
 		    	//log.info(idx);
 		    }
 		    
			Envelope2D boundry = new Envelope2D();
			shape.queryEnvelope2D(boundry);
		    Point2D point2d = boundry.getCenter();
 		    Point point = new Point(point2d);
 		    CellPath cellPath = grid.getPath(point);
 		    BufferedWriter centerPathWriter = new BufferedWriter(
 		    		new FileWriter(new File(targetDir, field + "_"+targetSrsId+"_grid_center_path.wkt")));
 		    centerPathWriter.write(cellPath.asWkt());		     
 		    centerPathWriter.close();
 		    
 		}
		reader.close();
		
	}
	
	private void walk(String wkt)
	{
		GeometryContext geom = parse(wkt);
        ParseTreeWalker.DEFAULT.walk(new wktBaseListener(){
        	@Override
        	public void enterPoint(wktParser.PointContext ctx) {
        		TerminalNode nodeX = ctx.DECIMAL(0);
        		TerminalNode nodeY = ctx.DECIMAL(1);
        		Double x = Double.valueOf(nodeX.getText());
        		Double y = Double.valueOf(nodeY.getText());
        		//log.info("transformed: " + p + " to " + pout);
        	}
        }, geom);				
	}
	 	
	private GeometryContext read(String wkt, int line) {
		GeometryContext geom = parse(wkt);
		List<MultiPolygonGeometryContext> multiList = geom.multiPolygonGeometry();
		if (multiList.size() > 0) {
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
		}
		else {
			List<PolygonGeometryContext> polygonList = geom.polygonGeometry();
			for (PolygonGeometryContext pgc : polygonList) {
				PolygonContext polygon = pgc.polygon();
				log.info("line: " + String.valueOf(line) + " polygon: " + 1 + " children: " + polygon.getChildCount());
				List<LineStringContext> lscList = polygon.lineString();
				for (LineStringContext lsc : lscList) {
					List<PointContext> points = lsc.point();
					for (PointContext point : points) {
						for (TerminalNode node : point.DECIMAL()) {
							log.info("decimal: " + node.toString());
						}
					}

				}
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
