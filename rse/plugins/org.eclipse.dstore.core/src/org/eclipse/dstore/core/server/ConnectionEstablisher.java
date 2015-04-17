/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
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
 * David McKnight  (IBM)   [220123][dstore] Configurable timeout on irresponsiveness
 * David McKnight  (IBM)   [220892][dstore] Backward compatibility: Server and Daemon should support old clients
 * Noriaki Takatsu (IBM)  - [220126] [dstore][api][breaking] Single process server for multiple clients
 * David McKnight     (IBM)   [224906] [dstore] changes for getting properties and doing exit due to single-process capability
 * Jacob Garcowski    (IBM)   [225175] [dstore] error handling change for Client
 * David McKnight   (IBM) - [225507][api][breaking] RSE dstore API leaks non-API types
 * Noriaki Takatsu  (IBM) - [226074] process for getStatus() API
 * Noriaki Takatsu  (IBM) - [226237] [dstore] Move the place where the ServerLogger instance is made
 * David McKnight   (IBM) - [226561] [apidoc] Add API markup to RSE Javadocs where extend / implement is allowed
 * Noriaki Takatsu  (IBM) - [242968] [multithread] serverSocket must be closed when an exception happens in Accept
 * David McKnight   (IBM) - [257321] [dstore] "Error binding socket" should include port of the failed socket
 * Noriaki Takatsu  (IBM) - [283656] [dstore][multithread] Serviceability issue
 * Noriaki Takatsu  (IBM) - [289678][api][breaking] ServerSocket creation in multiple IP addresses
 * David McKnight   (IBM) - [368072] [dstore][ssl] no exception logged upon bind error
 * David McKnight   (IBM) - [371401] [dstore][multithread] avoid use of static variables - causes memory leak after disconnect
 * David McKnight    (IBM) - [388472] [dstore] need alternative option for getting at server hostname
 * David McKnight   (IBM)  - [390681] [dstore] need to merge differences between HEAD stream and 3.2 in ConnectionEstablisher.finished()
 * David McKnight  (IBM)   [439545][dstore] potential deadlock on senders during shutdown
 * David McKnight  (IBM)   [464736][dstore] need methods to disable ciphers and protocols
 *******************************************************************************/

package org.eclipse.dstore.core.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.eclipse.dstore.core.model.Client;
import org.eclipse.dstore.core.model.DE;
import org.eclipse.dstore.core.model.DataElement;
import org.eclipse.dstore.core.model.DataStore;
import org.eclipse.dstore.core.model.DataStoreAttributes;
import org.eclipse.dstore.core.model.ISSLProperties;
import org.eclipse.dstore.internal.core.server.ServerAttributes;
import org.eclipse.dstore.internal.core.server.ServerCommandHandler;
import org.eclipse.dstore.internal.core.server.ServerReturnCodes;
import org.eclipse.dstore.internal.core.server.ServerSSLProperties;
import org.eclipse.dstore.internal.core.server.ServerUpdateHandler;
import org.eclipse.dstore.internal.core.util.ExternalLoader;
import org.eclipse.dstore.internal.core.util.Sender;
import org.eclipse.dstore.internal.core.util.ssl.DStoreSSLContext;

/**
 * ConnectionEstablisher is responsible for managing the server DataStore and
 * facilitating the communication between client and server DataStores.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 *
 * @since 3.0 moved from non-API to API
 */
public class ConnectionEstablisher
{


	private ServerSocket _serverSocket;
	private boolean _continue;

	private ArrayList _receivers;

	private ServerCommandHandler _commandHandler;
	private ServerUpdateHandler _updateHandler;

	private ServerAttributes _serverAttributes = new ServerAttributes();
	private DataStore _dataStore;

	private int _maxConnections;
	private int _timeout;
	private String _msg;
	

	private String[] _disabledCipherPatterns = null;
	private String[] _disabledProtocolPatterns = null;
	private String[] _enabledCiphers = null;
	private String[] _enabledProtocols = null;
	


	/**
	 * Creates the default ConnectionEstablisher.  Communication occurs
	 * on a default port, there is no timeout and no ticket is required
	 * for a client to work with the DataStore.
	 *
	 */
	public ConnectionEstablisher()
	{
		String port = _serverAttributes.getAttribute(DataStoreAttributes.A_HOST_PORT);
		setup(port, null, null);
	}

