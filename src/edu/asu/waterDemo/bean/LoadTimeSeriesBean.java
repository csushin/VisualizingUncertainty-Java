package edu.asu.waterDemo.bean;

import java.util.ArrayList;




public class LoadTimeSeriesBean {
	private ArrayList<String> nameArray;
	private ArrayList<Float[]> valueArray;
	private ArrayList<Integer[]> yearArray;
	
	public ArrayList<Integer[]> getYearArray() {
		return yearArray;
	}
	public void setYearArray(ArrayList<Integer[]> yearArray) {
		this.yearArray = yearArray;
	}
	public ArrayList<String> getNameArray() {
		return nameArray;
	}
	public void setNameArray(ArrayList<String> nameArray) {
		this.nameArray = nameArray;
	}
	public ArrayList<Float[]> getValue() {
		return valueArray;
	}
	public void setValue(ArrayList<Float[]> value) {
		this.valueArray = value;
	}

	
}

