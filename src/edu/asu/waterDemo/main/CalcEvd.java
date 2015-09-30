package edu.asu.waterDemo.main;

import java.io.File;
import java.util.ArrayList;

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

import edu.asu.waterDemo.commonclasses.GlobalVariables;
import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/calcEvidence")
public class CalcEvd {
	public String linux_prbDir;
	public static int INFINITE = -99999;
	public GlobalVariables globalVar;
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")){
			this.linux_prbDir = context.getRealPath("img/probability") + File.separatorChar;
		}
		else{
			this.linux_prbDir = "/work/asu/data/multinomial/";
		}

	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public boolean query(
			@QueryParam("yearChain") @DefaultValue("null") String yearChain,
			@QueryParam("yearIndex") @DefaultValue("1") int yearIndex){
		int baseYear = Integer.valueOf(yearChain.split(",")[yearIndex-1]);
		int predYear = Integer.valueOf(yearChain.split(",")[yearIndex]);
//		if the requried prior and joint probability is already existed in the file
		String targetFilePath = linux_prbDir + "Evidence_" + predYear + ".tif";
		File targetFile = new File(targetFilePath);
		if(targetFile.exists()){
			return true;
		}
		
		String prvTotal = linux_prbDir + "priorTotal_" + baseYear + ".tif";
		TiffParser prvTotalParser = new TiffParser(prvTotal);
		String prvNSDist = linux_prbDir + "priorNS_" + baseYear + ".tif";
		TiffParser prvNSDistParser = new TiffParser(prvNSDist);
		String prvStrDist = linux_prbDir + "priorStr_" + baseYear + ".tif";
		TiffParser prvStrDistParser = new TiffParser(prvStrDist);
		String prvScDist = linux_prbDir + "priorSc_" + baseYear + ".tif";
		TiffParser prvScDistParser = new TiffParser(prvScDist);
		String prvASDist = linux_prbDir + "priorAS_" + baseYear + ".tif";
		TiffParser prvASDistParser = new TiffParser(prvASDist);
		
		String nowTotal = linux_prbDir + "priorTotal_" + predYear + ".tif";
		TiffParser nowTotalParser = new TiffParser(nowTotal);
		String nowNSDist = linux_prbDir + "priorNS_" + predYear + ".tif";
		TiffParser nowNSDistParser = new TiffParser(nowNSDist);
		String nowStrDist = linux_prbDir + "priorStr_" + predYear + ".tif";
		TiffParser nowStrDistParser = new TiffParser(nowStrDist);
		String nowScDist = linux_prbDir + "priorSc_" + predYear + ".tif";
		TiffParser nowScDistParser = new TiffParser(nowScDist);
		String nowASDist = linux_prbDir + "priorAS_" + predYear + ".tif";
		TiffParser nowASDistParser = new TiffParser(nowASDist);
		
		globalVar = new GlobalVariables();
		globalVar.setGeoInfo(nowASDistParser.getGeoInfo());
		globalVar.setProjInfo(nowASDistParser.getProjRef());
		int tgtWidth = globalVar.targetWidth;
		int tgtHeight = globalVar.targetHeight;
		int index = 0;
		double[] targetArr = new double[tgtWidth * tgtHeight];
		double min = -INFINITE, max = INFINITE;
		while(index < tgtWidth*tgtHeight){
			if(prvNSDistParser.getData()[index] == INFINITE) {
				targetArr[index] = INFINITE;
				index++;
				continue;
			}
			double left = factorial(prvTotalParser.getData()[index])/(factorial(nowTotalParser.getData()[index]));
			double rightNS = factorial(nowNSDistParser.getData()[index])/factorial(prvNSDistParser.getData()[index]);
			double rightStr = factorial(nowStrDistParser.getData()[index])/factorial(prvStrDistParser.getData()[index]);
			double rightSc = factorial(nowScDistParser.getData()[index])/factorial(prvScDistParser.getData()[index]);
			double rightAS = factorial(nowASDistParser.getData()[index])/factorial(prvASDistParser.getData()[index]);
			double right = rightNS * rightStr * rightSc * rightAS;
			targetArr[index] = left * right;
			if(targetArr[index]>max)
				max = targetArr[index];
			if(targetArr[index]<min)
				min = targetArr[index];
			index++;
		}
		System.out.println("max is " + max + " min is: " + min);
		Driver driver = gdal.GetDriverByName("GTiff");
		Dataset dst_ds = driver.Create(targetFilePath, tgtWidth, tgtHeight, 1, gdalconst.GDT_Float64);
		dst_ds.SetGeoTransform(prvTotalParser.getGeoInfo());
		dst_ds.SetProjection(prvTotalParser.getProjRef());
		int flag = dst_ds.GetRasterBand(1).WriteRaster(0, 0, tgtWidth, tgtHeight, targetArr);
		dst_ds.delete();
		System.out.println("Writing geotiff result is: " + flag);	
		return true;
	}
	
	public double factorial(double number) {
		if (number <= 1)
			return 1;
		else
			return number * factorial(number - 1);
	}
}
