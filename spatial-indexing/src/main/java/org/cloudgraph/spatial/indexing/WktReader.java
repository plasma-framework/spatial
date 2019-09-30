package org.cloudgraph.spatial.indexing;

import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.spatial.parser.wktBaseListener;
import org.spatial.parser.wktLexer;
import org.spatial.parser.wktParser;
import org.spatial.parser.wktParser.GeometryContext;
import org.spatial.parser.wktParser.LineStringContext;
import org.spatial.parser.wktParser.MultiPolygonGeometryContext;
import org.spatial.parser.wktParser.PointContext;
import org.spatial.parser.wktParser.PolygonContext;
import org.spatial.parser.wktParser.PolygonGeometryContext;

public class WktReader {
	
	
	public void read(String wkt) {
		GeometryContext geom = parse(wkt);
        ParseTreeWalker.DEFAULT.walk(new wktBaseListener(){
        	@Override
        	public void enterPoint(wktParser.PointContext ctx) {
        		
        	}
        }, geom);		
	}	

	public void read2(String wkt) {
		GeometryContext geom = parse(wkt);
		
		List<MultiPolygonGeometryContext> multiList = geom.multiPolygonGeometry();
		if (multiList.size() > 0) {
			for (MultiPolygonGeometryContext multiPolygonGeom : multiList) {
				List<PolygonContext> polygonList = multiPolygonGeom.polygon();
				int poly = 1;
				for (PolygonContext polygon : polygonList) {
 					List<LineStringContext> lscList = polygon.lineString();
					for (LineStringContext lsc : lscList) {
						List<PointContext> points = lsc.point();
						for (PointContext point : points) {
							for (TerminalNode node : point.DECIMAL()) {
								//log.info("decimal: " + node.toString());
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
 				List<LineStringContext> lscList = polygon.lineString();
				for (LineStringContext lsc : lscList) {
					List<PointContext> points = lsc.point();
					for (PointContext point : points) {
						for (TerminalNode node : point.DECIMAL()) {
							//log.info("decimal: " + node.toString());
						}
					}

				}
			}
		}
		 
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
