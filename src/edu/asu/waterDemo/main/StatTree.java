package edu.asu.waterDemo.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.ArrayUtils;
import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.TiffParser;

public class StatTree {
	public class Treedata{
		int[] data;
		public double max;
		public double min;
	}
	
	public String uncertaintyDir;
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")){
			this.uncertaintyDir = context.getRealPath("img/uncertainty") + File.separatorChar;
		}
		else{
			this.uncertaintyDir = "/work/asu/data/wuncertainty/";
		}
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public Treedata query(
			@QueryParam("dx") @DefaultValue("1") int dx,
			@QueryParam("dy") @DefaultValue("1") int dy,
			@QueryParam("type") @DefaultValue("null") String type,
			@QueryParam("oldData") @DefaultValue("true") String oldData,
			@QueryParam("year") @DefaultValue("1960") String year,
			@QueryParam("demandfName") @DefaultValue("null") String demandfName,
			@QueryParam("emissionType") @DefaultValue("null") String emission,
			@QueryParam("scenarioType") @DefaultValue("null") String scenario) throws IOException{
		Treedata treedata = new Treedata();
		String path = this.uncertaintyDir + type + "/" + demandfName + "_" + year + "_" + emission + "_" + scenario + oldData + "_" + type+ ".tif"; 
		File file = new File(path);
		
		if(file.exists()){
			TiffParser tiffParser = new TiffParser();
			tiffParser.setFilePath(path);
			if(tiffParser.parser()){
				double width = tiffParser.getxSize();
				double height = tiffParser.getySize();
				int totalGridX = (int) (width/dx);
				int totalGridY = (int) (height/dy);
				int totalSerials = totalGridX * totalGridY;
				treedata.data = new int[totalGridX * totalGridY];
				int[] serialData = new int[totalGridX * totalGridY];
				int[] serialEffectNum = new int[totalGridX * totalGridY];
				if(width == 0 || height == 0)
					System.out.println("Error in reading sizes!");
				for(int x=0; x<width; x++){
					for(int y=0; y<height; y++){
						int xInd = x/dx;
						int yInd = y/dy;
						int totalSerialInd = xInd + yInd * totalGridX;
						int origInd = (int) (y * width + x);
						double origVal = tiffParser.getData()[origInd];
						if(origVal!=-1){
							serialData[totalSerialInd]+=origVal;
							serialEffectNum[totalSerialInd]+=1;
						}
							
					}
				}
				double max = 0, min = 999999;
				for(int i=0; i<totalSerials; i++){
					if(serialEffectNum[i]!=0)
						serialData[i] = serialData[i]/(serialEffectNum[i]);
					else
						serialData[i] = -1;
					if(max<serialData[i] && serialData[i] != -1)
						max = serialData[i];
					if(min>serialData[i] && serialData[i] != -1)
						min = serialData[i];
				}
				treedata.data = serialData;
				treedata.max = max;
				treedata.min = min;
				return treedata;
			}
			else{
				System.out.print("Cannot parse the statistics files!");
				return null;
			}
		}
		else{
			System.out.println("Cannot find the statistics files!");
			return null;
		}
		
			
	}
	
	
	public ArrayList<String> getAllSupplies(String demandPath, String supplyDir, String emission, String scenario, String oldData) throws IOException{
		ArrayList<File> supplyListOfFiles = new ArrayList<File>();
		supplyListOfFiles = getAllFiles(supplyDir, supplyListOfFiles); 
		ArrayList<String> supplyPathList = new ArrayList<String>();
		for (int j = 0; j < supplyListOfFiles.size(); j++) {
			if (supplyListOfFiles.get(j).isFile()) {
				String supplyFiles = supplyListOfFiles.get(j).getName();
				if (supplyFiles.endsWith(".tif")  && supplyFiles.contains(scenario) && supplyFiles.contains(emission)) {
					if(oldData.equalsIgnoreCase("old")){
						if(supplyFiles.contains("MPI-ESM-LR_CCLM") || supplyFiles.contains("HadGEM2-ES_CCLM") || supplyFiles.contains("EC-EARTH-r12_CCLM")
								|| supplyFiles.contains("CNRM-CM5_CCLM") || supplyFiles.contains("EC-EARTH-r3_HIRHAM")){
							String supplyPath = supplyListOfFiles.get(j).getAbsolutePath();
							supplyPathList.add(supplyPath);	
							System.out.println("one of the supply path is:" + supplyPath);
						}
					}
					else{
						if(!supplyFiles.contains("MPI-ESM-LR_CCLM") && !supplyFiles.contains("HadGEM2-ES_CCLM") && !supplyFiles.contains("EC-EARTH-r12_CCLM")
								&& !supplyFiles.contains("CNRM-CM5_CCLM") && !supplyFiles.contains("EC-EARTH-r3_HIRHAM")){
							String supplyPath = supplyListOfFiles.get(j).getAbsolutePath();
							supplyPathList.add(supplyPath);							
						}						
					}
					
				}
			}
		}
		return supplyPathList;
	}
	
	public ArrayList<File> getAllFiles(String directoryName, ArrayList<File> files) {
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
