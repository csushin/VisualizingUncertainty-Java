package edu.asu.waterDemo.main;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

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

public class GeoserverService {
	
	private String name;
	private String dataPath;
	private String port;
	private String ws;
	private String style;
	
	public GeoserverService(String name, String dataPath, String port, String ws, String stlye) {
		this.name = name;
		this.dataPath = dataPath;
		this.port = port;
		this.ws = ws;
		this.style = stlye;
	}	
	
	public boolean isExistance() throws ClientProtocolException, IOException{
		boolean allExistance = true;
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
	            new AuthScope("localhost",  Integer.parseInt(port)),
	            new UsernamePasswordCredentials("admin", "geoserver"));
		CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
		/////////////////////////////////////check if have data///////////////////////////////////////////
		File data = new File(dataPath);
		if(!data.exists()){
			allExistance = false;
			return allExistance;
		}
		
		/////////////////////////////////////check if have coverage store///////////////////////////////////////////
		String getCoveragesURL = "http://localhost:"+port+"/geoserver/rest/workspaces/"+ws+"/coveragestores/";
		HttpGet coverageListGet = new HttpGet(getCoveragesURL);
		HttpResponse getCoverageResponse = httpclient.execute(coverageListGet);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				getCoverageResponse.getEntity().getContent()));
		String buffer = "";
		String line = reader.readLine();
		buffer = line;
		while ((line = reader.readLine()) != null) {
			buffer += line;
		}
		reader.close();
//		System.out.println("get coverage: " + buffer );
		if(!buffer.contains(name)){
			allExistance =  false;
			return allExistance;
		}
		
		/////////////////////////////////////check if have layer///////////////////////////////////////////
		String getLayersURL = "http://localhost:"+port+"/geoserver/rest/layers/";
		HttpGet layerListGet = new HttpGet(getLayersURL);
		HttpResponse getLayerResponse = httpclient.execute(layerListGet);
		BufferedReader layerReader = new BufferedReader(new InputStreamReader(
				getLayerResponse.getEntity().getContent()));
		buffer = "";
		line = layerReader.readLine();
		buffer = line;
		while ((line = layerReader.readLine()) != null) {
			buffer += line;
		}
