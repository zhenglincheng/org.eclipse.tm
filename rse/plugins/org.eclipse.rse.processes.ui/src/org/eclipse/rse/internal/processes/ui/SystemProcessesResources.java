/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 * David McKnight   (IBM)        - [216252] [api][nls] Resource Strings specific to subsystems should be moved from rse.ui into files.ui / shells.ui / processes.ui where possible
 * David McKnight   (IBM)        - [220547] [api][breaking] SimpleSystemMessage needs to specify a message id and some messages should be shared
 * David McKnight   (IBM)        - [223204] [cleanup] fix broken nls strings in files.ui and others
 *******************************************************************************/

package org.eclipse.rse.internal.processes.ui;

import org.eclipse.osgi.util.NLS;

public class SystemProcessesResources extends NLS
{
	private static String BUNDLE_NAME = "org.eclipse.rse.internal.processes.ui.SystemProcessesResources";  //$NON-NLS-1$
		
	// -------------------------
	// ACTIONS...
	// -------------------------
	public static String	ACTION_NEWPROCESSFILTER_LABEL;
	public static String	ACTION_NEWPROCESSFILTER_TOOLTIP;
	
	public static String	ACTION_UPDATEFILTER_LABEL;
	public static String	ACTION_UPDATEFILTER_TOOLTIP;
	
	public static String	ACTION_KILLPROCESS_LABEL;
	public static String	ACTION_KILLPROCESS_TOOLTIP;

	// -------------------------
	// WIZARDS...
	// -------------------------

	// New System process Filter wizard...
	public static String	RESID_NEWPROCESSFILTER_PAGE1_TITLE;
	public static String	RESID_NEWPROCESSFILTER_PAGE1_DESCRIPTION;

	// Change process filter
	public static String	RESID_CHGPROCESSFILTER_TITLE;
	
	// Process Filter String Re-Usable form (used in dialog and wizard)

	public static String	RESID_PROCESSFILTERSTRING_EXENAME_LABEL;
	public static String	RESID_PROCESSFILTERSTRING_USERNAME_LABEL;
	public static String	RESID_PROCESSFILTERSTRING_GID_LABEL; 
	public static String	RESID_PROCESSFILTERSTRING_MINVM_LABEL;
	public static String	RESID_PROCESSFILTERSTRING_MAXVM_LABEL; 
	public static String	RESID_PROCESSFILTERSTRING_UNLIMITED_LABEL; 
	public static String	RESID_PROCESSFILTERSTRING_STATUS_LABEL; 
	
	public static String	RESID_PROCESSFILTERSTRING_EXENAME_TOOLTIP;
	public static String	RESID_PROCESSFILTERSTRING_USERNAME_TOOLTIP;
	public static String	RESID_PROCESSFILTERSTRING_GID_TOOLTIP; 
	public static String	RESID_PROCESSFILTERSTRING_MINVM_TOOLTIP;
	public static String	RESID_PROCESSFILTERSTRING_MAXVM_TOOLTIP; 
	public static String	RESID_PROCESSFILTERSTRING_UNLIMITED_TOOLTIP; 
	public static String	RESID_PROCESSFILTERSTRING_STATUS_TOOLTIP;

	// Warnings
	public static String 	RESID_KILL_WARNING_LABEL;
	public static String 	RESID_KILL_WARNING_TOOLTIP;
	
	// KILL Process dialog
	public static String 	RESID_KILL_TITLE;
	public static String 	RESID_KILL_PROMPT;
	public static String 	RESID_KILL_PROMPT_SINGLE;
	public static String 	RESID_KILL_BUTTON;
	public static String 	RESID_KILL_SIGNAL_TYPE_LABEL;
	public static String 	RESID_KILL_SIGNAL_TYPE_TOOLTIP;
	public static String    RESID_KILL_SIGNAL_TYPE_DEFAULT;
	public static String 	RESID_KILL_COLHDG_EXENAME;
	public static String 	RESID_KILL_COLHDG_PID;

	// Remote Processes dialog
	public static String	RESID_REMOTE_PROCESSES_EXECUTABLE_LABEL;
	public static String	RESID_REMOTE_PROCESSES_EXECUTABLE_TOOLTIP;
	

	
	public static String MSG_VALIDATE_FILEFILTERSTRING_NOTUNIQUE;

	
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, SystemProcessesResources.class);
	}
}
