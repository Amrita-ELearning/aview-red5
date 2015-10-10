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
 * Module		: Collaboration 
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: Main class for collaboration in a classroom. Creates all classroom related shared objects and has methods for setting the status of a user.
 * Dependencies: This class has objects for different modules of classroom, facilitating the client interaction with those modules.
 *  
 */
package com.amrita.edu.red5.collaboration_module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.service.ServiceUtils;
import org.red5.server.api.so.ISharedObject;

/**
 * The Class Application.
 * 
 * ApplicationAdapter/MultiThreadedApplicationAdapter serves as a base class for all Red5 applications.
 * It provides methods to work with SharedObjects and streams, as well as connections and scheduling services.
 * All custom red5 applications MUST implement either ApplicationAdapter or MultiThreadedApplicationAdapter for Red5 Server to listen to the connection
 * requests from the client.
 * All the public methods added in this subclass can be called from client side with NetConnection.call method.
 */
public class Application extends MultiThreadedApplicationAdapter {
//public class Application extends ApplicationAdapter {

	/** The log. */
private static Logger log = Logger.getLogger(Application.class);
	
	/** The t ping interval. */
	private int T_PING_INTERVAL = 2000;
	
	/** The t poll to log ratio. */
	private int T_POLL_TO_LOG_RATIO = 100;

	//private String MODERATOR_ROLE = "MODERATOR";
	/** The presenter role. */
	private String PRESENTER_ROLE = "PRESENTER";
	
	/** The viewer role. */
	private String VIEWER_ROLE = "VIEWER";

	//private String TEACHER_TYPE = "TEACHER";
	/** The student type. */
	private String STUDENT_TYPE = "STUDENT";
	
	/** The guest type. */
	private String GUEST_TYPE = "GUEST";

	/** The accept status. */
	private String ACCEPT_STATUS = "accept";
	
	/** The hold status. */
	private String HOLD_STATUS = "hold";
	
	/** The waiting status. */
	private String WAITING_STATUS = "waiting";
	//private String VIEW_STATUS = "view";

	/** The freetalk ptt. */
	private String FREETALK_PTT = "freetalk";
	
	/** The un mute ptt. */
	private String UN_MUTE_PTT = "unmute";		
	
	/** The mute ptt. */
	private String MUTE_PTT = "mute";

	/*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appStart(org.red5.server.api.IScope)
	 * Called when application scope is started. 
	 */
	public boolean appStart(IScope app){
		log.info("appStart:scope:" + app.getName());
		return super.appStart(app);
	}

	/*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomStart(org.red5.server.api.IScope)
	 * 
	 * Called when room scope is started.
	 * This method will execute when first client will connect to Red5 server.
	 * Creates all shared objects, starts all the modules and initializes all the shared objects.
	 * It also initializes a thread to keep polling the clients to check if they are active.
	 */
	public boolean roomStart(IScope room)
	{
		log.info("roomStart:scope: " + room.getName());
		
		if (room.getName().equals("ConnectionTester")){
			log.info("roomStart:ConnectionTester, returning true from roomStart");
			return true;
		}
		
		room.setAttribute("guestCount", 0);
		
		ArrayList<HashMap<String, Object>> removedUsersArray = new ArrayList<HashMap<String,Object>>();
		room.setAttribute("removedUsersArray", removedUsersArray);
		
		//createAllSharedObjects(room);
		createCommonSharedObjects(room);
		createModuleSharedObjects(room);
		startAllModules(room);
		int totalSharedObjects = getSharedObjectNames(room).size();
		log.info("roomStart:total shared objects:" + totalSharedObjects);
		room.setAttribute("totalSharedObjects", totalSharedObjects);
		
		//Timer to call the ping method. This method pings the client to see if it's live
		//and if is not live, disconnects
		//setInterval(pollClients,T_PING_INTERVAL);
		log.info("calling pollClients method...");
		pollClients(room);

		return super.roomStart(room);
	}

	/*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appConnect(org.red5.server.api.IConnection, java.lang.Object[])
	 * Called every time new client connects (that is, new IConnection object is created after call from a SWF movie) to the application.
	 * Override this method to pass additional data from client to server application using NetConnection.connect method.
	 */
	public boolean appConnect(IConnection conn, Object[] params){
		log.info("appConnect");
		IClient client = conn.getClient();
		log.info("appConnect:Client id: " + client.getId());
		return super.appConnect(conn, params);
	}
	

	/**
	 * This method will execute every time when a client will connect to Red5 server.
	 *
	 * @param conn the conn
	 * @param params the params
	 * @return true, if successful
	 */
	/*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomConnect(org.red5.server.api.IConnection, java.lang.Object[])
	 * This method is executed every time when a client connects to room.
	 * Override this method to pass additional data from client to server application using NetConnection.connect method.
	 * Functionality of this method:
	 * 1. checks if the max. students number is exceeded. If so, the connection is rejected.
	 * 2. checks if the connection is coming from an already connected user.
	 * 		If so,
	 * 			a. if the connection request is coming from a different computer, it is treated as a duplicate login,
	 * 				kills the earlier connection and establishes connection with the new client.
	 * 			b. if the connection is coming from the same computer, it treats it as attempt to reconnect after a lost connection.
	 * 3. checks if the user is removed from the class, if so, call a method on the client to indicate the same.
	 * Then it connects to all the modules.
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean roomConnect(IConnection conn, Object params[]){
		if(super.roomConnect(conn, params) == false){
			return false;
		}
		
		IScope scope = conn.getScope();
		log.info("roomConnect:scope: " + scope.getName());
		
		if (scope.getName().equals("ConnectionTester")){
			log.info("roomConnect:ConnectionTester, returning true from roomConnect");
			return true;
		}
		Set<String> sharedObjects = getSharedObjectNames(scope);
		log.debug("roomConnect:sharedObjects:" + sharedObjects);
		int numSharedObjects = sharedObjects.size();
		log.debug("roomConnect:sharedObjects.size:" + numSharedObjects);
		int totalSharedObjects = Integer.parseInt(scope.getAttribute("totalSharedObjects").toString());
		log.debug("roomConnect:totalSharedObjects.size:" + totalSharedObjects);
		log.debug("roomConnect:params.length " + params.length);
		
		/*
		 * In red5, the shared object gets destroyed when the last user gets disconnected.
		 * One special case in AVIEW Classroom is when admin user is logged in and all others log out.
		 * In this case, the application is still loaded but the module specific shared objects are destroyed
		 * since admin user is not connected to those shared objects.
		 * When another user tries to log in and connect to the shared object, 
		 * red5 automatically creates the SO when red5 recognizes that the SO a client connects does not exist.
		 * This client-side initiated SO was not calling sync method when SO is update.
		 * Sometimes, the SO itself is not returned by getSharedObject method.
		 * To overcome these problems, create the shared objects in the roomConnect method.
		 * Since the admin user connects to a few SOs, the total number of SOs will differ when another user logs in.
		 */
		if (numSharedObjects < totalSharedObjects)
		{
			log.info("roomConnect:creating module shared objects and starting all modules.");
			createModuleSharedObjects(scope);
			startAllModules(scope);
		}
		/*
		 * retrieve all the params passed to this method into corresponding variables.
		 */
		String name = params[0].toString();
		log.info("roomConnect:username: " + name);
		int retryCounter = -1;
		HashMap userDetails = null;
		String userType = null;
		int maxStudent = 0;
		String hardwareAddress = null;
		int lectureID = -1;

		retryCounter = Integer.parseInt(params[1].toString());
		userDetails = (HashMap) params[2];
		userType = params[3].toString();
		maxStudent  = Integer.parseInt(params[4].toString());
		hardwareAddress = params[5].toString();
		lectureID  = Integer.parseInt(params[6].toString());
		scope.setAttribute("lectureID", lectureID);
		
		//For checking maximum number of users
		int studentNumber = scope.getClients().size();
		log.info("Current Number of viewers: " + studentNumber + " Maximum viewers in the class: " + maxStudent);
		
		//If maxStudent is either set to -1 or 0, then it means unlimited students are allowed
		if(maxStudent > 0){
			if( (userType.equals(STUDENT_TYPE) || userType.equals(GUEST_TYPE))   && studentNumber >= maxStudent)
			{
				log.info("Rejecting the current viewer as max number of viewers are exceeding. studentNumber: " + studentNumber + " maxstudent:" + maxStudent);
				rejectClient();
				return false;
			}
		}

		IClient client = conn.getClient();
		client.setAttribute("hardwareAddress", hardwareAddress);
		client.setAttribute("userType", userType);

		//if(name != null && retryCounter != null)
		if(name != null)
		{
			log.info("calling disconnectPreviousConnects...");
			//Explicitly close the previous connections on the same user name
			//So that in case duplicate login we want to close the earlier login with a message
			disconnectPreviousConnections(name, client.getId(), retryCounter, conn.getRemoteAddress(), client.getAttribute("hardwareAddress").toString());
		}
		
		log.info("rommConnect:client.name: " + client.getAttribute("name"));
		client.setAttribute("name", name);
		
		if (userDetails != null && userDetails.containsKey("displayName"))  //userDetails.get("displayName") != null 
		{
			client.setAttribute("displayName", userDetails.get("displayName"));
			log.info("rommConnect:client.displayName: " + client.getAttribute("displayName"));
		}
		
