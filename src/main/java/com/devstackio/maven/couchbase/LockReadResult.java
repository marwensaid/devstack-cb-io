package com.devstackio.maven.couchbase;

import com.devstackio.maven.entity.DefaultEntity;

public class LockReadResult {
    
    private DefaultEntity entity;
    private long cas;

    public DefaultEntity getEntity() {
        return entity;
    }

    public void setEntity(DefaultEntity entity) {
        this.entity = entity;
    }

    public long getCas() {
        return cas;
    }

    public void setCas(long cas) {
        this.cas = cas;
    }
    
}
