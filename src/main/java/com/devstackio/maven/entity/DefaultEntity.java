package com.devstackio.maven.entity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * when extending, define this.bucket in constructor
 * would suggest a Constants singleton and set the bucket on appinit after reading our bucket data
 * this way we can create an AppDefaultEntity that will set this.bucket to AppConstants.getMainBucket();
 * have AppDefaultEntity extend DefaultEntity and all your CB Entity objects extend AppDefaultEntity and call super() in constructor
 *
 * @author devstackio
 */
public class DefaultEntity {

    protected String id;
    protected String prefix;
    protected String timeCreated;
    protected transient String bucket;

    public DefaultEntity() {
        this.prefix = this.getClass().getSimpleName();
        this.timeCreated = this.getTime();
    }

    private String getTime() {
        String returnobj="";
        
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.hhmmss");
        returnobj = sdf.format(date);
        
        return returnobj;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return getClass().getSimpleName()
     */
    public String getPrefix() {
        return this.getClass().getSimpleName();
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * couchbase document id
     *
     * @return getPrefix()+":"+this.getId();
     */
    public String getDocId() {
        return this.getPrefix() + ":" + this.getId();
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

}
