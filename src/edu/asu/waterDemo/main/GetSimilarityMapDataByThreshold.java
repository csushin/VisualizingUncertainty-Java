package edu.asu.waterDemo.main;

import java.io.File;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getSimilarityMapDataByMaxMin")
public class GetSimilarityMapDataByThreshold {
	private String basisDir;
	private int NUMBER_OF_PROCESSORS = 16;
	private String[] GCM = {"CCCma-CanESM2", "CNRM-CERFACS-CNRM-CM5", "CSIRO-QCCCE-CSIRO-Mk3-6-0", "ICHEC-EC-EARTH", "IPSL-IPSL-CM5A-MR", "MIROC-MIROC5", "MOHC-HadGEM2-ES", "MPI-M-MPI-ESM-LR", "NCC-NorESM1-M", "NOAA-GFDL-GFDL-ESM2M"};
	private String[] RCM = {"CCCma-CanRCM4", "SMHI-RCA4", "CLMcom-CCLM4-8-17", "DMI-HIRHAM5", "KNMI-RACMO22T"};
	private String[] modelSet = {"CCCma-CanESM2_CCCma-CanRCM4",
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
				    "NOAA-GFDL-GFDL-ESM2M_SMHI-RCA4"};
	
	public class GetSimilarityMapDataBean{
		public String imgStr;
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
	
	public class OpenTiffParserThread implements Runnable{
		private String path;
		private ArrayList<TiffParser> parsers;
		private String modelName;
		
		public OpenTiffParserThread(String path, String modelName, ArrayList<TiffParser> parsers){
			this.path = path;
			this.parsers = parsers;
			this.modelName = modelName;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			this.parsers.add(new TiffParser(path, this.modelName));
		}
	}
	
//	public class ExtractTopCountThread implements Runnable{
//		private double threshold;
//		private 
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			
//		}
//		
//	}
	
}
