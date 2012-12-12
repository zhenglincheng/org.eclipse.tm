/*******************************************************************************
 *  Copyright (c) 2006, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Initial Contributors:
 *  The following IBM employees contributed to the Remote System Explorer
 *  component that contains this file: David McKnight, Kushal Munir, 
 *  Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 *  Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 *  Contributors:
 *  Martin Oberhuber (Wind River) - Adapted template for ssh service.
 *  Anna Dushistova  (MontaVista) - [259414][api] refactor the "SSH Shell" to use the generic Terminal->IHostShell converter
 *******************************************************************************/

package org.eclipse.rse.subsystems.shells.ssh;

import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.internal.connectorservice.ssh.SshConnectorService;
import org.eclipse.rse.internal.connectorservice.ssh.SshConnectorServiceManager;
import org.eclipse.rse.internal.services.ssh.ISshService;
import org.eclipse.rse.internal.services.ssh.terminal.SshTerminalService;
import org.eclipse.rse.internal.subsystems.shells.ssh.SshServiceCommandShell;
import org.eclipse.rse.services.shells.IHostShell;
import org.eclipse.rse.services.shells.IShellService;
import org.eclipse.rse.subsystems.shells.core.subsystems.IRemoteCmdSubSystem;
import org.eclipse.rse.subsystems.shells.core.subsystems.servicesubsystem.IServiceCommandShell;
import org.eclipse.rse.subsystems.shells.core.subsystems.servicesubsystem.ShellServiceSubSystem;
import org.eclipse.rse.subsystems.shells.core.subsystems.servicesubsystem.ShellServiceSubSystemConfiguration;

public class SshShellSubSystemConfiguration extends
		ShellServiceSubSystemConfiguration {

	public SshShellSubSystemConfiguration() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.SubSystemConfiguration#isFactoryFor(java.lang.Class)
	 */
	public boolean isFactoryFor(Class subSystemType) {
		boolean isFor = ShellServiceSubSystem.class.equals(subSystemType);
		return isFor;
	}

	/**
	 * Instantiate and return an instance of OUR subsystem. 
	 * Do not populate it yet though!
	 * @see org.eclipse.rse.core.subsystems.SubSystemConfiguration#createSubSystemInternal(IHost)
	 */
	public ISubSystem createSubSystemInternal(IHost host) 
	{
		SshConnectorService connectorService = (SshConnectorService)getConnectorService(host);
		ISubSystem subsys = new ShellServiceSubSystem(host, connectorService, createShellService(host));
		return subsys;
	}

	public IShellService createShellService(IHost host) {
		SshConnectorService cserv = (SshConnectorService)getConnectorService(host);
		return (IShellService) (new SshTerminalService(cserv)).getAdapter(IShellService.class);
	}

	public IConnectorService getConnectorService(IHost host) {
		return SshConnectorServiceManager.getInstance().getConnectorService(host, ISshService.class);
	}

	public void setConnectorService(IHost host,
			IConnectorService connectorService) {
		SshConnectorServiceManager.getInstance().setConnectorService(host, ISshService.class, connectorService);
	}

	public Class getServiceImplType() {
		return ISshService.class;
	}

	public IServiceCommandShell createRemoteCommandShell(IRemoteCmdSubSystem cmdSS, IHostShell hostShell) {		
		return new SshServiceCommandShell(cmdSS, hostShell);
	}

}
