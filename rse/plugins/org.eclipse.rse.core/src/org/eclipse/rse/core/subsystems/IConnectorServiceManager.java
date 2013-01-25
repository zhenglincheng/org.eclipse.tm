/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [cleanup] Add API "since" Javadoc tags
 *******************************************************************************/

package org.eclipse.rse.core.subsystems;

import org.eclipse.rse.core.model.IHost;

/**
 * Connector Service Manager Interface.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients should subclass {@link AbstractConnectorServiceManager}
 *              instead.
 */
public interface IConnectorServiceManager {
	IConnectorService getConnectorService(IHost host, Class commonSSinterface);

	void setConnectorService(IHost host, Class commonSSinterface, IConnectorService connectorService);
}
