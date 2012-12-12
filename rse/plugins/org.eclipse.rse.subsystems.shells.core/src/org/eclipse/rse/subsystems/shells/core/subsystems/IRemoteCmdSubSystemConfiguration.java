/********************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - Get rid of invalid Javadoc
 ********************************************************************************/

package org.eclipse.rse.subsystems.shells.core.subsystems;

import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;

public interface IRemoteCmdSubSystemConfiguration extends ISubSystemConfiguration
{

	/**
	 * Return true if subsystems of this factory support the environment variables property.
	 * Return true to show it, return false to hide it.
	 * @return <code>true</code> if environment variables are fully supported
	 */
	public boolean supportsEnvironmentVariablesPropertyPage();


	/**
	 * Return in string format the character used to separate commands. Eg, ";" or "&"
	 * @return command separator String
	 */
    public String getCommandSeparator();


}