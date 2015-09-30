package edu.asu.waterDemo.main;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.JSONP;

import com.google.common.primitives.Doubles;

import edu.asu.waterDemo.commonclasses.GenerateTiles;
import edu.asu.waterDemo.commonclasses.LatLng;
import edu.asu.waterDemo.commonclasses.TiffParser;


@Path("/drawMNProbability")
public class DrawMNJointPr {
	
	public class ImgBase64{
		public String imgStr;
	}
	
	public String linux_demandDir;
	public String linux_supplyDir;
	public String linux_prbDir;
	public static String PREDJOINT  = "predJoint";
	public static String PRIOR  = "prior";
	public static int INFINITE = -99999;
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")){
			this.linux_demandDir = context.getRealPath("img/demand") + File.separatorChar;
			this.linux_prbDir = context.getRealPath("img/probability") + File.separatorChar;
			this.linux_supplyDir = context.getRealPath("img/supply") + File.separatorChar;
		}
		else{
			this.linux_demandDir = "/work/asu/data/wdemand/popden_pred/";
			this.linux_prbDir = "/work/asu/data/multinomial/";
			this.linux_supplyDir = "/work/asu/data/wsupply/BW_1km/"; 
		}

	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public ImgBase64 query(
			@QueryParam("yearChain") @DefaultValue("null") String yearChain,
			@QueryParam("yearIndex") @DefaultValue("1") int yearIndex,
			@QueryParam("calctype") @DefaultValue("null") String calctype,
			@QueryParam("tfFunction") @DefaultValue("255,0,0|254,178,76|240,210,0|49,163,84") String tfFunction,
			@QueryParam("mapPixelOrigin") @DefaultValue("0,0") String mapPixelOrigin,
			@QueryParam("zoomLevel") @DefaultValue("4") int zoomLevel,
			@QueryParam("binSize") @DefaultValue("0.2") double binSize) throws IOException{
		ImgBase64 result = new ImgBase64();
		int baseYear = Integer.valueOf(yearChain.split(",")[yearIndex-1]);
		int prdYear = Integer.valueOf(yearChain.split(",")[yearIndex]);
		String targetTiffFile = "";
		String targetPNGFile = "";
		if(calctype.equalsIgnoreCase("predJoint")){
			targetTiffFile = linux_prbDir + "priorPrvJt_" + prdYear + ".tif";
			targetPNGFile = linux_prbDir + "priorPrvJt_" + prdYear + ".png";
		}
		else if(calctype.equalsIgnoreCase("evd")){
			targetTiffFile = linux_prbDir + "Evidence_" + prdYear + ".tif";
			targetPNGFile = linux_prbDir + "Evidence_" + prdYear + ".png";
		}
		String[] mapPixelOriginArr = mapPixelOrigin.split(",");
		Point2D mapPixelOriginPt = new Point2D.Double();
		mapPixelOriginPt.setLocation(Integer.valueOf(mapPixelOriginArr[0]), Integer.valueOf(mapPixelOriginArr[1]));
		String[] tfFunctionArr = tfFunction.split("\\|");
		
		GenerateTiles tile = new GenerateTiles(targetPNGFile, mapPixelOriginPt, calctype, zoomLevel, tfFunctionArr);
		
		TiffParser parser = new TiffParser();
		parser.setFilePath(targetTiffFile);
		if(parser.parser()){
			int width = (int) parser.getSize()[1];
			int height = (int) parser.getSize()[0];
			
			LatLng southwest = new LatLng(parser.getLrLatlng()[0], parser.getUlLatlng()[1]);
			LatLng northeast = new LatLng(parser.getUlLatlng()[0], parser.getLrLatlng()[1]);
			tile.processWidthHeight(width, height, southwest, northeast);
			tile.initializeBufferImage();
			
			for(int h=0; h<height; h++){
				for(int w=0; w<width; w++){
					int index = h*width + w;
					double lat = parser.getUlLatlng()[0] + h*parser.getGeoInfo()[5];
					double lng = parser.getUlLatlng()[1] + w*parser.getGeoInfo()[1];
					double value = parser.getData()[index];
//					normalize the value
					if(!calctype.equalsIgnoreCase("evd") && value!=INFINITE)
						value = Math.floor(value/binSize);
					if(calctype.equalsIgnoreCase("evd") && value!=INFINITE){
//						if(value!=0) System.out.println("value is " + value);
						value = Math.floor(value*10.0/binSize);
					}
					tile.drawTiles(value, 0, index, lat, lng);
				}
			}
			String base64 = tile.writeBufferImage();
			result.imgStr = base64;
		}
		else{
			result.imgStr = null;
		}
		
		return result;
	}
	
}