	/**
	 * Creates a ConnectionEstablisher.  Communication occurs
	 * on the specified port, there is no timeout and no ticket is required
	 * for a client to work with the DataStore.
	 *
	 * @param port the number of the socket port
	 */
	public ConnectionEstablisher(String port)
	{
		setup(port, null, null);
	}

	/**
	 * Creates a ConnectionEstablisher.  Communication occurs
	 * on the specified port, a timeout value indicates the idle wait
	 * time before shutting down, and no ticket is required
	 * for a client to work with the DataStore.
	 *
	 * @param port the number of the socket port
	 * @param timeout the idle duration to wait before shutting down
	 */
	public ConnectionEstablisher(String port, String timeout)
	{
		setup(port, timeout, null);
	}

	/**
	 * Creates a ConnectionEstablisher.  Communication occurs
	 * on the specified port, a timeout value indicates the idle wait
	 * time before shutting down, and ticket specified the required
	 * ticket for a client to present in order to work with the DataStore.
	 *
	 * @param port the number of the socket port
	 * @param timeout the idle duration to wait before shutting down
	 * @param ticket validation id required by the client to access the DataStore
	 */
	public ConnectionEstablisher(String port, String timeout, String ticket)
	{
		setup(port, timeout, ticket);
	}
	
	/**
	 * Creates a ConnectionEstablisher.  Communication occurs
	 * on the specified port and the specified IP address, 
	 * a timeout value indicates the idle wait time
	 * before shutting down, and ticket specified the required
	 * ticket for a client to present in order to work with the DataStore.
	 *
	 * @param port the number of the socket port
	 * @param backlog listen backlog
	 * @param bindAddr the local IP address to bind to
	 * @param timeout the idle duration to wait before shutting down
	 * @param ticket validation id required by the client to access the DataStore
	 * @since 3.2
	 */
	public ConnectionEstablisher(String port, int backlog, InetAddress bindAddr, String timeout, String ticket)
	{
		setup(port, backlog, bindAddr, timeout, ticket);
	}


	/**
	 * Starts the run loop for the ConnectionEstablisher.
	 */
	public void start()
	{
		run();
	}



	/**
	 * Returns the DataStore.
	 *
	 * @return the DataStore
	 */
	public DataStore getDataStore()
	{
		return _dataStore;
	}

	/**
	 * Return the Server port opened for this client
	 *
	 * @return the Server port opened for this client
	 */
	public int getServerPort()
	{
		if (_serverSocket != null)
		{
			return _serverSocket.getLocalPort();
		}

		return -1;
	}

	/**
	 * Return the connection status for this client
	 *
	 * * @return the connection status for this client
	 */
	public String getStatus()
	{
		return _msg;
	}

	/**
	 * Tells the connection establisher to clean up and shutdown
	 */
	public void finished(ServerReceiver receiver)
	{
		if (_dataStore.getClient() != null) {
			_dataStore.getClient().getLogger().logInfo(this.getClass().toString(), "ConnectionEstablisher.finished()"); //$NON-NLS-1$
		}
		
		if (_dataStore.getClient() != null) {
			_dataStore.getClient().getLogger().logInfo(this.getClass().toString(), "ConnectionEstablisher - removing receiver"); //$NON-NLS-1$
		}
		_receivers.remove(receiver);
		
		if (_dataStore.getClient() != null) {
			_dataStore.getClient().getLogger().logInfo(this.getClass().toString(), "ConnectionEstablisher - removing preference listener"); //$NON-NLS-1$
		}	
		_dataStore.removeDataStorePreferenceListener(receiver);
		//if (_receivers.size() == 0)
		{
			_continue = false;
			_commandHandler.finish();			
			
			if (_dataStore.getClient() != null) {
				_dataStore.getClient().getLogger().logInfo(this.getClass().toString(), "ConnectionEstablisher - finishing update handler"); //$NON-NLS-1$
			}
			_updateHandler.finish();
			
			if (_dataStore.getClient() != null) {
				_dataStore.getClient().getLogger().logInfo(this.getClass().toString(), "ConnectionEstablisher - removing sender"); //$NON-NLS-1$
			}
			_updateHandler.removeSenderWith(receiver.socket());
			
			if (_dataStore.getClient() != null) {
				_dataStore.getClient().getLogger().logInfo(this.getClass().toString(), "ConnectionEstablisher - finishing DataStore"); //$NON-NLS-1$
			}
			_dataStore.finish();
			System.out.println(ServerReturnCodes.RC_FINISHED);

			if (SystemServiceManager.getInstance().getSystemService() == null)
				System.exit(0);
		}
	}

