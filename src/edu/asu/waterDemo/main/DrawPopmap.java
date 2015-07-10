package edu.asu.waterDemo.main;


import java.io.File;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;
import org.json.simple.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCursor;

import edu.asu.waterDemo.bean.DrawPopmapBean;
import edu.asu.waterDemo.bean.ListPointsBean;
import edu.asu.waterDemo.main.TiffParser;

@Path("/getPopPoints")
public class DrawPopmap {
	String imageDir;
	String linux_demandDir;
	String linux_supplyDir;
	
	@Context
	public void setServletContext(ServletContext context) {
		imageDir = context.getRealPath("img/demand") + File.separatorChar;
		linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
		linux_supplyDir = "/work/asu/data/wsupply/BW/"; 
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public DrawPopmapBean query(@QueryParam("message") @DefaultValue("FALSE") String message,
			@QueryParam("fName") @DefaultValue("null") String fName,
			@QueryParam("resolution") @DefaultValue("low") String resolution) throws UnknownHostException {
		DrawPopmapBean result = new DrawPopmapBean();
//		String Path = imageDir + "\\demand\\afds00ag.tif";
		System.out.println(fName);
		//System.out.println(Path);
		if(message.equals("requestPop"))
		{
			TiffParser tiffParser = new TiffParser();
			
			if(resolution.equals("low")){
				String Path = imageDir + "popden_pred/720x360/" + fName;
				tiffParser.setFilePath(Path);
				System.out.println("pop path: "+ Path);
				if(tiffParser.parser()){
					double[] size = tiffParser.getPixelHW();
					double[] lrLatlng = tiffParser.getLrLatlng();//lower-right
					double[] ulLatlng = tiffParser.getUlLatlng();//top-left
					double[] geoInfoUnit = tiffParser.getGeoUnitPerUW();
					double[] data = tiffParser.getData();
					double[] statistics = tiffParser.getStatistics();
					
					double minVal = 999999999;
					double maxVal = 0;
					
					List<ListPointsBean> jsonList = new LinkedList<>();
					ListPointsBean curIndex; 
					for(int h=0; h<size[0] ;h++){
						for(int w=0; w<size[1]; w++){
							double value = data[(int)(h*size[1]+w)];
							if(value>0)
							{
								curIndex= new ListPointsBean();
								double lat = ulLatlng[0] + h*geoInfoUnit[0];
								double lng = ulLatlng[1] + w*geoInfoUnit[1];
								curIndex.setLat(lat);
								curIndex.setLng(lng);
								curIndex.setValue(Math.round(value));
								if(value<minVal)
			        				minVal = value;
			        			if(value>maxVal)
			        				maxVal = value;
								jsonList.add(curIndex);
							}
						}
					}
					statistics[0] = minVal;
					statistics[1] = maxVal;
					result.statistics = statistics;
					result.geoInfoUnit = geoInfoUnit;
					result.ulLatlng = ulLatlng;
					result.lrLatlng = lrLatlng;
					result.size = size;
					result.setDataList(jsonList);
				}
				else{
					System.out.println("Cannot parse the pop file!");
					double[] size = {-1,-1};
					result.size = size;					
				}
			}
			else{
				// get higher resolution data
			}
		}
		else{
		}
		return result;
	}
}
