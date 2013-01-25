/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
 * David McKnight   (IBM)        - [223103] [cleanup] fix broken externalized strings
 *******************************************************************************/

package org.eclipse.rse.internal.ui.view.monitor;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;





public class ClearSelectedAction extends BrowseAction
{
	public ClearSelectedAction(SystemMonitorViewPart view)
	{
		super(view, SystemResources.ACTION_CLEAR_SELECTED_LABEL,
		        RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_CLEAR_SELECTED_ID));
	
		setToolTipText(SystemResources.ACTION_CLEAR_SELECTED_TOOLTIP);
		// TODO DKM - get help for this!
		//PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.CLEAR_CONSOLE_ACTION);
	}

	public void checkEnabledState()
	{
	    if (part.getViewer() != null)
	    {	       
	    	setEnabled(true);
		    return;
	    }

	    setEnabled(false);			    
	}

	public void run()
	{
		clear();
	}

	private void clear()
	{
		part.removeItemToMonitor((IAdaptable)part.getViewer().getInput());
	}
}
