//package edu.asu.waterDemo.main;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//
//import javax.ws.rs.DefaultValue;
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.QueryParam;
//
//import org.apache.http.HttpResponse;
//import org.apache.http.auth.AuthScope;
//import org.apache.http.auth.UsernamePasswordCredentials;
//import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.CredentialsProvider;
//import org.apache.http.client.methods.HttpPut;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.BasicCredentialsProvider;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.glassfish.jersey.server.JSONP;
//
//@Path("/changeAgreeStyle")
//public class ChangeAgreeStyle {
//	public class ChangeAgreeStyleBean{
//		public boolean changed;
//	};
//	
//	@GET
//	@JSONP(queryParam = "callback", callback = "eval")
//	@Produces({"application/x-javascript"})
//	public ChangeAgreeStyleBean query(
//			@QueryParam("coverageName") @DefaultValue("null") String coverageName,
//			@QueryParam("styleName") @DefaultValue("null") String styleName,
//			@QueryParam("workspace") @DefaultValue("null") String workspace,
//			@QueryParam("port") @DefaultValue("agree") String port) throws ClientProtocolException, IOException {
//		
//		ChangeAgreeStyleBean result = new ChangeAgreeStyleBean();
//		CredentialsProvider credsProvider = new BasicCredentialsProvider();
//		credsProvider.setCredentials(
//	            new AuthScope("localhost", 8080),
//	            new UsernamePasswordCredentials("admin", "geoserver"));
//		CloseableHttpClient httpclient = HttpClients.custom()
//                .setDefaultCredentialsProvider(credsProvider)
//                .build();
//		
//		String styleLayerURL = "http://localhost:"+port+"/geoserver/rest/layers/"+coverageName;
//		String createStyleXmlContent = "<layer><defaultStyle><name>"+styleName+"</name><workspace>"+workspace+"</workspace></defaultStyle></layer>";
//		StringEntity styleLayerEntity = new StringEntity(createStyleXmlContent);
//		styleLayerEntity.setContentType("text/xml");
//		HttpPut styleLayerPut = new HttpPut(styleLayerURL);
//		styleLayerPut.setEntity(styleLayerEntity);
////		
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
//		result.changed = true;
//		return result;
//		
//	}
//	
//}
