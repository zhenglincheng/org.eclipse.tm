/********************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others. All rights reserved.
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
 * Kushal Munir (IBM) - moved to internal package
 * Martin Oberhuber (Wind River) - [181917] EFS Improvements: Avoid unclosed Streams,
 *    - Fix early startup issues by deferring FileStore evaluation and classloading,
 *    - Improve performance by RSEFileStore instance factory and caching IRemoteFile.
 *    - Also remove unnecessary class RSEFileCache and obsolete branding files.
 ********************************************************************************/

package org.eclipse.rse.internal.eclipse.filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.RemoteChildrenContentsType;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.IHostFile;
import org.eclipse.rse.subsystems.files.core.model.RemoteFileFilterString;
import org.eclipse.rse.subsystems.files.core.model.RemoteFileUtility;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.FileServiceSubSystem;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.IFileServiceSubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystemConfiguration;
import org.eclipse.rse.subsystems.files.core.subsystems.RemoteFileContext;
import org.eclipse.rse.ui.RSEUIPlugin;

/**
 * Implementation of IFileStore for RSE.
 * 
 * The RSEFileStore delegates to this impl class in order to defer class
 * loading until file contents are really needed.
 */
public class RSEFileStoreImpl extends FileStore
{
	private RSEFileStore _parent;

	//cached IRemoteFile object: an Object to avoid early class loading
	private transient IRemoteFile _remoteFile;
	
