/********************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others. All rights reserved.
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
 * IBM Corporation - initial API and implementation
 * Kushal Munir (IBM) - moved to internal package
 * Martin Oberhuber (Wind River) - added progress dialog 
 *    - (adapted from org.eclipse.ui.actions.CopyProjectAction, copyright IBM)
 * Martin Oberhuber (Wind River) - [181917] EFS Improvements: Avoid unclosed Streams,
 *    - Fix early startup issues by deferring FileStore evaluation and classloading,
 *    - Improve performance by RSEFileStore instance factory and caching IRemoteFile.
 *    - Also remove unnecessary class RSEFileCache and obsolete branding files.
 ********************************************************************************/

package org.eclipse.rse.internal.eclipse.filesystem.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.rse.core.SystemBasePlugin;
import org.eclipse.rse.internal.eclipse.filesystem.Activator;
import org.eclipse.rse.internal.eclipse.filesystem.RSEFileSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;


public class CreateRemoteProjectActionDelegate implements IActionDelegate {
	
	protected IStructuredSelection fSelection;
	private IStatus errorStatus;

	//----------------------------------------------------------------------------
	// <Adapted from org.eclipse.ui.actions.CopyProjectAction as of Eclipse 3.3M6> 
	// (Copyright 2000, 2006 IBM Corporation and others)
	//----------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) 
	{
		errorStatus = null;

		IRemoteFile directory = (IRemoteFile)fSelection.getFirstElement();

		boolean completed = performCreateRemoteProject(directory);

        if (!completed) {
			return; // not appropriate to show errors
		}

        // If errors occurred, open an Error dialog
        if (errorStatus != null) {
            ErrorDialog.openError(getShell(), getErrorsTitle(), null, errorStatus);
            errorStatus = null;
        }
	}

	/**
	 * Records the core exception to be displayed to the user once the action is
	 * finished.
	 * 
	 * @param error
	 *            a <code>CoreException</code>
	 */
	final void recordError(CoreException error) {
		this.errorStatus = error.getStatus();
	}

	/**
	 * Opens an error dialog to display the given message.
	 * <p>
	 * Note that this method must be called from UI thread.
	 * </p>
	 * 
	 * @param message
	 *            the message
	 */
	void displayError(String message) {
		MessageDialog.openError(getShell(), getErrorsTitle(), message);
	}

	/**
	 * Return the title of the errors dialog.
	 * 
	 * @return java.lang.String
	 */
	protected String getErrorsTitle() {
		return "Error creating remote project";
	}

	/**
	 * Creates a remote project.
	 * 
	 * @param directory
	 *            the remote folder on which the EFS project should be locaed
	 * @return <code>true</code> if the copy operation completed, and
	 *         <code>false</code> if it was abandoned part way
	 */
	boolean performCreateRemoteProject(final IRemoteFile directory) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
			        createRemoteProject(directory, monitor);
				} catch (Exception e) {
					if (e.getCause() instanceof CoreException) {
						recordError((CoreException)e.getCause());
					} else {
						Activator.getDefault().getLog().log(new Status(IStatus.ERROR,
								Activator.getDefault().getBundle().getSymbolicName(),
								-1, e.getMessage(), e));
						displayError(e.getMessage());
					}
				}
			}
		};

		try {
			//TODO make this a Job an run in foreground with option to send to background
			ProgressMonitorDialog mon = new ProgressMonitorDialog(getShell()) {
			    protected void configureShell(Shell shell) {
			        super.configureShell(shell);
					shell.setText("Creating remote Project");
			    }
			};
			mon.run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			displayError("Internal Error: "+e.getTargetException().getMessage());
			return false;
		}

		return true;
	}

	//----------------------------------------------------------------------------
	// </Adapted from org.eclipse.ui.actions.CopyProjectAction as of Eclipse 3.3M6> 
	// (Copyright 2000, 2006 IBM Corporation and others)
	//----------------------------------------------------------------------------

	private IProject createRemoteProject(IRemoteFile directory, IProgressMonitor monitor)
	{
		IWorkspaceRoot root = SystemBasePlugin.getWorkspaceRoot();

		IProject editProject = root.getProject(directory.getName());

		if ((editProject != null) && (editProject.exists()) && (editProject.isOpen()))
		{
			return editProject;
		}
		
		if (editProject == null) {
			return null;
		}

		try
		{
			IProjectDescription description = root.getWorkspace().newProjectDescription(directory.getName());
			String hostNameOrAddr = directory.getParentRemoteFileSubSystem().getHost().getHostName();
			String absolutePath = directory.getAbsolutePath();
			URI location = RSEFileSystem.getURIFor(hostNameOrAddr, absolutePath);
			description.setLocationURI(location);

			editProject.create(description, monitor);
			
			editProject.open(monitor);
			
		    editProject.refreshLocal(IResource.DEPTH_ONE, monitor);
		}
		catch (CoreException e)
		{
			SystemBasePlugin.logError("Error creating temp project", e);
		}
		catch (Exception e)
		{
			SystemBasePlugin.logError("Error creating temp project", e);
		}
		return editProject;
	}
	
	/**
	 * Sets the selection. The selection is only set if given a structured selection, otherwise it is set to an
	 * empty structured selection.
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		
		if (selection instanceof IStructuredSelection) {
			fSelection = (IStructuredSelection)selection;
		}
		else {
			fSelection = StructuredSelection.EMPTY;
		}
	}
	
	/**
	 * Returns the remote file in the selection.
	 * Use this method if this action allows only a single remote file selection.
	 * @return the single remote file.
	 */
	protected IRemoteFile getRemoteFile(IStructuredSelection selection) {
		return (IRemoteFile)selection.getFirstElement();
	}
	
	/**
	 * Returns the remote files in the selection.
	 * Use this method if this action allows multiple remote file selection.
	 * @return an array of remote files.
	 */
	protected IRemoteFile[] getRemoteFiles(IStructuredSelection selection) {

		IRemoteFile[] files = new IRemoteFile[selection.size()];
		Iterator iter = selection.iterator();
		
		int i = 0;
		
		while (iter.hasNext()) {
			files[i++] = (IRemoteFile)iter.next();
		}
		
		return files;
	}
	
	/**
	 * Returns the description file for the first description file in
	 * the selection. Use this method if this action allows only
	 * a single file selection.
	 * @return the single description file.
	 */
	protected IFile getDescriptionFile(IStructuredSelection selection) {
		return (IFile)selection.getFirstElement();
	}

	/**
	 * Returns a description file for each description file in
	 * the selection. Use this method if this action allows multiple
	 * selection.
	 * @return an array of description files.
	 */
	protected IFile[] getDescriptionFiles(IStructuredSelection selection) {
		IFile[] files = new IFile[selection.size()];
		Iterator iter = selection.iterator();
		
		int i = 0;
		
		while (iter.hasNext()) {
			files[i++] = (IFile)iter.next();
		}
		
		return files;
	}

	/**
	 * Returns the workbench.
	 * @return the workbench.
	 */
	protected IWorkbench getWorkbench() {
		return PlatformUI.getWorkbench();
	}
	
	/**
	 * Returns the active shell.
	 * @return the active shell.
	 */
	protected Shell getShell() {
		return Display.getDefault().getActiveShell();
	}

	/**
	 * Returns the selection.
	 * @return the selection.
	 */
	protected IStructuredSelection getSelection() {
		return fSelection;
	}
}