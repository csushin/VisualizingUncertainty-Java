package edu.asu.waterDemo.commonclasses;


import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

public class DrawTileThread implements Runnable{
	private int startIndex;
	private int endIndex;
	private ArrayList<TiffParser> parsers;
	private GenerateTiles tile;
	private double[] thresholds;
	private JSONObject tffunction;
	private HashMap<String, Color> selectedNodes;
	
	public DrawTileThread(int startIndex, int endIndex, double[] thresholds, JSONObject tffunction, ArrayList<TiffParser> parsers, GenerateTiles tile, HashMap<String, Color> selectedNodes){
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.parsers = parsers;
		this.tile =  tile;
		this.thresholds = thresholds;
		this.tffunction = tffunction;
		this.selectedNodes = selectedNodes;
	}
	
	
	@Override
	public void run() {
		for(int i=startIndex; i<endIndex; i++){
			int h = i/(int)this.parsers.get(0).getSize()[1];
			int w = (int) (i - h*this.parsers.get(0).getSize()[1]-1);
			double lat = this.parsers.get(0).getUlLatlng()[0] + h*this.parsers.get(0).getGeoInfo()[5];
			double lng = this.parsers.get(0).getUlLatlng()[1] + w*this.parsers.get(0).getGeoInfo()[1];
			double[] values = new double[this.parsers.size()];
			for(int index = 0; index<this.parsers.size(); i++){
				values[index] = this.parsers.get(index).getData()[i];
			}
			try {
				tile.drawTiles(values, this.thresholds, this.tffunction, this.selectedNodes, lat, lng);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
