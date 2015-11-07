package edu.asu.waterDemo.main;

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
			@QueryParam("thresholds") @DefaultValue("null") String thresholds){
		GetMapForOverviewBean result = new GetMapForOverviewBean();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		this.metricDir = this.basisDir + _dataType + "/" + modalType + metricType + File.separatorChar;
		this.targetPath = getAllFiles(this.metricDir, key);
		String imgPath = this.targetPath.replace(".tif", "_zoomLevel"+ zoomLevel +".png");
		
		// look for base64 img string
		
		String[] colortf = colorTable.split("&");
		TiffParser targetparser = new TiffParser(this.targetPath);
		double[] minmax = new double[2];
		targetparser.GetMinMax(minmax);
		double[] _thresholds = new double[colortf.length-1];
		for(int i=0; i<colortf.length-1; i++){
			_thresholds[i] = (i+1)/(double)colortf.length*(minmax[1]-minmax[0])+minmax[0];
		}
		
		GenerateTiles tile = new GenerateTiles(imgPath, null, "overviewVis", Integer.valueOf(zoomLevel), colortf);
		double[] size = targetparser.getSize();
		LatLng southwest = new LatLng(targetparser.getLrLatlng()[0], targetparser.getUlLatlng()[1]);
		LatLng northeast = new LatLng(targetparser.getUlLatlng()[0], targetparser.getLrLatlng()[1]);
		tile.processWidthHeight((int) size[1], (int) size[0], southwest, northeast);
		tile.initializeBufferImage();
		File imgFile = new File(imgPath);
		// if the img already exists, then convert it to base64 and return the string
		if(imgFile.exists()){
			result.imgStr = tile.encodeFromReaderToBase64(imgPath, "PNG");
			return result;
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