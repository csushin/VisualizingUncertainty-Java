package edu.asu.waterDemo.bean;

import java.io.File;
import java.io.IOException;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

public class GetNetCDF {
	private int minYear = 2010;
	private int stepYear = 5;
	private String path;// = //+dbName+".nc";
	private double[] swPt;
	private double[] nePt;
	private String dbName;
	private String type;
	private int year;
	
	GetNetCDF(double[] swPt, double[] nePt, String dbName, String type, int year){
		this.setDbName(dbName);
		this.setNePt(nePt);
		this.setSwPt(swPt);
		this.setType(type);
		this.setYear(year);
		String path = checkSystem();
		this.setPath(path);
	}
	
	public double[] getGridValue(){
		double[] result ;
		String fileLocation = path + this.dbName + ".nc";
		NetcdfFile ncfile = null;
		try {
			ncfile = NetcdfDataset.openFile(fileLocation, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return result;
	}
	
	private int getYearIndex(){
		int yearIndex = (this.year - this.minYear) / this.stepYear;
		return yearIndex;
	}
	
	public String checkSystem(){
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		String path = "";
		if(osNameMatch.contains("windows")) 
			path = "E:\\geoTiffData" + File.separatorChar;
		else
			path = "/home/zchang3/demo-data-v2.1/";
		return path;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public double[] getSwPt() {
		return swPt;
	}

	public void setSwPt(double[] swPt) {
		this.swPt = swPt;
	}

	public double[] getNePt() {
		return nePt;
	}

	public void setNePt(double[] nePt) {
		this.nePt = nePt;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}
}
