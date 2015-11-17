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
			@QueryParam("binSize") @DefaultValue("") String binSize,
			@QueryParam("variable") @DefaultValue("null") String variable) throws IOException{
		HistDataBean result = new HistDataBean();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		this.metricDir = this.basisDir + _dataType + "/" + type + metricType + File.separatorChar;
		if(dataType.equalsIgnoreCase("Ensemble")){
			String _variable = "";
			if(variable.equals("Precipitation"))
				_variable = "pr_HIST";
			if(variable.equals("TemperatureMin"))
				_variable = "tasmin_HIST";
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
			    double min = 99999999;
			    double max = 0;
			    for(int i=0; i<everything.length; i++){
			    	hist[i] = Double.valueOf(everything[i]);
			    	if(max<Double.valueOf(everything[i]))
			    		max = Double.valueOf(everything[i]);
			    	if(min>Double.valueOf(everything[i]))
			    		min = Double.valueOf(everything[i]);
			    }
			    result.hist = hist;
				result.metric = metricType;
				result.data = dataType;
				result.min = min;
				result.max = max;
			} finally {
			    br.close();
			}
			return result;
		}
		
		TiffParser targetparser = new TiffParser(this.targetFile);
//		double[] MinMax = targetparser.getMinmax();
		double[] globalMinmax = new double[2];
		if(dataType.equalsIgnoreCase("Ensemble")){
			globalMinmax = targetparser.getMinmax();
		}else{
			ArrayList<File> files = new ArrayList<File>();
			files = getAllFiles(this.metricDir, files);
			for(File each : files){
				String filepath = each.getAbsolutePath();
				TiffParser eachparser = new TiffParser(filepath);
				double[] minmax = eachparser.getMinmax();
				if(globalMinmax[0] > minmax[0])
					globalMinmax[0] = minmax[0];
				if(globalMinmax[1] < minmax[1])
					globalMinmax[1] = minmax[1];
			}
		}
		
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
