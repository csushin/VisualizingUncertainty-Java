package edu.asu.waterDemo.commonclasses;
import java.awt.Point;


public final class GoogleMercator {
	private final int TILE_SIZE = 256;
	private Point pixelOrigin;
	private double pixelsPerLonDegree;
	private double pixelsPerLonRadian;
	private int zoom;

	public void setZoom(int z) {
		zoom = z;
	}

	public GoogleMercator() {
		pixelOrigin = new Point(TILE_SIZE / 2, TILE_SIZE / 2);
		pixelsPerLonDegree = TILE_SIZE / 360.0;
		pixelsPerLonRadian = TILE_SIZE / (2 * Math.PI);
	}

	public double degreesToRadians(double deg) {
		return deg * (Math.PI / 180);
	}

	public double radiansToDegrees(double rad) {
		return rad / (Math.PI / 180);
	}

	private double bound(double value, double opt_min, double opt_max) {
		double result = Math.max(value, opt_min);
		result = Math.min(value, opt_max);
		return result;
	}

	public Point fromLatLngToPoint(LatLng latLng) {
		Point point = new Point();
		int numTiles = 1 << zoom;
		double x, y;
		x = pixelOrigin.x + latLng.getLng() * pixelsPerLonDegree;
		double siny = bound(Math.sin(degreesToRadians(latLng.getLat())),
				-0.9999, 0.9999);
		y = pixelOrigin.y + 0.5 * Math.log((1 + siny) / (1 - siny))
				* -pixelsPerLonRadian;
		x *= numTiles;
		y *= numTiles;
		point.setLocation((int) x, (int) y);
		return point;
	}

	public LatLng fromPointToLatLng(Point p) {
		int numTiles = 1 << zoom;
		double x = p.x / (double) numTiles;
		double y = p.y / (double) numTiles;
		double lng = (x - pixelOrigin.x) / pixelsPerLonDegree;
		double latRadius = (y - pixelOrigin.y) / -pixelsPerLonRadian;
		double lat = radiansToDegrees(2 * Math.atan(Math.exp(latRadius))
				- Math.PI / 2);
		return new LatLng(lat, lng);
	}

	public double distance(LatLng a, LatLng b) {
		double radius = 6378.137;
		double dLat = degreesToRadians(a.getLat() - b.getLat());
		double dLng = degreesToRadians(a.getLng() - b.getLng());
		double latA = degreesToRadians(a.getLat());
		double latB = degreesToRadians(b.getLat());

		double x = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLng / 2)
				* Math.sin(dLng / 2) * Math.cos(latA) * Math.cos(latB);
		double y = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
		return radius * y;
	}

}
