package com.devstackio.maven.loaders;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConfigLoaderTest {
    
    private final ConfigLoader configLoader = ConfigLoader.INSTANCE;

    /**
     * Test of values method, of class ConfigLoader.
     */
    @Test
    public void testLoadConfig() {
        
        CbInfo cbInfo = this.configLoader.loadConfig( "config.properties", "");
        cbInfo.print();
        
        assertEquals( "appName loaded successfully", "devstackio.app", cbInfo.getAppName() );
        assertEquals( "couchbaseIps loaded successfully", "[127.0.0.1]", cbInfo.getCouchbaseIps().toString() );
        assertEquals( "mainCbBucket loaded successfully", "devstack", cbInfo.getMainCbBucket() );
        assertEquals( "mainCbPass loaded successfully", "io", cbInfo.getMainCbPass() );
        
    }

    
}
