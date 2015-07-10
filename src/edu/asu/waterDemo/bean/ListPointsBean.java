package edu.asu.waterDemo.bean;

public class ListPointsBean {
	private double lat;
	private double lng;
	private double w;
	private double h;
	private double val;
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLng() {
		return lng;
	}
	public void setLng(double lng) {
		this.lng = lng;
	}
	public double getValue() {
		return val;
	}
	public void setValue(double value) {
		this.val = value;
	}
	public double getwIndex() {
		return w;
	}
	public void setwIndex(double wIndex) {
		this.w = wIndex;
	}
	public double gethIndex() {
		return h;
	}
	public void sethIndex(double hIndex) {
		this.h = hIndex;
	}
}
