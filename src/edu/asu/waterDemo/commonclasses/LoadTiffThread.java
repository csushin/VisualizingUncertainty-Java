package edu.asu.waterDemo.commonclasses;

import java.util.ArrayList;

public class LoadTiffThread implements Runnable{
	private TiffParser parser;
	private int startIndex;
	private int endIndex;
	private ArrayList<String> sPathList;
	private ArrayList<TiffParser> sParserArr;
	private String filePath;
	
	public LoadTiffThread(String filePath){
		this.filePath = filePath;
	}
	
	public TiffParser getResult(){
		return this.parser;
	}
	
	public void run(){
//		System.out.println("Start parsing file: " + this.filePath);
		this.parser = new TiffParser(this.filePath);
	}
}

