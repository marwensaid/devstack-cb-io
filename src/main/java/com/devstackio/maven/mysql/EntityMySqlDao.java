package com.devstackio.maven.mysql;

import com.devstackio.maven.databaseshared.IDao;
import com.devstackio.maven.application.config.AbstractAppData;
import com.google.gson.Gson;
import com.devstackio.maven.logging.IoLogger;
import com.devstackio.maven.uuid.UuidGenerator;
import javax.enterprise.context.RequestScoped;

/**
 * if entity Dao is going to MySQL : extend this
 * under construction o_O
 * @author devstackio
 */
@RequestScoped
public abstract class EntityMySqlDao implements IDao {
	
	protected String tableName;
	protected Gson gson;
	protected UuidGenerator uuidGenerator;
	//protected CbDao cbDao;
	protected IoLogger ioLogger;
	protected AbstractAppData appData;
	
}
