package edu.asu.waterDemo.commonclasses;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class GenerateTiles {
	private String outputPath;
	private String[] rgbTable = {"255,0,0", "254,178,76", "173,221,142", "49,163,84"};
	private int[] alphaTable;
	private Point2D initialTopLeftPoint;
//	private static double MAX_LATITUDE = 85.0511287798;
	private double tileWidth;
	private double tileHeight;
	private BufferedImage img;
	private File fObject;
	private Point pixelSW;
	private Point pixelNE;
	private String type;
	private int zoomLevel;


	//	To get the initialTopLeftMapPoint, we should call the map.getPixelOrigin() function in leaflet	
	public GenerateTiles(String outputfile, Point2D mapOrigin, String type, int zoomLevel){
		this.setOutputPath(outputfile);	
		this.setType(type);
		this.setInitialTopLeftPoint(mapOrigin);
		this.setZoomLevel(zoomLevel);
	}
	
	public void processWidthHeight(int xSize, int ySize, LatLng southwest, LatLng northeast){
		Point pixelSW = this.latLngToLayerPoint(southwest);
		Point pixelNE = this.latLngToLayerPoint(northeast);
		this.setPixelNE(pixelNE);
		this.setPixelSW(pixelSW);
		double tileWidth = Math.abs(pixelNE.getX() - pixelSW.getX() + 1); 
		double tileHeight = Math.abs(pixelNE.getY() - pixelSW.getY() + 1);
		this.setTileWidth(tileWidth);
		this.setTileHeight(tileHeight);
	}
	
	public void initializeBufferImage(){
		BufferedImage img = new BufferedImage( 
				(int) this.getTileWidth(), (int) this.getTileHeight(), BufferedImage.TYPE_INT_ARGB );
		File f = new File(this.getOutputPath());
		this.setImg(img);
		this.setfObject(f);
	}
	
	public boolean writeBufferImage(){
		try{
			ImageIO.write(this.getImg(), "PNG", this.getfObject());
			System.out.println(this.getType() + " png tiles are finished!");
			return true;
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Failure in writing tiles for " + this.outputPath);
			return false;
		}
	}
	
	public void drawTiles(double mean, double stat, int originIndex, double lat, double lng) throws IOException{
		LatLng latLng = new LatLng(lat, lng);
		Point2D pt = this.latLngToLayerPoint(latLng);
		int wImg = (int) (pt.getX() - this.getPixelSW().getX());
		int hImg = (int) (pt.getY() - this.getPixelNE().getY());
		int alpha = this.indexAlpha(stat, mean);
		int[] rgb = this.indexRGB(mean);
		int color = (alpha<<24) | (rgb[0]<<16) | (rgb[1]<<8) | rgb[2];
//		System.out.println("wImg: " + wImg + " hImg: " + hImg);
		if(wImg<this.getImg().getWidth() && hImg<this.getImg().getHeight())
			this.getImg().setRGB(wImg, hImg, color);
		
	}
	
//	return the value from 0~255
	private int indexAlpha(double val, double meanVal){
//		set the full transparency for those NaN Values
		if(val == -1 || meanVal == -1)
			return 0;
		int result = 0;
		double min=0, max=0;
		if(this.getType().equals("variance")){
			min = 0;
//			according to the formula on:
//			http://math.stackexchange.com/questions/83046/maximum-of-the-variance-function-for-given-set-of-bounded-numbers
//			the max of variance is 
			max = 25.0/4.0;
			result = (int) Math.round((val-min)*255.0/(max-min));
		}
		else if(this.getType().equals("entropy")){
			min = 0;
			max = 1.0;
			result = (int) Math.round((val-min)*255.0/(max-min));
		}
		else if(this.getType().equals("agree")){
			min = 0.4;
			max = 1.0;
			result = (int) Math.round((max-val)*255.0/(max-min));
		}
		else{
			System.out.println("Cannot recognize the input type in mapping alpha value during the generation of tiles!");
		}
		
		return result;
	}
	
//	return the value from 0~255 in the order of r/g/b
	private int[] indexRGB(double val){
		if(val == -1){
			int[] result = {255,255,255};
			return result;
		}
		int[] result = new int[3];
		int index = (int) Math.round(val);
//		for those whose mean is 0.xxx<0.5, assigning them to 1 (Abs. Scar.)
		if(index == 0)
				index = 1;
		String color = this.getRGBTable()[index-1];
		result[0] = Integer.valueOf(color.split(",")[0]);
		result[1] = Integer.valueOf(color.split(",")[1]);
		result[2] = Integer.valueOf(color.split(",")[2]);
		return result;
	}
	
	private Point latLngToLayerPoint(LatLng latlng){
		Point2D projectedPoint = new Point2D.Double();
//		projectedPoint.x = Math.round(projectedPoint.x);
//		projectedPoint.y = Math.round(projectedPoint.y);
//		double x = projectedPoint.x - this.initialTopLeftPoint.x;
//		double y = projectedPoint.y - this.initialTopLeftPoint.y;
		double x = latlng.getLng();
		double y = latlng.getLat();
		projectedPoint.setLocation(x, y);
//		Notice the difference of Point2D and Point classes. 
//		The former supports double variable and another only supports integer in default
		Point trsPt = this.transformation(projectedPoint);
		return trsPt;
//		float earthRadius = 6378137;
//		x = projectedPoint.x * earthRadius;
//		y = projectedPoint.y * earthRadius;
//		Point result = new Point();
//		result.setLocation(x, y);
//		return result;
	}
	
	private Point transformation(Point2D prjPt){
		Point result = new Point();
		double a = 1/360.0, b = 0.5, c = -1/360.0, d = 0.5;
		int scale = (int)(256*Math.pow(2, this.getZoomLevel()));
		double x = scale * (a * prjPt.getX() + b);
		double y = scale * (c * prjPt.getY() + d);
		result.setLocation(x, y);
		return result;
	}
	
//	private Point project(LatLng latlng){
//		double degreeToRad = Math.PI / 180.0;
//		double lat = Math.max(Math.min(this.MAX_LATITUDE, latlng.getLat()), -this.MAX_LATITUDE);
//		double x = latlng.getLng() * degreeToRad;
//		double _y = lat * degreeToRad;
//		double y = Math.log(Math.tan((Math.PI/4.0)+(_y/2.0)));
//		Point result = new Point();
//		result.setLocation(x,y);
//		return result;
//	}
	
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public Point2D getInitialTopLeftPoint() {
		return initialTopLeftPoint;
	}

	public void setInitialTopLeftPoint(Point2D mapOrigin) {
		this.initialTopLeftPoint = mapOrigin;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public String[] getRGBTable() {
		return rgbTable;
	}

	public void setRGBTable(String[] rgbTable) {
		this.rgbTable = rgbTable;
	}

	public int[] getAlphaTable() {
		return alphaTable;
	}

	public void setAlphaTable(int[] alphaTable) {
		this.alphaTable = alphaTable;
	}
	
	public double getTileWidth() {
		return tileWidth;
	}

	public void setTileWidth(double tileWidth) {
		this.tileWidth = tileWidth;
	}

	public double getTileHeight() {
		return tileHeight;
	}

	public void setTileHeight(double tileHeight) {
		this.tileHeight = tileHeight;
	}
	
	public BufferedImage getImg() {
		return img;
	}

	public void setImg(BufferedImage img) {
		this.img = img;
	}
	
	public File getfObject() {
		return fObject;
	}

	public void setfObject(File fObject) {
		this.fObject = fObject;
	}
	
	public Point2D getPixelSW() {
		return pixelSW;
	}

	public void setPixelSW(Point pixelSW) {
		this.pixelSW = pixelSW;
	}

	public Point getPixelNE() {
		return pixelNE;
	}

	public void setPixelNE(Point pixelNE2) {
		this.pixelNE = pixelNE2;
	}
	
	public int getZoomLevel() {
		return zoomLevel;
	}

	public void setZoomLevel(int zoomLevel) {
		this.zoomLevel = zoomLevel;
	}
}
