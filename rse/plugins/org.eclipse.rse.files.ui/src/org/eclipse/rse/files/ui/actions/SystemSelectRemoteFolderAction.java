/********************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [184095] Replace systemTypeName by IRSESystemType
 * Xuan Chen (IBM) - [220995] [api] Need to add setCustomViewFilter API to SystemSelectRemoteFileAction
 * Xuan Chen (IBM) - [220995] [api] Need to remove unnecessary APIs.
 ********************************************************************************/

package org.eclipse.rse.files.ui.actions;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.files.ui.dialogs.SystemRemoteFileDialog;
import org.eclipse.rse.files.ui.dialogs.SystemRemoteFolderDialog;
import org.eclipse.rse.internal.files.ui.FileResources;
import org.eclipse.rse.subsystems.files.core.model.RemoteFileUtility;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.rse.ui.SystemActionViewerFilter;
import org.eclipse.rse.ui.actions.SystemBaseDialogAction;
import org.eclipse.rse.ui.dialogs.SystemRemoteResourceDialog;
import org.eclipse.rse.ui.validators.IValidatorRemoteSelection;
import org.eclipse.swt.widgets.Shell;



/**
 * The action for allowing the user to select a remote folder.
 * <p>
 * To configure the functionality, call these methods:
 * <ul>
 *   <li>{@link #setShowNewConnectionPrompt(boolean)}
 *   <li>{@link #setHost(IHost) or #setDefaultConnection(SystemConnection)}
 *   <li>{@link #setSystemType(IRSESystemType)} or {@link #setSystemTypes(IRSESystemType[])}
 *   <li>{@link #setShowPropertySheet(boolean)}
 *   <li>{@link #setMultipleSelectionMode(boolean)}
 *   <li>{@link #setSelectionValidator(IValidatorRemoteSelection)}
 * </ul>
 * <p>
 * To configure the text on the dialog, call these methods:
 * <ul>
 *   <li>{@link #setDialogTitle(String)}
 *   <li>{@link #setMessage(String)}
 *   <li>{@link #setSelectionTreeToolTipText(String)}
 * </ul>
 * <p>
 * After running, call these methods to get the output:
 * <ul>
 *   <li>{@link #getSelectedFolder()} or {@link #getSelectedFolders()}
 *   <li>{@link #getSelectedConnection()}
 * </ul>
 */
public class SystemSelectRemoteFolderAction extends SystemBaseDialogAction
{
    private IRSESystemType[] systemTypes;
    private boolean foldersOnly = false;
    private IHost systemConnection, outputConnection;
    private IHost rootFolderConnection;
    private IRemoteFile preSelection;
    private String   rootFolderAbsPath;
    private String   message, treeTip, dlgTitle;
    private boolean  showNewConnectionPrompt = true;
	private boolean  showPropertySheet = false;
	private boolean  showPropertySheetDetailsButtonInitialState;
	private boolean  showPropertySheetDetailsButton = false;
	private boolean  multipleSelectionMode = false;
	private boolean  onlyConnection = false;
	private IValidatorRemoteSelection selectionValidator;
	private SystemActionViewerFilter customViewerFilter = null;

	/**
	 * Constructor that uses default action label and tooltip
	 *
	 * @param shell The shell to hang the dialog off of
	 */
	public SystemSelectRemoteFolderAction(Shell shell)
	{
		this(shell, FileResources.ACTION_SELECT_DIRECTORY_LABEL, FileResources.ACTION_SELECT_DIRECTORY_TOOLTIP);
	}
	/**
	 * Constructor when you have your own action label and tooltip
	 *
	 * @param shell The shell to hang the dialog off of
	 * @param label string to display in menu or toolbar
	 * @param tooltip string to display when user hovers mouse over action.
	 */
	public SystemSelectRemoteFolderAction(Shell shell, String label, String tooltip)
	{
		super(label, tooltip, null, shell);
		super.setNeedsProgressMonitor(true); // the default is to include a monitor. Caller can override
	}


