package edu.asu.waterDemo.main;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.glassfish.jersey.server.JSONP;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;


@Path("/calcGCAMScarcity")
public class CalcScarcityFromGCAM {
	String netcdfPath;
	String supplyPath;
	String scarcityPath;
	
	@Context
	public void setServletContext(ServletContext context){
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")){
			this.netcdfPath = context.getRealPath("img/netcdf") + File.separatorChar;
			this.supplyPath = context.getRealPath("img/supply") + File.separatorChar;
			this.scarcityPath = context.getRealPath("img/scarcity") + File.separatorChar;
		}
		else{
			this.netcdfPath = "/work/asu/data/netcdf/";
			this.supplyPath = "/work/asu/data/wsupply";
			this.scarcityPath = "/work/asu/data/wscarcity";
		}
		
	}

	@GET
	@JSONP(queryParam = "callback", callback="eval")
	@Produces({"application/x-javascript"})
	public boolean query(@QueryParam("year") @DefaultValue("2010") int year,
			@QueryParam("demandType") @DefaultValue("livestock") String dataType,
			@QueryParam("demandName") @DefaultValue("null") String demandName,
			@QueryParam("supplyName") @DefaultValue("none") String supplyName){
		this.netcdfPath = this.netcdfPath + demandName + ".nc";
		this.supplyPath = this.supplyPath + supplyName + ".tif";
		this.scarcityPath = this.scarcityPath + demandName + "_" + supplyName + ".tif";
		int yearMin = 2010;
		int yearStep = 5;
		int yearIndex = (year - yearMin) / yearStep;
		double startLatNC = -89.75;
		double startLngNC = -179.75;
		double latstepNC = 0.5;
		double lngstepNC = 0.5;
		int latSize = 360;
		int lngSize = 720;

		
//		 read netcdf file
		NetcdfFile ncfile = null;
		try {
			ncfile = NetcdfDataset.openFile(this.netcdfPath, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		float[] netcdfDemand = new float[latSize*lngSize];
		if(!dataType.equals("All")){
			String[] allType = {"irrigation_demand", "livestock_demand", "electricity_demand", "mfg_demand","domestic_demand"};
			for(String each : allType){
				float[] curDemand = new float[latSize*lngSize];
				curDemand = getVariableArary(ncfile, yearIndex, dataType);
				netcdfDemand = addArray(netcdfDemand, curDemand);
			}
		}
		else{
			netcdfDemand = getVariableArary(ncfile, yearIndex, dataType);
		}
		
// 		read geotiff file
 		TiffParser parser = new TiffParser();
 		parser.setFilePath(this.supplyPath);
 		if(parser.parser()){
 			double startLatTif = parser.getGeoInfo()[3];
 			double startLngTif = parser.getGeoInfo()[0];
 			int startLatIndNC = (int) Math.abs(Math.floor((startLatTif - startLatNC) / latstepNC));//start from 0
 			int startLngIndNC = (int) Math.abs(Math.floor((startLngTif - startLngNC) / lngstepNC));//start from 0
 			int startIndNC = startLatIndNC * lngSize + startLngIndNC;
 			int yStepRatio = (int)(Math.abs( Math.round(latstepNC/parser.getGeoInfo()[5])));
 			int xStepRatio = (int) (Math.abs(Math.round(lngstepNC/parser.getGeoInfo()[1])));
 			double[] tiffSize = parser.getSize();
 			double[] targetSct = new double[(int) (tiffSize[0]*tiffSize[1])];
 			for(int y=0; y<tiffSize[0]; y++){
 				for(int x=0; x<tiffSize[1]; x++){
 					int index = (int) (y*tiffSize[1] + x); 
 					double val = parser.getData()[index];
 					int curIndNC = (int) ((startIndNC-y/yStepRatio*lngSize) + x/xStepRatio);
 					double valNC = netcdfDemand[curIndNC];
 					if(!Double.isNaN(valNC) && !Double.isNaN(val)){
 						if(valNC!=0){
 							targetSct[index] = val*1000/(valNC/100);
 						}
 						else{
 							targetSct[index] = 1701;
 						}
 					}
 					else{
 						targetSct[index] = -1;
 					}
 				}
 			}
 			
 			Driver driver = gdal.GetDriverByName("GTiff");
			Dataset dst_ds = driver.Create(this.scarcityPath, (int)tiffSize[1], (int)tiffSize[0], 1, gdalconst.GDT_Float64);
			dst_ds.SetGeoTransform(parser.getGeoInfo());
			dst_ds.SetProjection(parser.getProjRef());
			int writeResult = dst_ds.GetRasterBand(1).WriteRaster(0, 0, (int)tiffSize[1], (int)tiffSize[0], targetSct);
			dst_ds.delete();
			System.out.println("Result for writing geotiff files: " + writeResult);	
 		}
 		
		
		return false;
	}
	
	
	public float[] getVariableArary(NetcdfFile ncfile, int yearIndex, String type){
		Variable v=ncfile.findVariable(type);
		Array data = null;
		try {
			data = v.read(yearIndex+",:,:");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		Object temp=data.getStorage();
 		float[] res=(float[]) temp;
 		return res;
	}
	
	public float[] addArray(float[] src1, float[] src2){
		if(src1.length == src2.length){
			int i=0;
			while(i<src1.length){
				src1[i] = src1[i] + src2[i];
				i++;
			}
			return src1;
		}
		else{
			System.out.println("two arrays are not equal length, they cann't be added!");
			return null;
		}
	}
}
