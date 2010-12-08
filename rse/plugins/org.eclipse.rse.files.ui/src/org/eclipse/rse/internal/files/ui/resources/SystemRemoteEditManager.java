/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [177523] Unify singleton getter methods
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Martin Oberhuber (Wind River) - [189130] Move SystemIFileProperties from UI to Core
 * David McKnight   (IBM) - [195285] mount path mapper changes
 * David McKnight   (IBM)        - [228343] RSE unable to recover after RemoteSystemsTempfiles deletion
 * David McKnight   (IBM)        - [253262] Cache Cleanup is removing .settings directory
 * David McKnight   (IBM)        - [245260] Different user's connections on a single host are mapped to the same temp files cache
 * David McKnight   (IBM)        - [276103] Files with names in different cases are not handled properly
 *******************************************************************************/

package org.eclipse.rse.internal.files.ui.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.files.ui.resources.ISystemMountPathMapper;
import org.eclipse.rse.files.ui.resources.SystemEditableRemoteFile;
import org.eclipse.rse.files.ui.resources.UniversalFileTransferUtility;
import org.eclipse.rse.internal.subsystems.files.core.ISystemFilePreferencesConstants;
import org.eclipse.rse.subsystems.files.core.SystemIFileProperties;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.RemoteFile;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.view.ISystemEditableRemoteObject;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;


/**
 * This is a singleton class that manages the remote editing
 */

public class SystemRemoteEditManager
{
	public static final String REMOTE_EDIT_PROJECT_NAME = "RemoteSystemsTempFiles"; //$NON-NLS-1$
	public static final String REMOTE_EDIT_PROJECT_NATURE_ID = "org.eclipse.rse.ui.remoteSystemsTempNature"; //$NON-NLS-1$
	public static final String REMOTE_EDIT_PROJECT_BUILDER_ID = "org.eclipse.rse.ui.remoteSystemsTempBuilder"; //$NON-NLS-1$

	private static SystemRemoteEditManager inst;
	//private RSEUIPlugin plugin;
	private List _mountPathMappers;

	/**
	 * Constructor for SystemRemoteEditManager
	 */
	private SystemRemoteEditManager()
	{
		super();
		//plugin = RSEUIPlugin.getDefault();
		registerMountPathMappers();
	}

	/**
	 * Get the singleton instance
	 */
	public static SystemRemoteEditManager getInstance()
	{
		if (inst == null)
			inst = new SystemRemoteEditManager();
		return inst;
	}

	/**
	 * Return the hostname that corresponds to the resource specified on the host specified.  If
	 * the resource is actually on the specified host, then the result will be the same as hostname.  If
	 * the resource is mounted then a mount path mapper has the opportunity to return the actual host.
	 * @param hostname the system on which a resource is obtained (may contain the file via a mount)
	 * @param remotePath the path on the host where the resource is obtained
	 * @param subsystem the subsystem
	 * @return the actual host where the resource exists
	 */
	public String getActualHostFor(String hostname, String remotePath, IRemoteFileSubSystem subsystem)
	{
		ISystemMountPathMapper mapper = getMountPathMapperFor(hostname, remotePath, subsystem);
		if (mapper != null)
		{
			return mapper.getActualHostFor(hostname, remotePath);
		}
		else
		{
			return hostname;
		}
	}

	/**
	 * Return the path to use on the system (i.e. Windows) for saving from the workspace to remote
	 * @param hostname the remote host
	 * @param remotePath the file path on the remote host
	 * @param subsystem the subsystem
	 * @return the system path
	 */
	public String getMountPathFor(String hostname, String remotePath, IRemoteFileSubSystem subsystem)
	{
		ISystemMountPathMapper mapper = getMountPathMapperFor(hostname, remotePath, subsystem);
		if (mapper != null)
		{
			return mapper.getMountedMappingFor(hostname, remotePath);
		}
		else
		{
			return remotePath;
		}
	}


