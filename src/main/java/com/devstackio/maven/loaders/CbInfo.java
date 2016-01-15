package com.devstackio.maven.loaders;

import java.util.ArrayList;

public enum CbInfo {
    
    INSTANCE;
    
    private String appName;
    private ArrayList<String> couchbaseIps;
    private String mainCbBucket;
    private String mainCbPass;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public ArrayList<String> getCouchbaseIps() {
        return couchbaseIps;
    }

    public void setCouchbaseIps(ArrayList<String> couchbaseIps) {
        this.couchbaseIps = couchbaseIps;
    }

    public String getMainCbBucket() {
        return mainCbBucket;
    }

    public void setMainCbBucket(String mainCbBucket) {
        this.mainCbBucket = mainCbBucket;
    }

    public String getMainCbPass() {
        return mainCbPass;
    }

    public void setMainCbPass(String mainCbPass) {
        this.mainCbPass = mainCbPass;
    }
    
    public void print() {
        System.out.println( "[[ CbInfo : print --- ");
        System.out.println( "  appName : " + this.appName );
        System.out.println( "  couchbaseIps : " + this.couchbaseIps );
        System.out.println( "  mainCbBucket : " + this.mainCbBucket );
        System.out.println( "  mainCbPass : " + this.mainCbPass );
    }
    
}
