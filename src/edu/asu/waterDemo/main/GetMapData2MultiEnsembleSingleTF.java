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

@Path("/getMapData2MultiEnsembleSingleTF")
public class GetMapData2MultiEnsembleSingleTF {
	private String basisDir;
	private String targetPath;
	private int NUMBER_OF_PROCESSORS = 16;
	private String metricDir;
	
	public class GetMapData2MultiEnsembleSingleTFBean{
		public String imgStr;
		public double[] minmax;
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
		private String scale;
		
		public DrawOverviewMapThreads(String scale, int startIndex, int endIndex, TiffParser Parser, double[] thresholds, String[] tfFucntion, GenerateTiles tile){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.parser = Parser;
			this.thresholds = thresholds;
			this.tfFunction = tfFucntion;
			this.tile = tile;
			this.scale = scale;
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
					if(this.scale.equals("true")){
						if(!Double.isNaN(value) && value!=-1)
							value = Math.log(value);
					}
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
	public GetMapData2MultiEnsembleSingleTFBean query(
			@QueryParam("metricA") @DefaultValue("null") String metricA,
			@QueryParam("metricB") @DefaultValue("null") String metricB,
			@QueryParam("_metricA") @DefaultValue("null") String _metricA,
			@QueryParam("_metricB") @DefaultValue("null") String _metricB,
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("colorTable") @DefaultValue("null") String colorTable,
			@QueryParam("zoomLevel") @DefaultValue("null") String zoomLevel,
			@QueryParam("uniformRange") @DefaultValue("null") String uniformRange,
			@QueryParam("alpha") @DefaultValue("null") String alpha,
			@QueryParam("scale") @DefaultValue("null") String scale){
		GetMapData2MultiEnsembleSingleTFBean result = new GetMapData2MultiEnsembleSingleTFBean();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		this.targetPath = this.basisDir + _dataType + "/EnsembleStatOf" + metricB + "/" + metricA + "Of" + metricB + ".tif";
		String imgPath = this.targetPath.replace(".tif", "_zoomLevel"+ zoomLevel +".png");
		
		
		TiffParser targetparser = new TiffParser(this.targetPath);
		String[] colortf = colorTable.split("&");
		
		GenerateTiles tile = new GenerateTiles(imgPath, null, "EnsembleSingleTF", Integer.valueOf(zoomLevel), colortf);
		double[] size = targetparser.getSize();
		LatLng southwest = new LatLng(targetparser.getLrLatlng()[0], targetparser.getUlLatlng()[1]);
		LatLng northeast = new LatLng(targetparser.getUlLatlng()[0], targetparser.getLrLatlng()[1]);
		tile.processWidthHeight((int) size[1], (int) size[0], southwest, northeast);
		tile.initializeBufferImage();
		tile.setAlpha(Integer.valueOf(alpha));
		File imgFile = new File(imgPath);
		// look for base64 img string
		// if the img already exists, then convert it to base64 and return the string
//		if(imgFile.exists()){
//			result.imgStr = tile.encodeFromReaderToBase64(imgPath, "PNG");
//			return result;
//		}
		
		double[] globalMinmax = new double[2];
		globalMinmax = targetparser.getMinmax();
		if(Boolean.valueOf(uniformRange)){
			String _comparedFilePath = this.basisDir +  _dataType + "/EnsembleStatOf" + _metricB + "/" + _metricA + "Of" + _metricB + ".tif";
			TiffParser comparedParser = new TiffParser(_comparedFilePath);
			double[] comparedMinmax = comparedParser.getMinmax();
			if(globalMinmax[0] > comparedMinmax[0])
				globalMinmax[0] = comparedMinmax[0];
			if(globalMinmax[1] < comparedMinmax[1])
				globalMinmax[1] = comparedMinmax[1];
		}
		
		if(scale.equals("true")){
			globalMinmax[0] = Math.log(globalMinmax[0]);
			globalMinmax[1] = Math.log(globalMinmax[1]);
		}
		
		
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
			drawOverviewMapService[i] = new DrawOverviewMapThreads(scale, startIndex, endIndex, targetparser, _thresholds, colortf, tile);
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
		
		
		result.imgStr = tile.encodeFromBufferImgToBase64();
//		result.imgStr = tile.writeBufferImage();
		result.minmax = globalMinmax;
		return result;
	}
}
