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
 * David McKnight (IBM) - [193426] don't display exceptions
 *******************************************************************************/

package org.eclipse.dstore.core.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.eclipse.dstore.core.model.DE;
import org.eclipse.dstore.core.model.IDataStoreConstants;
import org.eclipse.dstore.core.model.ISSLProperties;
import org.eclipse.dstore.internal.core.server.ServerReturnCodes;
import org.eclipse.dstore.internal.core.server.ServerSSLProperties;
import org.eclipse.dstore.internal.core.util.ssl.DStoreSSLContext;

/**
 * This class is the DataStore daemon. It is used for authenticating users,
 * launching DataStore servers under particular user IDs, and providing a client
 * with enough information to conntect to a launched server.
 */
public class ServerLauncher extends Thread {

	/**
	 * An instances of this class get loaded whenever a client requests access
	 * to a DataStore server. The ConnectionListener attempts to launch a server
	 * under the client user's ID, communicating back information to the client
	 * so that if may connect to the launched server. If the authentification
	 * and connection to the server are successful, ConnectionListener continues
	 * to monitor the server connection until it is terminated.
	 */
	public class ConnectionListener extends Thread implements
			HandshakeCompletedListener {

		private Socket _socket;

		private PrintWriter _writer;

		private BufferedReader _reader;

		private Process _serverProcess;

		private String _port;

		private boolean _done;

		private BufferedReader _outReader;

		private BufferedReader _errReader;

		/**
		 * Constructor
		 * 
		 * @param socket
		 *            a socket to the daemon
		 */
		public ConnectionListener(Socket socket) {

			_socket = socket;
			try {
				_writer = new PrintWriter(new OutputStreamWriter(_socket
						.getOutputStream(), DE.ENCODING_UTF_8));
				_reader = new BufferedReader(new InputStreamReader(_socket
						.getInputStream(), DE.ENCODING_UTF_8));
			} catch (java.io.IOException e) {
				System.out.println("ServerLauncher:" + e); //$NON-NLS-1$
			}
		}

		/**
		 * Called when shutdown
		 */
		public void finalize() throws Throwable {
			if (_serverProcess != null) {
				_serverProcess.destroy();
			}
			super.finalize();
		}

		/**
		 * Listens to the connection and prints any output while the connection
		 * is active
		 */
		public void run() {
			_done = true;
			if (listen()) {
				if (_serverProcess != null) {
					_done = false;

					try {
						String line = null;

						while ((_outReader != null)
								&& ((line = _outReader.readLine()) != null)) {
							if (line.equals(ServerReturnCodes.RC_FINISHED)) {
								break;
							} else {
								System.out.println(line);
							}
						}

						if (_outReader != null) {
							_outReader.close();
						}
						if (_errReader != null) {
							_errReader.close();
						}

						_serverProcess.waitFor();
					} catch (Exception e) {
						System.out.println("ServerLauncher:" + e); //$NON-NLS-1$
					}
				}

				System.out.println("finished on port " + _port); //$NON-NLS-1$
				_outReader = null;
				_errReader = null;
				_serverProcess = null;
				_done = true;
			} else {
				_done = true;
			}
		}

		/**
		 * Indicates whether the connection has terminated or not
		 * 
		 * @return true if the connection has terminated
		 */
		public boolean isDone() {
			return _done;
		}

		/**
		 * Returns the DataStore server port used
		 * 
		 * @return the server port
		 */
		public String getServerPort() {
			return _port;
		}

		private boolean isPortInRange(String portStr, String portRange) {
			if (portRange != null) {
				String[] range = portRange.split("-"); //$NON-NLS-1$
				if (range.length == 2) {
					int lPort = 0;
					int hPort = 0;
					int port = 0;
					try {
						lPort = Integer.parseInt(range[0]);
						hPort = Integer.parseInt(range[1]);
						port = Integer.parseInt(portStr);
					} catch (Exception e) {
					}

					return (port >= lPort && port <= hPort);
				}
			}
			return true;
		}

		/**
		 * Attempt to start a new DataStore server. The port and the ticket for
		 * a newly started DataStore are captured and sent back to the client so
		 * that it may connect to the server.
		 * 
		 * @return whether the server started successfully
		 */
		public boolean listen()
		{
			boolean connected = false;

			String user = null;
			String password = null;

			_port = null;
			try
			{
				user = _reader.readLine();
				password = _reader.readLine();
				_port = _reader.readLine();
			}
			catch (IOException e)
			{
				// don't display exceptions 193426
				//e.printStackTrace();
				_port = "0"; //$NON-NLS-1$
			}

			
			
			if (_serverPortRange != null && (_port == null || _port.equals("0"))) //$NON-NLS-1$
			{
				_port = _serverPortRange;
			}		

			{
				boolean isError = false;
				if (_serverPortRange != null && _port != _serverPortRange)
				{
					if (!isPortInRange(_port, _serverPortRange))
					{
						String message = IDataStoreConstants.PORT_OUT_RANGE;
						message += _serverPortRange;
						_writer.println(message);
						isError = true;
					}
				}
				if (!isError)
				{
				// start new server
				try
				{
					String launchStatus = null;
					String ticket = new String("" + System.currentTimeMillis()); //$NON-NLS-1$

					String theOS = System.getProperty("os.name"); //$NON-NLS-1$
					String timeout = "120000"; //$NON-NLS-1$

							
					if (!theOS.toLowerCase().startsWith("win")) //$NON-NLS-1$
					{
						// assuming unix compatable
                        //
                        // Get the property which
                        // contains the authorization
                        // script path
                        //
						String authPath = System.getProperty("RSE.AUTH"); //$NON-NLS-1$
						File authFile = null;
						if (authPath != null && authPath.length() > 0)
						{
							authFile = new File(authPath);
						}
						if (authFile == null || !authFile.exists())
						{
							authPath = "perl " + _path + File.separator + "auth.pl"; //$NON-NLS-1$ //$NON-NLS-2$							
						}

						String authString =
								authPath
								+ " " //$NON-NLS-1$
								+ user
								+ " " //$NON-NLS-1$
								+ _path
								+ " " //$NON-NLS-1$
								+ _port
								+ " " //$NON-NLS-1$
								+ timeout
								+ " " //$NON-NLS-1$
								+ ticket
								+ " " //$NON-NLS-1$
								+ System.getProperty("java.home") //$NON-NLS-1$
								;

						String[] authArray = { "sh", "-c", authString }; //$NON-NLS-1$ //$NON-NLS-2$

						// test password
						_serverProcess = Runtime.getRuntime().exec(authArray);

						_outReader = new BufferedReader(new InputStreamReader(_serverProcess.getInputStream()));
						_errReader = new BufferedReader(new InputStreamReader(_serverProcess.getErrorStream()));
						BufferedWriter inWriter  = new BufferedWriter(new OutputStreamWriter(_serverProcess.getOutputStream()));
						// write password
						if (password != null)
						{
							inWriter.write(password);
							inWriter.newLine();
							inWriter.flush();
						
							launchStatus = _outReader.readLine();
						}
					}
					else
					{
			
						// launch new server
						String[] cmdArray =
							{
								"java", //$NON-NLS-1$
								"-DA_PLUGIN_PATH=" + _path, //$NON-NLS-1$
								"-DDSTORE_SPIRIT_ON=true", //$NON-NLS-1$
								"org.eclipse.dstore.core.server.Server", //$NON-NLS-1$
								_port,
								timeout,
								ticket};

						_serverProcess = Runtime.getRuntime().exec(cmdArray);
						_outReader = new BufferedReader(new InputStreamReader(_serverProcess.getInputStream()));
						_errReader = new BufferedReader(new InputStreamReader(_serverProcess.getErrorStream()));

						launchStatus = "success"; //$NON-NLS-1$
					}

					if ((launchStatus == null) || !launchStatus.equals("success")) //$NON-NLS-1$
					{
						_writer.println(IDataStoreConstants.AUTHENTICATION_FAILED);
					}
					else
					{
						// look for the server startup string, it needs to occur
						// somewhere in the line.
						String status = _errReader.readLine();
						while (status!=null && (status.indexOf(ServerReturnCodes.RC_DSTORE_SERVER_MAGIC) < 0))
						{
							status = _errReader.readLine();
						}
						// now read the real server status
						if (status != null)
						{
							status = _errReader.readLine();
						}
						if ((status != null) && status.equals(ServerReturnCodes.RC_SUCCESS))
						{
							_port = _errReader.readLine();
							_errReader.readLine();
							_writer.println(IDataStoreConstants.CONNECTED);
							_writer.println(_port);
							_writer.println(ticket);

							System.out.println("launched new server on " + _port); //$NON-NLS-1$
							connected = true;
						}
						else
						{
							if (status == null)
							{
								status = new String(IDataStoreConstants.UNKNOWN_PROBLEM);
							}
							// TODO Make sure that the client doesnt try
							// connecting forever
							_writer.println(status);

							_serverProcess.destroy();
							_serverProcess = null;
							_outReader.close();
							_outReader = null;

							_errReader.close();
							_errReader = null;
						}
					}
		
				}
				catch (IOException e)
				{
					_writer.println(IDataStoreConstants.SERVER_FAILURE + e);
				}
				}
			}

			_writer.flush();

			// close socket
			try
			{
				_socket.close();
			}
			catch (IOException e)
			{
				System.out.println("ServerLauncher:" + e); //$NON-NLS-1$
			}

			return connected;
		}

		public void handshakeCompleted(HandshakeCompletedEvent event) {
			System.out.println("handshake completed"); //$NON-NLS-1$
			System.out.println(event);

		}
	}

