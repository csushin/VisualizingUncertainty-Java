package edu.asu.waterDemo.commonclasses;

import java.util.ArrayList;

public class GetHistDataThread implements Runnable {
	private double[] data;
	private double[] minmax;
	private int startIndex;
	private int endIndex;
	private int binSize;
	private TiffParser parser;
	
	public GetHistDataThread(TiffParser parser, double[] data, double[] minmax, int startIndex, int endIndex, int binSize){
		this.data = data;
		this.minmax = minmax;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.binSize = binSize;
		this.parser = parser;
	}
	
	public double[] getResults(){
		return this.data;
	}
	
	@Override
	public void run() {
		for(int index=this.startIndex; index<this.endIndex; index++){
			double originval = this.parser.getData()[index];
			if(originval != -1 && !Double.isNaN(originval)){
				int xValue = (int) Math.floor ((originval - this.minmax[0])/(this.minmax[1] - this.minmax[0] + 1)*binSize);
				this.data[xValue]++;
			}
		}
	}

}
