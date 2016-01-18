package com.devstackio.maven.couchbase;

import com.couchbase.client.java.AsyncBucket;
import com.devstackio.maven.databaseshared.IDao;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.view.Stale;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import com.devstackio.maven.application.config.AppData;
import com.google.gson.Gson;
import com.devstackio.maven.entity.DefaultEntity;
import com.devstackio.maven.logging.IoLogger;
import com.devstackio.maven.uuid.UuidGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.log4j.Level;

/**
 * if entity Dao is going to couchbase : extend this : has main methods needed to
 * store and read documents into couchbase using java client 2.1.3 for couchbase
 * 3 make sure to add @Named and @ApplicationScoped on sub classes
 *
 * @author devstackio
 */
@Demo
@ApplicationScoped
public class CbDao extends CbConnectionManager implements IDao {

    protected String bucketName;
    protected Gson gson = new Gson();
    protected UuidGenerator uuidGenerator;
    protected IoLogger ioLogger;
    protected AppData appData;
    protected CbViews cbViews = new CbViews();

    @Inject
    public void setAppData(AppData appdata) {
        this.appData = appdata;
    }

    @Inject
    public void setIoLogger(IoLogger iologger) {
        this.ioLogger = iologger;
    }
    
    public IoLogger getIoLogger() {
        return this.ioLogger;
    }

    @Inject
    public void setUuidGenerator(UuidGenerator uuidgenerator) {
        this.uuidGenerator = uuidgenerator;
    }
    
    public void setBucketName( String bucketname ) {
        this.bucketName = bucketname;
    }

