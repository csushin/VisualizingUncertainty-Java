package edu.asu.waterDemo.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.glassfish.jersey.server.JSONP;

import com.google.common.io.Files;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

import edu.asu.waterDemo.bean.DrawScarcitymapBean;
import edu.asu.waterDemo.bean.ListPointsBean;


@Path("/getScarcityPointsDev")
public class DrawScarcitymapDev {
	private double[] size;
	private double[] lrLatlng;
	private double[] ulLatlng;
	private double[] geoInfoUnit;
	private double[] statistics;
	
	String supplyDir;
	String demandDir;
	String linux_supplyDir;
	String linux_demandDir;
	String linux_scarcityDir;
	
	@Context
	public void setServletContext(ServletContext context) {
		supplyDir = context.getRealPath("img") + File.separatorChar;
		demandDir = context.getRealPath("img/demand") + File.separatorChar;
		linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
		linux_supplyDir = "/work/asu/data/wsupply/"; 
		linux_scarcityDir = "/work/asu/data/wscarcity/"; 
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public DrawScarcitymapBean query(@QueryParam("message") @DefaultValue("FALSE") String message,
			@QueryParam("supplyfName") @DefaultValue("null") String supplyfName,
			@QueryParam("supplySubDir") @DefaultValue("null") String supplySubDir,
			@QueryParam("demandfName") @DefaultValue("null") String demandfName,
			@QueryParam("resolution") @DefaultValue("raw") String resolution) throws ClientProtocolException, IOException {
		DrawScarcitymapBean result = new DrawScarcitymapBean();
		
		if(message.equals("requestScarcity"))
		{
			if(resolution.equals("low")){
				TiffParser popParser = new TiffParser();
				TiffParser waterParser = new TiffParser();
				double[] size = {360, 720};
				String supplyPath = linux_supplyDir + "BW_10km/" + supplySubDir + "/" + supplyfName;
				String demandPath = linux_demandDir + "resampled_10km/" + demandfName;
				popParser.setFilePath(demandPath);
				waterParser.setFilePath(supplyPath);
  				if(popParser.parser() && waterParser.parser()){	
//					double[] scarcity = calScarcity(waterParser, popParser);
					double[] popData = popParser.getData();
					double[] waterData = waterParser.getData();
					List<ListPointsBean> jsonList = new LinkedList<>();
					ListPointsBean curIndex; 
//					result.statistics = this.statistics;
					result.geoInfoUnit = popParser.getGeoUnitPerUW();
					result.ulLatlng = popParser.getUlLatlng();
					result.lrLatlng = popParser.getLrLatlng();
					result.size = popParser.getSize();
					result.supplyName = supplyfName;
					double[] stat = new double[2];
					stat[0] = 999999999;
					stat[1] = 0;
					
					for(int h=0; h<result.size[0] ;h++){
						for(int w=0; w<result.size[1]; w++){
							int index = (int)(h*result.size[1]+w);
							double popVal = popData[index];
							double waterVal = waterData[index];
							if(!Double.isNaN(popVal) && !Double.isNaN(waterVal)){
								if(popVal>=1){
									double scarVal = waterVal*1000/popVal;
									curIndex= new ListPointsBean();
//									double lat = result.ulLatlng[0] + h*result.geoInfoUnit[0];
//									double lng = result.ulLatlng[1] + w*result.geoInfoUnit[1];
//									curIndex.setLat(lat);
//									curIndex.setLng(lng);								
									curIndex.setValue(Math.round(scarVal));
									curIndex.setwIndex(w);
									curIndex.sethIndex(h);
									jsonList.add(curIndex);
									if(stat[0] > scarVal)
										stat[0] = scarVal;
									if(stat[1] < scarVal)
										stat[1] =  scarVal;
//									System.out.println(scarVal+ "water Val: " + waterVal + "pop Val: "  + popVal);
								}
								else if(popVal<1 && popVal>=0){
									double scarVal = 1701;
									curIndex= new ListPointsBean();
//									double lat = result.ulLatlng[0] + h*result.geoInfoUnit[0];
//									double lng = result.ulLatlng[1] + w*result.geoInfoUnit[1];
//									curIndex.setLat(lat);
//									curIndex.setLng(lng);								
									curIndex.setValue(Math.round(scarVal));
									curIndex.setwIndex(w);
									curIndex.sethIndex(h);
									jsonList.add(curIndex);
									if(stat[0] > scarVal)
										stat[0] = scarVal;
									if(stat[1] < scarVal)
										stat[1] =  scarVal;
								}								
							}
						}
					}
					result.statistics = stat;
					result.setDataList(jsonList);
				}
			}
			else{
//				parameters for geoserver request
				String port = "8080";
				String ws = "niger_river";
				String style = "nr_wscarcity_fm";
				TiffParser popParser = new TiffParser();
				TiffParser waterParser = new TiffParser();
//				double[] size = {3600, 7200};
				boolean createFlag = false;
				String scarcityName = demandfName.replace(".tif", "") + "_" + supplyfName;
				String outputfile = linux_scarcityDir + scarcityName;
				GeoserverService geoserver = new GeoserverService(scarcityName.replace(".tif", ""), outputfile, port, ws, style);
				if(geoserver.isExistance()){
					result.created = true;
					return result;
				}
				else{
//					delete data, delete coverage, delete layer
					if(geoserver.deleteAll()){
						createFlag = true;
					}
					else{
						result.created = false;
						System.out.println("Cannot delete coverage or layer or data!\n");
						return result;
					}
//					create newdata
					if(createFlag){
						String supplyPath = linux_supplyDir + "BW_1km/" + supplySubDir + "/" + supplyfName;
						String demandPath = linux_demandDir + demandfName;
						popParser.setFilePath(demandPath);
						waterParser.setFilePath(supplyPath);
						System.out.println("supply path: " + supplyPath);
						if(popParser.parser() && waterParser.parser()){	
							double[] popData = popParser.getData();
							double[] waterData = waterParser.getData();
							double[] buf = new double[(int) (waterParser.getySize()*waterParser.getxSize())];
							int deltaX = 0;
							int deltaY = 0;
							if(popParser.getxSize() != waterParser.getxSize() || popParser.getySize() != waterParser.getySize()){
								deltaX = popParser.getxSize() - waterParser.getxSize();
								deltaY = popParser.getySize() - waterParser.getySize();
							}
							for(int h=0; h<waterParser.getySize() ;h++){
								for(int w=0; w<waterParser.getxSize(); w++){
									int wIndex = h*waterParser.getxSize()+w;
									int popIndex = (h+deltaY)*popParser.getySize() + (w+deltaX);
									double popVal = popData[popIndex];
									double waterVal = waterData[wIndex];
									if(!Double.isNaN(popVal) && !Double.isNaN(waterVal)){
										if(popVal>=1 && waterVal>=0){
											double scarVal = waterVal*1000/popVal;
											buf[wIndex] = scarVal;
										}
										else if(popVal<1 && popVal>=0 && waterVal>=0){
											double scarVal = 1701;
											buf[wIndex] = scarVal;
										}								
									}
									else{
										buf[wIndex] = -1;
									}
								}
							}
							int writeResult = writeGeotiff(buf, waterParser, outputfile);
							System.out.println("Result for writing geotiff files: " + writeResult);							
				
						}
//						create coverage and layer
						if(geoserver.generateCoverage())
						{
							result.created = true;
							return result;
						}
						else{
							System.out.println("Error in create covarage and Layer!");
							result.created = false;
							return result;
						}						
					}
				}
			}
				
		}
		else{
			result = null;
		}
		return result;
	}
	
	private int writeGeotiff(double[] buf, TiffParser targetParser, String outputfile){
		Driver driver = gdal.GetDriverByName("GTiff");
		int result = 1;
		System.out.println(targetParser.getGeoInfo().toString());
		System.out.println(targetParser.getxSize());
		System.out.println(targetParser.getxSize());
		Dataset dst_ds = driver.Create(outputfile, targetParser.getxSize(), targetParser.getySize(), 1, gdalconst.GDT_Float64);
		dst_ds.SetGeoTransform(targetParser.getGeoInfo());
		dst_ds.SetProjection(targetParser.getProjRef());
		result = dst_ds.GetRasterBand(1).WriteRaster(0, 0, targetParser.getxSize(), targetParser.getySize(), buf);
		System.out.println("Geotiff Created!");	
		
		return result;
	}

	public String parseName(String fName){
		String _fName = fName.replace(".tif", "");
		String[] __fName = _fName.split("/");
		System.out.println(Arrays.asList(__fName));
		return __fName[__fName.length-1];
	}
	
	public double[] calScarcity(TiffParser waterParser, TiffParser popParser){
		double[] latArr = new double[4];
		latArr[0] = waterParser.getUlLatlng()[0];
		latArr[1] = waterParser.getLrLatlng()[0];
		latArr[2] = popParser.getUlLatlng()[0];
		latArr[3] = popParser.getLrLatlng()[0];
		double[] lngArr = new double[4];
		lngArr[0] = waterParser.getUlLatlng()[1];
		lngArr[1] = waterParser.getLrLatlng()[1];
		lngArr[2] = popParser.getUlLatlng()[1];
		lngArr[3] = popParser.getLrLatlng()[1];
		
		//sort each array and get the medium value
		Arrays.sort(latArr);// sorted from the smallest to biggest
		Arrays.sort(lngArr);
		double[] interUL = new double[2];
		double[] interLR = new double[2];
		interUL[0] = latArr[2];//max latitude, max means the second max
		interUL[1] = lngArr[1];//min longitude
		interLR[0] = latArr[1];//min latitude
		interLR[1] = lngArr[2];//max longitude
		System.out.println(interUL[0] +" " +interUL[1] + " " + interLR[0] + " " + interLR[1]);
		
		//get the sizeo of the interseceted part in each data
		double[] popInterSize = new double[2];
		double[] waterInterSize = new double[2];
		System.out.println("pop interSize h and w:" + popInterSize[0] + " " + popInterSize[1]);
		
		//get the original data for each intersection part in original data
		double[] waterInter = findSingleIntersection(waterParser,  interUL, interLR, waterInterSize);
		double[] popInter = findSingleIntersection(popParser, interUL, interLR, popInterSize);
		System.out.println("intersected size: "+waterInter.length);
		System.out.println("intersected size: "+popInter.length);
		
		//here take the larger picture, pop density, as the target size
		//but to save the calculation process, we take the targeSize as 300*300
		double targetSize[] = new double[] {100, 100};
		double[] srcWaterSize= waterParser.getPixelHW();
		double[] srcPopSize = popParser.getPixelHW();
		//scale the smaller image, water, to the larger target size
		double[] scaledPop = scaleTiff(popInter, popInterSize, targetSize);
		System.out.println("scaled water size: "+ scaledPop.length);
		double[] scaledWater = scaleTiff(waterInter, waterInterSize, targetSize);
		System.out.println("scaled water size: "+ scaledWater.length);
		
		double[] finalScaricty = calculateScarcity(scaledPop, scaledWater, targetSize);
		System.out.println("final water scarcity size: "+ finalScaricty.length);
		
		// return the basic scarcity data information
		this.size = targetSize;
		this.ulLatlng = interUL;
		this.lrLatlng = interLR;
		
		double[] geoUnit = new double[2];
		geoUnit[0] = (interUL[0]-interLR[0])/targetSize[0];
		geoUnit[1] = (interUL[1]-interLR[1])/targetSize[1];
		this.geoInfoUnit = geoUnit;
		
		//get the min and max value
		List temp = Arrays.asList(ArrayUtils.toObject(finalScaricty));
		double min = Collections.min(temp);
		double max = Collections.max(temp);
		double[] stat = new double[] {min, max};
		this.statistics = stat;
		System.out.println("check stat info:" + this.statistics[0] + "," + this.statistics[1]);
		return finalScaricty;
	}
	
	public double[] findSingleIntersection(TiffParser parser, double[] interUL, double[] interLR, double[] interSize){
		// for the width, the smaller minus the larger would get the negative value but the unit is negative too
		// so, it will get the positive value in final
		double interWidth = Math.floor(Math.abs((interUL[1] - interLR[1])/parser.getGeoUnitPerUW()[1]));
		double interHeight = Math.floor(Math.abs((interUL[0] - interLR[0])/parser.getGeoUnitPerUW()[0]));
		interSize[0] = interHeight;
		interSize[1] = interWidth;
		System.out.println("interWidth: "+interWidth+" interHeight: "+ interHeight);
		// get the relative coordinates of the intersected points in source data
		double relativePoint_x = Math.floor(Math.abs((interUL[1] - parser.getUlLatlng()[1])/parser.getGeoUnitPerUW()[1]));
		double relativePoint_y = Math.floor(Math.abs((interUL[0] - parser.getUlLatlng()[0])/parser.getGeoUnitPerUW()[0]));
		System.out.println("relative points x, y are: " + relativePoint_x + "," + relativePoint_y);
		
		double[] source = parser.getData();
		double[] target = new double[(int) (interWidth*interHeight)];
		for(int j=0; j<interHeight; j++){
			for(int i=0; i<interWidth; i++){
				int xInTiff = (int) (i + relativePoint_x);
				int yInTiff = (int) (j + relativePoint_y);
				int indexInTiff= (int) (yInTiff*parser.getPixelHW()[1]+xInTiff);
				double value = source[indexInTiff];
				int indexInInter = (int) (j*interWidth + i);
				target[indexInInter] = value;
				//System.out.println(target[indexInInter] + "," + i + "," + j);
			}
		}
		//System.out.println()
		return target;
	}

	// the source here represents the intersected part of the data
	public double[] scaleTiff(double[] source, double[] srcSize, double[] tSize){
		int tWidth = (int) tSize[1];
		int tHeight = (int) tSize[0];
		int srcWidth = (int) srcSize[1];
		int srcHeight = (int) srcSize[0];
		System.out.println(srcWidth + " " + srcHeight + "," + source.length);

		
		float widthUnit = tWidth/(float)srcWidth;
		float heightUnit = tHeight/(float)srcHeight;
		double[] target = new double[tWidth*tHeight];
		
		for(int h=0; h<tHeight; h++){
			for(int w=0; w<tWidth; w++){
				int xInSrc = (int) Math.floor((float)w/widthUnit);
				int yInSrc = (int) Math.floor((float)h/heightUnit);
				int indexInSrc = yInSrc*srcWidth + xInSrc;
				int indexInTarget = h*tWidth + w;
				target[indexInTarget] = source[indexInSrc];
				//System.out.println(target[indexInTarget]+","+w+","+h+","+xInSrc+", "+yInSrc +" ,"+indexInSrc);
			}
		}
		System.out.println("target sample: " + target[800]);
		return target;
	}

	public double[] calculateScarcity(double[] popSrc, double[] waterSrc, double[] tSize){
		int width = (int) tSize[1];
		int height = (int) tSize[0];
		double[] target = new double[width*height];
		int count = 0;
		for(int h=0; h<height; h++){
			for(int w=0; w<width; w++){
				int index = h*width + w;
				double popVal = popSrc[index];
				double waterVal = waterSrc[index];
				if(waterVal>0 && popVal>0){
					if(popVal<1.0)
						target[index] = waterVal;
					else
						target[index] = waterVal/popVal;
//					System.out.println("demand sample: " + popVal + " water sample: " + waterVal + " target sample: " + target[index]);
				}
				else{
					target[index] = 0;
				}
			}
		}
		System.out.println(count);
		return target;
	}
	
	public double sum(double[] data){
		double sum = 1;
		for(int i=0; i<data.length; i++)
		{
			if(data[i]>0)
				sum+=data[i];
		}	
		return sum;
	}
}
