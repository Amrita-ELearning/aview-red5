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
 * File			: DocumentSharing.java
 * Module		: Collaboration - Document Sharing
 * Developer(s)	: Radha
 * Reviewer(s)	: Jayahari 
 * 
 * Description: Class for chat application
 * Dependencies: Used by collaboration_module.Application for forwarding document sharing related calls from the clients.
 *  
 */

package com.amrita.edu.red5.collaboration_module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.so.SharedObjectService;

/**
 * The Class DocumentSharing.
 */
public class DocumentSharing {
	//private static final Log log = LogFactory.getLog(Application.class);
	/** The log. */
	private static Logger log = Logger.getLogger(DocumentSharing.class);
	//ISharedObject document_so;
	/** The value. */
	String value = "";
	
	/** The appender. */
	String appender = "";

	/**
	 * On document sharing start.
	 *
	 * @param scope the scope
	 */
	public void onDocumentSharingStart(IScope scope){
//		SharedObjectService sos = new SharedObjectService();
//		this.document_so = sos.getSharedObject(scope, "document_so");
	}
	
	/**
	 * Change page.
	 *
	 * @param data the data
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void changePage(HashMap data)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("changePage:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		String name = client.getAttribute("name").toString();
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		//This was creating an unwanted sync calls which were not
		//handled properly and was causing runtime exceptions
		document_so.setAttribute(name, data);
		if (document_so.getAttribute(name) != null)
		{
			HashMap<String, String> map = (HashMap<String, String>)document_so.getAttribute(name);
			String path = map.get("path");
			log.info("Selected Document:" + path);
		}
	}


	/**
	 * Load new file.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void loadNewFile(HashMap data)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("loadNewFile:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		//document_so.beginUpdate();
		document_so.setAttribute("NewFile", data);
		//document_so.endUpdate();
		
		if (document_so.getAttribute("NewFile") != null)
		{
			log.info("Documentsharing.loadNewFile.NewFile '" + document_so.getAttribute("NewFile")+"'");
		}
	}

	/**
	 * Page change.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void pageChange(HashMap data)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("pageChange:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		document_so.setAttribute("PageChange",data);
		if (document_so.getAttribute("PageChange") != null)
		{
			log.info("New Page '" + document_so.getAttribute("PageChange")+"'");
		}
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
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("animationChange:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		document_so.setAttribute("StepChange",data);
		if (document_so.getAttribute("StepChange") != null)
		{
			log.info("New Step '" + document_so.getAttribute("StepChange")+"'");
		}
	}

	/**
	 * Doc rotation.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void docRotation(HashMap data)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("docRotation:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		document_so.setAttribute("Rotation",data);
		if (document_so.getAttribute("Rotation") != null)
		{
			log.info("New Angle '" + document_so.getAttribute("Rotation")+"'");
		}
	}

	/**
	 * Zoom document.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void zoomDocument(HashMap data)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("zoomDocument:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		document_so.setAttribute("Scale",data);
		if (document_so.getAttribute("Scale") != null)
		{
			log.info("zoomDocument:New Scale '" + document_so.getAttribute("Scale")+"'");
		}
	}

	/**
	 * Scroll document.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void scrollDocument(HashMap data)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("scrollDocument:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		document_so.setAttribute("Scroll",data);
		if (document_so.getAttribute("Scroll") != null)
		{
			log.info("New Scroll Position '" + document_so.getAttribute("Scroll")+"'");
		}
	}

	/**
	 * Sharing mouse point.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void sharingMousePoint(HashMap data)
	{
		log.info("sharingMousePoint:data:" + data);
		IScope scope = Red5.getConnectionLocal().getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		document_so.setAttribute("SharingMousePoint", data);
		/*
		if (document_so.getAttribute("SharingMousePoint") != null)
		{
			log.info("New Point Location '" + document_so.getAttribute("SharingMousePoint")+"'");
		}
		*/
	}

	/**
	 * Download permission.
	 *
	 * @param data the data
	 */
	@SuppressWarnings("rawtypes")
	public void downloadPermission(HashMap data)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("downloadPermission:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		document_so.setAttribute("PermissionToDownload",data);
		if (document_so.getAttribute("PermissionToDownload") != null)
		{
			log.info("PermissionToDownload '" + document_so.getAttribute("PermissionToDownload")+"'");
		}
	}

	/**
	 * Sets the annotation tool.
	 *
	 * @param data the new annotation tool
	 */
	@SuppressWarnings("rawtypes")
	public void setAnnotationTool(HashMap data)
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("setAnnotationTool:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		document_so.setAttribute("SetAnnotationTool",data);
		if (document_so.getAttribute("SetAnnotationTool") != null)
		{
			log.info("New annotations tool '" + document_so.getAttribute("SetAnnotationTool")+"'");
		}
	}
	
	/**
	 * Sets the annotation values.
	 *
	 * @param data the new annotation values
	 */
	@SuppressWarnings("rawtypes")
	public void setAnnotationValues(HashMap data)
	{
		log.info("setAnnotationValues:data:" + data);
		IScope scope =  Red5.getConnectionLocal().getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		//log.info = "setAnnotationValues, "+this.ip+","+this.name;
		document_so.setAttribute("SetAnnotationValues",data);
		/*
		if (document_so.getAttribute("SetAnnotationValues") != null)
		{
			log.info("New Annotation Value '" + document_so.getAttribute("SetAnnotationValues")+"'",log.info,T_INFO);
		}
		*/
	}

	//public void clearProperties(IConnection conn)
	/**
	 * Clear properties.
	 */
	public void clearProperties()
	{
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("clearProperties:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		
		IScope scope = conn.getScope();
		ISharedObject document_so = new SharedObjectService().getSharedObject(scope, "document_so");
		
		document_so.setAttribute("ClearProperties", null);
		document_so.setAttribute("SharingMousePoint", null);
		document_so.setAttribute("Scroll", null);
		document_so.setAttribute("Scale", null);
		document_so.setAttribute("Rotation", null);
		document_so.setAttribute("StepChange", null);
		document_so.setAttribute("PageChange", null);
		document_so.setAttribute("NewFile", null);
		document_so.setAttribute("DownloadPermission", null);
        document_so.setAttribute("SetAnnotationTool", null);
        document_so.setAttribute("SetAnnotationValues", null);
		if (document_so.getAttribute("ClearProperties") != null)
		{
			log.info("ClearProperties '" + document_so.getAttribute("ClearProperties")+"'");
		}
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
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("recordData:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));
		log.info("Started Recording");
//		value+="\n\n"+'<item>'+"\n\n"+'<time>'+timer+'</time>'+"\n\n"+'<page>'+pag+'</page>'+"\n\n"+
//		          '<filename>'+path+'</filename>'+"\n\n"+'</item>'+"\n\n";
		value += "\n\n" + "<item>" + "\n\n" + "<time>" + timer + "</time>" + "\n\n" +
				"<page>" + pag + "</page>" + "\n\n" + "<filename>" + path + "</filename>" +
				"\n\n" + "</item>" + "\n\n";
		
		appender += value;
		log.info(value);
	}
	
	/**
	 * Stop record data.
	 *
	 * @param d the d
	 */
	public void stopRecordData(String d)
	{	
		IConnection conn = Red5.getConnectionLocal();
		IClient client = conn.getClient();
		log.info("stopRecordData:ip:" + conn.getRemoteAddress() + ",name:" + client.getAttribute("name"));

		File xmlFile = new File("Test.xml");
		log.info("Created file Test.xml");
		try{
			FileWriter xmlFileWriter = new FileWriter(xmlFile);
			String endtag = "<items>" + value + "</items>";
			xmlFileWriter.write(endtag);
			xmlFileWriter.close();
			
			log.info("value = " + value);
			value = "";
			log.info("Recording stopped");

			FileInputStream in = new FileInputStream(xmlFile);
			FileOutputStream out = new FileOutputStream("iPPT$"+d+".xml");
			byte [] b = new byte[1024];
			in.read(b, 0, (int)xmlFile.length());
			out.write(b);
			log.info("Copied Test.xml to "+"iPPT$"+d+".xml");
		}
		catch(Exception e){
			log.info("ERROR while copying Test.xml to "+"iPPT$"+d+".xml");
		}
		finally{
			
		}
//		if(xmlFile.copyTo("iPPT$"+d+".xml"))
//		{
//			log.info("Copied Test.xml to "+"iPPT$"+d+".xml");
//		}
//		else
//		{
//			log.info("ERROR while copying Test.xml to "+"iPPT$"+d+".xml");
//		}
		log.info("Removing Text.xml");
		//xmlFile.remove();
		xmlFile.delete();
	}
}
