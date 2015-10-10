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
 * File			: Application.java
 * Module		: Pre-testing 
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: Main class for pretesting_module in a classroom. Records streams during pretesting.
 * Dependencies: Nil.
 *  
 */

package com.amrita.edu.red5.pretesting_module;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.stream.ClientBroadcastStream;

/**
 * The Class Application.
 */
public class Application extends MultiThreadedApplicationAdapter {

	//private static final Log log = LogFactory.getLog(Application.class);
	/** The log. */
	private static Logger log = Logger.getLogger(Application.class);

	// private int T_PING_INTERVAL = 2000;
	// private int T_POLL_TO_LOG_RATIO = 100;

	/** The flix suffix. */
	private String FLIX_SUFFIX = "_flix";
	//private String BW_SUFFIX = "Kbps";

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appStart(org.red5.server.api.IScope)
	 */
	public boolean appStart(IScope app) {
		log.info("pretesting_module:appStart");
		return super.appStart(app);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appConnect(org.red5.server.api.IConnection, java.lang.Object[])
	 */
	public boolean appConnect(IConnection conn, Object[] params) {
		log.info("pretesting_module:appConnect");

		return super.appConnect(conn, params);
	}

	/**
	 * This method will execute when first client will connect to Red5 server.
	 *
	 * @param room the room
	 * @return true, if successful
	 */
	public boolean roomStart(IScope room) {
		log.info("pretesting_module:roomStart:scope: " + room.getName());

		return super.roomStart(room);
	}

	/**
	 * This method will execute every time when a client will connect to Red5
	 * server.
	 *
	 * @param conn the conn
	 * @param params the params
	 * @return true, if successful
	 */
	public boolean roomConnect(IConnection conn, Object params[]) {
		if (super.roomConnect(conn, params) == false) {
			return false;
		}
		log.info("pretesting_module:roomConnect:scope: "
				+ conn.getScope().getName());
		log.info("pretesting_module:roomConnect:params: " + params);
		IClient client = conn.getClient();
		client.setAttribute("filename", "");
		log.info("pretesting_module:roomConnect:client.filename: "
				+ client.getAttribute("filename"));
		return true;
	}

	/**
	 * This method will be called when a client disconnect from the room.
	 *
	 * @param conn the conn
	 */
	public void roomDisconnect(IConnection conn) {
		log.info("pretesting_module:roomDisconnect:userName: "
				+ conn.getClient().getAttribute("name"));
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appDisconnect(org.red5.server.api.IConnection)
	 */
	public void appDisconnect(IConnection conn) {
		log.info("Red5First.appDisconnect " + conn.getClient().getId());
		super.appDisconnect(conn);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appStop(org.red5.server.api.IScope)
	 */
	public void appStop(IScope app) {
		log.info("appStop:scope: " + app.getName());
		super.appStop(app);
	}

	/**
	 * Flix user.
	 *
	 * @param flixUsername the flix username
	 */
	public void flixUser(String flixUsername) {
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("flixUser:Flix UserName:" + flixUsername + ", Client:"
				+ client.getId() + ", IP:" + conn.getRemoteAddress());
		client.setAttribute("name", flixUsername + FLIX_SUFFIX);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#streamBroadcastStart(org.red5.server.api.stream.IBroadcastStream)
	 * streamBroadcastStart - Notified when a broadcaster starts.
	 */
	public void streamBroadcastStart(IBroadcastStream s) {
		IClientBroadcastStream stream = (IClientBroadcastStream) s;
		log.info("pretesting_module:streamBroadcastStart: "
				+ stream.getPublishedName());
		super.streamBroadcastStart(s);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#streamPublishStart(org.red5.server.api.stream.IBroadcastStream)
	 * This will be called when the first video packet has been received.
	 */
	public void streamPublishStart(IBroadcastStream s) {
		IClientBroadcastStream stream = (IClientBroadcastStream) s;
		log.info("pretesting_module:streamPublishStart: "
				+ stream.getPublishedName());
		super.streamPublishStart(s);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#streamRecordStart(org.red5.server.api.stream.IBroadcastStream)
	 * A broadcast stream starts being recorded.
	 */
	public void streamRecordStart(IBroadcastStream s) {
		super.streamRecordStart(s);
		log.info("pretesting_module:streamRecordStart:Entered Function");
		IClientBroadcastStream stream = (IClientBroadcastStream) s;

		log.info("pretesting_module:streamRecordStart:streamPublshiedName: "
				+ stream.getPublishedName());
		// stream is published as "mp4:streamname.f4v". While saving, remove the
		// "mp4:" suffix from stream name.

		String streamPublishName = stream.getPublishedName();
		String streamRecordName = streamPublishName;

		int startIndex = streamPublishName.indexOf("mp4:");
		// int endIndex;
		if (startIndex != -1) {
			log.info("mp4 stream recording");
			int endIndex = streamPublishName.indexOf(".f4v");
			streamRecordName = streamPublishName.substring(startIndex + 4,
					endIndex - 1);
		}

		log.info("pretesting_module:streamRecordStart:streamRecordName: "
				+ streamRecordName);

		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();

		log.info("streamPublishStart, clientName:"
				+ client.getAttribute("name") + ", Stream:"
				+ stream.getPublishedName() + ", IP:" + conn.getRemoteAddress()
				+ ", id:" + client.getId());

		try {
			// Save the stream to disk.
			stream.saveAs(streamRecordName, false);
		} catch (Exception e) {
			log.error(
					"Error while saving stream: " + stream.getPublishedName(),
					e);
		}

		log.info("Exited Function - streamPublishStart");
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#streamBroadcastClose(org.red5.server.api.stream.IBroadcastStream)
	 * Notified when a broadcaster closes.
	 */
	public void streamBroadcastClose(IBroadcastStream stream) {
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();

		log.info("streamBroadcastClose:Stream:" + stream.getPublishedName()
				+ ", IP:" + conn.getRemoteAddress() + ", id:" + client.getId()
				+ ", discFlag:"
				+ client.getAttribute("disconnectedDuringRetrys"));

		String streamPublishName = stream.getPublishedName();
		if (streamPublishName.indexOf("mp4:") != -1) {
			String folderName = conn.getScope().getName();
			log.info("scope name: " + conn.getScope().getName());
			log.info("scope path: " + conn.getScope().getPath());
			File file1 = new File("streams/" + folderName + "/"
					+ streamPublishName + ".flv");
			log.info("file1: " + file1.getAbsolutePath());
			File file2 = new File("streams/" + folderName + "/"
					+ streamPublishName + ".f4v");
			file1.renameTo(file2);
			log.info("file2: " + file2.getAbsolutePath());
			log.info("flv renamed to f4v");
		}
	}

	/**
	 * Recording.
	 *
	 * @param userName the user name
	 * @param tempFileName the temp file name
	 */
	public void recording(String userName, String tempFileName) {
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		client.setAttribute("fileName", tempFileName);
		log.info("pretesting_module:recording:username: " + userName
				+ ", tempFileName:" + tempFileName + ", client.fileName: "
				+ client.getAttribute("fileName"));
		// log.info("pretesting_module:recording:scope: " + conn.getScope() +
		// ", parent scope: " + conn.getScope().getParent());
		// org.springframework.core.io.Resource[]flvs;
		// try{
		// flvs = conn.getScope().getResources("/streams/*.flv");
		// log.info("flvs: " + flvs);
		// }
		// catch(IOException e){
		// log.info("IOException while getting resources");
		// }
		ClientBroadcastStream stream = (ClientBroadcastStream) getBroadcastStream(
				conn.getScope(), userName);
		log.info("pretesting_module:recording:stream: " + stream);
		client.setAttribute("s", stream);
		if (client.getAttribute("s") != null) {
			log.info("recording stream found");
			// client.s.record();
			try {
				// Save the stream to disk.
				stream.saveAs(userName, false);
			} catch (Exception e) {
				log.error("Error while saving stream: " + userName, e);
			}
		}
		log.info("Exited Function - recording: userName " + userName);
	}

	/**
	 * Stop recording.
	 *
	 * @param userName the user name
	 * @param folderName the folder name
	 */
	public void stopRecording(String userName, String folderName) {
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("stopRecording username: " + userName + ", folderName :"
				+ folderName + ", client.fileName: "
				+ client.getAttribute("fileName"));

		//if (client.getAttribute("fileName").toString() != "") {
		if (client.getAttribute("fileName") != null) {
			log.info("stopRecording:fileName: "
					+ client.getAttribute("fileName") + ", userName: "
					+ userName);
			// client.s.flush();
			// client.s.record(false);
			ClientBroadcastStream stream = (ClientBroadcastStream) client
					.getAttribute("s");
			stream.stopRecording();

			IScope scope = conn.getScope();
//			String appPath = scope.getPath();
//			int index = appPath.indexOf("/default/");
//			if (index != -1) {
//				// path = path.substring(index+8) + "/streams/" +
//				// scope.getContextPath();
//				appPath = appPath.substring(index + 8);
//				log.info("pretesting_module:stopRecording:appPath: " + appPath);
//			}
//			String filePath = System.getProperty("red5.webapp.root") + "/"
//					+ appPath;
			
			String filePath = System.getProperty("red5.webapp.root") + "/" + scope.getParent().getName();
			
			// The stream is recorded using 'userName', but played back using
			// 'filename'.
			// Therefore the file needs to be renamed.
			File errorLog = new File(filePath + "/streams/" + folderName + "/"
					+ userName + ".flv");
			// log.info("errorLog.absolutePath: " + errorLog.getAbsolutePath());
			// log.info("errorLog exists: " + errorLog.exists());
			File tempD = new File(filePath + "/streams/" + folderName + "/"
					+ client.getAttribute("fileName") + ".flv");
			File errorLog1 = new File(filePath + "/streams/" + folderName + "/"
					+ client.getAttribute("fileName") + ".flv");

			if (tempD.exists()) {
				log.info("file exist for removal " + tempD);
				tempD.delete();
				log.info("temp exists");
			} else {
				log.info(" file not exist for removal " + tempD);
			}
			log.info("Checking existence of file errorLog: ");
			if (errorLog.exists()) {
				log.info("errorLog file exist");
				// try{
				// FileInputStream in = new FileInputStream(errorLog);
				// FileOutputStream out = new FileOutputStream(errorLog1);
				// byte [] b = new byte[1024];
				// in.read(b, 0, (int)errorLog.length());
				// out.write(b);
				// log.info("successfully copied errorLog to errorLog1");
				// }
				// catch(Exception e){
				// log.info("failed to copy errorLog to errorLog1");
				// }
				errorLog.renameTo(errorLog1);
				log.info("stopRecroding:errorLog renamed to errorLog1");
			} else {
				log.info("errorLog file not found");
			}
			log.info("stopRecording Exiting function ");
		}
	}

	/**
	 * Delete file.
	 *
	 * @param userName the user name
	 * @param folderName the folder name
	 * @param lstStreamNames the lst stream names
	 */
	public void deleteFile(String userName,
			String folderName, List lstStreamNames) {
		log.info("pretesting_module:deleteFile:userName: " + userName + ", folderName :"
				+ folderName);
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
//		String appPath = scope.getPath();
//		int index = appPath.indexOf("/default/");
//		if (index != -1) {
//			// path = path.substring(index+8) + "/streams/" +
//			// scope.getContextPath();
//			appPath = appPath.substring(index + 8);
//			log.info("pretesting_module:deleteFile:appPath: " + appPath);
//		}
//		String filePath = System.getProperty("red5.webapp.root") + "/"
//				+ appPath;
		String filePath = System.getProperty("red5.webapp.root") + "/" + scope.getParent().getName();
		// The stream is recorded using 'userName', but played back using
		// 'filename'.
		// Therefore the file needs to be renamed.
		File tempD = new File(filePath + "/streams/" + folderName + "/"
				+ userName + ".flv");

		log.info("pretesting_module:deleteFile:tempD: " + tempD.getAbsolutePath());
		if (tempD.exists()) {
			log.info("File with username will be deleted. userName: "
					+ userName);
			tempD.delete();
			// log.info("temp exists deleteFile");
		} else {
			log.info("File with username does not exist - delete failed. userName: "
					+ userName);
			// log.info("temp not deleteFile");
		}
		Object[] streamNames = lstStreamNames.toArray();
		log.info("Delete vp6 files...");
		for (int i = 0; i < streamNames.length; i++) {
			tempD = new File(filePath + "/streams/" + folderName + "/"
					+ streamNames[i].toString() + ".flv");
			
			if (tempD.exists()) {
				log.info("streamNames File exists for removal");
				boolean success = tempD.delete();
				log.info("tempD delete status: " + success);
				if (success){
					File metaFile = new File(filePath + "/streams/" + folderName + "/"
						+ streamNames[i].toString() + ".flv.meta");
					if (metaFile.exists()){
						log.info("deleting meta file: " + metaFile.getName());
						success = metaFile.delete();
						log.info("metaFile delete status: " + success);
					}
				}
				
			} else {
				log.info("streamNames File does not exist for removal");
				// log.info("temp not deleteFile");
			}

		}
		// log.info("File deletion flv");
		log.info("deleteFile function exited.");
	}
}
