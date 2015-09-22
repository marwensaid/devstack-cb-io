package com.devstackio.maven.url;

import com.devstackio.maven.logging.IoLogger;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.log4j.Level;

/**
 *
 * @author devstackio
 */
@Named
@RequestScoped
public class UrlReader {
	
	private IoLogger ioLogger;

	@Inject
	public void setIoLogger(IoLogger iologger) {
		this.ioLogger = iologger;
	}
	
	public String getParameter( String param ) {
		
		String returnobj = "";
		try {
			FacesContext context = FacesContext.getCurrentInstance();
			Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
			returnobj = requestMap.get( param );
		} catch(NullPointerException e) {
			this.ioLogger.logTo("UrlReader", Level.ERROR, "NullPointer caught trying to get param : [" + param + "] : " + e.getMessage());
		} catch (Exception e) {
			this.ioLogger.logTo("UrlReader", Level.ERROR, "Error trying to get param : [" + param + "] : " + e.getMessage());
			e.printStackTrace();
		}
		
		return returnobj;
	}
	
}