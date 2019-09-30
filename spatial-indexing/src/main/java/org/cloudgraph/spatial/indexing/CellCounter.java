package org.cloudgraph.spatial.indexing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CellCounter implements CellVisitor {
	private static Log log = LogFactory.getLog(CellCounter.class);
	private int count;
 	public CellCounter() {
	}
	
	public int getResult() {
		return this.count;
	}

	@Override
	public void visit(Cell target, Cell source, int level) {
		if (source == null)
			return; // ignore root
		this.count++;
	}

}