	private ServerSocket _serverSocket;

	private String _path;

	private ArrayList _connections;

	private String _serverPortRange;

	private ISSLProperties _sslProperties;

	public static int DEFAULT_DAEMON_PORT = 4075;

	/**
	 * Constructor
	 */
	public ServerLauncher() {
		String pluginPath = System.getProperty("A_PLUGIN_PATH"); //$NON-NLS-1$
		if (pluginPath == null) {
			System.out.println("A_PLUGIN_PATH is not defined"); //$NON-NLS-1$
			System.exit(-1);
		}
		else
		{
			_path = pluginPath.trim();

			_connections = new ArrayList();

			init(DEFAULT_DAEMON_PORT + ""); //$NON-NLS-1$
		}
	}

	/**
	 * Constructor
	 * 
	 * @param portStr
	 *            the port for the daemon socket to run on
	 */
	public ServerLauncher(String portStr) {
		String pluginPath = System.getProperty("A_PLUGIN_PATH"); //$NON-NLS-1$
		if (pluginPath == null) {
			System.out.println("A_PLUGIN_PATH is not defined"); //$NON-NLS-1$
			System.exit(-1);
		}
		else
		{
			_path = pluginPath.trim();

			_connections = new ArrayList();
			init(portStr);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param portStr
	 *            the port for the daemon socket to run on
	 * @param serverPortRange
	 *            the port range for launched servers
	 */
	public ServerLauncher(String portStr, String serverPortRange) {
		String pluginPath = System.getProperty("A_PLUGIN_PATH"); //$NON-NLS-1$
		if (pluginPath == null) {
			System.out.println("A_PLUGIN_PATH is not defined"); //$NON-NLS-1$
			System.exit(-1);
		}
		else
		{
			_path = pluginPath.trim();

			_serverPortRange = serverPortRange;
			_connections = new ArrayList();
			init(portStr);
		}
	}

	private String getKeyStoreLocation() {
		return _sslProperties.getDaemonKeyStorePath();
	}

	private String getKeyStorePassword() {
		return _sslProperties.getDaemonKeyStorePassword();
	}

	/**
	 * initializes the DataStore daemon
	 * 
	 * @param portStr the daemon port
	 */
	public void init(String portStr) {
		// create server socket from port
		_sslProperties = new ServerSSLProperties();

		// determine if portStr is a port range or just a port
		String[] range = portStr.split("-"); //$NON-NLS-1$
		if (range.length == 2) {
			int lPort = 0;
			int hPort = 0;
			try {
				lPort = Integer.parseInt(range[0]);
				hPort = Integer.parseInt(range[1]);
			} catch (Exception e) {
			}

			boolean socketBound = false;
			for (int i = lPort; i < hPort && !socketBound; i++) {
				// create server socket from port
				try {
					if (_sslProperties.usingSSL()) {
						String keyStoreFileName = getKeyStoreLocation();
						String keyStorePassword = getKeyStorePassword();

						try {
							SSLContext sslContext = DStoreSSLContext
									.getServerSSLContext(keyStoreFileName,
											keyStorePassword);

							_serverSocket = sslContext.getServerSocketFactory()
									.createServerSocket(i);
						} catch (Exception e) {
							// don't display exceptions 193426
							//e.printStackTrace();
						}
					} else {
						_serverSocket = new ServerSocket(i);
					}
					if (_serverSocket != null
							&& _serverSocket.getLocalPort() > 0) {
						socketBound = true;
						System.out.println("Daemon running on: " //$NON-NLS-1$
								+ InetAddress.getLocalHost().getHostName()
								+ ", port: " + i); //$NON-NLS-1$
					}
				} catch (UnknownHostException e) {
					System.err
							.println("Networking problem, can't resolve local host"); //$NON-NLS-1$
					// don't display exceptions 193426
					//e.printStackTrace();
					System.exit(-1);
				} catch (BindException e) {
					System.err.println("socket taken on " + i); //$NON-NLS-1$
					// keep going
				} catch (IOException e) {
					System.err.println("Failure to create ServerSocket"); //$NON-NLS-1$
					// don't display exceptions 193426
					//e.printStackTrace();
					System.exit(-1);
				}

			}
		} else {
			int port = Integer.parseInt(portStr);
			try {
				if (_sslProperties.usingSSL()) {
					String keyStoreFileName = getKeyStoreLocation();
					String keyStorePassword = getKeyStorePassword();

					try {
						SSLContext sslContext = DStoreSSLContext
								.getServerSSLContext(keyStoreFileName,
										keyStorePassword);

						_serverSocket = sslContext.getServerSocketFactory()
								.createServerSocket(port);
					} catch (Exception e) {
						// don't display exceptions 193426
						//e.printStackTrace();
					}
				} else {
					_serverSocket = new ServerSocket(port);
				}
				System.out.println("Daemon running on: " //$NON-NLS-1$
						+ InetAddress.getLocalHost().getHostName() + ", port: " //$NON-NLS-1$
						+ port);
			} catch (UnknownHostException e) {
				System.err
						.println("Networking problem, can't resolve local host"); //$NON-NLS-1$
				// don't display exceptions 193426
				//e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				System.err.println("Failure to create ServerSocket"); //$NON-NLS-1$
				// don't display exceptions 193426
				//e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	/**
	 * Return the connection listener for the specified port if there is one
	 * 
	 * @param port
	 *            the port
	 * @return the listener associated with the port
	 */
	protected ConnectionListener getListenerForPort(String port) {
		for (int i = 0; i < _connections.size(); i++) {
			ConnectionListener listener = (ConnectionListener) _connections
					.get(i);
			if (listener.getServerPort().equals(port)) {
				return listener;
			}
		}

		return null;
	}

	/**
	 * Run the daemon
	 */
	public void run() {
		while (true) {
			try {
				boolean connectionOkay = true;
				Socket newSocket = _serverSocket.accept();
				if (_sslProperties.usingSSL()) {

					SSLSocket sslSocket = (SSLSocket) newSocket;
					sslSocket
							.addHandshakeCompletedListener(new HandshakeCompletedListener() {

								public void handshakeCompleted(
										HandshakeCompletedEvent event) {
									System.out.println("handshake completed"); //$NON-NLS-1$
								}

							});
					SSLSession session = sslSocket.getSession();
					if (session == null) {
						System.out.println("handshake failed"); //$NON-NLS-1$

						sslSocket.close();
						connectionOkay = false;
					}
				}
				if (connectionOkay) {
					ConnectionListener listener = new ConnectionListener(
							newSocket);
					listener.start();
					_connections.add(listener);
				}
			} catch (IOException ioe) {
				System.err.println("Server: error initializing socket: " + ioe); //$NON-NLS-1$
				System.exit(-1);
			}
		}
	}

	/**
	 * Entry point into the DataStore daemon
	 * 
	 * @param args
	 *            the port for the daemon to run on (default is 4075).
	 *            Optionally, the second arg specifies whether to use SSL or
	 *            not. an optional second arg can be used to specify the port
	 *            range of servers that get launched
	 */
	public static void main(String args[]) {
		if (args.length == 2) {
			ServerLauncher theServer = new ServerLauncher(args[0], args[1]);
			theServer.start();
		} else if (args.length == 1) {
			ServerLauncher theServer = new ServerLauncher(args[0]);
			theServer.start();
		} else {
			ServerLauncher theServer = new ServerLauncher();
			theServer.start();
		}
	}
}
