package edu.asu.waterDemo.commonclasses;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

// to parse the files correctly, the name of files should follow the below rules strictly:
// EXP_GCM_RCM_VAR_YEARTYPE.tif 
public class LoadAllPossibleParams {
	
	String supplyPath;
	String demandPath;
	
	public LoadAllPossibleParams(String supplyPath, String demandPath){
		this.supplyPath = supplyPath;
		this.demandPath = demandPath;
	}
	
	public ArrayList<String> getDemandParams(){
		ArrayList<String> demandArr = new ArrayList<>();
		// Directory path here
		String files;
		File folder = new File(demandPath);
		File[] listOfFiles = folder.listFiles(); 
		 
		for (int i = 0; i < listOfFiles.length; i++) 
		{
			if (listOfFiles[i].isFile()) 
			{
				files = listOfFiles[i].getName();
				if (files.endsWith(".tif") || files.endsWith(".TIF"))
				{
					String _files = files.replace(".tif", "");
					String[] parts = _files.split("_");
					demandArr.add(parts[3]);
				}
			}
		}	
		
		return demandArr;
	}
	
	public ArrayList<String> getSupplyParams(String paramType){
		int flag = -1;
		switch(paramType){
			case "EXP": 
				flag = 0;
				break;
			case "GCM": 
				flag = 1;
				break;
			case "RCM": 
				flag = 2;
				break;
			case "VAR": 
				flag = 3;
				break;
			case "YEARTYPE": 
				flag = 4;
				break;
			default: 
				flag = 5;
				break;
		}
		ArrayList<String> curParam = new ArrayList<>();
		// Directory path here
		String path = supplyPath; 
		 
		String files;
		File folder = new File(path);
		ArrayList<File> listOfFiles = new ArrayList<File>();
		listOfFiles = getAllFiles(path, listOfFiles);
		for(int i=0; i< listOfFiles.size(); i++){
			files = listOfFiles.get(i).getName();
			if (files.endsWith(".tif") || files.endsWith(".TIF"))
			{
				String _files = files.replace(".tif", "");
				String[] parts = _files.split("_");
				curParam.add(parts[flag]);
			}		
		}

		Set<String> uniqueValue = new HashSet<String>(curParam);
		ArrayList<String> result = new ArrayList<String> (uniqueValue);
		return result;
	}
	
	public ArrayList<File> getAllFiles(String directoryName, ArrayList<File> files) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile()) {
	            files.add(file);
	        } else if (file.isDirectory()) {
	        	getAllFiles(file.getAbsolutePath(), files);
	        }
	    }
	    return files;
	}
}
