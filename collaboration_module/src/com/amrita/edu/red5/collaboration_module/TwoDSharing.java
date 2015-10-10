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
 * File			: TwoDSharing.java
 * Module		: Collaboration - 2D Sharing
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: Class for chat application
 * Dependencies: Used by collaboration_module.Application for forwarding 2D sharing related calls from the clients.
 *  
 */

package com.amrita.edu.red5.collaboration_module;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.red5.server.api.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.so.SharedObjectService;

/**
 * The Class TwoDSharing.
 */
public class TwoDSharing {
	
	/** The log. */
	private static Logger log = Logger.getLogger(TwoDSharing.class);
	//private ISharedObject twoD_so;
//	private ISharedObject globalUpdator;
//	private ISharedObject playBackUpdator;
//	private ISharedObject globalData;
//	private ISharedObject globalDownloader;
	
	//@SuppressWarnings("rawtypes")
	//private HashMap globalStore = new HashMap();
	
	/** The scope. */
	private IScope scope;
	
	/**
	 * Sets data in the global store.
	 */
	@SuppressWarnings("unchecked")
	private void setGlobalStore()
	{
		log.info("setGlobalStore");
		
		HashMap<String, Object> globalStore = (HashMap<String, Object>) scope.getAttribute("globalStore");
		globalStore.put("ran", 0);
		globalStore.put("onClass", false);
		globalStore.put("globalMovieHistory", null);
		globalStore.put("globalBreakPoints", null);
		globalStore.put("endFrame", 0);
		globalStore.put("breakBooln", "false");
		globalStore.put("currFrame", 1);
		globalStore.put("movieStatus", "Pause");
		globalStore.put("mymovieY", 0);
		globalStore.put("mymovieX", 0);
		globalStore.put("mymovieZ", 0);
		globalStore.put("slidervalue", 0);
		globalStore.put("resizeMode", "Full Size");
		globalStore.put("LastDownLoadPath", "");
		scope.setAttribute("globalStore", globalStore);
	} 
	
	/**
	 * On2 d sharing start.
	 *
	 * @param scope the scope
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void on2DSharingStart(IScope scope)
	{
		log.info("on2DAppStart");
		this.scope = scope;
		SharedObjectService sos = new SharedObjectService();
		//twoD_so = sos.getSharedObject(scope, "twoD_so");
		if (scope.hasAttribute("globalStore"))
		{
			log.info("on2DAppStart:globalstore exists");
		}
		HashMap<String, Object> globalStore = new HashMap<String, Object>();
		scope.setAttribute("globalStore", globalStore);
		
		//-2A---set properties in GLOBAL STORE----
		setGlobalStore();
		
		/*
		SHARED  OBJECTS FOR 2D VIEWER
		1,globalUpdator-FOR LATE USER AND PLAY BACK SYNC
		2,playBackUpdator-PLAY BACK CONTROL
		3,globalData-ALL GLOBAL DATAS ARE SORED IN THIS
		4,globalDownloader-Movie Dowloading through this

		*/
		//--2B---------------------------------------------------------------
		//globalUpdator = sos.getSharedObject(scope, "globalUpdator");
		ISharedObject playBackUpdator = sos.getSharedObject(scope, "playBackUpdator");
		ISharedObject globalData = sos.getSharedObject(scope, "globalData");
		//globalDownloader = sos.getSharedObject(scope, "globalDownloader");
		//------------------------------------------------------------------

		//---------------initial values---------------------------------------
		globalStore = (HashMap<String, Object>) scope.getAttribute("globalStore");
		globalData.setAttribute("globalValues",globalStore);
		HashMap playBackValues = new HashMap();
		playBackValues.put("status", "syncStart");
		
