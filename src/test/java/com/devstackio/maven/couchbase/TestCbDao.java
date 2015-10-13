package com.devstackio.maven.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.error.CASMismatchException;
import com.devstackio.maven.logging.IoLogger;
import com.devstackio.maven.uuid.UuidGenerator;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * tests to validate CbDao functionality pre-req : - couchbase bucket setup (
 * name / password ) bucket name "default" with no password
 *
 * @author devstackio
 */
public class TestCbDao {

    private static CbDao cbDao;
    private static Bucket bucket;
    private static ArrayList<String> testDocIds;
    private static boolean testCounterCreated = false;
    private final static String[] couchbaseIps = {"127.0.0.1"};
    private final static String demoBucketName = "default";
    private final static String demoBucketPass = "";
    private final static UuidGenerator uuidGenerator = new UuidGenerator();
    private final static IoLogger ioLogger = new IoLogger();
    private final static String docPrefix = "ContractEntity";
    private final static String testCounter = "test7488488";

    @BeforeClass
    public static void setUpClass() {
        try {
            testDocIds = new ArrayList();
            cbDao = new CbDao();
            cbDao.setIoLogger(ioLogger);
            cbDao.setUuidGenerator(uuidGenerator);
            cbDao.initializeStandalone(demoBucketName, demoBucketPass);

            cbDao.createConnection(couchbaseIps);
            cbDao.addBucketToCluster(demoBucketName, demoBucketPass);
            bucket = cbDao.getBucket(demoBucketName);
            
            //initialize test counter if does not exist
            cbDao.getCounter(bucket, docPrefix);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setUp() {
        //System.out.println("before");
    }

    @After
    public void tearDown() {
        //System.out.println("after");
    }

    @AfterClass
    public static void tearDownClass() {

        try {
            System.out.println("-- removing test documents created from couchbase --");

            for (int i = 0; i < testDocIds.size(); i++) {
                System.out.println("removing : " + testDocIds.get(i).toString());
                bucket.remove(testDocIds.get(i));
            }
            System.out.println("removing counter for : " + docPrefix);
            bucket.remove(docPrefix);
            
            if( testCounterCreated ) {
                System.out.println("removing test generated counter");
                bucket.remove(testCounter);
            }
            
            bucket.close();
            cbDao.destroyConnection(demoBucketName);
            cbDao.closeClusterConnection();

        } catch (Exception e) {
            System.out.println("exception caught in tearDownClass method....");
            e.printStackTrace();
        }
    }

    /**
     * adds a testing document id to remove from couchbase during tearDownClass
     *
     * @param docid
     */
    private void addTestDocId(String docid) {
        String docId = docid;
        try {
            if (!testDocIds.contains(docId)) {
                testDocIds.add(docid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * creates new ContractEntity document to couchbase
     * @param contract send 'default' for tests
     * @param tosession whether or not to save doc to session ( appends a UUID that would be stored browser cookie )
     * @param deleteAfter whether or not to delete this document after use ( for tests )
     * @return 
     */
    private String createMockContractEntity(String contract, boolean tosession, boolean deleteAfter) {

        ContractEntity entity = new ContractEntity();
        String returnobj = "";

        try {
            entity.setContract(contract);

            if (!tosession) {
                cbDao.create(entity);
            } else {
                cbDao.createToSession(entity);
            }

            returnobj = entity.getDocId();

            if(deleteAfter) {
                System.out.println("adding doc id to clean-up bag : " + returnobj);
                this.addTestDocId(returnobj);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnobj;
    }
    
    /**
     * test CbDao createDefaultCouchbaseView method - should create :
     *    design doc : entity's class name
     *    view name  : getAll
     */
    @Test
    public void testCreateDefaultCouchbaseView() {
        
        CbViews cbViews = new CbViews();
        ContractEntity entity = new ContractEntity();
        cbDao.createCouchbaseDefaultView( entity );
        
        String doc0 = this.createMockContractEntity("default", false, true);
        String doc1 = this.createMockContractEntity("default", false, true);
        String doc2 = this.createMockContractEntity("default", false, true);
        
        ArrayList<ContractEntity> results = cbDao.getAll( entity );
        int resultSize = results.size();
        
        System.out.println("result size from getAll method should be 3... was : " + resultSize );
        
        System.out.println("removing design document " + entity.getPrefix() + " from couchbase");
        cbViews.removeDesignDoc( bucket, entity.getPrefix() );
        
        assertEquals("defaultCouchbaseView successfully Created - getAll method success", resultSize, 3);
        
    }
    
    /**
     * make sure documents retrieved via readAndLock cant be updated during lockout time
     * make sure documents can be unlocked using JsonDocument's cas value
     */
    @Test
    public void testReadAndLock() {
        
        String docId="";
        ContractEntity entityResult = new ContractEntity();
        ContractEntity entityTry = new ContractEntity();
        String contract = "default";
        String contractUpdate = "newContract";
        int lockTime = 3;
        Bucket bucket = cbDao.getBucket( entityResult.getBucket() );
        
        try {
            
            docId = this.createMockContractEntity( contract, false, true );
            long casTestOne = 3;
            
            entityTry = cbDao.readAndLock( docId, entityTry, lockTime );
            entityTry.setContract( contractUpdate );
            cbDao.update(entityTry);
            ContractEntity readCheck = cbDao.read( docId, new ContractEntity() );
            assertEquals("should not allow any document writes immediately after a readAndLock", readCheck.getContract(), contract );
            System.out.println("waiting 5 seconds before trying to set again...");
            Thread.sleep(5000);
            cbDao.update(entityTry);
            ContractEntity anotherRead = cbDao.read( docId, new ContractEntity() );
            
            assertEquals("should allow document writes after wait time as expired", anotherRead.getContract(), contractUpdate );
            
            System.out.println("waiting 1 second before trying to update on same document with it's own CAS value...");
            Thread.sleep(1000);
            
            long realCas = bucket.getAndLock(docId, 10).cas();
            boolean testUnlock = bucket.unlock( docId, realCas);
            System.out.println("-- testUnlock is : " + testUnlock );
            if ( testUnlock ) {
                anotherRead.setContract("testUnlock!");
                cbDao.update( anotherRead );
            }
            
            String contractCheck = cbDao.read(docId, new ContractEntity() ).getContract();
            System.out.println("contractCheck final is : " + contractCheck) ;
            
            assertEquals("should be able to update document with it's own CAS value after running .unlock",contractCheck,"testUnlock!");
            
        } catch (CASMismatchException e) {
            System.out.println("CAS Mismatch caught...");
        } catch (Exception e) {
            System.out.println("exception type : " + e.getCause().toString() + " caught in Exception block");
            e.printStackTrace();
        }

    }

    /**
     * Test create and read methods of EntityCbDao (abstract super class) tests
     * abstract convert (overwritten) method of ContractCbDao.
     */
    @Test
    public void testCreateAndRead() {

        System.out.println("-- [TEST] testCreateAndRead() --");
        ContractEntity entityResult = new ContractEntity();
        String prefixCheck = entityResult.getPrefix();
        String docId = "";
        String contract = "default";

        try {
            docId = this.createMockContractEntity(contract, false, true);
            System.out.println("saved doc as : " + docId);

            entityResult = cbDao.read(docId, entityResult);

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals("contract '" + contract + "' saved into new couchbase document", contract, entityResult.getContract());
        assertEquals("prefix '" + prefixCheck + "' saved into new couchbase document", prefixCheck, entityResult.getPrefix());
    }

    /**
     * Test saving new document to session using user uuid stored in browser
     * tests abstract convert (overwritten) method of ContractCbDao.
     */
    @Test
    public void testCreateAndReadSession() {
        
        System.out.println("-- [TEST] testCreateAndReadSession() --");
        
        ContractEntity entityResult = new ContractEntity();
        String docId = "";
        String contract = "default";
        String prefixCheck = entityResult.getPrefix();

        try {
            docId = this.createMockContractEntity(contract, true, true);
            System.out.println("saved doc as : " + docId);

            entityResult = cbDao.read(docId, entityResult);

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals("contract '" + contract + "' saved into new couchbase document", contract, entityResult.getContract());
        assertEquals("prefix '" + prefixCheck + "' saved into new couchbase document", prefixCheck, entityResult.getPrefix());
    }
    
    @Test
    public void testCbCounter() {
        
        testCounterCreated = true;
        
        try {
            
            int c = cbDao.getCounter( bucket, testCounter );
            assertEquals( "counter should be created if does not exist and return 0", c, 0);
            
            int b = cbDao.getCounter( bucket, testCounter );
            assertEquals( "counter should increment by 1 in subsequent requests", b, 1);
            
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * reading a document that does not already exist in couchbase should
     * auto-generate the document to couchbase session
     */
    @Test
    public void testEmptyReadFromSession() {
        
        System.out.println("-- [TEST] testEmptyReadFromSession() --");

        ContractEntity entityResult = new ContractEntity();
        String docId = "";
        ContractEntity test = new ContractEntity();

        try {
            entityResult = cbDao.readFromSession(entityResult);
            docId = entityResult.getDocId();
            System.out.println("session object created with id : " + docId);

            test = cbDao.read(docId, entityResult);
            this.addTestDocId(docId);

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals("doc was created after not being found in session : '" + docId + "' saved into new couchbase document", docId, test.getDocId());
    }

    /**
     * Test update method of EntityCbDao (abstract super class)
     */
    @Test
    public void testUpdate() {
        
        System.out.println("-- [TEST] testUpdate() --");

        System.out.println("testing update method...");
        ContractEntity entity = new ContractEntity();
        String contract = "default";
        String contractUpdate = "updatedContract";
        String prefix = entity.getPrefix();
        String docId = "";

        String contractAfterUpdate = "";

        try {
            docId = this.createMockContractEntity(contract, false, true);
            entity = cbDao.read(docId, entity);

            entity.setContract(contractUpdate);
            cbDao.update(entity);
            entity = new ContractEntity();
            entity = cbDao.read(docId, entity);
            contractAfterUpdate = entity.getContract();

        } catch (Exception e) {
            e.printStackTrace();
        }

        this.addTestDocId(docId);
        
        System.out.println("update method finished");

        assertEquals("contract '" + contract + "' updated to : " + contractUpdate, contractUpdate, contractAfterUpdate);

    }

	//Test delete method of EntityCbDao (abstract super class)
    @Test
    public void testDelete() {
        
        System.out.println("-- [TEST] testDelete() --");

        ContractEntity entity = new ContractEntity();
        String contract = "default";
        String prefix = entity.getPrefix();
        String docId = "";

        try {
            docId = this.createMockContractEntity(contract, false, false);
            System.out.println("created docid : " + docId);
            entity = cbDao.read(docId, entity);
            cbDao.delete(docId, entity);

            entity = new ContractEntity();

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            entity = cbDao.read(docId, entity);

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals("added and removed docId '" + docId, null, entity);

    }
    
    @Test
    public void testDocExistMethod() {
        
        System.out.println("-- [TEST] testDocExistMethod() --");

        ContractEntity entity = new ContractEntity();
        String contract = "default";
        String docId = "";
        boolean test0 = false;
        boolean test1 = false;

        try {
            docId = this.createMockContractEntity(contract, false, false);
            System.out.println("created docid : " + docId);
            entity = cbDao.read(docId, entity);
            test0 = cbDao.docExists( docId, entity );
            cbDao.delete(docId, entity);
            test1 = cbDao.docExists( docId, entity );

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals("docExists correctly after adding to cb '" + docId, test0, true);
        assertEquals("docExists false after removing from cb and rechecking '" + docId, test1, false);

    }

}
