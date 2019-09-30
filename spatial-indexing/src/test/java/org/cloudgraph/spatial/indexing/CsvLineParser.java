package org.cloudgraph.spatial.indexing;

import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.common.collect.Iterables;

class CsvLineParser {

	private final CSVFormat csvFormat;

	CsvLineParser(char fieldDelimiter) {
		this.csvFormat = CSVFormat.newFormat(fieldDelimiter);
	}

	public CSVRecord parse(String input) throws IOException {
		CSVParser csvParser = new CSVParser(new StringReader(input),
				csvFormat);
		return Iterables.getFirst(csvParser, null);
	}
}