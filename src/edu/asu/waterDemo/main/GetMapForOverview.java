package edu.asu.waterDemo.main;

import java.io.File;
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

import edu.asu.waterDemo.commonclasses.GenerateTiles;
import edu.asu.waterDemo.commonclasses.LatLng;
import edu.asu.waterDemo.commonclasses.LoadTiffThread;
import edu.asu.waterDemo.commonclasses.TiffParser;
import edu.asu.waterDemo.main.DrawFuzzyThresholdsMap.DrawFuzzyThreads;

@Path("/getMapData")
public class GetMapForOverview {
	private String basisDir;
	private String targetPath;
	private int NUMBER_OF_PROCESSORS = 30;
	private String metricDir;
	
	public class GetMapForOverviewBean{
		public String imgStr;
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
	
	public class DrawOverviewMapThreads implements Runnable{
		private int startIndex;
		private int endIndex;
		private TiffParser parser;
		private double[] thresholds;
		private String[] tfFunction;
		private GenerateTiles tile;
		
		public DrawOverviewMapThreads(int startIndex, int endIndex, TiffParser Parser, double[] thresholds, String[] tfFucntion, GenerateTiles tile){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.parser = Parser;
			this.thresholds = thresholds;
			this.tfFunction = tfFucntion;
			this.tile = tile;
		}
		
		@Override
		public void run() {
			for(int index=this.startIndex; index<this.endIndex; index++){
				int h = index/(int)this.parser.getSize()[1];
				int w = index%(int)this.parser.getSize()[1];
				double lat = this.parser.getUlLatlng()[0] + h*this.parser.getGeoInfo()[5];
				double lng = this.parser.getUlLatlng()[1] + w*this.parser.getGeoInfo()[1];
				double value = this.parser.getData()[index];
				try {
					this.tile.drawTiles(value, this.thresholds, this.tfFunction, lat, lng);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public GetMapForOverviewBean query(
			@QueryParam("metricType") @DefaultValue("null") String metricType,
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("modalType") @DefaultValue("null") String modalType,
			@QueryParam("key") @DefaultValue("null") String key,
			@QueryParam("zoomLevel") @DefaultValue("null") String zoomLevel,
			@QueryParam("colorTable") @DefaultValue("null") String colorTable,
			@QueryParam("thresholds") @DefaultValue("null") String thresholds,
			@QueryParam("min") @DefaultValue("null") String min,
			@QueryParam("max") @DefaultValue("null") String max,
			@QueryParam("tfType") @DefaultValue("null") String tfType,
			@QueryParam("variable") @DefaultValue("null") String variable){
		GetMapForOverviewBean result = new GetMapForOverviewBean();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		this.metricDir = this.basisDir + _dataType + "/" + modalType + metricType + File.separatorChar;
		if(dataType.equalsIgnoreCase("Ensemble")){
			String _variable = variable;
			if(variable.equals("Precipitation"))
				_variable = "pr_HIST";
			if(variable.equals("TemperatureMin"))
				_variable = "tasmin_HIST";
			if(variable.equals("TemperatureMax"))
				_variable = "tasmax_HIST";
			this.metricDir = this.basisDir + _variable + "/EnsembleStatOfTime" + metricType + "/";
			this.targetPath = this.metricDir + "EnsembleMeanOfTime" + metricType + ".tif";
		}
		else{
			this.targetPath = getAllFiles(this.metricDir, key);
		}
			
		
		String imgPath = this.targetPath.replace(".tif", "_zoomLevel"+ zoomLevel +".png");
		
		
		TiffParser targetparser = new TiffParser(this.targetPath);
		String[] colortf = colorTable.split("&");
		
		GenerateTiles tile = new GenerateTiles(imgPath, null, "overviewVis", Integer.valueOf(zoomLevel), colortf);
		double[] size = targetparser.getSize();
		LatLng southwest = new LatLng(targetparser.getLrLatlng()[0], targetparser.getUlLatlng()[1]);
		LatLng northeast = new LatLng(targetparser.getUlLatlng()[0], targetparser.getLrLatlng()[1]);
		tile.processWidthHeight((int) size[1], (int) size[0], southwest, northeast);
		tile.initializeBufferImage();
		File imgFile = new File(imgPath);
		// look for base64 img string
		// if the img already exists, then convert it to base64 and return the string
		if(imgFile.exists()){
			result.imgStr = tile.encodeFromReaderToBase64(imgPath, "PNG");
			System.out.println("Use existed image!");
			return result;
		}
		
		String sourceDir = this.basisDir + _dataType + "/" + modalType + metricType + File.separatorChar;
		double[] globalMinmax = {999999999,0};
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
		
		
		
		double[] _thresholds = new double[colortf.length-1];
		for(int i=0; i<colortf.length-1; i++){
			_thresholds[i] = (i+1)/(double)colortf.length*(globalMinmax[1]-globalMinmax[0])+globalMinmax[0];
		}	
		
		double[] sSize = targetparser.getSize();
		int tgtHeight = (int)sSize[0];
		int tgtWidth = (int)sSize[1];
		DrawOverviewMapThreads[] drawOverviewMapService = new DrawOverviewMapThreads[NUMBER_OF_PROCESSORS];
		Thread[]  drawOverviewMapThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			drawOverviewMapService[i] = new DrawOverviewMapThreads(startIndex, endIndex, targetparser, _thresholds, colortf, tile);
			drawOverviewMapThread[i] = new Thread(drawOverviewMapService[i]);
			drawOverviewMapThread[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				drawOverviewMapThread[i].join();
//				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		result.imgStr = tile.writeBufferImage();
		
		return result;
	}
	
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
}
