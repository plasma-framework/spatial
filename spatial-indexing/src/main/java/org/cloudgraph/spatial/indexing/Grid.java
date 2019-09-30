package org.cloudgraph.spatial.indexing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree;
import com.esri.core.geometry.Operator;
import com.esri.core.geometry.OperatorContains;
import com.esri.core.geometry.OperatorDisjoint;
import com.esri.core.geometry.OperatorFactoryLocal;
import com.esri.core.geometry.Point;

public class Grid {
	private static Log log = LogFactory.getLog(Grid.class);
	private Geometry shape;
    private long rootCellUnits = 500000;
    private int cells = 16;
    private int cellsPerRow;
    private int levels = 4; 
    private Cell root;
	private double resolution = -1.0d;
	private int cellCount;
	private OperatorDisjoint disjointOp;
	private OperatorContains containsOp;
    @SuppressWarnings("unused")
	private Grid() {}
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
		//OperatorFactoryLocal engine = OperatorFactoryLocal.getInstance();
		this.disjointOp = (OperatorDisjoint) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Disjoint));
		this.containsOp = (OperatorContains) (OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Contains));
	}
    
    public void accellerate(Geometry shape) {
		this.disjointOp.accelerateGeometry(shape, null, GeometryAccelerationDegree.enumHot);		    	
		this.containsOp.accelerateGeometry(shape, null, GeometryAccelerationDegree.enumHot);		    	
    }
    
    /**
     * Generates a cell tesselation tree for the given geometry.
     * @returns the root cell of the tesselation tree  
     */ 
    public Cell tesselate(Geometry shape) {
		Envelope2D boundry = new Envelope2D();
		shape.queryEnvelope2D(boundry);
		Envelope shapeEnvelope = new Envelope(boundry);
 		
 		this.root = snapToRootCellGrid(boundry); 
		long width = (long)this.root.getRect().getWidth();
		long height = (long)this.root.getRect().getHeight(); 
		// Note: for root cells the number of cells for X and Y can vary
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
    
    public CellAddress getAddress(Point point) {
		Envelope globalCellRect = new Envelope(
			snapRectTerm(point.getX(), true), 
			snapRectTerm(point.getY(), true), 
			snapRectTerm(point.getX(), false), 
			snapRectTerm(point.getY(), false));
		int undexUnitsX = GridUtil.getGlobalIndexUnits(point.getX(), this.rootCellUnits);
		int undexUnitsY = GridUtil.getGlobalIndexUnits(point.getY(), this.rootCellUnits);
		long width = (long)globalCellRect.getWidth();
		long height = (long)globalCellRect.getHeight(); 		 
 		int widthX = (int)(width / (long)this.rootCellUnits);
 		int widthY = (int)(height / (long)this.rootCellUnits);		
 		
 		List<Integer> values = new ArrayList<>();
 		tesselate(globalCellRect, widthX, widthY, point, values, 0);
		int[] valueArray = new int[values.size()];
		int i = 0;
		for (Integer val : values) {
			valueArray[i] = val;
			i++;
		}
 		
		CellAddress result = new CellAddress(
			(long)globalCellRect.getXMin(), (long)globalCellRect.getYMax(), 
			undexUnitsX, undexUnitsY, valueArray);
		
		return result;
    }
    
    private void tesselate(Envelope source, int widthX, int widthY, Point point, List<Integer> values, int level)
    {
    	if (level == this.levels) {
    		return;
    	}
 		double width = source.getWidth();
 		double height = source.getHeight(); 		 
		double currX = source.getXMin();
		double currY = source.getYMax();
		double incrX = width / widthX;
		double incrY = height / widthY;
		int cellIndex = 0;
		for (int i = 0; i < widthY; i++) {
		    for (int j = 0; j < widthX; j++) {
 				Envelope cellRect = new Envelope(currX, currY-incrY, currX+incrX, currY);
 		 		if (this.containsOp.execute(cellRect, point, null, null)) {
 		 			values.add(cellIndex);
 	 				tesselate(cellRect, this.cellsPerRow, this.cellsPerRow, point, values, level+1);
 				}
  				currX = currX + incrX;
  				cellIndex++;
 			}
 			currY = currY - incrY;
 			currX = (int)source.getXMin();
 		}    	
    }
    
    public CellPath getPath(Point point) {
		Envelope globalCellRect = new Envelope(
			snapRectTerm(point.getX(), true), 
			snapRectTerm(point.getY(), true), 
			snapRectTerm(point.getX(), false), 
			snapRectTerm(point.getY(), false));
		long width = (long)globalCellRect.getWidth();
		long height = (long)globalCellRect.getHeight(); 		 
 		int widthX = (int)(width / (long)this.rootCellUnits);
 		int widthY = (int)(height / (long)this.rootCellUnits);		
 		
 		GlobalCell root = new GlobalCell(this, null, -1, globalCellRect, 1);
 		tesselate(root, widthX, widthY, point);
 		
 		CellPath result = new CellPath(root);
 		
		return result;
    }
    
	/**
     * Generates a cell tesselation tree for the given geometry.  
     * @param source the source or parent cell
     * @param widthX the number of cells along X axis
     * @param widthY the number of cells along Y axis
     * @param geom the geometry
     */
    private void tesselate(Cell source, int widthX, int widthY, Point point) {
    	if (source.getLevel() == this.levels) {
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
 				if (this.containsOp.execute(cellRect, point, null, null)) {
 				    Cell target = new Cell(this, source, cellIndex, cellRect, source.getLevel()+1);
	 				source.addCell(target);
	 				//FIXME: kind of a hack, ignores the input cell width X/Y on recursion. 
				    tesselate(target, this.cellsPerRow, this.cellsPerRow, point);
 				} // else don't need the cell 
  				currX = currX + incrX;
  				cellIndex++;
 			}
 			currY = currY - incrY;
 			currX = (int)source.getRect().getXMin();
 		}
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
    	
 		if (this.containsOp.execute(geom, source.getRect(), null, null)) {
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
 				boolean disjoint = false;
				disjoint = this.disjointOp.execute(cellRect, geom, null, null);
				if (!disjoint) {
					Cell target = null;
					if (source.getLevel() > 0)
	 				    target = new Cell(this, source, cellIndex, cellRect, source.getLevel()+1);
					else
						target = new GlobalCell(this, source, cellIndex, cellRect, source.getLevel()+1);
	 				source.addCell(target);
	 				//FIXME: kind of a hack, ignores the input cell width X/Y on recursion. 
				    tesselate(target, this.cellsPerRow, this.cellsPerRow, geom);
				}
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
