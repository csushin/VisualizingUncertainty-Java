package edu.asu.waterDemo.main;

import java.awt.geom.Point2D;
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

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.GenerateTiles;
import edu.asu.waterDemo.commonclasses.LatLng;
import edu.asu.waterDemo.commonclasses.TiffParser;


@Path("/genUncertaintyTile")
public class GenerateUncertaintyTiles {
	
	public class imgBase64{
		public String[] imgStr = new String[3];
	}
	
	public String supplyDir;
	public String demandDir;
	public String agreeDir;
	public String disagreeDir;
	public String varianceDir;
	public String entropyDir;
	public String meanVarianceDir;
	public String meanEntropyDir;
	public String meanVotingsDir;
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")) {
			System.out.println("Tomcat webapp path is : " + context.getRealPath("img"));
			supplyDir = context.getRealPath("img/supply/BW_1km") + File.separatorChar;
			demandDir = context.getRealPath("img/demand") + File.separatorChar;
			agreeDir = context.getRealPath("img/supply") + File.separatorChar;// + "agree" + File.separatorChar; 
			disagreeDir = context.getRealPath("img/wuncertainty") + File.separatorChar + "disagree"+ File.separatorChar;
			varianceDir = context.getRealPath("img/supply") + File.separatorChar;// + "variance"+ File.separatorChar;
			entropyDir = context.getRealPath("img/supply") + File.separatorChar;// + "entropy"+ File.separatorChar;
			meanVarianceDir = context.getRealPath("img/supply") + File.separatorChar;// + "MeanVariance"+ File.separatorChar;
			meanEntropyDir = context.getRealPath("img/supply") + File.separatorChar;// + "MeanEntropy"+ File.separatorChar;
			meanVotingsDir = context.getRealPath("img/supply") + File.separatorChar;// + "MeanVotings"+ File.separatorChar;
		}
		else{
			demandDir = "/work/asu/data/wdemand/popden_pred/";
			supplyDir = "/work/asu/data/wsupply/BW_1km/"; 
			agreeDir = "/work/asu/data/wuncertainty/agree/"; 
			disagreeDir = "/work/asu/data/wuncertainty/disagree/";
			varianceDir = "/work/asu/data/wuncertainty/variance/";
			entropyDir = "/work/asu/data/wuncertainty/entropy/";
			meanVarianceDir = "/work/asu/data/wuncertainty/MeanVariance/";
			meanEntropyDir = "/work/asu/data/wuncertainty/MeanEntropy/";
			meanVotingsDir = "/work/asu/data/wuncertainty/MeanVotings/";
		}
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public imgBase64 query(
			@QueryParam("demandfName") @DefaultValue("null") String demandfName,
			@QueryParam("emissionType") @DefaultValue("null") String emission,
			@QueryParam("scenarioType") @DefaultValue("null") String scenario,
			@QueryParam("uncertaintyType") @DefaultValue("agree") String uncertaintyType,
			@QueryParam("oldData") @DefaultValue("old") String oldData,
			@QueryParam("mapPixelOrigin") @DefaultValue("0,0") String mapPixelOrigin,
			@QueryParam("zoomLevel") @DefaultValue("7") int zoomLevel) throws IOException {
		imgBase64 result = new imgBase64();
		
//		initialize the output dir, path ,and filenames
		int jobNum = uncertaintyType.split(",").length;
		String[] fileName = new String[jobNum];
		String[] outputfile = new String[jobNum];
		String[] outputDir = new String[jobNum];
		if(uncertaintyType.contains("agree")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("agree");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_" + zoomLevel + "_" + oldData +"_MeanVotings.png";
			outputfile[index] = this.meanVotingsDir + fileName[index];
			outputDir[index] = this.meanVotingsDir;
		}
		if(uncertaintyType.contains("variance")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("variance");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_" + zoomLevel + "_" + oldData +"_MeanVariance.png";
			outputfile[index] = this.meanVarianceDir + fileName[index];
			outputDir[index] = this.meanVarianceDir;	
		}
		if(uncertaintyType.contains("entropy")){
			int index = Arrays.asList(uncertaintyType.split(",")).indexOf("entropy");
			fileName[index] = demandfName.replace(".tif", "") + "_" + emission + "_" + scenario + "_" + zoomLevel + "_" + oldData +"_MeanEntropy.png";
			outputfile[index] = this.meanEntropyDir + fileName[index];
			outputDir[index] = this.meanEntropyDir;
		}
		
//		initialize the class GenerateTiles
		String demandPath = this.demandDir + demandfName;
		ArrayList<GenerateTiles> tiles = new ArrayList<GenerateTiles>();
		String[] mapPixelOriginArr = mapPixelOrigin.split(",");
		for(int i=0; i<outputfile.length; i++){
			Point2D mapPixelOriginPt = new Point2D.Double();
			mapPixelOriginPt.setLocation(Integer.valueOf(mapPixelOriginArr[0]), Integer.valueOf(mapPixelOriginArr[1]));
			GenerateTiles tile = new GenerateTiles(outputfile[i], mapPixelOriginPt, uncertaintyType.split(",")[i], zoomLevel);
			tiles.add(tile);				
		}
		
//		check if the png tiles are existed in the local disk and convert them to the base64 string
		boolean[] doneFlag = new boolean[jobNum];
		for(int i=0; i<jobNum; i++){
			if(checkIfTileExist(outputDir[i], fileName[i])){
				doneFlag[i] = true;
				String path = outputDir[i] + fileName[i];
				result.imgStr[i] = tiles.get(i).encodeFromReaderToBase64(path, "PNG");
				if(result.imgStr[i] == null){
//					Error in reading the png files, so we need to regenerate that file
					doneFlag[i] = false;
				}
			}
			else{
				result.imgStr[i] = null;
				doneFlag[i] = false;
			}
		}
		
		if(isAllTrue(doneFlag)){
			return result;
		}
		
//		generate new tiles
		ArrayList<String> supplyPathList = getAllSupplies(demandPath, this.supplyDir, emission, scenario, uncertaintyType, oldData);
		if(computeAndsave(demandPath, supplyPathList, outputfile, uncertaintyType, doneFlag, tiles, result, oldData)){
			return result;		
		}
		else{
			System.out.println("Can't create geotiff image!");	
			return null;
		}			
	}
	
	private boolean checkIfTileExist(String targetDir, String fileName) {
		ArrayList<File> allFiles = new ArrayList<File>();
		allFiles = this.getAllFiles(targetDir, allFiles); 
		for (int j = 0; j < allFiles.size(); j++) {
			if (allFiles.get(j).isFile()) {
				String eachFile = allFiles.get(j).getName();
				if(eachFile.contains(fileName)){
					return true;
				}
			}
		}
		return false;
	}

	public ArrayList<String> getAllSupplies(String demandPath, String supplyDir, String emission, String scenario, String uncertaintyType, String oldData) throws IOException{
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
	
	public boolean computeAndsave(String dPath, ArrayList<String> sPathList, String outputfile[], String uncertaintyType, boolean[] doneFlag, 
			ArrayList<GenerateTiles> tiles, imgBase64 result, String oldData) throws IOException{
		if(sPathList.isEmpty())	{
			System.out.println("supply path is empty, so it cannot compute and save!");
			return false;
		}
		System.out.println("demand path is:" + dPath);
		TiffParser dParser = new TiffParser();
		dParser.setFilePath(dPath);
		ArrayList<TiffParser> sParserArr = new ArrayList<TiffParser>();
		double[] sSize = {};
//		double[] dSize = dParser.getSize();
		for(int i=0; i<sPathList.size(); i++){
			String curSupplyPath = sPathList.get(i);
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
			LatLng southwest = new LatLng(sParserArr.get(0).getLrLatlng()[0], sParserArr.get(0).getUlLatlng()[1]);
			LatLng northeast = new LatLng(sParserArr.get(0).getUlLatlng()[0], sParserArr.get(0).getLrLatlng()[1]);
			tiles.get(i).processWidthHeight((int) sSize[1], (int) sSize[0], southwest, northeast);
			tiles.get(i).initializeBufferImage();
		}
		if(dParser.parser() && !sParserArr.isEmpty()){
			int tgtHeight = (int)sSize[0];
			int tgtWidth = (int)sSize[1];
			int deltaX = 0;
			int deltaY = 0;
			if(dParser.getSize()[0] != tgtHeight || dParser.getSize()[1] != tgtWidth){
				deltaX = (int) (dParser.getSize()[1] - tgtWidth);
				deltaY = (int) (dParser.getSize()[0] - tgtHeight);
			}
			System.out.println("dSize[0] is " + dParser.getxSize() + " dSize[1] is "+ dParser.getySize());
			int typeIndexAgree = Arrays.asList(uncertaintyType.split(",")).indexOf("agree");
			int typeIndexVar = Arrays.asList(uncertaintyType.split(",")).indexOf("variance");
			int typeIndexEnt = Arrays.asList(uncertaintyType.split(",")).indexOf("entropy");
			double latstepNC = 0.5;
			double lngstepNC = 0.5;
//			double ratio = 1.0;
			double ratio = 1/1000.0;
//			if(oldData.equalsIgnoreCase("new")){
//				ratio = Math.pow(10,9);
//			}
			System.out.println("Ratio is:" + ratio);
			for(int h=0; h<tgtHeight; h++){
				for(int w=0; w<tgtWidth; w++){
					int tgtIndex = h*tgtWidth+w;
					int popIndex = (h+deltaY)*(tgtWidth+deltaX) + (w+deltaX);
					double lat = sParserArr.get(0).getUlLatlng()[0] + h*sParserArr.get(0).getGeoInfo()[5];
					double lng = sParserArr.get(0).getUlLatlng()[1] + w*sParserArr.get(0).getGeoInfo()[1];
					double popVal = dParser.getData()[popIndex];
					ArrayList<Integer> supplyValArr = new ArrayList<Integer>();
					int sum = 0;
					boolean nullFlag = false;
					int nonNANCount  = 0;
					for(int k=0; k<sParserArr.size(); k++){
						double scarVal = 0;
						double curSupplyVal = sParserArr.get(k).getData()[tgtIndex];
						if(!Double.isNaN(popVal) && !Double.isNaN(curSupplyVal)){
							if(popVal>=1 && curSupplyVal>=0){
								scarVal = curSupplyVal/(popVal*ratio);
							}
							else if(popVal<1 && curSupplyVal>=0){
								scarVal = 1701;
							}		
						}
						else{
							scarVal = -1;
						}
						
//						set the values of the scarcity by using 0/1/2/3 to represent AbScar/Scar/Stre/NoStre
						int flag;
						if(scarVal<=500 && scarVal>=0) {flag = 1;sum+=flag;nonNANCount++;}
						else if(scarVal>500 && scarVal<=1000) {flag = 2;sum+=flag;nonNANCount++;}
						else if(scarVal>1000 && scarVal<=1700) {flag = 3;sum+=flag;nonNANCount++;}
						else if(scarVal>1700)	{flag = 4;sum+=flag;nonNANCount++;}
						else {flag = -1; nullFlag=true;}//here we need to also consider the situation that water supply is NaN as it comes from the water model
						supplyValArr.add(flag);
					}
					
					double mean = calcMean(nullFlag, sum, nonNANCount);//(double) (sum/(double)supplyValArr.size());
					if(uncertaintyType.contains("agree")){
						if(doneFlag[typeIndexAgree] == false){
							double votings = calcVotings(nullFlag, mean, supplyValArr, nonNANCount);
							bufferSet.get(typeIndexAgree)[tgtIndex] = votings;
							tiles.get(typeIndexAgree).drawTiles(mean, votings, tgtIndex, lat, lng);
						}
					}
					if(uncertaintyType.contains("variance")){
						if(doneFlag[typeIndexVar] == false){
							double variance = calcVariance(nullFlag, sum, supplyValArr, nonNANCount);
							bufferSet.get(typeIndexVar)[tgtIndex] = variance;
							tiles.get(typeIndexVar).drawTiles(mean, variance, tgtIndex, lat, lng);
						}	
					}
					if(uncertaintyType.contains("entropy")){
						if(doneFlag[typeIndexEnt] == false){
							double entropy = calcEntropy(nullFlag, sum, supplyValArr, nonNANCount);
							bufferSet.get(typeIndexEnt)[tgtIndex] = entropy;
							tiles.get(typeIndexEnt).drawTiles(mean, entropy, tgtIndex, lat, lng);
						}
					}
					
				}
			}
			for(int i=0; i<outputfile.length; i++){
				if(doneFlag[i]==false && result.imgStr[i] == null){
//					write geotiff files
//					Driver driver = gdal.GetDriverByName("GTiff");
//					Dataset dst_ds = driver.Create(outputfile[i].replace(".png", ".tif"), (int)tgtWidth, (int)tgtHeight, 1, gdalconst.GDT_Float64);
//					dst_ds.SetGeoTransform(sParserArr.get(i).getGeoInfo());
//					dst_ds.SetProjection(sParserArr.get(i).getProjRef());
//					double[] curBuffer = bufferSet.get(i);
//					int writingResult = dst_ds.GetRasterBand(1).WriteRaster(0, 0, (int)tgtWidth, (int)tgtHeight, curBuffer);
//					dst_ds.FlushCache();
//					dst_ds.delete();
//					System.out.println("Writing geotiff result is: " + writingResult);	
					
//					write png tiles from buffered img
					result.imgStr[i] = tiles.get(i).writeBufferImage();
				}
			}
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
	
	public double calcMean(boolean isNullExisted, double sum, int nonNANCount){
		if(nonNANCount == 0)
			return -1;
		else
			return sum/(double)nonNANCount;
	}
	
	
	public double calcVariance(boolean isNullExisted, double sum, ArrayList<Integer> supplyValArr, int nonNANCount){
		if(!isNullExisted){
			double mean = (double) (sum/(double)supplyValArr.size());
			double sqrsum = 0;
			double var = 0;
			for(int i=0; i<supplyValArr.size(); i++){
				if(Integer.valueOf(supplyValArr.get(i))!=-1){
					sqrsum+= Math.pow((Double.valueOf(supplyValArr.get(i))-mean), 2.0);
				}
			}
			var = sqrsum/(double)nonNANCount;	
			return var;
		}
		else
			return -1;
	}
	
	
	public double calcEntropy(boolean isNullExisted, double sum, ArrayList<Integer> supplyValArr, int nonNANCount){
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
//			Normalize the entropy, referred to http://www.endmemo.com/bio/shannonentropy.php
			entropy = -(occurences[0]*Math.log(occurences[0]) + occurences[1]*Math.log(occurences[1]) + occurences[2]*Math.log(occurences[2])
							+ occurences[3]*Math.log(occurences[3]))/Math.log(4.0);
			double MaxEntropy = Math.log(supplyValArr.size())/Math.log(4);
			return entropy/(MaxEntropy-0);
		}
		else{
			return -1;
		}		
	}
	
	
	public double calcVotings(boolean isNullExisted, double mean, ArrayList<Integer> supplyValArr, int nonNANCount){
		if(isNullExisted)	return -1;
		else{
			double votings = 0;
			for(int i=0; i<supplyValArr.size(); i++){
				if(Double.valueOf(supplyValArr.get(i)) == Math.round(mean))
					votings++;
			}
			double percent = votings/(double)nonNANCount;
			return percent;
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
