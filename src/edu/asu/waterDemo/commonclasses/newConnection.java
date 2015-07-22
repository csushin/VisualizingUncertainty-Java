package edu.asu.waterDemo.commonclasses;

import java.net.UnknownHostException;

import javax.ws.rs.Path;

import com.mongodb.MongoClient;

@Path("/newConnection")
public class newConnection {
	public static MongoClient dbClient;
	static{
		try {
			System.out.println("excuted!");
			dbClient = new MongoClient("fsdb1.dtn.asu.edu", 27017);
//			dbClient = new MongoClient("vaderserver0.dhcp.asu.edu", 27017);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
