package org.cloudgraph.examples.spatial.repo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudgraph.examples.spatial.model.EpsgCoordinatereferencesystem;
import org.cloudgraph.examples.spatial.model.EpsgCoordinatesystem;
import org.cloudgraph.examples.spatial.model.EpsgCoordoperation;
import org.cloudgraph.examples.spatial.model.EpsgCoordoperationparamvalue;
import org.cloudgraph.examples.spatial.model.query.QEpsgCoordinatereferencesystem;
import org.cloudgraph.examples.spatial.model.query.QEpsgCoordinatesystem;
import org.cloudgraph.examples.spatial.model.query.QEpsgCoordoperation;
import org.cloudgraph.examples.spatial.model.query.QEpsgCoordoperationparamvalue;
import org.plasma.query.Query;
import org.plasma.sdo.access.client.DefaultServiceContext;
import org.plasma.sdo.access.client.JDBCPojoDataAccessClient;
import org.plasma.sdo.access.client.SDODataAccessClient;
import org.plasma.sdo.helper.PlasmaXMLHelper;
import org.plasma.sdo.xml.DefaultOptions;

import commonj.sdo.DataGraph;
import commonj.sdo.helper.XMLDocument;

/**
  *   
 * java -jar target/spatial-repo-0.5.2/spatial-repo-0.5.2.jar -file espg.xml -size 10000 -export all
 */
public class RDBStandaloneExport {
    protected SDODataAccessClient rdbService;
    private static String ARG_BATCH_SIZE = "-size";
    private static String ARG_BATCH_EXPORT = "-export";
    private static String ARG_BATCH_FILE = "-file";
    private String file;
    private OutputStream outputStream;
    private enum Export {
    	all
    }
    private Export toExport = Export.all;
	
    private static Log log = LogFactory.getLog(RDBStandaloneExport.class);
    private int incr = 1000;

    private RDBStandaloneExport(Map<String, String> args) throws IOException {
     	this.rdbService = new SDODataAccessClient(new JDBCPojoDataAccessClient(new DefaultServiceContext()));
    	
    	String value = args.get(ARG_BATCH_SIZE);
    	if (value != null)
    		incr = Integer.valueOf(value).intValue();
    	value = args.get(ARG_BATCH_FILE);
    	if (value != null) {
    		file = value;
    		this.outputStream = new FileOutputStream(new File(".", file));
    	}
    	else {
    		printUsage();
    		return;
    	}
    	value = args.get(ARG_BATCH_EXPORT);
    	if (value != null) {
    		try {
    		    toExport = Export.valueOf(value);
    		}
    		catch (IllegalArgumentException e) {
        		printUsage();
        		return;
    		}
    	}
    	
    	switch (toExport) {

    	case all:
    		loadCoordinateReferenceSystems();
    		loadCoordinateOperations();
    		loadCoordOperationParamValues();
     		break;
    	}
    	
    	this.outputStream.close();
    }
    
    public static void main(String[] args) throws IOException {
    	Map<String, String> map = new HashMap<String, String>();
    	try {
	    	for (int i = 0; i < args.length; i+=2) {
	    		map.put(args[i], args[i+1]);
	    	}
    	}
    	catch (ArrayIndexOutOfBoundsException e) {
    		printUsage();
    		return;
    	}
    	new RDBStandaloneExport(map);
    }
    
