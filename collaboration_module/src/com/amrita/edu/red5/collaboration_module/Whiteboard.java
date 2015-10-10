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
 * File			: Whiteboard.java
 * Module		: Collaboration - Whiteboard
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: Class for chat application
 * Dependencies: Used by collaboration_module.Application for forwarding whiteboard related calls from the clients.
 *  
 */

package com.amrita.edu.red5.collaboration_module;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.so.SharedObjectService;

/**
 * The Class Whiteboard.
 */
public class Whiteboard {
	
	/** The log. */
	private static Logger log = Logger.getLogger(Whiteboard.class);
	//private IScope scope;
	/** The shape property name when clear. */
	String shapePropertyNameWhenClear = "";
	//ISharedObject whiteboardShapesSO = null;
	//ISharedObject shapeIdSO = null;
	
	/**
	 * On whiteboard start.
	 *
	 * @param scope the scope
	 */
	public void onWhiteboardStart(IScope scope){
		log.info("onWhiteboardStart:scope: " + scope.getName());
		//this.scope = scope;
		SharedObjectService sos = new SharedObjectService();
		ISharedObject whiteboardShapesSO = sos.getSharedObject(scope, "whiteboardShapesSO");
		//shapeIdSO = sos.getSharedObject(scope, "ShapeIdSO");
		ISharedObject collaborationModeSO = sos.getSharedObject(scope,"collaborationModeSO");
		
		whiteboardShapesSO.setAttribute("shape", null);
		collaborationModeSO.setAttribute("collaborationMode","SelectedStudentOnly");
		//scope.setAttribute("shapePropertyNameWhenClear", "");
		this.shapePropertyNameWhenClear = "";
	}
	
	/**
	 * Gets the shape id.
	 * Called from client to get the current shape if in the SO
	 *
	 * @param shapeIdPropertyName the shape id property name
	 * @return the shape id
	 */
	public int getShapeId(String shapeIdPropertyName)
	{
		log.info("getShapeId:shapeIdPropertyName" + shapeIdPropertyName);
	
		int sId;
		SharedObjectService sos = new SharedObjectService();
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject shapeIdSO = sos.getSharedObject(scope, "ShapeIdSO");
		
		//if(shapeIdSO.getAttribute(shapeIdPropertyName) != null)
		if (shapeIdSO.hasAttribute(shapeIdPropertyName))
		{
			sId = Integer.parseInt(shapeIdSO.getAttribute(shapeIdPropertyName).toString());
		}
		else
		{
			sId = 1;
		}
		return sId;
	}
	
