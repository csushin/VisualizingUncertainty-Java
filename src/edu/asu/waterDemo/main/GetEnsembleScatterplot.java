package edu.asu.waterDemo.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;


@Path("/getEnsembleScatterplot")
public class GetEnsembleScatterplot {
	private String basisDir;
	
	public class GetEnsembleScatterplotBean{
		public double[] xMinmax;
		public double[] yMinmax;
		public String[] histData;
	}
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")) {
			this.basisDir = context.getRealPath("img") + File.separatorChar;
		}else{
			this.basisDir = "/work/asu/data/CalculationResults" + File.separatorChar;
		}
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public GetEnsembleScatterplotBean query(
			@QueryParam("xmetric") @DefaultValue("null") String xmetric,
			@QueryParam("ymetric") @DefaultValue("null") String ymetric,
			@QueryParam("dataType") @DefaultValue("null") String dataType) throws IOException{
		GetEnsembleScatterplotBean result = new GetEnsembleScatterplotBean();
		ArrayList<File> files = new ArrayList<File>();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		String basisDir =  this.basisDir + _dataType + "/SpatialStat/";
		files = getAllFiles(basisDir, files);
		String[] histData = new String[files.size()];
		double[] xMinmax = {999999999, 0};
		double[] yMinmax = {999999999, 0};
		for(File eachfile : files){
			BufferedReader in = new BufferedReader(new FileReader(eachfile.getAbsolutePath()));
			String line;
			String key = "";
			String[] xANDy = new String[2];
			while((line = in.readLine()) != null)
			{
			    String[] eachword = line.split(" ");
			    for(String eachstr : eachword){
			    	if(eachstr.equals(xmetric)){
			    		xANDy[0] = eachword[2];
			    		if(xMinmax[0] > Double.valueOf(xANDy[0]))
			    			xMinmax[0] = Double.valueOf(xANDy[0]);
			    		if(xMinmax[1] < Double.valueOf(xANDy[0]))
			    			xMinmax[1] = Double.valueOf(xANDy[0]);
			    	}
			    	if(eachstr.equals(ymetric)){
			    		xANDy[1] = eachword[2];
			    		if(yMinmax[0] > Double.valueOf(xANDy[1]))
			    			yMinmax[0] = Double.valueOf(xANDy[1]);
			    		if(yMinmax[1] < Double.valueOf(xANDy[1]))
			    			yMinmax[1] = Double.valueOf(xANDy[1]);
			    	}
			    	if(eachstr.contains("_"))
			    		key = eachstr.split("_")[1] + "_" +eachstr.split("_")[2];
			    }
			}
			histData[files.indexOf(eachfile)] = xANDy[0] + "," + xANDy[1] + "," + key;
			in.close();
		}
		result.histData = histData;
		result.xMinmax = xMinmax;
		result.yMinmax = yMinmax;
		
		return result;
	}
	
	public ArrayList<File> getAllFiles(String directoryName, ArrayList<File> files) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	    	String name = file.getName();
	        if (file.isFile() &&  !name.contains("MPI-ESM-LR_CCLM") && !name.contains("HadGEM2-ES_CCLM") && !name.contains("EC-EARTH-r12_CCLM")
					&& !name.contains("CNRM-CM5_CCLM") && !name.contains("EC-EARTH-r3_HIRHAM")) {
	            files.add(file);
	        } else if (file.isDirectory()) {
	        	getAllFiles(file.getAbsolutePath(), files);
	        }
	    }
	    return files;
	}
}
