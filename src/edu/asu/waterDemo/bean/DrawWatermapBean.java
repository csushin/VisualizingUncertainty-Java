package edu.asu.waterDemo.bean;

import java.util.List;

public class DrawWatermapBean {
	public String expName;
	public String gcmName;
	public String rcmName;
	public String varName;
	public String typeName;
	public double[] lrLatlng;
	public double[] geoInfoUnit;
	public double[] statistics;
	public double[] ulLatlng;
	public double[] size;
	public List<ListPointsBean> dataList;
	public List<ListPointsBean> getDataList() {
		return dataList;
	}
	public void setDataList(List<ListPointsBean> dataList) {
		this.dataList = dataList;
	}	
	
}