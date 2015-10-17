package edu.asu.waterDemo.commonclasses;

import java.util.ArrayList;
import java.util.Collections;

public class CalcStatThread implements Runnable{
	private String types;
	private double[] popValue;
	private double ratio;
	private ArrayList<double[]> results;
	private int startHIndex;
	private int endHIndex;
	private int deltaX;
	private int deltaY;
	private int totalWidth;
	private int typeIndexAgree;
	private int typeIndexVar;
	private int typeIndexEnt;
	private int typeIndexMean;
	private ArrayList<TiffParser> supplyParsers;
	
	public CalcStatThread(String types, double ratio, double[] popValue, ArrayList<TiffParser> supplyParsers, int startHIndex, int endHIndex, int deltaX, int deltaY, int totalWidth, ArrayList<double[]> bufferSet, int typeIndexAgree, int typeIndexVar, int typeIndexEnt, int typeIndexMean){
		this.types = types;
		this.popValue = popValue;
		this.ratio = ratio;
		this.supplyParsers = supplyParsers;
		this.startHIndex = startHIndex;
		this.endHIndex = endHIndex;
		this.deltaX = deltaX;
		this.deltaY = deltaY;
		this.totalWidth = totalWidth;
		this.results = bufferSet;
		this.typeIndexAgree = typeIndexAgree;
		this.typeIndexEnt = typeIndexEnt;
		this.typeIndexVar = typeIndexVar;
		this.typeIndexMean = typeIndexMean;
	}
	
	public ArrayList<double[]> getResults(){
		return this.results;
	}
	
	public void run(){
		for(int h=this.startHIndex; h<this.endHIndex; h++){
			for(int w=0; w<this.totalWidth; w++){
				int supplyIndex = h*totalWidth+w;
				int popIndex = supplyIndex;
				if(this.deltaY!=0 || this.deltaX!=0){
					popIndex = (h+this.deltaY)*(this.totalWidth + this.deltaX) + (w + this.deltaX);
				}
				boolean nullFlag = false;
				int sum = 0;
				int nonNANCount = 0;
				ArrayList<Integer> supplyValCat = new ArrayList<Integer>();
				ArrayList<Double> supplyValArr = new ArrayList<Double>();
				double singlePopValue = this.popValue[popIndex];
				for(int k=0; k<this.supplyParsers.size(); k++){
					double scarVal = 0;
					double curSupplyVal = this.supplyParsers.get(k).getData()[supplyIndex];
					if(!Double.isNaN(singlePopValue) && !Double.isNaN(curSupplyVal)){
						if(singlePopValue>=1 && curSupplyVal>=0){
							scarVal = curSupplyVal/(singlePopValue*this.ratio);
						}
						else if(singlePopValue<1 && curSupplyVal>=0){
							scarVal = 1701;
						}		
					}
					else{
						scarVal = -1;
					}
				
					//		set the values of the scarcity by using 0/1/2/3 to represent AbScar/Scar/Stre/NoStre
					int flag;
					if(scarVal<=500 && scarVal>=0) {flag = 1;sum+=flag;nonNANCount++;}
					else if(scarVal>500 && scarVal<=1000) {flag = 2;sum+=flag;nonNANCount++;}
					else if(scarVal>1000 && scarVal<=1700) {flag = 3;sum+=flag;nonNANCount++;}
					else if(scarVal>1700)	{flag = 4;sum+=flag;nonNANCount++;}
					else {flag = -1; nullFlag=true;}//here we need to also consider the situation that water supply is NaN as it comes from the water model
					supplyValCat.add(flag);
				}
		
				double mean = calcMean(nullFlag, sum, nonNANCount);//(double) (sum/(double)supplyValArr.size());
				if(this.types.contains("mean")){
					this.results.get(this.typeIndexMean)[supplyIndex] = mean;
				}
				if(this.types.contains("agree")){
					double votings = calcVotings(nullFlag, mean, supplyValCat, nonNANCount);
					this.results.get(this.typeIndexAgree)[supplyIndex] = votings;
				}
				if(this.types.contains("variance")){
					double variance = calcVariance(nullFlag, sum, supplyValCat, nonNANCount);
					this.results.get(this.typeIndexVar)[supplyIndex] = variance;
				}
				if(this.types.contains("entropy")){
					double entropy = calcEntropy(nullFlag, sum, supplyValCat, nonNANCount);
					this.results.get(this.typeIndexEnt)[supplyIndex] = entropy;
				}
			}
		}
	
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
}
