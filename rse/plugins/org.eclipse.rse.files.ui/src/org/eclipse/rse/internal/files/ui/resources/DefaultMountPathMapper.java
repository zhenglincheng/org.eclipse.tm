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
 * David McKnight   (IBM) - [195285] mount path mapper changes
 *******************************************************************************/

package org.eclipse.rse.internal.files.ui.resources;

import org.eclipse.rse.files.ui.resources.ISystemMountPathMapper;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;

public class DefaultMountPathMapper implements ISystemMountPathMapper
{

	public boolean handlesMappingFor(String hostname, String remotePath, IRemoteFileSubSystem subsystem)
	{
		return false;
	}
	
	public String getActualHostFor(String hostname, String remotePath)
	{
		return hostname;
	}
	
	public String getWorkspaceMappingFor(String hostname, String remotePath, IRemoteFileSubSystem subSystem)
	{
		return remotePath;
	}
	
	/**
	 * Returns the remote path.  
	 */
	public String getMountedMappingFor(String hostname, String remotePath)
	{
		return remotePath;
	}

	public int getPriority(String hostname, String remotePath,
			IRemoteFileSubSystem subsystem) {
		return Integer.MAX_VALUE;
	}
}
