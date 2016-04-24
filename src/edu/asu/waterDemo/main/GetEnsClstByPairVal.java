package edu.asu.waterDemo.main;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.GenerateTiles;
import edu.asu.waterDemo.commonclasses.LatLng;
import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getEnsClstByPairVal")
public class GetEnsClstByPairVal {
	private int NUMBER_OF_PROCESSORS = 16;
	
	public class GetMapBase64Bean{
		public String imgStr;
	}
	
	public class DrawMapThread implements Runnable{
		private int startIndex;
		private int endIndex;
		private TiffParser parser;
		private GenerateTiles tile;
		
		public DrawMapThread(int startIndex, int endIndex, TiffParser Parser, GenerateTiles tile){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.parser = Parser;
			this.tile = tile;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for(int index=this.startIndex; index<this.endIndex; index++){
				int h = index/(int)this.parser.getSize()[1];
				int w = index%(int)this.parser.getSize()[1];
				double lat = this.parser.getUlLatlng()[0] + h*this.parser.getGeoInfo()[5];
				double lng = this.parser.getUlLatlng()[1] + w*this.parser.getGeoInfo()[1];
				double value = this.parser.getData()[index];
				try {
					this.tile.drawTiles(value, 0, 0, lat, lng);
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
	public GetMapBase64Bean query(
//			the color transfer function should be a string concat by '&' and each part should be in the form of  R,G,B
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("zoomLevel") @DefaultValue("null") String zoomLevel,
			@QueryParam("alpha") @DefaultValue("null") String alpha,
			@QueryParam("modelA") @DefaultValue("null") String modelA,
			@QueryParam("modelB") @DefaultValue("null") String modelB,
			@QueryParam("colorTable") @DefaultValue("null") String colorTable){
		GetMapBase64Bean result = new GetMapBase64Bean();
		
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		
		String baseDir = "/work/asu/data/CalculationResults/" + _dataType + "/SimilarityResults/KMeans/";
		File srcFile = getAllFiles(baseDir, modelA, modelB);
		String targetPath = srcFile.getName().replace("tif", "png");
		
		TiffParser parser = new TiffParser(srcFile.getAbsolutePath());
		String[] colortf = colorTable.split("&");
		GenerateTiles tile = new GenerateTiles(targetPath, null, "EnsClstValMapping", Integer.valueOf(zoomLevel), colortf);
		tile.setAlpha(Integer.valueOf(alpha));
		double[] size = parser.getSize();
		LatLng southwest = new LatLng(parser.getLrLatlng()[0], parser.getUlLatlng()[1]);
		LatLng northeast = new LatLng(parser.getUlLatlng()[0], parser.getLrLatlng()[1]);
		tile.processWidthHeight((int) size[1], (int) size[0], southwest, northeast);
		tile.initializeBufferImage();
		File imgFile = new File(targetPath);
		// look for base64 img string
		// if the img already exists, then convert it to base64 and return the string
		if(imgFile.exists()){
			result.imgStr = tile.encodeFromReaderToBase64(targetPath, "PNG");
			System.out.println("Use existed image!");
			return result;
		}
		
		
		double[] sSize = parser.getSize();
		int tgtHeight = (int)sSize[0];
		int tgtWidth = (int)sSize[1];
		DrawMapThread[] drawOverviewMapService = new DrawMapThread[NUMBER_OF_PROCESSORS];
		Thread[]  drawOverviewMapThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			drawOverviewMapService[i] = new DrawMapThread(startIndex, endIndex, parser, tile);
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
		
		return result;
	}
	
	private File getAllFiles(String directoryName, String keywordA, String keywordB) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile() && file.getName().endsWith(".tif") && file.getName().contains(keywordA) && file.getName().contains(keywordB)) {
	        	return file;
	        } 
	    }
	    return null;
	}
	
}
