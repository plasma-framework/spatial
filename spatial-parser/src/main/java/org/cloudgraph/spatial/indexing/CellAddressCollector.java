package org.cloudgraph.spatial.indexing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CellAddressCollector implements CellVisitor {
	private static Log log = LogFactory.getLog(CellAddressCollector.class);
	List<CellAddress> result = new ArrayList<>(); 
 	public CellAddressCollector() {
	}
	
	public List<CellAddress> getResult() {
		 return result;
	}

	@Override
	public void visit(Cell target, Cell source, int level) {
		if (source == null)
			return; // ignore root
		if (!target.hasCells()) { // leaf
			CellAddress index = collect(target); 
			result.add(index);
		}
	}	
	
	private CellAddress collect(Cell target) {
		List<Integer> list = null;
 		Cell current = target;
  		while (true) {
			if (GlobalCell.class.isInstance(current)) {
				GlobalCell root = GlobalCell.class.cast(current);
				int[] indexPath = new int[0];
				if (list != null) {		
					indexPath = new int[list.size()];
					int i = 0;
					for (int j = list.size()-1; j >= 0; j--) {
						indexPath[i] = list.get(j);
						i++;
					}
				}
				return new CellAddress(root.getGlobalX(), root.getGlobalY(), 
						root.getGridX(), root.getGridY(), indexPath);		    	 
			}
			else {
				if (list == null)
					list = new ArrayList<>();
				list.add(current.getIndex());
			}
			current = current.getParent();
		}
 	}
	
	private GlobalCell findGlobalCell(Cell target) {
 		Cell parent = target.getParent();
		while (true) {
			if (GlobalCell.class.isInstance(parent))
				return GlobalCell.class.cast(parent);
			parent = target.getParent();
		}
 	}

	private Cell findRoot(Cell target) {
		Cell result = target.getParent();
		while (true) {
			if (target.getParent() != null)
			    result = target.getParent();
			else
				break;
		}
		return result;
	}
}
