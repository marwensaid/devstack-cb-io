package com.devstackio.maven.entity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * when extending, define this.bucket in constructor
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
        String dateStr = "Jul 27, 2011 8:35:29 AM";
        DateFormat readFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss aa");
        DateFormat writeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = readFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (date != null) {
            returnobj = writeFormat.format(date);
        }
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
