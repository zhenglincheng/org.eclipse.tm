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
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * David McKnight   (IBM)        - [216252] SystemMessages using RSEStatus
 * David McKnight   (IBM)        - [216252] [api][nls] Resource Strings specific to subsystems should be moved from rse.ui into files.ui / shells.ui / processes.ui where possible
 * David McKnight   (IBM)        - [220547] [api][breaking] SimpleSystemMessage needs to specify a message id and some messages should be shared
 *******************************************************************************/

package org.eclipse.rse.connectorservice.dstore.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.dstore.core.model.DataElement;
import org.eclipse.dstore.extra.DomainEvent;
import org.eclipse.dstore.extra.IDomainListener;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.SubSystemConfiguration;
import org.eclipse.rse.internal.connectorservice.dstore.Activator;
import org.eclipse.rse.services.clientserver.messages.CommonMessages;
import org.eclipse.rse.services.clientserver.messages.ICommonMessageIds;
import org.eclipse.rse.services.clientserver.messages.SimpleSystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class ConnectionStatusListener implements IDomainListener, IRunnableWithProgress
{


	protected DataElement _dataStoreStatus;
	protected IConnectorService _connection;
	protected boolean _connectionDown = false;

	/**
	 * @param status The status element for the DataStore handling this connection.
	 */
	public ConnectionStatusListener(DataElement status, IConnectorService connection)
	{
		_dataStoreStatus = status;
		_connection = connection;
	}

	protected Shell internalGetShell()
	{
		Shell activeShell = SystemBasePlugin.getActiveWorkbenchShell();
		if (activeShell != null)
		{
			return activeShell;
		}
		
		IWorkbenchWindow window = null;
		try
		{
			window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		}
		catch (Exception e)
		{
			return null;
		}
		if (window == null)
		{

			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			if (windows != null && windows.length > 0)
			{
				return windows[0].getShell();
			}
	
		}
		else
		{
			return window.getShell();
		}

		return null;
	}

	/**
	 * The handleConnectionDown method is invoked if the network connection between the 
	 * client and server goes down while connected.  Currently this method displays
	 * an error to the user and the subsytem is disconnected.
	 */
	protected void handleConnectionDown()
	{
		Display.getDefault().asyncExec(new ConnectionDown(this));
	}
	
	class ConnectionDown implements Runnable
	{
		private ConnectionStatusListener _listener;
		public ConnectionDown(ConnectionStatusListener listener)
		{
			_listener = listener;
		}
		
		public void run()
		{
			Shell shell = getShell();
			_connectionDown = true;
			
			String fmsgStr = NLS.bind(CommonMessages.MSG_CONNECT_UNKNOWNHOST, _connection.getPrimarySubSystem().getHost().getAliasName());

			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID, ICommonMessageIds.MSG_CONNECT_UNKNOWNHOST, IStatus.ERROR, fmsgStr);
			SystemMessageDialog dialog = new SystemMessageDialog(internalGetShell(), msg);
			dialog.open();

			try
			{
				IRunnableContext runnableContext = getRunnableContext(getShell());
		    	runnableContext.run(false,true,_listener); // inthread, cancellable, IRunnableWithProgress
		    	_connection.reset();
				ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();    	    
	            sr.connectedStatusChange(_connection.getPrimarySubSystem(), false, true, true);
			}
	    	catch (InterruptedException exc) // user cancelled
	    	{
	    	  if (shell != null)    		
	            showDisconnectCancelledMessage(shell, _connection.getHostName(), _connection.getPort());
	    	}    	
	    	catch (java.lang.reflect.InvocationTargetException invokeExc) // unexpected error
	    	{
	    	  Exception exc = (Exception)invokeExc.getTargetException();
	    	  if (shell != null)    		
	    	    showDisconnectErrorMessage(shell, _connection.getHostName(), _connection.getPort(), exc);    	    	
	    	}
			catch (Exception e)
			{
				SystemBasePlugin.logError("ConnectionStatusListener:  Error disconnecting", e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * @see IDomainListener#listeningTo(DomainEvent)
	 */
	public boolean listeningTo(DomainEvent event)
	{
		if (_dataStoreStatus == event.getParent())
		{
			return true;
		}
		return false;
	}

	/**
	 * @see IDomainListener#domainChanged(DomainEvent)
	 */
	public void domainChanged(DomainEvent event)
	{
		if (!_dataStoreStatus.getName().equals("okay")) //$NON-NLS-1$
		{
			handleConnectionDown();
		}
	}

	public Shell getShell()
	{
		return internalGetShell();
	}

	/**
	 * Callback method for the IConnectorService to determine if the connection is down.  This is
	 * called by the disconnect method to determine if we can do saves or not.
	 */
	public boolean isConnectionDown()
	{
		return _connectionDown;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException 
	{
		String message = null;
		message = SubSystemConfiguration.getDisconnectingMessage(_connection.getHostName(), _connection.getPort());
		monitor.beginTask(message, IProgressMonitor.UNKNOWN);
		try 
		{			
		  _connection.disconnect(monitor);
		}
		catch(Exception exc)
		{
		  if (exc instanceof java.lang.reflect.InvocationTargetException)
		    throw (java.lang.reflect.InvocationTargetException)exc;
		  if (exc instanceof java.lang.InterruptedException)
		    throw (java.lang.InterruptedException)exc;
		  throw new java.lang.reflect.InvocationTargetException(exc);
		}
		finally
		{
			monitor.done();
		}
	}
    /**
     * Get the progress monitor dialog for this operation. We try to 
     *  use one for all phases of a single operation, such as connecting
     *  and resolving.
     */
    protected IRunnableContext getRunnableContext(Shell rshell)
    {
    	Shell shell = getShell();
       	// for other cases, use statusbar
       	IWorkbenchWindow win = SystemBasePlugin.getActiveWorkbenchWindow();
       	if (win != null)
       	{
       		Shell winShell = RSEUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
       		if (winShell != null && !winShell.isDisposed() && winShell.isVisible())
       		{
       			SystemBasePlugin.logInfo("Using active workbench window as runnable context"); //$NON-NLS-1$
       			shell = winShell;
       			return win;	
       		}	
       		else
       		{
       			win = null;	
       		}
       	}	  
    
       	if (shell == null || shell.isDisposed() || !shell.isVisible()) 
       	{
       		SystemBasePlugin.logInfo("Using progress monitor dialog with given shell as parent"); //$NON-NLS-1$
       		shell = rshell;	
       	}
       	
     		    							
	      	IRunnableContext dlg =  new ProgressMonitorDialog(rshell);           
			return dlg; 
       }
    
    /**
     * Show an error message when the disconnection fails.
     * Shows a common message by default.
     * Overridable.
     */
    protected void showDisconnectErrorMessage(Shell shell, String hostName, int port, Exception exc)
    {
    	String dfailedMsg = NLS.bind(CommonMessages.MSG_DISCONNECT_FAILED, hostName);

 			
		try{	
			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID, ICommonMessageIds.MSG_DISCONNECT_FAILED, IStatus.ERROR, dfailedMsg, exc);
    	
			SystemMessageDialog msgDlg = new SystemMessageDialog(shell, msg);
			msgDlg.setException(exc);
			msgDlg.open();
		}
		catch (Exception e){			
		}
    }	
    /**
     * Show an error message when the user cancels the disconnection.
     * Shows a common message by default.
     * Overridable.
     */
    protected void showDisconnectCancelledMessage(Shell shell, String hostName, int port)
    {
    	String msg = NLS.bind(CommonMessages.MSG_DISCONNECT_CANCELLED, hostName);
    	SystemMessageDialog msgDlg = new SystemMessageDialog(shell, new SimpleSystemMessage(Activator.PLUGIN_ID, ICommonMessageIds.MSG_DISCONNECT_CANCELLED, IStatus.CANCEL, msg));
    	msgDlg.open();
    }
}
