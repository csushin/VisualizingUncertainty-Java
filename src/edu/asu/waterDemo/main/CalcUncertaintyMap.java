package edu.asu.waterDemo.main;

//import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
//import java.io.InputStreamReader;
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


@Path("/calcUncertainty")
public class CalcUncertaintyMap {
	public class CalcUncertaintyMapBean{
		public boolean created;
	};
	
	String supplyDir;
	String demandDir;
	String linux_supplyDir;
	String linux_demandDir;
	String linux_agreeDir;
	String linux_disagreeDir;
	String linux_varianceDir;
	String linux_entropyDir;
	String linux_meanDeviationDir;
	String linux_meanEntropyDir;
	String linux_meanVotingsDir;
	
	@Context
	public void setServletContext(ServletContext context) {
		supplyDir = context.getRealPath("img") + File.separatorChar;
		demandDir = context.getRealPath("img/demand") + File.separatorChar;
		linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
//		linux_supplyDir = "/work/asu/data/BW_HIST/"; 
		linux_supplyDir = "/work/asu/data/wsupply/BW_1km/"; 
		linux_agreeDir = "/work/asu/data/wuncertainty/agree/"; 
		linux_disagreeDir = "/work/asu/data/wuncertainty/disagree/";
		linux_varianceDir = "/work/asu/data/wuncertainty/variance/";
		linux_entropyDir = "/work/asu/data/wuncertainty/entropy/";
		linux_meanDeviationDir = "/work/asu/data/wuncertainty/MeanDeviation/";
		linux_meanEntropyDir = "/work/asu/data/wuncertainty/MeanEntropy/";
		linux_meanVotingsDir = "/work/asu/data/wuncertainty/MeanVotings/";
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public CalcUncertaintyMapBean query(
			@QueryParam("demandfName") @DefaultValue("null") String demandfName,
			@QueryParam("emissionType") @DefaultValue("null") String emission,
			@QueryParam("scenarioType") @DefaultValue("null") String scenario,
			@QueryParam("uncertaintyType") @DefaultValue("agree") String uncertaintyType,
			@QueryParam("resolution") @DefaultValue("raw") String resolution) throws IOException {
		CalcUncertaintyMapBean result = new CalcUncertaintyMapBean();
		
//		parameters for geoserver request
		String port = "8080";
		String ws = "niger_river";
		String style = "";
		boolean createFlag = false;
		String fileName = "";
		String outputfile = "";
		if(uncertaintyType.equalsIgnoreCase("agree")){
			fileName = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_agree.tif";
			outputfile = linux_agreeDir + fileName;
			style = "nr_wuncertainty_agree";
		}
		else if(uncertaintyType.equalsIgnoreCase("disagree")){
			fileName = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_disagree.tif";
			outputfile = linux_disagreeDir + fileName;
			style = "nr_wuncertainty_disagree";
		}
		else if(uncertaintyType.equalsIgnoreCase("deviation") || uncertaintyType.equalsIgnoreCase("variance")){
			fileName = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_variance.tif";
			outputfile = linux_varianceDir + fileName;
			style = "nr_wuncertainty_variance";			
		}
		else if(uncertaintyType.equalsIgnoreCase("entropy")){
			fileName = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_entropy.tif";
			outputfile = linux_entropyDir + fileName;
			style = "nr_wuncertainty_entropy";	
		}
		else if(uncertaintyType.equalsIgnoreCase("MeanDeviation")){
			fileName = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_MeanDeviation.tif";
			outputfile = linux_meanDeviationDir + fileName;
			style = "nr_wuncertainty_MeanDeviation";	
		}
		else if(uncertaintyType.equalsIgnoreCase("MeanEntropy")){
			fileName = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_MeanEntropy.tif";
			outputfile = linux_meanEntropyDir + fileName;
			style = "nr_wuncertainty_MeanEntropy";	
		}
		else if(uncertaintyType.equalsIgnoreCase("MeanVotings")){
			fileName = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_MeanVotings.tif";
			outputfile = linux_meanVotingsDir + fileName;
			style = "nr_wuncertainty_MeanVotings";	
		}
		
		
		GeoserverService geoserver = new GeoserverService(fileName.replace(".tif", ""), outputfile, port, ws, style);
		if(geoserver.isExistance()){
			result.created = true;
		}
		else{
//			delete data, delete coverage, delete layer
			if(geoserver.deleteAll()){
				createFlag = true;
			}
			else{
				result.created = false;
				System.out.println("Cannot delete coverage or layer or data!\n");
			}
//			create newdata
			if(createFlag){
				String demandPath = linux_demandDir + demandfName;
				if(generateTiff(demandPath, linux_supplyDir, emission, scenario, outputfile, uncertaintyType)){
//					result.created = true;
					System.out.println("Geotiff Created!");	
//					create coverage and layer
					if(geoserver.generateCoverage()){
						result.created = true;
						System.out.println("Coverage Created!");	
					}
					else{
						System.out.println("Error in create covarage and Layer!");
						result.created = false;
					}
				}
				else{
					result.created = false;
					System.out.println("Can't create geotiff image!");		
				}
			}
			else{
				result.created = false;
			}
		}
		return result;
	}
	
	public boolean generateTiff(String demandPath, String supplyDir, String emission, String scenario, String outputfile, String uncertaintyType) throws IOException{
		ArrayList<File> supplyListOfFiles = new ArrayList<File>();
		supplyListOfFiles = getAllFiles(supplyDir, supplyListOfFiles); 
		ArrayList<String> supplyPathList = new ArrayList<String>();
		for (int j = 0; j < supplyListOfFiles.size(); j++) {
			if (supplyListOfFiles.get(j).isFile()) {
				String supplyFiles = supplyListOfFiles.get(j).getName();
				if (supplyFiles.endsWith(".tif")  && supplyFiles.contains(scenario) && supplyFiles.contains(emission)) {
					String supplyPath = supplyListOfFiles.get(j).getAbsolutePath();
					supplyPathList.add(supplyPath);
				}
			}
		}
		if(computeAndsave(demandPath, supplyPathList, outputfile, emission, scenario, uncertaintyType)){
			return true;
		}
		else{
			System.out.println("Can't compute and Save data!");
			return false;
		}
	}
	
	public boolean computeAndsave(String dPath, ArrayList<String> sPathList, String outputfile, String emission, String scenario, String uncertaintyType) throws IOException{
		if(sPathList.isEmpty())	{
			System.out.println("supply path is empty, so it cannot compute and save!");
			return false;
		}
		TiffParser dParser = new TiffParser();
		dParser.setFilePath(dPath);
		ArrayList<TiffParser> sParserArr = new ArrayList<TiffParser>();
		double[] sSize = {};
		double[] dSize = dParser.getSize();
		for(int i=0; i<sPathList.size(); i++){
			String curSupplyPath = sPathList.get(i);
			boolean isExisted = new File(curSupplyPath).exists();
			if(isExisted)
				System.out.println(curSupplyPath + " Indeed exists " + sPathList.size() + " supplies !");
			else
				System.out.println(curSupplyPath + " Not exists!");
			System.out.println("the " + i + "th supply file is :" + curSupplyPath);
			long heapsize=Runtime.getRuntime().maxMemory();
		    System.out.println("heapsize is::"+heapsize);
			TiffParser sParser = new TiffParser();
			sParser.setFilePath(curSupplyPath);
			if(sParser.parser()){
				sParserArr.add(sParser);
				sSize = sParser.getSize();
			}
			else {
				System.out.println("Error in parsing supply files!");
				return false;
			}
		}
		double[] buf = new double[(int) (sSize[0]*sSize[1])];
		int index = 0;
		if(dParser.parser() && !sParserArr.isEmpty()){
			int tgtHeight = (int)sSize[0];
			int tgtWidth = (int)sSize[1];
			int deltaX = 0;
			int deltaY = 0;
			if(dParser.getxSize() != sParserArr.get(0).getxSize() || dParser.getySize() != sParserArr.get(0).getySize()){
				deltaX = dParser.getxSize() - sParserArr.get(0).getxSize();
				deltaY = dParser.getySize() - sParserArr.get(0).getySize();
			}
			for(int h=0; h<tgtHeight; h++){
				for(int w=0; w<tgtWidth; w++){
					int tgtIndex = h*tgtWidth+w;
					int popIndex = (h+deltaY)*(tgtWidth+deltaX) + (w+deltaX);
					double popVal = dParser.getData()[popIndex];
					ArrayList<Integer> supplyValArr = new ArrayList<Integer>();
					int sum = 0;
					boolean nullFlag = false;
					for(int k=0; k<sParserArr.size(); k++){
						double curSupplyVal = sParserArr.get(k).getData()[index];
						if(!Double.isNaN(curSupplyVal) && curSupplyVal<0){
							curSupplyVal = 0;
						}
						double scarVal = 0;
						if(!Double.isNaN(popVal)){
							if(popVal>=1){
								scarVal =  curSupplyVal*1000/popVal;
							}
							else if(popVal<1){
								scarVal = 1701;
							}								
						}
						else{
							scarVal = -1;
						}
//						set the values of the scarcity by using 0/1/2/3 to represent AbScar/Scar/Stre/NoStre
						int flag;
						if(scarVal<=500 && scarVal>=0) {flag = 1;sum+=flag;}
						else if(scarVal>500 && scarVal<=1000) {flag = 2;sum+=flag;}
						else if(scarVal>1000 && scarVal<=1700) {flag = 3;sum+=flag;}
						else if(scarVal>1700)	{flag = 4;sum+=flag;}
						else {flag = -1; nullFlag=true;}//here we need to also consider the situation that water supply is NaN as it comes from the water model
						supplyValArr.add(flag);
					}
					int maxNum = 0, maxIndex = 0;
					double mean = (double) (sum/(double)supplyValArr.size());
					if(uncertaintyType.equalsIgnoreCase("agree")){
						buf[tgtIndex] = calcVotings(nullFlag, sum, supplyValArr);
					}
					else if(uncertaintyType.equalsIgnoreCase("deviation") || uncertaintyType.equalsIgnoreCase("variance")){
						buf[tgtIndex] = calcDeviation(nullFlag, sum, supplyValArr);
					}
					else if(uncertaintyType.equalsIgnoreCase("entropy")){
						buf[tgtIndex] = calcEntropy(nullFlag, sum, supplyValArr);
					}
					else if(uncertaintyType.equalsIgnoreCase("disagree")){
						if(!nullFlag){
							Set<Integer> s = new HashSet<Integer>(supplyValArr);
							maxNum = s.size();		
							buf[tgtIndex] = maxNum;
						}
						else
							buf[tgtIndex] = -1;
					}
					else if(uncertaintyType.equalsIgnoreCase("MeanDeviation")){
						double deviation = calcDeviation(nullFlag, sum, supplyValArr);
						if(deviation!=-1){
							buf[tgtIndex] = Math.round(mean)*10.0 + deviation;
						}
						else{
							buf[tgtIndex] = -1;
						}
					}
					else if(uncertaintyType.equalsIgnoreCase("MeanEntropy")){
						double entropy = calcEntropy(nullFlag, sum, supplyValArr);
						if(entropy!=-1){
							buf[tgtIndex] = Math.round(mean)*10.0 + entropy;
						}
						else{
							buf[tgtIndex] = -1;
						}
					}
					else if(uncertaintyType.equalsIgnoreCase("MeanVotings")){
						double rate = calcVotingRates(nullFlag, sum, supplyValArr);
						if(rate!=-1){
							buf[tgtIndex] = Math.round(mean)*10.0 + rate;
						}
						else{
							buf[tgtIndex] = -1;
						}
					}
					else{
						System.out.println("current uncertainty type is " + uncertaintyType);
						System.out.println("No uncertainty type can match the input parameters!");
					}		
					if(buf[index]>0 && buf[index]!=40){
						System.out.println("demand Path is :" + dPath + " supply Path is: " + sPathList.get(0));
						System.out.println(" buf[index] is: " + buf[tgtIndex]);						
					}
				}
			}
			Driver driver = gdal.GetDriverByName("GTiff");
			
			Dataset dst_ds = driver.Create(outputfile, (int)tgtWidth, (int)tgtHeight, 1, gdalconst.GDT_Float64);
			dst_ds.SetGeoTransform(sParserArr.get(0).getGeoInfo());
			dst_ds.SetProjection(sParserArr.get(0).getProjRef());
			dst_ds.GetRasterBand(1).WriteRaster(0, 0, (int)tgtWidth, (int)tgtHeight, buf);
			dst_ds.delete();
			return true;
		}
		else{
			System.out.println("parse Error in path!");
			return false;	
		}
//		if(dParser.parser() && !sParserArr.isEmpty()){
//			int srcWidth = (int) sSize[0];
//			int srcHeight = (int) sSize[1];
//			double maxEntropy=0, minEntropy = 999999;
//			for(int h=0; h<srcHeight; h++){
//				for(int w=0; w<srcWidth; w++){
//					double lng = dParser.getGeoInfo()[0] + dParser.getGeoInfo()[1]*w;
//					double lat = dParser.getGeoInfo()[3] + dParser.getGeoInfo()[5]*h;
//					if(lat>=supplyStartPoint[0] && lat<=supplyEndPoint[0] && lng>=supplyStartPoint[1] && lng<=supplyStartPoint[1]){
//						index++;
//						double popVal = dParser.getData()[index];
//						ArrayList<Integer> supplyValArr = new ArrayList<Integer>();
//						int sum = 0;
//						boolean nullFlag = false;
//						for(int k=0; k<sParserArr.size(); k++){
//							double curSupplyVal = sParserArr.get(k).getData()[index];
//							if(!Double.isNaN(curSupplyVal) && curSupplyVal<0){
//								curSupplyVal = 0;
//							}
//							double scarVal = 0;
//							if(!Double.isNaN(popVal)){
//								if(popVal>=1){
//									scarVal =  curSupplyVal*1000/popVal;
//								}
//								else if(popVal<1){
//									scarVal = 1701;
//								}								
//							}
//							else{
//								scarVal = -1;
//							}
////							set the values of the scarcity by using 0/1/2/3 to represent AbScar/Scar/Stre/NoStre
//							int flag;
//							if(scarVal<=500 && scarVal>=0) {flag = 1;sum+=flag;}
//							else if(scarVal>500 && scarVal<=1000) {flag = 2;sum+=flag;}
//							else if(scarVal>1000 && scarVal<=1700) {flag = 3;sum+=flag;}
//							else if(scarVal>1700)	{flag = 4;sum+=flag;}
//							else {flag = -1; nullFlag=true;}//here we need to also consider the situation that water supply is NaN as it comes from the water model
//							supplyValArr.add(flag);
//						}
//						int maxNum = 0, maxIndex = 0;
//						double mean = (double) (sum/(double)supplyValArr.size());
//						if(uncertaintyType.equalsIgnoreCase("agree")){
//							buf[index] = calcVotings(nullFlag, sum, supplyValArr);
//						}
//						else if(uncertaintyType.equalsIgnoreCase("deviation") || uncertaintyType.equalsIgnoreCase("variance")){
//							buf[index] = calcDeviation(nullFlag, sum, supplyValArr);
//						}
//						else if(uncertaintyType.equalsIgnoreCase("entropy")){
//							buf[index] = calcEntropy(nullFlag, sum, supplyValArr);
//						}
//						else if(uncertaintyType.equalsIgnoreCase("disagree")){
//							if(!nullFlag){
//								Set<Integer> s = new HashSet<Integer>(supplyValArr);
//								maxNum = s.size();		
//								buf[index] = maxNum;
//							}
//							else
//								buf[index] = -1;
//						}
//						else if(uncertaintyType.equalsIgnoreCase("MeanDeviation")){
//							double deviation = calcDeviation(nullFlag, sum, supplyValArr);
//							if(deviation!=-1){
//								buf[index] = Math.round(mean)*10.0 + deviation;
//							}
//							else{
//								buf[index] = -1;
//							}
////							if(!nullFlag){
////								double mean = (double) (sum/(double)supplyValArr.size());
////								double sqrsum = 0;
////								double dev = 0;
////								for(int i=0; i<supplyValArr.size(); i++){
////									if(Integer.valueOf(supplyValArr.get(i))!=-1){
////										sqrsum+= Math.pow((Double.valueOf(supplyValArr.get(i))-mean), 2.0);
////									}
////								}
////								dev = Math.sqrt(sqrsum/5);	
////								meanArr[index] = mean;
////								varianceArr[index] = variance;
////								buf[index] = Math.round(mean)*10.0 + variance;
////							}
////							else{
////								meanArr[index] = -1;
////								varianceArr[index] = -1;
////							}
//						}
//						else if(uncertaintyType.equalsIgnoreCase("MeanEntropy")){
//							double entropy = calcEntropy(nullFlag, sum, supplyValArr);
//							if(entropy!=-1){
//								buf[index] = Math.round(mean)*10.0 + entropy;
//							}
//							else{
//								buf[index] = -1;
//							}
////							if(!nullFlag){
////								double mean = (double) (sum/(double)supplyValArr.size());
////								double[] occurences = new double[4];
////								double entropy = 0;
////								occurences[0] = (Collections.frequency(supplyValArr, 1))/(double)supplyValArr.size();
////								occurences[1] = (Collections.frequency(supplyValArr, 2))/(double)supplyValArr.size();
////								occurences[2] = (Collections.frequency(supplyValArr, 3))/(double)supplyValArr.size();
////								occurences[3] = (Collections.frequency(supplyValArr, 4))/(double)supplyValArr.size();
////								for(int i=0; i<occurences.length; i++){
////									if(occurences[i]==0)
////										occurences[i] = 1;
////								}
////								entropy = -(occurences[0]*Math.log(occurences[0]) + occurences[1]*Math.log(occurences[1]) + occurences[2]*Math.log(occurences[2])
////												+ occurences[3]*Math.log(occurences[3]))/Math.log(2.0);
////								buf[index] = Math.round(mean)*10.0 + entropy;
////								if(maxEntropy<entropy)
////									maxEntropy = entropy;
////								if(minEntropy>entropy)
////									minEntropy = entropy;
////								System.out.println("Maximum Entropy is: " + maxEntropy + " And the minimum Entropy is: " + minEntropy);
////							}
////							else{
////								buf[index] = -1;
////							}
//						}
//						else if(uncertaintyType.equalsIgnoreCase("MeanVotings")){
//							double rate = calcVotingRates(nullFlag, sum, supplyValArr);
//							if(rate!=-1){
//								buf[index] = Math.round(mean)*10.0 + rate;
//							}
//							else{
//								buf[index] = -1;
//							}
//						}
//						else{
//							System.out.println("current uncertainty type is " + uncertaintyType);
//							System.out.println("No uncertainty type can match the input parameters!");
//						}
//					}
//					else{
//						System.out.println("the start and end point of the data have some error!");
//					}
//
//				}
//			}
//
//			
//			Driver driver = gdal.GetDriverByName("GTiff");
//				
//			Dataset dst_ds = driver.Create(outputfile, (int)sSize[1], (int)sSize[0], 1, gdalconst.GDT_Float64);
//			dst_ds.SetGeoTransform(sParserArr.get(0).getGeoInfo());
//			dst_ds.SetProjection(sParserArr.get(0).getProjRef());
//			dst_ds.GetRasterBand(1).WriteRaster(0, 0, (int)sSize[1], (int)sSize[0], buf);
//			return true;				
//			
//
//		}

	}
	
	public double calcDeviation(boolean isNullExisted, double sum, ArrayList<Integer> supplyValArr){
		if(!isNullExisted){
			double mean = (double) (sum/(double)supplyValArr.size());
			double sqrsum = 0;
			double dev = 0;
			for(int i=0; i<supplyValArr.size(); i++){
				if(Integer.valueOf(supplyValArr.get(i))!=-1){
					sqrsum+= Math.pow((Double.valueOf(supplyValArr.get(i))-mean), 2.0);
				}
			}
			dev = Math.sqrt(sqrsum/5);	
			return dev;
		}
		else
			return -1;
	}
	
	public double calcEntropy(boolean isNullExisted, double sum, ArrayList<Integer> supplyValArr){
		if(!isNullExisted){
			double[] occurences = new double[4];
			double entropy = 0;
			occurences[0] = (Collections.frequency(supplyValArr, 1))/(double)supplyValArr.size();
			occurences[1] = (Collections.frequency(supplyValArr, 2))/(double)supplyValArr.size();
			occurences[2] = (Collections.frequency(supplyValArr, 3))/(double)supplyValArr.size();
			occurences[3] = (Collections.frequency(supplyValArr, 4))/(double)supplyValArr.size();
			for(int i=0; i<occurences.length; i++){
				if(occurences[i]==0)
					occurences[i] = 1;
			}
			entropy = -(occurences[0]*Math.log(occurences[0]) + occurences[1]*Math.log(occurences[1]) + occurences[2]*Math.log(occurences[2])
							+ occurences[3]*Math.log(occurences[3]))/Math.log(2.0);
			return entropy;
		}
		else{
			return -1;
		}		
	}
	
	public double calcVotings(boolean isNullExisted, double sum, ArrayList<Integer> supplyValArr){
		if(isNullExisted)	return -1;
		else{
			int[] occurences = new int[4];
			double maxNum=0, maxIndex=0;
			occurences[0] = Collections.frequency(supplyValArr, 1);
			occurences[1] = Collections.frequency(supplyValArr, 2);
			occurences[2] = Collections.frequency(supplyValArr, 3);
			occurences[3] = Collections.frequency(supplyValArr, 4);
			for(int i=0; i < occurences.length; i++){
				if(maxNum < occurences[i]){
					maxNum = occurences[i];
					maxIndex = i;
				}
			}
			double percent = maxNum/5.0;
			double votings = maxIndex*10.0 + percent*10.0;
			return votings;
		}		
	}
	
	public double calcVotingRates(boolean isNullExisted, double sum, ArrayList<Integer> supplyValArr){
		if(isNullExisted)	return -1;
		else{
			int[] occurences = new int[4];
			double maxNum=0, maxIndex=0;
			occurences[0] = Collections.frequency(supplyValArr, 1);
			occurences[1] = Collections.frequency(supplyValArr, 2);
			occurences[2] = Collections.frequency(supplyValArr, 3);
			occurences[3] = Collections.frequency(supplyValArr, 4);
			for(int i=0; i < occurences.length; i++){
				if(maxNum < occurences[i]){
					maxNum = occurences[i];
					maxIndex = i;
				}
			}
			double rate = maxNum/5.0;
			return rate;
		}		
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
