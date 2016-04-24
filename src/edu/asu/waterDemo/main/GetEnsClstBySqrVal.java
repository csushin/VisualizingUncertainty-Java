package edu.asu.waterDemo.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.GenerateTiles;
import edu.asu.waterDemo.commonclasses.LatLng;
import edu.asu.waterDemo.commonclasses.LoadTiffThread;
import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getEnsClstBySqrVal")
public class GetEnsClstBySqrVal {
	private int NUMBER_OF_PROCESSORS = 16;

	
	public class GetMapBase64Bean{
		public String imgStr;
	}
	
	public class DrawMapThread implements Runnable{
		private int startIndex;
		private int endIndex;
		private ArrayList<TiffParser> parsers;
		private GenerateTiles tile;
		private String minmaxType;
		
		
		public DrawMapThread(int startIndex, int endIndex, String minmaxType, ArrayList<TiffParser> parsers,  GenerateTiles tile){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.parsers = parsers;
			this.tile = tile;
			this.minmaxType = minmaxType;
			
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for(int index=this.startIndex; index<this.endIndex; index++){
				double comparableValue = 0;
				if(this.minmaxType.contains("min")){
					comparableValue = Integer.MAX_VALUE;
				}
				else if(this.minmaxType.contains("max")){
					comparableValue = Integer.MIN_VALUE;
				}
				int h = index/(int)this.parsers.get(0).getSize()[1];
				int w = index%(int)this.parsers.get(0).getSize()[1];
				double lat = this.parsers.get(0).getUlLatlng()[0] + h*this.parsers.get(0).getGeoInfo()[5];
				double lng = this.parsers.get(0).getUlLatlng()[1] + w*this.parsers.get(0).getGeoInfo()[1];
				for(int i=0; i<this.parsers.size(); i++){
					double value  = this.parsers.get(i).getData()[index];
					if(Double.isNaN(value)){
						comparableValue = Double.NaN;
						break;
					}
					else{
						if(this.minmaxType.contains("Minimum")){
							if(value < comparableValue)
								comparableValue = (int) value;
						}
						else if(this.minmaxType.contains("Maximum")){
							if(value > comparableValue)
								comparableValue = (int) value;
						}
						else if(this.minmaxType.contains("Average")){
							comparableValue+=(value/this.parsers.size());
						}
					}
				}
				try {
					this.tile.drawTiles(Math.floor(comparableValue), 0, 0, lat, lng);
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
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("zoomLevel") @DefaultValue("null") String zoomLevel,
			@QueryParam("alpha") @DefaultValue("null") String alpha,
			@QueryParam("modelSet") @DefaultValue("null") String modelSet,
			@QueryParam("colorTable") @DefaultValue("null") String colorTable,
			@QueryParam("minmaxType") @DefaultValue("null") String minmaxType){
		GetMapBase64Bean result = new GetMapBase64Bean();
		
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		
		String baseDir = "/work/asu/data/CalculationResults/" + _dataType + "/SimilarityResults/KMeans/";
		ArrayList<File> files = new ArrayList<File>();
		String[] models = modelSet.split("&");
		for(int i=0; i<models.length-1; i++){
			for(int j=i+1; j<models.length; j++){
				files.add(getAllFiles(baseDir, models[i], models[j]));
			}
		}
		ArrayList<TiffParser> parsers = new ArrayList<TiffParser>();
		parsers = loadTiffFiles(files);
		String targetPath = baseDir + modelSet +  "_" + minmaxType + ".png";
		
		String[] colortf = colorTable.split("&");
		GenerateTiles tile = new GenerateTiles(targetPath, null, "EnsClstValMapping", Integer.valueOf(zoomLevel), colortf);
		tile.setAlpha(Integer.valueOf(alpha));
		double[] size = parsers.get(0).getSize();
		LatLng southwest = new LatLng(parsers.get(0).getLrLatlng()[0], parsers.get(0).getUlLatlng()[1]);
		LatLng northeast = new LatLng(parsers.get(0).getUlLatlng()[0], parsers.get(0).getLrLatlng()[1]);
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
		
		double[] sSize = parsers.get(0).getSize();
		int tgtHeight = (int)sSize[0];
		int tgtWidth = (int)sSize[1];
		DrawMapThread[] drawMapService = new DrawMapThread[NUMBER_OF_PROCESSORS];
		Thread[] drawwMapThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			drawMapService[i] = new DrawMapThread(startIndex, endIndex, minmaxType, parsers, tile);
			drawwMapThread[i] = new Thread(drawMapService[i]);
			drawwMapThread[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				drawwMapThread[i].join();
//				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		result.imgStr = tile.encodeFromBufferImgToBase64();
		
		return result;
	}
	
//	thread entry for loading tiff files
	public ArrayList<TiffParser> loadTiffFiles(ArrayList<File> files){
		ArrayList<TiffParser> parsers = new ArrayList<TiffParser>();
		int numOfProcessors = files.size();
		LoadTiffThread[] LoadTiffFilesServices = new LoadTiffThread[numOfProcessors];
		Thread[] LoadTiffFilesThreads = new Thread[numOfProcessors];
		for(int i=0; i<numOfProcessors; i++){
			LoadTiffFilesServices[i] = new LoadTiffThread(files.get(i).getAbsolutePath());
			LoadTiffFilesThreads[i] = new Thread(LoadTiffFilesServices[i]);
			LoadTiffFilesThreads[i].start();
		}
		try{
			for(int i=0; i<numOfProcessors; i++){
				LoadTiffFilesThreads[i].join();
				parsers.add(LoadTiffFilesServices[i].getResult());
				System.out.println(i + " Loading Finished~");
			}
		}catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return parsers;
	}
	
	public File getAllFiles(String directoryName, String keywordA, String keywordB) {
	    File directory = new File(directoryName);
	
	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	    	String name = file.getName();
	        if (file.isFile() && name.endsWith(".tif") && !name.contains("MPI-ESM-LR_CCLM") && !name.contains("HadGEM2-ES_CCLM") && !name.contains("EC-EARTH-r12_CCLM")
					&& !name.contains("CNRM-CM5_CCLM") && !name.contains("EC-EARTH-r3_HIRHAM") && name.contains(keywordA) && name.contains(keywordB)) {
	            return file;
	        } else if (file.isDirectory()) {
//	        	getAllFiles(file.getAbsolutePath(), files, keywordA, keywordB);
	        }
	    }
	    return null;
	}
}
