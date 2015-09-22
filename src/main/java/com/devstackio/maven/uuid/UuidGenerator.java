package com.devstackio.maven.uuid;

import java.util.Map;
import java.util.UUID;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author devstackio
 */
@RequestScoped
public class UuidGenerator {
	
	/**
	 * if cookie exists : returns current uuid bound to browser cookie
	 * if not : returns new uuid and stores into cookie
	 * @param appname used as cookieName, should be defined in application config
	 * @return uuid for use in couchbase session docs
	 */
	public String getUuid( String appname ) {
		
		String returnobj = "";
		Cookie cookie = null;
		String cookieName = appname;
		
		try {
			
			if( FacesContext.getCurrentInstance() != null ) {
				ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
				HttpServletResponse response = (HttpServletResponse) ec.getResponse();
				Map<String, Object> cookieMap = ec.getRequestCookieMap();
				
				if ( cookieMap.containsKey( cookieName )) {
					
					cookie = (Cookie)cookieMap.get( cookieName );
					returnobj = cookie.getValue();
					
				} else {
					returnobj = UUID.randomUUID().toString();
					cookie = new Cookie( cookieName, returnobj );
				}
				
				//dead @ user session end
				cookie.setMaxAge(-1);
				response.addCookie( cookie );
				
			} else {
				UUID idOne = UUID.randomUUID();
				returnobj = idOne.toString();
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
			//cookie = new Cookie(this.COOKIE_NAME, "error");
		}
		
		return returnobj;
	}
	
}

