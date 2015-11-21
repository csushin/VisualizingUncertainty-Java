package edu.asu.waterDemo.main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getEnsembleHistByPoint")
public class GetEnsembleHistByPoint {
	public String basisDir;
	public int MODELCOUNT = 18;
	public String[] MODELNAME = {"CCCma-CanESM2_CCCma-CanRCM4",
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
			    "NOAA-GFDL-GFDL-ESM2M_SMHI-RCA4",
	};
	
	public class GetEnsembleHistByPointBean{
		public HashMap<String, double[]> values;
		public double[] histData;
		public double[] highlightData;
		public double ensembleX;
		public double ensembleY;
	}
	
	public class GetOtherStatsThread implements Runnable{
		public HashMap<String, double[]> values;
		public ArrayList<Integer> indexSet;
		public String xparserpath;
		public String yparserpath;
		public String modelName;
		
		public GetOtherStatsThread(HashMap<String, double[]> values, String xParserPath, String yParserPath, ArrayList<Integer> indexSet, String modelName){
			this.values = values;
			this.xparserpath = xParserPath;
			this.yparserpath = yParserPath;
			this.indexSet = indexSet;
			this.modelName = modelName;
		}
		
		@Override
		public void run() {
			TiffParser xParser = new TiffParser(this.xparserpath);
			TiffParser yParser = new TiffParser(this.yparserpath);
			// TODO Auto-generated method stub
			for(int i = 0; i<indexSet.size(); i++){
				double xvalue = xParser.getData()[indexSet.get(i)];
				double yvalue = yParser.getData()[indexSet.get(i)];
				if(!this.values.containsKey(this.modelName)){
					double[] newarr = {xvalue, yvalue};
					this.values.put(this.modelName, newarr);
				}
				else{
					double[] arr = this.values.get(this.modelName);
					arr[0] = xvalue;
					arr[1] = yvalue;
					this.values.put(this.modelName, arr);
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
	public GetEnsembleHistByPointBean query(
			@QueryParam("lat") @DefaultValue("null") String lat,
			@QueryParam("lng") @DefaultValue("null") String lng,
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("xmetric") @DefaultValue("null") String xmetric,
			@QueryParam("ymetric") @DefaultValue("null") String ymetric,
			@QueryParam("errorRange") @DefaultValue("null") String errorRange){
		GetEnsembleHistByPointBean result = new GetEnsembleHistByPointBean();
		// get ensemble stat values
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		String xTargetPath = this.basisDir + _dataType + "/" + "EnsembleStatOfTimeMean/Ensemble" + xmetric + "OfTimeMean.tif";
		String yTargetPath = this.basisDir + _dataType + "/" + "EnsembleStatOfTimeMean/Ensemble" + ymetric + "OfTimeMean.tif";
		TiffParser xTgtParser = new TiffParser(xTargetPath);
		TiffParser yTgtParser = new TiffParser(yTargetPath);
		int h = (int) ((Double.valueOf(lat) - xTgtParser.getUlLatlng()[0])/xTgtParser.getGeoInfo()[5]);
		int w = (int) ((Double.valueOf(lng) - xTgtParser.getUlLatlng()[1])/xTgtParser.getGeoInfo()[1]);
		int index = (int) (h*xTgtParser.getSize()[1] + w);
		result.ensembleX = xTgtParser.getData()[index];
		result.ensembleY = yTgtParser.getData()[index];
		
		// find all points in the given error range
//		ArrayList<Integer> indices = new ArrayList<Integer>();
//		ArrayList<double[]> locations = new ArrayList<double[]>();
//		int tgtHeight = (int) xTgtParser.getSize()[0];
//		int tgtWidth = (int) xTgtParser.getSize()[1];
//		for(int hInd=0; hInd<tgtHeight; hInd++){
//			for(int wInd=0; wInd<tgtWidth; wInd++){
//				int tgtIndex = hInd*tgtWidth+wInd;
//				double _value = xTgtParser.getData()[tgtIndex];
//				if(Math.abs(_value - result.ensembleX)<=Double.valueOf(errorRange)){
//					indices.add(tgtIndex);
//					double _lat = xTgtParser.getUlLatlng()[0] + hInd*xTgtParser.getGeoInfo()[5];
//					double _lng = xTgtParser.getUlLatlng()[1] + wInd*xTgtParser.getGeoInfo()[1];
//					double[] _location = {_lat, _lng};
//					locations.add(_location);
//				}
//			}
//		}
//		System.out.println("indices set length : " + indices.size());
		// here we only take in one point
		ArrayList<Integer> indices = new ArrayList<Integer>();
		indices.add(index);
		
		// get other 18 models stat
		HashMap<String, double[]> values = new HashMap<String, double[]>();
		String xbase = this.basisDir + _dataType + "/Time" + xmetric;
		String ybase = this.basisDir + _dataType + "/Time" + ymetric;
		String[] xParserPath = new String[MODELCOUNT];
		String[] yParserPath = new String[MODELCOUNT];
		for(int i=0; i<MODELCOUNT; i++){
			xParserPath[i] = getAllFiles(xbase, MODELNAME[i]);
			yParserPath[i] = getAllFiles(ybase, MODELNAME[i]);
		}
		GetOtherStatsThread[] getOtherStatsService = new GetOtherStatsThread[MODELCOUNT];
		Thread[]  getOtherStatsThread = new Thread[MODELCOUNT];
		for(int i=0; i<MODELCOUNT; i++){
			getOtherStatsService[i] = new GetOtherStatsThread(values, xParserPath[i], yParserPath[i], indices, MODELNAME[i]);
			getOtherStatsThread[i] = new Thread(getOtherStatsService[i]);
			getOtherStatsThread[i].start();
		}
		try{
			for(int i=0; i<MODELCOUNT; i++){
				getOtherStatsThread[i].join();
				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		result.values = values;
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
