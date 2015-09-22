package com.devstackio.maven.application.config;

import com.devstackio.maven.propertyloader.PropertyLoader;
import java.util.ArrayList;
import java.util.Properties;

/**
 * application data for core java testing
 * @author devstackio
 */
public class CoreAppData extends AbstractAppData {
	
	private Properties config;
	private String appName;
	private ArrayList<String> couchbaseIps;
	private String mainCbBucket;
	private String mainCbPass;
	
	public CoreAppData() {
		try {
			PropertyLoader propertyLoader = new PropertyLoader();
			this.config = propertyLoader.loadProperties("config.properties");
			this.appName = this.config.getProperty( "appName" );
			this.couchbaseIps = new ArrayList();
			this.couchbaseIps.add( this.config.getProperty( "couchbaseIps" ) );
			this.mainCbBucket = this.config.getProperty( "dbn" );
			this.mainCbPass = this.config.getProperty( "dbp" );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getAppName() {
		return this.appName;
	}
	@Override
	public ArrayList<String> getCouchbaseIps() {
		return this.couchbaseIps;
	}
	@Override
	public String getMainCbBucket() {
		return this.mainCbBucket;
	}
	@Override
	public String getMainCbPass() {
		return this.mainCbPass;
	}
	
}
