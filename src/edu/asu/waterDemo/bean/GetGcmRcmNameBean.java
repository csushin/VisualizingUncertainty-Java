package edu.asu.waterDemo.bean;

import java.util.ArrayList;

public class GetGcmRcmNameBean {
	private ArrayList<String[]> sfNames;
	private ArrayList<String[]> dfNames;
	
	public ArrayList<String[]> getDfNames() {
		return dfNames;
	}

	public void setDfNames(ArrayList<String[]> dfNames) {
		this.dfNames = dfNames;
	}

	public ArrayList<String[]> getsfNames() {
		return sfNames;
	}

	public void setsfNames(ArrayList<String[]> sfNames) {
		this.sfNames = sfNames;
	}
}
