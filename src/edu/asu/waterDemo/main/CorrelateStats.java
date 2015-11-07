package edu.asu.waterDemo.main;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.CorrelateStatsThread;
import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/CorrelateStats")
public class CorrelateStats {
	private String basisDir;
	private String metricXPath;
	private String metricYPath;
	private int NUMBER_OF_PROCESSORS = 30;
	
	public class PointSetBean{
		public double[] pointset;
		public double[] xHistogram;
		public double[] yHistogram;
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
	public PointSetBean query(
			@QueryParam("width") @DefaultValue("null") String width,
			@QueryParam("height") @DefaultValue("null") String height,
			@QueryParam("metricX") @DefaultValue("null") String metricX,
			@QueryParam("metricY") @DefaultValue("null") String metricY,
			@QueryParam("type") @DefaultValue("null") String type,
			@QueryParam("year") @DefaultValue("1960") String year,
			@QueryParam("modal") @DefaultValue("null") String modal,
			@QueryParam("dataType") @DefaultValue("") String dataType){
		PointSetBean result = new PointSetBean();
		String keyword = "";
		if(type.contains("Modal"))
			keyword = year;
		else
			keyword = modal;
		this.metricXPath = getAllFiles(this.basisDir + dataType + "/"  + metricX + File.separatorChar, keyword);
		this.metricYPath = getAllFiles(this.basisDir + dataType + "/"  + metricY + File.separatorChar, keyword);
		TiffParser xParser = new TiffParser(this.metricXPath);
		TiffParser yParser = new TiffParser(this.metricYPath);
		double[] xMinMax = new double[2];
		double[] yMinMax = new double[2];
		double[] buffer = new double[Integer.valueOf(width)*Integer.valueOf(height)];
		double[] xHistogram = new double[Integer.valueOf(width)];
		double[] yHistogram = new double[Integer.valueOf(height)];
		xParser.GetMinMax(xMinMax);
		yParser.GetMinMax(yMinMax);
		
		double[] sSize = xParser.getSize();
		int tgtHeight = (int)sSize[0];
		int tgtWidth = (int)sSize[1];
		CorrelateStatsThread[] correlateStatsService = new CorrelateStatsThread[NUMBER_OF_PROCESSORS];
		Thread[]  correlateStatsThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			correlateStatsService[i] = new CorrelateStatsThread(startIndex, endIndex, Integer.valueOf(width), Integer.valueOf(height), xParser, yParser, xMinMax, yMinMax, buffer, xHistogram, yHistogram);
			correlateStatsThread[i] = new Thread(correlateStatsService[i]);
			correlateStatsThread[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				correlateStatsThread[i].join();
				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		result.pointset = buffer;
		result.xHistogram = xHistogram;
		result.yHistogram = yHistogram;
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
