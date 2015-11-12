package edu.asu.waterDemo.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.glassfish.jersey.server.JSONP;

import edu.asu.waterDemo.commonclasses.GenerateTiles;
import edu.asu.waterDemo.commonclasses.LatLng;
import edu.asu.waterDemo.commonclasses.TiffParser;

@Path("/getMapData2MultiEnsemble")
public class GetMapData2MultiEnsemble {
	private String basisDir;
	private String targetPath;
	private int NUMBER_OF_PROCESSORS = 30;
	private String xmetricDir;
	private String ymetricDir;
	
	public class GetMapForOverviewBean{
		public String imgStr;
	}
	
	@Context
	public void setServletContext(ServletContext context) {
		String osName = System.getProperty("os.name");
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("windows")) {
			this.basisDir = context.getRealPath("img") + File.separatorChar;
		}else{
			this.basisDir = "/work/asu/data/CalculationResults" + File.separatorChar;
		}
	}
	
	public class DrawOverviewMapThreads implements Runnable{
		private int startIndex;
		private int endIndex;
		private TiffParser xparser;
		private TiffParser yparser;
		private double[] xthresholds;
		private double[] ythresholds;
		private String[] tfFunction;
		private double[] xMinmax;
		private double[] yMinmax;
		private GenerateTiles tile;
		private TiffParser comparedXParser;
		
		public DrawOverviewMapThreads(int startIndex, int endIndex, TiffParser xParser, TiffParser yParser, double[] xthresholds, double[] ythresholds, double[] xMinmax, double[] yMinmax, String[] tfFucntion, GenerateTiles tile, TiffParser comparedXParser){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.xparser = xParser;
			this.yparser = yParser;
			this.xthresholds = xthresholds;
			this.ythresholds = ythresholds;
			this.tfFunction = tfFucntion;
			this.xMinmax = xMinmax;
			this.yMinmax = yMinmax;
			this.tile = tile;
			this.comparedXParser = comparedXParser;
		}
		
		@Override
		public void run() {
			for(int index=this.startIndex; index<this.endIndex; index++){
				int h = index/(int)this.xparser.getSize()[1];
				int w = index%(int)this.xparser.getSize()[1];
				double lat = this.xparser.getUlLatlng()[0] + h*this.xparser.getGeoInfo()[5];
				double lng = this.xparser.getUlLatlng()[1] + w*this.xparser.getGeoInfo()[1];
				double xvalue = this.xparser.getData()[index];
				double yvalue = this.yparser.getData()[index];
				double comparedXValue = this.comparedXParser.getData()[index];
//				normalize them into range 0~1
				double delta = xvalue-comparedXValue;
				double xnormalized = (delta - xMinmax[0])/(xMinmax[1] - xMinmax[0]);
				double ynormalized = (yvalue - yMinmax[0])/(yMinmax[1] - yMinmax[0]);
//				position of the values in the threshold
				int xpos = (int) (xnormalized / (1.0/(double)this.xthresholds.length));
				int ypos = (int) (ynormalized / (1.0/(double)this.ythresholds.length));
				this.tile.drawTiles(xpos, ypos, this.xthresholds, this.ythresholds, this.tfFunction, this.xMinmax, this.yMinmax, lat, lng);
			}
		}
		
	}
	
	@GET
	@JSONP(queryParam = "callback", callback = "eval")
	@Produces({"application/x-javascript"})
	public GetMapForOverviewBean query(
			@QueryParam("dataType") @DefaultValue("null") String dataType,
			@QueryParam("zoomLevel") @DefaultValue("null") String zoomLevel,
			@QueryParam("colorTable") @DefaultValue("null") String colorTable,
			@QueryParam("xthresholds") @DefaultValue("null") String xthresholds,
			@QueryParam("ythresholds") @DefaultValue("null") String ythresholds,
			@QueryParam("xmetric") @DefaultValue("null") String xmetric,
			@QueryParam("ymetricA") @DefaultValue("null") String ymetricA,
			@QueryParam("ymetricB") @DefaultValue("null") String ymetricB){
		GetMapForOverviewBean result = new GetMapForOverviewBean();
		String _dataType = dataType;
		if(dataType.equals("Precipitation"))
			_dataType = "pr_HIST";
		if(dataType.equals("TemperatureMin"))
			_dataType = "tasmin_HIST";
		this.xmetricDir = this.basisDir + _dataType + "/GlobleStat/" + xmetric + ".tif";
		String comparedXDir = this.basisDir + dataType + "/EnsembleStatOf" + ymetricB + "/" + "EnsembleStdOf" + ymetricB + ".tif";
		this.ymetricDir = this.basisDir + _dataType + "/EnsembleStatOf" + ymetricB + "/" + ymetricA + "Of" + ymetricB + ".tif";
		this.targetPath = this.basisDir + _dataType + "/Ensemble2Models/" + xmetric + "_" + ymetricA + "Of" + ymetricB + "_zoomLevel" + zoomLevel + ".tif";
		String imgPath = this.targetPath.replace(".tif", "_zoomLevel"+ zoomLevel +".png");
		
		TiffParser xparser = new TiffParser(this.xmetricDir);
		
		String[] colortf = colorTable.split("&");
		System.out.println("colortf: ");
		System.out.println(colortf);
		GenerateTiles tile = new GenerateTiles(imgPath, null, "overviewVis", Integer.valueOf(zoomLevel), colortf);
		double[] size = xparser.getSize();
		LatLng southwest = new LatLng(xparser.getLrLatlng()[0], xparser.getUlLatlng()[1]);
		LatLng northeast = new LatLng(xparser.getUlLatlng()[0], xparser.getLrLatlng()[1]);
		tile.processWidthHeight((int) size[1], (int) size[0], southwest, northeast);
		tile.initializeBufferImage();
		File imgFile = new File(imgPath);
		// look for base64 img string
		// if the img already exists, then convert it to base64 and return the string
		if(imgFile.exists()){
			result.imgStr = tile.encodeFromReaderToBase64(imgPath, "PNG");
			return result;
		}
		
		TiffParser comparedXParser = new TiffParser(comparedXDir);
		TiffParser yparser = new TiffParser(this.ymetricDir);
		
		double[] yMinmax = yparser.getMinmax();
		double[] _xthreshold = new double[xthresholds.split(",").length]; 
		double[] _ythreshold = new double[ythresholds.split(",").length];
		for(int i=0; i<_xthreshold.length; i++)
			_xthreshold[i] = Double.valueOf(xthresholds.split(",")[i]);
		for(int j=0; j<_ythreshold.length; j++)
			_ythreshold[j] = Double.valueOf(ythresholds.split(",")[j]);
		
		int tgtHeight = (int)size[0];
		int tgtWidth = (int)size[1];
//		look for the minimum and maximum value of the differences between ensemble std and global std
		double[] deltaMinMax = {9999999,0};
		for(int i=0; i<tgtHeight*tgtWidth; i++){
			double delta = xparser.getData()[i]-comparedXParser.getData()[i];
			if(delta<deltaMinMax[0])
				deltaMinMax[0] = delta;
			if(delta>deltaMinMax[1])
				deltaMinMax[1] = delta;
		}
		DrawOverviewMapThreads[] drawOverviewMapService = new DrawOverviewMapThreads[NUMBER_OF_PROCESSORS];
		Thread[]  drawOverviewMapThread = new Thread[NUMBER_OF_PROCESSORS];
		int delta = tgtHeight/NUMBER_OF_PROCESSORS;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = h1 * tgtWidth;
			int endIndex =  h2 * tgtWidth;
			drawOverviewMapService[i] = new DrawOverviewMapThreads(startIndex, endIndex, xparser, yparser, _xthreshold, _ythreshold, deltaMinMax, yMinmax, colortf, tile, comparedXParser);
			drawOverviewMapThread[i] = new Thread(drawOverviewMapService[i]);
			drawOverviewMapThread[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				drawOverviewMapThread[i].join();
//				System.out.println(i + " Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		result.imgStr = tile.writeBufferImage();
		return result;
	}
	
}
