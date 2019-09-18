package org.cloudgraph.spatial.indexing;

public class GridUtil {
	/**
	 * Return the number of root cell index units for the given value based on the
	 * current grid units
	 * @param src the value
	 * @return the index value
	 */
	public static long getGlobalUnits(double src, long rootCellUnits)
	{
		long value = (long)Math.abs(src);
		long rootCells = value / rootCellUnits;
  		long result = rootCells * rootCellUnits;
 		if (src < 0.0d) //FIXME: may need to accommodate if src == 0 in context of other rect terms
 			result = result * -1;
		return result;
	}

	public static int getGlobalIndexUnits(double src, long rootCellUnits)
	{
		long value = (long)Math.abs(src);
		long result = value / rootCellUnits;
		if (result > Integer.MAX_VALUE)
			throw new IllegalArgumentException("factor exceeds integer MAX");
 		if (src < 0.0d) //FIXME: may need to accommodate if src == 0 in context of other rect terms
 			result = result * -1;
		return (int)result;
	}
}
