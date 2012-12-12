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

package org.eclipse.dstore.internal.core.server;

import java.util.ResourceBundle;

import org.eclipse.dstore.core.model.ISSLProperties;


public class ServerSSLProperties implements ISSLProperties
{
	private boolean _enableSSL = false;
	private boolean _disableServerSSL = false;
	
	private String _daemonKeyStorePath;
	private String _daemonKeyStorePassword;

	private String _serverKeyStorePath;
	private String _serverKeyStorePassword;

	
	private static final String ENABLE_SSL    = "enable_ssl";	 //$NON-NLS-1$
	private static final String DISABLE_SERVER_SSL = "disable_server_ssl"; //$NON-NLS-1$
	
	private static final String DAEMON_KEYSTORE_FILE = "daemon_keystore_file"; //$NON-NLS-1$
	private static final String DAEMON_KEYSTORE_PASSWORD = "daemon_keystore_password"; //$NON-NLS-1$

	private static final String SERVER_KEYSTORE_FILE = "server_keystore_file"; //$NON-NLS-1$
	private static final String SERVER_KEYSTORE_PASSWORD = "server_keystore_password"; //$NON-NLS-1$
		
	
	public ServerSSLProperties() 
	{
		try 
		{ 
			ResourceBundle properties = ResourceBundle.getBundle("ssl"); //$NON-NLS-1$
			_enableSSL = properties.getString(ENABLE_SSL).trim().equals("true"); //$NON-NLS-1$
			if (_enableSSL)
			{
				try
				{
					_disableServerSSL = properties.getString(DISABLE_SERVER_SSL).trim().equals("true"); //$NON-NLS-1$
				}
				catch (Exception e)
				{
					
				}
				
				try
				{
					_daemonKeyStorePath = properties.getString(DAEMON_KEYSTORE_FILE).trim();
					_daemonKeyStorePassword = properties.getString(DAEMON_KEYSTORE_PASSWORD).trim();
				}
				catch (Exception e)
				{					
				}
				
				if (!_disableServerSSL)
				{
					try
					{
						_serverKeyStorePath = properties.getString(SERVER_KEYSTORE_FILE).trim();
						_serverKeyStorePassword = properties.getString(SERVER_KEYSTORE_PASSWORD).trim();			
					}
					catch (Exception e)
					{
					}
				}
				
				if (_daemonKeyStorePath == null && _serverKeyStorePath != null)
				{
					_daemonKeyStorePath = _serverKeyStorePath;
					_daemonKeyStorePassword = _serverKeyStorePassword;
				}
				if (!_disableServerSSL && _serverKeyStorePath == null && _daemonKeyStorePath != null)
				{
					_serverKeyStorePath = _daemonKeyStorePath;
					_serverKeyStorePassword = _daemonKeyStorePassword;
				}
				
			}
			
			if (_enableSSL)
			{
				System.out.println("SSL Settings"); //$NON-NLS-1$
				System.out.println("[daemon keystore:\t"+_daemonKeyStorePath+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				System.out.println("[daemon keystore pw:\t"+_daemonKeyStorePassword+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				if (!_disableServerSSL)
				{
					System.out.println("[server keystore:\t"+_serverKeyStorePath+"]"); //$NON-NLS-1$ //$NON-NLS-2$
					System.out.println("[server keystore pw:\t"+_serverKeyStorePassword+"]");					 //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	
	public boolean usingSSL()
	{
		return _enableSSL;
	}
	
	public boolean usingServerSSL()
	{
		return !_disableServerSSL;
	}
	
	
	public String getDaemonKeyStorePath()
	{
		return _daemonKeyStorePath;
	}
	
	public String getServerKeyStorePath()
	{
		return _serverKeyStorePath;
	}
	
	public String getDaemonKeyStorePassword()
	{
		return _daemonKeyStorePassword;
	}
	
	public String getServerKeyStorePassword()
	{
		return _serverKeyStorePassword;
	}

}
