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
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 ********************************************************************************/

package org.eclipse.rse.internal.ui.actions;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.model.ISystemProfileManager;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.internal.core.model.SystemProfileManager;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.swt.widgets.Shell;


/**
 * A selectable profile name action.
 */
public class SystemProfileNameSelectAction extends SystemBaseAction 
{	
	private ISystemProfile profile;
	
	/**
	 * Constructor
	 */
	public SystemProfileNameSelectAction(Shell parent, ISystemProfile profile) 
	{
		super(profile.getName(),parent);
		this.profile = profile;
		ISystemProfileManager mgr = SystemProfileManager.getDefault();
		setChecked(mgr.isSystemProfileActive(profile.getName()));
        setSelectionSensitive(false);
        
		setHelp(RSEUIPlugin.HELPPREFIX+"actn0004"); //$NON-NLS-1$
	}


	/**
	 * This is the method called when the user selects this action.
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() 
	{
		ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
		sr.setSystemProfileActive(profile, isChecked());
	}		
}