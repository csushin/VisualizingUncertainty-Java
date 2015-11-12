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
import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getMapData2MultiEnsemble")
public class GetMapData2MultiEnsemble {
	private String basisDir;
	private String targetPath;
	private int NUMBER_OF_PROCESSORS = 30;
	private String xmetricDir;
	private String ymetricDir;
	
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
		private double[] xthresholds;
		private double[] ythresholds;
		private String[] tfFunction;
		private GenerateTiles tile;
		
		public DrawOverviewMapThreads(int startIndex, int endIndex, TiffParser Parser, double[] xthresholds, double[] ythresholds, String[] tfFucntion, GenerateTiles tile){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.parser = Parser;
			this.xthresholds = xthresholds;
			this.ythresholds = ythresholds;
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
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("zoomLevel") @DefaultValue("null") String zoomLevel,
			@QueryParam("colorTable") @DefaultValue("null") String colorTable,
			@QueryParam("xthresholds") @DefaultValue("null") String xthresholds,
			@QueryParam("ythresholds") @DefaultValue("null") String ythresholds,
			@QueryParam("xmetric") @DefaultValue("null") String xmetric,
			@QueryParam("ymetricA") @DefaultValue("null") String ymetricA,
			@QueryParam("ymetricB") @DefaultValue("null") String ymetricB,
			@QueryParam("tfType") @DefaultValue("null") String tfType){
		GetMapForOverviewBean result = new GetMapForOverviewBean();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		this.xmetricDir = this.basisDir + _dataType + "/GlobleStat/" + xmetric + ".tif";
		this.ymetricDir = this.basisDir + _dataType + "/EnsembleStatOf" + ymetricB + "/" + ymetricA + "Of" + ymetricB + ".tif";
		this.targetPath = this.basisDir + _dataType + "/Ensemble2Models/" + xmetric + "_" + ymetricA + "Of" + ymetricB + "_zoomLevel" + zoomLevel + ".tif";
		String imgPath = this.targetPath.replace(".tif", "_zoomLevel"+ zoomLevel +".png");
		
		TiffParser xparser = new TiffParser(this.xmetricDir);
		TiffParser yparser = new TiffParser(this.ymetricDir);
		String[] colortf = colorTable.split("&");
		
		GenerateTiles tile = new GenerateTiles(imgPath, null, "overviewVis", Integer.valueOf(zoomLevel), colortf);
		double[] size = xparser.getSize();
		LatLng southwest = new LatLng(xparser.getLrLatlng()[0], xparser.getUlLatlng()[1]);
		LatLng northeast = new LatLng(xparser.getUlLatlng()[0], xparser.getLrLatlng()[1]);
		tile.processWidthHeight((int) size[1], (int) size[0], southwest, northeast);
		tile.initializeBufferImage();
		File imgFile = new File(imgPath);
		// look for base64 img string
		// if the img already exists, then convert it to base64 and return the string
		if(imgFile.exists()){
			result.imgStr = tile.encodeFromReaderToBase64(imgPath, "PNG");
			return result;
		}
		
		double[] xMinmax = xparser.getMinmax();
		double[] yMinmax = yparser.getMinmax();
		
		int tgtHeight = (int)size[0];
		int tgtWidth = (int)size[1];
		DrawOverviewMapThreads[] drawOverviewMapService = new DrawOverviewMapThreads[NUMBER_OF_PROCESSORS];
		Thread[]  drawOverviewMapThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			drawOverviewMapService[i] = new DrawOverviewMapThreads(startIndex, endIndex, xparser, yparser, xthreshold, ythreshold, colortf, tile);
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
		
		return result;
	}
}