	/**
	 * Sets the shape id.
	 * Called from the client if any updation for the shape id is needed when moderator logs in
	 * This is called usually when FMS is restarted in the middle of a lecture session.
	 * 
	 * @param lecturePageNum the lecture page num
	 * @param newShapeId the new shape id
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setShapeId(String lecturePageNum, int newShapeId)
	{
		log.info("setShapeId:lecturePageNum:" + lecturePageNum + ":newShapeId:" + newShapeId);
		SharedObjectService sos = new SharedObjectService();
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject whiteboardShapesSO = sos.getSharedObject(scope, "whiteboardShapesSO");
		//When we are setting the new Shape Id, there may be some shapes written with lesser shape ids.
		//All of them needs to be updated with greater shape ids
		Object[] propertyNames = whiteboardShapesSO.getAttributeNames().toArray();
		log.info("setShapeId:propertyNames.length: " + propertyNames.length);
		//Set<String> propertyNames = whiteboardShapesSO.getAttributeNames();
		
		int highestShapeId = newShapeId;

		whiteboardShapesSO.beginUpdate();

		for (int i = 0; i < propertyNames.length; i++){
			String propertyName = propertyNames[i].toString();
			log.info("setShapeId:property name: " + propertyName);
			String oldShape = propertyName.substring(propertyName.lastIndexOf('|') + 1);
			log.info("setShapeId:oldShape: " + oldShape);
			int oldShapeId = 0;
			if (oldShape != null)
			{
				oldShapeId = Integer.parseInt(oldShape);
			}
			log.info("setShapeId:oldShapeId: " + oldShapeId);
			if(propertyName.substring(0, propertyName.lastIndexOf("|")).equals(lecturePageNum) && oldShapeId <= newShapeId)
			{
				HashMap shape = (HashMap)whiteboardShapesSO.getAttribute(propertyName);
				int shapeId = newShapeId + oldShapeId;
				shape.put("shapeId", shapeId);
				
				whiteboardShapesSO.setAttribute(lecturePageNum + "|" + shapeId, shape);
				whiteboardShapesSO.setAttribute(lecturePageNum + "|" + oldShapeId, null);
				//This keeps the highest new shape id
				if(highestShapeId < shapeId)
				{
					highestShapeId = shapeId;
				}
			}
		}
		whiteboardShapesSO.endUpdate();

		ISharedObject shapeIdSO = sos.getSharedObject(scope, "ShapeIdSO");
		shapeIdSO.setAttribute(lecturePageNum, highestShapeId);
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
		log.info("deleteSavedShapes:shapeIdPropertyName:" + shapeIdPropertyName + ",shapeId:" + shapeId);
		SharedObjectService sos = new SharedObjectService();
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject whiteboardShapesSO = sos.getSharedObject(scope, "whiteboardShapesSO");
		Object[] propertyNames = whiteboardShapesSO.getAttributeNames().toArray();
		for (int i = 0; i < propertyNames.length; i++)
		{
			String name = propertyNames[i].toString();
			int sId = Integer.parseInt(name.substring(name.lastIndexOf("|")+1));
			if(name.substring(0,name.lastIndexOf("|")).equals(shapeIdPropertyName) && sId <= shapeId)
			{
				log.info("deleteSavedShapes:setting property name: " + name +", to sId:" + sId);
				whiteboardShapesSO.setAttribute(name, null);
			}
		}
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
		log.info("importPage:clearGraphic:" + clearGraphic + ",importedGraphics:" + importedGraphics + ",lecturePageNum:" + lecturePageNum);
		addGraphic(clearGraphic, lecturePageNum);
		log.info("importPage:importedGraphics.size:" + importedGraphics.size());
		for (int i = 0; i < importedGraphics.size(); i++)
		{
			//HashMap graphic = (HashMap) importedGraphics.get(i);
			addGraphic((HashMap) importedGraphics.get(i), lecturePageNum);
		}
	}
	
	/**
	 * Adds the graphic.
	 * Called from the client when a new shape is drawn. 
	 * 
	 * @param graphic the graphic
	 * @param lecturePageNum the lecture page num
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addGraphic(HashMap graphic, String lecturePageNum)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		IScope scope = Red5.getConnectionLocal().getScope();
		
		log.info("addGraphic:scope.name:" + scope.getName() + ",client.name:" + client.getAttribute("name"));
		log.info("addGraphic:lecturePageNum:" + lecturePageNum);
		int sId;// = 0;

		int priorSId;
		log.info("addGraphic:graphic: " + graphic.toString());

		SharedObjectService sos = new SharedObjectService();
		ISharedObject shapeIdSO = sos.getSharedObject(scope, "ShapeIdSO");
		ISharedObject whiteboardShapesSO = sos.getSharedObject(scope, "whiteboardShapesSO");
		
		log.info("addGraphic:lecturePageNum attribute exists in shapdIdSO:" + shapeIdSO.hasAttribute(lecturePageNum));
		
		//if (shapeIdSO.getAttribute(lecturePageNum) == null){
		if (!shapeIdSO.hasAttribute(lecturePageNum))
		{
			log.info("addGraphic:no such property:LecturePageNum:" + lecturePageNum);
			priorSId = 0;
		}
		else{
			priorSId = Integer.parseInt(shapeIdSO.getAttribute(lecturePageNum).toString());
		}
		
		sId = priorSId + 1;
		
		shapeIdSO.beginUpdate();
		whiteboardShapesSO.beginUpdate();
		
		String toolName = graphic.get("toolName").toString();
		log.info("addGraphic:toolName:" + toolName);
		if(toolName.equals("restore"))
		{
			log.info("addGraphic:Tool Name is restore");
			//Clear the clear tool flag
			this.shapePropertyNameWhenClear = "";
			//scope.setAttribute("shapePropertyNameWhenClear", "");
		}
		else
		{
			log.info("addGraphic:New sId :" + sId);
			if(toolName.equals("clear"))
			{
				log.info("addGraphic:Tool Name is clear and setting shapePropertyNameWhenClear to " + lecturePageNum);
				this.shapePropertyNameWhenClear = lecturePageNum;
				//scope.setAttribute("shapePropertyNameWhenClear", lecturePageNum);
			}
			else 
			{
				log.info("addGraphic:Tool Name is not clear and not restore");
				//Check if the previous tool is clear and if true then remove all the shapes
				if (whiteboardShapesSO.hasAttribute(lecturePageNum + "|" + priorSId))
				{
					HashMap priorShape = (HashMap) whiteboardShapesSO.getAttribute(lecturePageNum+"|"+priorSId);
					if(priorShape.get("toolName").toString().equals("clear"))
					{
						log.info("addGraphic:prev tool name is clear:" + priorSId);
						clearShapesFromSO(lecturePageNum, whiteboardShapesSO, shapeIdSO);
						//clearShapesFromSO(lecturePageNum);
						//Override sId to 2. Because clear is there in the first shapeId
						sId=2;
						log.info("addGraphic:cleared sId:" + sId);
					}
					else
					{
						log.info("addGraphic:prev tool name is not clear:" + priorSId);
					}
				}
				else
				{
					log.info("addGraphic:prev shape is null");
				}
			}
		}

		graphic.put("shapeId", sId);
		log.info("addGraphic:Adding a graphic with property:" + lecturePageNum + "|" + sId + ",graphic:" + graphic);
		
		whiteboardShapesSO.setAttribute(lecturePageNum + "|" + sId, graphic);
		whiteboardShapesSO.endUpdate();

		shapeIdSO.setAttribute(lecturePageNum, sId);
		shapeIdSO.endUpdate();
	}

	/**
	 * Share pointer.
	 *
	 * @param shapePoint the shape point
	 */
	public void sharePointer(Object shapePoint){
		log.info("sharePointer:shapePoint: " + shapePoint);
		SharedObjectService sos = new SharedObjectService();
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject whiteboardPointerSO = sos.getSharedObject(scope, "whiteboardPointerSO");
		whiteboardPointerSO.setAttribute("pointerPosition", shapePoint);
	}

	
	/**
	 * Sets the collaboration mode.
	 *
	 * @param mode the new collaboration mode
	 */
	public void setCollaborationMode(String mode){
		IConnection conn = Red5.getConnectionLocal();
		log.info("setCollaborationMode: " + conn.getRemoteAddress());
		log.info("Setting collaborationModeSO collaborationMode property to "+ mode);
		SharedObjectService sos = new SharedObjectService();
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject collaborationModeSO = sos.getSharedObject(scope, "collaborationModeSO");
		collaborationModeSO.setAttribute("collaborationMode", mode);
	}
	
