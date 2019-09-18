package org.cloudgraph.spatial.indexing;

public interface CellVisitor {
    public void visit(Cell target, Cell source, int level);
}
