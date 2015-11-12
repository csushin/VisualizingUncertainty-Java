//package edu.asu.waterDemo.main;
//
//import LoadTiffThread;
//import TiffParser;
//
//import java.io.File;
//import java.util.ArrayList;
//
//import javax.servlet.ServletContext;
//import javax.ws.rs.DefaultValue;
//import javax.ws.rs.GET;
//import javax.ws.rs.Produces;
//import javax.ws.rs.QueryParam;
//import javax.ws.rs.core.Context;
//
//import org.glassfish.jersey.server.JSONP;
//
//
//@Path("GetGlobalMinMax")
//public class GetGlobalMinMaxOverview {
//	private String dataType;
//	private String metricType;
//	private String basisDir;
//	private String targetPath;
//	
//	public class GetGlobalMinMaxBean{
//		public double[] minmax;
//	}
//	
//	@Context
//	public void setServletContext(ServletContext context) {
//		String osName = System.getProperty("os.name");
//		String osNameMatch = osName.toLowerCase();
//		if(osNameMatch.contains("windows")) {
//			this.basisDir = context.getRealPath("img") + File.separatorChar;
//		}else{
//			this.basisDir = "/work/asu/data/CalculationResults" + File.separatorChar;
//		}
//	}
//	
//	@GET
//	@JSONP(queryParam = "callback", callback = "eval")
//	@Produces({"application/x-javascript"})
//	public GetGlobalMinMaxBean query(@QueryParam("metricType") @DefaultValue("null") String metricType,
//			@QueryParam("dataType") @DefaultValue("null") String dataType){
//		this.targetPath = this.basisDir + dataType + "/" + metricType;
//		ArrayList<File> targets = new ArrayList<File>();
//		targets = getAllFiles(this.targetPath, targets);
//		
//	}
//	
//	public ArrayList<TiffParser> parseFilesThread(ArrayList<File> files, ArrayList<TiffParser> parsers){
//		LoadTiffThread[] service = new LoadTiffThread[files.size()];
//		Thread[] serverThread = new Thread[files.size()];
//		for(int i=0; i<files.size(); i++){
//			String filePath = files.get(i).getAbsolutePath();
//			service[i] = new LoadTiffThread(filePath);
//			serverThread[i] = new Thread(service[i]);
//			serverThread[i].start();
//		}
//		
//		try {
//			for(int i=0; i<files.size(); i++){
//				serverThread[i].join();
//			}
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		for(int i=0; i<files.size(); i++){
//			parsers.add(service[i].getResult());
//		}
//		return parsers;
//	}
//	
//	public ArrayList<File> getAllFiles(String directoryName, ArrayList<File> files) {
//	    File directory = new File(directoryName);
//
//	    // get all the files from a directory
//	    File[] fList = directory.listFiles();
//	    for (File file : fList) {
//	    	String name = file.getName();
//	        if (file.isFile() && name.endsWith(".tif") && !name.contains("MPI-ESM-LR_CCLM") && !name.contains("HadGEM2-ES_CCLM") && !name.contains("EC-EARTH-r12_CCLM")
//					&& !name.contains("CNRM-CM5_CCLM") && !name.contains("EC-EARTH-r3_HIRHAM")) {
//	            files.add(file);
//	        } else if (file.isDirectory()) {
//	        	getAllFiles(file.getAbsolutePath(), files);
//	        }
//	    }
//	    return files;
//	}
//}
