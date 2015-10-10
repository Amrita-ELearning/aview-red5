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
 * File			: VideoSharing.java
 * Module		: Collaboration - Video Sharing
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: Class for chat application
 * Dependencies: Used by collaboration_module.Application for forwarding video sharing related calls from the clients.
 *  
 */

package com.amrita.edu.red5.collaboration_module;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.red5.server.api.IClient;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.so.SharedObjectService;

/**
 * The Class VideoSharing.
 */
public class VideoSharing {
	
	/** The url info. */
	private String URL_INFO = "urlinfo";
	
	/** The video commad. */
	private String VIDEO_COMMAD = "videocommand";
	
	/** The play time. */
	private String PLAY_TIME = "playTime";

	//private static final Log log = LogFactory.getLog(Application.class);
	/** The log. */
	private static Logger log = Logger.getLogger(VideoSharing.class);
	//private Application app;
	//IScope scope;
	//ISharedObject video_so = null;
	//ISharedObject info_so = null;
	//String currentPresenterName;
	//int nextId;

	//JHCR: This method has to be removed since it is not used
	/**
	 * On video share start.
	 *
	 * @param scope the scope
	 */
	public void onVideoShareStart(IScope scope)
	{
		//this.app = app;
		//this.scope = scope;
		//SharedObjectService sos = new SharedObjectService();
		
		// Get the server shared object 'video_so'
		//video_so = sos.getSharedObject(scope, "vidSO");
		
		// Get the server shared object 'info_so'
		//info_so = sos.getSharedObject(scope, "infSO");
		
		//this.currentPresenterName = Application.currentPresenterName;
		// Initialize the unique user ID
		//nextId = 0;
	}

	/**
	 * On video share connect.
	 */
	public void onVideoShareConnect()
	{   
		IClient client = Red5.getConnectionLocal().getClient();
		log.info("onVideoShareConnect:client.name:" + client.getAttribute("name"));
		//video_so.setAttribute("newClient.name", Client.name );
		//info_so.setAttribute("uname", client.getAttribute("name"));
	}

	/**
	 * On video share disconnect.
	 *
	 * @param client the client
	 * @param currentPresenterName the current presenter name
	 */
	public void onVideoShareDisconnect(IClient client, String currentPresenterName)
	{
		log.info("onVideoShareDisconnect:client.name:" + client.getAttribute("name") + ",currentPresenterName:" + currentPresenterName);
		if( client.getAttribute("name").toString().equals(currentPresenterName))
		{
			IScope scope = Red5.getConnectionLocal().getScope();
			ISharedObject video_so = new SharedObjectService().getSharedObject(scope, "vidSO");
			if (video_so != null)
			{
				log.info("onVideoShareDisconnect:begin updating video_so:" + video_so);
				video_so.beginUpdate();
					video_so.setAttribute(VIDEO_COMMAD, null);
					video_so.setAttribute(PLAY_TIME, null);
					video_so.setAttribute(URL_INFO, null);
					video_so.setAttribute("vol", null);
				video_so.endUpdate();
				log.info("onVideoShareDisconnect:end updating video_so");
			}
		}
		//info_so.setAttribute(client.getAttribute("name").toString(), null);
	}
	
	/**
	 * Msg from prsntr video share.
	 * The client will call this  to get the server
	 * to accept the message, add the user's name to it, and
	 * send it back out to all connected clients.
	 * 
	 * @param url the url
	 * @param command the command
	 * @param playTime the play time
	 */
	public void msgFromPrsntrVideoShare(String url, String command, String playTime)
	{
		log.info("msgFromPrsntrVideoShare:url: " + url + ",command:" + command + ",playTime:" + playTime);
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject video_so = new SharedObjectService().getSharedObject(scope, "vidSO");
		video_so.beginUpdate();
			video_so.setAttribute(URL_INFO,url);
			video_so.setAttribute(VIDEO_COMMAD,command); 
			video_so.setAttribute(PLAY_TIME,playTime);        
		video_so.endUpdate();
	}
	
	/**
	 * Msg from client video share.
	 *
	 * @param syncState the sync state
	 * @param uname the uname
	 */
	public void msgFromClientVideoShare(Object syncState, String uname)
	{
		log.info("msgFromClientVideoShare:syncState:" + syncState + ",uname:" + uname);
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject info_so = new SharedObjectService().getSharedObject(scope, "infSO");
		info_so.setAttribute(uname,syncState);
	}

	/**
	 * Mute sound.
	 *
	 * @param vol the vol
	 */
	public void muteSound(String vol)
	{
		log.info("muteSound:vol:" + vol);
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject video_so = new SharedObjectService().getSharedObject(scope, "vidSO");
		video_so.setAttribute("vol",vol);
		List<String> l = new ArrayList<String>();
		l.add(vol);
		video_so.sendMessage("controlVol", l);
	}

	/**
	 * Gets the playhead time for late coming user.
	 *
	 * @param uname the uname
	 * @param currentPresenterName the current presenter name
	 * @return the playhead time for late coming user
	 */
	public void getPlayheadTimeForLateComingUser(String uname,String currentPresenterName)
	{
		
		log.info("getPlayheadTimeForLateComingUser: uname:" + uname + ",currentPresenterName: " + currentPresenterName);
		//Getting the presenter's connection
		IClient clientObj = Application.getCurrentActiveClient(currentPresenterName);
		//IClient clientObj = this.app.getCurrentActiveClient(currentPresenterName);

		if(clientObj != null)
		{
			log.info("timReqLateUser:Found presenter's connection");
			
			//clientObj.call("timReqFromSrvr", null, uname);
			log.info("getPlayheadTimeForLateComingUser:calling getPlayheadTime on presenter:" + currentPresenterName +" sending uname:" + uname);
			//IScope scope = Red5.getConnectionLocal().getScope();
			//log.info("VideoSharing:timReqLateUser:scope:"+scope);
			log.info("getPlayheadTimeForLateComingUser.clientObj.getConnections().length:" + clientObj.getConnections().toArray().length);
			//ServiceUtils.invokeOnClient(clientObj, scope, "timReqFromSrvr", new Object[] {uname});
			IServiceCapableConnection currentClientConnection = (IServiceCapableConnection)clientObj.getConnections().toArray()[0];
			currentClientConnection.invoke("getPlayheadTime", new Object[] {uname});
		}
	}

	/**
	 * Send playhead time to user.
	 *
	 * @param sliderTim the slider tim
	 * @param uname the uname
	 */
	public void sendPlayheadTimeToUser(int sliderTim, String uname)
	{
		log.info("sendPlayheadTimeToUser:uname:" + uname+": sliderTim:" + sliderTim);

		//Getting the users's connection
		IClient clientObj =  Application.getCurrentActiveClient(uname);
		//IClient clientObj =  this.app.getCurrentActiveClient(uname);

		if(clientObj != null)
		{
			log.info("sendPlayheadTimeToUser:Found user's connection:client.name:" + clientObj.getAttribute("name"));
			//clientObj.call("delayFunc", null, sliderTim, uname);
			//IScope scope = Red5.getConnectionLocal().getScope();
			//ServiceUtils.invokeOnClient(clientObj, scope, "delayFunc", new Object[] {sliderTim, uname});
			IServiceCapableConnection currentClientConnection = (IServiceCapableConnection)clientObj.getConnections().toArray()[0];
			currentClientConnection.invoke("getPlayheadTimeForLateComingUser", new Object[] {sliderTim, uname});
			log.info("sendPlayheadTimeToUser:called getPlayheadTimeForLateComingUser on user:" + uname);
		}
	}
}