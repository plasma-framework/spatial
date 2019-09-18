package org.cloudgraph.spatial.indexing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GridPrinter implements CellVisitor {
	private static Log log = LogFactory.getLog(GridPrinter.class);

	@Override
	public void visit(Cell target, Cell source, int level) {
		if (log.isDebugEnabled()) {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < level; i++)
				buf.append("\t");
			buf.append(target);
		    log.debug(buf.toString()); 		
		}
	}

}
