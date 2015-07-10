package edu.asu.waterDemo.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.bean.GetGcmRcmNameBean;


@Path("/getGcmRcmName")
public class GetGcmRcmName {

	
	String supplyDir;
	String demandDir;
	String linux_demandDir;
	String linux_supplyDir;
	
	@Context
	public void setServletContext(ServletContext context) {
		supplyDir = context.getRealPath("img/supply720x360") + File.separatorChar;
		demandDir = context.getRealPath("img/demand/popden_pred/720x360") + File.separatorChar;
		linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
		linux_supplyDir = "/work/asu/data/wsupply/BW/"; 
	}
	
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public GetGcmRcmNameBean query(@QueryParam("message") @DefaultValue("FALSE") String message,
			@QueryParam("EXP") @DefaultValue("FALSE") String EXP,
			@QueryParam("VAR") @DefaultValue("FALSE") String VAR,
			@QueryParam("YEARTYPE") @DefaultValue("FALSE") String YEARTYPE,
			@QueryParam("POP") @DefaultValue("FALSE") String POP,
			@QueryParam("gRate") @DefaultValue("FALSE") String gRate,
			@QueryParam("yearVal") @DefaultValue("FALSE") String waterYVal) {
		GetGcmRcmNameBean result = new GetGcmRcmNameBean();
		String supplyPath = supplyDir+EXP;
		
		System.out.println(supplyPath);
		if(message.equals("searchGcmRcm"))
		{
			String[] _baseSName = new String[3];
			_baseSName[0] = EXP;
			_baseSName[1] = VAR;
			_baseSName[2] = YEARTYPE;
			result.setsfNames(getAllNames(_baseSName, supplyPath));
			String[] _baseDName = new String[3];
			if(POP.equals("EXP")){
				_baseDName[0] = POP;
				_baseDName[1] = gRate+"pct";
				_baseDName[2] = waterYVal;
			}
			else{
				_baseDName[0] = "density";
				_baseDName[1] = POP;
				_baseDName[2] = waterYVal;
			}
			result.setDfNames(getAllNames(_baseDName, demandDir));
		}
		else {
			result = null;
		}
		return result;
	}
	
	public ArrayList<String[]> getAllNames(String[] _baseName, String dir){
		ArrayList<String[]> gcmrcmCombination = new ArrayList<String[]>();
		String files;
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles(); 
		 
		for (int i = 0; i < listOfFiles.length; i++) 
		{
			if (listOfFiles[i].isFile()) 
			{
				files = listOfFiles[i].getName();
				if (files.endsWith(".tif") || files.endsWith(".TIF"))
				{
					String _files = files.replace(".tif", "");
					String[] parts = _files.split("_");
					boolean contain = Arrays.asList(parts).containsAll(Arrays.asList(_baseName));
					if(contain){
						Arrays.asList(parts).remove(_baseName);
						gcmrcmCombination.add(parts);
					}
				}
			}
		}
		return gcmrcmCombination;
	}
}
