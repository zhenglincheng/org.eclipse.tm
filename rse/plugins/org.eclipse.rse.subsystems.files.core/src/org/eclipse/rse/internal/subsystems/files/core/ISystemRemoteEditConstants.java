/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * Martin Oberhuber (Wind River) - [189130] Move SystemIFileProperties from UI to Core
 ********************************************************************************/

package org.eclipse.rse.internal.subsystems.files.core;

public interface ISystemRemoteEditConstants {

	
	
	// Constants for remote editing
	public static final String REMOTE_FILE_OBJECT_KEY 	= "remote_file_object_key"; //$NON-NLS-1$

	public static final String REMOTE_FILE_SUBSYSTEM_KEY  = "remote_file_subsystem_key"; //$NON-NLS-1$
	public static final String REMOTE_FILE_PATH_KEY 		= "remote_file_path_key"; //$NON-NLS-1$

	public static final String REMOTE_FILE_MODIFIED_STAMP  = "remote_file_modified_stamp"; //$NON-NLS-1$
	public static final String REMOTE_FILE_BINARY_TRANSFER = "remote_file_binary_transfer"; //$NON-NLS-1$
	public static final String TEMP_FILE_DIRTY 			 = "temp_file_dirty"; //$NON-NLS-1$
	public static final String TEMP_FILE_READONLY        = "temp_file_readonly"; //$NON-NLS-1$

	public static final String DOWNLOAD_FILE_MODIFIED_STAMP = "download_file_modified_stamp"; //$NON-NLS-1$
	// for mounted mappings
	public static final String REMOTE_FILE_MOUNTED = "remote_file_mounted"; //$NON-NLS-1$
	public static final String RESOLVED_MOUNTED_REMOTE_FILE_PATH_KEY = "resolved_mounted_remote_file_path_key"; //$NON-NLS-1$
	public static final String RESOLVED_MOUNTED_REMOTE_FILE_HOST_KEY = "resolved_mounted_remote_file_host_key"; //$NON-NLS-1$
	
	
	// Constants related to how the editor will set the document content
	public static final String LOAD_TYPE_KEY = "load_type_key"; //$NON-NLS-1$
	public static final String LOAD_TYPE_USE_STRING = "load_type_use_string"; //$NON-NLS-1$
	
	
	// Universal remote editing profile
	public static final String DEFAULT_EDITOR_PROFILE = "default"; //$NON-NLS-1$
	public static final String UNIVERSAL_EDITOR_PROFILE = "universal"; //$NON-NLS-1$
	public static final String UNIVERSAL_LOCAL_EDITOR_PROFILE = "universallocal"; //$NON-NLS-1$
	
	
	// Local relative directory for various editor actions
	public static final String EDITOR_COMPARE_LOCATION	=	"/compare/"; //$NON-NLS-1$
	public static final String EDITOR_GET_FILE_LOCATION	=	"/get/"; //$NON-NLS-1$
}