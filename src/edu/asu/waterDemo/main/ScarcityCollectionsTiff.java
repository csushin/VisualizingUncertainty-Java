package edu.asu.waterDemo.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

@Path("/calcScarcityCollections")
public class ScarcityCollectionsTiff {
	
	private String linux_demandDir;
	private String linux_supplyDir;
	private String linux_collectionDir;
	private String linux_lowdemandDir;
	private String linux_lowsupplyDir;
	
	public class ScarcityCollectionsTiffBean{
		public boolean created;
	};
	
	@Context
	public void setServletContext(ServletContext context) {
		linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
		linux_supplyDir = "/work/asu/data/wsupply/BW_1km/"; 
		linux_collectionDir = "/work/asu/data/wuncertainty/collection/";
		linux_lowdemandDir = "/work/asu/data/wdemand/popden_pred/resampled_10km/";
		linux_lowsupplyDir = "/work/asu/data/wsupply/BW_10km/"; 
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public ScarcityCollectionsTiffBean query(@QueryParam("demandfName") @DefaultValue("null") String demandfName,
			@QueryParam("emissionType") @DefaultValue("null") String emission,
			@QueryParam("scenarioType") @DefaultValue("null") String scenario,
			@QueryParam("resolution") @DefaultValue("raw") String resolution){
		ScarcityCollectionsTiffBean result = new ScarcityCollectionsTiffBean();
		String resSupplyDir = "";
		String resdemandDir = "";
		if(resolution!="low"){
			resSupplyDir = linux_supplyDir;
			resdemandDir = linux_demandDir;
		}
		else{
			resSupplyDir = linux_lowsupplyDir;
			resdemandDir = linux_lowdemandDir;
		}
		String fileName = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_full_" + resolution + ".tif";
		resdemandDir = resdemandDir + demandfName;
		String outputfile = linux_collectionDir + fileName;
		File data = new File(outputfile);
		if(!data.exists()){

			if(resolution != "low")
			{
			
			}
			else{
				
			}
			ArrayList<File> supplyListOfFiles = new ArrayList<File>();
			String supplyDir = resSupplyDir + emission + "/";
			supplyListOfFiles = getAllFiles(supplyDir, supplyListOfFiles); 
			ArrayList<String> supplyPathList = new ArrayList<String>();
			for (int j = 0; j < supplyListOfFiles.size(); j++) {
				if (supplyListOfFiles.get(j).isFile()) {
					String supplyFiles = supplyListOfFiles.get(j).getName();
					if (supplyFiles.endsWith(".tif")  && supplyFiles.contains(scenario)) {
						String supplyPath = supplyListOfFiles.get(j).getAbsolutePath();
						supplyPathList.add(supplyPath);
					}
				}
			}
			if(createTiff(resdemandDir, supplyPathList, outputfile)){
				result.created = true;
			}
			else{
				System.out.println("creating file failed!");
			}	
		}
		else{
			result.created = true;
		}
		
		return result;
	}
	
	private boolean createTiff(String dPath, ArrayList<String> sPathList, String output){
		boolean created = false;
		if(sPathList.isEmpty())	created = false;
		int[] sSize = {7200, 3600};
		TiffParser dParser = new TiffParser();
		dParser.setFilePath(dPath);
		ArrayList<TiffParser> sParserArr = new ArrayList<TiffParser>();
		for(int i=0; i<sPathList.size(); i++){
			String curSupplyPath = sPathList.get(i);
//			System.out.println(curSupplyPath);
			long heapsize=Runtime.getRuntime().maxMemory();
//		    System.out.println("heapsize is::"+heapsize);
			TiffParser sParser = new TiffParser();
			sParser.setFilePath(curSupplyPath);
			if(sParser.parser())
				sParserArr.add(sParser);
			else {
				System.out.println("Error in parsing supply files!");
				created = false;
			}
		}
		double[] buf = new double[sSize[0]*sSize[1]];
		if(dParser.parser() && !sParserArr.isEmpty()){
			int srcWidth = (int) sSize[0];
			int srcHeight = (int) sSize[1];
			for(int h=0; h<srcHeight; h++){
				for(int w=0; w<srcWidth; w++){
					int index = h*srcWidth+w;
					double popVal = dParser.getData()[index];
					ArrayList<Integer> supplyValArr = new ArrayList<Integer>();
					for(int k=0; k<sParserArr.size(); k++){
						double curSupplyVal = sParserArr.get(k).getData()[index];
						double scarVal = 0;
						if(!Double.isNaN(curSupplyVal) && !Double.isNaN(popVal)){
							if(popVal>=1 && curSupplyVal>=0){
								scarVal =  curSupplyVal*1000/popVal;
							}
							else if(popVal<1 && popVal>=0 && curSupplyVal>=0){
								scarVal = 1701;
							}	
						}
						else{
							scarVal = -1;
						}
//						set the values of the scarcity by using 0/1/2/3 to represent AbScar/Scar/Stre/NoStre
						int flag;
						if(scarVal<=500 && scarVal>=0) {flag = 1;}
						else if(scarVal>500 && scarVal<=1000) {flag = 2;}
						else if(scarVal>1000 && scarVal<=1700) {flag = 3;}
						else if(scarVal>1700)	{flag = 4;}
						else {flag = 0; }//here we need to also consider the situation that water supply is NaN as it comes from the water model
						supplyValArr.add(flag);
						if(sPathList.get(k).contains("CNRM-CM5_CCLM")){
							buf[index] += flag*10000;
						}
						else if(sPathList.get(k).contains("EC-EARTH-r12_CCLM")){
							buf[index] += flag*1000;
						}
						else if(sPathList.get(k).contains("HadGEM2-ES_CCLM")){
							buf[index] += flag*100;
						}
						else if(sPathList.get(k).contains("MPI-ESM-LR_CCLM")){
							buf[index] += flag*10;
						}
						else{
							buf[index] += flag;
						}
					}
				}
			}
			Driver driver = gdal.GetDriverByName("GTiff");
			
			Dataset dst_ds = driver.Create(output, 7200, 3600, 1, gdalconst.GDT_Float64);
			dst_ds.SetGeoTransform(dParser.getGeoInfo());
			dst_ds.SetProjection(dParser.getProjRef());
			dst_ds.GetRasterBand(1).WriteRaster(0, 0, 7200, 3600, buf);
			created = true;
		}
		else{
			System.out.println("parse Error in path!");
			created = false;	
		}
		return created;
	}
	
	private ArrayList<File> getAllFiles(String directoryName, ArrayList<File> files) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile()) {
	            files.add(file);
	        } else if (file.isDirectory()) {
	        	getAllFiles(file.getAbsolutePath(), files);
	        }
	    }
	    return files;
	}
}