	/**
	 * Return the path to use relative to the hostname in the
	 * RemoteSystemsTempFiles project for saving a local replica
	 *
	 * @param hostname the originating remote host
	 * @param remotePath the file path on the system (i.e. Windows)
	 * @param subsystem the remote subsystem. The subsystem may be null if none
	 *            is available.
	 * @return the relative replica path
	 */
	public String getWorkspacePathFor(String hostname, String remotePath, IRemoteFileSubSystem subsystem)
	{
		ISystemMountPathMapper mapper = getMountPathMapperFor(hostname, remotePath, subsystem);
				
		if (mapper != null)
		{
			return mapper.getWorkspaceMappingFor(hostname, remotePath, subsystem);
		}
		else
		{
			return remotePath;
		}
	}

	/**
	 * Return the appropriate registered mapper for a host & path
	 *
	 * @param hostname the originating remote host
	 * @param remotePath the file path on the system (i.e. Windows)
	 * @param subsystem the remote subsystem. The subsystem may be null if none
	 *            is available.
	 * @return appropriate mapper
	 */
	public ISystemMountPathMapper getMountPathMapperFor(String hostname, String remotePath, IRemoteFileSubSystem subsystem)
	{
		ISystemMountPathMapper result = null;
		for (int i = 0; i < _mountPathMappers.size(); i++)
		{
			ISystemMountPathMapper mapper = (ISystemMountPathMapper) _mountPathMappers.get(i);
			if (mapper != null)
			{
				if (mapper.handlesMappingFor(hostname, remotePath, subsystem))
				{
					if (result == null) {
						result = mapper;
					}
					else if (mapper.getPriority(hostname, remotePath, subsystem) < result.getPriority(hostname, remotePath, subsystem)){
						result = mapper;
					}
				}
			}
		}
		
		// no result - fall back to the default
		if (result == null){
			return new DefaultMountPathMapper();
		}
		
		return result;
	}

