package edu.asu.waterDemo.main;

import java.io.File;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.TiffParser;
import edu.asu.waterDemo.main.DrawTreeVisMap.imgBase64;


@Path("/getMetricStat")
public class GetMetricStat {
	private String preCalcDir;
	private String metricDir;
	private String targetFile;
	private int NUMBER_OF_PROCESSORS = 30;
	
	public class StatBean{
		public double max;
		public double min;
		public String metric;
	}
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")) {
			this.preCalcDir = context.getRealPath("img") + File.separatorChar;
		}else{
			this.preCalcDir = "/work/asu/data/CalculationResults" + File.separatorChar;
		}
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public StatBean query(
			@QueryParam("metric") @DefaultValue("null") String metric,
			@QueryParam("year") @DefaultValue("1960") String year,
			@QueryParam("modal") @DefaultValue("null") String modal,
			@QueryParam("type") @DefaultValue("null") String type,
			@QueryParam("dataType") @DefaultValue("") String dataType) {
		StatBean result = new StatBean();
		this.metricDir = this.preCalcDir + dataType + "/" + metric + File.separatorChar;
		if(type.contains("Modal") && metric.contains("Modal"))
			this.targetFile = getAllFiles(this.metricDir, year);
		if(type.contains("Time") && metric.contains("Time"))
			this.targetFile = getAllFiles(this.metricDir, modal);
		TiffParser parser = new TiffParser(this.targetFile);
		double[] minmax = new double[2];
		parser.GetMinMax(minmax);
		result.max = minmax[1];
		result.min = minmax[0];
		result.metric = metric;
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
