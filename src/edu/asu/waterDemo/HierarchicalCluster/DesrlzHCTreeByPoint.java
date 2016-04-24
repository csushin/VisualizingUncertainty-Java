package edu.asu.waterDemo.HierarchicalCluster;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import edu.asu.vader.apporiented.algorithm.clustering.AverageLinkageStrategy;
import edu.asu.vader.apporiented.algorithm.clustering.Cluster;
import edu.asu.vader.apporiented.algorithm.clustering.ClusteringAlgorithm;
import edu.asu.vader.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.main.GetVHData.VHDataUnit;

@Path("/getHCTree")
public class DesrlzHCTreeByPoint {
	private String[] modelList = {
			"CCCma-CanESM2_CCCma-CanRCM4",
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
		    "NOAA-GFDL-GFDL-ESM2M_SMHI-RCA4"
	};
	private String[] metricList = {"TimeMean", "TimeSkewness", "TimeKurtosis", "TimeEntropy", "TimeQuadraticScore", "TimeCV", "TimeStd", "TimeIQR"};

	private String basisDir;
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")) {
			this.basisDir = context.getRealPath("img") + File.separatorChar;
		}else{
			this.basisDir = "/work/asu/data/CalculationResults/pr_HIST/SimilarityResults/OverallSum/";
		}
	}
	
	
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public String query(
			@QueryParam("lat") @DefaultValue("null") String lat,
			@QueryParam("lng") @DefaultValue("null") String lng){
		HashMap<String, File> filePath = new HashMap<String, File>();
		HashMap<String, Band> bandParsers = new HashMap<String, Band>();
		filePath = getAllFiles(basisDir, "OverallSum");
		double[] upperleftLatLng = new double[2];
		double[] unitLatLng = new double[2];
		for(String key : filePath.keySet()){
			gdal.AllRegister();
			Dataset hDataset = gdal.Open(filePath.get(key).getAbsolutePath(), gdalconstConstants.GA_ReadOnly);
			if (hDataset == null)
			{
				System.err.println("GDALOpen failed - " + gdal.GetLastErrorNo());
				System.err.println(gdal.GetLastErrorMsg());
			}
			Band hBand = hDataset.GetRasterBand(1);
			upperleftLatLng[0] = hDataset.GetGeoTransform()[3];
			upperleftLatLng[1] = hDataset.GetGeoTransform()[0];
			unitLatLng[0] = hDataset.GetGeoTransform()[5];
			unitLatLng[1] = hDataset.GetGeoTransform()[1];
			if(!bandParsers.containsKey(key)) bandParsers.put(key, hBand);
		}
		String[] names = new String[modelList.length];
		double[][] dist = new double[modelList.length][modelList.length];
		double[][] cmddist = new double[modelList.length][modelList.length];
		int w = (int) ((Double.valueOf(lng) - upperleftLatLng[1])/unitLatLng[1]);
		int h = (int)((Double.valueOf(lat) - upperleftLatLng[0])/unitLatLng[0]);
		for(int m=0; m<modelList.length; m++){
			names[m] = "O"+m;
			for(int n=0; n<modelList.length; n++){
				 if(n==m){
					 dist[m][n] = 0;
				 }
				 else if(n<m){
					 dist[m][n] = dist[n][m];
				 }
				 else{
					double[] temp = new double[1];
					Band hBand = bandParsers.get(modelList[m]+"_"+modelList[n]+"_OverallSum");
					if(hBand == null)
						System.out.println(modelList[m]+"_"+modelList[n] + ", " + w + ", " + h);
					hBand.ReadRaster(w, h, 1, 1, gdalconst.GDT_Float64, temp);
					dist[m][n] = temp[0];
				 }
			}
		}
		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
		Cluster cluster = alg.performClustering(dist, names, new AverageLinkageStrategy());
		for(int m=0; m<names.length-1; m++){
		  	for(int n=m+1; n<names.length; n++){
		  		if(dist[m][n] == 0){
		  			cmddist[m][n] = Double.NaN;
		  		}
		  		else{
		  			cmddist[m][n] = cluster.computeCMDDistance(names[m], names[n]);
		  		}
		  		
		  	}
		}
		return Arrays.deepToString(cmddist);
	}
	
	// module for getting all files within keywords, loop while
		public HashMap<String, File> getAllFiles(String directoryName, String keyword) {
				    File directory = new File(directoryName);
				    HashMap<String, File> result = new HashMap<String, File>();
				    // get all the files from a directory
				    File[] fList = directory.listFiles();
				    for (File file : fList) {
				    	String name = file.getName();
				        if (file.isFile() && name.endsWith(".tif") && name.contains(keyword)) {
				        	String key = file.getName().replace(".tif", "");
				        	if(!result.containsKey(key)) result.put(key, file);
				        }
				    }
				    if(result.size()==0) System.out.println("Cannot find the given file: " + keyword);
				    return result;
		}
}