	private void waitForConnections()
	{
		while (_continue == true)
		{
			try
			{
				if (_dataStore.usingSSL())
				{
					SSLServerSocket sslServerSocket = (SSLServerSocket)_serverSocket;
					// for security, disable ciphers and protocols we don't want
					disableCiphers(sslServerSocket);
					disableProtocols(sslServerSocket);
					
					// for security, enable only ciphers and protocols that are common
					enableCiphers(sslServerSocket);
					enableProtocols(sslServerSocket);
					
					logAvailableCiphersAndProtocols(sslServerSocket);
				}
				
				Socket newSocket = _serverSocket.accept();
				if (_dataStore.usingSSL())
				{

					// wait for connection
					SSLSocket sslSocket = (SSLSocket)newSocket;
					sslSocket.setUseClientMode(false);
					sslSocket.setNeedClientAuth(false);
					SSLSession session = sslSocket.getSession();

					if (session == null)
					{
						System.out.println("handshake failed"); //$NON-NLS-1$
						sslSocket.close();
						return;
					}
				}

				doHandShake(newSocket);
				newSocket.setKeepAlive(true);

				ServerReceiver receiver = new ServerReceiver(newSocket, this);
				_dataStore.addDataStorePreferenceListener(receiver);

				if (_dataStore.getClient() != null)
				     _dataStore.getClient().setServerReceiver(receiver);

				Sender sender = new Sender(newSocket, _dataStore);

				// add this connection to list of elements
				_receivers.add(receiver);
				_updateHandler.addSender(sender);

				receiver.start();

				if (_receivers.size() == 1)
				{
					_updateHandler.start();
					_commandHandler.start();
				}

				if (_receivers.size() == _maxConnections)
				{
					_continue = false;
					_serverSocket.close();

				}
			}
			catch (IOException ioe)
			{
				System.err.println(ServerReturnCodes.RC_CONNECTION_ERROR);
				System.err.println("Server: error initializing socket: " + ioe); //$NON-NLS-1$
				_msg = ioe.toString();
				try
				{
					_serverSocket.close();
				}
				catch (Throwable e)
				{
					
				}
				_continue = false;
			}
		}
	}




