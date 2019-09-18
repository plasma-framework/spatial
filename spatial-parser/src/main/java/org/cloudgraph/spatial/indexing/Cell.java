package org.cloudgraph.spatial.indexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.esri.core.geometry.Envelope;

/**
 * A grid cell which is not globally addressable but must rely on its cell ancestry
 * for global address information. 
 * @author scinnamond
 */
public class Cell {
	protected Grid grid;
	protected Cell parent; // can be null
	protected int level;
	/** index relative to the parent cell */
	protected int index;
	protected Envelope rect;
	protected List<Cell> cells;
	public Cell(Grid grid, Cell parent, int index, Envelope rect, int level) {
		super();
		this.grid = grid;
		this.grid.incrementCellCount();
		this.parent = parent;
		this.index = index;
		this.level = level;
		this.rect = rect;
	}
	
	public Grid getGrid() {
		return grid;
	}

	public Cell getParent() {
		return parent;
	}

	public List<Cell> getCells() {
		if (this.cells != null)
		    return this.cells;
		else
			return Collections.emptyList();
	}
	public boolean hasCells() {
		return this.cells != null;
	}
	public void addCell(Cell cell) {
		if (this.cells == null)
			this.cells = new ArrayList<>();
		this.cells.add(cell);
	}
	public void setCells(List<Cell> cells) {
		this.cells = cells;
	}
	public Envelope getRect() {
		return rect;
	}
	
	public int getLevel() {
		return level;
	}
	public int getIndex() {
		return index;
	}
	
	public String toTruncString() {
		return this.getClass().getSimpleName() + ": ("+this.level+"/"+this.index+") xmin: " + (long)rect.getXMin() + " ymin: " + 
			    (long)rect.getYMin() + " xmax: " + (long)rect.getXMax() + " ymax: " + (long)rect.getYMax();		
	}
	
	public void accept(CellVisitor visitor) {
		accept(this, null, visitor, 0);
	}
	
	private void accept(Cell target, Cell source, CellVisitor visitor, int level) {
		visitor.visit(target, source, level);
		for (Cell cell: target.getCells())
			accept(cell, target, visitor, level+1);
	}
	
	public String toString() {
		return toTruncString();
	}
}
