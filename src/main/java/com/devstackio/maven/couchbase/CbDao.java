package com.devstackio.maven.couchbase;

import com.devstackio.maven.databaseshared.IDao;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
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
import java.util.Iterator;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.log4j.Level;

/**
 * if entity Dao is going to couchbase : extend this has main methods needed to
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

    @Inject
    public void setAppData(AppData appdata) {
        this.appData = appdata;
    }

    @Inject
    public void setIoLogger(IoLogger iologger) {
        this.ioLogger = iologger;
    }

    @Inject
    public void setUuidGenerator(UuidGenerator uuidgenerator) {
        this.uuidGenerator = uuidgenerator;
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
            System.out.println("[CbDao initialized for standalone use]");
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

            String entid = entity.getId();
            if (entid == null || entid.isEmpty()) {
                String entityId = this.generateId(bucket, prefix);
                entity.setId(entityId);
            }

            RawJsonDocument rJsonDoc = this.convertToRawJsonDoc(entity.getDocId(), entity);
            returnobj = prefix + ":" + entity.getId();

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

            System.out.println("creating session entity! : " + entity.getId());
            
            JsonDocument found = bucket.get( entity.getDocId() );
            if (found == null) {
                // doc not found
                System.out.println("doc not found call...");
                returnobj = this.create(entity);
            } else {
                // doc found
                System.out.println("doc found - calling update -----");
                this.update(entity);
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

        } catch (NullPointerException e) {
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, "document : " + docId + " not found in couchbase.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnobj;
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
            docId = entity.getPrefix() + ":" + this.appData.getUuid();
            Bucket bucket = this.getBucket(entity.getBucket());
            String logMsg = "trying readFromSession using docid : " + docId + " bucket is : " + bucket;
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, logMsg);
            entity.setId(docId);
            JsonDocument jd = bucket.get(docId);

            entityJson = jd.content().toString();
            returnobj = (T) this.gson.fromJson(entityJson, t.getClass());

        } catch (NullPointerException e) {
            String generatedId = this.create((DefaultEntity) returnobj);
            returnobj = this.read(generatedId, returnobj);
        } catch (Exception e) {
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
            String logMsg = "-- tried replace on : " + rJsonDoc + " --";
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, logMsg);

        } catch (DocumentDoesNotExistException e) {
            this.ioLogger.logTo("DevStackIo-debug", Level.INFO, "document : " + docId + " not found in couchbase.");
        } catch (Exception e) {
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
     * increments entity prefix counter on couchbase
     *
     * @return returns id related to String (entity)prefix passed in(ex: "22")
     */
    protected String generateId(Bucket bucket, String prefix) {

        String returnobj = "";
        Bucket cbBucket = bucket;

        try {
            JsonLongDocument jsonLongDoc = cbBucket.counter(prefix, 1);
            Long idLong = jsonLongDoc.content();
            returnobj = Long.toString(idLong);

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

}
