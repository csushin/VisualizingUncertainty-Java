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

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.GlobalVariables;
import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/calcMultinomial")
public class CalcPrior {	
	public String linux_demandDir;
	public String linux_supplyDir;
	public String linux_prbDir;
	public static String PREDJOINT  = "predJoint";
	public static String PRIOR  = "prior";
	public static int INFINITE = -99999;
	public GlobalVariables globalVar;
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")){
			this.linux_demandDir = context.getRealPath("img/demand") + File.separatorChar;
			this.linux_prbDir = context.getRealPath("img/probability") + File.separatorChar;
			this.linux_supplyDir = context.getRealPath("img/supply") + File.separatorChar;
		}
		else{
			this.linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
			this.linux_prbDir = "/work/asu/data/multinomial/";
			this.linux_supplyDir = "/work/asu/data/wsupply/BW_1km/"; 
		}

	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public boolean query(@QueryParam("baseDemandfName") @DefaultValue("null") String baseDemandfName,
			@QueryParam("prdDemandfName") @DefaultValue("null") String prdDemandfName,
			@QueryParam("yearChain") @DefaultValue("null") String yearChain,
			@QueryParam("yearIndex") @DefaultValue("1") int yearIndex){
		int baseYear = Integer.valueOf(yearChain.split(",")[yearIndex-1]);
		int predYear = Integer.valueOf(yearChain.split(",")[yearIndex]);
//		if the requried prior and joint probability is already existed in the file
		String targetExistedTest = linux_prbDir + "priorAS_" + predYear + ".tif";
		String prvJtExistedPath = linux_prbDir + "priorPrvJt_" + predYear + ".tif";
		File targetFile = new File(targetExistedTest);
		File targetJointFile = new File(prvJtExistedPath);
		if(targetFile.exists() && targetJointFile.exists()){
			return true;
		}
		
		globalVar = new GlobalVariables();
		TiffParser basedemandParser = new TiffParser();
		TiffParser predictdemandParser = new TiffParser();
		basedemandParser.setFilePath(linux_demandDir + baseDemandfName);
		predictdemandParser.setFilePath(linux_demandDir + prdDemandfName);
		if (basedemandParser.parser() && predictdemandParser.parser()) {
			System.out.println("demand: " + basedemandParser.getSize()[0]);
			ArrayList<File> supplyListOfFiles = new ArrayList<File>();
			supplyListOfFiles = getAllFiles(linux_supplyDir, supplyListOfFiles);
			ArrayList<TiffParser> supplyParsers = new ArrayList<TiffParser>();
			
			supplyParsers = getSupplies(supplyListOfFiles, baseYear);
			
//			distribution of each category in the base year scarcity
			String StressPath, NSPath, ScarcPath, ASPath, totalPath, prvJtPath;
			globalVar.setGeoInfo(supplyParsers.get(0).getGeoInfo());
			globalVar.setProjInfo(supplyParsers.get(0).getProjRef());
			

			
//			check if the prior for the prediction year exists
			File StressFile = new File(linux_prbDir + "priorStr_" + baseYear + ".tif");
			if(!StressFile.exists()){
//			if it is the starter of the chain
				StressPath = NSPath = ScarcPath = ASPath = linux_prbDir + "prior_Initial.tif";
				totalPath = linux_prbDir + "priotToal_Initial.tif";
				globalVar.readPriorFile(ASPath, NSPath, StressPath, ScarcPath, totalPath, null);
				
				calcJointAndPrior("prior", basedemandParser, supplyParsers);
				ASPath = linux_prbDir + "priorAS_" + baseYear + ".tif";
				NSPath = linux_prbDir + "priorNS_" + baseYear + ".tif";
				ScarcPath = linux_prbDir + "priorSc_" + baseYear + ".tif";
				StressPath = linux_prbDir + "priorStr_" + baseYear + ".tif";
				totalPath = linux_prbDir + "priorTotal_" + baseYear + ".tif";
				globalVar.updatePriorData(ASPath, NSPath, StressPath, ScarcPath, totalPath, null);
			}
//			else start from the existed prior knowledge in the base year
			ASPath = linux_prbDir + "priorAS_" + baseYear + ".tif";
			NSPath = linux_prbDir + "priorNS_" + baseYear + ".tif";
			ScarcPath = linux_prbDir + "priorSc_" + baseYear + ".tif";
			StressPath = linux_prbDir + "priorStr_" + baseYear + ".tif";
			totalPath = linux_prbDir + "priorTotal_" + baseYear + ".tif";
			globalVar.readPriorFile(ASPath, NSPath, StressPath, ScarcPath, totalPath, null);
				
			supplyListOfFiles = new ArrayList<File>();
			supplyListOfFiles = getAllFiles(linux_supplyDir, supplyListOfFiles);
			supplyParsers = new ArrayList<TiffParser>();
			supplyParsers = getSupplies(supplyListOfFiles, predYear);
				
//			calculate the prediction joint probability and update the prior probability from the prediction years
			calcJointAndPrior("predJoint", predictdemandParser, supplyParsers);	
			ASPath = linux_prbDir + "priorAS_" + predYear + ".tif";
			NSPath = linux_prbDir + "priorNS_" + predYear + ".tif";
			ScarcPath = linux_prbDir + "priorSc_" + predYear + ".tif";
			StressPath = linux_prbDir + "priorStr_" + predYear + ".tif";
			totalPath = linux_prbDir + "priorTotal_" + predYear + ".tif";
			prvJtPath = linux_prbDir + "priorPrvJt_" + predYear + ".tif";
			globalVar.updatePriorData(ASPath, NSPath, StressPath, ScarcPath, totalPath, prvJtPath);
			
			return true;
		}
		else{
			return false;
		}
	}
	
	public void calcJointAndPrior(String calcType, TiffParser demandParser, ArrayList<TiffParser> SupplyParsers){
		traverseGeotiff(calcType, demandParser, SupplyParsers);
	}
	
	public double[] traverseGeotiff(String calcType, TiffParser demandParser, ArrayList<TiffParser> SupplyParsers){
		int deltaX = 0;
		int deltaY = 0;
		int tgtWidth = globalVar.targetWidth;
		int tgtHeight = globalVar.targetHeight;
		System.out.println("demand: " + demandParser.getSize()[0]);
		if (demandParser.getSize()[1] != SupplyParsers.get(0).getSize()[1]
				|| demandParser.getSize()[0] != SupplyParsers.get(0).getSize()[0]) {
			deltaX = (int) (demandParser.getSize()[1] - tgtWidth);
			deltaY = (int) (demandParser.getSize()[0] - tgtHeight);
		}
		System.out.println("delta X: " + deltaX);
		double min = 99999, max = -99999;
		double[] targetArr = new double[tgtHeight*tgtWidth];
		for (int h = 0; h < tgtHeight; h++) {
			for (int w = 0; w < tgtWidth; w++) {
				int tgtIndex = h * tgtWidth + w;
				int popIndex = (h + deltaY) * (tgtWidth + deltaX)
						+ (w + deltaX);
				double popVal = demandParser.getData()[popIndex];
				boolean nullFlag = false;
				double NSCount = 0, StrCount = 0, ScCount = 0, ASCount = 0;
				if(!calcType.equals("spatialKL")){
					for (int k = 0; k < SupplyParsers.size(); k++) {
						double scarVal = 0;
						double curSupplyVal = SupplyParsers.get(k).getData()[tgtIndex];
						if (!Double.isNaN(popVal) && !Double.isNaN(curSupplyVal)) {
							if (popVal >= 1 && curSupplyVal >= 0) {
								scarVal = curSupplyVal * 1000 / popVal;
							} else if (popVal < 1 && curSupplyVal >= 0) {
								scarVal = 1701;
							}
						} else {
							scarVal = -1;
						}

						// set the values of the scarcity by using 0/1/2/3
						// to represent AbScar/Scar/Stre/NoStre
						if (scarVal < 500 && scarVal >= 0) {
							ASCount++;
						} else if (scarVal >= 500 && scarVal < 1000) {
							ScCount++;
						} else if (scarVal >= 1000 && scarVal < 1700) {
							StrCount++;
						} else if (scarVal >= 1700) {
							NSCount++;
						} else {
							nullFlag = true;
						}// here we need to also consider the situation that
							// water supply is NaN as it comes from the
							// water model
					}
					if(calcType.equalsIgnoreCase(PRIOR)){
						calcDist(tgtIndex, NSCount, StrCount, ScCount, ASCount, SupplyParsers.size(), nullFlag);
					}
					else if(calcType.equalsIgnoreCase(PREDJOINT)){
						calcJointPr(tgtIndex, NSCount, StrCount, ScCount, ASCount, SupplyParsers.size(), nullFlag);
					}
					else if(calcType.equalsIgnoreCase("varNS")){
						targetArr[tgtIndex] = calcVarianceWithNaivePrior(NSCount, SupplyParsers.size());
					}
					else if(calcType.equalsIgnoreCase("prior")){
						targetArr[tgtIndex] = NSCount / 18.0;
					}
					else if(calcType.equalsIgnoreCase("varStr")){
						targetArr[tgtIndex] = calcVarianceWithNaivePrior(StrCount, SupplyParsers.size());
					}
					else if(calcType.equalsIgnoreCase("varAS")){
						targetArr[tgtIndex] = calcVarianceWithNaivePrior(ASCount, SupplyParsers.size());
					}
					else if(calcType.equalsIgnoreCase("priorAS")){
						targetArr[tgtIndex] = ASCount / (double)SupplyParsers.size();
					}
					if(targetArr[tgtIndex]<min && targetArr[tgtIndex]!=INFINITE)
						min = targetArr[tgtIndex];
					if(targetArr[tgtIndex]>max && targetArr[tgtIndex]!=INFINITE)
						max = targetArr[tgtIndex];
//						 if(P_Str_Arr[tgtIndex] != 0 || P_Sc_Arr[tgtIndex]!=0){
//							 System.out.println(" prediction self_Arr probability is: " +
//									 self_Arr[tgtIndex]);
//							 System.out.println(" prediction Situation is: " + " NS " +
//									 P_NS_Arr[tgtIndex] + " Str " + P_Str_Arr[tgtIndex] + " Sc " + P_Sc_Arr[tgtIndex] + " AS " + P_AS_Arr[tgtIndex]);
//							 System.out.println(" prediction FacCount is: " + facCount +
//									 " times Count is: " + timesCount);
//						 }
//					}					
				}
				else{
					
				}
				
			}
		}
		System.out.println("max is " + max + " min is: " + min);
		for (int j = 0; j < SupplyParsers.size(); j++) {
			SupplyParsers.get(j).close();
		}
		return targetArr;
	}
	
	public double calcVarianceWithNaivePrior(double count, double total){
		double prior = count / total;
		return prior*(1.0 - prior);
	}
	
	public void calcJointPr(int tgtIndex, double NSCount, double StrCount, double ScCount, double ASCount, int totalNum, boolean nullFlag){
		double P_NS = globalVar.getPriorNS(tgtIndex)/globalVar.getPriorTotal(tgtIndex), 
				P_Str = globalVar.getPriorStress(tgtIndex)/globalVar.getPriorTotal(tgtIndex), 
				P_Sc = globalVar.getPriorScarcity(tgtIndex)/globalVar.getPriorTotal(tgtIndex), 
				P_AS = globalVar.getPriorAS(tgtIndex)/globalVar.getPriorTotal(tgtIndex);
		if (nullFlag || P_NS == -1 
				|| globalVar.getPriorNS(tgtIndex)==INFINITE || globalVar.getPriorStress(tgtIndex) == INFINITE
				|| globalVar.getPriorScarcity(tgtIndex) == INFINITE || globalVar.getPriorAS(tgtIndex) == INFINITE) {
			globalVar.setPriorNS(tgtIndex, INFINITE);
			globalVar.setPriorScarcity(tgtIndex, INFINITE);
			globalVar.setPriorAS(tgtIndex, INFINITE);
			globalVar.setPriorStress(tgtIndex, INFINITE);
			globalVar.setPriorTotal(tgtIndex, INFINITE);
			globalVar.setPrvJoint(tgtIndex, INFINITE);
		} else {
			double timesCount = Math.pow(P_NS, NSCount)
					* Math.pow(P_Str, StrCount)
					* Math.pow(P_Sc, ScCount)
					* Math.pow(P_AS, ASCount);
			double facN = factorial(NSCount+StrCount+ScCount+ASCount);
			double facCount = facN
			/ (factorial(ASCount)
						* factorial(ScCount)
							* factorial(StrCount) * factorial(NSCount));
//			predict joint minus base joint probability
			double result = timesCount * facCount;
			
//			Calculte the joint probability of previous scenario
			double prev_timesCount = Math.pow(P_NS, globalVar.getPriorNS(tgtIndex))
					* Math.pow(P_Str, globalVar.getPriorStress(tgtIndex))
					* Math.pow(P_Sc, globalVar.getPriorScarcity(tgtIndex))
					* Math.pow(P_AS, globalVar.getPriorAS(tgtIndex));
			facN = factorial(globalVar.getPriorNS(tgtIndex)+globalVar.getPriorStress(tgtIndex)+globalVar.getPriorScarcity(tgtIndex)+globalVar.getPriorAS(tgtIndex));
			double prev_facCount = facN
					/ (factorial(globalVar.getPriorNS(tgtIndex))
					* factorial(globalVar.getPriorStress(tgtIndex))
					* factorial(globalVar.getPriorScarcity(tgtIndex)) 
					* factorial(globalVar.getPriorAS(tgtIndex)));
			double prev_result = prev_timesCount * prev_facCount;

//			update priors and joint probabilities
			globalVar.setPriorNS(tgtIndex, NSCount + globalVar.getPriorNS(tgtIndex));
			globalVar.setPriorAS(tgtIndex, ASCount + globalVar.getPriorAS(tgtIndex));
			globalVar.setPriorScarcity(tgtIndex, ScCount + globalVar.getPriorScarcity(tgtIndex));
			globalVar.setPriorStress(tgtIndex, StrCount + globalVar.getPriorStress(tgtIndex));
			globalVar.setPriorTotal(tgtIndex, totalNum + globalVar.getPriorTotal(tgtIndex));
			globalVar.setPrvJoint(tgtIndex, result/prev_result);
		}
	}
	
	
	public void calcDist(int tgtIndex, double NSCount, double StrCount, double ScCount, double ASCount, int totalNum, boolean nullFlag){
		if (nullFlag) {
			globalVar.setPriorNS(tgtIndex, INFINITE);
			globalVar.setPriorScarcity(tgtIndex, INFINITE);
			globalVar.setPriorAS(tgtIndex, INFINITE);
			globalVar.setPriorStress(tgtIndex, INFINITE);
			globalVar.setPriorTotal(tgtIndex, INFINITE);
		} else {
			double ns = NSCount + globalVar.getPriorNS(tgtIndex);
			double as = ASCount + globalVar.getPriorAS(tgtIndex);
			double sc = ScCount + globalVar.getPriorScarcity(tgtIndex);
			double str = StrCount + globalVar.getPriorStress(tgtIndex);
			double total = totalNum + globalVar.getPriorTotal(tgtIndex);
			globalVar.setPriorNS(tgtIndex, ns);
			globalVar.setPriorScarcity(tgtIndex, sc);
			globalVar.setPriorAS(tgtIndex, as);
			globalVar.setPriorStress(tgtIndex, str);
			globalVar.setPriorTotal(tgtIndex, total);
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
	
	public ArrayList<TiffParser> getSupplies(ArrayList<File> supplyListOfFiles, int year) {
		ArrayList<TiffParser> supplyParsers = new ArrayList<TiffParser>();
		for (int j = 0; j < supplyListOfFiles.size(); j++) {
			if (supplyListOfFiles.get(j).isFile()) {
				String supplyFiles = supplyListOfFiles.get(j).getName();
				if (supplyFiles.endsWith(".tif")) {
					if(!supplyFiles.contains("MPI-ESM-LR_CCLM") && !supplyFiles.contains("HadGEM2-ES_CCLM") && !supplyFiles.contains("EC-EARTH-r12_CCLM")
							&& !supplyFiles.contains("CNRM-CM5_CCLM") && !supplyFiles.contains("EC-EARTH-r3_HIRHAM") && supplyFiles.contains(String.valueOf(year))){
						String supplyPath = supplyListOfFiles.get(j).getAbsolutePath();
						TiffParser parser = new TiffParser();
						parser.setFilePath(supplyPath);
						if (parser.parser()) {
							supplyParsers.add(parser);
						} else {
							System.out.println("Cannot parse the file: " + supplyPath);
						}
					}
				}
			}
		}
		return supplyParsers;
	}
	
	public double factorial(double number) {
		if (number <= 1)
			return 1;
		else
			return number * factorial(number - 1);
	}
}
