/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [168870] refactor org.eclipse.rse.core package of the UI plugin
 * Martin Oberhuber (Wind River) - [189130] Move SystemIFileProperties from UI to Core
 * David McKnight   (IBM)        - [189873] DownloadJob changed to DownloadAndOpenJob
 * David McKnight   (IBM)        - [224377] "open with" menu does not have "other" option
 * David McKnight   (IBM)        - [277141] System Editor Passed Incorrect Cache Information in Presence of Case-Differentiated-Only filenames
 * David McKnight   (IBM)        - [284596] [regression] Open with-> problem when descriptor doesn't match previous
 * David McKnight   (IBM)        - [309755] SystemRemoteFileOpenWithMenu.getPreferredEditor(), the listed default editor is not always correct
 * David McKnight   (IBM)        - [312362] Editing Unix file after it changes on host edits old data
 *******************************************************************************/
package org.eclipse.rse.internal.files.ui.actions;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.files.ui.resources.SystemEditableRemoteFile;
import org.eclipse.rse.files.ui.resources.UniversalFileTransferUtility;
import org.eclipse.rse.internal.files.ui.FileResources;
import org.eclipse.rse.internal.files.ui.resources.SystemRemoteEditManager;
import org.eclipse.rse.internal.files.ui.view.DownloadAndOpenJob;
import org.eclipse.rse.subsystems.files.core.SystemIFileProperties;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.eclipse.rse.ui.view.ISystemEditableRemoteObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.EditorSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;


/**
 * Open with menu class for remote files
 */
public class SystemRemoteFileOpenWithMenu extends ContributionItem 
{
	protected IWorkbenchPage page;
	protected IRemoteFile _remoteFile;
	protected IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();

	private static Hashtable imageCache = new Hashtable(11);
	 
	/**
	 * The id of this action.
	 */
	public static final String ID = PlatformUI.PLUGIN_ID + ".OpenWithMenu";//$NON-NLS-1$

