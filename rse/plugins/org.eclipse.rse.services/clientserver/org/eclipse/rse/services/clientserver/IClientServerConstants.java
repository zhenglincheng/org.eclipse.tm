/*******************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir,
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson,
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 *
 * Contributors:
 * Martin Oberhuber (Wind River) - [releng] Get rid of invalid clientserver -> rse.services dependency
 *******************************************************************************/

package org.eclipse.rse.services.clientserver;

/**
 * Constants used by the Client and Server
 */

public interface IClientServerConstants {

	/**
	 * PLUGIN_ID to use in SimpleSystemMessage for clientserver.
	 *
	 * @since 3.0
	 */
	public static final String PLUGIN_ID = "org.eclipse.rse.services"; //$NON-NLS-1$

//	public static final String PREFIX = PLUGIN_ID+".";
//	// Resource Bundle ids
//	public static final String RESID_PREFIX = PREFIX+"ui.";
//	// Messages
//	public static final String MSG_PREFIX = RESID_PREFIX+"msg.";

	public static final String MSG_EXCEPTION_OCCURRED = "RSEG1003"; //$NON-NLS-1$
	public static final String MSG_EXCEPTION_DELETING = "RSEG1063"; //""RSEG1004"; //$NON-NLS-1$
	public static final String MSG_EXCEPTION_RENAMING = "RSEG1064"; //"RSEG1005"; //MSG_EXCEPTION_PREFIX + "Renaming"; //$NON-NLS-1$
	public static final String MSG_EXCEPTION_MOVING   = "RSEG1065"; //MSG_EXCEPTION_PREFIX + "Moving"; //$NON-NLS-1$

	public static final String FILEMSG_DELETE_FILE_FAILED  = "RSEF1300"; //$NON-NLS-1$
	public static final String FILEMSG_RENAME_FILE_FAILED  = "RSEF1301"; //$NON-NLS-1$
	public static final String FILEMSG_CREATE_FILE_FAILED  = "RSEF1302"; //$NON-NLS-1$
	public static final String FILEMSG_CREATE_FILE_FAILED_EXIST  = "RSEF1303"; //$NON-NLS-1$
	public static final String FILEMSG_CREATE_FOLDER_FAILED  = "RSEF1304"; //$NON-NLS-1$
	public static final String FILEMSG_CREATE_FOLDER_FAILED_EXIST  = "RSEF1309"; //$NON-NLS-1$
	public static final String FILEMSG_CREATE_RESOURCE_NOTVISIBLE  = "RSEF1310"; //$NON-NLS-1$
	public static final String FILEMSG_RENAME_RESOURCE_NOTVISIBLE  = "RSEF1311"; //$NON-NLS-1$
	public static final String FILEMSG_ERROR_NOFILETYPES = "RSEF1010"; //$NON-NLS-1$
	public static final String FILEMSG_COPY_FILE_FAILED  = "RSEF1306"; //$NON-NLS-1$
	public static final String FILEMSG_MOVE_FILE_FAILED  = "RSEF1307"; //$NON-NLS-1$
	public static final String FILEMSG_MOVE_TARGET_EQUALS_SOURCE  = "RSEF1308"; //$NON-NLS-1$
	public static final String FILEMSG_ARCHIVE_CORRUPTED = "RSEG1122"; //$NON-NLS-1$
	public static final String FILEMSG_NO_PERMISSION = "RSEF5001"; //$NON-NLS-1$

	public static final String FILEMSG_REMOTE_SAVE_FAILED = "RSEF5006"; //$NON-NLS-1$
    /**
	 * Flag to indicate "include files only, not folders"
	 */
	public static final int INCLUDE_FILES_ONLY = 2;
	/**
	 * Flag to indicate "include files only, not folders"
	 */
	public static final int INCLUDE_FOLDERS_ONLY = 4;
	/**
	 * Flag to indicate "include files only, not folders"
	 */
	public static final int INCLUDE_ALL = 8;


}