	protected void registerMountPathMappers()
	{
		_mountPathMappers = new ArrayList();

		// Get reference to the plug-in registry
		IExtensionRegistry registry = Platform.getExtensionRegistry();

		// Get configured extenders
		IConfigurationElement[] systemTypeExtensions = registry.getConfigurationElementsFor("org.eclipse.rse.ui", "mountPathMappers"); //$NON-NLS-1$ //$NON-NLS-2$

		for (int i = 0; i < systemTypeExtensions.length; i++)
		{
			try
			{
				_mountPathMappers.add(systemTypeExtensions[i].createExecutableExtension("class")); //$NON-NLS-1$
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Check if a remote edit project exists
	 * @return true if it does
	 */
	public boolean doesRemoteEditProjectExist()
	{
		IWorkspaceRoot root = SystemBasePlugin.getWorkspaceRoot();

		IProject editProject = root.getProject(REMOTE_EDIT_PROJECT_NAME);

		if ((editProject != null) && (editProject.exists()) && (editProject.isOpen()))
			return true;
		return false;
	}

	/**
	 * Get the project that in which all folders and files are held temporarily
	 * for remote editing. Create the project if it doesn't exist already, and opens
	 * it if it is not already open.
	 * @return the project where all files should be stored during remote edit.
	 */
	public IProject getRemoteEditProject()
	{
		IWorkspaceRoot root = SystemBasePlugin.getWorkspaceRoot();

		IProject editProject = root.getProject(REMOTE_EDIT_PROJECT_NAME);

		if ((editProject != null) && (editProject.exists()) && (editProject.isOpen()))
		{
			try
			{
				/* no more java support
				if (!editProject.hasNature(JavaCore.NATURE_ID))
				{
				addJavaSupport(editProject);
				}
				* no need for this anymore
				 * normally there is no cdt - so let's not do this everytime
				if (!editProject.hasNature("org.eclipse.cdt.core.cnature"))
				{
				addCSupport(editProject);
				}
				*/
			}
			catch (Exception e)
			{

			}
			return editProject;
		}

		if ((editProject == null) || !(editProject.exists()) || !(editProject.isOpen()))
			editProject = createRemoteEditProject();

		return editProject;
	}

	/**
	 * Creates the remote project, and opens it if it not open.
	 * @return the project where all files should be stored during remote editing
	 */
	private IProject createRemoteEditProject()
	{
		IWorkspaceRoot root = SystemBasePlugin.getWorkspaceRoot();

		IProject editProject = root.getProject(REMOTE_EDIT_PROJECT_NAME);

		if ((editProject != null) && (editProject.exists()) && (editProject.isOpen()))
		{
			/*
			try
			{
				// no java or c support - this needs to be contributed from elsewhere
				if (!editProject.hasNature(JavaCore.NATURE_ID))
				{
					addJavaSupport(editProject);
				}

				if (!editProject.hasNature("org.eclipse.cdt.core.cnature"))
				{
					addCSupport(editProject);
				}

			}
			catch (CoreException e)
			{
			}
			*/
			return editProject;
		}

		if (editProject == null)
		{
			// log error and throw an exception
		}

		try
		{
			if (editProject != null)
			{
				IProgressMonitor monitor = new NullProgressMonitor();
				if (!editProject.exists())
					editProject.create(monitor);

				if (!editProject.isOpen()){
					try {
						editProject.open(monitor);
					}
					catch (CoreException e) {
						// probably no .project file
						// time to start again!
						editProject.delete(true, monitor);
						editProject.create(monitor);
						editProject.open(monitor);
					}
				}

			IProjectDescription description = editProject.getDescription();
			String[] natures = description.getNatureIds();
			String[] newNatures = new String[natures.length + 1];

			// copy all previous natures
			for (int i = 0; i < natures.length; i++)
			{
				newNatures[i] = natures[i];
			}


			newNatures[newNatures.length - 1] = REMOTE_EDIT_PROJECT_NATURE_ID;

			description.setNatureIds(newNatures);
			editProject.setDescription(description, null);
			// editProject.setDefaultCharset(SystemEncodingUtil.ENCODING_UTF_8, new NullProgressMonitor());


			// add java support
			//addJavaSupport(editProject);

			// add c support
			//addCSupport(editProject);
			}
		}
		catch (CoreException e)
		{
			SystemBasePlugin.logError("Error creating temp project", e); //$NON-NLS-1$
		}
		return editProject;
	}
/*
	public void addCSupport(IProject editProject)
	{

		try
		{
			IProjectDescription description = editProject.getDescription();
			String[] natures = description.getNatureIds();
			ICommand[] buildSpecs = description.getBuildSpec();

			String[] newNatures = new String[natures.length + 2];

			// copy all previous natures
			for (int i = 0; i < natures.length; i++)
			{
				newNatures[i] = natures[i];
			}

			newNatures[newNatures.length - 2] = "org.eclipse.cdt.core.cnature";
			newNatures[newNatures.length - 1] = "org.eclipse.cdt.core.ccnature";//CCProjectNature.CC_NATURE_ID;

			description.setNatureIds(newNatures);

			// make sure no build specs added
			description.setBuildSpec(buildSpecs);

			editProject.setDescription(description, null);
		}
		catch (Exception e)
		{

		}
	}
	*/
	/*
	public void addJavaSupport(IProject editProject)
	{

		try
		{
			IProjectDescription description = editProject.getDescription();
			String[] natures = description.getNatureIds();
			ICommand[] buildSpecs = description.getBuildSpec();

			String[] newNatures = new String[natures.length + 1];

			// copy all previous natures
			for (int i = 0; i < natures.length; i++)
			{
				newNatures[i] = natures[i];
			}

			newNatures[newNatures.length - 1] = JavaCore.NATURE_ID;

			description.setNatureIds(newNatures);
			description.setBuildSpec(buildSpecs);



			editProject.setDescription(description, null);


			IJavaProject proj = JavaCore.create(editProject);
			IPath outputLocation = proj.getOutputLocation();


			// classpath
			IClasspathEntry[] classpath= new IClasspathEntry[1];

			IPath jdkLoc = new Path("org.eclipse.jdt.launching.JRE_CONTAINER");
			ClasspathEntry jreEntry = new ClasspathEntry(
					IPackageFragmentRoot.K_BINARY,
					IClasspathEntry.CPE_CONTAINER,
					jdkLoc,
					ClasspathEntry.INCLUDE_ALL,
					ClasspathEntry.EXCLUDE_NONE,
					null, // source attachment
					null, // source attachment root
					null, // specific output folder
					false);
			classpath[0]=jreEntry;


			proj.setRawClasspath(classpath, outputLocation, null);

			((JavaProject)proj).deconfigure();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	*/


	/**
	 * Get the location of the project used for remote editing
	 */
	public IPath getRemoteEditProjectLocation()
	{
		//if (!doesRemoteEditProjectExist())
		//	return null;
		// DKM - originally return null if it doesn't exist
		// but looks like lots of calls reference this expected to get something back
		// so I'll let project creation happen here

		return getRemoteEditProject().getLocation();
	}

	/**
	 * Get the absolute path of the project used for remote editing
	 */
	public String getRemoteEditProjectAbsolutePath()
	{
		return getRemoteEditProjectLocation().makeAbsolute().toOSString();
	}

	protected int caculateCacheSize()
	{
		if (!doesRemoteEditProjectExist())
		{
			return 0;
		}
		IProject project = getRemoteEditProject();
		String path = project.getLocation().toOSString();
		File file = new File(path);
		return calculateSize(file);
	}

	private int calculateSize(File file)
	{
		int size = 0;
		if (file.isFile())
		{
			size = (int) file.length();
		}
		else if (file.isDirectory())
		{
			File[] children = file.listFiles();
			for (int i = 0; i < children.length; i++)
			{
				size += calculateSize(children[i]);
			}
		}

		return size;
	}

	protected IFile getLeastRecentlyChangedFile(List deletedList)
	{
		// if no project exists, then no file exists
		if (!doesRemoteEditProjectExist())
			return null;

		IProject project = getRemoteEditProject();

		IFile result = getLeastRecentlyChangedFile(project, deletedList);
		deletedList.add(result);
		return result;
	}

	private IFile getLeastRecentlyChangedFile(IContainer file, List deletedList)
	{

		IFile result = null;
		try
		{
			IResource[] children = file.members();
			for (int i = 0; i < children.length; i++)
			{
				IFile candidate = null;
				IResource child = children[i];
				if (child instanceof IFolder)
				{
					if (!child.getName().equals(".settings")){  //$NON-NLS-1$
						candidate = getLeastRecentlyChangedFile((IFolder) child, deletedList);
					}
				}
				else
				{
					candidate = (IFile) child;
				}

				if (candidate != null && !deletedList.contains(candidate) && !candidate.getName().startsWith(".") && !isFileInUse(candidate, true)) //$NON-NLS-1$
				{
					if (result == null)
					{
						result = candidate;
					}
					else if (candidate.getLocation().toFile().lastModified() < result.getLocation().toFile().lastModified())
					{
						result = candidate;
					}
				}
			}
		}
		catch (Exception e)
		{
		}
		return result;
	}

	public ISystemEditableRemoteObject getEditableFor(IFile fileToDelete, boolean quickCheck)
	{
		SystemIFileProperties properties = new SystemIFileProperties(fileToDelete);
		Object object = properties.getRemoteFileObject();
		if (object != null)
		{
			ISystemEditableRemoteObject editableFile = (ISystemEditableRemoteObject) object;
			return editableFile;
		}
		else if (!quickCheck)
		{
			// no object in memory, so try to reconstruct it from ids
			String subsystemStr = properties.getRemoteFileSubSystem();
			String pathStr = properties.getRemoteFilePath();
			if (subsystemStr != null && pathStr != null)
			{
				ISubSystem subsystem = RSECorePlugin.getTheSystemRegistry().getSubSystem(subsystemStr);
				if (subsystem != null)
				{
					Object rmtObject = null;
					try
					{
						rmtObject = subsystem.getObjectWithAbsoluteName(pathStr, new NullProgressMonitor());
					}
					catch (Exception e)
					{
						return null;
					}
					if (rmtObject != null && rmtObject instanceof IAdaptable)
					{
						ISystemRemoteElementAdapter adapter = (ISystemRemoteElementAdapter) ((IAdaptable) rmtObject).getAdapter(ISystemRemoteElementAdapter.class);
						if (adapter != null)
						{
							return adapter.getEditableRemoteObject(rmtObject);
						}
					}
				}
			}
		}
		return null;
	}

	public class DeleteFileRunnable implements Runnable
	{
		private IFile _theFile;
		public DeleteFileRunnable(IFile theFile)
		{
			_theFile = theFile;
		}

		public void run()
		{
			if (!inUse())
			{
				//System.out.println("deleting " + _theFile.getName());
				_theFile.getLocation().toFile().delete();
			}
		}

		public boolean inUse()
		{
			// if no temp files project, not in use
			if (!doesRemoteEditProjectExist())
				return false;

			IWorkbenchWindow activeWindow = SystemBasePlugin.getActiveWorkbenchWindow();
			IWorkbenchPage activePage = activeWindow.getActivePage();

			IEditorReference[] activeReferences = activePage.getEditorReferences();

			IEditorPart part;

			for (int k = 0; k < activeReferences.length; k++)
			{
				part = activeReferences[k].getEditor(true);

				if (part != null)
				{
					IEditorInput editorInput = part.getEditorInput();

					if (editorInput instanceof IFileEditorInput)
					{
						IFile file = ((IFileEditorInput) editorInput).getFile();
						if (file.equals(_theFile))
						{
							//System.out.println(file.getName() + " is in use");
							return true;
						}
					}
				}

			}
			return false;
		}
	}

	public boolean isFileInUse(IFile fileToDelete, boolean quickCheck)
	{
		// first check for dirty flag
		SystemIFileProperties properties = new SystemIFileProperties(fileToDelete);
		boolean isDirty = properties.getDirty();
		if (isDirty)
		{
			return true;
		}
		else
		{

			ISystemEditableRemoteObject editable = getEditableFor(fileToDelete, quickCheck);
			if (editable != null && quickCheck)
			{
				return true;
			}

			if (editable != null)
			{
				try
				{
					boolean result = editable.checkOpenInEditor() == ISystemEditableRemoteObject.NOT_OPEN;
					if (!result)
					{
						return !result;
					}
				}
				catch (Exception e)
				{
				}
			}
		}
		return false;
	}

	protected void cleanupCache()
	{

		IPreferenceStore store = RSEUIPlugin.getDefault().getPreferenceStore();
		boolean enableMaxSize = store.getBoolean(ISystemFilePreferencesConstants.LIMIT_CACHE);
		if (enableMaxSize)
		{
			int max = Integer.parseInt(ISystemFilePreferencesConstants.DEFAULT_MAX_CACHE_SIZE) * 1000000;

			// get the cache limit
			try {
				String maxSize = store.getString(ISystemFilePreferencesConstants.MAX_CACHE_SIZE);

				if (maxSize != null && !maxSize.equals("")) { //$NON-NLS-1$
					max = Integer.parseInt(maxSize) * 1000000;
				}
			}
			catch (NumberFormatException nfe) {
				SystemBasePlugin.logError("Could not get max cache size", nfe); //$NON-NLS-1$
				max = Integer.parseInt(ISystemFilePreferencesConstants.DEFAULT_MAX_CACHE_SIZE) * 1000000;
			}

			try {

				//	get the current cache size
				int currentSize = caculateCacheSize();
				if (currentSize > max)
				{
					// determine what to get rid of
					int delta = currentSize - max;

					List deletedList = new ArrayList();
					while (delta > 0)
					{

						// need to purge delta from the cache
						IFile leastRecent = getLeastRecentlyChangedFile(deletedList);
						if (leastRecent != null)
						{

							File theFile = leastRecent.getLocation().toFile();

							int sizeSaved = (int) theFile.length();
							Display.getDefault().asyncExec(new DeleteFileRunnable(leastRecent));

							// delete file
							delta -= sizeSaved;
						}
						else
						{
							delta = 0;
						}
					}
				}
			}
			catch (Exception e)
			{
				SystemBasePlugin.logError("Error occured trying to clean cache", e); //$NON-NLS-1$
				// e.printStackTrace();
			}
		}
	}

	/**
	 * Refresh the remote edit project
	 */
	public void refreshRemoteEditProject()
	{
		// no temp files project, then nothing to refresh
		if (!doesRemoteEditProjectExist())
		{
			return;
		}
		try
		{
			IProject project = getRemoteEditProject();
			if (!project.getWorkspace().isTreeLocked())
			{
				cleanupCache();
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
			}
		}
		catch (Exception e)
		{
			SystemBasePlugin.logError("Error refreshing remote edit project", e); //$NON-NLS-1$
		}
	}

	/**
	  * Refresh the remote edit project
	  */
	public void refreshRemoteEditContainer(IContainer parent)
	{
		// no project exists, then nothing to refresh
		if (!doesRemoteEditProjectExist())
			return;

		try
		{

			IProject project = getRemoteEditProject();
			if (!project.getWorkspace().isTreeLocked())
			{
				cleanupCache();
				parent.refreshLocal(IResource.DEPTH_ONE, null);
			}
		}
		catch (Exception e)
		{
			SystemBasePlugin.logError("Error refreshing remote edit project", e); //$NON-NLS-1$
		}
	}
	
	
	public static SystemEditableRemoteFile getEditableRemoteObject(Object element, IEditorDescriptor descriptor)
	{
		SystemEditableRemoteFile editable = null;
		RemoteFile remoteFile = (RemoteFile) element;
		if (remoteFile.isFile())
		{
			try
			{
				IFile file = (IFile)UniversalFileTransferUtility.getTempFileFor(remoteFile);
				if (file != null)
				{
					SystemIFileProperties properties = new SystemIFileProperties(file);
					
					Object obj = properties.getRemoteFileObject();
					if (obj != null && obj instanceof SystemEditableRemoteFile)
					{
						editable = (SystemEditableRemoteFile) obj;
						
						String remotePath = remoteFile.getAbsolutePath();
						String replicaRemotePath = editable.getAbsolutePath();
						// first make sure that the correct remote file is referenced (might be difference because of different case)
						if (!replicaRemotePath.equals(remotePath)){ // for bug 276103
							
							IEditorPart editor = editable.getEditorPart();
							boolean editorWasClosed = false;
							if (editor.isDirty()){
								editorWasClosed = editor.getEditorSite().getPage().closeEditor(editor, true);
								if (editorWasClosed)
									editable.doImmediateSaveAndUpload();								
							}
							else {
								editorWasClosed = editor.getEditorSite().getPage().closeEditor(editor, true);
							}
							
							if (!editorWasClosed){
								// use cancelled operation so we need to get out of here
								return null;
							}
							
							try {
								IFile originalFile = editable.getLocalResource();
								originalFile.delete(true, new NullProgressMonitor());												
							}
							catch (CoreException e){
							}
							// fall through and let the new editable get created
						}
						else {					
							return editable;
						}
					}
				}
				
				if (descriptor != null){
					editable = new SystemEditableRemoteFile(remoteFile, descriptor);
				}
				else {
					editable = new SystemEditableRemoteFile(remoteFile);
				}
			}
			catch (Exception e)
			{
			}
		}
		return editable;
	}

}