    // ------------------------
	// CONFIGURATION METHODS...
    // ------------------------
    /**
     * Set the title for the dialog. The default is "Browse for Folder"
     */
    public void setDialogTitle(String title)
    {
    	this.dlgTitle = title;
    }

    public void setFoldersOnly(boolean flag)
    {
    	this.foldersOnly = flag;
    }

    /**
     * Set the message shown at the top of the form
     */
    public void setMessage(String message)
    {
    	this.message = message;
    }
    /**
     * Set the tooltip text for the remote systems tree from which an item is selected.
     */
    public void setSelectionTreeToolTipText(String tip)
    {
    	this.treeTip = tip;
    }

    /**
     * Set the system connection to restrict the user to seeing in the tree.
     *
     * @see #setRootFolder(IHost, String)
     */
    public void setHost(IHost conn)
    {
    	systemConnection = conn;
    	onlyConnection = true;
    }
    /**
     * Set the connection to default the selection to
     */
    public void setDefaultConnection(IHost conn)
    {
    	systemConnection = conn;
    	onlyConnection = false;
    }
    /**
     * Set the system types to restrict what connections the user sees, and what types of
     * connections they can create.
     * @param systemTypes An array of system type names
     *
     * @see org.eclipse.rse.core.IRSESystemType
     */
    public void setSystemTypes(IRSESystemType[] systemTypes)
    {
    	this.systemTypes = systemTypes;
    }
    /**
     * Convenience method to restrict to a single system type.
     * Same as setSystemTypes(new String[] {systemType})
     *
     * @param systemType The name of the system type to restrict to,
     *     or <code>null</code> to allow all valid system types.
     *     A system type is valid if at least one subsystem
     *     configuration is registered against it.
     */
    public void setSystemType(IRSESystemType systemType)
    {
    	if (systemType == null)
    	  setSystemTypes(null);
    	else
    	  setSystemTypes(new IRSESystemType[] {systemType});
    }

    /**
     * Set to true if a "New Connection..." special connection is to be shown for creating new connections
     */
    public void setShowNewConnectionPrompt(boolean show)
    {
    	this.showNewConnectionPrompt = show;
    }

