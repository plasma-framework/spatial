package org.cloudgraph.spatial.indexing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;

public class Grid {
	private static Log log = LogFactory.getLog(Grid.class);
    private long rootCellUnits = 500000;
    private int cells = 16;
    private int cellsPerRow;
    private int levels = 4; 
    private Cell root;
	private double resolution = -1.0d;
	private int cellCount;
    
    public Grid(long rootCellUnits, int cells, int levels) {
		super();
		this.rootCellUnits = rootCellUnits;
		this.cells = cells;
		this.cellsPerRow = (int)Math.sqrt(this.cells);		
		if (this.cells < 4 || this.cells > 100)
			throw new IllegalArgumentException("illegal cells value range - expected [4 to 100]");
		if (((double)this.cellsPerRow) != Math.sqrt(this.cells))
			throw new IllegalArgumentException(
				"expected cells value where the square root is a whole number, not " + Math.sqrt(this.cells));
		this.levels = levels;
		if (log.isDebugEnabled())
		    log.debug("levels: " + this.levels + " cells: " + this.cells + " cells per row: " + this.cellsPerRow); 		
	}
    
    /**
     * Generates a cell tesselation tree for the given geometry.
     * @returns the root cell of the tesselation tree  
     */ 
    public Cell tesselate(Geometry shape) {
		Envelope2D boundry = new Envelope2D();
		shape.queryEnvelope2D(boundry);
		
 		this.root = snapToRootCellGrid(boundry); 
		long width = (long)this.root.getRect().getWidth();
		long height = (long)this.root.getRect().getHeight(); 		 
 		int widthX = (int)(width / (long)this.rootCellUnits);
 		int widthY = (int)(height / (long)this.rootCellUnits);
 		
 		tesselate(this.root, widthX, widthY, shape);
		return this.root;
    }
    
    public double getResolution() {
    	if (this.resolution == -1.0d) {
	    	if (this.root == null)
	    		throw new IllegalStateException("this grid has no tesselation - cannot determine resolution");
	    	GridResolutionFinder finder = new GridResolutionFinder(); 
	    	this.root.accept(finder);
	    	this.resolution = finder.getResult();
    	}
    	return this.resolution;
    }
    
    void incrementCellCount() {
    	this.cellCount++;
    }    
     
    public int getCellCount() {
		return cellCount;
	}

	/**
     * Generates a cell tesselation tree for the given geometry.  
     * @param source the source or parent cell
     * @param widthX the number of cells along X axis
     * @param widthY the number of cells along Y axis
     * @param geom the geometry
     */
    private void tesselate(Cell source, int widthX, int widthY, Geometry geom) {
    	if (source.getLevel() == this.levels) {
    		return;
    	}
    	
 		if (GeometryEngine.contains(geom, source.getRect(), null)) {
 			// current cell entirely contained by shape, no need to
 			// tesselate further
			return; 
		}
 		double width = source.getRect().getWidth();
 		double height = source.getRect().getHeight(); 		 
		double currX = source.getRect().getXMin();
		double currY = source.getRect().getYMax();
		double incrX = width / widthX;
		double incrY = height / widthY;
		int cellIndex = 0;
		for (int i = 0; i < widthY; i++) {
		    for (int j = 0; j < widthX; j++) {
 				Envelope cellRect = new Envelope(currX, currY-incrY, currX+incrX, currY);
				boolean disjoint = GeometryEngine.disjoint(cellRect, geom, null);
				if (!disjoint) {
					Cell target = null;
					if (source.getLevel() > 0)
	 				    target = new Cell(this, source, cellIndex, cellRect, source.getLevel()+1);
					else
						target = new GlobalCell(this, source, cellIndex, cellRect, source.getLevel()+1);
	 				source.addCell(target);
	 				//FIXME: kind of a hack, ignores the input cell width X/Y on recursion. 
				    tesselate(target, this.cellsPerRow, this.cellsPerRow, geom);
 				} // else don't need the cell as outside shape/line
  				currX = currX + incrX;
  				cellIndex++;
 			}
 			currY = currY - incrY;
 			currX = (int)source.getRect().getXMin();
 		}
    }    
    
	private Cell snapToRootCellGrid(Envelope2D shapeBoundingBox) {
		Envelope result = new Envelope(
				snapRectTerm(shapeBoundingBox.xmin, true), 
				snapRectTerm(shapeBoundingBox.ymin, true), 
				snapRectTerm(shapeBoundingBox.xmax, false), 
				snapRectTerm(shapeBoundingBox.ymax, false));
		return new RootCell(this, null, 0, result, 0);
	}
	
	private double snapRectTerm(double src, boolean minValue)
	{
 		long result = GridUtil.getGlobalUnits(src, this.rootCellUnits);
		if (minValue) {
			if (src < 0.0d)
 			    result = result - this.rootCellUnits;
		}
		else {
			if (src > 0.0d)
			    result = result + this.rootCellUnits;
		}
		return Long.valueOf(result).doubleValue();
	}	
	
    
	public long getRootCellUnits() {
		return rootCellUnits;
	}
	public void setRootCellUnits(long rootCellUnits) {
		this.rootCellUnits = rootCellUnits;
	}
	public int getCells() {
		return cells;
	}
	public void setCells(int cells) {
		this.cells = cells;
	}
	public int getLevels() {
		return levels;
	}
	public void setLevels(int levels) {
		this.levels = levels;
	}
    
    public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(this.getClass().getSimpleName());
		buf.append(" levels: ");
		buf.append(this.levels);
		buf.append(" cells: ");
		buf.append(this.cells);
		buf.append(" total cells: ");
		buf.append(this.getCellCount());
		buf.append(" resolution: ");
		buf.append(this.getResolution());
		buf.append(" ");
		buf.append(this.getUnitsAsString());
		return buf.toString();
	}

	private Object getUnitsAsString() {
 		return "meters";
	}
}
