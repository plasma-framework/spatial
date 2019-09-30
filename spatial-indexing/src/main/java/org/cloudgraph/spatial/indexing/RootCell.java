package org.cloudgraph.spatial.indexing;

import com.esri.core.geometry.Envelope;

public class RootCell extends Cell {

	public RootCell(Grid grid, Cell parent, int index, Envelope rect, int level) {
		super(grid, parent, index, rect, level);
 	}

}