	/**
     * Set the root folder from which to start listing files.
     * This version identifies the folder via a connection object and absolute path.
     * There is another overload that identifies the folder via a single IRemoteFile object.
     * <p>
     * This call effectively transforms the select dialog by:
     * <ul>
     *  <li>Preventing the user from selecting other connections
     *  <li>Preventing the user from selecting other filter strings
     * </ul>
     *
     * @param connection The connection to the remote system containing the root folder
     * @param folderAbsolutePath The fully qualified folder to start listing from (eg: "\folder1\folder2")
     *
     * @see org.eclipse.rse.subsystems.files.core.model.RemoteFileFilterString
	 */
	public void setRootFolder(IHost connection, String folderAbsolutePath)
	{
		rootFolderConnection = connection;
		rootFolderAbsPath = folderAbsolutePath;

		IRemoteFileSubSystem ss  =	RemoteFileUtility.getFileSubSystem(rootFolderConnection);
		if (ss != null)
		{
			try
			{
			IRemoteFile rootFolder = ss.getRemoteFileObject(rootFolderAbsPath, new NullProgressMonitor());
			if (rootFolder != null)
			{
				setPreSelection(rootFolder);
			}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		onlyConnection = true;
	}
	/**
     * Set the root folder from which to start listing folders.
     * This version identifies the folder via an IRemoteFile object.
     * There is another overload that identifies the folder via a connection and folder path.
     * <p>
     * This call effectively transforms the select dialog by:
     * <ul>
     *  <li>Preventing the user from selecting other connections
     *  <li>Preventing the user from selecting other filter strings
     * </ul>
     *
     * @param rootFolder The IRemoteFile object representing the remote folder to start the list from
     *
     * @see org.eclipse.rse.subsystems.files.core.model.RemoteFileFilterString
	 */
	public void setRootFolder(IRemoteFile rootFolder)
	{
		setRootFolder(rootFolder.getHost(),rootFolder.getAbsolutePath());
	}
	/**
	 * Set a file or folder to preselect. This will:
	 * <ul>
	 *   <li>Set the parent folder as the root folder
	 *   <li>Pre-expand the parent folder
	 *   <li>Pre-select the given file or folder after expansion
	 * </ul>
	 * If there is no parent, then we were given a root. In which case we will
	 * <ul>
	 *  <li>Force setRestrictFolders to false
	 *  <li>Pre-expand the root drives (Windows) or root files (Unix)
	 *  <li>Pre-select the given root drive (Windows only)
	 * </ul>
	 */
	public void setPreSelection(IRemoteFile selection)
	{
		preSelection = selection;
	}

    /**
     * Show the property sheet on the right hand side, to show the properties of the
     * selected object.
     * <p>
     * Default is false
     */
    public void setShowPropertySheet(boolean show)
    {
    	this.showPropertySheet = show;
    }
    /**
     * Show the property sheet on the right hand side, to show the properties of the
     * selected object.
     * <p>
     * This overload shows a Details>>> button so the user can decide if they want to see the
     * property sheet.
     * <p>
     * @param show True if to show the property sheet within the dialog
     * @param initialState True if the property is to be initially displayed, false if it is not
     *  to be displayed until the user presses the Details button.
     */
    public void setShowPropertySheet(boolean show, boolean initialState)
    {
    	setShowPropertySheet(show);
    	if (show)
    	{
    	  this.showPropertySheetDetailsButton = true;
    	  this.showPropertySheetDetailsButtonInitialState = initialState;
    	}
    }

    /**
     * Set multiple selection mode. Default is single selection mode
     * <p>
     * If you turn on multiple selection mode, you must use the getSelectedObjects()
     *  method to retrieve the list of selected objects.
     *
     * @see #getSelectedObjects()
     */
    public void setMultipleSelectionMode(boolean multiple)
    {
    	this.multipleSelectionMode = multiple;
    }

    /**
     * Specify a validator to use when the user selects a remote file or folder.
     * This allows you to decide if OK should be enabled or not for that remote file or folder.
     */
    public void setSelectionValidator(IValidatorRemoteSelection selectionValidator)
    {
    	this.selectionValidator = selectionValidator;
    }

    // -----------------
    // OUTPUT METHODS...
    // -----------------


    /**
     * Retrieve selected folder object. If multiple folders selected, returns the first.
     */
    public IRemoteFile getSelectedFolder()
    {
    	Object o = getValue();
    	if (o instanceof IRemoteFile[])
    	  return ((IRemoteFile[])o)[0];
    	else if (o instanceof IRemoteFile)
    	  return (IRemoteFile)o;
        else
    	  return null;
    }
    /**
     * Retrieve selected folder objects. If no folders selected, returns an array of zero.
     * If one folder selected returns an array of one.
     */
    public IRemoteFile[] getSelectedFolders()
    {
    	Object o = getValue();
    	if (o instanceof Object[]) {

    		Object[] temp = (Object[])o;

    		IRemoteFile[] files = new IRemoteFile[temp.length];

    		// ensure all objects are IRemoteFiles
    		for (int i = 0; i < temp.length; i++) {

    			if (temp[i] instanceof IRemoteFile) {
    				files[i] = (IRemoteFile)temp[i];
    			}
    			// should never happen
    			else {
    				return new IRemoteFile[0];
    			}
    		}

    		return files;
    	}
    	if (o instanceof IRemoteFile[])
    	  return (IRemoteFile[])o;
    	else if (o instanceof IRemoteFile)
    	  return new IRemoteFile[] {(IRemoteFile)o};
        else
    	  return new IRemoteFile[0];
    }

    /**
     * Return all selected objects. This method will return an array of one
     *  unless you have called setMultipleSelectionMode(true)!
     * <p>
     * It will always return null if the user cancelled the dialog.
     *
     * @see #setMultipleSelectionMode(boolean)
     */
    public Object[] getSelectedObjects()
    {
    	Object remoteObject = getValue();
    	if (remoteObject == null)
    	  return null;
    	else if (remoteObject instanceof Object[])
    	  return (Object[])remoteObject;
    	else if (remoteObject instanceof IRemoteFile[])
    	  return (Object[])remoteObject;
    	else
    	  return null;
    }

    /**
     * Return selected connection
     */
    public IHost getSelectedConnection()
    {
    	return outputConnection;
    }

    /**
	 * @since 3.0
	 */
    public void setCustomViewerFilter(SystemActionViewerFilter filter)
	{
		customViewerFilter = filter;
	}

    // -------------------
    // INTERNAL METHODS...
    // -------------------

	/**
	 * Called by eclipse when user selects this action
	 */
	protected Dialog createDialog(Shell shell)
	{
		SystemRemoteResourceDialog dlg = null;
		if (foldersOnly)
		{
			if (dlgTitle == null)
			{
				dlg = new SystemRemoteFolderDialog(shell);
			}
			else
			{
				dlg = new SystemRemoteFolderDialog(shell, dlgTitle);
			}
		}
		else
		{
			if (dlgTitle == null)
			{
				dlg = new SystemRemoteFileDialog(shell);
			}
			else
			{
				dlg = new SystemRemoteFileDialog(shell, dlgTitle);
			}
		}
		dlg.setMultipleSelectionMode(multipleSelectionMode);
		if (systemConnection != null)
		{
			dlg.setDefaultSystemConnection(systemConnection, onlyConnection);
		}
		dlg.setCustomViewerFilter(customViewerFilter);

		/*
		SystemSelectRemoteFileOrFolderDialog dlg = null;
		if (dlgTitle == null)
		  dlg = new SystemSelectRemoteFileOrFolderDialog(shell, false); // false => folder vs file mode
		else
		  dlg = new SystemSelectRemoteFileOrFolderDialog(shell, dlgTitle, false); // false => folder vs file mode
		*/


		dlg.setShowNewConnectionPrompt(showNewConnectionPrompt);
		dlg.setMultipleSelectionMode(multipleSelectionMode);

		/*
		dlg.setAllowForMultipleParents(allowForMultipleParents);
		if (restrictFolders)
		  dlg.setRestrictFolders(true);


		if (systemConnection != null)
		{
			if (onlyConnection)
		      dlg.setSystemConnection(systemConnection);
		    else
		      dlg.setDefaultConnection(systemConnection);
		}
		*/

		if (message != null)
		  dlg.setMessage(message);
		if (treeTip != null)
		  dlg.setSelectionTreeToolTipText(treeTip);

		if (systemTypes != null)
		  dlg.setSystemTypes(systemTypes);
		/*
		if (expandDepth != 0)
		  dlg.setAutoExpandDepth(expandDepth);
		  */
		if (preSelection != null)
		  dlg.setPreSelection(preSelection);

		/*
		else if (rootFolderConnection != null)
		  dlg.setRootFolder(rootFolderConnection, rootFolderAbsPath);
		*/

		if (showPropertySheet)
		  if (showPropertySheetDetailsButton)
		    dlg.setShowPropertySheet(true, showPropertySheetDetailsButtonInitialState);
		  else
		    dlg.setShowPropertySheet(true);

		/*
		if (addButtonCallback != null)
          if ((addLabel!=null) || (addToolTipText!=null))
            dlg.enableAddMode(addButtonCallback, addLabel, addToolTipText);
          else
            dlg.enableAddMode(addButtonCallback);
            */
        if (selectionValidator != null)
          dlg.setSelectionValidator(selectionValidator);

		return dlg;
	}

	/**
	 * Required by parent. We return the selected object
	 */
	protected Object getDialogValue(Dialog dlg)
	{
		SystemRemoteResourceDialog ourDlg = (SystemRemoteResourceDialog)dlg;
		Object outputObject = null;
		outputConnection = null;
		if (!ourDlg.wasCancelled())
		{
		    if (multipleSelectionMode)
			  outputObject = ourDlg.getSelectedObjects();
			else
			  outputObject = ourDlg.getSelectedObject();
		    outputConnection = ourDlg.getSelectedConnection();
		}
		return outputObject; // parent class calls setValue on what we return
	}

}