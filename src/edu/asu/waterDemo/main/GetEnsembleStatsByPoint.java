package edu.asu.waterDemo.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getOtherStatsForSameMean")
public class GetEnsembleStatsByPoint {
	private String basisDir;
	
	
	public class GetEnsembleStatsByPointBean{
		public HashMap<String, double[]> values;
		public ArrayList<Integer> indices;
		public ArrayList<DistinctStat2Amount> distinctStats;
		public HashMap<String, ArrayList<Double>> distinctStatsJS;
		public ArrayList<double[]> locations;
		public double selectedValue;
		public ArrayList<Integer> distinctIndices;
		public int totalAmount = 0;
//		public ArrayList<Location> distinctLocations;
	}
	
	public class DistinctStat2Amount{
		public double[] stats;
		public int amount;
		public DistinctStat2Amount(double[] stats, int amount) {
			this.stats = stats;
			this.amount = amount;
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
	
	public class GetOtherStatsThread implements Runnable{
		public HashMap<String, double[]> values;
		public ArrayList<Integer> indexSet;
		public TiffParser parser;
		public double tgtValue;
		public String metricType;
		
		public GetOtherStatsThread(HashMap<String, double[]> values, TiffParser parser, String metricType, double tgtValue, ArrayList<Integer> indexSet){
			this.values = values;
			this.parser = parser;
			this.tgtValue = tgtValue;
			this.indexSet = indexSet;
			this.metricType = metricType;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for(int i = 0; i<indexSet.size(); i++){
				double _value = this.parser.getData()[indexSet.get(i)];
				if(!this.values.containsKey(this.metricType)){
					double[] newarr = new double[indexSet.size()];
					newarr[i] = _value;
					this.values.put(this.metricType, newarr);
				}
				else{
					double[] arr = this.values.get(this.metricType);
					arr[i] = _value;
					this.values.put(this.metricType, arr);
				}
			}
		}
		
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public GetEnsembleStatsByPointBean query(
			@QueryParam("lat") @DefaultValue("null") String lat,
			@QueryParam("lng") @DefaultValue("null") String lng,
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("srcTypeA") @DefaultValue("null") String srcTypeA,
			@QueryParam("srcTypeB") @DefaultValue("null") String srcTypeB,
			@QueryParam("metricList") @DefaultValue("null") String metricList,
			@QueryParam("errorRange") @DefaultValue("null") String errorRange){
		GetEnsembleStatsByPointBean result = new GetEnsembleStatsByPointBean();
		result.values = new HashMap<String, double[]>();
		// find the target file pat
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		String targetPath = this.basisDir + _dataType + "/" + "EnsembleStatOf"+ srcTypeB + "/" + srcTypeA + "Of" + srcTypeB + ".tif";
		// get the pointed value
		TiffParser tgtParser = new TiffParser(targetPath);
		int h = (int) ((Double.valueOf(lat) - tgtParser.getUlLatlng()[0])/tgtParser.getGeoInfo()[5]);
		int w = (int) ((Double.valueOf(lng) - tgtParser.getUlLatlng()[1])/tgtParser.getGeoInfo()[1]);
		int index = (int) (h*tgtParser.getSize()[1] + w);
		double value = tgtParser.getData()[index];
//		System.out.println("index are " + index);
//		System.out.println("original values are " + value);
		if(Double.isNaN(value) || value == -1)
			return result;
		
		// get the locations of points with similar values
		ArrayList<Integer> indices = new ArrayList<Integer>();
		ArrayList<double[]> locations = new ArrayList<double[]>();
		int tgtHeight = (int) tgtParser.getSize()[0];
		int tgtWidth = (int) tgtParser.getSize()[1];
		for(int hInd=0; hInd<tgtHeight; hInd++){
			for(int wInd=0; wInd<tgtWidth; wInd++){
				int tgtIndex = hInd*tgtWidth+wInd;
				double _value = tgtParser.getData()[tgtIndex];
				if(Math.abs(_value - value)<Double.valueOf(errorRange)){
					indices.add(tgtIndex);
//					double _lat = tgtParser.getUlLatlng()[0] + hInd*tgtParser.getGeoInfo()[5];
//					double _lng = tgtParser.getUlLatlng()[1] + wInd*tgtParser.getGeoInfo()[1];
//					double[] _location = {_lat, _lng};
//					locations.add(_location);
				}
				
			}
		}
		
		// get other stat values
		String[] metrics = metricList.split(",");
//		HashMap<Integer, double[]> values = new HashMap<Integer, double[]>();
		GetOtherStatsThread[] getOtherStatsService = new GetOtherStatsThread[metrics.length];
		Thread[]  getOtherStatsThread = new Thread[metrics.length];
		for(int i=0; i<metrics.length; i++){
			String path = this.basisDir + _dataType + "/"  + "EnsembleStatOf"+ srcTypeB + "/Ensemble" + metrics[i] + "Of" + srcTypeB + ".tif";
			TiffParser parser = new TiffParser(path);
			getOtherStatsService[i] = new GetOtherStatsThread(result.values, parser, metrics[i], value, indices);
			getOtherStatsThread[i] = new Thread(getOtherStatsService[i]);
			getOtherStatsThread[i].start();
		}
		try{
			for(int i=0; i<metrics.length; i++){
				getOtherStatsThread[i].join();
				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// to avoid the duplication, extract the distinct combinations of the stat values and their counts
		ArrayList<DistinctStat2Amount> distinctStats = new ArrayList<DistinctStat2Amount>();
//		ArrayList<Location> distinctLocations = new ArrayList<Location>();
		ArrayList<Integer> distinctIndices = new ArrayList<Integer>();
		for(int i=0; i<indices.size(); i++){
			double[] stats = new double[metrics.length];
			for(int k=0; k<metrics.length; k++){
				stats[k] = result.values.get(metrics[k])[i];
			}	
			if(distinctStats.size() == 0){
				DistinctStat2Amount element = new DistinctStat2Amount(stats, 1);
				distinctStats.add(element);
				distinctIndices.add(indices.get(i));
			}
			else{
				boolean existedKey = false;
				for(int j=0; j<distinctStats.size(); j++){
					double[] existedStats = distinctStats.get(j).stats;
					if(Arrays.equals(existedStats, stats)){
			    		distinctStats.get(j).amount+=1;
			    		existedKey = true;
			    	}
				}
			    if(!existedKey){
			    	DistinctStat2Amount element = new DistinctStat2Amount(stats, 1);
					distinctStats.add(element);
			    	distinctIndices.add(indices.get(i));
			    }	
			}
		}
		System.out.println("distinctStats Finished!");
		//	convert the distinct combinations into a Javascript friendly format	
		HashMap<String, ArrayList<Double>> distinctStatsJS = new HashMap<String, ArrayList<Double>>();
		for(int i=0; i<distinctStats.size(); i++){
			for(int j=0; j<metrics.length; j++){
				ArrayList<Double> array = new ArrayList<Double>();
				if(distinctStatsJS.containsKey(metrics[j])){
					array = distinctStatsJS.get(metrics[j]);
				}
				array.add(distinctStats.get(i).stats[j]);
				distinctStatsJS.put(metrics[j], array);
			}
		}
//		Iterator it = distinctStats.entrySet().iterator();
//		boolean existedKey = false;
//	    while (it.hasNext()) {
//	    	Map.Entry pair = (Map.Entry)it.next();
//	    	double[] key = (double[]) pair.getKey();
//	    	for(int i=0; i<metrics.length; i++){
//	    		ArrayList<Double> array = new ArrayList<Double>();
//	    		if(distinctStatsJS.containsKey(metrics[i])){
//	    			array = distinctStatsJS.get(metrics[i]);
//	    		}
//	    		array.add(key[i]);
//	    		distinctStatsJS.put(metrics[i], array);
//	    	}
//	    }
	    System.out.println("dinstanceStatJS Finished!");
	    result.totalAmount = indices.size();
	    result.selectedValue = value;
	    result.distinctIndices = distinctIndices;
//	    result.distinctLocations = distinctLocations;
//	    result.locations = locations;
	    result.distinctStatsJS = distinctStatsJS;
		result.distinctStats = distinctStats;
//		result.indices = indices;
		return result;
	}
	
	
}
