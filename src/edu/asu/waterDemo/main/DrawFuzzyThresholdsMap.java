package edu.asu.waterDemo.main;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

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
import edu.asu.waterDemo.commonclasses.TiffParser;


@Path("/drawFuzzyThresholdMap")
public class DrawFuzzyThresholdsMap {
	private String basisDir;
	private String targetDir;
	private int NUMBER_OF_PROCESSORS = 30; 
	
	public class imgBase64{
		public String imgStr;
	}
	
	public class DrawFuzzyThreads implements Runnable{
		private int startIndex;
		private int endIndex;
		private TiffParser parser;
		private double[] thresholds;
		private String[] tfFunction;
		private GenerateTiles tile;
		
		public DrawFuzzyThreads(int startIndex, int endIndex, TiffParser Parser, double[] thresholds, String[] tfFucntion, GenerateTiles tile){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.parser = Parser;
			this.thresholds = thresholds;
			this.tfFunction = tfFucntion;
			this.tile = tile;
		}
		
		@Override
		public void run() {
			for(int index=startIndex; index<endIndex; index++){
				int h = index/(int)this.parser.getSize()[1];
				int w = index%(int)this.parser.getSize()[1];
				double lat = this.parser.getUlLatlng()[0] + h*this.parser.getGeoInfo()[5];
				double lng = this.parser.getUlLatlng()[1] + w*this.parser.getGeoInfo()[1];
				double value = this.parser.getData()[index];
				try {
					tile.drawTiles(value, this.thresholds, this.tfFunction, lat, lng);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
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
	public imgBase64 query(
			@QueryParam("modaltype") @DefaultValue("null") String modaltype,
			@QueryParam("keywords") @DefaultValue("null") String keywords,
			@QueryParam("metricstype") @DefaultValue("null") String metricstype,
			@QueryParam("thresholds") @DefaultValue("null") String thresholds,
			@QueryParam("tfFunction") @DefaultValue("null") String tfFunction,
			@QueryParam("zoomLevel") @DefaultValue("7") int zoomLevel,
			@QueryParam("dataType") @DefaultValue("") String dataType){
		imgBase64 result = new imgBase64();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		this.targetDir = getAllFiles(this.basisDir + _dataType + "/" + modaltype + File.separatorChar, keywords);
		TiffParser targetParser = new TiffParser(this.targetDir);
		String[] thresholdStr=  thresholds.split("&");
		double[] thresholdArr = new double[thresholdStr.length];
		for (int i = 0; i<thresholdStr.length; i++) {
			thresholdArr[i] = Double.valueOf(thresholdStr[i]);
		}
		String[] tfFunctionStr = tfFunction.split("&");
		
		Point2D mapPixelOriginPt = new Point2D.Double();
		mapPixelOriginPt.setLocation(0, 0);
		GenerateTiles tile = new GenerateTiles("", null, "fuzzyThresholdVis", zoomLevel, tfFunctionStr);
		double[] size = targetParser.getSize();
		LatLng southwest = new LatLng(targetParser.getLrLatlng()[0], targetParser.getUlLatlng()[1]);
		LatLng northeast = new LatLng(targetParser.getUlLatlng()[0], targetParser.getLrLatlng()[1]);
		tile.processWidthHeight((int) size[1], (int) size[0], southwest, northeast);
		tile.initializeBufferImage();
		
		double[] sSize = targetParser.getSize();
		int tgtHeight = (int)sSize[0];
		int tgtWidth = (int)sSize[1];
		DrawFuzzyThreads[] drawTileService = new DrawFuzzyThreads[NUMBER_OF_PROCESSORS];
		Thread[]  drawTileThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			drawTileService[i] = new DrawFuzzyThreads(startIndex, endIndex, targetParser, thresholdArr, tfFunctionStr, tile);
			drawTileThread[i] = new Thread(drawTileService[i]);
			drawTileThread[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				drawTileThread[i].join();
//				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		result.imgStr = tile.encodeFromBufferImgToBase64();
		
		
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
}
