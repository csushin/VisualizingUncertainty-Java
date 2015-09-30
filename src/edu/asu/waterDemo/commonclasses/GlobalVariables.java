package edu.asu.waterDemo.commonclasses;

import java.io.File;
import java.util.Arrays;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

public class GlobalVariables {
	public int targetWidth = 3600;
	public int targetHeight = 2640;
//	initialize with 1 as the alpha value
	public double[] priorAS = new double[targetWidth*targetHeight];
	

	public double[] priorNS = new double[targetWidth*targetHeight];
	public double[] priorStress = new double[targetWidth*targetHeight];
	public double[] priorScarcity = new double[targetWidth*targetHeight];
	public double[] prvJoint = new double[targetWidth*targetHeight];
	public double[] priorTotal = new double[targetWidth*targetHeight];
	public double[] geoInfo;
	public String projInfo;
	
	public void setGeoInfo(double[] geoInfo) {
		this.geoInfo = geoInfo;
	}

	public void setProjInfo(String projInfo) {
		this.projInfo = projInfo;
	}
	
	public void readPriorFile(String ASPath, String NSPath, String StressPath, String ScarPath, String totalPath, String prvJointPath){
		TiffParser parser = new TiffParser();
		priorAS = new double[targetWidth*targetHeight];
		priorNS = new double[targetWidth*targetHeight];
		priorStress = new double[targetWidth*targetHeight];
		priorScarcity = new double[targetWidth*targetHeight];
		priorTotal = new double[targetWidth*targetHeight];
		prvJoint = new double[targetWidth*targetHeight];
		priorAS = readPriorData(parser, ASPath);
		priorNS = readPriorData(parser, NSPath);
		priorStress = readPriorData(parser, StressPath);
		priorScarcity = readPriorData(parser, ScarPath);
		priorTotal = readPriorData(parser, totalPath);
		if(prvJointPath != null){
			File fPrvJt = new File(prvJointPath);
			if(fPrvJt.exists()){
				prvJoint = readPriorData(parser, prvJointPath);
			}			
		}
	}
	
	public void updatePriorData(String ASPath, String NSPath, String StressPath, String ScarPath, String totalPath, String prvJointPath){
		savePrior(priorTotal, totalPath);
		savePrior(priorAS, ASPath);
		savePrior(priorNS, NSPath);
		savePrior(priorStress, StressPath);
		savePrior(priorScarcity, ScarPath);
		if(prvJointPath != null){
			savePrior(prvJoint, prvJointPath);		
		}
	}

	public void savePrior(double[] targetArray, String targetDir){
		File targetFile = new File(targetDir);
		if(targetFile.exists()){
			targetFile.delete();
		}
		Driver driver = gdal.GetDriverByName("GTiff");
		Dataset dst_ds = driver.Create(targetDir, targetWidth, targetHeight, 1, gdalconst.GDT_Float64);
		dst_ds.SetGeoTransform(geoInfo);
		dst_ds.SetProjection(projInfo);
		int result = dst_ds.GetRasterBand(1).WriteRaster(0, 0, targetWidth, targetHeight, targetArray);
		dst_ds.delete();
		System.out.println("Writing geotiff result is: " + result);			
	}
	
	public double[] readPriorData(TiffParser parser, String path){
		File data = new File(path);
		double[] prior = new double[targetWidth*targetHeight];
		if(data.exists()){
			parser.setFilePath(path);
			if(parser.parser()){
				prior = parser.getData();
			}
			else{
				System.out.println("Cannot parse the prior data given path: " + path);
			}
		}
		else{
			Arrays.fill(prior, 1);
			if(path.contains("Total")){
				Arrays.fill(prior, 4);
			}
			if(path.contains("Joint")){
				Arrays.fill(prior, 0);
			}
		}
		return prior;			
	}
	
	
	public double getPrvJoint(int index) {
		return prvJoint[index];
	}

	public void setPrvJoint(int index, double value) {
		this.prvJoint[index] = value;
	}
	
	public double getPriorAS(int index) {
		return priorAS[index];
	}

	public void setPriorAS(int index, double value) {
		this.priorAS[index] = value;
	}

	public double getPriorNS(int index) {
		return priorNS[index];
	}

	public void setPriorNS(int index, double value) {
		this.priorNS[index] = value;
	}

	public double getPriorStress(int index) {
		return priorStress[index];
	}

	public void setPriorStress(int index, double value) {
		this.priorStress[index] = value;
	}

	public double getPriorScarcity(int index) {
		return priorScarcity[index];
	}

	public void setPriorScarcity(int index, double value) {
		this.priorScarcity[index] = value;
	}

	public double getPriorTotal(int index) {
		return priorTotal[index];
	}

	public void setPriorTotal(int index, double value) {
		this.priorTotal[index] = value;
	}
}