	private ServerSocket createSocket(String portStr, int backlog, InetAddress bindAddr) throws UnknownHostException
	{
		ServerSocket serverSocket = null;
		SSLContext sslContext = null;
		// port
		int port = 0;

		if (_dataStore.usingSSL())
		{
			String keyStoreFileName = _dataStore.getKeyStoreLocation();
			String keyStorePassword = _dataStore.getKeyStorePassword();

			try
			{
				sslContext = DStoreSSLContext.getServerSSLContext(keyStoreFileName, keyStorePassword);
			}
			catch (Exception e)
			{

			}
		}

		// determine if portStr is a port range or just a port
		String[] range = portStr.split("-"); //$NON-NLS-1$
		if (range.length == 2)
		{
			int lPort = 0;
			int hPort = 0;
			try
			{
				lPort = Integer.parseInt(range[0]);
				hPort = Integer.parseInt(range[1]);
			}
			catch (Exception e)
			{
			}

			for (int i = lPort; i < hPort; i++)
			{
				// create server socket from port
				try
				{
					if (_dataStore.usingSSL() && sslContext != null)
					{
						try
						{
							serverSocket = sslContext.getServerSocketFactory().createServerSocket(i, backlog, bindAddr);
						}
						catch (BindException e)
						{
							_msg = ServerReturnCodes.RC_BIND_ERROR  + " on port " + port + ": " + e.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
							System.err.println(_msg);
							_dataStore.trace(_msg);
						}
						catch (Exception e)
						{
						}
					}
					else
					{
						try
						{
							serverSocket = new ServerSocket(i, backlog, bindAddr);
						}						
						catch (BindException e)
						{
							_msg = ServerReturnCodes.RC_BIND_ERROR  + " on port " + port + ": " + e.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
							System.err.println(_msg);
							_dataStore.trace(_msg);
						}
						catch (Exception e)
						{
						}
					}
				}
				catch (Exception e)
				{
					_dataStore.trace(e);
				}
				if (serverSocket != null && serverSocket.getLocalPort() > 0)
				{
					return serverSocket;
				}
			}
			if (serverSocket == null){
				_msg = ServerReturnCodes.RC_BIND_ERROR  + " on ports " + portStr; //$NON-NLS-1$
				System.err.println(_msg); 				
			}
		}
		else
		{
			port = Integer.parseInt(portStr);


			// create server socket from port
			if (_dataStore.usingSSL() && sslContext != null)
			{
				try
				{
					serverSocket = sslContext.getServerSocketFactory().createServerSocket(port, backlog, bindAddr);
				}
				catch (BindException e){
					_msg = ServerReturnCodes.RC_BIND_ERROR  + " on port " + port + ": " + e.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
					System.err.println(_msg);
					_dataStore.trace(_msg);
				}
				catch (Exception e)
				{
					_dataStore.trace(e);
				}
			}
			else
			{
				try
				{
					serverSocket = new ServerSocket(port, backlog, bindAddr);
				}
				catch (BindException e){
					_msg = ServerReturnCodes.RC_BIND_ERROR  + " on port " + port + ": " + e.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
					System.err.println(_msg);
					_dataStore.trace(_msg);
				}
				catch (Exception e)
				{
					_dataStore.trace(e);
				}
			}
		}
		return serverSocket;
	}
	
	/**
	 * Create the DataStore and initializes it's handlers and communications.
	 *
	 * @param portStr the number of the socket port
	 * @param timeoutStr the idle duration to wait before shutting down
	 * @param ticketStr validation id required by the client to access the DataStore
	 */
	private void setup(String portStr, String timeoutStr, String ticketStr)
	{
		setup(portStr, 50, null, timeoutStr, ticketStr);
	}

	/**
	 * Create the DataStore and initializes it's handlers and communications.
	 *
	 * @param portStr the number of the socket port
	 * @param backlog listen backlog
	 * @param bindAddr the local IP address to bind to
	 * @param timeoutStr the idle duration to wait before shutting down
	 * @param ticketStr validation id required by the client to access the DataStore
	 */
	private void setup(String portStr, int backlog, InetAddress bindAddr, String timeoutStr, String ticketStr)
	{
		_maxConnections = 1;


		ArrayList loaders = new ArrayList();
		loaders.add(new ExternalLoader(getClass().getClassLoader(), "*")); //$NON-NLS-1$
		_commandHandler = new ServerCommandHandler(loaders);
		_updateHandler = new ServerUpdateHandler();

		ISSLProperties sslProperties = new ServerSSLProperties();

		_dataStore = new DataStore(_serverAttributes, _commandHandler, _updateHandler, null);
		_dataStore.setSSLProperties(sslProperties);

		DataElement ticket = _dataStore.getTicket();
		ticket.setAttribute(DE.A_NAME, ticketStr);

		_updateHandler.setDataStore(_dataStore);
		_commandHandler.setDataStore(_dataStore);

		if (SystemServiceManager.getInstance().getSystemService() == null)
		{
			Client client = new Client();
			_dataStore.setClient(client);
		     ServerLogger logger = new ServerLogger(_dataStore.getUserPreferencesDirectory());
    		client.setLogger(logger);
		}

		_receivers = new ArrayList();
		_continue = true;

		try
		{

			_serverSocket = createSocket(portStr, backlog, bindAddr);
			if (_serverSocket == null)
			{
				_continue = false;
			}
			else
			{
				// timeout
				if (timeoutStr != null)
				{
					_timeout = Integer.parseInt(timeoutStr);
				}
				else
				{
					_timeout = 120000;
				}

				if (_timeout > 0)
				{
					_serverSocket.setSoTimeout(_timeout);
				}

				System.err.println(ServerReturnCodes.RC_SUCCESS);
				System.err.println(_serverSocket.getLocalPort());
				_msg = ServerReturnCodes.RC_SUCCESS;
				System.err.println("Server running on: " + ServerAttributes.getHostName()); //$NON-NLS-1$
			}
		}
		catch (UnknownHostException e)
		{
			System.err.println(ServerReturnCodes.RC_UNKNOWN_HOST_ERROR + ':' + e.getMessage());
			_msg = ServerReturnCodes.RC_UNKNOWN_HOST_ERROR;
			_continue = false;
		}
	   catch (BindException e)
		{
			System.err.println(ServerReturnCodes.RC_BIND_ERROR + ':' + e.getMessage());
			_msg = ServerReturnCodes.RC_BIND_ERROR;
			_continue = false;
		}
		catch (IOException e)
		{
			System.err.println(ServerReturnCodes.RC_GENERAL_IO_ERROR + ':' + e.getMessage());
			_msg = ServerReturnCodes.RC_GENERAL_IO_ERROR;
			_continue = false;
		}
		catch (SecurityException e)
		{
			System.err.println(ServerReturnCodes.RC_SECURITY_ERROR + ':' + e.getMessage());
			_msg = ServerReturnCodes.RC_SECURITY_ERROR;
			_continue = false;
		}
	}

