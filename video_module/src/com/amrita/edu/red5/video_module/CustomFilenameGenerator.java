/*
 * Author: Radha
 * Description: class used for recording/playback from custom directories.
 * Dependencies: Not currently used. But can be used by video_module if the custom directory value is set in red5-web.xml.
 * Note: for future use only.
 */
package com.amrita.edu.red5.video_module;

import org.apache.log4j.Logger;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.stream.IStreamFilenameGenerator;

/**
 * The Class CustomFilenameGenerator.
 */
public class CustomFilenameGenerator implements IStreamFilenameGenerator {

    /** Path that will store recorded videos. */
    public static String recordPath;
    /** Path that contains VOD streams. */
    public static String playbackPath;
    
    //private static final Log log = LogFactory.getLog(Application.class);
    /** The log. */
    private static Logger log = Logger.getLogger(Application.class);
    
    /**
     * Gets the stream directory.
     *
     * @param scope the scope
     * @return the stream directory
     */
    private String getStreamDirectory(IScope scope) {
		final StringBuilder result = new StringBuilder();
		final IScope app = ScopeUtils.findApplication(scope);
		while (scope != null && scope != app) {
			result.insert(0, "/" + scope.getName());
			scope = scope.getParent();
		}
		return playbackPath + result.toString();
	}
    
	/* (non-Javadoc)
	 * @see org.red5.server.api.stream.IStreamFilenameGenerator#generateFilename(org.red5.server.api.IScope, java.lang.String, org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType)
	 */
	public String generateFilename(IScope scope, String name, GenerationType type) {
		return generateFilename(scope, name, null, type);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.api.stream.IStreamFilenameGenerator#generateFilename(org.red5.server.api.IScope, java.lang.String, java.lang.String, org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType)
	 */
	public String generateFilename(IScope scope, String name, String extension, GenerationType type) {
		//if (type == GenerationType.RECORD){
			String filename;    
			filename = getStreamDirectory(scope) + "/" + name;
	        if (extension != null)
	            // Add extension
	            filename += extension;
	        log.info("CustomFilenameGenerator:generateFilename:filename: " + filename);
	        return filename;
//		}
//		return "";
	}
	
	/* ----- Spring injected dependencies ----- */
    /* (non-Javadoc)
	 * @see org.red5.server.api.stream.IStreamFilenameGenerator#resolvesToAbsolutePath()
	 */
	public boolean resolvesToAbsolutePath() {
    	return true;
    }
    
    /**
     * Sets the playback path.
     *
     * @param path the new playback path
     */
    public void setPlaybackPath(String path) {
    	playbackPath = path;
    }

    /**
     * Sets the record path.
     *
     * @param path the new record path
     */
    public void setRecordPath(String path) {
        recordPath = path;
    }
}