		playBackUpdator.setAttribute("playBackValues", playBackValues);
		//--------------------------------------------------------------------
	}
	
	/**
	 * On 2D sharing connect.
	 */
	@SuppressWarnings("unchecked")
	public void on2DSharingConnect()
	{
		HashMap<String, Object> globalStore = (HashMap<String, Object>) this.scope.getAttribute("globalStore");
		log.info("on2DSharingConnect:globalStore:" + globalStore);
		SharedObjectService sos = new SharedObjectService();
		ISharedObject globalData = sos.getSharedObject(this.scope, "globalData");
		globalData.setAttribute("globalValues", globalStore); 
	}
	
	//--3---
	//-3A-STORING DATA IS GLOBAL STORE ------(IT HAS NO SYNC)----
	/**
	 * Update global.
	 *
	 * @param prop the prop
	 * @param values the values
	 */
	@SuppressWarnings("unchecked")
	public void updateGlobal(String prop, Object values)
	{
		log.info("updateGlobal:prop:"+prop+",values:"+values);
		HashMap<String, Object> globalStore = (HashMap<String, Object>) this.scope.getAttribute("globalStore");
		globalStore.put(prop, values);
		this.scope.setAttribute("globalStore", globalStore);
	}

	/**
	 * Gets the global data.
	 * 3B-Return to client
	 * @return the global data
	 */
	@SuppressWarnings("unchecked")
	public void getGlobalData()
	{	
		log.info("getGlobalData");
		HashMap<String, Object> globalStore = (HashMap<String, Object>) this.scope.getAttribute("globalStore");
		log.info("getGlobalData:globalStore:" + globalStore);
		globalStore.put("ran", Math.random());
		this.scope.setAttribute("globalStore", globalStore);
		log.info("getGlobalData:shared object globalData exists:" + new SharedObjectService().hasSharedObject(this.scope, "globalData"));
		ISharedObject globalData = new SharedObjectService().getSharedObject(this.scope, "globalData");
		globalData.setAttribute("globalValues",globalStore);

		log.info("getGlobalData:globalData.getAttribute('globalValues'):" + globalData.getAttribute("globalValues"));
	}

	//---PLAY BACK SYNC ----------------------------------------
	
	/**
	 * Send data.
	 *
	 * @param info the info
	 */
	public void sendData(Object info)
	{
		log.info("sendData:info: " + info);
		ISharedObject playBackUpdator = new SharedObjectService().getSharedObject(this.scope, "playBackUpdator");
		playBackUpdator.setAttribute("playBackValues",info);
	}

	//---Download for Client ------------------------------------
	/**
	 * Client download.
	 *
	 * @param path the path
	 */
	@SuppressWarnings("unchecked")
	public void clientDownload(String path)
	{
		log.info("clientDownload:path: " + path);
		HashMap<String, Object> globalStore = (HashMap<String, Object>) this.scope.getAttribute("globalStore");
		//----------set for new movie--------
		globalStore.put("onClass", true);
		globalStore.put("currFrame", 1);
		globalStore.put("movieStatus", "Pause");
		globalStore.put("mymovieY", 0);
		globalStore.put("mymovieX", 0);
		globalStore.put("mymovieZ", 0);
		globalStore.put("slidervalue", 0);
		globalStore.put("endFrame", 0);
		globalStore.put("breakBooln", "false");
		globalStore.put("resizeMode", "Full Size");
		globalStore.put("globalBreakPoints", null);
		this.scope.setAttribute("globalStore", globalStore);
		//-----------------------------------	
		ISharedObject globalDownloader = new SharedObjectService().getSharedObject(this.scope, "globalDownloader");
		globalDownloader.beginUpdate();
			globalDownloader.setAttribute("ran",Math.random());
			globalDownloader.setAttribute("globalDownloadValues",path);
		globalDownloader.endUpdate();
	}

	/**
	 * Frame updator.
	 * update frame for movie syncing and lateuser
	 *
	 * @param cuuFra the cuu fra
	 */
	@SuppressWarnings("unchecked")
	public void frameUpdator(int cuuFra)
	{
		log.info("frameUpdator:cuuFra: " + cuuFra);
		
		HashMap<String, Object> globalStore = (HashMap<String, Object>) this.scope.getAttribute("globalStore");
		globalStore.put("currFrame", cuuFra);
		this.scope.setAttribute("globalStore", globalStore);
		ISharedObject globalUpdator = new SharedObjectService().getSharedObject(this.scope, "globalUpdator");
		globalUpdator.setAttribute("globalframe", cuuFra);
	}
	
	/**
	 * Global class out.
	 * this to close class bind up
	 */
	public void globalClassOut()
	{
		log.info("GlobalClassOut");
		
		setGlobalStore();
		ISharedObject globalDownloader = new SharedObjectService().getSharedObject(this.scope, "globalDownloader");
		globalDownloader.beginUpdate();
			globalDownloader.setAttribute("globalDownloadValues", null);
			globalDownloader.setAttribute("ran", Math.random());
		globalDownloader.endUpdate();
	}
	
	/**
	 * On 2D disconnect.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void on2DDisconnect()
	{
		log.info("on2DDisconnect");
		
		HashMap playBackValues = new HashMap();
		playBackValues.put("status", "syncStart");
		ISharedObject playBackUpdator = new SharedObjectService().getSharedObject(this.scope, "playBackUpdator");
		playBackUpdator.setAttribute("playBackValues", playBackValues);
	//end Disconnect---		
	}
}
