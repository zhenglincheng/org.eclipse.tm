/*******************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others.
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
 * Javier Montalvo Orús (Symbian)- [138619] Fix codepage on Win2K
 * Lothar Werzinger (Tradescape) - [161838] Support terminating local shells
 * David McKnight       (IBM)    - [189387] Use specified encoding for shell output
 * Martin Oberhuber (Wind River) - [161838] local shell reports isActive() wrong
 * Anna Dushistova  (MontaVsita) - [249354] Incorrect behaviour of local shells subsystem runCommand method 
 *******************************************************************************/

package org.eclipse.rse.internal.services.local.shells;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;

/**
 * The LocalCommandThread class is used for running and interacting with a
 * local command shell.
 */
public class LocalShellThread extends Thread
{

	private volatile Thread _commandThread;
	protected boolean _isCancelled;

	private String _cwd;
	private String _invocation;
	private String[] _envVars;
	private String PSEUDO_TERMINAL;


	private boolean _isShell;
	private boolean _isDone;

	private Process _theProcess;


	private boolean _isTTY = false;

	private boolean _isWindows;
	private String  _encoding;

	private BufferedReader _stdInput;
	private BufferedReader _stdError;

	/**
	 * constructor for local command shell monitor
	 * 
	 * @param cwd initial working directory
	 * @param invocation launch shell command
	 * @param encoding encoding to use or <code>null</code> for default
	 * @param envVars user and system environment variables to launch shell with
	 */
	public LocalShellThread(String cwd, String invocation, String encoding, String[] envVars)
	{
		super();
		_encoding = encoding;
		_isCancelled = false;
		_cwd = cwd;
		_invocation = invocation;

		// if pty exists for this client
		// then the rse.pty property will have been set
		// by the contributor of the pty exectuable
		// on linux client this is a likely scenario
		PSEUDO_TERMINAL = System.getProperty("rse.pty"); //$NON-NLS-1$
		if (PSEUDO_TERMINAL != null) {
			try {
				PSEUDO_TERMINAL = FileLocator.resolve(new URL(PSEUDO_TERMINAL)).getPath();
			} catch (Exception e) {
				/* ignore, no pty available */
			}
		}

		_envVars = envVars;
		init();
	}


	public boolean isShell()
	{
		return _isShell;
	}

	public boolean isWindows()
	{
		return _isWindows;
	}

	public boolean isDone()
	{
		return _isDone || _isCancelled;
	}

	public String getInvocation()
	{
		return _invocation;
	}

	public String getCWD()
	{
		return _cwd;
	}

	public void setCWD(String cwd)
	{
		_cwd = cwd;
	}

