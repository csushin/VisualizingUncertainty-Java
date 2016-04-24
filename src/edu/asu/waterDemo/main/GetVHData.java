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

import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getVHData")
public class GetVHData {
	private String basisDir;
	private int NUMBER_OF_PROCESSORS = 16;
	
	public class VHDataUnit{
		public double x;
		public double y;
		public int amount = 0;
		
		public VHDataUnit(double x, double y, int amount){
			this.x = x;
			this.y = y;
			this.amount = amount;
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
	
	public class CalcGradientService implements Runnable{
		public int startIndex;
		public int endIndex;
		public TiffParser parser;
		public String scale;
		public ArrayList<VHDataUnit> result;
		public String derivative;
		
		public CalcGradientService(int startIndex, int endIndex, TiffParser parser, ArrayList<VHDataUnit> result, String scale, String derivative){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.parser = parser;
			this.result = result;
			this.scale = scale;
			this.derivative = derivative;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			int width = (int) this.parser.getSize()[1];
			int height = (int) this.parser.getSize()[0];
			for(int i=this.startIndex; i<this.endIndex; i++){
				double value = this.parser.getData()[i];
				if(!Double.isNaN(value) && value!=-1){
					int left, right, top, bottom;
					left = right = top = bottom = i;
					double x = i % width;
					double y = i / width;
					if(x>0)	{ left = i - 1;}
					if(x<width-1) { right = i+1;}
					if(y>0) {top = i - width;}
					if(y<height-1) {bottom = i+width;}
					double leftVal, rightVal, topVal, bottomVal;
					leftVal = this.parser.getData()[left];
					rightVal = this.parser.getData()[right];
					topVal = this.parser.getData()[top];
					bottomVal = this.parser.getData()[bottom];
					if(this.scale.equals("true")){
						leftVal = Math.log(leftVal);
						rightVal = Math.log(rightVal);
						topVal = Math.log(topVal);
						bottomVal = Math.log(bottomVal);
					}
					if(ReasonableValue(leftVal) && ReasonableValue(rightVal)
							&& ReasonableValue(topVal) && ReasonableValue(bottomVal)){
						if(this.derivative.equals("first")){
							double gradient_x = (rightVal - leftVal)/2.0;
							double gradient_y = (topVal - bottomVal)/2.0;
							double gradient = Math.sqrt(Math.pow(gradient_x, 2.0) + Math.pow(gradient_y, 2.0));
							VHDataUnit unit = new VHDataUnit(value, gradient, 1);
							this.result.add(unit);
						}
						else{
							double logValue = value;
							if(this.scale.equals("true"))
								logValue = Math.log(logValue);
							double gradient_x = (rightVal + leftVal - 2*logValue);
							double gradient_y = (topVal + bottomVal - 2*logValue);
							double gradient = Math.sqrt(Math.pow(gradient_x, 2.0) + Math.pow(gradient_y, 2.0));
							VHDataUnit unit = new VHDataUnit(value, gradient, 1);
							this.result.add(unit);
						}
					}
				}
			}
		}
		
		private boolean ReasonableValue(double val){
			if(!Double.isNaN(val) && val!=-1)
				return true;
			else 
				return false;
		}
	}
	
	public class RemoveDuplicationService implements Runnable{
		public int startIndex;
		public int endIndex;
		public ArrayList<VHDataUnit> source;
		public ArrayList<VHDataUnit> subResult;
		
		public RemoveDuplicationService(int startIndex, int endIndex, ArrayList<VHDataUnit> source){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.source = source;
			this.subResult = new ArrayList<VHDataUnit>();
		}
		
		public ArrayList<VHDataUnit> getResult(){
			return this.subResult;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for(int i=this.startIndex; i<this.endIndex; i++){
				if(this.subResult.size() == 0){
					this.subResult.add(this.source.get(i));
				}
				else{
					boolean existedVal = false;
					for(int j=0; j<this.subResult.size(); j++){
						if(this.subResult.get(j).x == this.source.get(i).x &&
								this.subResult.get(j).y == this.source.get(i).y){
							existedVal = true;
							this.subResult.get(j).amount+=this.source.get(i).amount;
							break;
						}
					}
					if(!existedVal){
						this.subResult.add(this.source.get(i));
					}
				}
			}
		}
		
	}
	
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public ArrayList<VHDataUnit> query(
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("dataLevel") @DefaultValue("null") String dataLevel,
			@QueryParam("dataNameKeyword") @DefaultValue("null") String dataNameKeyword,
			@QueryParam("scale") @DefaultValue("null") String scale,
			@QueryParam("derivative") @DefaultValue("null") String derivative){
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		if(dataType.equals("TemperatureMax"))
			_dataType = "tasmax_HIST";
		String targetPath = getAllFiles(this.basisDir + _dataType + "/" + dataLevel, dataNameKeyword);
		TiffParser targetParser = new TiffParser(targetPath);
		CalcGradientService[] CalcGradientServices = new CalcGradientService[NUMBER_OF_PROCESSORS];
		Thread[] CalcGradientThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = (int) (targetParser.getSize()[0] / NUMBER_OF_PROCESSORS);
		ArrayList<VHDataUnit> result = new ArrayList<VHDataUnit>();
		for(int m=0; m<NUMBER_OF_PROCESSORS; m++){
			int startIndex = m*delta;
			int endIndex = (m+1)*delta;
			if(m == NUMBER_OF_PROCESSORS - 1)
				endIndex = (int) (targetParser.getSize()[0]*targetParser.getSize()[1] - 1);
			CalcGradientServices[m] = new CalcGradientService(startIndex, endIndex, targetParser, result, scale, derivative);
			CalcGradientThread[m] = new Thread(CalcGradientServices[m]);
			CalcGradientThread[m].start();
		}
		try{
			for(int h=0; h<NUMBER_OF_PROCESSORS; h++){
				CalcGradientThread[h].join();
				System.out.println("Gradient Finished: " + h + " Finished~");
			}
		} catch(InterruptedException e){
			e.printStackTrace();
		}
		
		ArrayList<VHDataUnit> _uniques = new ArrayList<VHDataUnit>();
		RemoveDuplicationService[] RemoveDuplicationServices = new RemoveDuplicationService[NUMBER_OF_PROCESSORS];
		Thread[] RemoveDuplicationThreads = new Thread[NUMBER_OF_PROCESSORS];
		delta = result.size()/NUMBER_OF_PROCESSORS;
		for(int k=0; k<NUMBER_OF_PROCESSORS; k++){
			int startIndex = k*delta;
			int endIndex = (k+1)*delta;
			if(k == NUMBER_OF_PROCESSORS - 1)
				endIndex = result.size() -1;
			RemoveDuplicationServices[k] = new RemoveDuplicationService(startIndex, endIndex, result);
			RemoveDuplicationThreads[k] = new Thread(RemoveDuplicationServices[k]);
			RemoveDuplicationThreads[k].start();
		}
		try{
			for(int j=0; j<NUMBER_OF_PROCESSORS; j++){
				RemoveDuplicationThreads[j].join();
				_uniques.addAll(RemoveDuplicationServices[j].getResult());
				System.out.println("Remove Dupilications: " + j + " Finished~");
			}
		} catch(InterruptedException e){
			e.printStackTrace();
		}
		
		ArrayList<VHDataUnit> uniques = new ArrayList<VHDataUnit>();
		for(int p=0; p<_uniques.size(); p++){
			if(uniques.size() == 0){
				uniques.add(_uniques.get(p));
			}
			else{
				boolean existedVal = false;
				for(int q=0; q<uniques.size(); q++){
					if(uniques.get(q).x == _uniques.get(p).x &&
							uniques.get(q).y == _uniques.get(p).y){
						uniques.get(q).amount+=_uniques.get(p).amount;
						existedVal = true;
						break;
					}
				}
				if(!existedVal){
					uniques.add(_uniques.get(p));
				}
			}
		}
		
		return uniques;
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
