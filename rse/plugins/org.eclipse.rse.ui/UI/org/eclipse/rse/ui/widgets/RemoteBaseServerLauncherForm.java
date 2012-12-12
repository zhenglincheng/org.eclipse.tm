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
 * David Dykstal (IBM) - refactoring IConnectorService and ServerLauncher hierarchies
 * Martin Oberhuber (Wind River) - [226364][api][breaking] RemoteBaseServerLauncherForm should not implement RemoteServerLauncherConstants.
 *******************************************************************************/

package org.eclipse.rse.ui.widgets;

import org.eclipse.rse.core.subsystems.IServerLauncherProperties;
import org.eclipse.rse.core.subsystems.ServerLaunchType;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBaseForm;
import org.eclipse.rse.ui.SystemWidgetHelpers;
import org.eclipse.rse.ui.messages.ISystemMessageLine;
import org.eclipse.rse.ui.propertypages.ISystemConnectionWizardErrorUpdater;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;


/**
 * Base Remote server launcher form.  Extend this to provide a specialized server launcher form
 */
public abstract class RemoteBaseServerLauncherForm extends SystemBaseForm implements IServerLauncherForm, ISystemConnectionWizardErrorUpdater
{
	protected String _hostName;

	protected ISystemMessageLine _msgLine;

	/**
	 * Constructor for EnvironmentVariablesForm.
	 * @param msgLine
	 */
	public RemoteBaseServerLauncherForm(Shell shell, ISystemMessageLine msgLine)
	{
		super(shell, msgLine);
		_msgLine = msgLine;
	}

	public abstract void disable();


	/**
	 * @see org.eclipse.rse.ui.SystemBaseForm#createContents(Composite)
	 */
	public Control createContents(Composite parent)
	{
		// server lanucher group
		Group group =createGroupControl(parent);

		// create launcher type controls
		createLauncherControls(group);

		// help
		SystemWidgetHelpers.setCompositeHelp(parent, RSEUIPlugin.HELPPREFIX + "srln0000"); //$NON-NLS-1$

		// initialization
		initDefaults();
		return parent;
	}

	protected Group createGroupControl(Composite parent)
	{
	    return SystemWidgetHelpers.createGroupComposite(
				parent,
				1,
				SystemResources.RESID_PROP_SERVERLAUNCHER_MEANS);

	}
	protected abstract void createLauncherControls(Group group);
	protected abstract ServerLaunchType getLaunchType();
	protected abstract void setLaunchType(ServerLaunchType type);




	protected abstract void initDefaults();



	/**
	 * Verify page contents on OK.
	 * @return true if all went well, false if error found.
	 */
	public abstract boolean verify();

	/**
	 * Update the actual values in the server launcher, from the widgets. Called on successful press of OK.
	 * @return true if all went well, false if something failed for some reason.
	 */
	public abstract boolean updateValues(IServerLauncherProperties launcher);


	public void setHostname(String hostname)
	{
	    _hostName = hostname;
	}
}