package edu.asu.waterDemo.HierarchicalCluster;

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

@Path("/drawHCTreeBalanceMap")
public class DrawMapOfHCTreeBalance {
	public class DrawMapOfHCTreeBalanceBean{
		public String imgStr;
	}

	private String basisDir;
	
//	duplicate code in many classes.
	public class DrawMapOfHCTreeBalanceThread implements Runnable{
		private int startIndex;
		private int endIndex;
		private TiffParser parser;
		private double[] thresholds;
		private String[] tfFunction;
		private GenerateTiles tile;
		
		public DrawMapOfHCTreeBalanceThread(int startIndex, int endIndex, TiffParser Parser, double[] thresholds, String[] tfFucntion, GenerateTiles tile){
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
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")) {
			this.basisDir = context.getRealPath("img") + File.separatorChar;
		}else{
			this.basisDir = "/work/asu/data/CalculationResults/pr_HIST/SimilarityResults/HierarchicalClst/TotalDistance/";
		}
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public DrawMapOfHCTreeBalanceBean query(
			@QueryParam("zoomLevel") @DefaultValue("null") String zoomLevel,
			@QueryParam("colorTable") @DefaultValue("null") String colorTable){
		DrawMapOfHCTreeBalanceBean result = new DrawMapOfHCTreeBalanceBean();
		String targetPath = this.basisDir + "Result.tif";
		TiffParser parser = new TiffParser(targetPath);
		double max = parser.getMinmax()[1];
		double min = parser.getMinmax()[0];
		String imgPath = this.basisDir + "Result_zoomLevel"+ zoomLevel +".png";
		String[] colortf = colorTable.split("&");
		GenerateTiles tile = new GenerateTiles(imgPath, null, "overviewVis", Integer.valueOf(zoomLevel), colortf);
		double[] size = parser.getSize();
		LatLng southwest = new LatLng(parser.getLrLatlng()[0], parser.getUlLatlng()[1]);
		LatLng northeast = new LatLng(parser.getUlLatlng()[0], parser.getLrLatlng()[1]);
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
		// Define the threshold according to the tranfer function and MinMax data value uniformly
		double[] _thresholds = new double[colortf.length-1];
		for(int i=0; i<colortf.length-1; i++){
			_thresholds[i] = (i+1)/(double)colortf.length*(max - min)+min;
		}
		int tgtHeight = (int)size[0];
		int tgtWidth = (int)size[1];
		int NUMBER_OF_PROCESSORS = 16;
		DrawMapOfHCTreeBalanceThread[] drawOverviewMapService = new DrawMapOfHCTreeBalanceThread[NUMBER_OF_PROCESSORS];
		Thread[]  drawOverviewMapThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			drawOverviewMapService[i] = new DrawMapOfHCTreeBalanceThread(startIndex, endIndex, parser, _thresholds, colortf, tile);
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
}
