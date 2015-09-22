package com.devstackio.maven.databaseshared;

import com.devstackio.maven.entity.DefaultEntity;

/**
 *
 * @author devstackio
 */
public interface IDao {
	/**
	 * database CRUD
	 * @param entityobj
	 * @return id of entity after insertion
	 */
	public String create( DefaultEntity entityobj );
	public <T> T read( String id, T t );
	public void update( DefaultEntity entityobj );
	public void delete( String docid, DefaultEntity entityobj );
	
}
