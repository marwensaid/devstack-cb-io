package com.devstackio.maven.application.config;

import com.devstackio.maven.loaders.PropertyLoader;
import java.util.ArrayList;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * application data for web will try to read from META-INF/config.properties
 * should have by default following defined : appName=yourApplicationName
 * couchbaseIps=couchbaseIps [ comma-delimited ] dbn=mainBucketName
 * dbp=mainBucketPass 
 *
 * @author devstackio
 */
@Named
@ApplicationScoped
public class AppData extends AbstractAppData {

    private final String PARAM_CONFIG_FILENAME = "config.properties";
    private final String PARAM_COUCHBASE_IPS = "couchbaseIps";
    private final String PARAM_APP_NAME = "appName";
    private final String PARAM_MAIN_BUCKET_NAME = "dbn";
    private final String PARAM_MAIN_BUCKET_PASS = "dbp";
    private PropertyLoader propertyLoader;
    private String appName;
    private ArrayList<String> couchbaseIps;
    private String mainCbBucket;
    private String mainCbPass;

    @PostConstruct
    public void init() {
        try {
            System.out.println("appData init call");
            this.propertyLoader = new PropertyLoader();
            Properties config = this.propertyLoader.loadFromThread(Thread.currentThread(), PARAM_CONFIG_FILENAME);
            this.setParams(config);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * for running outside of web context
     */
    public void initWithConfig(Properties config) {
        this.setParams(config);
    }

    private void setParams(Properties config) {
        try {
            this.appName = config.getProperty(PARAM_APP_NAME);
            this.couchbaseIps = this.parseCouchbaseIps(config);
            this.setMainCbBucket(config.getProperty(PARAM_MAIN_BUCKET_NAME));
            this.setMainCbPass(config.getProperty(PARAM_MAIN_BUCKET_PASS));

            System.out.println("appData loaded config with : ");
            System.out.println("appName : " + this.appName);
            System.out.println("couchbaseIps : " + this.couchbaseIps);
            System.out.println("mainCbBucket : " + this.getMainCbBucket());
            System.out.println("mainCbPass : " + this.getMainCbPass());

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Inject
    public void setPropertyLoader(PropertyLoader propertyloader) {
        this.propertyLoader = propertyloader;
    }

    @Override
    public String getAppName() {
        return this.appName;
    }

    @Override
    public String getMainCbBucket() {
        return this.mainCbBucket;
    }

    @Override
    public String getMainCbPass() {
        return this.mainCbPass;
    }

    @Override
    public ArrayList<String> getCouchbaseIps() {
        return this.couchbaseIps;
    }

    public void setMainCbBucket(String mainCbBucket) {
        this.mainCbBucket = mainCbBucket;
    }

    public void setMainCbPass(String mainCbPass) {
        this.mainCbPass = mainCbPass;
    }

}
