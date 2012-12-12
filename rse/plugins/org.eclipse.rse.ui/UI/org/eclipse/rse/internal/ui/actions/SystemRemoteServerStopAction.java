/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
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
 * {Name} (company) - description of contribution.
 *******************************************************************************/

package org.eclipse.rse.internal.ui.actions;

import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.swt.widgets.Shell;



/**
 * This is the "Stop" action that shows up under a remote server action
 *  within the Remote Servers cascading menu.
 */
public class SystemRemoteServerStopAction extends SystemBaseAction
{
	private SystemCascadingRemoteServerBaseAction parentAction;
	
	/**
	 * Constructor.
	 * @param shell  Shell of parent window, used as the parent for the dialog.
	 *               Can be null, but be sure to call setParent before the action is used (ie, run).
	 * @param parentAction The action that cascades into this action.
 	 */
	public SystemRemoteServerStopAction(Shell shell, SystemCascadingRemoteServerBaseAction parentAction) 
	{
	  	super(SystemResources.ACTION_REMOTESERVER_STOP_LABEL,SystemResources.ACTION_REMOTESERVER_STOP_TOOLTIP,  shell);
	  	this.parentAction = parentAction;
	  	allowOnMultipleSelection(false);
	  	//setContextMenuGroup(ISystemContextMenuConstants.GROUP_CONNECTION);
    	setHelp(RSEUIPlugin.HELPPREFIX+"actnspsv"); //$NON-NLS-1$
	}
	
	/**
	 * Called when this action is selection from the popup menu.
	 * Calls {@link SystemCascadingRemoteServerBaseAction#stopServer()} in the parent action.
	 */
	public void run()
	{
		boolean ok = parentAction.stopServer();
		setEnabled(!ok);	
	}
}
