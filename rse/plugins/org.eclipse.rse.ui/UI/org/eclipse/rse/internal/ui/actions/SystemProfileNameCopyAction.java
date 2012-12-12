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
 * Martin Oberhuber (Wind River) - [168975] Move RSE Events API to Core
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 *******************************************************************************/

package org.eclipse.rse.internal.ui.actions;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.internal.ui.dialogs.SystemCopyProfileDialog;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.actions.SystemBaseDialogAction;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


/**
 * A copy profile action. Will copy the profile, and all connections for the profile. 
 * We must first prompt user for a new name for the copied profile.
 */
public class SystemProfileNameCopyAction extends SystemBaseDialogAction 
                                 implements  IRunnableWithProgress
{	
	private ISystemProfile profile, newProfile;
	//private ISystemProfileManager mgr;
	private ISystemRegistry sr;
	private String oldName,newName;
	private boolean makeActive;
    private Exception runException = null;
    	
	/**
	 * Constructor for selection-sensitive popup menu for profiles in Team view. 
	 */
	public SystemProfileNameCopyAction(Shell shell) 
	{
		super(SystemResources.ACTION_PROFILE_COPY_LABEL, SystemResources.ACTION_PROFILE_COPY_TOOLTIP,
			  PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY), 
			  shell);
		//mgr = SystemProfileManager.getSystemProfileManager();
		sr = RSECorePlugin.getTheSystemRegistry();
		setSelectionSensitive(true);
        allowOnMultipleSelection(false);
		setHelp(RSEUIPlugin.HELPPREFIX+"actndupr"); //$NON-NLS-1$
	}
	
	/**
	 * Set the profile
	 */
	public void setProfile(ISystemProfile profile)
	{
		this.profile = profile;
	}
	
	/**
	 * Override of parent
	 * @see #run()
	 */
	protected Dialog createDialog(Shell parent)
	{
		return new SystemCopyProfileDialog(parent, profile); 
	}
	
	/**
	 * Required by parent. We use it to return the new name
	 */
	protected Object getDialogValue(Dialog dlg)
	{
		newName = null;
		SystemCopyProfileDialog rnmDlg = (SystemCopyProfileDialog)dlg;
		if (!rnmDlg.wasCancelled())
	    {
           oldName = profile.getName();
		   newName = rnmDlg.getNewName();
		   makeActive = rnmDlg.getMakeActive();
    	   IRunnableContext runnableContext = getRunnableContext();
    	   try
    	   {
    	     runnableContext.run(false,false,this); // inthread, cancellable, IRunnableWithProgress
             if (makeActive && (newProfile!=null))
               sr.setSystemProfileActive(newProfile, true);
    	   }
    	   catch (java.lang.reflect.InvocationTargetException exc) // unexpected error
    	   {
    	  	 showOperationMessage(exc, getShell());
    	     //throw (Exception) exc.getTargetException();    	    	
    	   }
    	   catch (Exception exc)
    	   {
    	  	 showOperationMessage(exc, getShell());
             //throw exc;
    	   }    	
		}
		return newName;					
	}
	/**
	 * Get an IRunnable context to show progress in. If there is currently a dialog or wizard up with
	 * a progress monitor in it, we will use this, else we will create a progress monitor dialog.
	 */
	protected IRunnableContext getRunnableContext()
	{
		IRunnableContext irc = RSEUIPlugin.getTheSystemRegistryUI().getRunnableContext();
		if (irc == null)
		  irc = new ProgressMonitorDialog(getShell());
		return irc;
	}
	
	
    // ----------------------------------
    // INTERNAL METHODS...
    // ----------------------------------
	/**
	 * Method required by IRunnableWithProgress interface.
	 * Allows execution of a long-running operation modally by via a thread.
	 * In our case, it runs the copy operation with a visible progress monitor
	 */
    public void run(IProgressMonitor monitor)
         throws java.lang.reflect.InvocationTargetException,
                java.lang.InterruptedException
	{		
		String msg = getCopyingMessage(oldName,newName);
		runException = null;
		
        try
        {
           int steps = 0;
           IHost[] conns = sr.getHostsByProfile(profile);
           if ((conns != null) && (conns.length > 0))
             steps = conns.length;
           steps += 2; // for filterpools and subsystems
	       monitor.beginTask(msg, steps);
		   newProfile = sr.copySystemProfile(profile, newName,makeActive,monitor);
           monitor.done();
        }
        catch(java.lang.InterruptedException exc)
        {
           monitor.done();
           runException = exc;
           throw (java.lang.InterruptedException)runException;
        }						
        catch(Exception exc)
        {
           monitor.done();
           runException = new java.lang.reflect.InvocationTargetException(exc);
           throw (java.lang.reflect.InvocationTargetException)runException;
        }

	}
	

    /**
     * Helper method to return the message "Copying &1 to &2..."
     */
    public static String getCopyingMessage(String oldName, String newName)
    {
    	SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_COPY_PROGRESS);
    	msg.makeSubstitution(oldName,newName);
    	return msg.getLevelOneText();
    }

    /**
     * Helper method to show an error message resulting from the attempted operation.
     */
	protected void showOperationMessage(Exception exc, Shell shell)
	{
		if (exc instanceof java.lang.InterruptedException)
		  showOperationCancelledMessage(shell);
		else if (exc instanceof java.lang.reflect.InvocationTargetException)
		  showOperationErrorMessage(shell, ((java.lang.reflect.InvocationTargetException)exc).getTargetException());
		else
		  showOperationErrorMessage(shell, exc);
	}

    /**
     * Show an error message when the operation fails.
     * Shows a common message by default.
     * Overridable.
     */
    protected void showOperationErrorMessage(Shell shell, Throwable exc)
    {
    	SystemMessageDialog msgDlg = new SystemMessageDialog(shell, RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_OPERATION_FAILED).makeSubstitution(exc.getMessage()));
    	msgDlg.open();
        SystemBasePlugin.logError("Copy profile operation failed",exc); //$NON-NLS-1$
    }	
    /**
     * Show an error message when the user cancels the operation.
     * Shows a common message by default.
     * Overridable.
     */
    protected void showOperationCancelledMessage(Shell shell)
    {
    	SystemMessageDialog msgDlg = new SystemMessageDialog(shell, RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_OPERATION_CANCELLED));
    	msgDlg.open();
    }	


}
