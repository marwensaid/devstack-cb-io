package com.devstackio.maven.application.config;

import com.devstackio.maven.projectstage.ProjectStageUtil;
import com.devstackio.maven.uuid.UuidGenerator;
import java.util.ArrayList;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 *
 * @author devstackio
 */
@ApplicationScoped
public abstract class AbstractAppData {
	
	private ProjectStageUtil projectStageUtil;
	private UuidGenerator uuidGenerator;
	public abstract String getAppName();
	public abstract ArrayList<String> getCouchbaseIps();
	public abstract String getMainCbBucket();
	public abstract String getMainCbPass();
	
	@Inject
	public void setProjectStageUtil(ProjectStageUtil projectstage) {
		this.projectStageUtil = projectstage;
	}
	@Inject
	public void setUuidUtil(UuidGenerator uuidgenerator) {
		this.uuidGenerator = uuidgenerator;
	}
	
	/**
	 * checks if PROJECT_STAGE from web.xml
	 * @return 
	 */
	public boolean inDevelopmentStage() {
		
		boolean returnobj = false;
		
		try {
			
			returnobj = this.isStage("Development");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return returnobj;
		
	}
	/**
	 * checks if PROJECT_STAGE from web.xml
	 * @return 
	 */
	public boolean inUnitTestStage() {
		
		boolean returnobj = false;
		
		try {
			
			returnobj = this.isStage("UnitTest");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return returnobj;
		
	}
	
	private boolean isStage( String stage ) {
		return this.projectStageUtil.getStage().equals( stage );
	}
	
	public String getUuid() {
		return this.uuidGenerator.getUuid(this.getAppName());
	}
	
}