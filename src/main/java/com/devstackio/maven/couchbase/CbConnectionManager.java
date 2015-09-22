package com.devstackio.maven.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.devstackio.maven.logging.IoLogger;
import java.util.ArrayList;
import java.util.HashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * couchbase connection manager
 *
 * @author devstackio
 */
public class CbConnectionManager {
	
	private static Cluster cluster;
	private HashMap<String,Bucket> buckets;
	private IoLogger ioLogger;

	@Inject
	public void setIoLogger(IoLogger iologger) {
		this.ioLogger = iologger;
	}
	public CbConnectionManager() {
		this.buckets = new HashMap();
	}
	/**
	 * use false if needed outside web application ( running core Java files )
	 * @param webBased
	 */
	public CbConnectionManager( boolean webBased ) {
		this();
		if( !webBased ) {
			this.ioLogger = new IoLogger();
		}
	}
	
	/**
	 * creates cluster connection
	 * should be called from contextInitialized [ ServletContextListener ]
	 * @param ips ip of server ex: {"127.0.0.1", "devstackio.com"}
	 * @param bucketname
	 * @param bucketpass
	 */
	public void createConnection(String[] ips) {
		ArrayList<String> ipList = new ArrayList();
		for (int i = 0; i < ips.length; i++) {
			String string = ips[i];
			ipList.add(string);
		}
		this.initCluster( ipList );
	}
	/**
	 * creates cluster connection
	 * should be called from contextInitialized [ ServletContextListener ]
	 * @param ips ip[] of server ex: ["127.0.0.1","devstackio.com"]
	 * @param bucketname
	 * @param bucketpass 
	 */
	public void createConnection(ArrayList<String> ips) {
		this.initCluster( ips );
	}
	private void initCluster(ArrayList<String> ips) {

		try {
			cluster = CouchbaseCluster.create( ips );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void addBucketToCluster( String bucketname, String bucketpass ) {
		String bName = bucketname;
		String bPass = bucketpass;
		try {
			Bucket bucket = cluster.openBucket( bName, bPass);
			this.getBuckets().put( bName, bucket);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void closeClusterConnection() {
		this.cluster.disconnect();
	}
	public Bucket getBucket( String bucketname ) {
		
		Bucket returnobj = null;
		
		try {
			returnobj = this.getBuckets().get( bucketname );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return returnobj;
	}
	/**
	 * should be called from contextDestroyed [ ServletContextListener ]
	 */
	public void destroyConnection(String bucketname) {
		try {
			//this.ioLogger.logTo(this.LOGFILE, Level.INFO, "destroyingConnection to : " + bucketname );
			this.getBuckets().get(bucketname).close();
		} catch (Exception e) {
			//this.ioLogger.logTo(this.LOGFILE, Level.ERROR, "destroyingConnection to : " + bucketname + " : error : " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public HashMap<String,Bucket> getBuckets() {
		return buckets;
	}
	
}

