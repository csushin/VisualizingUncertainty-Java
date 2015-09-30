package edu.asu.waterDemo.commonclasses;

public class LoadTiffThread implements Runnable{
	private TiffParser parser;
	private String filePath;
	
	public LoadTiffThread(String filePath){
		this.filePath = filePath;
	}
	
	public TiffParser getResult(){
		return parser;
	}
	
	public void run(){
		System.out.println("Start parsing file: " + this.filePath);
		parser = new TiffParser(filePath);
	}
}

