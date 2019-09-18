package org.cloudgraph.spatial.indexing;

public class CellAddress {
	/** global (cartesian) index X of the cell center in grid units from zero (can be positive or negative ) */
	private long globalX;
	/** global (cartesian) index Y of the cell center in grid units from zero (can be positive or negative ) */
	private long globalY;
	/** global  index X of the cell center in grid units from zero (can be positive or negative ) */
	private int gridX;
	/** global  index Y of the cell center in grid units from zero (can be positive or negative ) */
	private int gridY;
    private int[] values;
	public CellAddress(long globalX, long globalY, int gridX, int gridY,  int[] values) {
		super();
		this.globalX = globalX;
		this.globalY = globalY;
		this.gridX = gridX;
		this.gridY = gridY;
		this.values = values;
	}
	public long getGlobalX() {
		return globalX;
	}
	public long getGlobalY() {
		return globalY;
	}
	public int[] getValues() {
		return values;
	}
 
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(this.getClass().getSimpleName());
		buf.append(" ");
		buf.append(this.globalX);
		buf.append("/");
		buf.append(this.globalY);
		buf.append(" ");
		buf.append(this.gridX);
		buf.append("/");
		buf.append(this.gridY);
		if (values != null && values.length > 0) {
			buf.append(" ");
			for (int i = 0; i < values.length; i++) {
				if (i > 0)
					buf.append(".");
				buf.append(values[i]);
			}
		}
		return buf.toString();
	}
}