//		System.out.println("get Layers: " + buffer );
		if(!buffer.contains(name)){
			allExistance = false;
			return allExistance;
		}
		
		httpclient.close();
		return allExistance;
	}
	
	public boolean deleteAll() throws ClientProtocolException, IOException{
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
	            new AuthScope("localhost", Integer.parseInt(port)),
	            new UsernamePasswordCredentials("admin", "geoserver"));
		CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
		
		/////////////////////////////////////delete data////////////////////
		File data = new File(dataPath);
		if(data.exists()){
			data.delete();
		}
		
		/////////////////////////////////////check if the coverage exists in the geoserver//////////////////////////////////////
		String getCoveragesURL = "http://localhost:"+port+"/geoserver/rest/workspaces/"+ws+"/coveragestores/";
		HttpGet coverageListGet = new HttpGet(getCoveragesURL);
		HttpResponse getCoverageResponse = httpclient.execute(coverageListGet);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				getCoverageResponse.getEntity().getContent()));
		String buffer = "";
		String line = reader.readLine();
		buffer = line;
		while ((line = reader.readLine()) != null) {
			buffer += line;
		}
		reader.close();
		if(buffer.contains(name)){
			/////////////////////////////////////delete coverage store(layer would be deleted with the coverage)////////////////////
			String delCoveragesURL = "http://localhost:"+port+"/geoserver/rest/workspaces/"+ws+"/coveragestores/"+name+"?recurse=true";
			HttpDelete coverageDel = new HttpDelete(delCoveragesURL);
			HttpResponse delCoverageResponse = httpclient.execute(coverageDel);
			BufferedReader delReader = new BufferedReader(new InputStreamReader(
			delCoverageResponse.getEntity().getContent()));
			String delBuffer = "";
			String delLine = delReader.readLine();
			delBuffer = delLine;
			while ((delLine = delReader.readLine()) != null) {
				delBuffer += delLine;
			}
			delReader.close();
//			return true;
//			if(delBuffer != null){
//				System.out.println(delBuffer);
//				System.out.println("deleted coverage !");
//				return true;
//			}
//			else{
//				System.out.println("Cannot delete coverage !");
//				return false;
//			}			
		}
		
		httpclient.close();
		return true;
	}
	
	public boolean generateCoverage() throws ClientProtocolException, IOException{

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
	            new AuthScope("localhost",  Integer.parseInt(port)),
	            new UsernamePasswordCredentials("admin", "geoserver"));
		CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
		/////////////////////////////////////create Coverage Store///////////////////////////////////////////
		String createCoverageURL = "http://localhost:"+port+"/geoserver/rest/workspaces/"+ws+"/coveragestores?configure=all";
		HttpPost loginPost = new HttpPost(createCoverageURL);
		String createCoverageXmlContent = "<coverageStore><name>"+name+"</name><workspace>"+ws+"</workspace><enabled>true</enabled><type>GeoTIFF</type><url>"+dataPath+"</url></coverageStore>";
		StringEntity converageEntity = new StringEntity(createCoverageXmlContent);
		converageEntity.setContentType("text/xml");
		loginPost.setEntity(converageEntity);
		HttpResponse createCoverageResponse = httpclient.execute(loginPost);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				createCoverageResponse.getEntity().getContent()));
		String buffer = "";
		String line = reader.readLine();
		buffer = line;
		while ((line = reader.readLine()) != null) {
			buffer += line;
		}
		reader.close();
		System.out.println("create coverage: " + buffer);
		
		/////////////////////////////////////create Single Coverage///////////////////////////////////////////
		String createLayerURL = "http://localhost:"+port+"/geoserver/rest/workspaces/"+ws+"/coveragestores/"+name+"/coverages";
		String createLayerXmlContent = "<coverage><name>"+name+"</name><title>"+name+"</title><enabled>true</enabled></coverage>";
		StringEntity layerEntity = new StringEntity(createLayerXmlContent);
		layerEntity.setContentType("text/xml");
		HttpPost layerPost = new HttpPost(createLayerURL);
		layerPost.setEntity(layerEntity);
		HttpResponse layerResponse = httpclient.execute(layerPost);
		
		BufferedReader layerReader = new BufferedReader(new InputStreamReader(
				layerResponse.getEntity().getContent()));
		String layerBuffer = "";
		String layerLine = layerReader.readLine();
		layerBuffer = layerLine;
		while ((layerLine = layerReader.readLine()) != null) {
			layerBuffer += layerLine;
		}
		layerReader.close();
		System.out.println("create layer: " + layerBuffer);
		
		
		/////////////////////////////////////style layer///////////////////////////////////////////
//		String styleLayerURL = "http://localhost:"+port+"/geoserver/rest/layers/"+name;
//		String createStyleXmlContent = "<layer><defaultStyle><name>"+style+"</name><workspace>"+ws+"</workspace></defaultStyle></layer>";
//		StringEntity styleLayerEntity = new StringEntity(createStyleXmlContent);
//		styleLayerEntity.setContentType("text/xml");
//		HttpPut styleLayerPut = new HttpPut(styleLayerURL);
//		styleLayerPut.setEntity(styleLayerEntity);
//		HttpResponse styleLayerResponse = httpclient.execute(styleLayerPut);
//		
//		BufferedReader styleLayerReader = new BufferedReader(new InputStreamReader(
//				styleLayerResponse.getEntity().getContent()));
//		String styleLayerBuffer = "";
//		String styleLayerLine = styleLayerReader.readLine();
//		styleLayerBuffer = styleLayerLine;
//		while ((styleLayerLine = styleLayerReader.readLine()) != null) {
//			styleLayerBuffer += styleLayerLine;
//		}
//		System.out.println("style layer: " + styleLayerBuffer);
		
		httpclient.close();
		return true;
	}
}
