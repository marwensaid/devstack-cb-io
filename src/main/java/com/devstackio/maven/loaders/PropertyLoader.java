package com.devstackio.maven.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;

/**
 *
 * @author devstackio
 */
@ApplicationScoped
public class PropertyLoader {
	
	/**
	 * load property from META-INF/filePath
	 * @param filepath directory starting at META-INF/
	 * @return 
	 */
	public Properties loadProperties( String file ) {
		
		return this.loadProperties( file, "META-INF" );
		
	}
	/**
	 * load properties from path+file
	 * @param file
	 * @param path
	 * @return 
	 */
	public Properties loadProperties( String file, String path ) {
		Properties returnobj = new Properties();
		String fullPath = path+file;

		InputStream in = new InputStream() {

			@Override
			public int read() throws IOException {
				return 0;
			}
		};
		try {
			in = ClassLoader.getSystemResourceAsStream( fullPath );
			returnobj.load(in);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { 
				in.close();
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		return returnobj;
	}
	/**
	 * used to grab Properties if needed @ ApplicationContextListener
	 * @param thread
	 * @return 
	 */
	public Properties loadFromThread( Thread thread, String filepath ) {
		
		Properties returnobj = new Properties();
		try {
			returnobj.load( thread.getContextClassLoader().getResourceAsStream( filepath ));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnobj;
	}
	
}
