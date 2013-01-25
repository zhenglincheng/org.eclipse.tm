/********************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation. All rights reserved.
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
 * Noriaki Takatsu (IBM)  - [220126] [dstore][api][breaking] Single process server for multiple clients
 * David McKnight  (IBM)  - [226086] [dstore][api][breaking] Move ServerLogger class to dstore.core
 * Jacob Garcowski (IBM)  - [232738] [dstore] Delay creation of log file until written to
 * Noriaki Takatsu (IBM)  - [232443] [multithread] A single rsecomm.log for all clients
 * Noriaki Takatsu (IBM)  - [239419] [multithread] Dynamically change the level of logging
 * David McKnight  (IBM)  - [244876] [dstore] make DEBUG a non-final variable of the ServerLogger class
 ********************************************************************************/

package org.eclipse.dstore.core.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * Class that facilitates logging for errors, warnings, debug messages and info
 * for DataStore servers.
 *
 * @since 3.0 moved from non-API to API
 */
public class ServerLogger implements IServerLogger
{


	// Constants for logging - for use in rsecomm.properties
	private static final String DEBUG_LEVEL = "debug_level"; //$NON-NLS-1$
	private static final String LOG_LOCATION = "log_location"; //$NON-NLS-1$

	private static final int LOG_WARNING = 1;
	private static final int LOG_INFO = 2;
	private static final int LOG_DEBUG = 3;

	private static final String LOG_TO_STDOUT = "Log_To_StdOut"; //$NON-NLS-1$

	private Object writeLock = new Object();
	private PrintWriter _logFileStream = null;

	/**
	 * Switch to enable debug-level logging. Note that, in 3.0, this variable
	 * was final but, as of 3.0.1, it's not.
	 */
	public static boolean DEBUG = false;

	private int log_level = 0;

	private boolean initialized = false;
	private String logPathName = null;
	private boolean logToFile = true;

	/**
	 * Constructs a new ServerLogger.
	 *
	 * @param logPathName the path on the filesystem to store the log information
	 */
	public ServerLogger(String logPathName) {
		this.logPathName = logPathName;
		// Read .properties file to configure
		try {
			ResourceBundle properties = ResourceBundle.getBundle("rsecomm"); //$NON-NLS-1$
			String debug_level = properties.getString(DEBUG_LEVEL).trim();
			log_level = Integer.parseInt(debug_level);
			String log_location = properties.getString(LOG_LOCATION).trim();
			if (log_location.equalsIgnoreCase(LOG_TO_STDOUT)) {
				logToFile = false;
				_logFileStream = new PrintWriter(System.out);
			}
		} catch (Exception e) {
			// Just use logging defaults: log_level = 0, log to file
			//e.printStackTrace();
		}
	}

	private void initialize()
	{
		initialized = true;
		if (_logFileStream == null) {
			if (logToFile) {
				try {
			  		File _logFile = new File(logPathName, "rsecomm.log"); //$NON-NLS-1$

	 	 			if (!_logFile.exists()) {
	  					_logFile.createNewFile();
	  				}

	  				_logFileStream = new PrintWriter(new FileOutputStream(_logFile));

				} catch (IOException e) {
					System.out.println("Error opening log file " + logPathName + "rsecomm.log");		 //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}


	/**
	 * Logs an informational message
	 *
	 * @param minerName the name of the miner associated with this message
	 * @param message Message text to be logged.
	 */
	public void logInfo(String minerName, String message) {
		if (!initialized)
			initialize();
		String loggerLogLevel = System.getProperty("DSTORE_LOGGER_LOG_LEVEL"); //$NON-NLS-1$
		if (loggerLogLevel != null){
			try {
				log_level = Integer.parseInt(loggerLogLevel);
			}
			catch (NumberFormatException e){
				System.err.println("ServerLogger: "+e.toString()); //$NON-NLS-1$
			}
		}
		if (log_level >= LOG_INFO) {
			if (_logFileStream != null) {
				synchronized(writeLock) {
					try {
						_logFileStream.println(new Date());
						_logFileStream.println("INFO " + minerName + ": " + message); //$NON-NLS-1$ //$NON-NLS-2$
						_logFileStream.println("---------------------------------------------------------------"); //$NON-NLS-1$
						_logFileStream.flush();
					}catch (Exception e) {}
				}
			}
		}
	}


	/**
	 * Logs a warning message
	 *
	 * @param minerName the name of the miner associated with this message
	 * @param message Message text to be logged.
	 */
	public void logWarning(String minerName, String message) {
		if (!initialized)
			initialize();
		String loggerLogLevel = System.getProperty("DSTORE_LOGGER_LOG_LEVEL"); //$NON-NLS-1$
		if (loggerLogLevel != null){
			try {
				log_level = Integer.parseInt(loggerLogLevel);
			}
			catch (NumberFormatException e){
				System.err.println("ServerLogger: "+e.toString()); //$NON-NLS-1$
			}
		}
		if (log_level >= LOG_WARNING) {
			if (_logFileStream != null) {
				synchronized(writeLock) {
					try {
						_logFileStream.println(new Date());
						_logFileStream.println("WARNING " + minerName + ": " + message); //$NON-NLS-1$ //$NON-NLS-2$
						_logFileStream.println("---------------------------------------------------------------"); //$NON-NLS-1$
						_logFileStream.flush();
					}catch (Exception e) {}
				}
			}
		}
	}


	/**
	 * Logs an error message
	 *
	 * @param minerName the name of the miner associated with this message
	 * @param message Message text to be logged.
	 *
	 * @param exception Exception that generated the error.  Used to print a stack trace.
	 */
	public void logError(String minerName, String message, Throwable exception) {
		if (!initialized)
			initialize();
		if (_logFileStream != null) {
			synchronized(writeLock) {
				try {
					_logFileStream.println(new Date());
					_logFileStream.println("ERROR " + minerName + ": " + message); //$NON-NLS-1$ //$NON-NLS-2$
					if (exception != null) {
						exception.printStackTrace(_logFileStream);
					}
					_logFileStream.println("---------------------------------------------------------------"); //$NON-NLS-1$
					_logFileStream.flush();
				}catch (Exception e) {}
			}
		}
	}


	/**
	 * Logs a debug message
	 *
	 * @param minerName the name of the miner associated with this message
	 * @param message Message text to be logged.
	 */
	public synchronized void logDebugMessage(String minerName, String message) {
		if (!initialized)
			initialize();
		if (DEBUG && log_level == LOG_DEBUG) {
			if (_logFileStream != null) {
				synchronized(writeLock) {
					try {
						_logFileStream.println(new Date());
						_logFileStream.println("DEBUG " + minerName + ": " + message); //$NON-NLS-1$ //$NON-NLS-2$
						_logFileStream.println("---------------------------------------------------------------"); //$NON-NLS-1$
						_logFileStream.flush();
					}catch (Exception e) {}
				}
			}
		}
	}

}