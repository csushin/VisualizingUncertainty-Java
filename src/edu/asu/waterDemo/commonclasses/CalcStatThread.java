package edu.asu.waterDemo.commonclasses;

public class CalcStatThread implements Runnable{
	private String type;
	private double[] value;
	private double[] result;
	
	public CalcStatThread(String type, double[] value){
		this.type = type;
		this.value = value;
	}
	
	public double[] getResults(){
		return this.result;
	}
}
