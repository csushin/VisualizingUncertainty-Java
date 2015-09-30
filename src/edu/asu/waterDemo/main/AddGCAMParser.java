package edu.asu.waterDemo.main;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;


@Path("/addGCAMParser")
public class AddGCAMParser{
	public String netcdfPath;

	
	@Context
	public void setServletContext(ServletContext context){
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")){
			this.netcdfPath = context.getRealPath("img/netcdf") + File.separatorChar;
		}
		else{
			this.netcdfPath = "/work/asu/data/netcdf/";
		}
	}
	
	
	@GET
	@JSONP(queryParam = "callback", callback="eval")
	@Produces({"application/x-javascript"})
	public Map<String, Double> get(@QueryParam("southwest") @DefaultValue("0,0") String southwest,
			@QueryParam("northeast") @DefaultValue("0,0") String northeast,
			@QueryParam("fileName") @DefaultValue("") String filename,
			@QueryParam("year") @DefaultValue("2010") int year){
		Map<String, Double> element = new HashMap<String, Double>();
		int yearMin = 2010;
		int yearStep = 5;
		int yearIndex = (year - yearMin) / yearStep;
		double startLatNC = -89.75;
		double startLngNC = -179.75;
		double latstepNC = 0.5;
		double lngstepNC = 0.5;
		int latSize = 360;
		int lngSize = 720;
		String[] type = {"irrigation_demand", "livestock_demand", "electricity_demand", "mfg_demand", "domestic_demand", "total_demand"};
		this.netcdfPath = this.netcdfPath + filename + ".nc";
		NetcdfFile ncfile = null;
		try {
			ncfile = NetcdfDataset.openFile(this.netcdfPath, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		float[] netcdfDemand = new float[latSize*lngSize];
		int startLatIndNC = (int) Math.abs(Math.floor((Double.valueOf(southwest.split(",")[0]) - startLatNC) / latstepNC));//start from southwest
		int startLngIndNC = (int) Math.abs(Math.floor((Double.valueOf(southwest.split(",")[1]) - startLngNC) / lngstepNC));//start from southwest
		int startIndNC = startLatIndNC * lngSize + startLngIndNC;
		int endLatIndNC = (int) Math.abs(Math.floor((Double.valueOf(northeast.split(",")[0]) - startLatNC) / latstepNC));//end as northeast
		int endLngIndNC = (int) Math.abs(Math.floor((Double.valueOf(northeast.split(",")[1]) - startLngNC) / lngstepNC));//end as northeast
		int endIndNC = endLatIndNC * lngSize + endLngIndNC;
		
		for(int i=0; i<type.length; i++){
			netcdfDemand = getVariableArary(ncfile, yearIndex, type[i]);
			double num = 0;
			for(int j=startIndNC; j<endIndNC; j++){
				num+=netcdfDemand[j];
			}
			element.put(type[i], num);
		}
		
		return element;
	}
	
	public float[] getVariableArary(NetcdfFile ncfile, int yearIndex, String type){
		Variable v=ncfile.findVariable(type);
		Array data = null;
		try {
			data = v.read(yearIndex+",:,:");
		} catch (IOException e) {
			System.out.println("Exception thrown in reading netcdf File!");
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		} 
		Object temp=data.getStorage();
 		float[] res=(float[]) temp;
 		return res;
	}
}
