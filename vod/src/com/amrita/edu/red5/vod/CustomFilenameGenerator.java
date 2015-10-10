////////////////////////////////////////////////////////////////////////////////
//
// Copyright  © 2013 E-Learning Research Lab, 
// Amrita Vishwa Vidyapeetham. All rights reserved. 
// E-Learning Research Lab and the A-VIEW logo are trademarks or
// registered trademarks of E-Learning Research Lab. 
// All other names used are the trademarks of their respective owners.
//
////////////////////////////////////////////////////////////////////////////////

/**
 * 
 * File			: CustomFilenameGenerator.java
 * Module		: VOD 
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: class used for recording/playback from custom directories.
 * Dependencies: Used by vod by setting the custom directory value in red5-web.xml.
 *  
 */

package com.amrita.edu.red5.vod;

import java.io.File;

import org.apache.log4j.Logger;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.stream.IStreamFilenameGenerator;

/**
 * The Class CustomFilenameGenerator.
 */
public class CustomFilenameGenerator implements IStreamFilenameGenerator {

	 /** Path that contains VOD streams. */
    public static String playbackPath;
    /** Path that will store recorded videos. */
    public static String recordPath;
    //JHCR: Need to change the variable name, so that it looks like a boolean and the comment
    /** absolute or relative paths. */
   public static Boolean resolvesAbsolutePath;
    
    //private static final Log log = LogFactory.getLog(Application.class);
    /** The log. */
    private static Logger log = Logger.getLogger(CustomFilenameGenerator.class);
    
    /**
     * Gets the stream directory.
     *
     * @param scope the scope
     * @return the stream directory
     */
    private String getStreamDirectory(IScope scope) 
    {
    	log.info("getStreamDirectory:scope:" + scope.getName());
    	if (playbackPath == null)
    	{
    		playbackPath = "streams";
    	}
		final StringBuilder result = new StringBuilder();
		final IScope app = ScopeUtils.findApplication(scope);
		while (scope != null && scope != app)
		{
			result.insert(0, "/" + scope.getName());
			log.debug("getStreamDirectory:setting the result value to " + result);
			scope = scope.getParent();
			log.debug("getStreamDirectory:setting the scope to its parent:" + scope.getName());
		}
		log.info("getStreamDirectory:playbackPath:" + playbackPath + ", result:" + result);
		return playbackPath + result.toString();
	}
    
	/* (non-Javadoc)
	 * @see org.red5.server.api.stream.IStreamFilenameGenerator#generateFilename(org.red5.server.api.IScope, java.lang.String, org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType)
	 */
	public String generateFilename(IScope scope, String name, GenerationType type) {
		log.info("CustomFilenameGenerator:generateFilename:name: " + name);
		return generateFilename(scope, name, null, type);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.api.stream.IStreamFilenameGenerator#generateFilename(org.red5.server.api.IScope, java.lang.String, java.lang.String, org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType)
	 */
	public String generateFilename(IScope scope, String name, String extension, GenerationType type) {
		//if (type == GenerationType.RECORD){
			String filename;    
			//filename = getStreamDirectory(scope) + "/" + name;
			filename = getStreamDirectory(scope) + name;
	        if (extension != null)
	        {
	            // Add extension
	            filename += extension;
	        }
	        log.info("CustomFilenameGenerator:generateFilename:filename: " + filename);
	        return filename;
//		}
//		return "";
	}
	
	/* ----- Spring injected dependencies ----- */
    /* (non-Javadoc)
	 * @see org.red5.server.api.stream.IStreamFilenameGenerator#resolvesToAbsolutePath()
	 */
	//JHCR: Can rename to resolveToAbsolutePath
	public boolean resolvesToAbsolutePath() {
    	//return true;
    	return resolvesAbsolutePath;
    }
    
    /**
     * Sets the playback path.
     *
     * @param path the new playback path
     */
    public void setPlaybackPath(String path) {
    	log.info("setPlaybackPath:path:" + path);
    	
    	File file = new File(path);
		if (!file.exists())
		{
			try
			{
				log.info("created:" + file.mkdirs());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		log.info("setPlaybackPath:absolutepath:" + file.getAbsoluteFile());
		
    	playbackPath = path;
    }

    /**
     * Sets the record path.
     *
     * @param path the new record path
     */
    public void setRecordPath(String path) {
    	log.info("setRecordPath:path:" + path);
    	
    	File file = new File(path);
		if (!file.exists())
		{
			try
			{
				log.info("created:" + file.mkdirs());
			}
			catch (Exception exception)
			{
				exception.printStackTrace();
			}
		}
		log.info("setRecordPath:absolutepath:" + file.getAbsoluteFile());
        
		recordPath = path;
    }
    
    /**
     * Sets the resolved absolute path.
     *
     * @param absolute the new resolved absolute path
     */
    //JHCR: The function has to be renamed
    public void setResolvesAbsolutePath(Boolean absolute) {
    	resolvesAbsolutePath = absolute;
    }
}