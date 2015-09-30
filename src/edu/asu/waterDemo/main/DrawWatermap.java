//package edu.asu.waterDemo.main;
//
//import java.io.File;
//import java.net.UnknownHostException;
//import java.util.Arrays;
//import java.util.LinkedList;
//import java.util.List;
//
//import javax.servlet.ServletContext;
//import javax.ws.rs.DefaultValue;
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.QueryParam;
//import javax.ws.rs.core.Context;
//
//import org.glassfish.jersey.server.JSONP;
//
//import com.mongodb.BasicDBList;
//import com.mongodb.BasicDBObject;
//import com.mongodb.DB;
//import com.mongodb.DBCollection;
//import com.mongodb.DBCursor;
//import com.mongodb.MongoClient;
//
//import edu.asu.waterDemo.bean.ListPointsBean;
//import edu.asu.waterDemo.bean.DrawWatermapBean;
//import edu.asu.waterDemo.commonclasses.TiffParser;
//import edu.asu.waterDemo.commonclasses.newConnection;
//
//@Path("/getWaterPoints")
//public class DrawWatermap {
//	String supplyDir;
//	String demandDir;
//	String historicalDir;
//	String rcp45Dir;
//	String rcp85Dir;
//	String linux_demandDir;
//	String linux_supplyDir;
//	
//	@Context
//	public void setServletContext(ServletContext context) {
//		supplyDir = context.getRealPath("img") + File.separatorChar;
//		demandDir = context.getRealPath("img/demand") + File.separatorChar;
//		linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
//		linux_supplyDir = "/work/asu/data/wsupply/BW/"; 
//	}
//	
//	@GET
//	@JSONP(queryParam = "callback", callback = "eval")
//	@Produces({"application/x-javascript"})
//	public DrawWatermapBean query(@QueryParam("message") @DefaultValue("FALSE") String message, 
//			@QueryParam("supplyfName") @DefaultValue("null") String supplyfName,
//			@QueryParam("resolution") @DefaultValue("low") String resolution,
//			@QueryParam("partIdx") @DefaultValue("null") String partIdx,
//			@QueryParam("partIdy") @DefaultValue("null") String partIdy,
//			@QueryParam("batchNum") @DefaultValue("null") String batchNum) throws UnknownHostException {
//		DrawWatermapBean result = new DrawWatermapBean();
//		String supplyPath = "";
//		String collectionName = "";
//		if(message.equals("requestWater"))
//		{
//				double[] size = new double[2];
//				StringBuilder nameConstructor = new StringBuilder();
//				if(resolution.equals("high") || resolution.equals("low")){
//					if(resolution.equals("high")){
//						size[0] = 1800;
//						size[1] = 3600;						
//					}
//					if(resolution.equals("low")){
//						size[0] = 90;
//						size[1] = 180;									
//					}
//					supplyPath = supplyDir + "supply/" + supplyfName;
//					collectionName = parseName(supplyfName);
//					System.out.println(collectionName);
//					nameConstructor.append("geoTiff");
//					nameConstructor.append((int)size[1]);
//					nameConstructor.append("x");
//					nameConstructor.append((int)size[0]);
//					System.out.println(nameConstructor.toString());
//					DB db = newConnection.dbClient.getDB(nameConstructor.toString());				
//					DBCollection coll = db.getCollection(collectionName);
//					List<ListPointsBean> jsonList = new LinkedList<>();
//					ListPointsBean curIndex; 
//					
//					double[] valRange = new double[2];
//					double[] lrCoord = new double[2];
//					double[] ulCoord = new double[2];
//					
//					result.statistics = null;
//					result.geoInfoUnit = null;
//					result.ulLatlng = null;
//					result.lrLatlng = null;
//					result.size = size;
//					DBCursor cursor = coll.find();
//					
//					while(cursor.hasNext()){
//						curIndex= new ListPointsBean();
//						BasicDBObject obj = (BasicDBObject) cursor.next();
//						if(!obj.containsField("minLat")){
//							double value = ((Number)obj.get("value")).doubleValue();
//							BasicDBObject loc = (BasicDBObject) obj.get("loc");
//							BasicDBList points = (BasicDBList) loc.get("coordinates");
//							double lat = ((Number) points.get(0)).doubleValue();
//							double lng = ((Number) points.get(1)).doubleValue();
//							if(resolution.equals("low")){
//								double wIndex = ((Number)obj.get("wIndex")).doubleValue();
//								double hIndex = ((Number)obj.get("hIndex")).doubleValue();
//								curIndex.setwIndex(wIndex);
//								curIndex.sethIndex(hIndex);
//							}
//							curIndex.setLat(lat);
//							curIndex.setLng(lng);
//							curIndex.setValue(Math.round(value));
//							jsonList.add(curIndex);
//						}
//						else{
//							double minVal = ((Number)obj.get("minVal")).doubleValue();
//							double maxVal = ((Number)obj.get("maxVal")).doubleValue();
//							double minLat = ((Number)obj.get("minLat")).doubleValue();
//							double maxLat = ((Number)obj.get("maxLat")).doubleValue();
//							double minLng = ((Number)obj.get("minLng")).doubleValue();
//							double maxLng = ((Number)obj.get("maxLng")).doubleValue();
//							
//							valRange[0] = minVal;
//							valRange[1] = maxVal;
//							lrCoord[0] = minLat;
//							lrCoord[1] = maxLng;
//							ulCoord[0] = maxLat;
//							ulCoord[1] = minLng;
//							
//							result.ulLatlng = ulCoord;
//							result.lrLatlng = lrCoord;
//							result.statistics = valRange;
//						}
//					}
//					result.setDataList(jsonList);
//					
//				}
//				else if(resolution.equals("raw")){
//					// get lower resolution data
//					size[0] = 3600;
//					int hUnit = (int) (size[0]/Integer.parseInt(batchNum));
//					size[1] = 7200;
//					int wUnit = (int) (size[1]/Integer.parseInt(batchNum));
//					double[] partSize = {hUnit, wUnit};
//					supplyPath = supplyDir + "supply/" + supplyfName;
//					collectionName = parseName(supplyfName);
//					TiffParser tiffParser = new TiffParser();
//					tiffParser.setFilePath(supplyPath);
//					if(tiffParser.parser()){
//						double[] lrLatlng = tiffParser.getLrLatlng();//lower-right
//						double[] ulLatlng = tiffParser.getUlLatlng();//top-left
//						double[] geoInfoUnit = tiffParser.getGeoUnitPerUW();
//						double[] data = tiffParser.getData();
//						double[] statistics = tiffParser.getStatistics();
//						
//						double minVal = 999999999;
//						double maxVal = 0;
//						List<ListPointsBean> jsonList = new LinkedList<>();
//						ListPointsBean curIndex; 
//						for(int h=0; h<hUnit ;h++){
//							for(int w=0; w<wUnit; w++){
//								double value = data[(int)(h*wUnit+w)];
//								if(value>0)
//								{
//									curIndex= new ListPointsBean();
//									double lat = ulLatlng[0] + h*geoInfoUnit[0];
//									double lng = ulLatlng[1] + w*geoInfoUnit[1];
//									curIndex.setLat(lat);
//									curIndex.setLng(lng);
//									curIndex.setValue(Math.round(value));
//									jsonList.add(curIndex);
//								}
//							}
//						}
//						if(maxVal==0){
//							minVal = 0;
//						}
//						result.statistics = statistics;
//						result.geoInfoUnit = geoInfoUnit;
//						result.ulLatlng = ulLatlng;
//						result.lrLatlng = lrLatlng;
//						result.size = partSize;
//						result.setDataList(jsonList);
//					}
//				}
//				
//		}
//		
//		return result;
//	}
//	
//	public String parseName(String fName){
//		String _fName = fName.replace(".tif", "");
//		String[] __fName = _fName.split("/");
////		System.out.println(Arrays.asList(__fName));
//		return __fName[__fName.length-1];
//	}
//}
