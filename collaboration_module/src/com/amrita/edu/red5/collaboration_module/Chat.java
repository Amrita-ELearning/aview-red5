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
 * File			: Chat.java
 * Module		: Collaboration - Chat
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: Class for chat application
 * Dependencies: Used by collaboration_module.Application for forwarding chat related calls from the clients.
 *  
 */

package com.amrita.edu.red5.collaboration_module;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.so.SharedObjectService;

/**
 * The Class Chat.
 */
public class Chat{

	/** The log. */
	private static Logger log = Logger.getLogger(Chat.class);
	
	/** The history. */
	private String history = null;
	
	/** The scope. */
	IScope scope;
	
	/** The breakout resume time. */
	Date breakoutResumeTime = null;;
	
	/** The break session pop msg. */
	String breakSessionPopMsg = "" ;
	
	/**
	 * On chat start.
	 *
	 * @param scope the scope
	 */
	public void onChatStart(IScope scope){
		log.info("onChatStart:scope:" + scope.getName());
		this.scope = scope;
		
		// Initialize the history of the text share
		log.info("Initializing the chat text history to blank string");  
		this.history = "";
	}

	/**
	 * On chat connect.
	 *
	 * @param conn the conn
	 */
	public void onChatConnect(IConnection conn)
	{
		IClient client = conn.getClient();
		String userName = client.getAttribute("name").toString().trim();
		log.info("onChatConnect:userName:" + userName + ",history:" + history);
		// though msg history is set, verify if session on break and inform late connect(s) through popup besides chat history replay
		Date now = new Date();
		long breakResumeTime = 0;
		if (breakoutResumeTime != null)
		{
			breakResumeTime = breakoutResumeTime.getTime();
		}
		double diffBreakTimeMilliSecs = Math.ceil(breakResumeTime - now.getTime()) ;

		if (diffBreakTimeMilliSecs < 0)
		{
			breakSessionPopMsg = "" ;
		}
		if (history != "" || diffBreakTimeMilliSecs > 0)
		{
			log.info("Setting client text history for '"+ userName +"': " + history);
			((IServiceCapableConnection)conn).invoke("setChatHistory", new Object[] {history,  breakoutResumeTime, now, breakSessionPopMsg});
		}
	}

	/**
	 * On chat disconnect.
	 *
	 * @param conn the conn
	 */
	public void onChatDisconnect(IConnection conn)
	{
		IClient client = conn.getClient();
		log.info("onChatDisconnect:client.name:" + client.getAttribute("name"));
		ISharedObject chat_users_so = new SharedObjectService().getSharedObject(scope, "ChatUsers");
		chat_users_so.setAttribute(client.getAttribute("name").toString(), null);
	}

	//public void chatMessageFromClient(String msg, String fontColour, String fontFace, String fontWeight, String fontSize){
	/**
	 * Chat message from client.
	 *
	 * @param msg the msg
	 */
	public void chatMessageFromClient(String msg){
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("chatMessageFromClient.client.name:" + client.getAttribute("name") + ",msg:" + msg);
		//log.info("chatMessageFromClient:msg:" + msg + ",fontColour:" + ",fontFace:" + fontFace + ",fontWeight:" + fontWeight + ",fontSize:" + fontSize);
		String displayName = client.getAttribute("displayName").toString().trim();
		log.info("chatMessageFromClient:ip address:" + conn.getRemoteAddress() + ",displayName:" + displayName);
		// Different font colour, face, weight, size dependent on user role	start	SRS
		msg = displayName + ": " + getChatTime() + msg + "\n";
		log.info("calling chatMessageFromServer: " + msg);
		//ServiceUtils.invokeOnAllConnections("chatMessageFromServer",  new Object[]{msg});
		List<String > list = new ArrayList<String>();
		list.add(msg);
		//IScope scope = conn.getScope();
		ISharedObject chat_users_so = new SharedObjectService().getSharedObject(this.scope, "ChatUsers");
		chat_users_so.sendMessage("chatMessageFromServer", list);

	    //Clear the 'Break Session' key & markers from Chat history
		if (msg.indexOf("3 8 6TUOEMIT 777") > 0 )
		{
//			String  str = msg;
//			msg = str.substring(0,str.indexOf("7-7-7")) + "" + str.substring((str.indexOf("3 8 6TUOEMIT 777")+ 16), str.length());
			msg = msg.substring(0,msg.indexOf("7-7-7")) + "" + msg.substring((msg.indexOf("3 8 6TUOEMIT 777")+ 16), msg.length());
			breakSessionPopMsg = msg ;
		}
		
		history += msg;
		if (msg.indexOf("Clear Aum Chat Namah Area Shivaya") > 0) 
		{
			history = "";
		}
	}
	
	/**
	 * Gets the chat time.
	 *
	 * @return the chat time
	 */
	private String getChatTime()
	{
		//get date info from system
		Date mydate_ist = new Date();
		// Correct the single digit hr / min / sec to read as "0n" instead of "n"
		SimpleDateFormat dateFormat = new SimpleDateFormat("(HH:mm:ss)");
		return dateFormat.format(mydate_ist);
	}
	
	/**
	 * Gets the chat server time.
	 *
	 * @param functionToCall the function to call
	 * @return the chat server time
	 */
	public void getChatServerTime(String functionToCall) 
	{
		log.info("getChatServerTime:functionToCall: " + functionToCall);
		Date now = new Date();
		//format of date created is 'Sat Jun 15 13:38:25 IST 2013'
		//Format the date to 'Sat Jun 15 13:38:25 GMT+0530 2013' which is the format sent from FMS
		//SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT'Z yyyy");
		
		List<Object > list = new ArrayList<Object>();
		//list.add(dateFormat.format(now));
		list.add(now);
		//IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject chat_users_so = new SharedObjectService().getSharedObject(this.scope, "ChatUsers");
		chat_users_so.sendMessage(functionToCall, list);
	}
	
	/**
	 * Sets the breakout resume time.
	 *
	 * @param resumeTime the new breakout resume time
	 */
	public void setBreakoutResumeTime(Date resumeTime) 
	{
		log.info("setBreakoutResumeTime:" + resumeTime.toString());
		this.breakoutResumeTime = resumeTime;
	}
}