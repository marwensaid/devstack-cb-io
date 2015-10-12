package com.devstackio.maven.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;

/**
 *
 * @author devstackio
 */
@RequestScoped
public class CbViews {

    public void addDesignDoc(Bucket bucket, String designDocName, HashMap<String, String> viewToMapFunctions) {
        try {

            System.out.println("  -- [ creating designDoc view : [" + designDocName + "]..." );
            List<View> viewList = new ArrayList();
            HashMap<String, String> views = viewToMapFunctions;
            Iterator it = views.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
//                System.out.println("------------------------------");
//                System.out.println(pairs.getKey().toString());
//                System.out.println(pairs.getValue().toString());
                View vd = DefaultView.create(pairs.getKey().toString(), pairs.getValue().toString());
                viewList.add(vd);
                it.remove();
            }

            DesignDocument designDoc = DesignDocument.create(designDocName, viewList);

            bucket.bucketManager().upsertDesignDocument(designDoc);
            
            System.out.println("     -- designDoc created successfully ]");

        } catch (Exception e) {
            System.err.println("[ CBViews ] Error : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void removeDesignDoc( Bucket bucket, String designDoc ) {
        
        try {
            
            bucket.bucketManager().removeDesignDocument(designDoc);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
