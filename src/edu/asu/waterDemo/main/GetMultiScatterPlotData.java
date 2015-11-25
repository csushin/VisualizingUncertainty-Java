package edu.asu.waterDemo.main;

import java.awt.geom.Point2D;
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

import edu.asu.waterDemo.commonclasses.TiffParser;
import edu.asu.waterDemo.main.GetMatrixData.GetModelStatsThread;

@Path("/getMultiScatterPlotData")
public class GetMultiScatterPlotData {
	private String basisDir;
	private int NUMBER_OF_PROCESSORS = 16;
	
	public class ScatterMatrixUnit{
		public double[] values;
		public int amount;
		
		public ScatterMatrixUnit(double[] values, int amount){
			this.values = values;
			this.amount = amount;
		}
	}

	public class ConvertValue2Pixel implements Runnable{
		public String metricPath;
		public int unitSize;
		public ArrayList<ScatterMatrixUnit> result;
		public int metricIndex;
		
		public ConvertValue2Pixel(ArrayList<ScatterMatrixUnit> result, String metricPath, int metricIndex, int unitSize){
			this.metricPath = metricPath;
			this.unitSize = unitSize;
			this.result = result;
			this.metricIndex = metricIndex;
		}

		@Override
		public void run() {
			TiffParser parser = new TiffParser(this.metricPath);
			int width = (int)parser.getSize()[1];
			int height = (int)parser.getSize()[0];
			double[] MinMax = parser.getMinmax();
			for(int i=0; i<width*height; i++){
				double value = parser.getData()[i];
				if(!Double.isNaN(value) && value!=-1){
					int _value = (int) Math.floor(this.unitSize*(value-MinMax[0])/(MinMax[1]-MinMax[0]));
					this.result.get(i).values[this.metricIndex] = _value;
					if(this.result.get(i).amount == 0)
						this.result.get(i).amount = 1;
				}
			}
		}
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
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public void query(
			@QueryParam("unitSize") @DefaultValue("null") String unitSize,
			@QueryParam("metrics") @DefaultValue("null") String metrics,
			@QueryParam("type") @DefaultValue("null") String dataType){
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		String[] metricList = metrics.split(",");
		
		ArrayList<ScatterMatrixUnit> roundData = new ArrayList<ScatterMatrixUnit>();
		for(int i=0; i<3600*2640; i++){
			double[] unitValues = new double[metricList.length];
			ScatterMatrixUnit unit = new ScatterMatrixUnit(unitValues, 0);
			roundData.add(unit);
		}
		ConvertValue2Pixel[] convert2intService = new ConvertValue2Pixel[metricList.length];
		Thread[]  convert2intThread = new Thread[metricList.length];
		for(int i=0; i<metricList.length; i++){
			String path = this.basisDir + "EnsembleMeanOfTimeStat/Ensemble"+metricList[i]+"TimeMean.tif";
			convert2intService[i] = new ConvertValue2Pixel(roundData, path, i, Integer.valueOf(unitSize));
			convert2intThread[i] = new Thread(convert2intService[i]);
			convert2intThread[i].start();
		}
		try{
			for(int i=0; i<metricList.length; i++){
				convert2intThread[i].join();
				System.out.println("GetModelStat: " + i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<ScatterMatrixUnit> uniqueData = new ArrayList<ScatterMatrixUnit>();
		for(int i=0; i<roundData.size(); i++){
			if(roundData.get(i).amount!=0){
				double[] roundValues = roundData.get(i).values;
				if(uniqueData.size() == 0)
					uniqueData.add(roundData.get(i));
			}
		}
//		return result;
	}

}
