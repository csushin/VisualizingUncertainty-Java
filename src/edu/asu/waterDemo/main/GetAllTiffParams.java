//package edu.asu.waterDemo.main;
//
//import java.io.File;
//import java.net.UnknownHostException;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import javax.servlet.ServletContext;
//import javax.ws.rs.DefaultValue;
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.QueryParam;
//import javax.ws.rs.core.Context;
//
//import org.glassfish.jersey.server.JSONP;
//
//import edu.asu.waterDemo.bean.GetAllTiffParamsBean;
//import edu.asu.waterDemo.commonclasses.LoadAllPossibleParams;
//
//
//@Path("/LoadTiffParams")
//public class GetAllTiffParams {
//	String supplyDir;
//	String demandDir;
//	String linux_demandDir;
//	String linux_supplyDir;
//
//	@Context
//	public void setServletContext(ServletContext context) {
////		supplyDir = context.getRealPath("img\\supply") + File.separatorChar;
//		supplyDir = context.getRealPath("img/supply720x360") + File.separatorChar;
//		demandDir = context.getRealPath("img/demand/popden_pred/720x360") + File.separatorChar;
//		linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
//		linux_supplyDir = "/work/asu/data/wsupply/BW/"; 
//	}
//	
//	@GET
//	@JSONP(queryParam = "callback", callback = "eval")
//	@Produces({"application/x-javascript"})
//	public GetAllTiffParamsBean query(@QueryParam("message") @DefaultValue("FALSE") String message) {
//		GetAllTiffParamsBean result = new GetAllTiffParamsBean();
//		String demandPath = demandDir;
//		String supplyPath = supplyDir;
//		//System.out.println(Path);
//		if(message.equals("loadFiles"))
//		{
//			ArrayList<String> POP = new ArrayList<>();
//			ArrayList<String> EXP = new ArrayList<>();
//			ArrayList<String> GCM = new ArrayList<>();
//			ArrayList<String> RCM = new ArrayList<>();
//			ArrayList<String> VAR = new ArrayList<>();
//			ArrayList<String> YEARTYPE = new ArrayList<>();
//			loadFiles(supplyPath, demandPath, POP, EXP, GCM, RCM, VAR, YEARTYPE);
//			result.setPOP(POP);
//			result.setEXP(EXP);
//			result.setGCM(GCM);
//			result.setRCM(RCM);
//			result.setVAR(VAR);
//			result.setYEARTYPE(YEARTYPE);
//		}
//		return result;
//	}
//	
//	public void loadFiles(String supplyPath, String demandPath, ArrayList<String> POP, ArrayList<String> EXP, ArrayList<String> GCM, 
//			ArrayList<String> RCM, ArrayList<String> VAR, ArrayList<String> YEARTYPE){
//		LoadAllPossibleParams loadFile = new LoadAllPossibleParams(supplyPath, demandPath);
//		EXP.addAll(loadFile.getSupplyParams("EXP"));
//		GCM.addAll(loadFile.getSupplyParams("GCM"));
//		RCM.addAll(loadFile.getSupplyParams("RCM"));
//		VAR.addAll(loadFile.getSupplyParams("VAR"));
//		YEARTYPE.addAll(loadFile.getSupplyParams("YEARTYPE"));
//		POP.addAll(loadFile.getDemandParams());
//	}
//}
