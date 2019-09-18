package org.cloudgraph.spatial.indexing;

 
 
public class GridResolutionFinder implements CellVisitor {
 	private double result = 0.0d; 
 	public GridResolutionFinder() {
	}
	
	public double getResult() {
		 return result;
	}

	@Override
	public void visit(Cell target, Cell source, int level) {
		if (source == null)
			return; // ignore root
		if (!target.hasCells()) { // leaf
			this.result = target.getRect().getWidth();
 		}
	}	
	
}
