package org.cloudgraph.spatial.indexing;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GridWktWriter implements CellVisitor {
	private static Log log = LogFactory.getLog(GridWktWriter.class);
	private StringBuilder buf = new StringBuilder();
	private NumberFormat formatter = new DecimalFormat("###############.###############");     
	public GridWktWriter() {
	}
	
	public String getResult() {
		return buf.toString();
	}

	@Override
	public void visit(Cell target, Cell source, int level) {
		//if (source == null)
		//	return;
		buf.append("POLYGON (");
 		buf.append("(");
		
 		buf.append(formatter.format(target.getRect().getXMin()));
		buf.append(" ");
		buf.append(formatter.format(target.getRect().getYMin()));
		
		buf.append(",");
		
		buf.append(formatter.format(target.getRect().getXMax()));
		buf.append(" ");
		buf.append(formatter.format(target.getRect().getYMin()));

		buf.append(",");
		
		buf.append(formatter.format(target.getRect().getXMax()));
		buf.append(" ");
		buf.append(formatter.format(target.getRect().getYMax()));

		buf.append(",");
		
 		buf.append(formatter.format(target.getRect().getXMin()));
		buf.append(" ");
		buf.append(formatter.format(target.getRect().getYMax()));
		
		buf.append(",");
		
		buf.append(formatter.format(target.getRect().getXMin()));
		buf.append(" ");
		buf.append(formatter.format(target.getRect().getYMin()));
		
		buf.append(")");
		buf.append(")");
		
		buf.append('\n');
		buf.append("\r\n");
	}

}
