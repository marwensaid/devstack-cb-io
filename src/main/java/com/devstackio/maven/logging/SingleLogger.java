package com.devstackio.maven.logging;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

/**
 *
 * @author devstackio
 */
public class SingleLogger {
	
	private Logger logger;
	private String fileName;
	private Appender fa;
	
	/**
	 * logs to independent file
	 * @param filename filename this logger will log to
	 */
	public SingleLogger(String filename) {
		try {
			if (filename != null && !filename.isEmpty()) {
				this.fileName = filename;
				init();
			} else {
				throw new Exception("filename cannot be null or empty");
			}
		} catch (Exception e) {
			System.out.println("[ SingleLogger ] error : " + e.getMessage());
		}
	}
	
	/**
	 * logs single message to this SingleLogger's filename
	 * @param msg 
	 */
	public void logSingle(String msg, Level level) {
		this.logger.log(level, msg);
	}
	
	private void init() {
		try {
			String filename = this.getFileName();
			this.logger = Logger.getLogger(filename);
			fa = new FileAppender(new SimpleLayout(), "../logs/" + filename + ".log");
			logger.addAppender(fa);
			fa.setLayout(new SimpleLayout());
		} catch (Exception e) {
			System.err.println("[ SingleLogger ] error : " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
}