	/**
	 * Constructor to use if the parent file store is known.
	 * @param parent the parent file store.
	 */
	public RSEFileStoreImpl(RSEFileStore parent) {
		_parent = parent;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#getChild(java.lang.String)
	 */
	public IFileStore getChild(String name) {
		return _parent.getChild(name);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#getName()
	 */
	public String getName() {
		return _parent.getName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#getParent()
	 */
	public IFileStore getParent() {
		return _parent.getParent();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#toURI()
	 */
	public URI toURI() {
		return _parent.toURI();
	}

	/**
	 * Return the best RSE connection object matching the given host name.
	 * 
	 * @param hostNameOrAddr IP address of requested host.
	 * @return RSE connection object matching the given host name, or
	 *     <code>null</code> if no matching connection object was found.
	 */
	public static IHost getConnectionFor(String hostNameOrAddr, IProgressMonitor monitor) {
		if (hostNameOrAddr==null) {
			return null;
		}
		ISystemRegistry sr = RSEUIPlugin.getTheSystemRegistry();
		IHost[] connections = sr.getHosts();
		
		//FIXME HACK workaround until we get an API method to know when persistent data is fully restored
		//Without this hack, the first time a directory is expanded RSEUIPlugin is activated and it
		//is too slow reading persistent data. We wait up to 5 seconds for RSE to initialize.
		if (connections.length==0) {
			long startTime = System.currentTimeMillis();
			long endTime = startTime+5000;
			//int iterations=0;
			do {
				try {
					Thread.sleep(500);
					//iterations++;
				} catch(InterruptedException e) {
					break;
				}
				if (monitor!=null) {
					monitor.worked(1);
					if (monitor.isCanceled()) {
						break;
					}
				}
				connections = sr.getHosts();
			} while((connections.length==0 || getConnectionFor(hostNameOrAddr, monitor)==null)
					&& System.currentTimeMillis()<endTime);
			//System.out.println("Time: "+ (System.currentTimeMillis()-startTime)+", Iterations: "+iterations);
			//System.out.println("Dbg Breakpoint");
		}

		IHost unconnected = null;
		
		for (int i = 0; i < connections.length; i++) {
			
			IHost con = connections[i];
			
			//TODO use more elaborate methods of checking whether two
			//host names/IP addresses are the same; or, use the host alias
			if (hostNameOrAddr.equalsIgnoreCase(con.getHostName())) {
				IRemoteFileSubSystem fss = getRemoteFileSubSystem(con);
				if (fss!=null && fss.isConnected()) {
					return con;
				} else {
					unconnected = con;
				}
			}
		}
		
		return unconnected;
	}
	
	/**
	 * Return the best available remote file subsystem for a connection.
	 * Criteria are:
	 * <ol>
	 *   <li>A connected FileServiceSubsystem</li>
	 *   <li>A connected IRemoteFileSubSystem</li>
	 *   <li>An unconnected FileServiceSubsystem</li>
	 *   <li>An unconnected IRemoteFileSubSystem</li>
	 * </ol>
	 * @param host
	 * @return an IRemoteFileSubSystem for the given connection, or 
	 *     <code>null</code> if no IRemoteFileSubSystem is configured.
	 */
	public static IRemoteFileSubSystem getRemoteFileSubSystem(IHost host) {
		IRemoteFileSubSystem candidate = null;
		FileServiceSubSystem serviceCandidate = null;
		IRemoteFileSubSystem[] subSys = RemoteFileUtility.getFileSubSystems(host);
		for (int i=0; i<subSys.length; i++) {
			if (subSys[i] instanceof FileServiceSubSystem) {
				if (subSys[i].isConnected()) {
					//best candidate: service and connected
					return subSys[i];
				} else if (serviceCandidate==null) {
					serviceCandidate = (FileServiceSubSystem)subSys[i];
				}
			} else if(candidate==null) {
				candidate=subSys[i];
			} else if(subSys[i].isConnected() && !candidate.isConnected()) {
				candidate=subSys[i];
			}
		}
		//Now find the best candidate
		if (candidate!=null && candidate.isConnected()) {
			return candidate;
		} else if (serviceCandidate!=null) {
			return serviceCandidate;
		}
		return candidate;
	}

	/**
	 * Returns the best connected file subsystem for this file store.
	 * Never returns <code>null</code>.
	 * @param hostNameOrAddr host name or IP address
	 * @param monitor progress monitor
	 * @return The best connected file subsystem for this file store.
	 * @throws CoreException if no file subsystem could be found or connected.
	 */
	public static IRemoteFileSubSystem getConnectedFileSubSystem(String hostNameOrAddr, IProgressMonitor monitor) throws CoreException
	{
		IHost con = RSEFileStoreImpl.getConnectionFor(hostNameOrAddr, monitor);
		if (con == null) {
			throw new CoreException(new Status(IStatus.ERROR, 
					Activator.getDefault().getBundle().getSymbolicName(),
					"Connection not found for host: "+hostNameOrAddr));
		}
		IRemoteFileSubSystem subSys = RSEFileStoreImpl.getRemoteFileSubSystem(con);
		if (subSys == null) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getDefault().getBundle().getSymbolicName(),
					"No file subsystem found on host: "+hostNameOrAddr+" connection "+con.getAliasName()));
		}
		if (!subSys.isConnected()) {
			try {
				if (monitor==null) monitor=new NullProgressMonitor();
				subSys.connect(monitor, false);
			}
			catch (Exception e) {
				throw new CoreException(new Status(IStatus.ERROR,
						Activator.getDefault().getBundle().getSymbolicName(),
						"Could not connect to host: "+hostNameOrAddr+" subsystem "+subSys.getConfigurationId(), e));
			}
		}
		return subSys;
	}

	private IRemoteFile getCachedRemoteFile() {
		return _remoteFile;
	}
	
	private void cacheRemoteFile(IRemoteFile remoteFile) {
		//if (_remoteFile != remoteFile) _remoteFile = remoteFile;
		_remoteFile = remoteFile;
	}
	
	/**
	 * Returns an IRemoteFile for this file store.
	 * Requires that the file subsystem is connected.
	 * @param monitor progress monitor
	 * @param forceExists if <code>true</code>, throw an exception if the remote file does not exist
	 * @return an IRemoteFile for this file store
	 * @throws CoreException if connecting is not possible.
	 */
	private synchronized IRemoteFile getRemoteFileObject(IProgressMonitor monitor, boolean forceExists) throws CoreException {
		IRemoteFile remoteFile = getCachedRemoteFile();
		if (remoteFile!=null) {
			if (remoteFile.getParentRemoteFileSubSystem().isConnected()) {
				return remoteFile;
			} else {
				//need to re-initialize cache
				remoteFile=null;
				cacheRemoteFile(null);
			}
		}

		RSEFileStore parentStore = _parent.getParentStore();
		if (parentStore!=null) {
			//Handle was created naming a parent file store
			IRemoteFile parent = parentStore.getImpl().getRemoteFileObject(monitor, forceExists);
			if (parent==null) {
				throw new CoreException(new Status(IStatus.ERROR, 
						Activator.getDefault().getBundle().getSymbolicName(),
						"Could not get remote file"));
			}
			try {
				remoteFile = parent.getParentRemoteFileSubSystem().getRemoteFileObject(parent, _parent.getName());
			} catch(Exception e) {
				throw new CoreException(new Status(IStatus.ERROR,
						Activator.getDefault().getBundle().getSymbolicName(),
						"Could not get remote file", e));
			}
		} else {
			//Handle was created with an absolute name
			IRemoteFileSubSystem subSys = RSEFileStoreImpl.getConnectedFileSubSystem(_parent.getHost(), monitor);
			try {
				//TODO method missing a progressmonitor!
				remoteFile = subSys.getRemoteFileObject(_parent.getAbsolutePath());
			}
			catch (Exception e) {
				throw new CoreException(new Status(
						IStatus.ERROR, 
						Activator.getDefault().getBundle().getSymbolicName(), 
						"Could not get remote file", e));
			}
		}

		cacheRemoteFile(remoteFile);
		if (forceExists && (remoteFile == null || !remoteFile.exists())) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getDefault().getBundle().getSymbolicName(),
					"The file store does not exist"));
		}
		return remoteFile;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#childNames(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		
		String[] names;
		IRemoteFile remoteFile = getRemoteFileObject(monitor, true);
		IRemoteFileSubSystem subSys = remoteFile.getParentRemoteFileSubSystem();
		if (!remoteFile.isStale() && remoteFile.hasContents(RemoteChildrenContentsType.getInstance()) && !(subSys instanceof IFileServiceSubSystem))
		{
			Object[] children = remoteFile.getContents(RemoteChildrenContentsType.getInstance());
			names = new String[children.length];
			                
			for (int i = 0; i < children.length; i++)
			{
				names[i] = ((IRemoteFile)children[i]).getName();
			}
		}
		else
		{
			try {
				IRemoteFile[] children = null;
				
				if (subSys instanceof FileServiceSubSystem) {
					FileServiceSubSystem fileServiceSubSystem = ((FileServiceSubSystem)subSys);
					IHostFile[] results = fileServiceSubSystem.getFileService().getFilesAndFolders(monitor, remoteFile.getAbsolutePath(), "*"); //$NON-NLS-1$
					IRemoteFileSubSystemConfiguration config = subSys.getParentRemoteFileSubSystemConfiguration();
					RemoteFileFilterString filterString = new RemoteFileFilterString(config, remoteFile.getAbsolutePath(), "*"); //$NON-NLS-1$
					filterString.setShowFiles(true);
					filterString.setShowSubDirs(true);
					RemoteFileContext context = new RemoteFileContext(subSys, remoteFile, filterString);
					children = fileServiceSubSystem.getHostFileToRemoteFileAdapter().convertToRemoteFiles(fileServiceSubSystem, context, remoteFile, results);
				}
				else {
					children = subSys.listFoldersAndFiles(remoteFile, "*", monitor); //$NON-NLS-1$
				}
				
				names = new String[children.length];
				
				for (int i = 0; i < children.length; i++) {
					names[i] = (children[i]).getName();
				}		
			}
			catch (SystemMessageException e) {
				names = new String[0];
			}
		}
		
		return names;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#childInfos(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
		
		FileInfo[] infos;
		IRemoteFile remoteFile = getRemoteFileObject(monitor, true);
		IRemoteFileSubSystem subSys = remoteFile.getParentRemoteFileSubSystem();
		if (!remoteFile.isStale() && remoteFile.hasContents(RemoteChildrenContentsType.getInstance()) && !(subSys instanceof IFileServiceSubSystem))
		{
			Object[] children = remoteFile.getContents(RemoteChildrenContentsType.getInstance());
			
			infos = new FileInfo[children.length];
			                
			for (int i = 0; i < children.length; i++)
			{
				IRemoteFile file = (IRemoteFile)(children[i]);
				FileInfo info = new FileInfo(file.getName());
				
				if (!file.exists()) {
					info.setExists(false);
				}
				else {
					info.setExists(true);
					info.setLastModified(file.getLastModified());
					boolean isDir = file.isDirectory();
					info.setDirectory(isDir);
					info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, !file.canWrite());
					info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, file.isExecutable());
					info.setAttribute(EFS.ATTRIBUTE_ARCHIVE, file.isArchive());
					info.setAttribute(EFS.ATTRIBUTE_HIDDEN, file.isHidden());

					if (!isDir) {
						info.setLength(file.getLength());
					}
				}
				
				infos[i] = info;
			}
		}
		else
		{
			try {
				
				IRemoteFile[] children = null;
				
				if (subSys instanceof FileServiceSubSystem) {
					FileServiceSubSystem fileServiceSubSystem = ((FileServiceSubSystem)subSys);
					IHostFile[] results = fileServiceSubSystem.getFileService().getFilesAndFolders(monitor, remoteFile.getAbsolutePath(), "*"); //$NON-NLS-1$
					IRemoteFileSubSystemConfiguration config = subSys.getParentRemoteFileSubSystemConfiguration();
					RemoteFileFilterString filterString = new RemoteFileFilterString(config, remoteFile.getAbsolutePath(), "*"); //$NON-NLS-1$
					filterString.setShowFiles(true);
					filterString.setShowSubDirs(true);
					RemoteFileContext context = new RemoteFileContext(subSys, remoteFile, filterString);
					children = fileServiceSubSystem.getHostFileToRemoteFileAdapter().convertToRemoteFiles(fileServiceSubSystem, context, remoteFile, results);
				}
				else {
					children = subSys.listFoldersAndFiles(remoteFile, "*", monitor); //$NON-NLS-1$
				}
				
				infos = new FileInfo[children.length];
				
				for (int i = 0; i < children.length; i++)
				{
					IRemoteFile file = children[i];
					FileInfo info = new FileInfo(file.getName());
					
					if (!file.exists()) {
						info.setExists(false);
					}
					else {
						info.setExists(true);
						info.setLastModified(file.getLastModified());
						boolean isDir = file.isDirectory();
						info.setDirectory(isDir);
						info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, !file.canWrite());
						info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, file.isExecutable());
						info.setAttribute(EFS.ATTRIBUTE_ARCHIVE, file.isArchive());
						info.setAttribute(EFS.ATTRIBUTE_HIDDEN, file.isHidden());
						//TODO Add symbolic link attribute

						if (!isDir) {
							info.setLength(file.getLength());
						}
					}
					
					infos[i] = info;
				}		
			}
			catch (SystemMessageException e) {
				//TODO check whether we should not throw an exception ourselves
				infos = new FileInfo[0];
			}
		}
		
		return infos;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#fetchInfo(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		
		// connect if needed. Will throw exception if not successful.
		IRemoteFile remoteFile = getRemoteFileObject(monitor, false);
		
		FileInfo info = new FileInfo(_parent.getName());
		if (remoteFile == null || !remoteFile.exists()) {
			info.setExists(false);
			return info;
		}
		
		info.setExists(true);
		info.setLastModified(remoteFile.getLastModified());
		boolean isDir = remoteFile.isDirectory();
		info.setDirectory(isDir);
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, !remoteFile.canWrite());
		info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, remoteFile.isExecutable());
		info.setAttribute(EFS.ATTRIBUTE_ARCHIVE, remoteFile.isArchive());
		info.setAttribute(EFS.ATTRIBUTE_HIDDEN, remoteFile.isHidden());

		if (!isDir) {
			info.setLength(remoteFile.getLength());
		}
		
		return info;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#openInputStream(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException 
	{
		IRemoteFile remoteFile = getRemoteFileObject(monitor, true);
		IRemoteFileSubSystem subSys = remoteFile.getParentRemoteFileSubSystem();
		
		if (remoteFile.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, 
					Activator.getDefault().getBundle().getSymbolicName(),
					"The file store represents a directory"));
		}
		
		if (remoteFile.isFile()) {
				
			try {
				return subSys.getInputStream(remoteFile.getParentPath(), remoteFile.getName(), true, monitor);
			}
			catch (SystemMessageException e) {
				throw new CoreException(new Status(IStatus.ERROR,
						Activator.getDefault().getBundle().getSymbolicName(),
						"Could not get input stream", e));
			}
		}
		
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#mkdir(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException 
	{
		IRemoteFile remoteFile = getRemoteFileObject(monitor, false);
		if (remoteFile==null) {
			throw new CoreException(new Status(IStatus.ERROR, 
					Activator.getDefault().getBundle().getSymbolicName(),
					"Could not get remote file"));
		}
		IRemoteFileSubSystem subSys = remoteFile.getParentRemoteFileSubSystem();
		if (!remoteFile.exists()) {
			try {
				remoteFile = subSys.createFolder(remoteFile);
			}
			catch (Exception e) {
				throw new CoreException(new Status(IStatus.ERROR,
						Activator.getDefault().getBundle().getSymbolicName(), 
						"The directory could not be created", e));
			}
			return _parent;
		}
		else if (remoteFile.isFile()) {
			throw new CoreException(new Status(IStatus.ERROR, 
					Activator.getDefault().getBundle().getSymbolicName(),
					"A file of that name already exists"));
		}
		else {
			return _parent;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#openOutputStream(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		
		IRemoteFile remoteFile = getRemoteFileObject(monitor, false);
		if (remoteFile==null) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getDefault().getBundle().getSymbolicName(),
					"Could not get remote file"));
		}
		IRemoteFileSubSystem subSys = remoteFile.getParentRemoteFileSubSystem();
		if (!remoteFile.exists()) {
			try {
				remoteFile = subSys.createFile(remoteFile);
			}
			catch (Exception e) {
				throw new CoreException(new Status(IStatus.ERROR,
						Activator.getDefault().getBundle().getSymbolicName(),
						"Could not create file", e));
			} 
		}
			
		if (remoteFile.isFile()) {
			
			try {
				return subSys.getOutputStream(remoteFile.getParentPath(), remoteFile.getName(), true, monitor);
			}
			catch (SystemMessageException e) {
				throw new CoreException(new Status(IStatus.ERROR,
						Activator.getDefault().getBundle().getSymbolicName(),
						"Could not get output stream", e));
			}
		}
		else if (remoteFile.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getDefault().getBundle().getSymbolicName(),
					"This is a directory"));
		}
		else {
			//TODO check what to do for symbolic links and other strange stuff
			return null;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#delete(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void delete(int options, IProgressMonitor monitor) throws CoreException 
	{
		IRemoteFile remoteFile = getRemoteFileObject(monitor, false);
		if (remoteFile==null || !remoteFile.exists()) {
			return;
		}
		else {
			IRemoteFileSubSystem subSys = remoteFile.getParentRemoteFileSubSystem();
			try {
				boolean success = subSys.delete(remoteFile, monitor);
				if (!success) {
					throw new CoreException(new Status(IStatus.ERROR,
							Activator.getDefault().getBundle().getSymbolicName(),
							"Could not delete file"));
				}
			}
			catch (Exception e) {
				throw new CoreException(new Status(IStatus.ERROR,
						Activator.getDefault().getBundle().getSymbolicName(),
						"Could not delete file", e));
			}
		}
	}
}