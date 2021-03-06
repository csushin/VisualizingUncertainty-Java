package edu.asu.waterDemo.main;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.GetHistDataThread;
import edu.asu.waterDemo.commonclasses.LoadTiffThread;
import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getHistData")
public class GetHistDataForOverview {
	private String basisDir;
	private int NUMBER_OF_PROCESSORS = 30;
	private String metricDir;
	private String targetFile;
	
	public class HistDataBean{
		public double[] hist;
		public String metric;
		public String data;
		public double min;
		public double max;
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
	public HistDataBean query(
			@QueryParam("metricType") @DefaultValue("null") String metricType,
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("type") @DefaultValue("null") String type,
			@QueryParam("key") @DefaultValue("null") String key,
			@QueryParam("binSize") @DefaultValue("null") String binSize,
			@QueryParam("variable") @DefaultValue("null") String variable,
			@QueryParam("min") @DefaultValue("null") String min,
			@QueryParam("max") @DefaultValue("null") String max) throws IOException{
		HistDataBean result = new HistDataBean();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		this.metricDir = this.basisDir + _dataType + "/" + type + metricType + File.separatorChar;
		if(dataType.equalsIgnoreCase("Ensemble")){
			String _variable = variable;
			if(variable.equals("Precipitation"))
				_variable = "pr_HIST";
			if(variable.equals("TemperatureMin"))
				_variable = "tasmin_HIST";
			if(variable.equals("TemperatureMax"))
				_variable = "tasmax_HIST";
			this.metricDir = this.basisDir + _variable + "/EnsembleStatOfTimeMean/";
			this.targetFile = this.metricDir + "Ensemble" + metricType + "OfTimeMean.tif";
		}
		else{
			this.targetFile = getAllFiles(this.metricDir, key);
		}
		
		if(metricType.contains("Area")){
			if(dataType.equalsIgnoreCase("Ensemble"))
				this.targetFile = this.metricDir + "Ensemble" + metricType + "OfTimeMean.tif";
			else
				this.targetFile = getAllFilesForArea(this.metricDir, key);
			BufferedReader br = new BufferedReader(new FileReader(this.targetFile));
			try {
			    StringBuilder sb = new StringBuilder();
			    String line = br.readLine();

			    while (line != null) {
			        sb.append(line);
			        sb.append(System.lineSeparator());
			        line = br.readLine();
			    }
			    String[] everything = sb.toString().split(" ");
			    double[] hist = new double[everything.length];
			    double areamin = 99999999;
			    double areamax = 0;
			    for(int i=0; i<everything.length; i++){
			    	hist[i] = Double.valueOf(everything[i]);
			    	if(areamax<Double.valueOf(everything[i]))
			    		areamax = Double.valueOf(everything[i]);
			    	if(areamin>Double.valueOf(everything[i]))
			    		areamin = Double.valueOf(everything[i]);
			    }
			    result.hist = hist;
				result.metric = metricType;
				result.data = dataType;
				result.min = areamin;
				result.max = areamax;
			} finally {
			    br.close();
			}
			return result;
		}
		
		String sourceDir = this.basisDir + _dataType + "/" + type + metricType + File.separatorChar;
		TiffParser targetparser = new TiffParser(this.targetFile);
//		double[] MinMax = targetparser.getMinmax();
		double[] globalMinmax = {999999999, 0};
		if(min!=null && max!=null && !dataType.contains("Ensemble")){
			globalMinmax[0] = Double.valueOf(min);
			globalMinmax[1] = Double.valueOf(max);
		}
		else{
			globalMinmax = targetparser.getMinmax();
		}
//		if(dataType.equalsIgnoreCase("Ensemble")){
//			globalMinmax = targetparser.getMinmax();
//		}else{
//			ArrayList<File> files = new ArrayList<File>();
//			files = getAllFiles(sourceDir, files);
//			ArrayList<TiffParser> parsers = new ArrayList<TiffParser>();
//			parsers = parseFilesThread(files, parsers);
//			for(TiffParser each : parsers){
//				double[] minmax = each.getMinmax();
//				if(globalMinmax[0] > minmax[0])
//					globalMinmax[0] = minmax[0];
//				if(globalMinmax[1] < minmax[1])
//					globalMinmax[1] = minmax[1];
//			}
//		}
		
		double[] histData = new double[Integer.valueOf(binSize)];
		
		double[] sSize = targetparser.getSize();
		int tgtHeight = (int)sSize[0];
		int tgtWidth = (int)sSize[1];
		GetHistDataThread[] getHistService = new GetHistDataThread[NUMBER_OF_PROCESSORS];
		Thread[]  getHistThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		ArrayList<double[]> data = new ArrayList<>();
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			double[] eachHistData = new double[Integer.valueOf(binSize)];
			data.add(eachHistData);
			getHistService[i] = new GetHistDataThread(targetparser, eachHistData, globalMinmax, startIndex, endIndex, Integer.valueOf(binSize));
			getHistThread[i] = new Thread(getHistService[i]);
			getHistThread[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				getHistThread[i].join();
				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			for(int j=0; j<Integer.valueOf(binSize); j++){
				histData[j] += data.get(i)[j];
			}
		}
		result.hist = histData;
		result.metric = metricType;
		result.data = dataType;
		result.min = globalMinmax[0];
		result.max = globalMinmax[1];
		return result;
	}
	
	// module for parsing tiff files
	public ArrayList<TiffParser> parseFilesThread(ArrayList<File> files, ArrayList<TiffParser> parsers){
			LoadTiffThread[] service = new LoadTiffThread[files.size()];
			Thread[] serverThread = new Thread[files.size()];
			for(int i=0; i<files.size(); i++){
				String filePath = files.get(i).getAbsolutePath();
				service[i] = new LoadTiffThread(filePath);
				serverThread[i] = new Thread(service[i]);
				serverThread[i].start();
			}
			
			try {
				for(int i=0; i<files.size(); i++){
					serverThread[i].join();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for(int i=0; i<files.size(); i++){
				parsers.add(service[i].getResult());
			}
			return parsers;
	}
	
	private String getAllFiles(String directoryName, String keyword) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile() && file.getName().endsWith(".tif") && file.getName().contains(keyword)) {
	        	return file.getAbsolutePath();
	        } 
	    }
	    return null;
	}
	
	public ArrayList<File> getAllFiles(String directoryName, ArrayList<File> files) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	    	String name = file.getName();
	        if (file.isFile() && name.endsWith(".tif") && !name.contains("MPI-ESM-LR_CCLM") && !name.contains("HadGEM2-ES_CCLM") && !name.contains("EC-EARTH-r12_CCLM")
					&& !name.contains("CNRM-CM5_CCLM") && !name.contains("EC-EARTH-r3_HIRHAM")) {
	            files.add(file);
	        } else if (file.isDirectory()) {
	        	getAllFiles(file.getAbsolutePath(), files);
	        }
	    }
	    return files;
	}
	
	private String getAllFilesForArea(String directoryName, String keyword) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile() && file.getName().endsWith(".txt") && file.getName().contains(keyword)) {
	        	return file.getAbsolutePath();
	        } 
	    }
	    return null;
	}
}
