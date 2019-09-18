package org.cloudgraph.spatial.indexing;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;

/**
 * A grid cell which is globally addressable. 
 * @author scinnamond
 */
public class GlobalCell extends Cell {
 	/** global (cartesian) index X of the cell center in grid units from zero (can be positive or negative ) */
	private long globalX;
	/** global (cartesian) index Y of the cell center in grid units from zero (can be positive or negative ) */
	private long globalY;
	/** global grid index X of the cell center in grid index units  */
	private int gridX;
	/** global grid index Y of the cell center in grid index units   */
	private int gridY;
	
	public GlobalCell(Grid grid, Cell parent, int index, Envelope rect,
			int level) {
		super(grid, parent, index, rect, level);
		Point center = this.rect.getCenter();
		double x = center.getX();
		double y = center.getY();
        x = this.rect.getXMin();
        y = this.rect.getYMax();
		
		long longX = GridUtil.getGlobalUnits(x, this.grid.getRootCellUnits());
		if (longX > Integer.MAX_VALUE)
			throw new IllegalStateException("integer overflow X: " + longX);
		if (longX < Integer.MIN_VALUE)
			throw new IllegalStateException("integer overflow X: " + longX);
		this.globalX = (int)longX;
		this.gridX = GridUtil.getGlobalIndexUnits(x, this.grid.getRootCellUnits());
		
		long longY = GridUtil.getGlobalUnits(y, this.grid.getRootCellUnits());
		if (longY > Integer.MAX_VALUE)
			throw new IllegalStateException("integer overflow Y: " + longX);
		if (longY < Integer.MIN_VALUE)
			throw new IllegalStateException("integer overflow Y: " + longX);
		this.globalY = (int)longY;
		this.gridY = GridUtil.getGlobalIndexUnits(y, this.grid.getRootCellUnits());
		
		
	}
	
	public long getGlobalX() {
		return globalX;
	}
	public long getGlobalY() {
		return globalY;
	}

	public int getGridX() {
		return gridX;
	}

	public int getGridY() {
		return gridY;
	}


}
