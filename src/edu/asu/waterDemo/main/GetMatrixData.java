package edu.asu.waterDemo.main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.TiffParser;


@Path("/getMatrixData")
public class GetMatrixData {
	private String basisDir;
	public String[] MODELNAME = {"CCCma-CanESM2_CCCma-CanRCM4",
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
			    "NOAA-GFDL-GFDL-ESM2M_SMHI-RCA4"
	};
	private int NUMBER_OF_PROCESSORS = MODELNAME.length;
	
	public class MatrixUnit{
		public String modelName;
		public int sampleIndex;
		public double z_score; 
	}
	
	public class GetMatrixDataBean{
		public ArrayList<MatrixUnit> data;
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
	
	// thread class 1
	public class GetModelStatsThread implements Runnable{
		private HashMap<String, double[]> values;
		private String[] indexSet;
		private String parserPath;
		private String modelName;
		
		public GetModelStatsThread(HashMap<String, double[]> values, String parserPath, String[] indexSet, String modelName){
			this.values = values;
			this.parserPath = parserPath;
			this.indexSet = indexSet;
			this.modelName = modelName;
		}
		
		@Override
		public void run() {
			TiffParser parser = new TiffParser(this.parserPath);
			double[] arr = this.values.get(this.modelName);
			// TODO Auto-generated method stub
			for(int i = 0; i<indexSet.length; i++){
				double value = parser.getData()[Integer.valueOf(indexSet[i])];
				if(value!=-1 && !Double.isNaN(value))
					arr[i] = value;
			}
			this.values.put(this.modelName, arr);
		}
	}
	
	// thread class 2
	public class CalcZScoreThread implements Runnable{
		private int startIndex;
		private int endIndex;
		private HashMap<String, double[]> values;
		private ArrayList<MatrixUnit> data;
		private TiffParser meanParser;
		private TiffParser stdParser;
		private String[] indicesStr;
		
		public CalcZScoreThread(int startIndex, int endIndex, String[] indicesStr, HashMap<String, double[]> values, ArrayList<MatrixUnit> data, TiffParser meanParser, TiffParser stdParser){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.values = values;
			this.data = data;
			this.meanParser = meanParser;
			this.stdParser = stdParser;
			this.indicesStr = indicesStr;
		}
		
		@Override
		public void run() {
			for(int i=startIndex; i<endIndex; i++){
				double mean = this.meanParser.getData()[Integer.valueOf(this.indicesStr[i])];
				double std = this.stdParser.getData()[Integer.valueOf(this.indicesStr[i])];
				Iterator it = this.values.entrySet().iterator();
			    while (it.hasNext()) {
			    	MatrixUnit unit = new MatrixUnit();
			        Map.Entry pair = (Map.Entry)it.next();
			        unit.modelName = (String) pair.getKey();
			        double[] values = (double[]) pair.getValue();
			        unit.sampleIndex = Integer.valueOf(this.indicesStr[i]);
			        unit.z_score = (values[i] - mean ) / std;
			        this.data.add(unit);
//			        it.remove(); // avoids a ConcurrentModificationException
			    }
			}
			
		}
		
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public GetMatrixDataBean query(
			@QueryParam("indices") @DefaultValue("null") String indices,
			@QueryParam("sourceDim") @DefaultValue("null") String sourceDim,
			@QueryParam("dataType") @DefaultValue("null") String dataType){
		GetMatrixDataBean result = new GetMatrixDataBean();
		String[] indicesStr = indices.split(",");
		String basis = "";
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		basis = this.basisDir + _dataType + "/" + sourceDim;
		GetModelStatsThread[] getModelStatsService = new GetModelStatsThread[NUMBER_OF_PROCESSORS];
		Thread[]  getModelStatsThread = new Thread[NUMBER_OF_PROCESSORS];
		HashMap<String, double[]> valuesPerModel = new HashMap<String, double[]>();
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			String path = getAllFiles(basis, MODELNAME[i]);
			double[] arrays = new double[indicesStr.length];
			valuesPerModel.put(MODELNAME[i], arrays);
			getModelStatsService[i] = new GetModelStatsThread(valuesPerModel, path, indicesStr, MODELNAME[i]);
			getModelStatsThread[i] = new Thread(getModelStatsService[i]);
			getModelStatsThread[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				getModelStatsThread[i].join();
				System.out.println("GetModelStat: " + i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String meanParserPath = this.basisDir + _dataType + "/EnsembleStatOf" + sourceDim + "/EnsembleMeanOf" + sourceDim + ".tif";
		TiffParser meanparser = new TiffParser(meanParserPath);
		String stdParserPath = this.basisDir + _dataType + "/EnsembleStatOf" + sourceDim + "/EnsembleStdOf" + sourceDim + ".tif";
		TiffParser stdparser = new TiffParser(stdParserPath);
		ArrayList<MatrixUnit> data = new ArrayList<MatrixUnit>();
		int countPerThread = indicesStr.length / NUMBER_OF_PROCESSORS;
		CalcZScoreThread[] calcZScorethread = new CalcZScoreThread[NUMBER_OF_PROCESSORS];
		Thread[] getCalcResultsThread = new Thread[NUMBER_OF_PROCESSORS];
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int startIndex = i*countPerThread;
			int endIndex = (i+1)*countPerThread;
			if(i == NUMBER_OF_PROCESSORS - 1)
				endIndex = indicesStr.length - 1;
			calcZScorethread[i] = new CalcZScoreThread(startIndex, endIndex, indicesStr, valuesPerModel, data, meanparser, stdparser);
			getCalcResultsThread[i] = new Thread(calcZScorethread[i]);
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				getCalcResultsThread[i].join();
				System.out.println("Zscore: " + i + " Finished~");
			}
		}catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		result.data = data;
		
		return result;
	}
	
	private String getAllFiles(String directoryName, String keyword) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile() && file.getName().endsWith(".tif") && file.getName().contains(keyword)) {
	        	return file.getAbsolutePath();
	        } 
	    }
	    return null;
	}
	
	
}