		//Checking for removed user
		ArrayList<HashMap<String, Object>> removedUsersArray = (ArrayList<HashMap<String, Object>>) scope.getAttribute("removedUsersArray");
		for(int i=0; i < removedUsersArray.size(); i++)
		{
			HashMap<String, Object> map = (HashMap<String, Object>) removedUsersArray.get(i);
		 	if(map.get("userName").toString().equals(name) && Integer.parseInt(map.get("lectureID").toString()) == lectureID)
			{
		 		log.info("rommConnect:calling alreadyRemovedUser on client:" + name);
		 		//client.call("alreadyRemovedUser",null,name);
		 		((IServiceCapableConnection)conn).invoke("alreadyRemovedUser", new Object[] {name});
		 		break;
		 		//return true;
			}
		}
				
		//if(client.getAttribute("userType").toString().equals(GUEST_TYPE))
		if(userType.equals(GUEST_TYPE))
		{
			int guestCount = Integer.parseInt(scope.getAttribute("guestCount").toString());
			guestCount++;
			log.info("Guest User connected to server. New Guest Count: " + guestCount);
			scope.setAttribute("guestCount", guestCount);
		}
		
		//This flag denotes whether this connection is closed because of retrys..
		//This is a initialization
		client.setAttribute("disconnectedDuringRetrys", false);
		//This counter is used to log the client poll message only once for T_POLL_TO_LOG_RATIO times
		client.setAttribute("clientPingLogMessageCounter", 0);

		//setting adminConsole shared object
		log.info("roomConnect:setting adminSharedObject for " + name + " with values " + userDetails);
		//if (userDetails != null){
		ISharedObject adminSharedObject = getSharedObject(scope, "adminSharedObj");
		adminSharedObject.lock();
		adminSharedObject.setAttribute(name, userDetails);
		adminSharedObject.unlock();
		//}

		log.info("calling onChatConnect");
		((Chat)scope.getAttribute("chat")).onChatConnect(conn);
		
		log.info("Calling on2DConnect");
		TwoDSharing twoDSharing = (TwoDSharing)scope.getAttribute("twoDSharing");
		twoDSharing.on2DSharingConnect();

		log.info("Calling on3DConnect");
		//((ThreeDSharing)scope.getAttribute("threeDSharing")).on3DConnect(client);
		((ThreeDSharing)scope.getAttribute("threeDSharing")).on3DConnect();
		
