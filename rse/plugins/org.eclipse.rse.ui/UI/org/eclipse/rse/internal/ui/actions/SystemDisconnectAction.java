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

import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.swt.widgets.Shell;



/**
 * This is the action for disconnecting from a remote subsystem.
 */
public class SystemDisconnectAction extends SystemBaseAction
{
	
	/**
	 * Constructor.
	 * @param shell  Shell of parent window, used as the parent for the dialog.
	 *               Can be null, but be sure to call setParent before the action is used (ie, run).
	 */
	public SystemDisconnectAction(Shell shell)
	{
	    super(SystemResources.ACTION_DISCONNECT_LABEL, SystemResources.ACTION_DISCONNECT_TOOLTIP, shell);
	    allowOnMultipleSelection(false);
	    setContextMenuGroup(ISystemContextMenuConstants.GROUP_CONNECTION);
    	setHelp(RSEUIPlugin.HELPPREFIX+"actn0048"); //$NON-NLS-1$
	}
	/**
	 * Override of parent. Called when testing if action should be enabled based on current
	 *  selection. We check the selected object is one of our subsystems, and if we are
	 *  currently connected.
	 */
	public boolean checkObjectType(Object obj) 
	{
		if ( !(obj instanceof ISubSystem) ||
		     !((ISubSystem)obj).getConnectorService().isConnected() )
		  return false;
		else 
		  return true;
	}
	
	/**
	 * Called when this action is selection from the popup menu.
	 */
	public void run()	
	{		  
		ISubSystem ss = (ISubSystem)getFirstSelection();
		try {
		  ss.disconnect();
		} catch (Exception exc) {} // msg already shown		
	}
}
