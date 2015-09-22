package com.devstackio.maven.projectstage;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 *
 * @author devstackio
 */
@Named
@RequestScoped
public class ProjectStageUtil {
	
	public String getStage() {
		String returnobj = "";
		try {
			returnobj = FacesContext.getCurrentInstance().getApplication().getProjectStage().toString();
		} catch (NullPointerException e) {
			System.out.println("Env not found, setting to Development...");
			returnobj="Development";
		}
		return returnobj;
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
		return this.getStage().equals( stage );
	}
	
}