package edu.asu.waterDemo.main;

//import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
//import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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


@Path("/calcUncertaintyOnce")
public class CalcUncertaintyOnce {
	
	String supplyDir;
	String demandDir;
	String agreeDir;
	String disagreeDir;
	String varianceDir;
	String entropyDir;
	String meanDeviationDir;
	String meanEntropyDir;
	String meanVotingsDir;
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")) {
			System.out.println("Tomcat webapp path is : " + context.getRealPath("img"));
			supplyDir = context.getRealPath("img/supply") + File.separatorChar;
			demandDir = context.getRealPath("img/demand") + File.separatorChar;
			agreeDir = context.getRealPath("img/supply") + File.separatorChar;// + "agree" + File.separatorChar; 
			disagreeDir = context.getRealPath("img/wuncertainty") + File.separatorChar + "disagree"+ File.separatorChar;
			varianceDir = context.getRealPath("img/supply") + File.separatorChar;// + "variance"+ File.separatorChar;
			entropyDir = context.getRealPath("img/supply") + File.separatorChar;// + "entropy"+ File.separatorChar;
			meanDeviationDir = context.getRealPath("img/wuncertainty") + File.separatorChar + "MeanDeviation"+ File.separatorChar;
			meanEntropyDir = context.getRealPath("img/wuncertainty") + File.separatorChar + "MeanEntropy"+ File.separatorChar;
			meanVotingsDir = context.getRealPath("img/wuncertainty") + File.separatorChar + "MeanVotings";
		}
		else{
			demandDir = "/work/asu/data/wdemand/popden_pred/";
			supplyDir = "/work/asu/data/wsupply/"; 
			agreeDir = "/work/asu/data/wuncertainty/agree/"; 
			disagreeDir = "/work/asu/data/wuncertainty/disagree/";
			varianceDir = "/work/asu/data/wuncertainty/variance/";
			entropyDir = "/work/asu/data/wuncertainty/entropy/";
			meanDeviationDir = "/work/asu/data/wuncertainty/MeanDeviation/";
			meanEntropyDir = "/work/asu/data/wuncertainty/MeanEntropy/";
			meanVotingsDir = "/work/asu/data/wuncertainty/MeanVotings/";
		}
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public boolean query(
			@QueryParam("demandfName") @DefaultValue("null") String demandfName,
			@QueryParam("emissionType") @DefaultValue("null") String emission,
			@QueryParam("scenarioType") @DefaultValue("null") String scenario,
			@QueryParam("uncertaintyType") @DefaultValue("agree") String uncertaintyType,
			@QueryParam("oldData") @DefaultValue("true") String oldData,
			@QueryParam("resolution") @DefaultValue("raw") String resolution) throws IOException {
		boolean result = false;
		
//		parameters for geoserver request
		String port = "8080";
		String ws = "niger_river";
//		String style = "";
		boolean createFlag = false;
//		String fileName = "";
//		String outputfile = "";
		int jobNum = uncertaintyType.split(",").length;
		String[] style = new String[jobNum];
		String[] fileName = new String[jobNum];
		String[] outputfile = new String[jobNum];
		if(uncertaintyType.contains("agree")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("agree");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_agree.tif";
			outputfile[index] = agreeDir + fileName[index];
			style[index] = "nr_wuncertainty_agree";
		}
		if(uncertaintyType.contains("disagree")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("disagree");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_disagree.tif";
			outputfile[index] = disagreeDir + fileName[index];
			style[index] = "nr_wuncertainty_disagree";
		}
		if(uncertaintyType.contains("deviation") || uncertaintyType.contains("variance")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("variance");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_variance.tif";
			outputfile[index] = varianceDir + fileName[index];
			style[index] = "nr_wuncertainty_variance";	
		}
		if(uncertaintyType.contains("entropy")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("entropy");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_entropy.tif";
			outputfile[index] = entropyDir + fileName[index];
			style[index] = "nr_wuncertainty_entropy";	
		}
		if(uncertaintyType.contains("MeanDeviation")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("MeanDeviation");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_MeanDeviation.tif";
			outputfile[index] = meanDeviationDir + fileName[index];
			style[index] = "nr_wuncertainty_MeanDeviation";	
		}
		if(uncertaintyType.contains("MeanEntropy")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("MeanEntropy");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_MeanEntropy.tif";
			outputfile[index] = meanEntropyDir + fileName[index];
			style[index] = "nr_wuncertainty_MeanEntropy";	
		}
		if(uncertaintyType.contains("MeanVotings")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("MeanVotings");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_MeanVotings.tif";
			outputfile[index] = meanVotingsDir + fileName[index];
			style[index] = "nr_wuncertainty_MeanVotings";	
		}
		
		ArrayList<GeoserverService> geoserverSet = new ArrayList<GeoserverService>();
		boolean[] doneFlag = new boolean[jobNum];
		boolean[] deletedFlag = new boolean[jobNum];
		for(int i=0; i<jobNum; i++){
			GeoserverService geoserver = new GeoserverService(fileName[i].replace(".tif", ""), outputfile[i], port, ws, style[i]);
			geoserverSet.add(geoserver);
			if(geoserver.isExistance()){
				doneFlag[i] = true;
			}
			else{
				doneFlag[i] = false;
				if(geoserver.deleteAll()){
					deletedFlag[i] = true;
				}
				else{
					deletedFlag[i] = false;
					System.out.println("Cannot delete coverage or layer or data of " + i + "\n");
					return doneFlag[i];
				}
			}
		}
		String demandPath = this.demandDir + demandfName;
		if(isAllTrue(doneFlag)){
			return true;
		}
		else{
			if(generateTiff(demandPath, this.supplyDir, emission, scenario, outputfile, uncertaintyType, oldData, doneFlag)){
				for(int i=0; i<outputfile.length; i++){
					if(doneFlag[i]==false){
						if(!geoserverSet.get(i).generateCoverage()){
							System.out.println("Error in create covarage and Layer!");
							return false;
						}
					}
				}			
			}
			else{
				System.out.println("Can't create geotiff image!");	
				return false;
			}			
		}

		
		return true;
	}
	
	public boolean generateTiff(String demandPath, String supplyDir, String emission, String scenario, String[] outputfile, String uncertaintyType, String oldData, boolean[] doneFlag) throws IOException{
		ArrayList<File> supplyListOfFiles = new ArrayList<File>();
		supplyListOfFiles = getAllFiles(supplyDir, supplyListOfFiles); 
		ArrayList<String> supplyPathList = new ArrayList<String>();
		for (int j = 0; j < supplyListOfFiles.size(); j++) {
			if (supplyListOfFiles.get(j).isFile()) {
				String supplyFiles = supplyListOfFiles.get(j).getName();
				if (supplyFiles.endsWith(".tif")  && supplyFiles.contains(scenario) && supplyFiles.contains(emission)) {
					if(oldData.equals("true")){
						if(supplyFiles.contains("MPI-ESM-LR_CCLM") || supplyFiles.contains("HadGEM2-ES_CCLM") || supplyFiles.contains("EC-EARTH-r12_CCLM")
								|| supplyFiles.contains("CNRM-CM5_CCLM") || supplyFiles.contains("EC-EARTH-r3_HIRHAM")){
							String supplyPath = supplyListOfFiles.get(j).getAbsolutePath();
							supplyPathList.add(supplyPath);							
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
		if(computeAndsave(demandPath, supplyPathList, outputfile, emission, scenario, uncertaintyType, doneFlag)){
			return true;
		}
		else{
			System.out.println("Can't compute and Save data!");
			return false;
		}
	}
	
	public boolean computeAndsave(String dPath, ArrayList<String> sPathList, String outputfile[], String emission, String scenario, String uncertaintyType, boolean[] doneFlag) throws IOException{
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
		ArrayList<double[]> bufferSet = new ArrayList<double[]>();
		for(int i=0; i<outputfile.length; i++){
				double[] buf = new double[(int) (sSize[0]*sSize[1])];
				bufferSet.add(buf);	
		}
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
						double scarVal = 0;
						double curSupplyVal = sParserArr.get(k).getData()[tgtIndex];
						if(!Double.isNaN(popVal) && !Double.isNaN(curSupplyVal)){
							if(popVal>=1 && curSupplyVal>=0){
								scarVal = curSupplyVal*1000/popVal;
							}
							else if(popVal<1 && curSupplyVal>=0){
								scarVal = 1701;
							}		
						}
						else{
							scarVal = -1;
						}
						
//						if(!Double.isNaN(curSupplyVal) && curSupplyVal<0){
//							curSupplyVal = 0;
//						}
//						if(!Double.isNaN(popVal)){
//							if(popVal>=1){
//								scarVal =  curSupplyVal*1000/popVal;
//							}
//							else if(popVal<1){
//								scarVal = 1701;
//							}								
//						}
//						else{
//							scarVal = -1;
//						}
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
					if(uncertaintyType.contains("agree")){
						int typeIndex = Arrays.asList(uncertaintyType.split(",")).indexOf("agree");
						if(doneFlag[typeIndex] == false){
							bufferSet.get(typeIndex)[tgtIndex] = calcVotings(nullFlag, sum, supplyValArr);
						}
					}
					if(uncertaintyType.contains("deviation") || uncertaintyType.contains("variance")){
						int typeIndex = Arrays.asList(uncertaintyType.split(",")).indexOf("variance");
						if(doneFlag[typeIndex] == false){
							bufferSet.get(typeIndex)[tgtIndex] = calcDeviation(nullFlag, sum, supplyValArr);
						}	
					}
					if(uncertaintyType.contains("entropy")){
						int typeIndex = Arrays.asList(uncertaintyType.split(",")).indexOf("entropy");
						if(doneFlag[typeIndex] == false){
							bufferSet.get(typeIndex)[tgtIndex] = calcEntropy(nullFlag, sum, supplyValArr);
						}
					}
					if(uncertaintyType.contains("disagree")){
						int typeIndex = Arrays.asList(uncertaintyType.split(",")).indexOf("disagree");
						if(doneFlag[typeIndex] == false){
							if(!nullFlag){
								Set<Integer> s = new HashSet<Integer>(supplyValArr);
								maxNum = s.size();		
								bufferSet.get(typeIndex)[tgtIndex] = maxNum;
							}
							else
								bufferSet.get(typeIndex)[tgtIndex] = -1;
						}
						
					}
					if(uncertaintyType.contains("MeanDeviation")){
						int typeIndex = Arrays.asList(uncertaintyType.split(",")).indexOf("MeanDeviation");
						if(doneFlag[typeIndex] == false){
							double deviation = calcDeviation(nullFlag, sum, supplyValArr);
							if(deviation!=-1){
								bufferSet.get(typeIndex)[tgtIndex] = Math.round(mean)*10.0 + deviation;
							}
							else{
								bufferSet.get(typeIndex)[tgtIndex] = -1;
							}	
						}
						
					}
					if(uncertaintyType.contains("MeanEntropy")){
						int typeIndex = Arrays.asList(uncertaintyType.split(",")).indexOf("MeanDeviation");
						if(doneFlag[typeIndex] == false){
							double entropy = calcEntropy(nullFlag, sum, supplyValArr);
							if(entropy!=-1){
								bufferSet.get(typeIndex)[tgtIndex] = Math.round(mean)*10.0 + entropy;
							}
							else{
								bufferSet.get(typeIndex)[tgtIndex] = -1;
							}
						}
					}
					if(uncertaintyType.contains("MeanVotings")){
						int typeIndex = Arrays.asList(uncertaintyType.split(",")).indexOf("MeanDeviation");
						if(doneFlag[typeIndex] == false){
							double rate = calcVotingRates(nullFlag, sum, supplyValArr);
							if(rate!=-1){
								bufferSet.get(typeIndex)[tgtIndex] = Math.round(mean)*10.0 + rate;
							}
							else{
								bufferSet.get(typeIndex)[tgtIndex] = -1;
							}
						}
					}
				}
			}
//			for(int i=0; i<tgtWidth*tgtHeight; i++){
//				for(int j=0; j<outputfile.length; j++){
//					if(bufferSet.get(j)[i]!=0 && bufferSet.get(j)[i]!=1 && bufferSet.get(j)[i]%10!=0){
//						System.out.println("the " + i + "th buffer value is " + bufferSet.get(j)[i]);
//					}
//				}
//			}
			
//			for(int i=0; i<outputfile.length; i++){
//				if(doneFlag[i]==false){
					Driver driver = gdal.GetDriverByName("GTiff");
					Dataset dst_ds = driver.Create(outputfile[0], (int)tgtWidth, (int)tgtHeight, 1, gdalconst.GDT_Float64);
					dst_ds.SetGeoTransform(sParserArr.get(0).getGeoInfo());
					dst_ds.SetProjection(sParserArr.get(0).getProjRef());
					double[] curBuffer = bufferSet.get(0);
					int result = dst_ds.GetRasterBand(1).WriteRaster(0, 0, (int)tgtWidth, (int)tgtHeight, curBuffer);
					System.out.println("Writing geotiff result is: " + result);		
					
//					Driver driver1 = gdal.GetDriverByName("GTiff");
//					Dataset dst_ds1 = driver1.Create(outputfile[1], (int)tgtWidth, (int)tgtHeight, 1, gdalconst.GDT_Float64);
//					dst_ds1.SetGeoTransform(sParserArr.get(0).getGeoInfo());
//					dst_ds1.SetProjection(sParserArr.get(0).getProjRef());
//					double[] curBuffer1 = bufferSet.get(1);
//					int result1 = dst_ds.GetRasterBand(1).WriteRaster(0, 0, (int)tgtWidth, (int)tgtHeight, curBuffer1);
//					System.out.println("Writing geotiff result is: " + result1);	
//				}
//			}
			return true;
		}
		else{
			System.out.println("parse Error in path!");
			return false;	
		}
	}

	public static boolean isAllTrue(boolean[] array)
	{
	    for(boolean b : array) if(!b) return false;
	    return true;
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
