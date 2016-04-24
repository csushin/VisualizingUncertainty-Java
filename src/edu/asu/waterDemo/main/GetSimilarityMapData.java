package edu.asu.waterDemo.main;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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


@Path("/getSimilarityMapData")
public class GetSimilarityMapData {
	private String basisDir;
	private int NUMBER_OF_PROCESSORS = 16;
	private String[] GCM = {"CCCma-CanESM2", "CNRM-CERFACS-CNRM-CM5", "CSIRO-QCCCE-CSIRO-Mk3-6-0", "ICHEC-EC-EARTH", "IPSL-IPSL-CM5A-MR", "MIROC-MIROC5", "MOHC-HadGEM2-ES", "MPI-M-MPI-ESM-LR", "NCC-NorESM1-M", "NOAA-GFDL-GFDL-ESM2M"};
	private String[] RCM = {"CCCma-CanRCM4", "SMHI-RCA4", "CLMcom-CCLM4-8-17", "DMI-HIRHAM5", "KNMI-RACMO22T"};
	private String[] modelSet = {"CCCma-CanESM2_CCCma-CanRCM4",
				    "CCCma-CanESM2_SMHI-RCA4",
				    "CNRM-CERFACS-CNRM-CM5_CLMcom-CCLM4-8-17",
				    "CNRM-CERFACS-CNRM-CM5_SMHI-RCA4",
				    "CSIRO-QCCCE-CSIRO-Mk3-6-0_SMHI-RCA4",
				    "ICHEC-EC-EARTH_CLMcom-CCLM4-8-17",
				    "ICHEC-EC-EARTH_DMI-HIRHAM5",
				    "ICHEC-EC-EARTH_KNMI-RACMO22T",
				    "ICHEC-EC-EARTH_SMHI-RCA4",
				    "IPSL-IPSL-CM5A-MR_SMHI-RCA4",
				    "MIROC-MIROC5_SMHI-RCA4_v1",
				    "MOHC-HadGEM2-ES_CLMcom-CCLM4-8-17",
				    "MOHC-HadGEM2-ES_SMHI-RCA4",
				    "MOHC-HadGEM2-ES_KNMI-RACMO22T_v1",
				    "MPI-M-MPI-ESM-LR_CLMcom-CCLM4-8-17",
				    "MPI-M-MPI-ESM-LR_SMHI-RCA4",
				    "NCC-NorESM1-M_SMHI-RCA4",
				    "NOAA-GFDL-GFDL-ESM2M_SMHI-RCA4"};
	
	public class GetSimilarityMapDataBean{
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
	
	public class DrawSimilaritywMapService implements Runnable{
		private int startIndex;
		private int endIndex;
		private TiffParser parser;
		private String colorBy;
		private String[] tfFunction;
		private GenerateTiles tile;
		
		public DrawSimilaritywMapService(int startIndex, int endIndex, TiffParser Parser, String colorBy, String[] tfFucntion, GenerateTiles tile){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.parser = Parser;
			this.colorBy = colorBy;
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
				int ind = -1;
				if(Double.isNaN(value)){
					this.tile.drawTile(ind, tfFunction, lat, lng);
				}
				else{
					if(colorBy.equals("GCM"))
						ind = Arrays.asList(GCM).indexOf(modelSet[(int) value].split("_")[0]);
					if(colorBy.equals("RCM"))
						ind = Arrays.asList(RCM).indexOf(modelSet[(int) value].split("_")[1]);
					if(colorBy.equals("All") || colorBy.contains("Threshold"))
						ind = (int) value;
					this.tile.drawTile(ind, tfFunction, lat, lng);
				}
			}
		}
		
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public GetSimilarityMapDataBean query(
			@QueryParam("color") @DefaultValue("null") String color,
			@QueryParam("colorBy") @DefaultValue("null") String colorBy,
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("zoomLevel") @DefaultValue("null") String zoomLevel,
			@QueryParam("alpha") @DefaultValue("null") String alpha,
			@QueryParam("shapeOnly") @DefaultValue("null") String shapeOnly,
			@QueryParam("similarityType") @DefaultValue("null") String similarityType,
			@QueryParam("similarityDis") @DefaultValue("null") String similarityDis,
			@QueryParam("threshold") @DefaultValue("null") String threshold){
		GetSimilarityMapDataBean result = new GetSimilarityMapDataBean();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		String targetPath = "";
		String subdir = "";
		String distance = similarityDis;
		if(similarityType.equals("Euclidean")){
			subdir = "OverallSum";
		}
		if(similarityType.equals("Cosine")){
			subdir = "OverallCosine";
//			if(similarityDis.contains("Maximum"))
//				distance = "MinimumDis";
//			else
//				distance = "MaximumDis";
		}
		if(similarityType.equals("CosinePlusL1Norm")){
			subdir = "L1NormPlusCosine";
		}
		if(similarityType.equals("L1Norm")){
			subdir = "DistributionDistance_L1Norm";
		}
		
		if(colorBy.contains("Threshold")){
			distance = distance + "TopCount_" + threshold;
			if(similarityType.contains("Plus") || similarityType.contains("L1Norm")){
				if(colorBy.contains("Count"))
					distance = "Top_"+threshold+"_Count_gt";
				else if(colorBy.contains("Index"))
					distance = "Top_"+threshold+"_Index_gt";
			}
		}
		
		if(shapeOnly.equals("false")){
			targetPath = this.basisDir + _dataType + "/SimilarityResults/" + subdir+ "/" + distance + ".tif";
		}
		else{
			targetPath = this.basisDir + _dataType + "/SimilarityResults/"+ subdir +"_ShapeDescriptor" + "/" + distance + ".tif";
		}
		String imgPath = targetPath.replace(".tif", ".png");
		TiffParser targetparser = new TiffParser(targetPath);
		String[] colortf = color.split("&");
		
		GenerateTiles tile = new GenerateTiles(imgPath, null, "SimilarityMap", Integer.valueOf(zoomLevel), colortf);
		double[] size = targetparser.getSize();
		LatLng southwest = new LatLng(targetparser.getLrLatlng()[0], targetparser.getUlLatlng()[1]);
		LatLng northeast = new LatLng(targetparser.getUlLatlng()[0], targetparser.getLrLatlng()[1]);
		tile.processWidthHeight((int) size[1], (int) size[0], southwest, northeast);
		tile.initializeBufferImage();
		tile.setAlpha(Integer.valueOf(alpha));
		
		double[] sSize = targetparser.getSize();
		int tgtHeight = (int)sSize[0];
		int tgtWidth = (int)sSize[1];
		DrawSimilaritywMapService[] DrawSimilaritywMapServices = new DrawSimilaritywMapService[NUMBER_OF_PROCESSORS];
		Thread[] DrawSimilaritywMapThreads = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			DrawSimilaritywMapServices[i] = new DrawSimilaritywMapService(startIndex, endIndex, targetparser, colorBy, colortf, tile);
			DrawSimilaritywMapThreads[i] = new Thread(DrawSimilaritywMapServices[i]);
			DrawSimilaritywMapThreads[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				DrawSimilaritywMapThreads[i].join();
//				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		result.imgStr = tile.encodeFromBufferImgToBase64();
		return result;
	}
}
