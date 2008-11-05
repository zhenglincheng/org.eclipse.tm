/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
 * David McKnight   (IBM)        - [251626] Backport [dstore] shell output readers not cleaned up on disconnect
 *******************************************************************************/

package org.eclipse.rse.internal.services.dstore.shells;

import org.eclipse.dstore.core.model.DE;
import org.eclipse.dstore.core.model.DataElement;
import org.eclipse.dstore.core.model.DataStore;
import org.eclipse.rse.services.dstore.util.DStoreStatusMonitor;
import org.eclipse.rse.services.shells.AbstractHostShell;
import org.eclipse.rse.services.shells.IHostShell;
import org.eclipse.rse.services.shells.IHostShellOutputReader;


public class DStoreHostShell extends AbstractHostShell implements IHostShell
{
	private DStoreShellThread _shellThread;
	private IHostShellOutputReader _stdoutHandler;
	private IHostShellOutputReader _stderrHandler;
	private DataElement _status;
	private DStoreStatusMonitor _statusMonitor;
	
	public DStoreHostShell(DStoreStatusMonitor statusMonitor, DataStore dataStore, String initialWorkingDirectory, String invocation, String encoding, String[] environment)
	{
		_shellThread = new DStoreShellThread(dataStore, initialWorkingDirectory, invocation, encoding, environment);	
		_status = _shellThread.getStatus();
		_stdoutHandler = new DStoreShellOutputReader(this, _status, false);
		_stderrHandler = new DStoreShellOutputReader(this, _status,true);	
		_statusMonitor = statusMonitor;
	}
	
	public boolean isActive()
	{
		return !_statusMonitor.determineStatusDone(_status);
	}

	public void writeToShell(String command)
	{
		_shellThread.writeToShell(command);
	}

	public IHostShellOutputReader getStandardOutputReader()
	{
		return _stdoutHandler;
	}

	public IHostShellOutputReader getStandardErrorReader()
	{
		return _stderrHandler;
	}
	
	public DataElement getStatus()
	{
		return _status;
	}
	
	public void exit()
	{
		writeToShell("exit"); //$NON-NLS-1$		
		_status.setAttribute(DE.A_VALUE, "done"); //$NON-NLS-1$
		_stdoutHandler.finish();
		_stderrHandler.finish();				
	}

	
}