	private void init()
	{
		try
		{
			File theDirectory = new File(_cwd);
			if (!theDirectory.isDirectory())
				theDirectory = theDirectory.getParentFile();
			String theOS = System.getProperty("os.name"); //$NON-NLS-1$
			_isWindows = theOS.toLowerCase().startsWith("win"); //$NON-NLS-1$
			_isTTY = PSEUDO_TERMINAL != null && (new File(PSEUDO_TERMINAL).exists());

			String theShell = null;

			if (!_isWindows)
			{
				String[] envVars = getEnvironmentVariables(false);

				{

					String property = "SHELL="; //$NON-NLS-1$

					for (int i = 0; i < envVars.length; i++)
					{
						String var = envVars[i];
						if (var.startsWith(property))
						{
							theShell = var.substring(property.length(), var.length());

							if (theShell.endsWith("bash")) //$NON-NLS-1$
							{
								theShell = "sh"; //$NON-NLS-1$
							}

						}
					}

					if (theShell == null)
					{
						theShell = "sh"; //$NON-NLS-1$
					}


				    if (_isTTY)
				    {
				        if (_invocation.equals(">")) //$NON-NLS-1$
						{
							_invocation = theShell;
							_isShell = true;
						}

						String args[] = new String[2];
						args[0] = PSEUDO_TERMINAL;
						args[1] = _invocation;

						_theProcess = Runtime.getRuntime().exec(args, envVars, theDirectory);
				    }
				    else
				    {
				    	String args[];
						if (_invocation.equals(">")) //$NON-NLS-1$
						{
							_invocation = theShell;
							_isShell = true;
							args = new String[1];
							args[0] = _invocation;
							_theProcess = Runtime.getRuntime().exec(args[0], envVars, theDirectory);
						} else {	
							args = new String[3];
							args[0] = theShell;
							args[1] = "-c";//$NON-NLS-1$
							args[2] = _invocation;
						   _theProcess = Runtime.getRuntime().exec(args, envVars, theDirectory);
						}
				    }
				}

			}
			else
			{
				String[] envVars = getEnvironmentVariables(true);
				if ((theOS.indexOf("95") >= 0) || (theOS.indexOf("98") >= 0) || (theOS.indexOf("ME") >= 0)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{
					theShell = "start"; //$NON-NLS-1$
				}
				else
				{
					theShell = "cmd"; //$NON-NLS-1$
				}

				if (_invocation.equals(">")) //$NON-NLS-1$
				{
					_invocation = theShell;
					_isShell = true;
				}

				if (theShell.equals("start")) //$NON-NLS-1$
				{
					theShell += " /B "; //$NON-NLS-1$
				}
				else
				{
					theShell += " /C "; //$NON-NLS-1$
				}

				_theProcess = Runtime.getRuntime().exec(theShell + _invocation, envVars, theDirectory);
			}

			// determine the windows encoding
			if (_encoding == null || _encoding.length() == 0)
			{
				try
				{
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					OutputStreamWriter osw = new OutputStreamWriter(os);
					_encoding = osw.getEncoding();
					osw.close();
					os.close();
				}
				catch (Exception x)
				{
				}
				if (_encoding == null)
				{
					if (_encoding == null || _encoding.length() == 0)
					{
						_encoding = System.getProperty("file.encoding"); //$NON-NLS-1$
					}
				}
			}

			_stdInput = new BufferedReader(new InputStreamReader(_theProcess.getInputStream(), _encoding));

			_stdError = new BufferedReader(new InputStreamReader(_theProcess.getErrorStream()));

		}
		catch (IOException e)
		{
			_theProcess = null;
			e.printStackTrace();
			return;
		}


		if (_isShell && !_isWindows && !_isTTY)
		{
			OutputStream output = _theProcess.getOutputStream();

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
			createPrompt(writer);
			//createObject("prompt", _cwdStr + ">");
		}
	}

	private void createPrompt(BufferedWriter writer)
	{

		try
		{
			writer.write("echo $PWD'>'"); //$NON-NLS-1$
			writer.write('\n');
			writer.flush();
		}
		catch (Exception e)
		{

		}

	}

	public BufferedReader getOutputStream()
	{
		return _stdInput;
	}

	public BufferedReader getErrorStream()
	{
		return _stdError;
	}



	public synchronized void stopThread()
	{
		if (_commandThread != null)
		{
			_isCancelled = true;

			try
			{
				_commandThread = null;
			}
			catch (Exception e)
			{
				System.out.println(e);
			}

		}
		notify();
	}

	public void sendInput(String input)
	{
		if (!_isDone)
		{
			OutputStream output = _theProcess.getOutputStream();

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

			try
			{

				writer.write(input);
				writer.write('\n');
				writer.flush();

				if (!_isWindows && !_isTTY)
				{
					// special case for pattern interpretting
					// if cwd is not set, then files aren't resolved
					// create mock prompt to ensure that they do get resolved
					//if (input.startsWith("cd ") || input.equals("cd"))
					{
						createPrompt(writer);
						/*
						writer.write("echo $PWD'>'");
						writer.write('\n');
						writer.flush();
*/
						// sleep to allow reader to interpret before going on
						try
						{
							Thread.sleep(100);
						}
						catch (InterruptedException e)
						{
						}
					}
				}
			}
			catch (IOException e)
			{
				//MOB: Exception is expected when the process is already dead
				//System.out.println(e);

		        // make the thread exit;
		        _isShell = false;
			}

		}
	}

	public void run()
	{
		Thread thisThread = Thread.currentThread();
		_commandThread = thisThread;


		while (_commandThread != null && _commandThread == thisThread && _commandThread.isAlive() && !_isCancelled)
		{
			try
			{
				Thread.sleep(200);
			}
			catch (InterruptedException e)
			{
				//System.out.println(e);
				_isCancelled = true;
			}

			//This function is where the Threads do real work, and return false when finished
			if (!doThreadedWork())
			{
				try
				{
					_commandThread = null;
				}
				catch (Exception e)
				{
					System.out.println(e);
				}
			}
			else
			{
			}
		}

		//This function lets derived classes cleanup or whatever
		cleanupThread();
	}

	public boolean doThreadedWork()
	{
		if (_stdInput == null || _isShell == false)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	public void cleanupThread()
	{
		if (_isShell)
		{
			sendInput("exit"); //$NON-NLS-1$
		}

		_isDone = true;
		try
		{
			_stdInput.close();
			_stdError.close();

			if (_theProcess != null)
			{

				try
				{
					if (_isCancelled)
					{
						_theProcess.destroy();
					}
					else
					{
					 _theProcess.exitValue();
					}
				}
				catch (IllegalThreadStateException e)
				{
					//e.printStackTrace();
					_theProcess.destroy();
				}
				_theProcess = null;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}


	public String getPathEnvironmentVariable()
	{
		String[] vars = _envVars;
		if (vars != null)
		{

			for (int i = 0; i < vars.length; i++)
			{
				String var = vars[i].toUpperCase();
				if (var.startsWith("PATH=")) //$NON-NLS-1$
				{
					return var;
				}
			}

		}
		return null;
	}

	/**
	 * Retrieve the system environment variables and append the user defined
	 * environment variables to create the String array that can be passed to
	 * Runtime.exec().  We need to retrieve the system env vars because the
	 * env vars passed to Runtime.exec() prevent the system ones from being
	 * inherited.
	 */
	private String[] getEnvironmentVariables(boolean windows)
	{
		if (_isTTY)
		{
			String[] newEnv = new String[_envVars.length + 1];
			for (int i = 0; i < _envVars.length; i++)
				newEnv[i] = _envVars[i];
			newEnv[_envVars.length] = "PS1=$PWD/>"; //$NON-NLS-1$
			_envVars = newEnv;
		}
		return _envVars;
	}


}