	/*
	 * Compares the labels from two IEditorDescriptor objects 
	 */
	private static final Comparator comparer = new Comparator() 
	{
		private Collator collator = Collator.getInstance();

		public int compare(Object arg0, Object arg1) {
			String s1 = ((IEditorDescriptor)arg0).getLabel();
			String s2 = ((IEditorDescriptor)arg1).getLabel();
			return collator.compare(s1, s2);
		}
	}; 


/**
 * Constructs a new instance of <code>SystemOpenWithMenu</code>.  
 */
public SystemRemoteFileOpenWithMenu() 
{
	super(ID);
	this.page = null;
	_remoteFile = null;	
}

/*
 * Initializes the IRemoteFile
 */
public void updateSelection(IStructuredSelection selection)
{
	if (selection.size() == 1)
	{
		_remoteFile = (IRemoteFile)selection.getFirstElement();
	}
}

/**
 * Returns an image to show for the corresponding editor descriptor.
 *
 * @param editorDesc the editor descriptor, or null for the system editor
 * @return the image or null
 */
protected Image getImage(IEditorDescriptor editorDesc) {
	ImageDescriptor imageDesc = getImageDescriptor(editorDesc);
	if (imageDesc == null) {
		return null;
	}
	Image image = (Image) imageCache.get(imageDesc);
	if (image == null) {
		image = imageDesc.createImage();
		imageCache.put(imageDesc, image);
	}
	return image;
}

private String getFileName()
{
	return _remoteFile.getName();
}

/**
 * Returns the image descriptor for the given editor descriptor,
 * or null if it has no image.
 */
private ImageDescriptor getImageDescriptor(IEditorDescriptor editorDesc) {
	ImageDescriptor imageDesc = null;
	if (editorDesc == null) {
		imageDesc = registry.getImageDescriptor(getFileName());
	}
	else {
		imageDesc = editorDesc.getImageDescriptor();
	}
	if (imageDesc == null && editorDesc != null) {
		if (editorDesc.getId().equals(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID))
			imageDesc = registry.getSystemExternalEditorImageDescriptor(getFileName());
	}
	return imageDesc;
}
/**
 * Creates the menu item for the editor descriptor.
 *
 * @param menu the menu to add the item to
 * @param descriptor the editor descriptor, or null for the system editor
 * @param preferredEditor the descriptor of the preferred editor, or <code>null</code>
 */
protected void createMenuItem(Menu menu, final IEditorDescriptor descriptor, final IEditorDescriptor preferredEditor) 
{
	// XXX: Would be better to use bold here, but SWT does not support it.
	final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
	boolean isPreferred = preferredEditor != null && descriptor.getId().equals(preferredEditor.getId());
	menuItem.setSelection(isPreferred);
	menuItem.setText(descriptor.getLabel());
	Image image = getImage(descriptor);
	if (image != null) {
		menuItem.setImage(image);
	}
	Listener listener = new Listener() {
		public void handleEvent(Event event) {
			switch (event.type) {
				case SWT.Selection:
					if(menuItem.getSelection())
					{
						openEditor(_remoteFile, descriptor);
					}
					break;
			}
		}
	};
	menuItem.addListener(SWT.Selection, listener);
}

/**
 * Creates the Other... menu item
 *
 * @param menu the menu to add the item to
 */
private void createOtherMenuItem(final Menu menu, final IRemoteFile remoteFile) {

    new MenuItem(menu, SWT.SEPARATOR);
    final MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
    menuItem.setText(FileResources.OpenWithMenu_Other);
    Listener listener = new Listener() {
        public void handleEvent(Event event) {
            switch (event.type) {
            case SWT.Selection:
               	EditorSelectionDialog dialog = new EditorSelectionDialog(
						menu.getShell());
				dialog
						.setMessage(NLS
								.bind(
										FileResources.OpenWithMenu_OtherDialogDescription,
										remoteFile.getName()));
				if (dialog.open() == Window.OK) {
					IEditorDescriptor editor = dialog.getSelectedEditor();
					if (editor != null) {
						openEditor(remoteFile, editor);
					}
				}
                break;
            }
        }
    };
    menuItem.addListener(SWT.Selection, listener);
}


protected void openEditor(IRemoteFile remoteFile, IEditorDescriptor descriptor) {
	
	// make sure we're using the latest version of remoteFile
	try {
		remoteFile = remoteFile.getParentRemoteFileSubSystem().getRemoteFileObject(remoteFile.getAbsolutePath(), new NullProgressMonitor());
	}
	catch (Exception e){				
	}
	
	SystemEditableRemoteFile editable = SystemRemoteEditManager.getEditableRemoteObject(remoteFile, descriptor);
	if (editable == null){
		// case for cancelled operation when user was prompted to save file of different case
		return;
	}
	
	boolean systemEditor = descriptor != null && descriptor.getId().equals(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);

	if (isFileCached(editable, remoteFile)) {
		
		try {
			if (systemEditor) {
				editable.openSystemEditor();
			}
			else {		
				if (descriptor != null){
					hackOpenEditor(editable, descriptor);
				}
				else {
					editable.openEditor();
				}
			}
		}
		catch (Exception e) {}
	}
	else {
		DownloadAndOpenJob oJob = new DownloadAndOpenJob(editable, systemEditor);
		oJob.schedule();
	}
}

/**
 * This method is a hack to deal with bug 284596 while no API exists to set the editor descriptor for a
 * given SystemEditableRemoteFile.  The code here is essentially a modified version of
 * SystemEditableRemoteFile.openEditor()  
 */
private void hackOpenEditor(SystemEditableRemoteFile editable, IEditorDescriptor descriptor) throws PartInitException
{
	IWorkbenchPage activePage = this.page;
	IWorkbench wb = PlatformUI.getWorkbench();
	if (activePage == null)
	{
		activePage = wb.getActiveWorkbenchWindow().getActivePage();
	}
	IFile file = editable.getLocalResource();

	IRemoteFile remoteFile = editable.getRemoteFile();
	// get fresh remote file object
	remoteFile.markStale(true); // make sure we get the latest remote file (with proper permissions and all)
	IRemoteFileSubSystem ss = remoteFile.getParentRemoteFileSubSystem();
	if (!remoteFile.getParentRemoteFileSubSystem().isOffline()){
		try{
			remoteFile = ss.getRemoteFileObject(remoteFile.getAbsolutePath(), new NullProgressMonitor());
		}
		catch (Exception e){
			SystemMessageDialog.displayExceptionMessage(SystemMessageDialog.getDefaultShell(), e);
			return;
		}
	}
	editable.setRemoteFile(remoteFile);
	
	boolean readOnly = !remoteFile.canWrite();
	ResourceAttributes attr = file.getResourceAttributes();
	if (attr!=null) {
		attr.setReadOnly(readOnly);
		try	{
			file.setResourceAttributes(attr);
		}
		catch (Exception e)
		{}
	}

	// set editor as preferred editor for this file
	String editorId = descriptor.getId();
	IDE.setDefaultEditor(file, editorId);

	FileEditorInput finput = new FileEditorInput(file);

	IEditorPart editor = null;
	if (descriptor.isOpenExternal()){
		editor = ((WorkbenchPage)activePage).openEditorFromDescriptor(new FileEditorInput(file), descriptor, true, null);
	}
	else {
		editor =  activePage.openEditor(finput, descriptor.getId());
	}
	editable.setEditor(editor);
	
	SystemIFileProperties properties = new SystemIFileProperties(file);
	properties.setRemoteFileObject(editable);
}

private boolean isFileCached(ISystemEditableRemoteObject editable, IRemoteFile remoteFile)
{
	// DY:  check if the file exists and is read-only (because it was previously opened
	// in the system editor)
	IFile file = editable.getLocalResource();
	SystemIFileProperties properties = new SystemIFileProperties(file);
	boolean newFile = !file.exists();

	// detect whether there exists a temp copy already
	if (!newFile && file.exists())
	{
		// we have a local copy of this file, so we need to compare timestamps

		// get stored modification stamp
		long storedModifiedStamp = properties.getRemoteFileTimeStamp();

		// get updated remoteFile so we get the current remote timestamp
		remoteFile.markStale(true);
		IRemoteFileSubSystem subsystem = remoteFile.getParentRemoteFileSubSystem();
		try
		{
			remoteFile = subsystem.getRemoteFileObject(remoteFile.getAbsolutePath(), new NullProgressMonitor());
		}
		catch (Exception e)
		{
			
		}

		// get the remote modified stamp
		long remoteModifiedStamp = remoteFile.getLastModified();

		// get dirty flag
		boolean dirty = properties.getDirty();

		boolean remoteNewer = (storedModifiedStamp != remoteModifiedStamp);
		
		String remoteEncoding = remoteFile.getEncoding();
		String storedEncoding = properties.getEncoding();
		
		boolean encodingChanged = storedEncoding == null || !(remoteEncoding.equals(storedEncoding));

		boolean usedBinary = properties.getUsedBinaryTransfer();
		boolean isBinary = remoteFile.isBinary();
		
		boolean usedReadOnly = properties.getReadOnly();
		boolean isReadOnly = !remoteFile.canWrite();
		
		return (!dirty && 
				!remoteNewer && 
				usedBinary == isBinary &&
				usedReadOnly == isReadOnly && 
				!encodingChanged);
	}
	return false;
}

/**
 * Get the local cache of the remote file, or <code>null</code> if none.
 * @param remoteFile the remote file.
 * @return the local cached resource, or <code>null</code> if none.
 */
private IFile getLocalResource(IRemoteFile remoteFile) 
{
    return (IFile)UniversalFileTransferUtility.getTempFileFor(remoteFile);
}

/**
 * Returns the preferred editor for the remote file. If the remote file has a cached local resource,
 * then returns the default editor associated with that resource, by calling <code>IDE.getDefaultEditor(IFile)</code>.
 * Otherwise, get the default editor associated with that remote file name from the editor registry.
 * @param remoteFile the remote file.
 * @return the preferred editor for the remote file, or <code>null</code> if none.
 */
protected IEditorDescriptor getPreferredEditor(IRemoteFile remoteFile) {

	IFile localFile = getLocalResource(remoteFile);
	
	if (localFile == null || !localFile.exists()){
		return registry.getDefaultEditor(remoteFile.getName());
	}
	else {
		return IDE.getDefaultEditor(localFile);
	}
}


protected IEditorDescriptor getDefaultEditor(IRemoteFile remoteFile)
{
	IFile localFile = getLocalResource(remoteFile);
	
	if (localFile == null || !localFile.exists()) {
		return registry.getDefaultEditor(remoteFile.getName());
	}
	else 
	{
		IEditorDescriptor descriptor = IDE.getDefaultEditor(localFile);
		if (descriptor == null)
		{
			descriptor = getDefaultTextEditor();
		}
		return descriptor;
	}
}

protected void setDefaultEditor(IRemoteFile remoteFile, String editorId)
{
	IFile localFile = getLocalResource(remoteFile);
	
	if (localFile == null) {
		registry.setDefaultEditor(remoteFile.getName(), editorId);
	}
	else {
		IDE.setDefaultEditor(localFile, editorId);
	}
}


protected IEditorRegistry getEditorRegistry()
{
	return RSEUIPlugin.getDefault().getWorkbench().getEditorRegistry();
}

protected IEditorDescriptor getDefaultTextEditor()
{
	IEditorRegistry registry = getEditorRegistry();
	return registry.findEditor("org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
}

/* (non-Javadoc)
 * Fills the menu with perspective items.
 */
public void fill(Menu menu, int index) 
{
	if (_remoteFile == null) {
		return;
	}

	IEditorDescriptor defaultEditor = registry.findEditor("org.eclipse.ui.DefaultTextEditor"); // may be null //$NON-NLS-1$
	IEditorDescriptor preferredEditor = getPreferredEditor(_remoteFile); // may be null
	
	Object[] editors = registry.getEditors(getFileName());
	Collections.sort(Arrays.asList(editors), comparer);

	boolean defaultFound = false;
	
	//Check that we don't add it twice. This is possible
	//if the same editor goes to two mappings.
	ArrayList alreadyMapped= new ArrayList();

	for (int i = 0; i < editors.length; i++) {
		IEditorDescriptor editor = (IEditorDescriptor) editors[i];
		if(!alreadyMapped.contains(editor)){
			createMenuItem(menu, editor, preferredEditor);
			if (defaultEditor != null && editor.getId().equals(defaultEditor.getId()))
				defaultFound = true;
			alreadyMapped.add(editor);
		}		
	}

	// Only add a separator if there is something to separate
	if (editors.length > 0)
		new MenuItem(menu, SWT.SEPARATOR);

	// Add default editor. Check it if it is saved as the preference.
	if (!defaultFound && defaultEditor != null) {
		createMenuItem(menu, defaultEditor, preferredEditor);
	}

	// Add system editor (should never be null)
	IEditorDescriptor descriptor = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
	createMenuItem(menu, descriptor, preferredEditor);
	
	//DKM- disable inplace editor for now
	/*
	// Add system in-place editor (can be null)
	descriptor = registry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
	if (descriptor != null) {
		createMenuItem(menu, descriptor, preferredEditor);
	}
	*/	
	createDefaultMenuItem(menu, _remoteFile);
	
	// create other menu
	createOtherMenuItem(menu, _remoteFile);
	
}


/* (non-Javadoc)
 * Returns whether this menu is dynamic.
 */
public boolean isDynamic() 
{
	return true;
}


/**
 * Creates the menu item for clearing the current selection.
 *
 * @param menu the menu to add the item to
 * @param file the file bing edited
 */
protected void createDefaultMenuItem(Menu menu, final IRemoteFile file) 
{
	final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
	IEditorDescriptor defaultEditor = getDefaultEditor(file);
	menuItem.setSelection(defaultEditor == null);
	menuItem.setText(FileResources.DefaultEditorDescription_name); 
	
	Listener listener = new Listener() 
	{
		public void handleEvent(Event event) 
		{
			switch (event.type) 
			{
				case SWT.Selection:
					if(menuItem.getSelection()) 
					{
						setDefaultEditor(file, null);
		
						IEditorDescriptor defaultEditor = null;
						
						try {
							defaultEditor = getEditorDescriptor(file);
							openEditor(file, defaultEditor);
						}
						catch (PartInitException e) {
							SystemBasePlugin.logError("Error getting default editor descriptor", e); //$NON-NLS-1$
						}
					}
					break;
			}
		}
	};
	
	menuItem.addListener(SWT.Selection, listener);
}

protected IEditorDescriptor getEditorDescriptor(IRemoteFile file) throws PartInitException {
	return IDE.getEditorDescriptor(file.getName(), true);
}

}