	/**
	 * Sets the page info.
	 *
	 * @param pageNumberCaching the page number caching
	 * @param currentPageNumber the current page number
	 * @param totalPages the total pages
	 * @param lectureName the lecture name
	 */
	public void setPageInfo(int pageNumberCaching, int currentPageNumber, int totalPages, String lectureName)
	{
		IConnection conn = Red5.getConnectionLocal();
		SharedObjectService sos = new SharedObjectService();
		IScope scope = Red5.getConnectionLocal().getScope();
		
		log.info("setPageInfo: " + conn.getRemoteAddress());
		log.info("setPageInfo: " + "PageNumberCaching " + pageNumberCaching + "currentPageNumber: " + currentPageNumber + "totalPage: " + totalPages);
		log.info("setPageInfo:shapePropertyNameWhenClear: " + this.shapePropertyNameWhenClear);
		//log.info("setPageInfo:shapePropertyNameWhenClear: " + scope.getAttribute("shapePropertyNameWhenClear"));
		
		//After clearing navigation is done.
		//String shapePropertyNameWhenClear = scope.getAttribute("shapePropertyNameWhenClear").toString();
		//if(shapePropertyNameWhenClear != "")
		if(this.shapePropertyNameWhenClear != "")
		{
			ISharedObject shapeIdSO = sos.getSharedObject(scope, "ShapeIdSO");
			ISharedObject whiteboardShapesSO = sos.getSharedObject(scope, "whiteboardShapesSO");
			
			shapeIdSO.beginUpdate();
			whiteboardShapesSO.beginUpdate();
			clearShapesFromSO(this.shapePropertyNameWhenClear, whiteboardShapesSO, shapeIdSO);
			whiteboardShapesSO.endUpdate();
			shapeIdSO.endUpdate();
		}
		
		ISharedObject navigationSO = sos.getSharedObject(scope, "navigationSO");
		log.info("Locking navigationSO");
		navigationSO.beginUpdate();
		
			log.info("Setting navigationSO PageNumberCaching to " + pageNumberCaching);
			navigationSO.setAttribute("PageNumberCaching", pageNumberCaching);
			
			log.info("Setting navigationSO currentPageNumber to " + currentPageNumber);
			navigationSO.setAttribute("currentPageNumber", currentPageNumber);
			
			log.info("Setting navigationSO totalPageto " + totalPages);
			navigationSO.setAttribute("totalPage", totalPages);
			
			log.info("Setting navigationSO lectureName" + lectureName);
			navigationSO.setAttribute("lectureName", lectureName);
		
		log.info("Unlocking navigationSO");
		//navigationSO.unlock();
		navigationSO.endUpdate();
	}
	