    private static void printUsage() {
    	StringBuilder buf = new StringBuilder();
    	for (int i = 0; i < Export.values().length; i++)
    	{
    		if (i > 0)
    			buf.append(" | ");
    		buf.append(Export.values()[i]);
    	}
    	
    	System.out.println("--------------------------------------------------------------------------");
    	System.out.println("java -jar spatial-repo-0.5.2.jar [-size record-chunk-size] [-file export-file-name] [-export "+buf.toString()+"]");
    	System.out.println("--------------------------------------------------------------------------");
    	System.out.println("examples:");
     	System.out.println("java spatial-repo-0.5.2.jar -size 10000 -file name");
    	System.out.println("--------------------------------------------------------------------------");
    }
 
  
	public void loadCoordinateReferenceSystems() throws IOException {
		QEpsgCoordinatereferencesystem query = QEpsgCoordinatereferencesystem.newQuery();
		query.select(query.wildcard())
		  .select(query.datum().wildcard())
		  .select(query.datum().fkPrimeMeridianCode().wildcard())
		  .select(query.datum().fkPrimeMeridianCode().fkUomCode().wildcard())
		  .select(query.datum().fkEllipsoidCode().wildcard())
		  .select(query.datum().fkEllipsoidCode().fkUomCode4().wildcard())
		  .select(query.datum().fkAreaOfUseCode2().wildcard())
		  .select(query.areaOfUse().wildcard())
		  .select(query.coordSys().wildcard())
		  .select(query.cmpdVertCrs().wildcard())
		  .select(query.cmpdHorizCrs().wildcard())
		  .select(query.sourceGeogCrs().wildcard())
		  .select(query.epsgCoordoperation().wildcard())
		  .select(query.epsgCoordoperation().fkCoordOpMethodCode().epsgCoordoperationparamusage().wildcard())
		  .select(query.epsgCoordoperation().fkCoordOpMethodCode().epsgCoordoperationparamusage().fkParameterCode().wildcard())
		;
		load(query, EpsgCoordinatereferencesystem.class);
	}	
	
	public void loadCoordinateOperations() throws IOException {
		QEpsgCoordoperation query = QEpsgCoordoperation.newQuery();
		query.select(query.wildcard())
		  .select(query.fkCoordOpMethodCode().wildcard())
		  .select(query.fkCoordOpMethodCode().epsgCoordoperationparamusage().wildcard())
		  .select(query.fkCoordOpMethodCode().epsgCoordoperationparamusage().fkParameterCode().wildcard())
		  //.select(query.epsgCoordoperationmethod().epsgCoordoperationparamusage().epsgCoordoperationparamvalue().wildcard())
		  //.select(query.epsgCoordoperationparamvalue().wildcard())
		  .select(query.epsgCoordoperationpath().wildcard())
		  .select(query.fkAreaOfUseCode3().wildcard())
		  .select(query.fkSourceCrsCode().coordRefSysCode())
		  .select(query.fkTargetCrsCode().coordRefSysCode())
		;
		load(query, EpsgCoordoperation.class);
	}	
	
	public void loadCoordOperationParamValues() throws IOException {
		QEpsgCoordoperationparamvalue query = QEpsgCoordoperationparamvalue.newQuery();
		query.select(query.wildcard())
		  .select(query.fkUomCode3().wildcard())
		;
		load(query, EpsgCoordoperationparamvalue.class);
	}	
 		
    private void load(Query query, Class clss) throws IOException
    {
		for (int start = 1;; start += incr) {
			log.info("fetching " + start + " to " + (start + (incr-1)) + " " + clss.getSimpleName());
			query.setStartRange(start);
			query.setEndRange(start + (incr-1));
			DataGraph[] graphs = this.rdbService.find(query);
			log.info("found " + graphs.length + " results " + clss.getSimpleName());
			if (graphs.length > 0) {
				for (DataGraph graph : graphs) {
					 byte[] graphBytes = serializeGraph(graph);
					 this.outputStream.write(graphBytes);
					 this.outputStream.write("\n".getBytes());
					 this.outputStream.flush();
				}
			}
			else
				break;
		}    	
    }
    
    protected byte[] serializeGraph(DataGraph graph) throws IOException
    {
        DefaultOptions options = new DefaultOptions(
        		graph.getRootObject().getType().getURI());
        options.setRootNamespacePrefix("ns1");
        options.setPrettyPrint(true); // single line per graph       
        XMLDocument doc = PlasmaXMLHelper.INSTANCE.createDocument(graph.getRootObject(), 
        		graph.getRootObject().getType().getURI(), 
        		null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
	    PlasmaXMLHelper.INSTANCE.save(doc, os, options);        
        os.flush();
        os.close(); 
        return os.toByteArray();
    }

}