    /**
     * if needed outside web context
     */
    public void initializeStandalone(String bucket, String pass) {
        try {
            this.ioLogger = new IoLogger();
            this.uuidGenerator = new UuidGenerator();
            this.setIoLogger(ioLogger);
            this.setUuidGenerator(uuidGenerator);
            AppData appData = new AppData();
            appData.setMainCbBucket(bucket);
            appData.setMainCbPass(pass);
            this.setAppData(appData);
            this.setBucketName( bucket );
            System.out.println("[CbDao initialized for standalone use]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void initializeStandaloneWithCluster( String[] ips, String bucketname, String bucketpass ) {
        try {
            this.initializeStandalone( bucketname, bucketpass );
            this.createConnectionWithBucket( ips, bucketname, bucketpass );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void initializeStandaloneWithCluster( ArrayList<String> ips, String bucketname, String bucketpass ) {
        try {
            ArrayList<String> origIps = ips;
            String[] ipList = new String[ origIps.size() ];
            int counter = 0;
            for ( String str : origIps ) {
                ipList[ counter ] = str;
                counter++;
            }
            this.initializeStandalone( bucketname, bucketpass );
            this.createConnectionWithBucket( ipList, bucketname, bucketpass );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * creates entity in database
     *
     * @param obj
     * @return id of entity after insertion catches
     * DocumentAlreadyExistsException already in couchbase - logs to
     * "DocAlreadyExists.log" persists to Master node
     */
    @Override
    public String create(DefaultEntity entityobj) {

        String returnobj = "";
        String prefix = "";
        DefaultEntity entity = entityobj;
        Bucket bucket = this.getBucket(entity.getBucket());

        try {
            prefix = entity.getPrefix();
            System.out.println( "cbdao - create -- prefix is : " + prefix + " entityBucket is : " + entity.getBucket() );
            System.out.println("bucket is : " + bucket );

            String entid = entity.getId();
            System.out.println( "entid is : " + entid );
            
            if (entid == null || entid.isEmpty()) {
                System.out.println("entid is null or empty... first chek");
                String entityId = this.generateId(bucket, prefix);
                System.out.println( "entid was null or empty ... new generated id is : " + entityId );
                entity.setId(entityId);
            }
            
            System.out.println("getDocId is : " + entity.getDocId() );
            
            RawJsonDocument rJsonDoc = this.convertToRawJsonDoc(entity.getDocId(), entity);
            System.out.println( "rawJason doc is : " + rJsonDoc );
            returnobj = prefix + ":" + entity.getId();
            System.out.println( "returnobj is : " + returnobj );
            
            System.out.println( "bucket is : " + bucket );
            bucket.insert(rJsonDoc, PersistTo.MASTER);
            String logMsg = "-- tried upsert on : " + rJsonDoc + " --";
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, logMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnobj;
    }
    
    /**
     * updates counter for passed in entity, returns current (unused) value
     * @param entity
     * @return current counter of entity
     */
    public String updateCounter( DefaultEntity entity ) {
        
        String returnobj = "";
        
        try {
            
            String counterName = entity.getPrefix();
            Bucket bucket = this.getBucket(this.bucketName);
            returnobj = Integer.toString(this.getCounter( bucket, counterName ));

            int newId = Integer.parseInt( returnobj ) + 1;
            this.setCounter( bucket, counterName, newId );
            
            System.out.println("- updating counterName : " + counterName + " new value is : " + newId );
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return returnobj;
        
    }
    
    /**
     * uses AsyncBucket instead of regular bucket
     * @param entityobj
     * @return 
     */
    public String createAsync(DefaultEntity entityobj) {

        String returnobj = "";
        String prefix = "";
        DefaultEntity entity = entityobj;
        AsyncBucket bucket = this.getBucket(entity.getBucket()).async();

        try {
            prefix = entity.getPrefix();
            System.out.println( "cbdao - create -- prefix is : " + prefix );
            System.out.println("bucket is : " + bucket );

            String entid = entity.getId();
            System.out.println( "entid is : " + entid );
            
            if (entid == null || entid.isEmpty()) {
                System.out.println("entid is null or empty... first chek");
                String entityId = this.generateId( this.getBucket(entity.getBucket()), prefix);
                System.out.println( "entid was null or empty ... new generated id is : " + entityId );
                entity.setId(entityId);
            }
            
            System.out.println("getDocId is : " + entity.getDocId() );
            
            RawJsonDocument rJsonDoc = this.convertToRawJsonDoc(entity.getDocId(), entity);
            System.out.println( "rawJason doc is : " + rJsonDoc );
            returnobj = prefix + ":" + entity.getId();
            System.out.println( "returnobj is : " + returnobj );
            
            System.out.println( "bucket is : " + bucket );
            bucket.insert(rJsonDoc, PersistTo.MASTER);
            String logMsg = "-- tried upsert on : " + rJsonDoc + " --";
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, logMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnobj;
    }

    /**
     * stores this entity object to couchbase using user's uuid instead of an
     * incremented id uuid will be stored as browser cookie
     * if document already exists, will update doc
     *
     * @param entityobj
     * @return
     */
    public String createToSession(Object entityobj) {

        String returnobj = "";
        String uuid = "";

        uuid = this.uuidGenerator.getUuid(this.getAppData().getAppName());
        DefaultEntity entity = (DefaultEntity) entityobj;
        entity.setId(uuid);
        Bucket bucket = this.getBucket(entity.getBucket());

        try {

            System.out.println("[ CbDao ] : createToSession : " + entity.getId() + " : from " + this.getClass().getCanonicalName());
            
            JsonDocument found = bucket.get( entity.getDocId() );
            if (found == null) {
                // doc not found
                System.out.println("  -- document does not exist, creating new entity --");
                returnobj = this.create(entity);
            } else {
                // doc found
                System.out.println("  -- document exists already, updating entity --");
                this.updateToSession(entity);
                returnobj = entity.getId();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnobj;

    }

    /**
     * used to query simple gets from couchbase
     *
     * @param <T>
     * @param id full id of document (ex: doc.getPrefix() + ":" + doc.getId() )
     * @param classtype
     * @return object as entityobj (ex: ContractEntity) *requires protected
     * Object convert( JsonDocument jsonDoc ) to be implemented in subclass
     */
    @Override
    public <T> T read(String id, T t) {

        T returnobj = null;
        DefaultEntity entity = (DefaultEntity) t;
        Bucket bucket = this.getBucket(entity.getBucket());
        String docId = id;
        String entityJson = "";
        try {
            JsonDocument jd = bucket.get(docId);
            entityJson = jd.content().toString();
            returnobj = (T) this.gson.fromJson(entityJson, t.getClass());
            
            System.out.println( "returning obj with json : " + entityJson );
            
        } catch (NullPointerException e) {
            
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, "document : " + docId + " not found in couchbase.");
            System.out.println("[NullPointer] caught in CbDao for docId [" + docId + "] -- entityBucket was [" + entity.getBucket() + "]" );
            return null;
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnobj;
    }
    
    /**
     * read a document id direct from a bucket using a Class to cast to
     * @param docid
     * @param cbbucket
     * @return entityobj parsed from json string
     */
    public <T> T rawRead( String docid, Bucket cbbucket, T t ) {
        
        T returnobj = null;
        String docId = docid;
        Bucket bucket = cbbucket;
        String entityJson = "";
        
        try {
            
            JsonDocument jd = bucket.get( docId );
            entityJson = jd.content().toString();
            returnobj = (T) this.gson.fromJson( entityJson, t.getClass() );
            
            System.out.println("raw returning object with json : " + entityJson );
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return returnobj;
        
    }
    
    public <T> T convertFromJsonDocument( JsonDocument jd, T t ) {
        
        T returnobj = null;
        try {
            
            String entityJson = jd.content().toString();
            returnobj = (T) this.gson.fromJson( entityJson, t.getClass() );
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnobj;
    }
    
    public <T> T readAndLock( String id, T t, int locktime ) {
        
        T returnobj = null;
        int lockTime = locktime;
        DefaultEntity entity = (DefaultEntity) t;
        Bucket bucket = this.getBucket(entity.getBucket());
        
        String docId = id;
        String entityJson = "";
        try {
            JsonDocument jd = bucket.getAndLock( id, lockTime );
            entityJson = jd.content().toString();
            returnobj = (T) this.gson.fromJson( entityJson, t.getClass() );

        } catch (NullPointerException e) {
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, "document : " + docId + " not found in couchbase.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnobj;
        
    }
    
    public <T>ArrayList getAll( T t ) {
        
        ArrayList<T> returnobj = new ArrayList();
        DefaultEntity entity = (DefaultEntity) t;
        Bucket bucket = this.getBucket( entity.getBucket() );
        
        try {
            
            ArrayList<JsonDocument> jsonDocs = this.getBulkData( bucket, entity.getPrefix(), "getAll" );
            
            for (int i = 0; i < jsonDocs.size(); i++) {
                JsonDocument jd = jsonDocs.get(i);
                T ent = this.read(jd.id(), t);
                DefaultEntity de = (DefaultEntity) ent;
                String entId = de.getId();
                if( !entId.contains("-") ) {
                    returnobj.add( ent );
                }
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return returnobj;
        
    }
    
    /**
     * creates a default couchbase view for returning all entity objects from db
     * design doc mapped to entity.prefix ( class name )
     * view name mapped to 'getAll'
     * @param <T>
     * @param t DefaultEntity to create view for
     */
    public <T>void createCouchbaseDefaultView( T t ) {
        
        DefaultEntity entity = (DefaultEntity) t;
        String prefix = entity.getPrefix();
        
        try {
            
            String entityView = "function (doc, meta) {\n"
                    + "  if(doc.prefix == '" + prefix + "') {\n"
                    + "    emit(meta.id);\n"
                    + "  }\n"
                    + "}";
            
            HashMap<String, String> mapActionEvent = new HashMap();
            mapActionEvent.put( "getAll", entityView );
            
            Bucket bucket = this.getBucket( entity.getBucket() );

            this.cbViews.addDesignDoc( bucket, prefix, mapActionEvent );
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    /**
     * if document does not exist in couchbase will create it
     *
     * @param entityobj
     * @return
     */
    public <T> T readFromSession(T t) {

        T returnobj = t;

        String docId = "";
        String entityJson = "";
        try {
            DefaultEntity entity = (DefaultEntity) t;
            String uuid = this.appData.getUuid();
            docId = entity.getPrefix() + ":" + this.appData.getUuid();
            Bucket bucket = this.getBucket(entity.getBucket());
            String logMsg = "trying readFromSession using docid : " + docId + " bucket is : " + bucket;
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, logMsg);
            entity.setId( uuid );
            JsonDocument jd = bucket.get(docId);

            entityJson = jd.content().toString();
            returnobj = (T) this.gson.fromJson(entityJson, t.getClass());

        } catch (DocumentAlreadyExistsException e ) {
            
            System.out.println("DocumentAlreadyExists caught on cbDao.readFromSession -- ");
            
        } catch (NullPointerException e) {
            String generatedId = this.create((DefaultEntity) returnobj);
            System.out.println("generatedId is : " + generatedId );
            returnobj = this.read(generatedId, returnobj);
        } catch (Exception e) {
            System.out.println("Exception being thrown from CbDao");
            e.printStackTrace();
        }
        return returnobj;
    }

    /**
     * updates entityObject to couchbase catches DocumentDoesNotExistException
     * if not found - logs to "DocDoesNotExist.log" persists to Master node
     *
     * @param entityobj
     */
    @Override
    public void update(DefaultEntity entityobj) {

        DefaultEntity entity = entityobj;
        Bucket bucket = this.getBucket(entity.getBucket());
        String docId = "";
        try {
            docId = entity.getPrefix() + ":" + entity.getId();

            RawJsonDocument rJsonDoc = this.convertToRawJsonDoc(entity.getDocId(), entity);
            
            bucket.replace(rJsonDoc, PersistTo.MASTER);
//            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, logMsg);

        } catch (CASMismatchException e) {
            System.out.println("------------ CbDao update method ---------");
            System.out.println("CASMismatch caught on updating : " + docId );
            System.out.println("------------------------------------------");
        } catch (DocumentDoesNotExistException e) {
            System.out.println( "[NullPointer] caught in CbDao update method on docId : " + docId );
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, "document : " + docId + " not found in couchbase.");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    public void updateFromLock( DefaultEntity entityobj, long cas ) {
        
        DefaultEntity entity = entityobj;
        Bucket bucket = this.getBucket(entity.getBucket());
        String docId = "";
        long casKey = cas;
        
        try {
            docId = entity.getPrefix() + ":" + entity.getId();

            RawJsonDocument rJsonDoc = this.convertToRawJsonDoc(entity.getDocId(), entity);
            
            bucket.unlock(docId, casKey);
            bucket.replace(rJsonDoc, PersistTo.MASTER);
            
        } catch(Exception e) {
            e.printStackTrace();
        }
        
    }

    public void updateToSession(DefaultEntity entityobj) {

        DefaultEntity entity = entityobj;
        Bucket bucket = this.getBucket(entity.getBucket());
        String docId = "";
        try {
            docId = entity.getPrefix() + ":" + this.getAppData().getUuid();
            String logMsg = "update to session call : docid is : " + docId;
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, logMsg);

            RawJsonDocument rJsonDoc = this.convertToRawJsonDoc(entity.getDocId(), entity);

            bucket.replace(rJsonDoc, PersistTo.MASTER);

        } catch (DocumentDoesNotExistException e) {
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, "document : " + docId + " not found in couchbase.");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * persists to Master node
     *
     * @param docid
     */
    @Override
    public void delete(String docid, DefaultEntity entityobj) {

        Bucket bucket = this.getBucket(entityobj.getBucket());
        try {
            bucket.remove(docid, PersistTo.MASTER);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    public void rawDelete( String docid, Bucket bucket ) {
        
        try {
            bucket.remove( docid, PersistTo.MASTER );
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    /**
     * creates full JsonDocument required for inserting to couchbase
     *
     * @param docid should be entity.prefix() + ":" + entity.getId();
     * @param jsonobj created by
     * {@link #convertToRawJsonDoc(Object) convertToRawJsonDoc};
     * @return
     */
    protected JsonDocument JsonObjectToJsonDocument(String docid, JsonObject jsonobj) {

        JsonDocument returnobj = null;
        try {
            returnobj = JsonDocument.create(docid, jsonobj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnobj;

    }
    
    protected long getCasFromEntity( String docid, Object entityobj ) {
        RawJsonDocument rJson = this.convertToRawJsonDoc(docid, entityobj);
        return rJson.cas();
    }

    /**
     * uses gson and JsonTranscoder to create a JsonDocument for use with
     * {@link #JsonObjectToJsonDocument(String, JsonObject) JsonObjectToJsonDocument}
     *
     * @param entityobj
     * @return
     */
    protected RawJsonDocument convertToRawJsonDoc(String docid, Object entityobj) {

        RawJsonDocument returnobj = null;
        Gson gson = new Gson();
        String docId = docid;
        String jsonData = "";

        try {
            jsonData = gson.toJson(entityobj);
            returnobj = RawJsonDocument.create(docId, jsonData);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnobj;

    }
    
    /**
     * used in place of bucket.counter method - the bucket.counter method does not create a new document
     * if none exists - bug
     * @todo this should be thread safe, for now just writing it as is
     * @param bucket
     * @param prefix
     * @return 
     */
    public int getCounter( Bucket bucket, String prefix ) {
        
        int returnobj = -1;
        Bucket cbBucket = bucket;
        
        try {
            
            System.out.println( "[[ CbDao getCounter -- trying get on prefix : " + prefix );
            JsonDocument jsonDoc = cbBucket.get( prefix );
            
            returnobj = jsonDoc.content().getInt("value");
//            int current = jsonDoc.content().getInt("value");
//            current += 1;
//            JsonObject content = JsonObject.empty().put("value", current);
//            JsonDocument doc = JsonDocument.create(prefix, content);
//            JsonDocument inserted = bucket.replace(doc);
//            returnobj = current;
            
            System.out.println("     -- returning : " + returnobj + " ]] " );
            
        } catch ( NullPointerException e ) {
            
            System.out.println("-- no counter for prefix : " + prefix + " found... setting and returning 0.");
            System.out.println("---- bucket is : " + bucket );
            
            JsonObject content = JsonObject.empty().put("value", 0);
            JsonDocument doc = JsonDocument.create(prefix, content);
            JsonDocument inserted = bucket.insert(doc);
            returnobj = 0;
            
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
        return returnobj;
        
    }
    
    public void setCounter( Bucket bucket, String prefix, int value ) {
        
        Bucket cbBucket = bucket;
        int newCounterValue = value;
        
        try {
            
            System.out.println( "--[ updating counter prefix : " + prefix + " to : " + value + " ]--" );
            JsonDocument jsonDoc = cbBucket.get( prefix );
            System.out.println("jsonDoc is : " + jsonDoc );
            
            JsonObject content = JsonObject.empty().put( "value", newCounterValue );
            JsonDocument doc = JsonDocument.create( prefix, content );
            JsonDocument inserted = bucket.replace(doc);
            
        } catch ( Exception e) {
            e.printStackTrace();
        }
        
    }

    /**
     * increments entity prefix counter on couchbase
     *
     * @return returns id related to String (entity)prefix passed in(ex: "22")
     */
    protected String generateId(Bucket bucket, String prefix) {

        String returnobj = "";

        try {
            
            returnobj = Integer.toString( this.getCounter(bucket, prefix) );
            
        } catch (DocumentDoesNotExistException e) {
            System.out.println("DocDoesNotExist caught... creating new prefix document for counter");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnobj;

    }

    /**
     * @return entity.prefix + ":" + uuid
     */
    public String getEntitySessionId() {

        String returnobj = "";
        try {
            String className = this.getClass().getName();
            int index = className.indexOf("Entity");
            returnobj = className.substring(0, index + 6);
            returnobj += ":" + this.uuidGenerator.getUuid(this.appData.getAppName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnobj;
    }

    public AppData getAppData() {
        return this.appData;
    }

    /**
     * used to return a Map<String,Object> of the List of Document IDs returned
     * from a View that uses emit(meta.id);
     *
     * @param client
     * @param designDoc
     * @param viewName - must use emit(meta.id);
     * @return
     */
    public ArrayList<JsonDocument> getBulkData(Bucket bucket, String designDoc, String viewName) {

        ArrayList<JsonDocument> returnobj = new ArrayList();

        try {
            ViewQuery viewQuery = ViewQuery.from(designDoc, viewName);
            viewQuery.stale(Stale.FALSE);
            ViewResult viewResult = bucket.query(viewQuery);
            Iterator<ViewRow> viewResults = viewResult.rows();

            while (viewResults.hasNext()) {
                ViewRow row = viewResults.next();
                returnobj.add(row.document());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnobj;
    }
    
    public boolean docExists( String docid, DefaultEntity entityobj ) {
        
        boolean returnobj = false;
        
        try {
            
            DefaultEntity entity = entityobj;
            Bucket bucket = this.getBucket(entity.getBucket());
            JsonDocument jd = bucket.get( docid );
            returnobj = jd != null;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return returnobj;
        
    }

}