		log.info("Calling onVideoShareConnect");
		//((VideoSharing)scope.getAttribute("videoSharing")).onVideoShareConnect(client);
		((VideoSharing)scope.getAttribute("videoSharing")).onVideoShareConnect();

		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomDisconnect(org.red5.server.api.IConnection)
	 * Called every time client disconnects from the room. 
	 */
	public void roomDisconnect(IConnection conn){
		log.info( "roomDisconnect:client.id:" + conn.getClient().getId() + ",client.name:" + conn.getClient().getAttribute("name"));
		super.roomDisconnect(conn);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appDisconnect(org.red5.server.api.IConnection)
	 * Called every time client disconnects from the application.
	 */
	@SuppressWarnings("unchecked")
	public void appDisconnect(IConnection conn)
	{
		super.appDisconnect(conn);

		IClient client = conn.getClient();
		IScope scope = conn.getScope();
		
		log.info("appDisconnect:client.id:" + client.getId()+",client.name:" + client.getAttribute("name") + ",client.ip:" + conn.getRemoteAddress());

		if (scope.getName().equals("ConnectionTester")){
			log.info("appDisconnect: disconnecting ConnectionTester");
			return;
		}
		
		//String currentClientId = client.getId();
		String name = client.getAttribute("name").toString();
		String currentClientId = getCurrentActiveClientId(name);
		
		log.info("appDisconnect:current Id for this user '" + name + "' is:" + currentClientId);

		if(!currentClientId.equals("") && !currentClientId.equals(client.getId()))
		{
			log.info("appDisconnect:User already connected with a new client id :"+currentClientId+". Hence not cleaning the shared objects");
			Object[] clients = scope.getClients().toArray();
			int numClients = clients.length;
			for (int i = 0; i < numClients; i++)
			{
				IClient currentClient = (IClient)clients[i];
				log.info("appDisconnect:calling checkForRecording on user:" + currentClient.getAttribute("name"));
				//ServiceUtils.invokeOnClient(currentClient, scope, "checkForRecording", new Object[] {name});
				IServiceCapableConnection currentConn = (IServiceCapableConnection) currentClient.getConnections().toArray()[0];
				currentConn.invoke("checkForRecording", new Object[] {name});
			}
			return;
		}
		
		log.info("appDisconnect:cleaning the shared objects for user with id: " + currentClientId);
		
		String currentSelectedViewerName = null;
		if(scope.hasAttribute("currentSelectedViewerName"))
		{
			currentSelectedViewerName = scope.getAttribute("currentSelectedViewerName").toString();
		}
		
		if(currentSelectedViewerName!= null && currentSelectedViewerName.equals(name))
		{
			currentSelectedViewerName = null;
			log.info("appDisconnect:Resetting the currentSelectedViewerName value to null");
		}

		ISharedObject users_so = getSharedObject(scope, "users_so1");
		
		String currentPresenterName = null;
		if(scope.hasAttribute("currentPresenterName"))
		{
			 currentPresenterName=scope.getAttribute("currentPresenterName").toString();
		}
		//for the last user disconnect, users_so is null.
		if (users_so != null)
		{
			log.info("appDisconnect:Locked the users_so shared object:" + users_so);
			users_so.beginUpdate();
			
			HashMap<String, Object> userProperty = (HashMap<String, Object>) users_so.getAttribute(name);
			log.info("appDisconnect:userProperty:" + userProperty);
			log.info("appDisconnect:userProperty.isModerator:" + (Boolean)userProperty.get("isModerator"));
			if(userProperty != null && ((Boolean)userProperty.get("isModerator")).booleanValue() == true)
			{
				log.info("appDisconnect:Moderator is disconnecting, setting the moderatorName to null");
				scope.setAttribute("moderatorName",null);
			}
	
			if(userProperty != null && userProperty.get("userRole").toString().equals(PRESENTER_ROLE) )
			{
				if (userProperty.get("userRole").toString().equals(PRESENTER_ROLE))
				{
					log.info("appDisconnect:clearing PRESENTER properties");
					//if(((Boolean)userProperty.get("isModerator")).booleanValue() == false)
					if(!(Boolean)userProperty.get("isModerator"))
					{
						String moderatorName=scope.getAttribute("moderatorName").toString();
						log.info("appDisconnect:Current Moderator Name:" + moderatorName);
						HashMap<String, Object> moderatorUserProperty = (HashMap<String, Object>)users_so.getAttribute(moderatorName);
						if(moderatorUserProperty != null)
						{
							moderatorUserProperty.put("userRole", PRESENTER_ROLE);
							//if((Boolean)moderatorUserProperty.get("isVideoPublishing") == true)
							if((Boolean)moderatorUserProperty.get("isVideoPublishing"))
							{
								moderatorUserProperty.put("userStatus", ACCEPT_STATUS);
								log.info("appDisconnect:Accept Block:User status of '"+moderatorName+"' is '"+moderatorUserProperty.get("userStatus")+"'");
							}
							else
							{
								moderatorUserProperty.put("userStatus", HOLD_STATUS);
								log.info("appDisconnect:Hold Block:User status of '"+moderatorName+"' is '"+moderatorUserProperty.get("userStatus")+"'");
							}
							//currentPresenterName = moderatorName;
							scope.setAttribute("currentPresenterName", moderatorName);
							ISharedObject audioMuteSharedObject = getSharedObject(scope, "audioMuteSharedObject1");
							String pttValue = audioMuteSharedObject.getAttribute(PRESENTER_ROLE).toString();
							if(pttValue.equals(name))
							{
								audioMuteSharedObject.setAttribute(PRESENTER_ROLE,moderatorName);
							}
							users_so.setAttribute(moderatorName, moderatorUserProperty);		
							log.info("appDisconnect:Setting userRole as PRESENTER for Moderator:  "+ moderatorName);
						}
						else
						{
							//currentPresenterName = null;
							scope.setAttribute("currentPresenterName", null);
							log.info("appDisconnect:Setting the currentPresenter Name to: NULL ");
						}
					}
					else
					{
						//the disconnecting user is presenter as well as moderator
						scope.setAttribute("currentPresenterName", null);
						log.info("appDisconnect:Setting the currentPresenter Name to: NULL as the moderator who is also the presenter is disconneted");
					}
				}
				log.info("appDisconnect:Role of the user who is getting disconnected is:"+userProperty.get("userRole"));
			}
			else
			{
				log.info("appDisconnect:User does not have any property in the users_so shared object");
			}
	
			log.info("appDisconnect:Setting the " + client.getAttribute("name") + " property value to null in users_so shared object");
			users_so.setAttribute(name, null);
	
			//users_so.flush();
			users_so.endUpdate();
			log.info("appDisconnect:Unlocked users_so shared object");
		}

		ISharedObject adminSharedObject = getSharedObject(scope, "adminSharedObj");
		if (adminSharedObject != null)
		{
			log.info("appDisconnect:setting adminSharedObject for " + name + " to null");
			adminSharedObject.setAttribute(name, null);
		//adminSharedObject.flush();
		}

		log.info("appDisconnect:Calling onChatDisconnect()");
		Chat chat=(Chat)scope.getAttribute("chat");
		chat.onChatDisconnect(conn);

		log.info("appDisconnect:Calling on3DDisconnect()");
		((ThreeDSharing)scope.getAttribute("threeDSharing")).on3DDisconnect(client);
		
		log.info("appDisconnect:Calling on2DDisconnect()");
		TwoDSharing twoDSharing=(TwoDSharing)scope.getAttribute("twoDSharing");
		twoDSharing.on2DDisconnect();
		
		log.info("appDisconnect:Calling onVideoShareDisconnect()");
		((VideoSharing)scope.getAttribute("videoSharing")).onVideoShareDisconnect(client,currentPresenterName);
		
		if(client.getAttribute("userType").toString().equals(GUEST_TYPE))
		{
			//guestCount--;
			//IScope scope = conn.getScope();
			int guestCount = Integer.parseInt(scope.getAttribute("guestCount").toString());
			guestCount--;
			log.info("Guest User disconnected from server. New Guest Count: " + guestCount);
			scope.setAttribute("guestCount", guestCount);;
		}
		log.info("appDisconnect:Exited Function");
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#roomStop(org.red5.server.api.IScope)
	 * Called when room scope is stopped.
	 */
	public void roomStop(IScope room)
	{
		log.info("roomStop:scope: " + room.getName());
		
		Timer timer = (Timer) room.getAttribute("pollTimer");
		if (timer != null)
		{
			log.info("roomStop:cancelling poll timer");
			timer.cancel();
		}
		
		super.roomStop(room);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.MultiThreadedApplicationAdapter#appStop(org.red5.server.api.IScope)
	 * Called when application is stopped. 
	 */
	public void appStop(IScope app)
	{
		log.info("appStop:scope: " + app.getName());
		super.appStop(app);
	}
	
	/**
	 * Creates the common shared objects.
	 * This method is used to create common shared objects.
	 * 
	 * @param room the room
	 */
	private void createCommonSharedObjects(IScope room){
		log.info("createCommonSharedObjects:scope: " + room.getName());
		
		createSharedObject(room, "users_so1", false);
		createSharedObject(room, "adminSharedObj", false);
		createSharedObject(room, "audioMuteSharedObject1", false);
		ISharedObject audioMuteSharedObject = getSharedObject(room, "audioMuteSharedObject1");
		audioMuteSharedObject.setAttribute(PRESENTER_ROLE, FREETALK_PTT);
		// Create the server shared object 'privateChatSharedObject'
		createSharedObject(room, "privateChatSharedObj", false);
		// Create the server shared object 'settingMUI'
		createSharedObject(room, "muiSharedObj", false);
		// Create the server shared object 'privateChatSharedObject'
		createSharedObject(room, "recordSharedObj", false);
	}
	
	/**
	 * Creates the module shared objects.
	 * This method is used to create module level shared objects.
	 * 
	 * @param room the room
	 */
	private void createModuleSharedObjects(IScope room){
		log.info("createModuleSharedObjects:scope: " + room.getName());
		
		createSharedObject(room, "selectedModule", false);
		
		createSharedObject(room, "desktopSharingSharedObject1", false);
		ISharedObject desktopSharingSharedObject = getSharedObject(room, "desktopSharingSharedObject1");
		desktopSharingSharedObject.setAttribute("desktopSharing","default");
		
		createSharedObject(room, "questionSharedObj", false);
		
		createSharedObject(room, "videoWallSharedObject", false);
		
		//shared object for chat module
		createSharedObject(room, "ChatUsers", false);
		
		//document sharing related shared objects
		createSharedObject(room, "document_so", false);
		
		//whiteboard related shared objects
		createSharedObject(room, "whiteboardShapesSO", false);
		createSharedObject(room, "navigationSO", false);
		createSharedObject(room, "collaborationModeSO", false);
		createSharedObject(room, "whiteboardPointerSO", false);
		createSharedObject(room, "ShapeIdSO", false);
		
		//shared objects related video sharing
		createSharedObject(room, "vidSO", false);
		createSharedObject(room, "infSO", false);
		
		//shared object for 3DSharing
		createSharedObject(room, "threeD_so", false);
		
		//createSharedObject(room, "twoD_so", false);
		/*
		SHARED  OBJECTS FOR 2D VIEWER
		1,globalUpdator-FOR LATE USER AND PLAY BACK SYNC
		2,playBackUpdator-PLAY BACK CONTROL
		3,globalData-ALL GLOBAL DATAS ARE SORED IN THIS
		4,globalDownloader-Movie Dowloading through this

		*/
		createSharedObject(room, "globalUpdator", false);
		createSharedObject(room, "playBackUpdator", false);
		createSharedObject(room, "globalData", false);
		createSharedObject(room, "globalDownloader",false);
	}

	/**
	 * Start all modules.
	 * This method is used to start all modules, to set the shared objects of every module with initial values.
	 * 
	 * @param room the room
	 */
	private void startAllModules(IScope room){
		log.info("startAllModules:scope:" + room.getName());
		
		log.info("calling onChatAppStart");
		Chat chat = new Chat();
		chat.onChatStart(room);
		room.setAttribute("chat", chat);
		
		log.info("Calling onDocumentSharingStart()");
		DocumentSharing  docSharing = new DocumentSharing();
		docSharing.onDocumentSharingStart(room);
		room.setAttribute("docSharing", docSharing);
		
		log.info("Calling onWhiteboardStart()");
		Whiteboard whiteboard = new Whiteboard();
		whiteboard.onWhiteboardStart(room);
		room.setAttribute("whiteboard", whiteboard);

		log.info("Calling onVideoSharingStart");
		VideoSharing videoSharing = new VideoSharing();
		//videoSharing.onVideoShareStart(room, this);
		videoSharing.onVideoShareStart(room);
		room.setAttribute("videoSharing", videoSharing);

		// Get the server shared object 'threeD_so'
		//ISharedObject threeDSO = getSharedObject(room, "threeD_so");
		log.info("Calling on3DSharingStart");
		ThreeDSharing threeDSharing = new ThreeDSharing();
		threeDSharing.on3DShareStart(room);
		room.setAttribute("threeDSharing", threeDSharing);
		
		//ISharedObject twoDSO = getSharedObject(room, "twoD_so");
		log.info("Calling on2DSharingStart");
		TwoDSharing twoDSharing = new TwoDSharing();
		//twoDSharing.on2DSharingStart(twoDSO);
		room.setAttribute("twoDSharing", twoDSharing);
		twoDSharing.on2DSharingStart(room);
	}
	
	/**
	 * Disconnect connection.
	 * Cleanup method, to be called at the end of application close
	 */
	public void disconnectConnection()
	{
		log.info("disconnectConnection");
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("disconnectConnection:client: " + client);
		log.info("disconnectConnection:client.id:" + client.getId() + ",client.ip:" + conn.getRemoteAddress() + ",client.name:" + client.getAttribute("name") + ": ");
		//onDisconnect();
		client.disconnect();
	}

	//
	/**
	 * Disconnect previous connections.
	 * This function is called during a successful retry. 
	 * It takes the client name and clientid and searches the existing connections 
	 * with the same name and different id and close them. 
	 * These are the previous connection which are already closed but the server may not know about them yet.
	 * We are expediting the close process, because some times server takes long time to realize the connection is already closed
	 * Because of this delay, there are several issues on the client side
	 * 
	 * @param name the name
	 * @param newClientId the new client id
	 * @param retryCounter the retry counter
	 * @param newClientIP the new client ip
	 * @param newClientHardwareAddress the new client hardware address
	 */
	@SuppressWarnings("unchecked")
	public void disconnectPreviousConnections(String name, String newClientId, int retryCounter, String newClientIP, String newClientHardwareAddress)
	{
		IScope scope = Red5.getConnectionLocal().getScope();
		log.info("disconnectPreviousConnections:Name:" + name + ":NewId:" + newClientId + ":retryCounter:" + retryCounter + ":NewIP:" + newClientIP +
				":NewMacAddress:"+newClientHardwareAddress);
		//Set<IClient> clients = Red5.getConnectionLocal().getScope().getClients();
		//List<Object> clients = new ArrayList<Object>(Red5.getConnectionLocal().getScope().getClients());
		Object[] clients = scope.getClients().toArray();
		int numClients = clients.length;
		log.info("disconnectPreviousConnections:Current number of clients:" +  numClients);

		for (int i = 0; i < numClients; i++)
		{
			//			ObjectMap<String,Object> temp =(ObjectMap<String,Object>)clients.get(i);
			//			String currentMember = temp.get("name").toString().trim();
			IClient currentClient = (IClient)clients[i];
			if(currentClient.getAttribute("name").toString().equals(name) && !currentClient.getId().equals(newClientId))
			{
				if(retryCounter > 0)
				{
					//application.clients[i].disconnectedDuringRetrys = true;
					currentClient.setAttribute("disconnectedDuringRetrys", true);
				}
				//Bug 5080: Selected viewer video not visible at both sides
				//Some times,during network reconnections, due to timing issues, same node might try to reconnect after successfull reconnection
				//In the at case we will close the client connection silently

				IConnection currentClientConn = (IConnection) currentClient.getConnections().toArray()[0];
				log.info("disconnectPreviousConnections:Calling the duplicate login call back function: "
						+ ": id:" + currentClient.getId()
						+ ": ip:" +  currentClientConn.getRemoteAddress()
						+ ": name:" + currentClient.getAttribute("name")
						+ ": retryDisconnect:" + currentClient.getAttribute("disconnectedDuringRetrys"));

				ISharedObject users_so = getSharedObject(scope, "users_so1");
				HashMap<String, Object> userProperty = (HashMap<String, Object>) users_so.getAttribute(name);
				if(userProperty != null)
				{
					if(!currentClient.getAttribute("hardwareAddress").equals(newClientHardwareAddress) || 
							userProperty.get("avcRuntime").toString().equals("BROWSER"))
					{
						ServiceUtils.invokeOnClient(currentClient, getScope(), "duplicateLogin", new Object[] {newClientIP});
						//((IServiceCapableConnection)currentClientConn).invoke("duplicateLogin", new Object[] {newClientIP});
					}
				}
				
				//This is just to make sure that server dispatches the above client call, before disconnecting the client
				log.info("disconnectPreviousConnections:Setting the 1 second interval to call the duplicateLogin function");

				//application.clients[i].intervalId = setInterval(disconnectConnectionO,1000,application.clients[i]);
				class DisconnectClient0 extends TimerTask{
					IClient oldClient;
					Timer timer;
					DisconnectClient0(IClient oldClient, Timer timer){
						this.oldClient = oldClient;
						this.timer = timer;
					}
					public void run(){
						oldClient.disconnect();
						timer.cancel();
					}
				}
				Timer timer = new Timer();
				timer.schedule(new DisconnectClient0(currentClient, timer), 1000);
			}//end of if-condition
		}//end of for-loop
	}//end of disconnectPreviousConnections()
	

	/**
	 * Initiate recordof moderator.
	 *
	 * @param status the status
	 * @param name the name
	 */
	public void initiateRecordofModerator(String status, String name)
	{
		log.info("initiateRecordofModerator:status: " + status + ", name: " + name);
		IConnection conn = Red5.getConnectionLocal();
		Object[] clients = conn.getScope().getClients().toArray();
		for(int i=0; i< clients.length; i++)
		{
			IClient currentClient = (IClient)clients[i];
			if(currentClient.getAttribute("name").toString().equals(name))
			{
				IServiceCapableConnection currClientConn = (IServiceCapableConnection) currentClient.getConnections().toArray()[0];
				log.info("calling setRecordStatusModerator on client: " + currentClient.getAttribute("name"));
				currClientConn.invoke("setRecordStatusModerator", new Object[] {status});
			}
		}
	}
	
	/**
	 * Sets the selected module.
	 *
	 * @param index the new selected module
	 */
	public void setSelectedModule(int index) {

		//application.questionSharedObject.setProperty(question,questionSOValue);
		IScope scope = Red5.getConnectionLocal().getScope();
		//IScope scope =conn.getScope();
		ISharedObject selectedModule_so = getSharedObject(scope, "selectedModule");
		selectedModule_so.setAttribute("val", index);
	}
	
	/**
	 * Sets the private chat shared object.
	 *
	 * @param chatId the chat id
	 * @param chatDetails the chat details
	 */
	public void setPrivateChatSharedObject(String chatId, Object chatDetails)
	{
		log.info("setPrivateChatSharedObject:chatId: " + chatId);
		//SharedObjectService sos = new SharedObjectService();
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject privateChatSharedObject = getSharedObject(scope, "privateChatSharedObj");
		privateChatSharedObject.beginUpdate();
		privateChatSharedObject.setAttribute(chatId, "null");
		privateChatSharedObject.setAttribute(chatId, chatDetails);
		//privateChatSharedObject.flush();
		privateChatSharedObject.endUpdate();
	}
	
	/**
	 * Sets the mui shared object.
	 *
	 * @param property the property
	 * @param value the value
	 */
	public void setMUISharedObject(String property, Object value)
	{
		//SharedObjectService sos = new SharedObjectService();
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject muiSharedObject = getSharedObject(scope, "muiSharedObj");
		muiSharedObject.beginUpdate();
		muiSharedObject.setAttribute(property, "null");
		muiSharedObject.setAttribute(property, value);
		//muiSharedObject.flush();
		muiSharedObject.endUpdate();
	}
	
	/**
	 * Sets the selected video in video wall.
	 *
	 * @param userName the user name
	 * @param streamName the stream name
	 */
	public void setSelectedVideoInVideoWall(String userName, String streamName)
	{
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject videoWallSharedObject = getSharedObject(scope, "videoWallSharedObject");
		videoWallSharedObject.beginUpdate();
		videoWallSharedObject.setAttribute("selectedUser",userName);
		videoWallSharedObject.setAttribute("selectedStreamName",streamName);
		//videoWallSharedObject.flush();
		videoWallSharedObject.endUpdate();
	}
	
	/**
	 * Toggle video wall selection.
	 *
	 * @param isSelected the is selected
	 */
	public void toggleVideoWallSelection(Boolean isSelected)
	{
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject videoWallSharedObject = getSharedObject(scope, "videoWallSharedObject");
		videoWallSharedObject.beginUpdate();
		videoWallSharedObject.setAttribute("isSelected",isSelected);
		//videoWallSharedObject.flush();
		videoWallSharedObject.endUpdate();
	}
	
	/**
	 * Initiate recording by admin.
	 *
	 * @param label the label
	 * @param status the status
	 */
	public void initiateRecordingByAdmin(String label,Object status)
	{
		//SharedObjectService sos = new SharedObjectService();
		IScope scope=Red5.getConnectionLocal().getScope();
		ISharedObject recordingSharedObject = getSharedObject(scope, "ChatUsers");
		recordingSharedObject.beginUpdate();
		recordingSharedObject.setAttribute(label, status);
		//recordingSharedObject.flush();
		recordingSharedObject.endUpdate();
	}
	
	/**
	 * Sets the question shared object.
	 *
	 * @param question the question
	 * @param questionSOValue the question so value
	 */
	public void setQuestionSharedObject(String question, Object questionSOValue) {
		log.info("setQuestionSharedObject:question:" + question + ",questionSOValue:" + questionSOValue);
		//application.questionSharedObject.setProperty(question,questionSOValue);
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject questionSharedObject = getSharedObject(scope, "questionSharedObj");
		questionSharedObject.setAttribute(question, questionSOValue);
	}

	/**
	 * Restrict user.
	 * restrictUser method,to be called when admin removes a user.
	 * 
	 * @param uname the uname
	 */
	@SuppressWarnings("unchecked")
	public void restrictUser(String uname)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("restrictUser Client: " + client.getId() + ", " + conn.getRemoteAddress() +", " + client.getAttribute("name")+":");
		Object[] clients = conn.getScope().getClients().toArray();
		for(int i=0; i< clients.length; i++)
		{
			IClient currentClient = (IClient)clients[i];
			if(currentClient.getAttribute("name").toString().equals(uname))
			{
				log.info("calling removeUser on client: " + currentClient.getAttribute("name"));
				ArrayList<HashMap<String, Object>> removedUsersArray = (ArrayList<HashMap<String, Object>>) scope.getAttribute("removedUsersArray");
				int lectureID = Integer.parseInt(scope.getAttribute("lectureID").toString());
				HashMap<String, Object> obj = new HashMap<String, Object>();
				obj.put("userName", uname);
				obj.put("lectureID", lectureID);
				removedUsersArray.add(obj);
				scope.setAttribute("removedUsersArray", removedUsersArray);
				log.info("calling removeUser on client: " + currentClient.getAttribute("name"));
				IServiceCapableConnection currConn = (IServiceCapableConnection) currentClient.getConnections().toArray()[0];
				currConn.invoke("removeUser", new Object[] {currentClient.getAttribute("name")});
			}
		}
	}
	
	/**
	 * Msg from client.
	 * msgFromClient method,to be called when admin sends a message
	 * 
	 * @param msg the msg
	 * @param uname the uname
	 */
	public void msgFromClient(String msg, String uname)
	{	
		log.info("msgFromClient:msg:" + msg + ",uname:" + uname);
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		//receiving msg from client
		msg = client.getAttribute("name") + ": " + msg + "\n";
		
		Object[] clients = conn.getScope().getClients().toArray();
		for(int j =0; j < clients.length; j++)
		{
			IClient currentClient = (IClient)clients[j];
			if(currentClient.getAttribute("name").toString().equals(uname))
			{
				log.info("msgFromClient:sending message to client: " + client.getAttribute("name"));
				//ServiceUtils.invokeOnClient(currentClient, getScope(), "msgFromSrvr", new Object[] {msg, currentClient.getAttribute("name")});
				IServiceCapableConnection currConn = (IServiceCapableConnection) currentClient.getConnections().toArray()[0];
				currConn.invoke("msgFromSrvr", new Object[] {msg, currentClient.getAttribute("name")});
			}
		}
	}

	/**
	 * Poll clients.
	 * This function is pinging the client to see if the client is alive or not
	 * If the client is not alive, it initiates the disconnection process
	 * This cuts down the time for server to realize a lost client to max of the timer interval (10secs)
	 * @param scope the scope
	 */
	private void pollClients(IScope scope){
		log.info("pollClients");
		
		class PollAllClients extends TimerTask{
			IScope scope;
			public PollAllClients(IScope scope){
				this.scope = scope;
			}
			public void run(){
				log.info("PollAllClients:run");
				Collection<Set<IConnection>> clientConnections = scope.getConnections();
				Iterator<Set<IConnection>> iterators = clientConnections.iterator();
				while (iterators.hasNext())
				{
					Set<IConnection> connections = iterators.next();
					Iterator<IConnection> iterator = connections.iterator();
					log.info("PollAllClients:run:connections.size:" + connections.size());
					while (iterator.hasNext())
					{
						IConnection conn = iterator.next();
						log.info("PollAllClients:run:conn:" + conn);
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

	/**
	 * Poll.
	 */
	public void poll()
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		//IScope scope = conn.getScope();
		//int counter = Integer.parseInt(getSharedObject(scope, "clientPingLogMessageCounter").toString());
		int counter = Integer.parseInt(client.getAttribute("clientPingLogMessageCounter").toString());
		counter++;
		client.setAttribute("clientPingLogMessageCounter", counter);

		//pingLogMessageCounter is set and reset in pollClients method
		if(counter >= T_POLL_TO_LOG_RATIO)
		{
			log.info("poll: " + client.getId() + ", " + conn.getRemoteAddress() + ", " + client.getAttribute("name"));
			log.info("Polled from client. Printed only once in " + T_POLL_TO_LOG_RATIO + " polls.");
			client.setAttribute("clientPingLogMessageCounter", 0);
		}
	}

	/**
	 * Sets the users shared object.
	 *
	 * @param userName the user name
	 * @param userStatus the user status
	 * @param controlStatus the control status
	 * @param userType the user type
	 * @param isModerator the is moderator
	 * @param isAudioOnlyMode the is audio only mode
	 * @param userDisplayName the user display name
	 * @param isVideoPublishing the is video publishing
	 * @param currentUserRole the current user role
	 * @param streamBandwidth the stream bandwidth
	 * @param interactionCount the interaction count
	 * @param userInstituteName the user institute name
	 * @param avcRuntime the avc runtime
	 * @param avcDeviceType the avc device type
	 * @param videoHeight the video height
	 * @param videoWidth the video width
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setUsersSharedObject(String userName, String userStatus, String controlStatus,
			String userType, boolean isModerator, boolean isAudioOnlyMode, String userDisplayName, boolean isVideoPublishing,
			String currentUserRole, Object streamBandwidth, int interactionCount, String userInstituteName,
			String avcRuntime, String avcDeviceType, int videoHeight, int videoWidth)
	{
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		IClient client = conn.getClient();
		String currentPresenterName=null;
		
		if(scope.hasAttribute("currentPresenterName"))
		{
			currentPresenterName=scope.getAttribute("currentPresenterName").toString();
		}
		log.info("setUsersSharedObject:client.id:"+client.getId()+",client.ip:"+conn.getRemoteAddress()+",client.name:"+client.getAttribute("name")+",userName:"+userName+",userStatus:"+
				userStatus+",controlStatus:"+controlStatus+",userType:"+userType+",isModerator:"+isModerator+",userDisplayName:"+userDisplayName+",isVideoPublishing:"+isVideoPublishing+
				",currentUserRole:"+currentUserRole+",currentPresenterName:"+currentPresenterName+",userInstituteName: "+userInstituteName);

		String userRole = null;
		
		if(isModerator)
		{
			
			if(currentPresenterName == null || currentPresenterName.equals(userName))
			{
				log.info("Moderator is set as Presentor");
				userRole = PRESENTER_ROLE;
			}
			else
			{
				log.info("Moderator is set as Viewer");
				userRole = VIEWER_ROLE;
			}
		}
		else  if(currentUserRole.equals(PRESENTER_ROLE))
		{
			if(currentPresenterName != null && currentPresenterName.equals(userName))
			{
				log.info("Non Moderator presenter is set as Presentor again");
				userRole = PRESENTER_ROLE;
			}
			else
			{
				log.info("Non Moderator presenter is set as Viewer");
				userRole = VIEWER_ROLE;
				//If the current user is not a moderator and was presenter before connection was lost, 
				//And if some one else is current presenter, then we should change the user's status to HOLD, 
				//instead of earlier ACCEPT
				//Bug #686
				userStatus = HOLD_STATUS;
			}
		}
		else
		{
			if(currentPresenterName != null && currentPresenterName.equals(userName))
			{
				log.info("Non Moderator Viewer is set as Presentor again");
				userRole = PRESENTER_ROLE;
			}
			else
			{
				log.info("Non Moderator Viewer is set as Viewer");
				userRole = VIEWER_ROLE;
			}
		}

		//If the user is reconnecting. Before loosing connection, the user was in Accept status
		ISharedObject muiSharedObject = getSharedObject(scope, "muiSharedObj");
		if(userStatus.equals(ACCEPT_STATUS) && !isModerator && !userRole.equals(PRESENTER_ROLE))
		{
		
			if(muiSharedObject.hasAttribute("MUIData") && !muiSharedObject.getAttribute("MUIData").toString().equals("true"))
			{
				//Check to make sure that no other user was made a selected viewer in the mean time
				String currentSelectedViewerName = null;
				if(scope.hasAttribute("currentSelectedViewerName"))
				{
					currentSelectedViewerName = scope.getAttribute("currentSelectedViewerName").toString();
				}
				if(currentSelectedViewerName != null && !currentSelectedViewerName.equals(userName))
				{
					log.info("The selected viewer is changed to '"+currentSelectedViewerName+"'. Hence setting this user status to Hold.");
					//If the selected viewer is changed, then set this user's status to Hold status
					userStatus = HOLD_STATUS;
				}
			}
		}
		
		Map userProperty = new HashMap();
		userProperty.put("userStatus", userStatus);
		userProperty.put("controlStatus", controlStatus);
		userProperty.put("userRole", userRole);
		userProperty.put("userType", userType);
		userProperty.put("isModerator", isModerator);
		userProperty.put("isAudioOnlyMode", isAudioOnlyMode);
		userProperty.put("userDisplayName", userDisplayName);
		userProperty.put("isVideoPublishing", isVideoPublishing);
		userProperty.put("streamBandwidth", streamBandwidth);
        userProperty.put("id", userName);
        userProperty.put("userInstituteName", userInstituteName);
        userProperty.put("userInteractedCount", interactionCount);
        userProperty.put("avcRuntime", avcRuntime);
		userProperty.put("avcDeviceType", avcDeviceType);
		userProperty.put("videoHeight", videoHeight);
		userProperty.put("videoWidth", videoWidth);
		
		log.info("Setting users_so shared object property "+userName+"'s userStatus to "+ userStatus + " controlStatus to "+ controlStatus +	    
				" moderatorStatus to "  + isModerator +" User Role to "+ userRole + " display name to "+ userDisplayName + 
				" videoPublishStatus to " +  isVideoPublishing);

		ISharedObject users_so = getSharedObject(scope, "users_so1");
		users_so.setAttribute(userName, userProperty);

		if(isModerator == true)
		{
			scope.setAttribute("moderatorName", userName);
			//moderatorName = userName;
			log.info("Setting MODERATOR Name: " + userName);
		}

		if(userRole.equals(PRESENTER_ROLE))
		{
			//currentPresenterName = userName;	
			conn.getScope().setAttribute("currentPresenterName",userName);
			log.info("Setting PRESENTER Name: " + currentPresenterName);
		}
	}

	/**
	 * Sets the user role.
	 *
	 * @param userName the user name
	 * @param newUserRole the new user role
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setUserRole(String userName, String newUserRole)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("setUserRole:client.id:"+client.getId()+",client.ip:"+conn.getRemoteAddress()+",client.name:"+client.getAttribute("name"));

		//var userProperty = application.users_so.getProperty(userName);
		IScope scope = conn.getScope();
		ISharedObject users_so = getSharedObject(scope, "users_so1");
		HashMap userProperty = (HashMap) users_so.getAttribute(userName);
		if(userProperty != null)
		{
			userProperty.put("userRole", newUserRole);
			users_so.setAttribute(userName, userProperty);
			log.info("Setting users_so shared object property "+userName+"'s user Role to "+ newUserRole);
		}
		else
		{
			log.info("userProperty is null while setting users_so shared object property "+userName+"'s user Role to "+ newUserRole);
		}
	}

	/**
	 * Sets the control status.
	 *
	 * @param userName the user name
	 * @param newControlStatus the new control status
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setControlStatus(String userName, String newControlStatus)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("setControlStatus:"+client.getId()+","+conn.getRemoteAddress()+","+client.getAttribute("name") + ":");
		log.info("setControlStatus:newControlStatus:" + newControlStatus);

		ISharedObject users_so = getSharedObject(conn.getScope(), "users_so1");
		HashMap userProperty = (HashMap)users_so.getAttribute(userName);
		if(userProperty != null)
		{
			userProperty.put("controlStatus", newControlStatus);
			if(newControlStatus != null && newControlStatus.equals("prsntr_request"))
			{
				userProperty.put("requestTime", new Date().getTime());
				log.info("Setting users_so shared object property "+userName+"'s user requestTime to "+userProperty.get("requestTime"));
			}
			else
			{
				userProperty.put("requestTime", "");
				log.info("Resetting users_so shared object property "+userName+"'s user requestTime");
			}

			users_so.setAttribute(userName, userProperty);
			log.info("Setting users_so shared object property "+userName+"'s control status to "+ newControlStatus);
		}
		else
		{
			log.info("userProperty is null while setting users_so shared object property "+userName+"'s control status to "+ newControlStatus);
		}
	}

	/**
	 * Sets the video publish status.
	 *
	 * @param userName the user name
	 * @param videoPublishStatus the video publish status
	 * @param streamBandwidth the stream bandwidth
	 * @param videoHeight the video height
	 * @param videoWidth the video width
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setVideoPublishStatus(String userName, boolean videoPublishStatus, Object streamBandwidth, int videoHeight, int videoWidth)
	{  
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("setVideoPublishStatus:"+client.getId()+","+conn.getRemoteAddress()+","+client.getAttribute("name")+":");

		ISharedObject users_so = getSharedObject(conn.getScope(), "users_so1");
		HashMap userProperty = (HashMap) users_so.getAttribute(userName);

		if(userProperty != null)
		{
			userProperty.put("isVideoPublishing", videoPublishStatus);
			userProperty.put("streamBandwidth", streamBandwidth);
			userProperty.put("videoHeight", videoHeight);
			userProperty.put("videoWidth", videoWidth);

			if( userProperty.get("userRole").equals(PRESENTER_ROLE))
			{
				if(videoPublishStatus == true)
				{
					userProperty.put("userStatus", ACCEPT_STATUS);
				}
				else
				{
					userProperty.put("userStatus", HOLD_STATUS);
				}
			}

			users_so.setAttribute(userName,userProperty);

			log.info("Setting users_so shared object property "+userName+"'s videoPublishStatus to '"+ videoPublishStatus+"'");
		}
		else
		{
			log.info("userProperty is null while setting users_so shared object property "+userName+"'s videoPublishStatus to "+ videoPublishStatus);
		}
	}
	
	/**
	 * Sets the video wall shared so.
	 *
	 * @param isSelected the is selected
	 * @param selectedUser the selected user
	 * @param selectedStream the selected stream
	 */
	public void setVideoWallSharedSO(Boolean isSelected, String selectedUser, String selectedStream)
	{
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject videoWallSharedObject = getSharedObject(scope, "videoWallSharedObject");
		videoWallSharedObject.beginUpdate();
		videoWallSharedObject.setAttribute("selectedUser",selectedUser);
		videoWallSharedObject.setAttribute("selectedStreamName",selectedStream);
		videoWallSharedObject.setAttribute("isSelected",isSelected);
		//videoWallSharedObject.flush();
		videoWallSharedObject.endUpdate();
	}

	/**
	 * Sets the streaming status.
	 *
	 * @param userName the user name
	 * @param streamingStatus the streaming status
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setStreamingStatus(String userName, boolean streamingStatus)
	{  
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("setStreamingStatus :"+client.getId()+","+conn.getRemoteAddress()+","+client.getAttribute("name")+":");

		ISharedObject users_so = getSharedObject(conn.getScope(), "users_so1");
		HashMap userProperty = (HashMap) users_so.getAttribute(userName);

		if(userProperty != null)
		{
			userProperty.put("isAudioOnlyMode", streamingStatus);
			users_so.setAttribute(userName,userProperty);

			log.info("Setting users_so shared object property "+userName+"'s streamingStatus to '"+ streamingStatus+"'");
		}
		else
		{
			log.info("userProperty is null while setting users_so shared object property "+userName+"'s streamingStatus to "+ streamingStatus);
		}
	}

	/**
	 * Sets the user status.
	 *
	 * @param userName the user name
	 * @param newUserStatus the new user status
	 */
	@SuppressWarnings({ "unchecked" })
	public void setUserStatus(String userName, String newUserStatus)
	{  
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		IScope scope = conn.getScope();
		
		log.info("setUserStatus:client.id:"+client.getId()+",client.ip:"+conn.getRemoteAddress()+",client.name:"+client.getAttribute("name"));
		log.info("setUserStatus:userName:"+userName+",newUserStatus:"+newUserStatus);

		ISharedObject users_so = getSharedObject(conn.getScope(), "users_so1");
		HashMap<String, Object> userStatusProperty = (HashMap<String, Object>) users_so.getAttribute(userName);
		log.info("setUserStatus:userStatusProperty:" + userStatusProperty);
		//If the user's shared object is not yet set, but setUserStatus could be called.
		//This could some times happen if the user just logs in
		if(userStatusProperty == null)
		{
			log.info("userProperty is null while setting users_so shared object property "+userName+"'s user status to "+ newUserStatus);
			return;
		}
		
		String currentSelectedViewerName = null;
		if(scope.hasAttribute("currentSelectedViewerName"))
		{
			currentSelectedViewerName = scope.getAttribute("currentSelectedViewerName").toString();
		}
		
		if(newUserStatus.equals(ACCEPT_STATUS))
		{
			//int userInteractedCount = userStatusProperty.get("userInteractedCount");
			int userInteractedCount;
			//if(userStatusProperty.get("userInteractedCount") == null)
			if (!userStatusProperty.containsKey("userInteractedCount"))
			{
				userInteractedCount = 0;		//interactedCount;
			}
			else
			{
				userInteractedCount =  Integer.parseInt(userStatusProperty.get("userInteractedCount").toString());
			}
			userInteractedCount = userInteractedCount + 1;
			currentSelectedViewerName = userName;
			userStatusProperty.put("userInteractedCount", userInteractedCount);
			log.info("Setting the currentSelectedViewerName value to '"+userName+"'");
		}
		else if(currentSelectedViewerName != null && currentSelectedViewerName.equals(userName))
		{
			currentSelectedViewerName = null;
			log.info("Resetting the currentSelectedViewerName value to null");
		}
		scope.setAttribute("currentSelectedViewerName", currentSelectedViewerName);
		log.info("setUserStatus:setting userStatus property to " + newUserStatus);
		userStatusProperty.put("userStatus", newUserStatus);
		if(newUserStatus.equals(ACCEPT_STATUS) || newUserStatus.equals(HOLD_STATUS ))
		{
			userStatusProperty.put("requestTime", "");
			log.info("Resetting users_so shared object property "+userName+"'s user requestTime");
		}
		else if(newUserStatus.equals(WAITING_STATUS))
		{
			userStatusProperty.put("requestTime", new Date().getTime());
			log.info("Setting users_so shared object property "+userName+"'s user requestTime to "+userStatusProperty.get("requestTime"));
		}
		users_so.setAttribute(userName,userStatusProperty);
		log.info("setUserStatus:Setting users_so shared object property "+userName+"'s user status to "+ newUserStatus);
	}

	/**
	 * Sets the audio mute shared object.
	 *
	 * @param value the new audio mute shared object
	 */
	public void setAudioMuteSharedObject(String value)
	{
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		IClient client = conn.getClient();
		log.info("setAudioMuteSharedObject:"+client.getId()+","+conn.getRemoteAddress()+","+client.getAttribute("name")+":");
		//If the connected user is teacher,then set the value to audioMuteSharedObject
		ISharedObject audioMuteSharedObject = getSharedObject(scope, "audioMuteSharedObject1");
		audioMuteSharedObject.beginUpdate();
		audioMuteSharedObject.setAttribute(PRESENTER_ROLE, "null");
		audioMuteSharedObject.setAttribute(PRESENTER_ROLE, value);
		audioMuteSharedObject.endUpdate();
		log.info("Setting audioMuteSharedObject shared object property to '"+value+"' for the user");
	}


	/**
	 * Give control.
	 *
	 * @param selectedUserName the selected user name
	 * @param classModeratorName the class moderator name
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void giveControl(String selectedUserName, String classModeratorName)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("giveControl:"+client.getId()+","+conn.getRemoteAddress()+","+client.getAttribute("name")+":");
		log.info("giveControl:selectedUserName:" + selectedUserName + ",classModeratorName:" + classModeratorName);
		ISharedObject users_so = getSharedObject(conn.getScope(), "users_so1");
		HashMap userControlProperty = (HashMap)users_so.getAttribute(selectedUserName);
		HashMap moderatorControlProperty = (HashMap) users_so.getAttribute(classModeratorName);
		
		if(userControlProperty != null && moderatorControlProperty != null)
		{
			userControlProperty.put("userRole", PRESENTER_ROLE);
			log.info("giveControl:Publishing status of '"+selectedUserName+"' is '"+userControlProperty.get("isVideoPublishing")+"'");

			if(((Boolean)userControlProperty.get("isVideoPublishing")).booleanValue() == true)
			{
				userControlProperty.put("userStatus", ACCEPT_STATUS);
				log.info("giveControl:Accept Block:userStatus of '"+selectedUserName+"' is '"+userControlProperty.get("userStatus")+"'");
			}
			else
			{
				userControlProperty.put("userStatus", HOLD_STATUS);
				log.info("giveControl:Hold Block:User status of '"+selectedUserName+"' is '"+userControlProperty.get("userStatus")+"'");
			}
			userControlProperty.put("controlStatus", "null");

			moderatorControlProperty.put("userRole", VIEWER_ROLE);
			moderatorControlProperty.put("userStatus", HOLD_STATUS);

			users_so.beginUpdate();
				users_so.setAttribute(selectedUserName, userControlProperty);
				users_so.setAttribute(classModeratorName, moderatorControlProperty); 
			users_so.endUpdate();
			//currentPresenterName = selectedUserName;
			conn.getScope().setAttribute("currentPresenterName", selectedUserName);
			log.info("giveControl:Setting users_so shared object userRole of '"+selectedUserName+"' to PRESENTER and userStatus to '"+userControlProperty.get("userStatus")+"'");
		}
		else
		{
			log.info("giveControl:Can't set users_so shared object userRole of '"+selectedUserName+"' to PRESENTER and userStatus to '"+userControlProperty.get("userStatus")+"' as one of these user's property is null");
		}
		log.info("giveControl:Exited Function");
	}

	/**
	 * Take control.
	 *
	 * @param classModeratorName the class moderator name
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void takeControl(String classModeratorName)
	{  
		
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("takeControl:"+client.getId()+","+conn.getRemoteAddress()+","+client.getAttribute("name")+":");
		
		ISharedObject users_so = getSharedObject(conn.getScope(), "users_so1");
		String currentPresenterName=conn.getScope().getAttribute("currentPresenterName").toString();
		HashMap userControlProperty = (HashMap)users_so.getAttribute(currentPresenterName);
		HashMap moderatorControlProperty = (HashMap)users_so.getAttribute(classModeratorName);
		
		if(userControlProperty != null && moderatorControlProperty != null)
		{
			userControlProperty.put("userRole", VIEWER_ROLE);
			userControlProperty.put("userStatus", HOLD_STATUS);

			moderatorControlProperty.put("userRole", PRESENTER_ROLE);
			if((Boolean)moderatorControlProperty.get("isVideoPublishing") == true)
			{
				moderatorControlProperty.put("userStatus", ACCEPT_STATUS);
				log.info("Accept Block:User status of '"+classModeratorName+"' is '"+moderatorControlProperty.get("userStatus")+"'");
			}
			else
			{
				moderatorControlProperty.put("userStatus", HOLD_STATUS);
				log.info("Hold Block:User status of '"+classModeratorName+"' is '"+moderatorControlProperty.get("userStatus")+"'");
			}
			users_so.beginUpdate();
				users_so.setAttribute(currentPresenterName, userControlProperty);
				users_so.setAttribute(classModeratorName, moderatorControlProperty); 
			users_so.endUpdate();

			log.info("Setting users_so shared object userRole of '"+ currentPresenterName + "' to VIEWER '");
			conn.getScope().setAttribute("currentPresenterName", classModeratorName);
			//If PTT is on, when control is given back to moderator, the moderator should be on talk
			ISharedObject audioMuteSharedObject = getSharedObject(conn.getScope(), "audioMuteSharedObject1");
			String presenterRole = audioMuteSharedObject.getAttribute(PRESENTER_ROLE).toString();
			if(presenterRole != null && presenterRole.equals(MUTE_PTT))
			{
				client.setAttribute("setAudioMuteSharedObject", UN_MUTE_PTT);
			}
			else
			{
				log.info("Can't set users_so shared object userRole of '"+ currentPresenterName + "' to VIEWER ' as one of these user's property is null");
			}
		}
	}
	
	/**
	 * Start live quiz server.
	 * This function is used for starting the live quiz in the client side
	 * This is the first version. The teacher passes the Quiz id to this function
	 * This function currently works only for thoese users who have already logged into the class
	 * @param quizId the quiz id
	 * @param isQuiz the is quiz
	 */
	public void startLiveQuizServer(int quizId, boolean isQuiz)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		IScope scope = conn.getScope();
		log.info("startLiveQuiz:" + client.getId() + ", " + conn.getRemoteAddress() + ", " + client.getAttribute("name") +":");
		log.info("Entering the start live quiz with id '" + quizId + "'");
		scope.setAttribute("currentQuizId", quizId);
		invokeLiveQuiz(isQuiz);
		log.info("Exiting the start live quiz with id '" + quizId + "'");
	}

	/**
	 * Stop live quiz.
	 * This function is used for stopping the live quiz in the client side
	 * This is the first version
	 * 
	 */
	public void stopLiveQuiz()
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		IScope scope = conn.getScope();
		log.info("stopLiveQuiz:" + client.getId() + ", " + conn.getRemoteAddress() + ", " + client.getAttribute("name") +":");
		int currentQuizId = Integer.parseInt(scope.getAttribute("currentQuizId").toString());
		log.info("Entering the stop live quiz with id '" + currentQuizId + "'");
		scope.setAttribute("currentQuizId", 0);
	}
	
	/**
	 * Invoke live quiz.
	 * This function actually calls the live quiz in client side
	 * 
	 * @param isQuiz the is quiz
	 */
	public void invokeLiveQuiz(boolean isQuiz)
	{
		IConnection conn = Red5.getConnectionLocal();
		//IClient client = conn.getClient();
		IScope scope = conn.getScope();
		
		int currentQuizId = Integer.parseInt(scope.getAttribute("currentQuizId").toString());
		if(currentQuizId != 0)
		{
			 //Sending back the argument which tells if this request is Quiz or Polling
			//application.clients[i].call("startLiveQuizClient",null,currentQuizId, isQuiz);	
			ServiceUtils.invokeOnAllConnections(scope, "startLiveQuizClient",  new Object[]{currentQuizId, isQuiz});
		}
	}
	
	/**
	 * Gets the current active client id.
	 *
	 * @param name the name
	 * @return the current active client id
	 */
	public String getCurrentActiveClientId(String name)
	{
		log.info("getCurrentActiveClientId:name:" + name);
		
		IClient clientObj = getCurrentActiveClient(name);
		log.info("getCurrentActiveClientId:CurrentActiveClient:" + clientObj);
		if(clientObj != null)
		{
			return clientObj.getId();
		}
		log.info("getCurrentActiveClientId:client not found returning empty string.");
		return "";
	}
	
	/**
	 * Gets the current active client.
	 *
	 * @param name the name
	 * @return the current active client
	 */
	public static IClient getCurrentActiveClient(String name)
	{
		IScope scope = Red5.getConnectionLocal().getScope();
		log.info("getCurrentActiveClient:name:" + name +",scope:" + scope.getName());
		
		Object[] clients = scope.getClients().toArray();
		int numClients = clients.length;
		log.info("getCurrentActiveClient:numClients:" + numClients);
		for (int i = 0; i < numClients; i++)
		{
			IClient currentClient = (IClient)clients[i];
			String currClientName = currentClient.getAttribute("name").toString();
			if(currClientName.equals(name))
			{
				log.info("getCurrentActiveClient:returning currClientName:" + currClientName);
				return currentClient;
			}
		}

		log.info("getCurrentActiveClient:returning currClientName:null");
		return null;
	}
	
	/**
	 * Chat message from client.
	 * handling chat related calls from client
	 * 
	 * @param msg the msg
	 */
	public void chatMessageFromClient(String msg){
		log.info("chatMessageFromClient:msg: " + msg);
		log.info("calling chat.chatMessageFromClient...");
		//IScope scope = Red5.getConnectionLocal().getScope();
		//ISharedObject chat_users_so = getSharedObject(scope, "ChatUsers");
		//chat.chatMessageFromClient(msg, chat_users_so);
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		Chat chat=(Chat)scope.getAttribute("chat");
		chat.chatMessageFromClient(msg);
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
		log.info("calling chat.getChatServerTime...");
		//IScope scope = Red5.getConnectionLocal().getScope();
		//ISharedObject chat_users_so = getSharedObject(scope, "ChatUsers");
		//chat.chatMessageFromClient(msg, chat_users_so);
		IScope scope = Red5.getConnectionLocal().getScope();
		Chat chat=(Chat)scope.getAttribute("chat");
		chat.getChatServerTime(functionToCall);
	}
	
	/**
	 * Change page.
	 * handling document sharing related calls from client
	 * 
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void changePage(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).changePage(data);
	}
	
	/**
	 * Load new file.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void loadNewFile(HashMap data)
	{
		log.info("Application.loadNewFile:data: " + data);
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).loadNewFile(data);
	}
	
	/**
	 * Page change.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void PageChange(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).pageChange(data);
	}
	
	/**
	 * Animation change.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	//public void animationChange(HashMap data)
	public void animationChange(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).animationChange(data);
	}
	
	/**
	 * Doc rotation.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void docRotation(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).docRotation(data);
	}
	
	/**
	 * Zoom document.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void zoomDocument(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).zoomDocument(data);
	}
	
	/**
	 * Scroll document.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void scrollDocument(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).scrollDocument(data);
	}
	
	/**
	 * Sharing mouse point.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void sharingMousePoint(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).sharingMousePoint(data);
	}
	
	/**
	 * Download permission.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void downloadPermission(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).downloadPermission(data);
	}
	
	/**
	 * Sets the annotation tool.
	 *
	 * @param data the new annotation tool
	 */
	@SuppressWarnings("rawtypes")
	public void setAnnotationTool(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).setAnnotationTool(data);
	}
	
	/**
	 * Sets the annotation values.
	 *
	 * @param data the new annotation values
	 */
	@SuppressWarnings("rawtypes")
	public void setAnnotationValues(HashMap data)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).setAnnotationValues(data);
	}
	
	//public void clearProperties(IConnection conn)
	/**
	 * Clear properties.
	 */
	public void clearProperties()
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).clearProperties();
	}
	
	/**
	 * Record data.
	 *
	 * @param timer the timer
	 * @param pag the pag
	 * @param path the path
	 */
	public void recordData(String timer, String pag, String path)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).recordData(timer, pag, path);
	}
	
	/**
	 * Stop record data.
	 *
	 * @param d the d
	 */
	public void stopRecordData(String d)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((DocumentSharing)scope.getAttribute("docSharing")).stopRecordData(d);
	}	
	
	/**
	 * Gets the shape id.
	 * handling calls realted to whiteboard
	 * called from client to get the current shape if in the SO
	 *
	 * @param shapeIdPropertyName the shape id property name
	 * @return the shape id
	 */
	public int getShapeId(String shapeIdPropertyName)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		return ((Whiteboard)scope.getAttribute("whiteboard")).getShapeId(shapeIdPropertyName);
	}
	
	/**
	 * Sets the shape id.
	 * Called from the client if any updation for the shape id is needed when moderator logs in
	 * This is called usually when FMS is restarted in the middle of a lecture session.
	 * @param lecturePageNum the lecture page num
	 * @param newShapeId the new shape id
	 */
	public void setShapeId(String lecturePageNum, int newShapeId)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((Whiteboard)scope.getAttribute("whiteboard")).setShapeId(lecturePageNum, newShapeId);
	}
	
	/**
	 * Delete saved shapes.
	 * Called from the client to delete the saved shapes from the SO
	 * 
	 * @param shapeIdPropertyName the shape id property name
	 * @param shapeId the shape id
	 */
	public void deleteSavedShapes(String shapeIdPropertyName, int shapeId)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((Whiteboard)scope.getAttribute("whiteboard")).deleteSavedShapes(shapeIdPropertyName, shapeId);
	}
	
	/**
	 * Adds the graphic.
	 * called from the client when a new shape is drawn. 
	 * 
	 * @param graphic the graphic
	 * @param LecturePageNum the lecture page num
	 */
	@SuppressWarnings("rawtypes")
	public void addGraphic(HashMap graphic, String LecturePageNum)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((Whiteboard)scope.getAttribute("whiteboard")).addGraphic(graphic, LecturePageNum);
	}
	
	/**
	 * Share pointer.
	 *
	 * @param shapePoint the shape point
	 */
	public void sharePointer(Object shapePoint){
		IScope scope=Red5.getConnectionLocal().getScope();
		((Whiteboard)scope.getAttribute("whiteboard")).sharePointer(shapePoint);
	}
	
	/**
	 * Sets the collaboration mode.
	 *
	 * @param mode the new collaboration mode
	 */
	public void setCollaborationMode(String mode){
		IScope scope=Red5.getConnectionLocal().getScope();
		((Whiteboard)scope.getAttribute("whiteboard")).setCollaborationMode(mode);
	}
	
	/**
	 * Sets the page info.
	 *
	 * @param PageNumberCaching the page number caching
	 * @param currentPageNumber the current page number
	 * @param totalPage the total page
	 * @param lectureName the lecture name
	 */
	public void setPageInfo(int PageNumberCaching, int currentPageNumber, int totalPage, String lectureName)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((Whiteboard)scope.getAttribute("whiteboard")).setPageInfo(PageNumberCaching, currentPageNumber, totalPage, lectureName);
	}

	/**
	 * Import page.
	 *
	 * @param clearGraphic the clear graphic
	 * @param importedGraphics the imported graphics
	 * @param lecturePageNum the lecture page num
	 */
	@SuppressWarnings("rawtypes")
	public void importPage(HashMap clearGraphic, ArrayList importedGraphics, String lecturePageNum)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((Whiteboard)scope.getAttribute("whiteboard")).importPage(clearGraphic, importedGraphics, lecturePageNum);
	}
	
