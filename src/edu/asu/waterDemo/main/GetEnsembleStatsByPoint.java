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

import edu.asu.waterDemo.commonclasses.GetHistDataThread;
import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getOtherStatsForSameMean")
public class GetEnsembleStatsByPoint {
	private String basisDir;
	
	
	public class GetEnsembleStatsByPointBean{
		public HashMap<String, double[]> values;
		public ArrayList<Integer> indices;
		public HashMap<double[], Integer> distinctStats;
		public HashMap<String, ArrayList<Double>> distinctStatsJS;
		public ArrayList<double[]> locations;
		public double selectedValue;
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
		for(int hInd=0; h<tgtHeight; h++){
			for(int wInd=0; w<tgtWidth; w++){
				int tgtIndex = h*tgtWidth+w;
				double _value = tgtParser.getData()[tgtIndex];
				if(Math.abs(_value - value)<Double.valueOf(errorRange)){
					indices.add(tgtIndex);
					double _lat = tgtParser.getUlLatlng()[0] + hInd*tgtParser.getGeoInfo()[5];
					double _lng = tgtParser.getUlLatlng()[1] + wInd*tgtParser.getGeoInfo()[1];
					double[] _location = {_lat, _lng};
					locations.add(_location);
				}
				
			}
		}
//		for(int i=0; i<tgtParser.getSize()[1]*; i++){
//			double _value = tgtParser.getData()[i];
//			if(Math.abs(_value - value)<Double.valueOf(errorRange)){
//				indices.add(i);
//				double lat = (int) ((Double.valueOf(lat) - tgtParser.getUlLatlng()[0])/tgtParser.getGeoInfo()[5]);
//				double lng = (int) ((Double.valueOf(lng) - tgtParser.getUlLatlng()[1])/tgtParser.getGeoInfo()[1]);
//			}
//		}
		
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
		HashMap<double[], Integer> distinctStats = new HashMap<double[], Integer>();
		for(int i=0; i<indices.size(); i++){
			double[] stats = new double[metrics.length];
			for(int k=0; k<metrics.length; k++){
				stats[k] = result.values.get(metrics[k])[i];
			}			
			Iterator it = distinctStats.entrySet().iterator();
			boolean existedKey = false;
		    while (it.hasNext()) {
		    	Map.Entry pair = (Map.Entry)it.next();
		    	double[] key = (double[]) pair.getKey();
		    	if(Arrays.equals(key, stats)){
		    		int count = (int) pair.getValue();
		    		existedKey = true;
		    		count++;
		    		distinctStats.put(key, count);
		    	}
		    }
		    if(!existedKey)
		    	distinctStats.put(stats, 1);
		}
		
		//	convert the distinct combinations into a Javascript friendly format	
		HashMap<String, ArrayList<Double>> distinctStatsJS = new HashMap<String, ArrayList<Double>>();
		Iterator it = distinctStats.entrySet().iterator();
		boolean existedKey = false;
	    while (it.hasNext()) {
	    	Map.Entry pair = (Map.Entry)it.next();
	    	double[] key = (double[]) pair.getKey();
	    	for(int i=0; i<metrics.length; i++){
	    		ArrayList<Double> array = new ArrayList<Double>();
	    		if(distinctStatsJS.containsKey(metrics[i])){
	    			array = distinctStatsJS.get(metrics[i]);
	    		}
	    		array.add(key[i]);
	    		distinctStatsJS.put(metrics[i], array);
	    	}
	    }
	    result.selectedValue = value;
	    result.locations = locations;
	    result.distinctStatsJS = distinctStatsJS;
		result.distinctStats = distinctStats;
		result.indices = indices;
		return result;
	}
	
	
}
