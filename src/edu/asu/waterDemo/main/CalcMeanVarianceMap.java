//package edu.asu.waterDemo.main;
//
//import java.io.File;
//import java.io.IOException;
//
//import javax.servlet.ServletContext;
//import javax.ws.rs.DefaultValue;
//import javax.ws.rs.GET;
//import javax.ws.rs.Produces;
//import javax.ws.rs.QueryParam;
//import javax.ws.rs.core.Context;
//
//import org.glassfish.jersey.server.JSONP;
//
//
//public class CalcMeanVarianceMap {
//	public class CalcMeanVarianceMapBean{
//		public String imgLocation;
//		public boolean created;
//	};
//
//	String linux_supplyDir;
//	String linux_demandDir;
//	String linux_meanVarianceDir;
//	
//	@Context
//	public void setServletContext(ServletContext context) {
//		linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
//		linux_supplyDir = "/work/asu/data/wsupply/"; 
//		linux_meanVarianceDir = "/work/asu/data/wuncertainty/MeanVariance/";
//	}
//	
//	@GET
//	@JSONP(queryParam = "callback", callback = "eval")
//	@Produces({"application/x-javascript"})
//	public CalcMeanVarianceMapBean query(
//			@QueryParam("demandfName") @DefaultValue("null") String demandfName,
//			@QueryParam("emissionType") @DefaultValue("null") String emission,
//			@QueryParam("scenarioType") @DefaultValue("null") String scenario,
//			@QueryParam("uncertaintyType") @DefaultValue("agree") String uncertaintyType,
//			@QueryParam("resolution") @DefaultValue("raw") String resolution) throws IOException {
//		CalcMeanVarianceMapBean result = new CalcMeanVarianceMapBean();
//		
//		if(resolution.equals("raw")){
//			linux_supplyDir = linux_supplyDir+"BW_1km/";
//		}
//		else{
//			linux_supplyDir = linux_supplyDir + "BW_10km/";
//			linux_demandDir = linux_demandDir + "resampled_10km/";
//		}
//		String demandPath = linux_demandDir + demandfName;
//		String fileName = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_MeanVariance.tif";
//		String outputfile = linux_meanVarianceDir + fileName;
////		String link = null;
////		link = compute(demandPath, emission, scenario, outputfile, uncertaintyType));
////		if(!link.isEmpty()){
////			result.imgLocation = link;
////			result.created = true;
////		}
////		else{
////			result.created = false;
////			System.out.println("Can't create geotiff image!");		
////		}
//		
//		return result;
//	}
//	
//	
////	public String compute(String demandPath, String supplyPath, double[][] colorTable, String){
//		
////	}
//}
