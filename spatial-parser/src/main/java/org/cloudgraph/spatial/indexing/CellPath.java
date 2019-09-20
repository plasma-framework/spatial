package org.cloudgraph.spatial.indexing;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class CellPath {
    private GlobalCell root;
	private NumberFormat formatter = new DecimalFormat(GridUtil.PATTERN);     

	public CellPath(GlobalCell root) {
		super();
		this.root = root;
	}

	public GlobalCell getRoot() {
		return root;
	}
    
	public String asWkt() {
		StringBuilder buf = new StringBuilder();
		asWkt(this.root, null, buf);
		return buf.toString();
	}
    
	private void asWkt(Cell target, Cell source, StringBuilder buf) {
		
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
		
		for (Cell cell : target.getCells()) {
			asWkt(cell, target, buf);
		}
	}
}
