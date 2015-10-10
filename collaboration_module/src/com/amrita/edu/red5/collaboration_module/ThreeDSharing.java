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
 * File			: ThreeDSharing.java
 * Module		: Collaboration - 3D Sharing
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: Class for chat application
 * Dependencies: Used by collaboration_module.Application for forwarding 3D sharing related calls from the clients.
 *  
 */

package com.amrita.edu.red5.collaboration_module;

import org.apache.log4j.Logger;
import org.red5.server.api.IClient;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.so.SharedObjectService;

/**
 * The Class ThreeDSharing.
 */
public class ThreeDSharing {
	
	//private static final Log log = LogFactory.getLog(Application.class);
	/** The log. */
	private static Logger log = Logger.getLogger(ThreeDSharing.class);
	//private ISharedObject threeDSO;
	/** The scope. */
	IScope scope;
	//SharedObjectService sos;
	
	/**
	 * On3 d share start.
	 *
	 * @param scope the scope
	 */
	public void on3DShareStart(IScope scope)
	{
		log.info("on3DSharingStart");
		this.scope = scope;
		//sos = new SharedObjectService();
		// Get the server shared object 'threeD_so'
		//threeDSO = sos.getSharedObject(scope, "threeD_so");
	}
	
	/**
	 * On3 d connect.
	 */
	public void on3DConnect()
	{
		IClient client = Red5.getConnectionLocal().getClient();
		log.info("on3DConnect:client.name:" + client.getAttribute("name"));
//		SharedObjectService sos = new SharedObjectService();
//		if (!sos.hasSharedObject(this.scope, "threeD_so"))
//		{
//			log.info("on3DConnect:creating shared object threeD_so:" + sos.createSharedObject(this.scope, "threeD_so", false));
//		}
		//threeDSO.lock();
		//threeDSO.setAttribute(client.getAttribute("name").toString(), client.getAttribute("name").toString());
		//threeDSO.unlock();
	}

	/**
	 * On 3D disconnect.
	 *
	 * @param client the client
	 */
	public void on3DDisconnect(IClient client)
	{
		log.info("on3DDisconnect:client.name:" + client.getAttribute("name"));
		//threeDSO.setAttribute(client.getAttribute("name").toString(), null);
	}
	
	/**
	 * Sets the shared object.
	 *
	 * @param propertyName the property name
	 * @param value the value
	 */
	public void setSharedObject(String propertyName, Object value)
	{
		log.info("setPosition:propertyName: " + propertyName + ", value: " + value);
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject threeDSO = new SharedObjectService().getSharedObject(scope, "threeD_so");
		threeDSO.setAttribute(propertyName,value);
	}
	
	/**
	 * Sets the object name.
	 *
	 * @param propertyName the property name
	 * @param value the value
	 */
	public void setObjectName(String propertyName, Object value)
	{
		log.info("setObjectName:propertyName: " + propertyName + ", value: " + value);
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject threeDSO = new SharedObjectService().getSharedObject(scope, "threeD_so");
		threeDSO.beginUpdate();
			threeDSO.setAttribute("clearserver", null);
			threeDSO.setAttribute(propertyName, value);
		threeDSO.endUpdate();
	}
		
	/**
	 * Clear server.
	 *
	 * @param propertyName the property name
	 * @param value the value
	 */
	public void clearServer(String propertyName, Object value)
	{
		IClient client = Red5.getConnectionLocal().getClient();
		log.info("clearServer:client:" + client.getAttribute("name"));
		log.info("clearServer:propertyName: " + propertyName + ", value: " + value);
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject threeDSO = new SharedObjectService().getSharedObject(scope, "threeD_so");
		threeDSO.beginUpdate();
			threeDSO.setAttribute(propertyName, value);
			threeDSO.setAttribute("objectname", null);
			threeDSO.setAttribute("position", null);
			threeDSO.setAttribute("rotation", null);
			threeDSO.setAttribute("currentcanvas", null);
			threeDSO.setAttribute("newuser", null);
			threeDSO.setAttribute("totalcanvas", null);
			threeDSO.setAttribute("animation", null);
			threeDSO.setAttribute("delete", null);
			threeDSO.setAttribute("mousewheel", null);
			threeDSO.setAttribute("showpointer", null);
			threeDSO.setAttribute("pointerX", null);
			threeDSO.setAttribute("pointerY", null);
			threeDSO.setAttribute("partname", null);
			threeDSO.setAttribute("currentpage", null);
			threeDSO.setAttribute("partselection", null);
	       	threeDSO.setAttribute("selectedpivot", null);
			threeDSO.setAttribute("camerafocus", null);
	        threeDSO.setAttribute("width", null);
	        threeDSO.setAttribute("height", null);
	        threeDSO.setAttribute("loadedobject", null);
			threeDSO.setAttribute("layer", null);
			threeDSO.setAttribute("objectdetails", null);
			threeDSO.setAttribute("objectpartdetails", null);
			threeDSO.setAttribute("moderatorstate", null);
			threeDSO.setAttribute("collaborative", null);
			threeDSO.setAttribute("loadicon", null);
			threeDSO.setAttribute("newlogin", null);
	        threeDSO.setAttribute("showpointervisible", null);
		threeDSO.endUpdate();
	}
}