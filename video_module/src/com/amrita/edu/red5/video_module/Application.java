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
 * Module		: Video 
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: Main class for video_module in a classroom. Handles audio/video live streaming, recording and renaming the recorded files
 * Dependencies: Nil.
 *  
 */

package com.amrita.edu.red5.video_module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.stream.ClientBroadcastStream;

  
/**
 * The Class Application.
 * ApplicationAdapter/MultiThreadedApplicationAdapter serves as a base class for all Red5 applications.
 * It provides methods to work with SharedObjects and streams, as well as connections and scheduling services.
 * All custom red5 applications MUST implement either ApplicationAdapter or MultiThreadedApplicationAdapter for Red5 Server to listen to the connection
 * requests from the client.
 * All the public methods added in this subclass can be called from client side with NetConnection.call method.
 */
@SuppressWarnings("unchecked")
public class Application extends MultiThreadedApplicationAdapter
//public class Application extends ApplicationAdapter 
{

	/** The log. */
	private static Logger log = Logger.getLogger(Application.class);
	
	//JHCR: According to our guidelines, the variable names should be in lower camel case; but is there any reason for having the names like this? Are they constants?
	//JHCR: The variable name should be meaningful, does T stand for time? Also, please give some note for choosing the value 2000, 
	//		I think this can be a constant.
	/** The t ping interval. */
	private int T_PING_INTERVAL = 2000;
	
	//JHCR: The variable name should be meaningful, does T stand for time? Also, please give some note for choosing the value 100, 
	//		if it is a constant value, it variable should be constant	
	/** The t poll to log ratio. */
	private int T_POLL_TO_LOG_RATIO = 100;

	//JHCR: This can be a constant
	/** The flix suffix. */
	private final String FLIX_SUFFIX = "_flix";
	
	//JHCR: This can be a constant
	/** The bw suffix. */
	private final String BW_SUFFIX = "Kbps";
	
	//JHCR: This can be a constant
	/** The viewer suffix. */
	private final String VIEWER_SUFFIX = "VIEWER";

	/*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appStart(org.red5.server.api.IScope)
	 * Called when application scope is started. 
	 */
	public boolean appStart(IScope app) {
		log.info("appStart");
		return super.appStart(app);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appConnect(org.red5.server.api.IConnection, java.lang.Object[])
	 */
	//JHCR: Variable name conn can be more meaningful, it can be connection or something
	public boolean appConnect(IConnection conn, Object[] params) {
		log.info("appConnect");
		//JHCR: We can delete the following block since it is not used.
		//		IClient client = conn.getClient();
		//		log.info("video_module:appConnect:params.length: " + params.length
		//				+ ", scope: " + conn.getScope().getName());
		//		// if the connection is made from on2flix for vp6 encoding, no params
		//		// are passed.
		//		if (params.length == 0) {
		//			log.info("video_module:appConnect:flixUser");
		//			return true;
		//		}
		//
		//		client.setAttribute("name", params[0]);
		//		log.info("video_module:appConnect:Client id: " + client.getId()
		//				+ "client name: " + client.getAttribute("name"));

		// sList = new ArrayList();
		return super.appConnect(conn, params);
	}

	/**
	 * This method will execute when first client will connect to Red5 server.
	 *
	 * @param room the room
	 * @return true, if successful
	 */
	@SuppressWarnings("rawtypes")
	public boolean roomStart(IScope room) {
		log.info("roomStart:scope: " + room.getName());
		//JHCR: The variable sList can be named meaningfully, it can streamList or something
		List sList = new ArrayList();
		HashMap<String, Object> streamArray = new HashMap<String, Object>();		
		room.setAttribute("streamArray", streamArray);
		room.setAttribute("sList", sList);
		room.setAttribute("recordingPresenterFile", "");
		room.setAttribute("presenterFilenameForRename", "");
		room.setAttribute("recordingViewerFile", "");
		room.setAttribute("viewerFilenameForRename", "");
		
		// streamConnection=new Object[10];

		// Timer to call the ping method. This method pings the client to see if
		// it's live
		// and if is not live, disconnects
		// setInterval(pollClients,T_PING_INTERVAL);
		log.info("calling PollClients");
		pollClients(room);

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
	//JHCR: Variable name conn can be more meaningful, it can be connection or something
	public boolean roomConnect(IConnection conn, Object params[]) {
		if (super.roomConnect(conn, params) == false) {
			return false;
		}
		log.info("roomConnect");
		// if the connection is made from on2flix for vp6 encoding, no params
		// are passed.
		if (params.length == 0) {
			log.info("video_module:roomConnect:flixUser:accepting connection, returning true");
			return true;
		}
		
		//JHCR: Variable name 'name' can be more meaningful, it can be roomName or something
		String name = params[0].toString();
		int retryCounter = Integer.parseInt(params[1].toString());
		String hardwareAddress = params[2].toString();
		
		IClient client = conn.getClient();
		client.setAttribute("hardwareAddress", hardwareAddress);
		
		log.info("video_module:roomConnect:Client:" + client.getId() + ", " +
				conn.getRemoteAddress() + ", Name:" + name + ", Retry:" + retryCounter +
				",hardwareAddress:"+ client.getAttribute("hardwareAddress"));

		if (name != null) {
			// Explicitly close the prvious connections on the same user name
			// This is being done for two reasons
			// 1. To expedite the closing of previous connections, as there some
			// times delay in server getting the disconnect event.
			// Because of this delay, there are issues when the client tries to
			// re-publish the video
			// 2. In case duplicate login we want to close the earlier login
			// with a message
			disconnectPreviousConnections(name, client.getId(), retryCounter,
					conn.getRemoteAddress(),client.getAttribute("hardwareAddress").toString());
			// Disconnect earlier client's publishing connection
			disconnectPreviousConnections(name + FLIX_SUFFIX, client.getId(),
					retryCounter, conn.getRemoteAddress(),client.getAttribute("hardwareAddress").toString());

		}

		client.setAttribute("name", name);

		// This flag denotes whether this connection is closed because of
		// retrys..
		// This is a initialization
		client.setAttribute("disconnectedDuringRetrys", false);

		log.info("roomConnect:connecting user to server:username: " + name);

		// log.info("Client.agent:"+client.agent);
		// log.info("Client.ip:"+client.ip);
		// log.info("Client.pageUrl:"+client.pageUrl);
		// log.info("Client.referrer:"+client.referrer);
		// log.info("Client.uri:"+client.uri);
		// log.info("Client.virtualKey:"+client.virtualKey);
		// log.info("Client.virtualKey:"+client.virtualKey);

		// This counter is used to log the client poll message only once for
		// T_POLL_TO_LOG_RATIO times
		client.setAttribute("clientPingLogMessageCounter", 0);

		return true;
	}

	/**
	 * This method will be called when a client is disconnected from the room.
	 *
	 * @param conn the conn
	 */
	public void appDisconnect(IConnection conn) {
		//super.appDisconnect(conn);
		
		IClient client = conn.getClient();
		String clientName = client.getAttribute("name").toString();
		log.info(conn.getScope().getAttribute("recordingPresenterFile").toString());
		log.info("video_module:appDisconnect:userName:" + clientName);
		// if onDisconnect is called after publishing
		List<Object> sList = (ArrayList<Object>)conn.getScope().getAttribute("sList");
		if(sList.size()!= 0)
		{
			boolean previousStreamExistAndStopped = false;
			for(int i=0; i<sList.size(); i++)
			{
				HashMap<String, Object> obj = (HashMap<String, Object>) sList.get(i);
				String streamName = obj.get("streamName").toString();
				if (streamName.equals(clientName) || streamName.equals(clientName+"_VIEWER"))
				{
					if(!obj.get("id").toString().equals(client.getId()))
					{
						log.info("appDisconnect:Previous recording renamed already");
						return;
					
					}
					else
					{
						previousStreamExistAndStopped = true;
						log.info("appDisconnect:Removing the old stream ("+streamName + ") from the list. Length of stream list array:" + sList.size());
						sList.remove(i);
						stopRecordingAndRenamePreviousStream(clientName);
					}
					break;
				}
			}
			if(previousStreamExistAndStopped == false)
			{
				stopRecordingAndRenamePreviousStream(client.getAttribute("name").toString());
			}
			conn.getScope().setAttribute("sList", sList);
		}
		else
		{
			stopRecordingAndRenamePreviousStream(clientName);
		}
	}
	
	/**
	 * Stop recording and rename previous stream.
	 * 
	 * //JHCR: Please add description here
	 *
	 * @param streamName the stream name
	 */
	private void stopRecordingAndRenamePreviousStream(String streamName)
	{
		log.info("stopRecordingAndRenamePreviousStream: the stream is:" + streamName);
		IScope scope=Red5.getConnectionLocal().getScope();
		String presenterStream="";
		String presenterStreamExtnsn="";
		String viewerStream="";
		String viewerStreamExtnsn="";
		String recordingPresenterFile=scope.getAttribute("recordingPresenterFile").toString();
		String recordingViewerFile=scope.getAttribute("recordingViewerFile").toString();
		String presenterFilenameForRename=scope.getAttribute("presenterFilenameForRename").toString();
		String viewerFilenameForRename=scope.getAttribute("viewerFilenameForRename").toString();
		if(recordingPresenterFile!="")
		{
			presenterStream = recordingPresenterFile.substring(0,recordingPresenterFile.length()-4);
			presenterStreamExtnsn = recordingPresenterFile.substring(recordingPresenterFile.length()-4);
			log.info("The previous reorded file :"+recordingPresenterFile+",UserType:Presenter , File extention: "+presenterStreamExtnsn);
		}
		if(recordingViewerFile!="")
		{
			viewerStream = recordingViewerFile.substring(0,recordingViewerFile.length()-4);
			viewerStreamExtnsn = recordingViewerFile.substring(recordingViewerFile.length()-4);
			log.info("The previous reorded file :"+recordingViewerFile+",UserType:Viewer , File extention: "+viewerStreamExtnsn);
		}
		
		//JHCR: We can delete the following block since it is not used.
		//var str =application.name.split("/");
		//var streamPath = "/streams/"+str[1]+"/";
		//IConnection conn = Red5.getConnectionLocal();
		
		//JHCR: The variable str1 can be named more meaningfully
		String str1 = System.getProperty("red5.webapp.root") + "/" + scope.getParent().getName();
		//JHCR: The variable str can be named more meaningfully
		String str = scope.getName();
		String streamPath = str1 + "/streams/" + str+"/";
		String sourceFilleName; 
		String destinationFileName;
		
		log.info("streamName:"+streamName+" Presenter:"+ presenterStream);
	    if(streamName.equals(presenterStream)||streamName.equals(presenterStream+"_flix"))
		{
	    	
			sourceFilleName = streamPath + recordingPresenterFile;
			destinationFileName = streamPath + presenterFilenameForRename;
			stopRecordAndRenameTheStream(streamName, sourceFilleName, destinationFileName);
		}
		else if(viewerStream.equals(streamName) || viewerStream.equals(streamName+"_VIEWER")|| streamName.equals(viewerStream+"_flix"))
		{
			sourceFilleName = streamPath +recordingViewerFile;
			destinationFileName=streamPath +viewerFilenameForRename;
			stopRecordAndRenameTheStream(viewerStream, sourceFilleName, destinationFileName);
		}
	}
	
	/**
	 * Stop record and rename the stream.
	 * 
	 * //JHCR: Please add description here
	 *
	 * @param streamName the stream name
	 * @param sourseFilleName the sourse fille name
	 * @param destinationFileName the destination file name
	 */
	private void stopRecordAndRenameTheStream(String streamName, String sourseFilleName, String destinationFileName)
	{
		IScope scope = Red5.getConnectionLocal().getScope();
		log.info("stopRecordAndRenameTheStream:Going to call stopRecord and rename the stream(previous stream):" + streamName);
		stopRecordAndClearStream(streamName, false);
		renameRecordedStream(sourseFilleName, destinationFileName);
		scope.setAttribute("recordingPresenterFile","");
		scope.setAttribute("presenterFilenameForRename","");
	}
	
	/**
	 * Stop record and clear stream.
	 * 
	 * //JHCR: Please add description here
	 *
	 * @param streamName the stream name
	 * @param needToClearStream the need to clear stream
	 */
	private void stopRecordAndClearStream(String streamName, boolean needToClearStream)
	{
		IScope scope = Red5.getConnectionLocal().getScope();
		log.info("stopRecordAndClearStream:streamName:" + streamName + ",scope: " + scope.getName());
		
		HashMap<String, Object> streamArray = null;	// = new HashMap<String, Object>();
		if(scope.hasAttribute("streamArray"))	
		{
			streamArray=(HashMap<String, Object>)scope.getAttribute("streamArray");
			log.info("stopRecordAndClearStream:streamArray.size:" + streamArray.size());
		}
		
		HashMap<String, Object> streamObj = null;	//(HashMap<String, Object>) streamArray.get(streamName);
		ClientBroadcastStream videoStream = null;
		boolean isF4V = false;
		String fileExtn = ".flv";
		
		//if (streamObj != null)
		if (streamArray.containsKey(streamName))
		{
			streamObj = (HashMap<String, Object>) streamArray.get(streamName);
			isF4V = (Boolean) streamObj.get("isF4V");
			if (isF4V)
			{
				fileExtn = ".f4v";
			}
			else
			{
				fileExtn = ".flv";
			}
			streamArray.remove(streamName);
			log.info("stopRecordAndClearStream:Deleted the stream:" + streamName + " from streamArray");
			scope.setAttribute("streamArray", streamArray);
		}
		
		boolean msg = false;
		//JHCR: We can delete the following block since it is not used.
		//		List<String> streams = getBroadcastStreamNames(scope);
		//		log.info("stopRecordAndClearStream:streams.size:" + streams.size());
		//		
		//		for (int i = 0; i < streams.size(); i++)
		//		{
		//			log.info("stopRecordAndClearStream:streamName:" + streams.get(i));
		//		}
		//		
		//if (getBroadcastStream(scope, streamName) != null)
		//if (streams.size() > 0)
		if (hasBroadcastStream(scope, streamName))
		{
			videoStream =  (ClientBroadcastStream) getBroadcastStream(scope, streamName);
			log.info("stopRecordAndClearStream:calling stop recording, streamName:" + streamName + ", isF4V: " + isF4V); 
			
			if (isF4V)
			{
				stopFFmpeg(streamName, needToClearStream);
			}
			else
			{
				videoStream.stopRecording();
				IStreamListener listener = (IStreamListener) videoStream.getStreamListeners().toArray()[0];
				videoStream.removeStreamListener(listener);
			}
			
			msg = videoStream.isRecording();
			log.info("stopRecordAndClearStream:called stop record for the stream:" + streamName +", result: " + msg);
			//meta data is not automatically inserted into videos recording by red5.
			//use a third-party tool for injecting meta data immediately after recording,
			//before the recorded file is renamed and moved to vod folder.
//			injectMetaData(streamName, isF4V);
		}
		else
		{
			log.info("stopRecordAndClearStream:could not get videostream:" + videoStream);
		}

		if(needToClearStream)
		{
			msg = clearStreams(streamName + fileExtn);
			log.info("stopRecordAndClearStream:Clearing " + streamName + fileExtn + " stream:" + msg);
		}
	}
	
	//JHCR: We can delete the following block since it is not used.
	//	private void injectMetaData(String filename, boolean isF4V)
	//	{
	////		String filename = null;
	////		IScope scope = Red5.getConnectionLocal().getScope();
	////		String recordingPresenterFile = scope.getAttribute("recordingPresenterFile").toString();
	////		String recordingViewerFile = scope.getAttribute("recordingViewerFile").toString();
	////		
	////		if (recordingPresenterFile != null && recordingPresenterFile.equals(streamName))
	////		{
	////			filename = recordingPresenterFile;
	////		}
	////		else
	////		{
	////			filename = recordingViewerFile;
	////		}
	//		if (isF4V)
	//			filename += ".f4v";
	//		else
	//			filename += ".flv";
	//
	//		log.info("injectMetaData:filename: " + filename);
	//		
	//		String appScopeName = ScopeUtils.findApplication(scope).getName();
	//		IScope scope = Red5.getConnectionLocal().getScope();
	//		log.info("clearStreams :Recordedfilename:"+filename);
	//		//File file = new File(String.format("%s/webapps/%s/streams/%s/%s", System.getProperty("red5.root"), appScopeName,scope.getName(), filename));
	//		String file = String.format("%s/webapps/%s/streams/%s/%s", System.getProperty("red5.root"), appScopeName, scope.getName(), filename);
	//		File fileObj = new File(file);
	//		log.info ("file.exists: " + fileObj.exists());
	//		log.info("injectMetaData:file: " + file);
	//		//execute the third-party tool flvtool2 for injecting meta data into the recording file.
	//		String cmd = "flvtool2 -U " + file;
	//		try{
	//			Process p = Runtime.getRuntime().exec(cmd);
	//			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream())); 
	//			String line = reader.readLine(); 
	//			while(line!=null) 
	//			{ 
	//				log.info("flvtool2: " + line); 
	//				line = reader.readLine(); 
	//			} 
	//		}
	//		catch(IOException e)
	//		{
	//			log.info("injectMetaData:failed to inject metadata: " + e.getMessage());
	//		}
	//	}
	
	//Note: stream.saveAs() is used for recorded a stream. This method automatically deletes any previously recorded files with same URI.
	//But this method is written to simulate application.clearStreams functionality in fms, and to ensure the deletion of stream immediately after renaming the recorded file.
	/**
	 * Clear streams.
	 * 
	 * @param recordedFilename the recorded filename
	 * @return true, if successful
	 */
	private boolean clearStreams(String recordedFilename)
	{
		IScope scope = Red5.getConnectionLocal().getScope();
		
		String appScopeName = ScopeUtils.findApplication(scope).getName();
		log.info("clearStreams:Recordedfilename:" + recordedFilename);
		String filePath = String.format("%s/webapps/%s/streams/%s/%s", System.getProperty("red5.root"), appScopeName, scope.getName(), recordedFilename);
		log.info("clearStreams:filePath:" + filePath);
		File file = new File(filePath);
		log.info("clearStreams:File exists:" + file.getPath() + ":" + file.exists());
		if (file.exists() && !file.delete())
		{
			return false;
		}
		return true;
	}
	
//	private boolean clearStreams(String streamName)
//	{
//		// Get stream scope
//		//IStreamCapableConnection conn = (IStreamCapableConnection) Red5.getConnectionLocal();
//		IConnection conn = Red5.getConnectionLocal();
//		// get connections scope
//		IScope scope = conn.getScope();
//		// get the file for our filename
//		File file = getRecordFile(scope, streamName);
//		if (file.exists()) {
//			// when "live" or "record" is used, any previously recorded stream with the same stream URI is deleted.
//			if (!file.delete()) {
//				return false;
//			}
//		}
//		return true;
//	}
//	
//	protected File getRecordFile(IScope scope, String name) {
//		// get stream filename generator
//		IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
//		// generate filename
//		String recordingFilename = generator.generateFilename(scope, name, ".flv", GenerationType.RECORD);
//		File file = null;
//		if (generator.resolvesToAbsolutePath()) {
//			file = new File(recordingFilename);
//		} else {
//			Resource resource = scope.getContext().getResource(recordingFilename);
//			if (resource.exists()) {
//				try {
//					file = resource.getFile();
//					log.info("File exists");
//				} catch (IOException ioe) {
//					log.error("File error: {}", ioe);
//				}
//			} else {
//				String appScopeName = ScopeUtils.findApplication(scope).getName();
//				file = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, recordingFilename));
//			}
//		}
//		return file;
//	}
	
	/**
	 * Rename recorded stream.
	 *
	 * @param source the source
	 * @param newName the new name
	 */
	public void renameRecordedStream(String source, String newName)
	{
		log.info("renameRecordedStream:source:" + source +" newName:  " + newName);
		File file = new File(source);
//		if(file.copyTo(newName))
//		{
//			file.remove();
//		}
		if (copyfile(source, newName))
		{
			log.info("Successfully copied file to " + newName);
			if(file.delete())
				log.info(" deleted file " + source);
		}
	}
	
	/**
	 * Copy file.
	 * //JHCR: Please add description here
	 * 
	 * @param srFile the sr file
	 * @param dtFile the dt file
	 * @return true, if successful
	 */
	//JHCR: Parameters should be renamed to sourceFile and destinationFile
	private boolean copyfile(String srFile, String dtFile)
	{
		  try
		  {
			  //JHCR: Variable should be renamed 
			  File f1 = new File(srFile);
			  //JHCR: Variable should be renamed
			  File f2 = new File(dtFile);
			  //JHCR: Variable can be renamed
			  InputStream in = new FileInputStream(f1);
			  
			  //For appending the file.
			//  OutputStream out = new FileOutputStream(f2,true);
	
			  //For Overwrite the file.
			  //JHCR: Variable can be renamed
			  OutputStream out = new FileOutputStream(f2);
			  
			  //JHCR: Variable should be renamed
			  byte[] buf = new byte[1024];
			  //JHCR: Variable should be renamed
			  int len;
			  while ((len = in.read(buf)) > 0)
			  {
				  out.write(buf, 0, len);
			  }
			  in.close();
			  out.close();
			  log.info("File copied.");
			  return true;
		  }
		  catch(FileNotFoundException ex){
			  log.info(ex.getMessage() + " in the specified directory.");
			  return false;
		  }
		  catch(IOException e){
			  log.info(e.getMessage());
			  return false;
		  }
	}
	
	/**
	 * This method will be called when a client about to disconnect from the room.
	 *
	 * @param conn the conn
	 */
	//JHCR: Parameter should be renamed to connection or something meaningful
	public void roomDisconnect(IConnection conn) {
		log.info("appDisconnect " + conn.getClient().getId());
		super.roomDisconnect(conn);
	}

	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomStop(org.red5.server.api.IScope)
	 */
	public void roomStop(IScope room) {
		log.info("roomStop:scope: " + room.getName());
		
		Timer timer = (Timer) room.getAttribute("pollTimer");
		if (timer != null)
		{
			log.info("roomStop:cancelling poll timer");
			timer.cancel();
		}
		
		super.roomStop(room);
	}
	
	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appStop(org.red5.server.api.IScope)
	 */
	public void appStop(IScope app) {
		log.info("appStop:scope: " + app.getName());
		super.appStop(app);
	}

	// This function is pinging the client to see if the client is alive or not
	// If the client is not alive, it initiates the disconnection process
	// This cuts down the time for server to realize a lost client to max of the
	// timer interval (10secs)
	/**
	 * Poll clients.
	 *
	 * @param scope the scope
	 */
	private void pollClients(IScope scope) {
		log.info("video_module:pollClients");

		class PollAllClients extends TimerTask {
			private IScope room;

			public PollAllClients(IScope scope) {
				this.room = scope;
			}

			public void run() {
				log.info("video_module:PollAllClients:run");
				// IScope scope = Red5.getConnectionLocal().getScope();
//				Iterator<IConnection> iterator = scope.getConnections();
//				while (iterator.hasNext()) {
//					IConnection conn = iterator.next();
//					conn.ping();
//				}
				Collection<Set<IConnection>> clientConnections = this.room.getConnections();
				Iterator<Set<IConnection>> iterators = clientConnections.iterator();
				while (iterators.hasNext())
				{
					Set<IConnection> connections = iterators.next();
					Iterator<IConnection> iterator = connections.iterator();
					while (iterator.hasNext())
					{
						IConnection conn = iterator.next();
						conn.ping();
					}
				}
			}
		}

		Timer timer = new Timer();
		timer.schedule(new PollAllClients(scope), T_PING_INTERVAL);
		//timer object is stored so that it can be used to stop it in appStop method.
		scope.setAttribute("pollTimer", timer);
	}

	//
	// This function is called during a successful retry.
	// It takes the client name and clientid and searches the existing
	// connections
	// with the same name and different id and close them.
	// These are the previous connection which are already closed but the server
	// may not know about them yet.
	// We are expediting the close process, because some times server takes long
	// time to realize the connection is already closed
	// Because of this delay, there are several issues on the client side
	//
	/**
	 * Disconnect previous connections.
	 *
	 * @param name the name
	 * @param newClientId the new client id
	 * @param retryCounter the retry counter
	 * @param newClientIP the new client ip
	 * @param newClientHardwareAddress the new client hardware address
	 */
	public void disconnectPreviousConnections(String name, String newClientId,
			int retryCounter, String newClientIP, String newClientHardwareAddress) {
		log.info("disconnectPreviousConnections:Name:" + name + 
				": NewId:" + newClientId + ": retryCounter:" + retryCounter +
				": NewIP:" + newClientIP + "NewMacAddress:" + newClientHardwareAddress);

		Object[] clients = Red5.getConnectionLocal().getScope().getClients().toArray();
		int numOfClients = clients.length;

		log.info("video_module:disconnectPreviousConnections:Current number of clients:" + numOfClients);

		for (int i = 0; i < numOfClients; i++)
		{
			IClient currentClient = (IClient) clients[i];
			String currentUserName = null;
			if (currentClient.getAttribute("name") != null)
				currentUserName = currentClient.getAttribute("name").toString();
			String currentUserId = currentClient.getId();
			if (currentUserName != null && currentUserId != null &&
					currentUserName.equals(name)	&& !currentUserId.equals(newClientId)) 
			{
				if (retryCounter > 0) {
					currentClient.setAttribute("disconnectedDuringRetrys", true);
				}
				IConnection currentClientConn = (IConnection) currentClient.getConnections().toArray()[0];
				log.info("disconnectPreviousConnections:Calling the duplicate login call back function: "
						+ ": id:" + currentClient.getId()
						+ ": ip:" + currentClientConn.getRemoteAddress()
						+ ": name:" + currentClient.getAttribute("name")
						+ ": retryDisconnect:" + currentClient.getAttribute("disconnectedDuringRetrys"));

				//Bug 5080: Selected viewer video not visible at both sides
				//Some times,during network reconnections, due to timing issues, same node might try to reconnect after successful reconnection
				if(!currentClient.getAttribute("hardwareAddress").equals(newClientHardwareAddress))
				{
					//ServiceUtils.invokeOnClient(currentClient, getScope(), "duplicateLogin", new Object[] { newClientIP });
					((IServiceCapableConnection)currentClientConn).invoke("duplicateLogin", new Object[] { newClientIP });
				}

				// This is just to make sure that server dispatches the above
				// client call, before disconnecting the client
				log.info("disconnectPreviousConnections:Setting the 1 second interval for calling duplicateLogin function.");

				// .clients[i].intervalId =
				// setInterval(disconnectConnectionO,1000,.clients[i]);

				class DisconnectClient0 extends TimerTask {
					IClient oldClient;
					Timer timer;

					DisconnectClient0(IClient oldClient, Timer timer) {
						this.oldClient = oldClient;
						this.timer = timer;
					}

					public void run() {
						oldClient.disconnect();
						timer.cancel();
					}
				}
				Timer timer = new Timer();
				timer.schedule(new DisconnectClient0(currentClient, timer), 1000);
			}
		}
	}

	// Cleanup method, to be called at the end of close
	/**
	 * Disconnect connection.
	 */
	public void disconnectConnection() 
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("disconnectConnection Client: " + client.getId() + ", "
				+ conn.getRemoteAddress() + ", " + client.getAttribute("name")
				+ ": ");
		client.disconnect();
	}

	/**
	 * Poll.
	 */
	public void poll() {
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		// IScope scope = conn.getScope();
		// int counter = Integer.parseInt(getSharedObject(scope,
		// "clientPingLogMessageCounter").toString());
		int counter = Integer.parseInt(client.getAttribute(
				"clientPingLogMessageCounter").toString());
		counter++;
		client.setAttribute("clientPingLogMessageCounter", counter);

		// pingLogMessageCounter is set and reset in pollClients method
		if (counter >= T_POLL_TO_LOG_RATIO) {
			log.info("poll: " + client.getId() + ", " + conn.getRemoteAddress()
					+ ", " + client.getAttribute("name"));
			log.info("video_module:Polled from client. Printed only once in "
					+ T_POLL_TO_LOG_RATIO + " polls.");
			client.setAttribute("clientPingLogMessageCounter", 0);
		}
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

	// streamBroadcastStart - Notified when a broadcaster starts.
	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#streamBroadcastStart(org.red5.server.api.stream.IBroadcastStream)
	 */
	public void streamBroadcastStart(IBroadcastStream stream)
	{
		log.info("streamBroadcastStart:" + stream.getPublishedName());
	}
	

	// This will be called when the first video packet has been received.
	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#streamPublishStart(org.red5.server.api.stream.IBroadcastStream)
	 */
	public void streamPublishStart(IBroadcastStream stream) {

		super.streamPublishStart(stream);
		
		String streamName = stream.getPublishedName();
		log.info("streamPublishStart:streamName:"	+ streamName);

		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		IScope scope = conn.getScope();
		
		List<Object> sList = (List<Object>)scope.getAttribute("sList");
		log.info("streamPublishStart:clientName:" + client.getAttribute("name") +
				",Stream:" + streamName + ",IP:" + conn.getRemoteAddress() +
				",id:" + client.getId() + ",discFlag:" + client.getAttribute("disconnectedDuringRetrys"));
		
		
		log.info("streamPublishStart:StreamArray length:" + ((HashMap<String, Object>)conn.getScope().getAttribute("streamArray")).size());
		log.info("streamPublishStart:Calling stop record for the previous record for the stream.");
		
		stopRecordingAndRenamePreviousStream(streamName);
		log.info("streamPublishStart:sList.size:" + sList.size());
		for (int i=0; i<sList.size(); i++)
		{
			HashMap<String, Object> obj = (HashMap<String, Object>) sList.get(i);
			
			if(obj.get("streamName").toString().equals(streamName))
			{
				sList.remove(i);
				log.info("Removed the old stream ("+streamName+") from the list. Lenght of stream list array:" + sList.size());
				break;
			}
		}
		
		//call stratredStream function on the current client
		//ServiceUtils.invokeOnConnection("startedStream", new Object[] { stream.getPublishedName() });
		
		String clientUserName = getUserNamefromClientName(streamName);
		log.info("clientUserName:"+clientUserName);

		//IClient clientConnection = null;
		Object[] clients = scope.getClients().toArray();

		for (int i = 0; i < clients.length; i++)
		{
			IClient currentClient = (IClient) clients[i];
			String currentClientName = null;
			if (currentClient.getAttribute("name") != null)
			{
				currentClientName = currentClient.getAttribute("name").toString();
			}
			if (currentClientName != null && currentClientName.equals(clientUserName)) 
			{
				//clientConnection = currentClient;
				log.info("video_module:streamPublishStart:found clientConnection:" + currentClient + " and calling startedStream funciton on the client.");
				IServiceCapableConnection currentConnection = (IServiceCapableConnection) currentClient.getConnections().toArray()[0];
				currentConnection.invoke("startedStream", new Object[] { stream.getPublishedName()});
				log.info("streamPublishStart:Called the startedStream function on " + currentClient.getAttribute("name"));
				break;
			}
		}

//		if(clientConnection != null)
//		{
//			//ServiceUtils.invokeOnClient(clientConnection, scope, "startedStream", new Object[] { stream.getPublishedName()});
//			IServiceCapableConnection currentConnection = (IServiceCapableConnection) clientConnection.getConnections().toArray()[0];
//			//log.info("video_module:streamPublishStart:currrentConnection:" + currentConnection);
//			currentConnection.invoke("startedStream", new Object[] { stream.getPublishedName()});
//			log.info("video_module::streamPublishStart:Called the startedStream function on " + clientConnection.getAttribute("name"));
//		}
		
	    HashMap<String, Object> streamNameWithId = new HashMap<String, Object>();
	    streamNameWithId.put("streamName", streamName);
	    streamNameWithId.put("id", client.getId());  
		sList.add(streamNameWithId);
		
		scope.setAttribute("sList", sList);
	
		log.info("streamPublishStart:Added the stream (" + streamName + ") to sList. Number of streams are " + sList.size());
	}

	/**
	 * Gets the user namefrom client name.
	 *
	 * @param name the name
	 * @return the user namefrom client name
	 */
	private String getUserNamefromClientName(String name) {
		log.info("getUserNamefromClientName:name:" + name);
		int bwIndex = name.lastIndexOf(BW_SUFFIX);
		int bwDelimIndex = name.lastIndexOf("_");
		int vnIndex = name.lastIndexOf(VIEWER_SUFFIX);
		
		log.info("getUserNamefromClientName:bwIndex:" + bwIndex + ", bwDelimIndex:" + bwDelimIndex +
				",vnIndex:"+vnIndex + ",name.length:"+ name.length() + 
				",VIEWER_SUFFIX.length:"+VIEWER_SUFFIX.length());
		String clientUserName;
		// Only if the name ends with Kbps and there is a underscore
		if ((bwIndex == (name.length() - BW_SUFFIX.length()))
				&& bwDelimIndex != -1) {
			clientUserName = name.substring(0, bwDelimIndex);
		} else {
			clientUserName = name;
		}
		if((vnIndex == (name.length()-VIEWER_SUFFIX.length())) && bwDelimIndex != -1)
		{
			clientUserName =  name.substring(0,bwDelimIndex);
		}
		log.info("getUserNamefromClientName:clientUserName:" + clientUserName);
		return clientUserName;
	}

	// Notified when a broadcaster closes.
	/* (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#streamBroadcastClose(org.red5.server.api.stream.IBroadcastStream)
	 */
	public void streamBroadcastClose(IBroadcastStream stream) {
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		IScope scope = conn.getScope();
		
		List<Object> sList = (List<Object>)conn.getScope().getAttribute("sList");
		log.info("streamBroadcastClose,Stream:" + stream.getPublishedName() +
				",IP:" + conn.getRemoteAddress() + ",id:" + client.getId() +
				",discFlag:" + client.getAttribute("disconnectedDuringRetrys"));

		//String clientUserName = getUserNamefromClientName(stream.getPublishedName());
		//log.info("streamBroadcastClose:clientUserName:" + clientUserName);
		log.info("length slist:" + sList.size());
		IClient clientConnection = null;
		Object[] clients = scope.getClients().toArray();
		//We need to send the stopped stream call on the same connection
		//Same connection is identified by the id of the connection. 
		//Name is not not unique as the older connections may be still be pending with the same name.
		for (int i = 0; i < clients.length; i++) {
			IClient currentClient = (IClient) clients[i];
			String currentUserName = currentClient.getAttribute("name").toString(); 
			log.info("streamBroadcastClose:currentUserName:" + currentUserName);
			//if (currentUserName.equals(clientUserName)) {
			if (currentClient.getId().equals(client.getId())) {
				clientConnection = currentClient;
				log.info("streamBroadcastClose:found clientConnection:" + clientConnection);
				break;
			}
		}

		for (int i = 0; i < sList.size(); i++) {
			log.info("for loop slist " + sList.get(i));
			HashMap<String, Object> obj = (HashMap<String, Object>) sList.get(i);
			String streamName = obj.get("streamName").toString();
			if (streamName.equals(stream.getPublishedName())) {
				sList.remove(i);
				log.info("streamBroadcastClose:Removed the stream from the list. " + sList.size() + " streams are remaining");
				break;
			}
		}
		
		scope.setAttribute("sList", sList);
		// c_n +="Length of c"+streamConnection.length;
		if (clientConnection != null) {
			log.info("streamBroadcastClose:clientConnection.id" + clientConnection.getId());
			// clientConnection.call("stoppedStream",null,myStream.name,.clients[i].disconnectedDuringRetrys);
			//ServiceUtils.invokeOnClient(clientConnection, scope, "stoppedStream", new Object[] {stream.getPublishedName(), clientConnection.getAttribute("disconnectedDuringRetrys") });
			log.info("clientConnection.getConnections().toArray().length" + clientConnection.getConnections().toArray().length);
			((IServiceCapableConnection)clientConnection.getConnections().toArray()[0]).invoke("stoppedStream", new Object[] {stream.getPublishedName(), clientConnection.getAttribute("disconnectedDuringRetrys") });
			log.info("streamBroadcastClose:Called the stoppedStream function on client:" + clientConnection.getAttribute("name"));
			// log.info("Length of c"+streamConnection.length);
		}

		/*
		 * for(var i=0;i<.clients.length;i++) { //We need to send the stopped
		 * stream call on the same connection //Same connection is identified by
		 * the id of the connection. //Name is not not unique as the older
		 * connections may be still be pending with the same name.
		 * if(clientobj.id == .clients[i].id) {
		 * .clients[i].call("stoppedStream",
		 * null,myStream.name,.clients[i].disconnectedDuringRetrys);
		 * log.info("Called the stoppedStream function",c_n); break;
		 * 
		 * } }
		 */
	}

	/**
	 * Record stream.
	 *
	 * @param isPresenter the is presenter
	 * @param streamName the stream name
	 * @param fileName the file name
	 * @param dispName the disp name
	 * @param isF4V the is f4 v
	 */
	@SuppressWarnings("rawtypes")
	public void recordStream(String isPresenter, String streamName, String fileName, String dispName, boolean isF4V) {
		IConnection conn = Red5.getConnectionLocal();
		log.info("recordStream:isPresenter:" + isPresenter + ", streamName:" + streamName
				+ ", fileName for renaming:"+ fileName + ", dispName: " + dispName + ", isF4V:" + isF4V);
		if(conn==null)
		{
			log.info("recordStream:connection is null, returning from the function.");
			return;
		}

		IScope scope = conn.getScope();
		boolean isStreamPublishing = false;
		List sList = (List)scope.getAttribute("sList");

		for(int i=0; i < sList.size(); i++)
		{
			HashMap<String, Object> map = (HashMap<String, Object>) sList.get(i);
			String tempStreamName = map.get("streamName").toString();
			if (tempStreamName.equals(streamName))
			{
				isStreamPublishing = true;
				break;
			}
		}
		
		if(isStreamPublishing)
		{
			log.info("The stream "+ streamName + " is publishing");
			startRecrodingProcess(isPresenter, streamName, fileName, dispName, isF4V);
		}
		else //if (timeOutCount < 100)// if (!isStreamPublishing), if the stream publishing has not yet started
		{
			log.info("Waiting to publish the stream: -" + streamName);
			
			class RecordStream extends TimerTask
			{
				//JHCR: The variable name isPresenter looks like it is of type Boolean, we may rename it
				String isPresenter;
				String streamName;
				String fileName;
				String dispName;
				boolean isF4V;
				Timer timer;
				int timeOutCount = 0;

				RecordStream(Timer timer, String isPresenter, String streamName, String fileName, String dispName, boolean isF4V)
				{	
					this.timer = timer;
					this.isPresenter = isPresenter;
					this.streamName = streamName;
					this.fileName = fileName;
					this.dispName = dispName;
					this.isF4V = isF4V;
				}

				public void run()
				{
					IConnection conn = Red5.getConnectionLocal();
					IScope scope = conn.getScope();
					List<HashMap<String, Object>> sList = (List<HashMap<String, Object>>)scope.getAttribute("sList");
					boolean isStreamPublishing = false;
					
					for(int i=0; i < sList.size(); i++)
					{
						HashMap<String, Object> map = (HashMap<String, Object>) sList.get(i);
						String tempStreamName = map.get("streamName").toString();
						if (tempStreamName.equals(streamName))
						{
							isStreamPublishing = true;
						}
					}
					
					if (isStreamPublishing)
					{
						log.info("stream publishing started and cancelling timer");
						startRecrodingProcess( isPresenter, streamName, fileName, dispName, isF4V);
						timer.cancel();
					}
					else
					{
						timeOutCount++;
						log.info(timeOutCount + ":Waiting to publish the stream: -" + streamName);
					}
					
					if (timeOutCount > 100)
					{
						timer.cancel();
						log.info(timeOutCount + ":Waiting time to publish the stream expired:" + streamName);
						log.info("publish stream timer cancelled");
					}
				}
			}
			
			Timer timer = new Timer();
			//JHCR: If there some reason in choosing 500 and 100 as the firstTime and period, we may mention it here  
			timer.schedule(new RecordStream(timer, isPresenter, streamName, fileName, dispName, isF4V), 500, 100);
		}
	}
	
	/**
	 * Start recording process.
	 *
	 * @param isPresenter the is presenter
	 * @param streamName the stream name
	 * @param fileName the file name
	 * @param dispName the disp name
	 * @param isF4V the is f4 v
	 */
	//JHCR: The function name needs to be corrected, startRecordingProcess
	//JHCR: The parameter name, dispName can be more meaningful
	private void startRecrodingProcess(String isPresenter, String streamName, String fileName, String dispName, boolean isF4V)
	{
		log.info("startRecrodingProcess:isPresenter:" + isPresenter + ", streamName:" + streamName
				+ ", fileName for renaming:"+ fileName + ", dispName: " + dispName + ", isF4V:" + isF4V);
		
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		
		//JHCR: The variable name obj should be more meaningful
		HashMap<String, Object> obj = new HashMap<String, Object>();
		obj.put("fileName", fileName);
		obj.put("streamName", streamName);
		obj.put("isPresenter", isPresenter);
		obj.put("dispName", dispName);
		obj.put("status", "success");
		
		ClientBroadcastStream videoStream = (ClientBroadcastStream) getBroadcastStream(scope, streamName);
		String streamFileName = streamName;
		
		if (isF4V)
		{
			log.info("startRecrodingProcess:In F4V Stream.get:calling clearStreams for stream:" + streamName);
			streamFileName += ".f4v";
			
		}
		else
		{
			streamFileName += ".flv";
		}
		
		clearStreams(streamFileName);
		
		if(isPresenter.equals("true"))
		{
			
			scope.setAttribute("recordingPresenterFile", streamFileName);
			scope.setAttribute("presenterFilenameForRename", fileName);
			log.info(" The  recording file (for presenter)is:  "+ scope.getAttribute("recordingPresenterFile").toString());
		}
		else
		{
			scope.setAttribute("recordingViewerFile", streamFileName);
			scope.setAttribute("viewerFilenameForRename", fileName);					
			log.info("The recording file (for viewer)is:  "+ scope.getAttribute("recordingViewerFile").toString());
		}
		
		HashMap<String, Object> streamObj = new HashMap<String, Object>();
		streamObj.put("fileName", fileName);
		streamObj.put("isF4V", isF4V);
		//streamObj.put("videostream", videoStream);

		HashMap<String, Object> streamArray = (HashMap<String, Object>)scope.getAttribute("streamArray");
		streamArray.put(streamName, streamObj);
		scope.setAttribute("streamArray", streamArray);

		if (videoStream != null)
		{
			log.info("about to strat recording:streamName:" + streamName + ", videoStream:" + videoStream);
			log.info("Call record method for the stream -" + videoStream.getPublishedName());
			
			class StreamListener implements IStreamListener
			{
				@Override
				public void packetReceived(IBroadcastStream stream, IStreamPacket packet)
				{
					log.info("StreamListener:packetReceived:" + packet.getDataType());
					
				}
			}
				
			videoStream.addStreamListener(new StreamListener());
			
			if(isF4V)
			{
				log.info("recordStream:recording f4v");
				startFFmpeg(streamName);
			}
			else
			{
				try
				{
					// Save the stream to disk with streamName and append mode is false
					videoStream.saveAs(streamName, false);
					log.info("Stream -"+ videoStream.getPublishedName() +"-started recording");
				}
				catch (Exception e)
				{
					log.error("Error while saving stream: " + streamName, e);
				}
			}
			//log.info("startRecrodingProcess: calling recordingStatus on client:client.name:" + conn.getClient().getAttribute("name"));
			//((IServiceCapableConnection)conn).invoke("recordingStatus", new Object[] {obj});
		} 
		else
		{
			log.info("Failed to record the stream: -" + streamName);
			obj.put("status", "failed");
			//((IServiceCapableConnection)conn).invoke("recordingStatus", new Object[] {obj});
		}
		log.info("startRecrodingProcess: calling recordingStatus on client:client.name:" + conn.getClient().getAttribute("name"));
		((IServiceCapableConnection)conn).invoke("recordingStatus", new Object[] {obj});
	}
	
	/**
	 * Start ffmpeg.
	 *
	 * @param streamName the stream name
	 */
	private void startFFmpeg(String streamName)
	{
		log.info("startFFmpeg:streamName:" + streamName);
		IScope scope = Red5.getConnectionLocal().getScope();
		String appScopeName = ScopeUtils.findApplication(scope).getName();
		String fileName = streamName + ".f4v";
		
		log.info("path to recrod:" + String.format("%s/webapps/%s/streams/%s/%s", System.getProperty("red5.root"), appScopeName,scope.getName(), streamName));

        //String cmd = "ffmpeg -i \"rtmp://localhost/video_module/Red5_Class2_183/radha live=1\" C:/Red5/webapps/video_module/streams/Red5_Class2_183/radha.f4v";
		
		String liveStream = String.format("\"rtmp://localhost/%s/%s/%s live=1\"", appScopeName, scope.getName(), streamName);
		String outputDir = String.format("%s/webapps/%s/streams/%s", System.getProperty("red5.root"), appScopeName,scope.getName());
		
		//JHCR: The variable temp should be renamed
		File temp = new File(outputDir);
		if (!temp.exists())
		{
			log.info("the output directory does not exist, creating one...");
			log.info("output directory created successfully: " + temp.mkdirs());
		}
		
		String outputFile = String.format("%s/%s", outputDir, fileName);
		log.info("liveStream:" + liveStream);
		log.info("outputFile:" + outputFile);
		String cmd = "ffmpeg -i " + liveStream + " " + outputFile;
		log.info("cmd:" + cmd);
		
		HashMap<String, Object> ffmpegProcs;
		
		if (scope.hasAttribute("ffmpegProcs"))
		{
			ffmpegProcs = (HashMap<String, Object>) scope.getAttribute("ffmpegProcs");
		}
		else
		{
			ffmpegProcs = new HashMap<String, Object>();
		}
		
		//		if (ffmpegProcs == null)
		//		{
		//			ffmpegProcs = new HashMap<String, Object>();
		//		}
		
		//JHCR: The variable name rt can be meaningful
		Runtime rt = Runtime.getRuntime(); 
		//JHCR: The variable name proc can be meaningful
		Process proc = null;
		try
		{
			proc = rt.exec(cmd);
			String key = String.format("%s_%s", streamName, scope.getName());
			log.info("key:" + key);
			//ffmpegProcs.put(streamName, proc);
			ffmpegProcs.put(streamName, proc.getOutputStream());
			scope.setAttribute("ffmpegProcs", ffmpegProcs);
		}
		//JHCR: The parameter 'e' should be renamed
		catch(Exception e)
		{
			e.printStackTrace();	
		}
		
		if (proc != null)
		{
			FFmpegInputStreamGobbler ffmpegErrorGobbler = new FFmpegInputStreamGobbler(proc.getErrorStream(), "ERROR");             
			FFmpegInputStreamGobbler ffmpegOutputGobbler = new FFmpegInputStreamGobbler(proc.getInputStream(), "OUTPUT"); 
			ffmpegErrorGobbler.start(); 
			ffmpegOutputGobbler.start();
		}
	}
	
	/**
	 * The Class FFmpegInputStreamGobbler.
	 */
	class FFmpegInputStreamGobbler extends Thread { 
	    
		//JHCR: The variable is should be renamed
    	/** The is. */
    	InputStream is; 
    	
		//JHCR: We can make the variable name more meaningful, it does not say type of what, is it video file type?
    	/** The type. */
    	String type; 

	    /**
    	 * Instantiates a new ffmpeg input stream gobbler.
    	 *
    	 * @param is the is
    	 * @param type the type
    	 */
    	FFmpegInputStreamGobbler(InputStream is, String type) { 
	        this.is = is; 
	        this.type = type; 
	    } 

	    /* (non-Javadoc)
    	 * @see java.lang.Thread#run()
    	 */
    	public void run()
	    { 
	    	try 
	    	{ 
				InputStreamReader isr = new InputStreamReader(is); 
				BufferedReader br = new BufferedReader(isr); 
				String line=null; 
				while ( (line = br.readLine()) != null) 
				    System.out.println(type + ">" + line);     
	    	}
			catch (IOException ioe) 
			{ 
				ioe.printStackTrace();   
			} 
	    } 
	} 
	
	/**
	 * Stop ffmpeg.
	 *
	 * @param streamName the stream name
	 * @param needToClearStream the need to clear stream
	 */
	private void stopFFmpeg(String streamName, boolean needToClearStream)
	{
		log.info("stopFFmpeg:streamName:" + streamName);
		
		IScope scope = Red5.getConnectionLocal().getScope();
		HashMap<String, Object> ffmpegProcs = (HashMap<String, Object>) scope.getAttribute("ffmpegProcs");
		//Process p = (Process) ffmpegProcs.get(streamName);

		OutputStream outputStream = (OutputStream) ffmpegProcs.get(streamName);

		class StopProcess extends Thread
		{ 
		    OutputStream outputStream;
		    //String streamName;
		    //boolean needToClearStream;

		    //StopProcess(OutputStream outputStream, String streamName, boolean needToClearStream) 
		    StopProcess(OutputStream outputStream)
		    {
		        this.outputStream = outputStream; 
		        //this.needToClearStream = needToClearStream;
		        //this.streamName = streamName;
		    } 

		    public void run()
		    { 
		    	PrintStream printStream = new PrintStream(outputStream);
		        printStream.println("q");
		        printStream.flush();
		        printStream.close();
		     	//JHCR: We can delete the following block since it is not used.
				//		         if (this.needToClearStream)
				//		         {
				//		        	 //clearStreams(streamName + ".f4v");
				//		         }
		    }
		} 
		
		//StopProcess stopProcess = new StopProcess(outputStream, streamName, needToClearStream);
		StopProcess stopProcess = new StopProcess(outputStream);
		stopProcess.start();
		
		if (needToClearStream)
         {
        	 clearStreams(streamName + ".f4v");
         }
	}
	
	//JHCR: We can delete the following block since it is not used.
	// abstract method from IStreamListener
	//	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
	//		// RTMPMessage m = RTMPMessage.build((IRTMPEvent)
	//		// packet,packet.getTimestamp());
	//		// stream.pushMessage(null, m);
	//	}
	
	/**
	 * Gets the file size.
	 *
	 * @param streamName the stream name
	 * @param isF4V the is f4 v
	 * @return the file size
	 */
	public long getFileSize(String streamName, boolean isF4V)
	{
		log.info("getFileSize:streamName:" + streamName + " :isF4V: " + isF4V);
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		log.info("getFileSize:scope.getName: " + scope.getName() + ":contextPath: " + scope.getContextPath() + ":path: " + scope.getPath());
		log.info("System.getProperty: " + System.getProperty("red5.webapp.root"));
		log.info("scope.getParent().getName(): " + scope.getParent().getName());
		
		String str1 = System.getProperty("red5.webapp.root") + "/" + scope.getParent().getName();
		String str = scope.getName();
		String streamPath;
		if(isF4V)
			streamPath = str1 + "/streams/"+str+"/"+streamName+".f4v";
		else
			streamPath = str1 + "/streams/"+str+"/"+streamName+".flv";
		log.info("getFileSize:streamPath: "+streamPath);
		File file = new File(streamPath);
		log.info("getFileSize:file.getAbsolutePath: " + file.getAbsolutePath());
		log.info("getFileSize:file.exists: " + file.exists());
		log.info("The size of the recorded stream is:" + file.length());
		return file.length();

	}

	/**
	 * Flush record stream.
	 *
	 * @param streamName the stream name
	 * @param isF4V the is f4 v
	 * @return true, if successful
	 */
	public boolean flushRecordStream(String streamName, boolean isF4V)
	{
		log.info("flushRecordStream:streamName:" + streamName + " :isF4V: " + isF4V);
		
		//JHCR: We can delete the following block since it is not used.
		//		boolean msg = false;
		//		HashMap streamObj = (HashMap) streamArray.get(streamName);
		//		if(streamObj != null && streamObj.get("videostream") != null)
		//		{
		//			ClientBroadcastStream videoStream = (ClientBroadcastStream) streamObj.get("videostream");
		//			//msg = videoStream.flush();
		//		}
		//		log.info("Flush status:" + msg);
		//		return msg;
		return true;
	}

	/**
	 * Stop record stream.
	 *
	 * @param streamName the stream name
	 * @param isF4V the is f4 v
	 */
	public void stopRecordStream(String streamName, boolean isF4V)
	{
		log.info("stopRecordStream:streamName:" + streamName + ", isF4V:" + isF4V);
		IScope scope=Red5.getConnectionLocal().getScope();
		
		//JHCR: We can delete the following block since it is not used.
		//		if(isF4V)
		//		{
		//			if(scope.getAttribute("recordingPresenterFile").toString().equals(streamName+".f4v"))
		//			{
		//				scope.setAttribute("recordingPresenterFile","");
		//				scope.setAttribute("presenterFilenameForRename","");
		//			}
		//			else
		//			{
		//				scope.setAttribute("recordingViewerFile","");
		//				scope.setAttribute("viewerFilenameForRename","");
		//			}
		//		}
		//		else
		//		{
		//			if(scope.getAttribute("recordingPresenterFile").toString().equals(streamName+".flv"))
		//			{
		//				scope.setAttribute("recordingPresenterFile","");
		//				scope.setAttribute("presenterFilenameForRename","");
		//			}
		//			else
		//			{
		//				scope.setAttribute("recordingViewerFile","");
		//				scope.setAttribute("viewerFilenameForRename","");
		//			}
		//		}
		
		String recordingPresenterFile = scope.getAttribute("recordingPresenterFile").toString();
		
		if(recordingPresenterFile.equals(streamName+".f4v") || recordingPresenterFile.equals(streamName+".flv"))
		{
			log.info("stopRecordStream:previous recordingPresneterFile: " + recordingPresenterFile);
			scope.setAttribute("recordingPresenterFile","");
			scope.setAttribute("presenterFilenameForRename","");
		}
		else
		{
			log.info("stopRecordStream:previous recordingViewerFile: " + scope.getAttribute("recordingViewerFile").toString());
			scope.setAttribute("recordingViewerFile","");
			scope.setAttribute("viewerFilenameForRename","");
		}
		
		log.info("stopRecordStream:Calling Stop Record and clear stream:isF4V:" + isF4V);
		stopRecordAndClearStream(streamName, true);
	}
	
	/**
	 * Record webinar.
	 *
	 * @param streamName the stream name
	 */
	public void recordWebinar(String streamName)
	{
		log.info("recordWebinar");
		IConnection conn = Red5.getConnectionLocal();
		ClientBroadcastStream stream = (ClientBroadcastStream) getBroadcastStream(
					conn.getScope(), "mp4:" + streamName + ".f4v");
		try {
			// Save the stream to disk in append mode.
			stream.saveAs(streamName, true);
		} catch (Exception e) {
			log.error("Error while saving stream: " + streamName, e);
		}

		log.info("recordWebinar:Stream:" + stream.getName() + "-started recording");
	}
	
	/**
	 * Stop webinar recording.
	 *
	 * @param streamName the stream name
	 */
	public void stopWebinarRecording(String streamName)
	{
		log.info("stopWebinarRecording");
		IConnection conn = Red5.getConnectionLocal();
		ClientBroadcastStream stream = (ClientBroadcastStream) getBroadcastStream(
				conn.getScope(), streamName);
		stream.stopRecording();
	}
	
	/**
	 * Last call.
	 */
	public void lastCall() {
		log.info("lastCall");
	}
}