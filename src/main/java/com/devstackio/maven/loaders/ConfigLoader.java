package com.devstackio.maven.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public enum ConfigLoader {
    
    INSTANCE;
    
    private final String PARAM_COUCHBASE_IPS = "couchbaseIps";
    private final String PARAM_APP_NAME = "appName";
    private final String PARAM_MAIN_BUCKET_NAME = "dbn";
    private final String PARAM_MAIN_BUCKET_PASS = "dbp";
    
    /**
     * get param from properties obj : properties.getProperty( "propertyName" )
     * properties file :
     * propertyName=hello
     * propertyNameTwo=nosemicol
     * @param file ex: "config.properties"
     * @param path ex: "" for scr/main/resources
     * @return 
     */
    public Properties loadProperties( String file, String path ) {
        Properties returnobj = new Properties();
        String fullPath = path + file;

        InputStream in = new InputStream() {

            @Override
            public int read() throws IOException {
                return 0;
            }
        };
        try {
            in = ClassLoader.getSystemResourceAsStream(fullPath);
            System.out.println("--trying to load : " + fullPath);
            returnobj.load(in);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return returnobj;
    }
    
    /**
     * will load file specified and return CbInfo object
     * @param file string of file name ( usually "config.properties" )
     * @param path string of file path ( usually left blank "" and will look in src/main/resources )
     * @return 
     */
    public CbInfo loadConfig( String file, String path ) {
        return this.setParams( this.loadProperties(file, path) );
    }
    
    private CbInfo setParams(Properties config) {
        
        CbInfo returnobj = CbInfo.INSTANCE;
        Properties properties = config;
        
        try {
            
            returnobj.setAppName( properties.getProperty( PARAM_APP_NAME ));
            returnobj.setCouchbaseIps( this.parseCouchbaseIps(properties) );
            returnobj.setMainCbBucket( properties.getProperty(PARAM_MAIN_BUCKET_NAME) );
            returnobj.setMainCbPass( properties.getProperty(PARAM_MAIN_BUCKET_PASS) );

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return returnobj;
    }
    
    private ArrayList<String> parseCouchbaseIps(Properties props) {
        ArrayList<String> returnobj = new ArrayList();
        try {
            String cbIps = props.getProperty(PARAM_COUCHBASE_IPS);
            String[] cbIpsArr = cbIps.split(",");
            if (cbIpsArr.length > 0) {
                for (int i = 0; i < cbIpsArr.length; i++) {
                    String ip = cbIpsArr[i];
                    returnobj.add(ip);
                }
            } else {
                returnobj.add(cbIps);
            }

        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace();
        }
        return returnobj;
    }
    
}
