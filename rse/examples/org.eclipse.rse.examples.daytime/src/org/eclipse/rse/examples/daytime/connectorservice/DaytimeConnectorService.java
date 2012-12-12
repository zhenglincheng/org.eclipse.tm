/********************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and Wind River Systems, Inc.
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
 * Martin Oberhuber (Wind River) - adapted template for daytime example.
 * David Dykstal (IBM) - 168977: refactoring IConnectorService and ServerLauncher hierarchies
 ********************************************************************************/

package org.eclipse.rse.examples.daytime.connectorservice;

import java.net.ConnectException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.BasicConnectorService;
import org.eclipse.rse.examples.daytime.DaytimeResources;
import org.eclipse.rse.examples.daytime.service.DaytimeService;
import org.eclipse.rse.examples.daytime.service.IDaytimeService;

/**
 * The DaytimeConnectorService takes care of keeping a "session" for accessing
 * the remote host to retrieve the time of day.
 * 
 * Since the daytime service is really connectionless, there is not much to do
 * here. We basically keep a local "connected" flag only, so to make sure that
 * the remote host is only accessed when the user explicitly requested so. 
 */
public class DaytimeConnectorService extends BasicConnectorService {
	
	private boolean fIsConnected = false;
	private DaytimeService fDaytimeService;

	public DaytimeConnectorService(IHost host) {
		super(DaytimeResources.Daytime_Connector_Name, DaytimeResources.Daytime_Connector_Description, host, 13);
		fDaytimeService = new DaytimeService();
	}

	protected void internalConnect(IProgressMonitor monitor) throws Exception {
		fDaytimeService.setHostName(getHostName());
		try {
			fDaytimeService.getTimeOfDay();
		} catch (ConnectException e) {
			String message = NLS.bind(DaytimeResources.DaytimeConnectorService_NotAvailable, getHostName());
			throw new Exception(message);
		}
		//if no exception is thrown, we consider ourselves connected!
		fIsConnected = true;
		// Fire comm event to signal state changed -- 
		// Not really necessary since SubSystem.connect(Shell, boolean) does
		// SystemRegistry.connectedStatusChange(this, true, false) at the end
		notifyConnection();
	}

	public IDaytimeService getDaytimeService() {
		return fDaytimeService;
	}
	
	public boolean isConnected() {
		return fIsConnected;
	}

	protected void internalDisconnect(IProgressMonitor monitor) throws Exception {
		fIsConnected = false;
	}
	
}
