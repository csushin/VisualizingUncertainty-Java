package edu.asu.waterDemo.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.ArrayUtils;
import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/MergeGridStat")
public class StatTree {
	public class Treedata{
		public double[] statData;
		public double max;
		public double min;
	}
	
	public String uncertaintyDir;
	private int NUMBER_OF_PROCESSORS = 8;
	
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")){
			this.uncertaintyDir = context.getRealPath("img/supply") + File.separatorChar;
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
			@QueryParam("filename") @DefaultValue("null") String filename) throws IOException{
		Treedata treedata = new Treedata();
		String path = this.uncertaintyDir + type + "/" + filename; 
//		String path = this.uncertaintyDir + filename; 
		File file = new File(path);
		
		if(file.exists()){
			TiffParser tiffParser = new TiffParser();
			tiffParser.setFilePath(path);
			if(tiffParser.parser()){
				double width = tiffParser.getxSize();
				double height = tiffParser.getySize();
				int totalGridX = (int) Math.floor(width/dx);
				int totalGridY = (int) Math.floor(height/dy);
				int totalSerials = totalGridX * totalGridY;
				treedata.statData = new double[totalSerials];
				double[] serialData = new double[totalSerials];
				double[] serialEffectNum = new double[totalSerials];
				if(width == 0 || height == 0)
					System.out.println("Error in reading sizes!");
				for(int y=0; y<height; y++){
					for(int x=0; x<width; x++){
						int origInd = (int) (y * width + x);
						double origVal = tiffParser.getData()[origInd];
						if(origVal!=-1 && !Double.isNaN(origVal)){
							double xInd = Math.floor(x/dx);
							double yInd = Math.floor(y/dy);
							if(y>=dy*totalGridY)
								yInd = totalGridY - 1;
							if(x>=dx*totalGridX)
								xInd = totalGridX - 1;
							double totalSerialInd = xInd + yInd * totalGridX;
							serialData[(int)totalSerialInd]+=origVal;
							serialEffectNum[(int)totalSerialInd]+=1;
						}
					}
				}
				System.out.print("Finished division!");
				double max = 0, min = 999999;
				for(int i=0; i<totalSerials; i++){
					if(serialEffectNum[i]!=0)
						treedata.statData[i] = serialData[i]/(serialEffectNum[i]);
					else
						treedata.statData[i] = Double.NaN;;
					if(max<treedata.statData[i] && !Double.isNaN(treedata.statData[i]))
						max = treedata.statData[i];
					if(min>treedata.statData[i] && !Double.isNaN(treedata.statData[i]))
						min = treedata.statData[i];
				}
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
