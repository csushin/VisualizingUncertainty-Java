package edu.asu.waterDemo.commonclasses;

import java.util.ArrayList;

public class CorrelateStatsThread implements Runnable{
	private int startIndex;
	private int endIndex;
	private double[] xMinMax;
	private double[] yMinMax;
	private TiffParser xParser;
	private TiffParser yParser;
	private int width;
	private int height;
	private double[] results;
	private double[] xHistograms;;
	private double[] yHistograms;
	private Object lock = new Object();
	
	public CorrelateStatsThread(int startIndex, int endIndex, int width, int height, TiffParser xParser, TiffParser yParser, double[] xMinMax, double[] yMinMax, double[] results, double[] xHistograms, double[] yHistograms){
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.xParser = xParser;
		this.yParser = yParser;
		this.xMinMax = xMinMax;
		this.yMinMax = yMinMax;
		this.width = width;
		this.height = height;
		this.results = results;
		this.xHistograms = xHistograms;
		this.yHistograms = yHistograms;
	}
	
	@Override
	public void run() {
		for(int index=startIndex; index<endIndex; index++){
			double originXVal = xParser.getData()[index];
			double originYVal = yParser.getData()[index];
			if(originXVal!=-1 && originYVal!=-1){
				int xValue = (int) Math.floor(this.width*(originXVal-xMinMax[0])/(xMinMax[1]-xMinMax[0]));
				int yValue = (int) Math.floor(this.height*(originYVal-yMinMax[0])/(yMinMax[1]-yMinMax[0]));
				int offset = yValue*this.width + xValue;
				synchronized(lock){
					this.results[offset]+=1;
					this.xHistograms[xValue]+=1;
					this.yHistograms[yValue]+=1;
				}
			}
		}
	}
	
}