	/**
	 * Clear shapes from so.
	 *
	 * @param lecturePageNum the lecture page num
	 * @param whiteboardShapesSO the whiteboard shapes so
	 * @param shapeIdSO the shape id so
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void clearShapesFromSO(String lecturePageNum, ISharedObject whiteboardShapesSO, ISharedObject shapeIdSO)
	//private void clearShapesFromSO(String lecturePageNum)
	{	
		log.info("clearShapesFromSO:lecturePageNum:" + lecturePageNum);
		//SharedObjectService sos = new SharedObjectService();
		//IScope scope = Red5.getConnectionLocal().getScope();
		//ISharedObject whiteboardShapesSO = sos.getSharedObject(scope, "whiteboardShapesSO");
		Object[] propertyNames = whiteboardShapesSO.getAttributeNames().toArray();
		
		for (int i = 0; i < propertyNames.length; i++)
		{
			String propertyName = propertyNames[i].toString();
			log.info("clearShapesFromSO:property name:" + propertyName);
			log.info("clearShapesFromSO:name.substring(0,name.lastIndexOf('|'):" + propertyName.substring(0,propertyName.lastIndexOf("|")));
			log.info("clearShapesFromSO:lecturePageNum:" + lecturePageNum);
			if(propertyName.substring(0,propertyName.lastIndexOf("|")).equals(lecturePageNum))
			{
				log.info("clearShapesFromSO:found lecturePageNum in whiteboardShapesSO");
				HashMap shape = (HashMap) whiteboardShapesSO.getAttribute(propertyName);
				if(shape.get("toolName").toString().equals("clear"))
				{
					log.info("clearShapesFromSO:toolName is clear and setting it to cleared");
					shape.put("toolName", "cleared");
					shape.put("shapeId", 1);
					whiteboardShapesSO.setAttribute(lecturePageNum + "|1", shape);
				}
				whiteboardShapesSO.setAttribute(propertyName, null);
				log.info("clearShapesFromSO:setting " + propertyName + " attribute of whiteboardShapesSO to null");
			}
		}
		
		//ISharedObject shapeIdSO = sos.getSharedObject(scope, "ShapeIdSO");
		shapeIdSO.setAttribute(lecturePageNum, 1);
		this.shapePropertyNameWhenClear = "";
		//scope.setAttribute("shapePropertyNameWhenClear", "");
	}
}