	private void run()
	{
		waitForConnections();
	}
 
	private void doHandShake(Socket socket)
	{
	   	try
	   	{
		BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), DE.ENCODING_UTF_8));
	   		PrintWriter writer = new PrintWriter(bwriter);

	   		String version = DataStoreAttributes.DATASTORE_VERSION;
	   		String preferenceVersion = System.getProperty("DSTORE_VERSION"); //$NON-NLS-1$
	   		if (preferenceVersion != null && preferenceVersion.length() > 0){
	   			version = preferenceVersion;
	   		}
			writer.println(version);
			writer.flush();
			
		   if (socket instanceof SSLSocket){ // log the protocol and cipher suite used
			   SSLSocket sslSocket = (SSLSocket)socket;
			   SSLSession session = sslSocket.getSession();
			   String protocol = session.getProtocol();		   
			   String cipherSuite = session.getCipherSuite();
			   IServerLogger logger = _dataStore.getClient().getLogger();

			   String cn = getClass().toString();
			   
			   logger.logInfo(cn, "SSL/TLS Protocol: "+protocol); //$NON-NLS-1$
			   logger.logInfo(cn, "SSL/TLS Cipher Suite: " + cipherSuite); //$NON-NLS-1$
		   }			
	   	}
	   	catch (IOException e)
	   	{
	   		if (_dataStore.getClient() != null) {
				_dataStore.getClient().getLogger().logError(this.getClass().toString(), e.toString(), e);
			}
	   		System.out.println(e);
	   	}

	}
	
	
	
	private void logAvailableCiphersAndProtocols(SSLServerSocket sslSocket){
	   IServerLogger logger = _dataStore.getClient().getLogger();

	   String cn = getClass().toString();
	   // list the supported and available ciphers and protocols
	   logger.logDebugMessage(cn, "SSL/TLS Enabled Cipher Suites:"); //$NON-NLS-1$
	   String[] enabledSuites = sslSocket.getEnabledCipherSuites();			   
	   for (int i = 0; i < enabledSuites.length; i++){
		   String suite = enabledSuites[i];
		   logger.logDebugMessage(cn, '\t' + suite);
	   }			   			   
	   
	   String[] supportedSuites = sslSocket.getSupportedCipherSuites();
	   logger.logDebugMessage(cn, "SSL/TLS Supported Cipher Suites:"); //$NON-NLS-1$	   
	   for (int i = 0; i < supportedSuites.length; i++){
		   String suite = supportedSuites[i];
		   logger.logDebugMessage(cn, '\t' + suite);
	   }		
	   
	   String[] enabledProtocols = sslSocket.getEnabledProtocols();
	   logger.logDebugMessage(cn, "SSL/TLS Enabled Protocols:"); //$NON-NLS-1$		   
	   for (int i = 0; i < enabledProtocols.length; i++){
		   String eprotocol = enabledProtocols[i];
		   logger.logDebugMessage(cn, '\t' + eprotocol);
	   }		
	   
	   String[] supportedProtocols = sslSocket.getSupportedProtocols();
	   logger.logDebugMessage(cn, "SSL/TLS Supported Protocols:"); //$NON-NLS-1$		   
	   for (int i = 0; i < supportedProtocols.length; i++){
		   String sprotocol = supportedProtocols[i];
		   logger.logDebugMessage(cn, '\t' + sprotocol);
	   }		
	}
	
	/**
	 * Specify cipher patterns to be disabled when using SSL sockets
	 * @param cipherPatterns regex patterns of ciphers to disable
	 */
	public void setDisabledCipherPatterns(String[] cipherPatterns){
		_disabledCipherPatterns = cipherPatterns;
	}
	
	/**
	 * Specify protocol patterns to be disabled when using SSL sockets
	 * @param protocolPatterns regex patterns of protocols to disable
	 */
	public void setDisabledProtocolPatterns(String[] protocolPatterns){
		_disabledProtocolPatterns = protocolPatterns;
	}
	
	/**
	 * Specify ciphers to be enabled when using SSL sockets
	 * @param ciphers to enable
	 */
	public void setEnabledCiphers(String[] ciphers){
		_enabledCiphers = ciphers;
	}
	
	/**
	 * Specify protocols to be enabled when using SSL sockets
	 * @param protocols to enable
	 */
	public void setEnabledProtocols(String[] protocols){
		_enabledProtocols = protocols;
	}
	
	private String[] filterNames(String[] inNames, String[] filters){
		List outNames = new ArrayList();
		for (int n = 0; n < inNames.length; n++){
			String inName = inNames[n];
			boolean match = false;
			for (int i = 0; i < filters.length && !match; i++){
				String filter = filters[i];
				match = inName.matches(filter);
			}
			if (!match){
				outNames.add(inName);
			}
			else {
				String cn = getClass().toString();
				IServerLogger logger = _dataStore.getClient().getLogger();
				logger.logDebugMessage(cn, "Filtering out: " + inName); //$NON-NLS-1$	
			}
		}
		return (String[])outNames.toArray(new String[outNames.size()]);
	}
	
	private void disableCiphers(SSLServerSocket socket){
		if (_disabledCipherPatterns != null){
			String[] enabledSuites = socket.getEnabledCipherSuites();
			String[] newEnabledSuites = filterNames(enabledSuites, _disabledCipherPatterns);			
			
			socket.setEnabledCipherSuites(newEnabledSuites);
		}
	}
	
	private void disableProtocols(SSLServerSocket socket){
		if (_disabledProtocolPatterns != null){
			String[] enabledProtocols = socket.getEnabledProtocols();
			String[] newEnabledProtocols = filterNames(enabledProtocols, _disabledProtocolPatterns);
			socket.setEnabledProtocols(newEnabledProtocols);
		}
	}

	private String[] mergeCommon(String[] inNames1, String[] inNames2){
		List merged = new ArrayList();
		for (int n = 0; n < inNames1.length; n++){
			String inName1 = inNames1[n];
			boolean match = false;
			for (int i = 0; i < inNames2.length && !match; i++){
				match = inName1.equals(inNames2[i]);
			}
			if (match){
				merged.add(inName1);
			}
		}
		return (String[])merged.toArray(new String[merged.size()]);
	}
	
	private void enableCiphers(SSLServerSocket socket){
		if (_enabledCiphers != null){
			String[] enabledSuites = socket.getEnabledCipherSuites();
			String[] newEnabledSuites = mergeCommon(enabledSuites, _enabledCiphers);
			if (newEnabledSuites.length > 0){				
				socket.setEnabledCipherSuites(newEnabledSuites);
			}
		}
	}

	private void enableProtocols(SSLServerSocket socket){
		if (_enabledProtocols != null){
			String[] enabledProtocols = socket.getEnabledProtocols();
			String[] newEnabledProtocols = mergeCommon(enabledProtocols, _enabledProtocols);
			if (newEnabledProtocols.length > 0){
				socket.setEnabledCipherSuites(newEnabledProtocols);
			}
		}
	}
}
