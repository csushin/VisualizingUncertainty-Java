package edu.asu.waterDemo.main;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import com.opencsv.CSVReader;

import edu.asu.waterDemo.bean.LoadTimeSeriesBean;

@Path("/LoadTimeseries")
public class LoadTimeSeries {

	String fileDir;
	

	
	@Context
	public void setServletContext(ServletContext context) {
		fileDir = context.getRealPath("img/TimeSeries_NigerRiver") + File.separatorChar;
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public LoadTimeSeriesBean query(@QueryParam("message") @DefaultValue("FALSE") String message) throws IOException {
		LoadTimeSeriesBean result  = new LoadTimeSeriesBean();
		ArrayList<String> nameArray = new ArrayList<String>();
		ArrayList<Float []> valueArray = new ArrayList<Float []>();
		ArrayList<Integer []> yearArray = new ArrayList<Integer []>();
		String files;
		String line = "";
		String cvsSplitBy = ",";
		File folder = new File(this.fileDir);
		File[] listOfFiles = folder.listFiles(); 
		for (int i = 0; i < listOfFiles.length; i++) 
		{
			if (listOfFiles[i].isFile()) 
			{
				files = listOfFiles[i].getName();
				if (files.endsWith(".csv") || files.endsWith(".CSV"))
				{
//					store the name of the csv, including gcm/rcm/hist/rcp
					String _files = files.replace(".csv", "");
					String __files = _files.replace("Annual_Discharge_", "");
					nameArray.add(__files);
//					store the value of each csv
					CSVReader reader = new CSVReader(new FileReader(listOfFiles[i]));
					String [] nextLine;
					ArrayList<Integer> curYearArrList = new ArrayList<Integer>();// 141 comes from the start year 1960 to end year 2100 within 141 years
					ArrayList<Float> curValArrList = new ArrayList<Float>();// the same as above
					while ((nextLine = reader.readNext()) != null) {
				        // nextLine[] is an array of values from the line
						if(nextLine[0]!="Year" && !(nextLine[1].contains("Discharge"))){
							curYearArrList.add(Integer.parseInt(nextLine[0]));
							curValArrList.add(Float.parseFloat(nextLine[1]));
						}
				    }
					yearArray.add(curYearArrList.toArray(new Integer[curYearArrList.size()-1]));
					valueArray.add(curValArrList.toArray(new Float[curValArrList.size()-1]));
				}
			}
		}
		result.setNameArray(nameArray);
		result.setYearArray(yearArray);
		result.setValue(valueArray);
		return result;
	}
	
	static <T> T[] addElement(T[] a, T e) {
	    a  = Arrays.copyOf(a, a.length + 1);
	    a[a.length - 1] = e;
	    return a;
	}
	

}