//	public void clearShapesFromSO(String lecturePageNum)
//	{
//		IScope scope=Red5.getConnectionLocal().getScope();
//		((Whiteboard)scope.getAttribute("whiteboard")).clearShapesFromSO(lecturePageNum);
//	}
	
	/**
	 * Msg from prsntr video share.
	 * methods for handling calls related to video sharing
	 * @param url the url
	 * @param command the command
	 * @param playTime the play time
	 */
	public void msgFromPrsntrVideoShare(String url, String command, String playTime)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((VideoSharing)scope.getAttribute("videoSharing")).msgFromPrsntrVideoShare(url, command, playTime);
	}
	
	/**
	 * Msg from client video share.
	 *
	 * @param syncState the sync state
	 * @param uname the uname
	 */
	public void msgFromClientVideoShare(Object syncState, String uname)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((VideoSharing)scope.getAttribute("videoSharing")).msgFromClientVideoShare(syncState, uname);
	}
	
	/**
	 * Mute sound.
	 *
	 * @param vol the vol
	 */
	public void muteSound(String vol)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((VideoSharing)scope.getAttribute("videoSharing")).muteSound(vol);
	}
	
	/**
	 * Tim req late user.
	 *
	 * @param uname the uname
	 */
	public void timReqLateUser(String uname)
	{
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		String currentPresenterName=scope.getAttribute("currentPresenterName").toString();
		((VideoSharing)scope.getAttribute("videoSharing")).getPlayheadTimeForLateComingUser(uname,currentPresenterName);
	}
	
	/**
	 * Tim info.
	 *
	 * @param sliderTim the slider tim
	 * @param uname the uname
	 */
	public void timInfo(int sliderTim, String uname)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((VideoSharing)scope.getAttribute("videoSharing")).sendPlayheadTimeToUser(sliderTim, uname);
	}
	
	/**
	 * Gets the global data.
	 * calls related to 2DSharing
	 * 
	 * @return the global data
	 */
	public void getGlobalData()
	{
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		TwoDSharing twoDSharing=(TwoDSharing)scope.getAttribute("twoDSharing");
		twoDSharing.getGlobalData();
	}
	
	/**
	 * Update global.
	 *
	 * @param prop the prop
	 * @param values the values
	 */
	public void updateGlobal(String prop, Object values)
	{
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		TwoDSharing twoDSharing=(TwoDSharing)scope.getAttribute("twoDSharing");
		twoDSharing.updateGlobal(prop, values);
	}
	
	/**
	 * Send data.
	 *
	 * @param info the info
	 */
	public void sendData(Object info)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		TwoDSharing twoDSharing=(TwoDSharing)scope.getAttribute("twoDSharing");
		twoDSharing.sendData(info);
	}
	
	/**
	 * Client download.
	 *
	 * @param path the path
	 */
	public void ClientDownload(String path)
	{
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		TwoDSharing twoDSharing=(TwoDSharing)scope.getAttribute("twoDSharing");
		twoDSharing.clientDownload(path);
	}
	
	/**
	 * Frame updator.
	 *
	 * @param cuuFra the cuu fra
	 */
	public void frameUpdator(int cuuFra)
	{
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		TwoDSharing twoDSharing=(TwoDSharing)scope.getAttribute("twoDSharing");
		twoDSharing.frameUpdator(cuuFra);
	}
	
	/**
	 * Global class out.
	 */
	public void GlobalClassOut()
	{
		IConnection conn = Red5.getConnectionLocal();
		IScope scope = conn.getScope();
		TwoDSharing twoDSharing=(TwoDSharing)scope.getAttribute("twoDSharing");
		twoDSharing.globalClassOut();
	}
	
	//calls related to 3DSharing	
	/**
	 * Sets the shared object.
	 *
	 * @param propertyName the property name
	 * @param value the value
	 */
	public void setSharedObject(String propertyName, Object value)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((ThreeDSharing)scope.getAttribute("threeDSharing")).setObjectName(propertyName, value);
	}
	
	/**
	 * Sets the object name.
	 *
	 * @param propertyName the property name
	 * @param value the value
	 */
	public void setObjectName(String propertyName, Object value)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((ThreeDSharing)scope.getAttribute("threeDSharing")).setObjectName(propertyName, value);
	}
			
	/**
	 * Clear server.
	 *
	 * @param propertyname the propertyname
	 * @param value the value
	 */
	public void clearServer(String propertyname, Object value)
	{
		IScope scope=Red5.getConnectionLocal().getScope();
		((ThreeDSharing)scope.getAttribute("threeDSharing")).clearServer(propertyname, value);
	}
}