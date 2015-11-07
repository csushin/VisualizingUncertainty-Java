package edu.asu.waterDemo.main;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.asu.waterDemo.commonclasses.DrawTileThread;
import edu.asu.waterDemo.commonclasses.GenerateTiles;
import edu.asu.waterDemo.commonclasses.LatLng;
import edu.asu.waterDemo.commonclasses.TiffParser;
import edu.asu.waterDemo.main.GenerateUncertaintyTiles.imgBase64;


@Path("/drawTreeVis")
public class DrawTreeVisMap {
	private String preCalcDir;
	private String ModalEntropyDir;
	private String ModalMeanDir;
	private String ModalStdDir;
	private int NUMBER_OF_PROCESSORS = 30;
	
	public class imgBase64{
		public String imgStr;
	}
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")) {
			this.preCalcDir = context.getRealPath("img") + File.separatorChar;
		}else{
			this.preCalcDir = "/work/asu/data/CalculationResults" + File.separatorChar;
		}
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public imgBase64 query(
			@QueryParam("tfFunction") @DefaultValue("null") String tffunction,
			@QueryParam("levels") @DefaultValue("") String levels,
			@QueryParam("thresholds") @DefaultValue("") String thresholds,
			@QueryParam("mapPixelOrigin") @DefaultValue("0,0") String mapPixelOrigin,
			@QueryParam("zoomLevel") @DefaultValue("7") int zoomLevel,
			@QueryParam("treeTypeKeyword") @DefaultValue("") String treeTypeKeyword,
			@QueryParam("modal") @DefaultValue("") String modal,
			@QueryParam("dataType") @DefaultValue("") String dataType) throws ParseException {
//		note that the order of the threshold and levels should be the consistent
		imgBase64 result = new imgBase64();
		result.imgStr = "test";		
		JSONObject tfFuncJson = (JSONObject)new JSONParser().parse(tffunction);
		String[] levelsArr = levels.split(",");
		String[] _thresholdsArr = thresholds.split(",");
		double[] thresholdsArr = new double[_thresholdsArr.length];
		for (int i = 1; i<_thresholdsArr.length; i++) {
			thresholdsArr[i] = Double.valueOf(_thresholdsArr[i]);
		}
			
		HashMap<String, Color> selectedNodes = new HashMap<String, Color>();
		Iterator each = tfFuncJson.keySet().iterator();
		while(each.hasNext()){
			String key = (String)each.next();
			Color color = this.parse((String)tfFuncJson.get(key));
			if(color.getBlue() == 255 && color.getRed() == 255 && color.getGreen() == 255)
				continue;
			selectedNodes.put(key, color);
		}
		ArrayList<TiffParser> parsers = new ArrayList<TiffParser>();
		
		for(int i=1; i<levelsArr.length; i++){
			String dir = this.preCalcDir + dataType + "/" + levelsArr[i].replace(" ", "") + File.separatorChar;
			String filePath = getAllFiles(dir, treeTypeKeyword);
			parsers.add(new TiffParser(filePath));
		}
		if(parsers.isEmpty()){
			System.out.println("Parsers are empty!\n");
			return null;
		}
		
		Point2D mapPixelOriginPt = new Point2D.Double();
		mapPixelOriginPt.setLocation(Integer.valueOf(mapPixelOrigin.split(",")[0]), Integer.valueOf(mapPixelOrigin.split(",")[1]));
		GenerateTiles tile = new GenerateTiles("", mapPixelOriginPt, "treeVis", zoomLevel);
		double[] size = parsers.get(0).getSize();
		LatLng southwest = new LatLng(parsers.get(0).getLrLatlng()[0], parsers.get(0).getUlLatlng()[1]);
		LatLng northeast = new LatLng(parsers.get(0).getUlLatlng()[0], parsers.get(0).getLrLatlng()[1]);
		tile.processWidthHeight((int) size[1], (int) size[0], southwest, northeast);
		tile.initializeBufferImage();


		double[] sSize = parsers.get(0).getSize();
		int tgtHeight = (int)sSize[0];
		int tgtWidth = (int)sSize[1];
		DrawTileThread[] drawTileService = new DrawTileThread[NUMBER_OF_PROCESSORS];
		Thread[]  drawTileThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			drawTileService[i] = new DrawTileThread(startIndex, endIndex, thresholdsArr, tfFuncJson, parsers, tile, selectedNodes);
			drawTileThread[i] = new Thread(drawTileService[i]);
			drawTileThread[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				drawTileThread[i].join();
				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		result.imgStr = tile.encodeFromBufferImgToBase64();
		
		return result;
	}
	
	private String getAllFiles(String directoryName, String keyword) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile() && file.getName().endsWith(".tif") && file.getName().contains(keyword)) {
	        	return file.getAbsolutePath();
	        } 
	    }
	    return null;
	}
	
	private static Color parse(String input) 
	{
	    Pattern c = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
	    Matcher m = c.matcher(input);
	    if (m.matches()) 
	    {
	        return new Color(Integer.valueOf(m.group(1)),  // r
	                         Integer.valueOf(m.group(2)),  // g
	                         Integer.valueOf(m.group(3))); // b 
	    }
	    return null;  
	}
}
