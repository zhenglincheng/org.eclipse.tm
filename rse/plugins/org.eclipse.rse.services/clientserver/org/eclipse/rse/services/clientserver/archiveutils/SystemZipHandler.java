/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
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
 * {Name} (company) - description of contribution.
 * Xuan Chen (IBM) - [192741] [Archives] Move a folder from within an Archive doesn't work if > 1 level deep
 * Xuan Chen (IBM) - [194293] [Local][Archives] Saving file second time in an Archive Errors
 * Xuan Chen (IBM) - [181784] [Archives] zipped text files have unexpected contents
 * Xuan Chen (IBM) - [160775] [api] rename (at least within a zip) blocks UI thread
 * Xuan Chen (IBM) - [209828] Need to move the Create operation to a job.
 * Xuan Chen (IBM) - [191370] [dstore] Supertransfer zip not deleted when canceling copy
 * Xuan Chen (IBM) - [214251] [archive] "Last Modified Time" changed for all virtual files/folders if rename/paste/delete of one virtual file.
 * Xuan Chen (IBM) - [214786] [regression][archive]rename a virtual directory does not work properly
 * Xuan Chen (IBM) - [191370] [dstore] Supertransfer zip not deleted when canceling copy
 * Martin Oberhuber (Wind River) - [cleanup] Add API "since" tags
 * Martin Oberhuber (Wind River) - [199854][api] Improve error reporting for archive handlers
 *******************************************************************************/

package org.eclipse.rse.services.clientserver.archiveutils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.rse.internal.services.clientserver.archiveutils.SystemArchiveUtil;
import org.eclipse.rse.internal.services.clientserver.archiveutils.SystemUniversalZipEntry;
import org.eclipse.rse.services.clientserver.IClientServerConstants;
import org.eclipse.rse.services.clientserver.ISystemFileTypes;
import org.eclipse.rse.services.clientserver.ISystemOperationMonitor;
import org.eclipse.rse.services.clientserver.SystemEncodingUtil;
import org.eclipse.rse.services.clientserver.SystemReentrantMutex;
import org.eclipse.rse.services.clientserver.java.BasicClassFileParser;
import org.eclipse.rse.services.clientserver.messages.SystemLockTimeoutException;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.clientserver.messages.SystemOperationCancelledException;
import org.eclipse.rse.services.clientserver.messages.SystemOperationFailedException;
import org.eclipse.rse.services.clientserver.messages.SystemUnexpectedErrorException;
import org.eclipse.rse.services.clientserver.search.SystemSearchLineMatch;
import org.eclipse.rse.services.clientserver.search.SystemSearchStringMatchLocator;
import org.eclipse.rse.services.clientserver.search.SystemSearchStringMatcher;


/**
 * Implements an ISystemArchiveHandler for ZIP files.
 * @author mjberger
 */
public class SystemZipHandler implements ISystemArchiveHandler
{

	protected ZipFile _zipfile; // The underlying zipfile associated with this handler.
	protected HashMap _virtualFS; // The virtual file system formed by the entries in _zipfile.
	//--------------------------------------------------------------------------------------------------------------------
	// Explanation of how the virtual file system is stored in a HashMap:
	//
	// _virtualFS is a HashMap of HashMaps. How does this suggest a tree structure?
	// The keys in _virtualFS are all Strings. There is one key in _virtualFS for every directory
	// in the virtual file system. The root directory has the key "". Associated with each key, is a value, and that value
	// is itself another HashMap, representing the contents of that directory. In the "inner" HashMap, each key is
	// a String giving the name of an object in the directory, and each associated value is a
	// VirtualChild representing that object itself.
	//
	// Note that if the object is a directory, then
	// the value representing it in the inner HashMap is still a VirtualChild, not another
	// HashMap. There are only two levels of HashMaps in the virtual file system. If the
	// object is a directory, then the VirtualChild object representing it will have
	// isDirectory == true. We can then find the contents of this directory, by going
	// back out to the outer HashMap, and retrieving the inner HashMap associated
	// with our directory's name.
	//
	// This file system is designed for quick retrieval of virtual objects.
	// Retrieving a single object whose full path is known can be done
	// in worst case O(1) time rather than O(h) time for a tree file system with height h.
	// Retrieving the children of an object takes worst case O(s) time, where s is the size
	// of the inner HashMap containing those children.
	// For insertion, the object must be inserted into its appropriate inner HashMap,
	// and then the HashMaps for all ancestors must either be created or updated, so this
	// takes worst case O(d) time, where d is the depth of the object in the virtual file tree.
	// For deletion, the argument is similar, that this takes O(d) time.
	// For renames, the worst case is O(n) time, where n is the number of nodes in the
	// virtual file tree. This is because if we change the name of a directory under the root,
	// then we must change all of its children's VirtualChild objects as well. It is advisable
	// to just rebuild the tree for a rename.
	//
	// Building the tree from a list of entries in a zipfile takes O(nh) time, where n
	// is the number of entries in the zipfile, and h is the maximum height of an entry in
	// the virtual file system.
	//--------------------------------------------------------------------------------------------------------------------

	protected File _file; // The underlying file associated with this handler.
	protected long _vfsLastModified; // The timestamp of the file that the virtual file system reflects.
	protected boolean _exists; // Whether or not the zipfile "exists" (in order to exist, must be uncorrupted too)
	/** @since org.eclipse.rse.services 3.0 */
	protected SystemReentrantMutex _mutex;

	/**
	 * Creates a new SystemZipHandler and associates it with <code>file</code>.
	 * @param file The file that this handler will wrapper.
	 * @throws IOException If there is an error handling <code>file</code>
	 */
	public SystemZipHandler(File file) throws IOException
	{
		_file = file;
		_vfsLastModified = _file.lastModified();

		//ignore error opening non-existing zipfile in constructor,
		//because we might want to be creating the zipfile
		boolean zipFileOpen = false;
		if (_file.exists()) {
			try {
				openZipFile();
				zipFileOpen = true;
			} catch (IOException e) {
				// ignore
			}
		}
		if (zipFileOpen)
		{
			buildTree();
			closeZipFile();
			_exists = true;
		}
		else
		{
			_exists = false;
		}
		_mutex = new SystemReentrantMutex();
	}

	/**
	 * Builds the virtual file system tree out of the entries in
	 * the zipfile.
	 */
	protected void buildTree()
	{
		_virtualFS = new HashMap();
		Enumeration entries = _zipfile.entries();
		while (entries.hasMoreElements())
		{
			ZipEntry next = (ZipEntry) entries.nextElement();
			fillBranch(next);
		}
	}

	/**
	 * Update the virtual file system tree after rename operation.
	 * @since org.eclipse.rse.services 3.0
	 */
	protected void updateTreeAfterRename(HashMap newOldName, VirtualChild[] renameList)
	{
		Enumeration entries = _zipfile.entries();
		while (entries.hasMoreElements())
		{
			ZipEntry next = (ZipEntry) entries.nextElement();
			String oldName = null;
			if (newOldName.containsKey(next.getName()))
			{
				oldName = (String)newOldName.get(next.getName());
			}
			fillBranchAfterRename(next, oldName);
		}

		//also make sure all the directory affected by rename, we need to remove those hashmap from the
		//virtual file system.
		if (renameList == null)
		{
			return;
		}
		for (int i = 0; i < renameList.length; i++)
		{
			if (renameList[i].isDirectory)
			{
				String fullName = renameList[i].fullName;
				if (_virtualFS.containsKey(fullName))
				{
					_virtualFS.remove(fullName);
				}
			}
		}

	}

	/**
	 * Populates an entire branch of the tree that comprises the
	 * virtual file system. The parameter is the leaf node, and from
	 * the virtual path of the parameter, we can deduce what the ancestors
	 * of the leaves are, and populate the tree from there.
	 * @param next The ZipEntry from which the branch will be built.
	 */
	protected void fillBranch(ZipEntry next)
	{
		if (next.getName().equals("/")) return; // dummy entry //$NON-NLS-1$
		VirtualChild nextChild = null;

		//We need to search this entry in the virtual file system tree.
		//If oldName is passed in, we use the old name.
		//otherwise, we use the entry name.
		String pathNameToSearchInVFS = null;
		String nameToSearchInVFS = null;

		SystemUniversalZipEntry nextEntry = new SystemUniversalZipEntry(next);


		pathNameToSearchInVFS = nextEntry.getFullPath();
		nameToSearchInVFS = nextEntry.getName();

		//try to find the virtual child from the memory tree
		if (_virtualFS.containsKey(pathNameToSearchInVFS))
		{
			HashMap itsDirectory = (HashMap)_virtualFS.get(pathNameToSearchInVFS);
			if (itsDirectory.containsKey(nameToSearchInVFS))
			{
				nextChild = (VirtualChild)itsDirectory.get(nameToSearchInVFS);
			}
		}

		if (null == nextChild)
		{
			nextChild = new VirtualChild(this, nextEntry.getFullName());
		}
		else
		{
			//We found the virtual child, but its name could also been changed.  So need to update it
			nextChild.renameTo(nextEntry.getFullName());
		}

		if (next.isDirectory())
		{
			nextChild.isDirectory = true;

			if (!_virtualFS.containsKey(nextChild.fullName))
			{
				_virtualFS.put(nextChild.fullName, new HashMap());
			}
		}
		//Now, update other properties
		nextChild.setComment(next.getComment());
		nextChild.setCompressedSize(next.getCompressedSize());
		Integer methodIntValue = new Integer(next.getMethod());
		nextChild.setCompressionMethod(methodIntValue.toString());
		nextChild.setSize(next.getSize());
		nextChild.setTimeStamp(next.getTime());

		//	key has not been encountered before, create a new
		// element in the virtualFS.
		if (!_virtualFS.containsKey(nextChild.path))
		{
			//Do we really need to recursively create? yes for this case
			recursivePopulate(nextChild.path, nextChild);
		}
		else // key has been encountered before, no need to recursively
			// populate the subdirectories
		{
			HashMap hm = (HashMap) _virtualFS.get(nextChild.path);
			hm.put(nextChild.name, nextChild);
		}
	}

	/**
	 * Populates an entire branch of the tree that comprises the
	 * virtual file system. The parameter is the leaf node, and from
	 * the virtual path of the parameter, we can deduce what the ancestors
	 * of the leaves are, and populate the tree from there.
	 * @param next The ZipEntry from which the branch will be built.
	 * @since org.eclipse.rse.services 3.0
	 */
	protected void fillBranchAfterRename(ZipEntry next, String oldName)
	{
		if (next.getName().equals("/")) return; // dummy entry //$NON-NLS-1$
		VirtualChild nextChild = null;

		//We need to search this entry in the virtual file system tree.
		//If oldName is passed in, we use the old name.
		//otherwise, we use the entry name.
		String pathNameToSearchInVFS = null;
		String nameToSearchInVFS = null;
		boolean replace = (oldName != null);

		SystemUniversalZipEntry nextEntry = new SystemUniversalZipEntry(next);

		if (null != oldName)
		{
			int endOfPathPosition = oldName.lastIndexOf("/"); //$NON-NLS-1$
			if (endOfPathPosition != -1)
			{
				if (endOfPathPosition == (oldName.length() - 1))
				{
					//It is a directory, then we need to use its parent fullname as the its path.
					String nameWithoutLastSlash = oldName.substring(0, endOfPathPosition);
					int endOfPathPosNameWithoutLastSlash = nameWithoutLastSlash.lastIndexOf("/"); //$NON-NLS-1$
					if (endOfPathPosNameWithoutLastSlash != -1)
					{
						//example for this case is:
						//fullpath folder1/folder12/
						//Int this case, pathNameToSearchInVFS should be "folder1", and nameToSearchInVFS should be "folder12"
						pathNameToSearchInVFS = oldName.substring(0, endOfPathPosNameWithoutLastSlash);
						nameToSearchInVFS = oldName.substring(endOfPathPosNameWithoutLastSlash + 1, endOfPathPosition);
					}
					else
					{
						//It is the case of something where its full path is "folder1/"
						//In this case, pathNameToSearchInVFS should be "", and nameToSearchInVFS should be "folder"
						pathNameToSearchInVFS = "";  //$NON-NLS-1$
						nameToSearchInVFS = oldName.substring(0, endOfPathPosition);
					}
				}
				else
				{
					pathNameToSearchInVFS = oldName.substring(0,endOfPathPosition);
					nameToSearchInVFS = oldName.substring(endOfPathPosition+1);
				}
			}
			else
			{
				pathNameToSearchInVFS = ""; //$NON-NLS-1$
				nameToSearchInVFS = oldName;
			}
		}
		else
		{
			pathNameToSearchInVFS = nextEntry.getFullPath();
			nameToSearchInVFS = nextEntry.getName();
		}

		//try to find the virtual child from the memory tree
		if (_virtualFS.containsKey(pathNameToSearchInVFS))
		{
			HashMap itsDirectory = (HashMap)_virtualFS.get(pathNameToSearchInVFS);
			if (itsDirectory.containsKey(nameToSearchInVFS))
			{
				nextChild = (VirtualChild)itsDirectory.get(nameToSearchInVFS);
				//We also need to remove this virtual child from VFS tree first, since we need to
				//put it back any way later.
				if (replace)
				{
					itsDirectory.remove(nameToSearchInVFS);
				}
			}
			/*
			if (nameToSearchInVFS.equals(""))
			{
				//It is the directory itself, remove the hashmap.
				_virtualFS.remove(pathNameToSearchInVFS);
			}
			 */
		}

		if (null == nextChild)
		{
			nextChild = new VirtualChild(this, nextEntry.getFullName());
		}
		else
		{
			//We found the virtual child, but its name could also been changed.  So need to update it
			nextChild.renameTo(nextEntry.getFullName());
		}

		if (next.isDirectory())
		{
			nextChild.isDirectory = true;

			if (!_virtualFS.containsKey(nextChild.fullName))
			{
				_virtualFS.put(nextChild.fullName, new HashMap());
			}
		}
		//Now, update other properties
		nextChild.setComment(next.getComment());
		nextChild.setCompressedSize(next.getCompressedSize());
		Integer methodIntValue = new Integer(next.getMethod());
		nextChild.setCompressionMethod(methodIntValue.toString());
		nextChild.setSize(next.getSize());
		nextChild.setTimeStamp(next.getTime());

		//	key has not been encountered before, create a new
		// element in the virtualFS.
		if (!_virtualFS.containsKey(nextChild.path))
		{
			//Do we really need to recursively create? no in this case
			populate(nextChild.path, nextChild);
		}
		else // key has been encountered before, no need to recursively
			// populate the subdirectories
		{
			HashMap hm = (HashMap) _virtualFS.get(nextChild.path);
			hm.put(nextChild.name, nextChild);
		}
	}


	/**
	 * Actually does the work for the fillBranch method. Recursively
	 * inserts key/value pairs into the virtualFS, then uses the key
	 * to get the next parent (moving up one level) and thus recursively
	 * populates one branch.
	 */
	protected void recursivePopulate(String key, VirtualChild value)
	{
		// base case 1: key has been encountered before, finish recursing
		if (_virtualFS.containsKey(key))
		{
			HashMap hm = (HashMap) _virtualFS.get(key);
			hm.put(value.name, value);
			return;
		}

		// else
		HashMap newValue = new HashMap();
		newValue.put(value.name, value);
		_virtualFS.put(key, newValue);


		// base case 2
		if (key.equals("")) //$NON-NLS-1$
		{
			return;
		}
		else
		{
			int i = key.lastIndexOf("/"); //$NON-NLS-1$
			if (i == -1) // recursive last step
			{
				VirtualChild nextValue = new VirtualChild(this, key);
				nextValue.isDirectory = true;
				recursivePopulate("", nextValue); //$NON-NLS-1$
				return;
			}
			else // recursive step
			{
				String newKey = key.substring(0, i);
				VirtualChild nextValue = new VirtualChild(this, key);
				nextValue.isDirectory = true;
				recursivePopulate(newKey, nextValue);
				return;
			}

		}


	}

	/**
	 * @since org.eclipse.rse.services 3.0
	 */
	protected void populate(String key, VirtualChild value)
	{
		// base case 1: key has been encountered before, finish recursing
		if (_virtualFS.containsKey(key))
		{
			HashMap hm = (HashMap) _virtualFS.get(key);
			hm.put(value.name, value);
			return;
		}

		// else
		HashMap newValue = new HashMap();
		newValue.put(value.name, value);
		_virtualFS.put(key, newValue);


		// base case 2
		if (key.equals("")) //$NON-NLS-1$
		{
			return;
		}
		else
		{
			int i = key.lastIndexOf("/"); //$NON-NLS-1$
			if (i == -1) // recursive last step
			{
				VirtualChild nextValue = new VirtualChild(this, key);
				nextValue.isDirectory = true;
				recursivePopulate("", nextValue); //$NON-NLS-1$
				return;
			}
			else // recursive step
			{
				String newKey = key.substring(0, i);
				VirtualChild nextValue = new VirtualChild(this, key);
				nextValue.isDirectory = true;
				recursivePopulate(newKey, nextValue);
				return;
			}

		}


	}

	/**
	 * {@inheritDoc}
	 * @since 3.0
	 */
	public VirtualChild[] getVirtualChildrenList(ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		return getVirtualChildrenList(true, archiveOperationMonitor);
	}

	/**
	 * Same as getVirtualChildrenList(), but you can choose whether to leave the
	 * zip file open or closed upon return.
	 *
	 * @throws SystemMessageException in case of an error
	 * @since org.eclipse.rse.services 3.0
	 */
	public VirtualChild[] getVirtualChildrenList(boolean closeZipFile, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists) return new VirtualChild[0];
		if (!updateVirtualFSIfNecessary(archiveOperationMonitor)) return new VirtualChild[0];
		int mutexLockStatus = SystemReentrantMutex.LOCK_STATUS_NOLOCK;
		try
		{
			mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
			if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
			{
				if (openZipFile())
				{
					Vector children = new Vector();
					Enumeration entries = _zipfile.entries();
					while (entries.hasMoreElements())
					{
						ZipEntry next = (ZipEntry) entries.nextElement();
						SystemUniversalZipEntry nextEntry = new SystemUniversalZipEntry(next);
						VirtualChild nextChild = new VirtualChild(this, nextEntry.getFullName());
						nextChild.isDirectory = next.isDirectory();
						children.add(nextChild);
					}
					VirtualChild[] retVal = new VirtualChild[children.size()];
					for (int i = 0; i < children.size(); i++)
					{
						retVal[i] = (VirtualChild) children.get(i);
					}
					if (closeZipFile) closeZipFile();
					return retVal;
				}
			} else {
				throw new SystemLockTimeoutException(IClientServerConstants.PLUGIN_ID);
			}
		}
		catch (Exception e)
		{
			if (closeZipFile) closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
		}
		finally
		{
			releaseMutex(mutexLockStatus);
		}

		return new VirtualChild[0];
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public VirtualChild[] getVirtualChildrenList(String parent, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		return getVirtualChildrenList(parent, true, archiveOperationMonitor);
	}

	/**
	 * Same as getVirtualChildrenList(String parent) but you can choose whether
	 * or not you want to leave the zipfile open after return.
	 *
	 * @throws SystemMessageException in case of an error
	 * @since org.eclipse.rse.services 3.0
	 */
	public VirtualChild[] getVirtualChildrenList(String parent, boolean closeZipFile, ISystemOperationMonitor archiveOperationMonitor)
			throws SystemMessageException
	{
		if (!_exists) return new VirtualChild[0];
		if (!updateVirtualFSIfNecessary(archiveOperationMonitor)) return new VirtualChild[0];
		int mutexLockStatus = SystemReentrantMutex.LOCK_STATUS_NOLOCK;
		try
		{
			mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
			if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
			{
				if (openZipFile())
				{
					parent = ArchiveHandlerManager.cleanUpVirtualPath(parent);
					Vector children = new Vector();
					Enumeration entries = _zipfile.entries();
					while (entries.hasMoreElements())
					{
						ZipEntry next = (ZipEntry) entries.nextElement();
						String nextName = ArchiveHandlerManager.cleanUpVirtualPath(next.getName());
						if (nextName.startsWith(parent) && !nextName.equals(parent+"/")) //$NON-NLS-1$
						{
							SystemUniversalZipEntry nextEntry = new SystemUniversalZipEntry(next);
							VirtualChild nextChild = new VirtualChild(this, nextEntry.getFullName());
							nextChild.isDirectory = next.isDirectory();
							children.add(nextChild);
						}
					}
					VirtualChild[] retVal = new VirtualChild[children.size()];
					for (int i = 0; i < children.size(); i++)
					{
						retVal[i] = (VirtualChild) children.get(i);
					}
					if (closeZipFile) closeZipFile();
					return retVal;
				}
				else return new VirtualChild[0];
			}
			else
			{
				return new VirtualChild[0];
			}
		}
		catch (Exception e)
		{
			if (closeZipFile) closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
		}
		finally
		{
			releaseMutex(mutexLockStatus);
		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public VirtualChild[] getVirtualChildren(String fullVirtualName, ISystemOperationMonitor archiveOperationMonitor)
	{
		if (!_exists) return null;
		if (!updateVirtualFSIfNecessary(archiveOperationMonitor)) return null;

		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
		VirtualChild[] values = null;
		if (_virtualFS.containsKey(fullVirtualName))
		{
			HashMap hm = (HashMap) _virtualFS.get(fullVirtualName);
			Object valueArray[] = hm.values().toArray();
			values = new VirtualChild[hm.size()];
			for (int i = 0; i < hm.size(); i++)
			{
				values[i] = (VirtualChild) valueArray[i];
			}

		}
		return values;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public VirtualChild[] getVirtualChildFolders(String fullVirtualName, ISystemOperationMonitor archiveOperationMonitor)
	{
		if (!_exists) return null;
		if (!updateVirtualFSIfNecessary(archiveOperationMonitor)) return null;
		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
		Vector folders = new Vector();
		VirtualChild[] values = null;
		if (_virtualFS.containsKey(fullVirtualName))
		{
			HashMap hm = (HashMap) _virtualFS.get(fullVirtualName);
			Object valueArray[] = hm.values().toArray();
			for (int i = 0; i < hm.size(); i++)
			{
				if (((VirtualChild) valueArray[i]).isDirectory) folders.add(valueArray[i]);
			}
			values = new VirtualChild[folders.size()];
			for (int i = 0; i < folders.size(); i++)
			{
				values[i] = (VirtualChild) folders.get(i);
			}
		}
		return values;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public VirtualChild getVirtualFile(String fullVirtualName, ISystemOperationMonitor archiveOperationMonitor)
	{
		if (!_exists) return new VirtualChild(this, fullVirtualName);
		if (!updateVirtualFSIfNecessary(archiveOperationMonitor)) return new VirtualChild(this, fullVirtualName);

		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
		if (fullVirtualName == "" || fullVirtualName == null) return new VirtualChild(this); //$NON-NLS-1$
		int i = fullVirtualName.lastIndexOf("/"); //$NON-NLS-1$
		String path;
		String name;
		if (i == -1)
		{
			path = ""; //$NON-NLS-1$
			name = fullVirtualName;
		}
		else
		{
			path = fullVirtualName.substring(0, i);
			name = fullVirtualName.substring(i+1);
		}
		HashMap hm = (HashMap) _virtualFS.get(path);
		if (hm == null) return new VirtualChild(this, fullVirtualName);
		VirtualChild vc = (VirtualChild) hm.get(name);
		if (vc == null) return new VirtualChild(this, fullVirtualName);
		return vc;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public boolean exists(String fullVirtualName, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists) return false;

		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
		if (fullVirtualName == "" || fullVirtualName == null) return false; //$NON-NLS-1$

		if (_vfsLastModified == _file.lastModified())
		{
			int i = fullVirtualName.lastIndexOf("/"); //$NON-NLS-1$
			String path;
			String name;
			if (i == -1)
			{
				path = ""; //$NON-NLS-1$
				name = fullVirtualName;
			}
			else
			{
				path = fullVirtualName.substring(0, i);
				name = fullVirtualName.substring(i+1);
			}
			HashMap hm = (HashMap) _virtualFS.get(path);
			if (hm == null) return false;
			if (hm.get(name) == null) return false;
			return true;
		}
		else
		{
			boolean retval = false;
			boolean keepOpen = _zipfile != null;
			try {
				openZipFile();
			} catch (IOException ioe) {
				throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not open the ZipFile " + _file.toString(), ioe); //$NON-NLS-1$
			}
			try {
				safeGetEntry(fullVirtualName);
				retval = true;
			} catch (IOException e)
			{
				try
				{
					safeGetEntry(fullVirtualName + "/"); //$NON-NLS-1$
					retval = true;
				}
				catch (IOException f)
				{
					retval = false;
				}
			}
			buildTree();
			_vfsLastModified = _file.lastModified();
			if (!keepOpen)
				closeZipFile();
			return retval;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.clientserver.archiveutils.ISystemArchiveHandler#getArchive()
	 */
	public File getArchive()
	{
		return _file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.clientserver.archiveutils.ISystemArchiveHandler#getTimeStampFor(java.lang.String)
	 */
	public long getTimeStampFor(String fullVirtualName) throws SystemMessageException
	{
		return getTimeStampFor(fullVirtualName, true);
	}

	/**
	 * Same as getTimeStampFor(String fullVirtualName) but you can choose whether
	 * or not you want to leave the zipfile open after return.
	 */
	public long getTimeStampFor(String fullVirtualName, boolean closeZipFile) throws SystemMessageException
	{
		if (!_exists) return 0;
		try
		{
			openZipFile();
			fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
			ZipEntry entry = null;
			entry = safeGetEntry(fullVirtualName);
			if (closeZipFile) closeZipFile();
			return entry.getTime();
		}
		catch (IOException e) {
			if (closeZipFile)
				closeZipFile();
			return _file.lastModified();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.clientserver.archiveutils.ISystemArchiveHandler#getSizeFor(java.lang.String)
	 */
	public long getSizeFor(String fullVirtualName) throws SystemMessageException
	{
		return getSizeFor(fullVirtualName, true);
	}

	/**
	 * Same as {@link #getSizeFor(String)} but allows to specify whether to
	 * close the zip file or not.
	 *
	 * @param fullVirtualName absolute virtual path to the node to inspect
	 * @param closeZipFile <code>true</code> if the zip file should be closed
	 * @return the uncompressed size of the node requested.
	 * @see org.eclipse.rse.services.clientserver.archiveutils.ISystemArchiveHandler#getSizeFor(java.lang.String)
	 */
	public long getSizeFor(String fullVirtualName, boolean closeZipFile) throws SystemMessageException
	{
		if (!_exists) return 0;
		try
		{
			openZipFile();
			fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
			ZipEntry entry = null;
			entry = safeGetEntry(fullVirtualName);
			if (closeZipFile) closeZipFile();
			return entry.getSize();
		}
		catch (IOException e) {
			if (closeZipFile)
				closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public void extractVirtualFile(String fullVirtualName, File destination, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		extractVirtualFile(fullVirtualName, destination, true, SystemEncodingUtil.ENCODING_UTF_8, false, archiveOperationMonitor);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public void extractVirtualFile(String fullVirtualName, File destination, String sourceEncoding, boolean isText,
			ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		extractVirtualFile(fullVirtualName, destination, true, sourceEncoding, isText, archiveOperationMonitor);
	}

	/**
	 * Same as extractVirtualFile(String fullVirtualName, File destination) but you can choose whether
	 * or not you want to leave the zipfile open after return.
	 * @since org.eclipse.rse.services 3.0
	 */
	public void extractVirtualFile(String fullVirtualName, File destination, boolean closeZipFile, String sourceEncoding, boolean isText,
			ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists)
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);
		int mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
		if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
		{
			ZipEntry entry = null;
			try
			{
				if (openZipFile())
				{
					fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);

					entry = safeGetEntry(fullVirtualName);
					if (entry.isDirectory())
					{
						destination.delete();
						destination.mkdirs();
						destination.setLastModified(entry.getTime());
						if (closeZipFile) closeZipFile();
						return;
					}
					InputStream is = _zipfile.getInputStream(entry);
					if (is == null)
					{
						destination.setLastModified(entry.getTime());
						if (closeZipFile) closeZipFile();
						return;
					}
					BufferedInputStream reader = new BufferedInputStream(is);

					if (!destination.exists())
					{
						File parentFile = destination.getParentFile();
						if (!parentFile.exists())
							parentFile.mkdirs();
						destination.createNewFile();
					}
					BufferedOutputStream writer = new BufferedOutputStream(
							new FileOutputStream(destination));

					byte[] buf = new byte[1024];
					int numRead = reader.read(buf);

					while (numRead > 0)
					{
						if (isText)
						{
							String bufString = new String(buf, 0, numRead, sourceEncoding);
							byte[] convertedBuf = bufString.getBytes();
							int newSize = convertedBuf.length;
							writer.write(convertedBuf, 0, newSize);
						}
						else
						{
							writer.write(buf, 0, numRead);
						}
						numRead = reader.read(buf);
					}
					writer.close();
					reader.close();
				}
				destination.setLastModified(entry.getTime());
				if (closeZipFile) closeZipFile();
				return;
			}
			catch (IOException e)
			{
				if (_virtualFS.containsKey(fullVirtualName))
				{
					destination.delete();
					destination.mkdirs();
					destination.setLastModified(_file.lastModified());
					if (closeZipFile) closeZipFile();
					return;
				}
				if (closeZipFile) closeZipFile();
				throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
			}
			finally
			{
				releaseMutex(mutexLockStatus);
			}

		}
		else
		{
			throw new SystemLockTimeoutException(IClientServerConstants.PLUGIN_ID);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public void extractVirtualDirectory(String dir, File destinationParent, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		extractVirtualDirectory(dir, destinationParent, (File) null, SystemEncodingUtil.ENCODING_UTF_8, false, archiveOperationMonitor);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public void extractVirtualDirectory(String dir, File destinationParent, String sourceEncoding, boolean isText,
			ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		extractVirtualDirectory(dir, destinationParent, (File) null, sourceEncoding, isText, archiveOperationMonitor);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public void extractVirtualDirectory(String dir, File destinationParent, File destination, ISystemOperationMonitor archiveOperationMonitor)
			throws SystemMessageException
	{
		extractVirtualDirectory(dir, destinationParent, destination, SystemEncodingUtil.ENCODING_UTF_8, false, archiveOperationMonitor);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.0
	 */
	public void extractVirtualDirectory(String dir, File destinationParent, File destination, String sourceEncoding, boolean isText,
			ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists)
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);

		if (!destinationParent.isDirectory())
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);
		dir = ArchiveHandlerManager.cleanUpVirtualPath(dir);
		if (!_virtualFS.containsKey(dir))
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);

		String name;
		int charsToTrim;
		int j = dir.lastIndexOf("/"); //$NON-NLS-1$
		if (j == -1)
		{
			charsToTrim = 0;
			name = dir;
		}
		else
		{
			charsToTrim = dir.substring(0,j).length() + 1;
			name = dir.substring(j+1);
		}

		if (destination == null)
		{
			if (dir.equals("")) //$NON-NLS-1$
			{
				destination = destinationParent;
			}
			else
			{
				destination = new File(destinationParent, name);
			}
		}

		if (!(destination == destinationParent))
		{
			if (destination.isFile() && destination.exists())
			{
				if (!SystemArchiveUtil.delete(destination))
				{
					throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not overwrite directory " + destination); //$NON-NLS-1$
				}
			}

			if (!destination.exists())
			{
				if (!destination.mkdirs())
				{
					throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not overwrite directory " + destination); //$NON-NLS-1$
				}
			}
		}

		File topFile = destination;
		String topFilePath = topFile.getAbsolutePath().replace('\\', '/');
		//if (!dir.equals(topFile.getName()))
		String lastPortionOfDir = null;
		int lastSlashIndex = dir.lastIndexOf('/');
		if (-1 == lastSlashIndex)
		{
			lastPortionOfDir = dir;
		}
		else
		{
			lastPortionOfDir = dir.substring(lastSlashIndex + 1);
		}
		if (!topFilePath.endsWith(lastPortionOfDir))
		{
			rename(dir, topFile.getName(), archiveOperationMonitor);
			dir = topFile.getName();
		}
		VirtualChild[] newChildren = getVirtualChildrenList(dir, archiveOperationMonitor);

		if (newChildren.length == 0)
		{
			//it is a error situation, or the operation has been cancelled.
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);
		}
		extractVirtualFile(dir + '/', topFile, sourceEncoding, isText, archiveOperationMonitor);

		for (int i = 0; i < newChildren.length; i++)
		{
			String newName = newChildren[i].fullName.substring(charsToTrim);
			char separator = File.separatorChar;
			newName = newName.replace('/', separator);

			File nextFile = new File(destinationParent, newName);
			/*
			// DKM: case where a rename has taken place
			// don't want to extract root folder as it appears in zip
			if (!nextFile.getParent().equals(destination.getPath()) &&
			        nextFile.getParentFile().getParent().equals(destination.getParent()))
			{
			    nextFile = new File(destination, nextFile.getName());
			}
			 */
			if (!nextFile.exists())
			{
				if (newChildren[i].isDirectory)
				{
					if (!nextFile.mkdirs())
					{
						throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not create folder " + nextFile.toString()); //$NON-NLS-1$
					}
				}
				else
				{
					createFile(nextFile);
				}
				if (newChildren[i].isDirectory)
				{
					extractVirtualFile(newChildren[i].fullName + '/', nextFile, sourceEncoding, isText, archiveOperationMonitor);
				}
				else
				{
					extractVirtualFile(newChildren[i].fullName, nextFile, sourceEncoding, isText, archiveOperationMonitor);
				}
			}
		}
	}

	/**
	 * Create an empty file, also creating parent folders if necessary.
	 *
	 * @param file An abstract file handle to create physically.
	 * @throws SystemMessageException in case of an error otherwise.
	 * @since 3.0 throws SystemMessageException
	 */
	protected void createFile(File file) throws SystemMessageException
	{
		try
		{
			if (!file.createNewFile())
			{
				throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "File already exists: " + file.toString()); //$NON-NLS-1$
			}
			else
			{
				return;
			}
		}
		catch (IOException e)
		{
			if (!file.getParentFile().exists() && file.getParentFile().mkdirs())
			{
				createFile(file);
			}
			else
			{
				throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not create " + file.toString()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void add(File file, String virtualPath, String name, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		add(file, virtualPath, name, SystemEncodingUtil.ENCODING_UTF_8, SystemEncodingUtil.ENCODING_UTF_8, false, archiveOperationMonitor);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void add(InputStream stream, String virtualPath, String name, String sourceEncoding, String targetEncoding, boolean isText,
			ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists)
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);
		virtualPath = ArchiveHandlerManager.cleanUpVirtualPath(virtualPath);

		if (exists(virtualPath + "/" + name, archiveOperationMonitor)) //$NON-NLS-1$
		{
			// wrong method
			replace(virtualPath + "/" + name, stream, name, sourceEncoding, targetEncoding, isText, archiveOperationMonitor); //$NON-NLS-1$
		}

		int mutexLockStatus = SystemReentrantMutex.LOCK_STATUS_NOLOCK;
		try
		{
			mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
			if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
			{
				if (openZipFile())
				{
					virtualPath = ArchiveHandlerManager.cleanUpVirtualPath(virtualPath);
					File outputTempFile;
					try
					{
						// Open a new tempfile which will be our destination for the new zip
						outputTempFile = new File(_file.getAbsolutePath() + "temp"); //$NON-NLS-1$
						ZipOutputStream  dest = new ZipOutputStream(
								new FileOutputStream(outputTempFile));

						dest.setMethod(ZipOutputStream.DEFLATED);
						// get all the entries in the old zip
						VirtualChild[] vcList = getVirtualChildrenList(false, archiveOperationMonitor);

						// if it is an empty zip file, no need to recreate it
						if (!(vcList.length == 1) || !vcList[0].fullName.equals("")) //$NON-NLS-1$
						{
							boolean isCancelled = recreateZipDeleteEntries(vcList, dest, null, archiveOperationMonitor);
							if (isCancelled)
							{
								dest.close();
								if (!(outputTempFile == null)) outputTempFile.delete();
								closeZipFile();
								throw new SystemOperationCancelledException();
							}
						}

						// append the additional entry to the zip file.
						ZipEntry newEntry = appendBytes(stream, dest, virtualPath, name, sourceEncoding, targetEncoding, isText);
						// Add the new entry to the virtual file system in memory
						fillBranch(newEntry);

						dest.close();

						// Now replace the old zip file with the new one
						replaceOldZip(outputTempFile);

					}
					catch (IOException e)
					{
						closeZipFile();
						throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not add a file.", e); //$NON-NLS-1$
					}
					closeZipFile();
				}
			} else {
				throw new SystemLockTimeoutException(IClientServerConstants.PLUGIN_ID);
			}

		}
		catch(Exception e)
		{
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
		}
		finally
		{
			releaseMutex(mutexLockStatus);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void add(File[] files, String virtualPath, String[] names, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		String[] encodings = new String[files.length];
		boolean[] isTexts = new boolean[files.length];
		for (int i = 0; i < files.length; i++)
		{
			encodings[i] = SystemEncodingUtil.ENCODING_UTF_8;
			isTexts[i] = false;
		}
		add(files, virtualPath, names, encodings, encodings, isTexts, true, archiveOperationMonitor);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void add(File[] files, String virtualPath, String[] names, String[] sourceEncodings, String[] targetEncodings, boolean[] isText,
			ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		add(files, virtualPath, names, sourceEncodings, targetEncodings, isText, true, archiveOperationMonitor);
	}


	/**
	 * Same as add(File[] files, String virtualPath, String[] names, String[] encodings) but you can choose whether
	 * or not you want to leave the zipfile open after return.
	 * @since org.eclipse.rse.services 3.0
	 */
	public void add(File[] files, String virtualPath, String[] names, String[] sourceEncodings, String[] targetEncodings, boolean[] isText,
			boolean closeZipFile, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists)
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);

		int mutexLockStatus = SystemReentrantMutex.LOCK_STATUS_NOLOCK;
		try
		{
			mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
			if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
			{
				if (openZipFile())
				{
					virtualPath = ArchiveHandlerManager.cleanUpVirtualPath(virtualPath);
					int numFiles = files.length;
					for (int i = 0; i < numFiles; i++)
					{
						if (archiveOperationMonitor != null && archiveOperationMonitor.isCancelled())
						{
							//the operation has been cancelled
							closeZipFile();
							throw new SystemOperationCancelledException();
						}
						if (!files[i].exists() || !files[i].canRead()) {
							throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Cannot read: " + files[i]); //$NON-NLS-1$
						}
						String fullVirtualName = getFullVirtualName(virtualPath, names[i]);
						if (exists(fullVirtualName, archiveOperationMonitor))
						{
							// sorry, wrong method buddy
							replace(fullVirtualName, files[i], names[i], archiveOperationMonitor);
						}
					}
					File outputTempFile;

					// Open a new tempfile which will be our destination for the new zip
					outputTempFile = new File(_file.getAbsolutePath() + "temp"); //$NON-NLS-1$
					ZipOutputStream  dest = new ZipOutputStream(
							new FileOutputStream(outputTempFile));

					dest.setMethod(ZipOutputStream.DEFLATED);
					// get all the entries in the old zip
					VirtualChild[] vcList = getVirtualChildrenList(false, archiveOperationMonitor);

					// if it is an empty zip file, no need to recreate it
					if (!(vcList.length == 1) || !vcList[0].fullName.equals("")) //$NON-NLS-1$
					{
						boolean isCancelled = recreateZipDeleteEntries(vcList, dest, null, archiveOperationMonitor);
						if (isCancelled)
						{
							dest.close();
							if (!(outputTempFile == null))
							{
								outputTempFile.delete();
							}
							if (closeZipFile) closeZipFile();
							throw new SystemOperationCancelledException();
						}
					}

					// Now for each new file to add
					// We need to remember the entries added, and if this operation is not cancelled, we
					// will add them into Virtual File system.
					ZipEntry[] newEntriesAdded = new ZipEntry[numFiles];
					for (int i = 0; i < numFiles; i++)
					{
						if (archiveOperationMonitor != null && archiveOperationMonitor.isCancelled())
						{
							//the operation has been cancelled
							dest.close();
							if (!(outputTempFile == null))
							{
								outputTempFile.delete();
							}
							closeZipFile();
							throw new SystemOperationCancelledException();
						}
						// append the additional entry to the zip file.
						ZipEntry newEntry = appendFile(files[i], dest, virtualPath, names[i], sourceEncodings[i], targetEncodings[i], isText[i]);
						// Add the new entry to the array first.
						newEntriesAdded[i] = newEntry;
					}

					dest.close();

					// Now replace the old zip file with the new one
					replaceOldZip(outputTempFile);
					//Also need to add the new entries into VFS
					for (int i = 0; i < numFiles; i++)
					{
						fillBranch(newEntriesAdded[i]);
					}

					if (closeZipFile) closeZipFile();
				}
			} else {
				throw new SystemLockTimeoutException(IClientServerConstants.PLUGIN_ID);
			}
		}
		catch (IOException ioe) {
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, ioe);
		}
		finally
		{
			if (closeZipFile) closeZipFile();
			releaseMutex(mutexLockStatus);
		}
	}

	/**
	 * Helper method. populates <code>found</code> with a collapsed list of
	 * all nodes in the subtree of the file system rooted at <code>parent</code>.
	 *
	 * @since org.eclipse.rse.services 3.0
	 */
	public static boolean listAllFiles(File parent, HashSet found, ISystemOperationMonitor archiveOperationMonitor)
	{
		File[] children = parent.listFiles();
		if (children == null) // DKM - 56031, no authority on parent yields null
		{
			found.remove(parent);
			return false;
		}
		for (int i = 0; i < children.length; i++)
		{
			if (archiveOperationMonitor != null && archiveOperationMonitor.isCancelled())
			{
				//the operation has been cancelled
				return true;
			}
			if (!found.contains(children[i])) // prevent infinite loops due to symlinks
			{
				if (children[i].canRead())
				{
					found.add(children[i]);
					if (children[i].isDirectory())
					{
						listAllFiles(children[i], found, archiveOperationMonitor);
					}
				}
			}
		}

		return false;
	}

	/**
	 * Recreates a zip file from a list of virtual children, optionally omitting
	 * a group of children whose names are in the Set omitChildren
	 *
	 * @param vcList The list of virtual children to create the zip from
	 * @param dest The ZipOutputStream representing the zip file where the
	 * 		children are to be recreated
	 * @param omitChildren The set of names of children to omit when creating
	 * 		the zipfile. Null or empty set if there are no omissions.
	 * @throws IOException in case of a file I/O error
	 * @return <code>true</code> if the operation is cancelled
	 * @since org.eclipse.rse.services 3.0
	 */
	protected boolean recreateZipDeleteEntries(VirtualChild[] vcList, ZipOutputStream dest, HashSet omitChildren, ISystemOperationMonitor archiveOperationMonitor) throws IOException
	{
		if (!(omitChildren == null) && vcList.length == omitChildren.size())
		{
			// the zip file will be empty, but it must have at least one entry,
			// so we will put in a dummy entry.
			ZipEntry entry = new ZipEntry("/"); //$NON-NLS-1$
			dest.putNextEntry(entry);
			dest.closeEntry();
			return false;
		}
		//else
		for (int i = 0; i < vcList.length; i++)
		{
			if (archiveOperationMonitor != null && archiveOperationMonitor.isCancelled())
			{
				//the operation has been cancelled
				return true;
			}

			// for each entry, append it to the new temp zip
			// unless it is in the set of omissions
			if (omitChildren != null && omitChildren.contains(vcList[i].fullName)) continue;
			if (vcList[i].isDirectory)
			{
				ZipEntry nextEntry = safeGetEntry(vcList[i].fullName + "/"); //$NON-NLS-1$
				dest.putNextEntry(nextEntry);
				dest.closeEntry();
				continue;
			}
			ZipEntry nextEntry = safeGetEntry(vcList[i].fullName);
			BufferedInputStream source = new BufferedInputStream(
					_zipfile.getInputStream(nextEntry));
			nextEntry.setCompressedSize(-1);
			dest.putNextEntry(nextEntry);
			byte[] buf = new byte[1024];
			int numRead = source.read(buf);

			while (numRead > 0)
			{
				dest.write(buf, 0, numRead);
				numRead = source.read(buf);
			}
			dest.closeEntry();
			source.close();
		}
		return false;
	}

	/**
	 * Recreates a zip file from a list of virtual children, but renaming the
	 * one of the VirtualChildren.
	 *
	 * @param vcList The list of virtual children to create the zip from
	 * @param dest The ZipOutputStream representing the zip file where the
	 * 		children are to be recreated
	 * @param names HashMap maps the full path of a virtual file to the entry in
	 * 		the archive file
	 * @return <code>true</code> if the operation has been cancelled
	 * @throws IOException in case of an error.
	 * @since org.eclipse.rse.services 3.0
	 */
	protected boolean recreateZipRenameEntries(VirtualChild[] vcList, ZipOutputStream dest, HashMap names, ISystemOperationMonitor archiveOperationMonitor) throws IOException
	{
		for (int i = 0; i < vcList.length; i++)
		{
			if (archiveOperationMonitor != null && archiveOperationMonitor.isCancelled())
			{
				//the operation has been cancelled
				return true;
			}
			// for each entry, append it to the new temp zip
			ZipEntry nextEntry;
			ZipEntry newEntry;
			if (names.containsKey(vcList[i].getArchiveStandardName()))
			{
				// rename the entry
				String oldName = vcList[i].getArchiveStandardName();
				String newName = (String) names.get(oldName);
				vcList[i].renameTo(newName);
				nextEntry = safeGetEntry(oldName);
				newEntry = createSafeZipEntry(newName);
				newEntry.setComment(nextEntry.getComment());
				newEntry.setExtra(nextEntry.getExtra());
				newEntry.setTime(nextEntry.getTime());
			}
			else
			{
				nextEntry = safeGetEntry(vcList[i].getArchiveStandardName());
				newEntry = nextEntry;
			}
			if (nextEntry.isDirectory())
			{
				dest.putNextEntry(newEntry);
				dest.closeEntry();
				continue;
			}
			BufferedInputStream source = new BufferedInputStream(
					_zipfile.getInputStream(nextEntry));
			newEntry.setCompressedSize(-1);
			dest.putNextEntry(newEntry);
			byte[] buf = new byte[1024];
			int numRead = source.read(buf);

			while (numRead > 0)
			{
				dest.write(buf, 0, numRead);
				numRead = source.read(buf);
			}
			dest.closeEntry();
			source.close();
		}
		return false;
	}

	/**
	 * Compresses the contents of <code>file</code>, adding them to the ZipFile
	 * managed by <code>dest</code>. The file is encoded in the encoding
	 * specified by <code>encoding</code>. A new entry is created in the ZipFile
	 * with virtual path and name of <code>virtualPath</code> and
	 * <code>name</code> respectively.
	 *
	 * @return The ZipEntry that was added to the destination zip file.
	 * @throws IOException in case of an error.
	 */
	protected ZipEntry appendFile(File file, ZipOutputStream dest, String virtualPath, String name, String sourceEncoding, String targetEncoding, boolean isText) throws IOException
	{
		ZipEntry newEntry;
		if (file.isDirectory())
		{
			String fullName = virtualPath + "/" + name; //$NON-NLS-1$
			if (!fullName.endsWith("/")) fullName = fullName + "/"; //$NON-NLS-1$ //$NON-NLS-2$
			newEntry = createSafeZipEntry(fullName);
		}
		else
		{
			newEntry = createSafeZipEntry(virtualPath + "/" + name); //$NON-NLS-1$
		}
		newEntry.setTime(file.lastModified());
		dest.putNextEntry(newEntry);
		if (!file.isDirectory())
		{
			BufferedInputStream source = new BufferedInputStream(
					new FileInputStream(file));

			byte[] buf = new byte[1024];
			int numRead = source.read(buf);
			long fileSize = file.length();
			long totalRead = 0;
			while (numRead > 0 && totalRead < fileSize)
			{
				totalRead += numRead;
				if (isText)
				{
					// DKM - if you don't specify numRead here, then buf will get picked up wiht extra bytes from b4!!!!
					String bufString = new String(buf, 0, numRead, sourceEncoding);
					byte[] convertedBuf = bufString.getBytes(targetEncoding);
					int newSize = convertedBuf.length;
					dest.write(convertedBuf, 0, newSize);
				}
				else
				{
					dest.write(buf, 0, numRead);
				}
				// specify max size here
				long maxRead = 1024;
				long deltaLeft = fileSize - totalRead;
				if (deltaLeft > 1024)
				{
					numRead = source.read(buf, 0, (int)maxRead);
				}
				else
				{
					numRead = source.read(buf, 0, (int)deltaLeft);
				}

			}
			dest.closeEntry();
			source.close();
		}
		return newEntry;
	}

	/**
	 * Compresses the contents of <code>stream</code>, adding them to the
	 * ZipFile managed by <code>dest</code>. The stream is encoded in the
	 * encoding specified by <code>encoding</code>. A new entry is created in
	 * the ZipFile with virtual path and name of <code>virtualPath</code> and
	 * <code>name</code> respectively.
	 *
	 * @return The ZipEntry that was added to the destination zip file.
	 * @throws IOException in case of an error.
	 */
	protected ZipEntry appendBytes(InputStream stream, ZipOutputStream dest, String virtualPath, String name, String sourceEncoding, String targetEncoding, boolean isText) throws IOException
	{
		ZipEntry newEntry;
		newEntry = createSafeZipEntry(virtualPath + "/" + name); //$NON-NLS-1$
		dest.putNextEntry(newEntry);
		BufferedInputStream source = new BufferedInputStream(stream);

		byte[] buf = new byte[1024];
		int numRead = source.read(buf);
		long totalRead = 0;
		while (numRead > 0 && source.available() > 0)
		{
			totalRead += numRead;
			if (isText)
			{
				// DKM - if you don't specify numRead here, then buf will get picked up wiht extra bytes from b4!!!!
				String bufString = new String(buf, 0, numRead, sourceEncoding);
				byte[] convertedBuf = bufString.getBytes(targetEncoding);
				int newSize = convertedBuf.length;
				dest.write(convertedBuf, 0, newSize);
			}
			else
			{
				dest.write(buf, 0, numRead);
			}
			// specify max size here
			long maxRead = 1024;
			long deltaLeft = source.available();
			if (deltaLeft > 1024)
			{
				numRead = source.read(buf, 0, (int)maxRead);
			}
			else
			{
				numRead = source.read(buf, 0, (int)deltaLeft);
			}

		}
		dest.closeEntry();
		source.close();
		return newEntry;
	}

	/**
	 * Replaces the old zip file managed by this SystemZipHandler, with
	 * the zip file referred to by outputTempFile.
	 * @throws IOException if outputTempFile cannot be used as a ZipFile.
	 */
	protected void replaceOldZip(File outputTempFile) throws IOException
	{
		String oldName = _file.getAbsolutePath();
		_zipfile.close();
		File oldFile = new File(oldName + "old"); //$NON-NLS-1$
		if (!_file.renameTo(oldFile)) {
			throw new IOException("Failed to rename " + oldFile); //$NON-NLS-1$
		}
		if (!outputTempFile.renameTo(_file)) {
			throw new IOException("Failed to rename " + _file); //$NON-NLS-1$
		}
		_vfsLastModified = _file.lastModified();
		_zipfile = new ZipFile(_file);
		oldFile.delete();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public boolean delete(String fullVirtualName, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		boolean returnCode = delete(fullVirtualName, true, archiveOperationMonitor);
		setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
		return returnCode;
	}

	/**
	 * Same as delete(String fullVirtualName) but you can choose whether
	 * or not you want to leave the zipfile open after return.
	 * @since org.eclipse.rse.services 3.0
	 */
	public boolean delete(String fullVirtualName, boolean closeZipFile, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists) return false;
		File outputTempFile = null;
		int mutexLockStatus = SystemReentrantMutex.LOCK_STATUS_NOLOCK;
		try
		{
			mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
			if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
			{
				if (openZipFile())
				{
					fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
					VirtualChild vc = getVirtualFile(fullVirtualName, archiveOperationMonitor);
					VirtualChild[] vcList;
					VirtualChild[] vcOmmit = new VirtualChild[1];
					if (!vc.exists())
					{
						if (closeZipFile) closeZipFile();
						return false;
					} // file doesn't exist

					if (vc.isDirectory) // file is a directory, we must delete the contents
					{
						vcOmmit = getVirtualChildrenList(fullVirtualName, false, archiveOperationMonitor);
					}

					// Open a new tempfile which will be our destination for the new zip
					outputTempFile = new File(_file.getAbsolutePath() + "temp"); //$NON-NLS-1$
					ZipOutputStream  dest = new ZipOutputStream(
							new FileOutputStream(outputTempFile));
					dest.setMethod(ZipOutputStream.DEFLATED);

					// get all the entries in the old zip
					vcList = getVirtualChildrenList(false, archiveOperationMonitor);

					HashSet omissions = new HashSet();

					if (vc.isDirectory)
					{
						for (int i = 0; i < vcOmmit.length; i++)
						{
							omissions.add(vcOmmit[i].fullName);
						}
						try
						{
							safeGetEntry(vc.fullName);
							omissions.add(vc.fullName);
						}
						catch (IOException e) {}
					}
					else
					{
						omissions.add(fullVirtualName);
					}

					// recreate the zip file without the omissions
					boolean isCancelled = recreateZipDeleteEntries(vcList, dest, omissions, archiveOperationMonitor);
					if (isCancelled)
					{
						dest.close();
						if (!(outputTempFile == null)) outputTempFile.delete();
						if (closeZipFile) closeZipFile();
						throw new SystemOperationCancelledException();
					}

					dest.close();

					// Now replace the old zip file with the new one
					replaceOldZip(outputTempFile);

					// Now update the tree
					HashMap hm = (HashMap) _virtualFS.get(vc.path);
					hm.remove(vc.name);
					if (vc.isDirectory)
					{
						delTree(vc);
					}
					if (closeZipFile) closeZipFile();
					setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
					return true;
				}
			} else {
				throw new SystemLockTimeoutException(IClientServerConstants.PLUGIN_ID);
			}
		}
		catch (IOException e)
		{
			if (!(outputTempFile == null)) outputTempFile.delete();
			if (closeZipFile) closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not delete " + fullVirtualName, e); //$NON-NLS-1$
		}
		finally
		{
			releaseMutex(mutexLockStatus);
		}
		setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
		return false;
	}

	/**
	 * Deletes all the children of the directory VirtualChild <code>vc</code>
	 * recursively down to the leaves.
	 * Pre: vc.isDirectory is true
	 * @param vc The child whose children we are deleting.
	 */
	protected void delTree(VirtualChild vc)
	{
		HashMap hm = (HashMap) _virtualFS.get(vc.fullName);
		Object[] children = hm.values().toArray();
		for (int i = 0; i < children.length; i++)
		{
			VirtualChild next = (VirtualChild) children[i];
			hm.remove(next.name);
			if (next.isDirectory) delTree(next);
		}
		return;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void replace(String fullVirtualName, File file, String name, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		replace(fullVirtualName, file, name, true, archiveOperationMonitor);
	}

	/**
	 * Same as replace(String fullVirtualName, File file, String name) but you can choose whether
	 * or not you want to leave the zipfile open after return.
	 * @since org.eclipse.rse.services 3.0
	 */
	public void replace(String fullVirtualName, File file, String name, boolean closeZipFile, ISystemOperationMonitor archiveOperationMonitor)
			throws SystemMessageException
	{
		if (!_exists)
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);

		if (!file.exists() || !file.canRead()) {
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Cannot read: " + file); //$NON-NLS-1$
		}
		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
		if (!exists(fullVirtualName, archiveOperationMonitor))
		{
			// sorry, wrong method buddy
			add(file, fullVirtualName, name, archiveOperationMonitor);
		}

		File outputTempFile = null;
		try
		{
			openZipFile();
			// Open a new tempfile which will be our destination for the new zip
			outputTempFile = new File(_file.getAbsolutePath() + "temp"); //$NON-NLS-1$
			ZipOutputStream dest = new ZipOutputStream(new FileOutputStream(outputTempFile));
			dest.setMethod(ZipOutputStream.DEFLATED);
			// get all the entries in the old zip
			VirtualChild[] vcList = getVirtualChildrenList(false, archiveOperationMonitor);
			HashSet omissions = new HashSet();
			omissions.add(fullVirtualName);

			boolean isCancelled = recreateZipDeleteEntries(vcList, dest, omissions, archiveOperationMonitor);
			if (isCancelled)
			{
				dest.close();
				if (!(outputTempFile == null))
					outputTempFile.delete();
				if (closeZipFile)
					closeZipFile();
				throw new SystemOperationCancelledException();
			}

			// Now append the additional entry to the zip file.
			int i = fullVirtualName.lastIndexOf("/"); //$NON-NLS-1$
			String virtualPath;
			if (i == -1) {
				virtualPath = ""; //$NON-NLS-1$
			} else {
				virtualPath = fullVirtualName.substring(0, i);
			}


			// append the additional entry to the zip file.
			ZipEntry newEntry = appendFile(file, dest, virtualPath, name, SystemEncodingUtil.ENCODING_UTF_8, SystemEncodingUtil.ENCODING_UTF_8, false);

			// Add the new entry to the virtual file system in memory
			fillBranch(newEntry);

			dest.close();

			// Now replace the old zip file with the new one
			replaceOldZip(outputTempFile);

		} catch (IOException e) {
			if (!(outputTempFile == null))
				outputTempFile.delete();
			if (closeZipFile) closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not replace " + file.getName(), e); //$NON-NLS-1$
		}
		if (closeZipFile)
			closeZipFile();

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void replace(String fullVirtualName, InputStream stream, String name, String sourceEncoding, String targetEncoding, boolean isText,
			ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists)
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);

		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
		if (!exists(fullVirtualName, archiveOperationMonitor))
		{
			// wrong method
			add(stream, fullVirtualName, name, sourceEncoding, targetEncoding, isText, archiveOperationMonitor);
		}

		File outputTempFile = null;
		try
		{
			openZipFile();
			// Open a new tempfile which will be our destination for the new zip
			outputTempFile = new File(_file.getAbsolutePath() + "temp"); //$NON-NLS-1$
			ZipOutputStream dest = new ZipOutputStream(new FileOutputStream(outputTempFile));
			dest.setMethod(ZipOutputStream.DEFLATED);
			// get all the entries in the old zip
			VirtualChild[] vcList = getVirtualChildrenList(false, archiveOperationMonitor);
			HashSet omissions = new HashSet();
			omissions.add(fullVirtualName);
			boolean isCancelled = recreateZipDeleteEntries(vcList, dest, omissions, archiveOperationMonitor);
			if (isCancelled)
			{
				dest.close();
				if (!(outputTempFile == null))
					outputTempFile.delete();
				closeZipFile();
				throw new SystemOperationCancelledException();
			}

			// Now append the additional entry to the zip file.
			int i = fullVirtualName.lastIndexOf("/"); //$NON-NLS-1$
			String virtualPath;
			if (i == -1) {
				virtualPath = ""; //$NON-NLS-1$
			}
			else
			{
				virtualPath = fullVirtualName.substring(0, i);
			}
			appendBytes(stream, dest, virtualPath, name, sourceEncoding, targetEncoding, isText);
			dest.close();

			// Now replace the old zip file with the new one
			replaceOldZip(outputTempFile);

		} catch (IOException e) {
			if (!(outputTempFile == null))
				outputTempFile.delete();
			closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not replace " + fullVirtualName, e); //$NON-NLS-1$
		}
		closeZipFile();
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void fullRename(String fullVirtualName, String newFullVirtualName, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		fullRename(fullVirtualName, newFullVirtualName, true, archiveOperationMonitor);
	}

	/**
	 * Same as fullRename(String fullVirtualName, String newFullVirtualName) but
	 * you can choose whether or not you want to leave the zipfile open after
	 * return.
	 *
	 * @throws SystemMessageException in case of an error or user cancellation.
	 * @since org.eclipse.rse.services 3.0
	 */
	public void fullRename(String fullVirtualName, String newFullVirtualName, boolean closeZipFile, ISystemOperationMonitor archiveOperationMonitor)
			throws SystemMessageException
	{
		if (!_exists)
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);

		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
		newFullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(newFullVirtualName);
		VirtualChild vc = getVirtualFile(fullVirtualName, archiveOperationMonitor);
		if (!vc.exists())
		{
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "The virtual file " + fullVirtualName + " does not exist."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		File outputTempFile = null;
		int mutexLockStatus = SystemReentrantMutex.LOCK_STATUS_NOLOCK;
		try
		{
			mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
			if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
			{
				// Open a new tempfile which will be our destination for the new zip
				outputTempFile = new File(_file.getAbsolutePath() + "temp"); //$NON-NLS-1$
				ZipOutputStream  dest = new ZipOutputStream(
						new FileOutputStream(outputTempFile));
				dest.setMethod(ZipOutputStream.DEFLATED);
				// get all the entries in the old zip
				VirtualChild[] vcList = getVirtualChildrenList(false, archiveOperationMonitor);
				VirtualChild[] renameList = null;
				HashMap oldNewNames = new HashMap();
				HashMap newOldNames = new HashMap();
				// if the entry to rename is a directory, we must then rename
				// all files and directories below it en masse.
				if (vc.isDirectory)
				{
					renameList = getVirtualChildrenList(fullVirtualName, false, archiveOperationMonitor);
					for (int i = 0; i < renameList.length; i++)
					{
						int j = fullVirtualName.length();
						String suffix = renameList[i].fullName.substring(j);
						String newName = newFullVirtualName + suffix;
						if (renameList[i].isDirectory)
						{
							newName = newName + "/"; //$NON-NLS-1$
							oldNewNames.put(renameList[i].fullName + "/", newName); //$NON-NLS-1$
							newOldNames.put(newName, renameList[i].fullName + "/"); //$NON-NLS-1$
						}
						else
						{
							oldNewNames.put(renameList[i].fullName, newName);
							newOldNames.put(newName, renameList[i].fullName);
						}
					}
					oldNewNames.put(fullVirtualName + "/", newFullVirtualName + "/"); //$NON-NLS-1$ //$NON-NLS-2$
					newOldNames.put(newFullVirtualName + "/", fullVirtualName + "/"); //$NON-NLS-1$ //$NON-NLS-2$
					/*
					try
					{
						safeGetEntry(fullVirtualName);
						names.put(fullVirtualName + "/", newFullVirtualName + "/"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					catch (IOException e) {}
					 */
				}
				else
				{
					oldNewNames.put(fullVirtualName, newFullVirtualName);
					newOldNames.put(newFullVirtualName, fullVirtualName);
				}
				// find the entry to rename and rename it
				boolean isCancelled = recreateZipRenameEntries(vcList, dest, oldNewNames, archiveOperationMonitor);

				dest.close();

				if (isCancelled)
				{
					if (!(outputTempFile == null)) outputTempFile.delete();
					if (closeZipFile) closeZipFile();
					throw new SystemOperationCancelledException();
				}
				// Now replace the old zip file with the new one
				replaceOldZip(outputTempFile);

				// Now rebuild the tree
				updateTreeAfterRename(newOldNames, renameList);
				if (closeZipFile) closeZipFile();
			}
			else
			{
				throw new SystemLockTimeoutException(IClientServerConstants.PLUGIN_ID);
			}
		}
		catch (IOException e)
		{
			if (!(outputTempFile == null)) outputTempFile.delete();
			if (closeZipFile) closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not rename " + fullVirtualName, e); //$NON-NLS-1$
		}
		finally
		{
			releaseMutex(mutexLockStatus);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void move(String fullVirtualName, String destinationVirtualPath, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
		destinationVirtualPath = ArchiveHandlerManager.cleanUpVirtualPath(destinationVirtualPath);
		int i = fullVirtualName.lastIndexOf("/"); //$NON-NLS-1$
		if (i == -1)
		{
			fullRename(fullVirtualName, destinationVirtualPath + "/" + fullVirtualName, archiveOperationMonitor); //$NON-NLS-1$
		}
		String name = fullVirtualName.substring(i);
		fullRename(fullVirtualName, destinationVirtualPath + name, archiveOperationMonitor);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void rename(String fullVirtualName, String newName, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
		int i = fullVirtualName.lastIndexOf("/"); //$NON-NLS-1$
		if (i == -1)
		{
			fullRename(fullVirtualName, newName, archiveOperationMonitor);
		} else {
			String fullNewName = fullVirtualName.substring(0, i+1) + newName;
			fullRename(fullVirtualName, fullNewName, archiveOperationMonitor);
			setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
		}
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public File[] getFiles(String[] fullNames, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists) return new File[0];

		File[] files = new File[fullNames.length];
		for (int i = 0; i < fullNames.length; i++)
		{
			String name;
			String fullName = fullNames[i];
			fullName = ArchiveHandlerManager.cleanUpVirtualPath(fullName);
			int j = fullName.lastIndexOf("/"); //$NON-NLS-1$
			if (j == -1)
			{
				name = fullName;
			}
			else
			{
				name = fullName.substring(j+1);
			}
			try
			{
				files[i] = File.createTempFile(name, "virtual"); //$NON-NLS-1$
				files[i].deleteOnExit();
				extractVirtualFile(fullNames[i], files[i], archiveOperationMonitor);
			}
			catch (IOException e)
			{
				throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not extract virtual file: " + fullNames[i], e); //$NON-NLS-1$
			}
		}
		return files;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void createFolder(String name, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		name = ArchiveHandlerManager.cleanUpVirtualPath(name);
		name = name + "/"; //$NON-NLS-1$
		createVirtualObject(name, true, archiveOperationMonitor);
		setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void createFile(String name, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		name = ArchiveHandlerManager.cleanUpVirtualPath(name);
		createVirtualObject(name, true, archiveOperationMonitor);
		setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
	}

	/**
	 * Creates a new, empty object in the virtual File system, and creates an
	 * empty file or folder in the physical zip file.
	 *
	 * @param name The name of the file or folder to create. The object created
	 * 		will be a folder if and only if <code>name</code> ends in a "/".
	 * @return <code>true</code> if the object was created. <code>false</code>
	 * 	if the creation was a no-op because the object already existed.
	 * @throws SystemMessageException in case of an error or cancellation.
	 * @since org.eclipse.rse.services 3.0
	 */
	protected boolean createVirtualObject(String name, boolean closeZipFile, ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists)
		{
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);
		}
		if (exists(name, archiveOperationMonitor))
		{
			// The object already exists.
			return false;
		}

		int mutexLockStatus = SystemReentrantMutex.LOCK_STATUS_NOLOCK;
		try
		{
			mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
			if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
			{
				if (openZipFile())
				{
					File outputTempFile;

					// Open a new tempfile which will be our destination for the new zip
					outputTempFile = new File(_file.getAbsolutePath() + "temp"); //$NON-NLS-1$
					ZipOutputStream  dest = new ZipOutputStream(
							new FileOutputStream(outputTempFile));
					dest.setMethod(ZipOutputStream.DEFLATED);
					// get all the entries in the old zip
					VirtualChild[] vcList = getVirtualChildrenList(false, archiveOperationMonitor);

					// if it is an empty zip file, no need to recreate it
					if (!(vcList.length == 1) || !vcList[0].fullName.equals("")) //$NON-NLS-1$
					{
						boolean isCancelled = recreateZipDeleteEntries(vcList, dest, null, archiveOperationMonitor);
						if (isCancelled)
						{
							dest.close();
							if (!(outputTempFile == null)) outputTempFile.delete();
							if (closeZipFile) closeZipFile();
							throw new SystemOperationCancelledException();
						}
					}

					// append the additional entry to the zip file.
					ZipEntry newEntry = appendEmptyFile(dest, name);
					// Add the new entry to the virtual file system in memory
					fillBranch(newEntry);

					dest.close();

					// Now replace the old zip file with the new one
					replaceOldZip(outputTempFile);

					if (closeZipFile) closeZipFile();
					return true;
				}
			} else {
				throw new SystemLockTimeoutException(IClientServerConstants.PLUGIN_ID);
			}
		}
		catch (IOException e)
		{
			System.out.println();
			System.out.println(e.getMessage());
			if (closeZipFile) closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, "Could not add a file.", e); //$NON-NLS-1$
		}
		finally
		{
			releaseMutex(mutexLockStatus);
		}
		return false;
	}

	/**
	 * Works similarly to appendFile, except no actual data is appended
	 * to the zipfile, only an entry is created. Thus, if the file were
	 * to be extracted, it would be of length 0.
	 * @param dest The destination zip stream to append the entry.
	 * @param name The new, virtual fullname to give the entry.
	 * @return The ZipEntry that was created.
	 * @throws IOException If there was an error appending the entry to the stream.
	 */
	protected ZipEntry appendEmptyFile(ZipOutputStream dest, String name) throws IOException
	{
		boolean isDirectory = name.endsWith("/"); //$NON-NLS-1$
		ZipEntry newEntry;
		newEntry = createSafeZipEntry(name);
		dest.putNextEntry(newEntry);
		if (!isDirectory)
		{
			dest.write(new byte[0], 0, 0);
			dest.closeEntry();
		}
		return newEntry;
	}

	/**
	 * A "safe" ZipEntry is one whose virtual path does not begin with a
	 * "/". This seems to cause the least problems for archive utilities,
	 * including this one.
	 * @param name The virtual name for the new, safe ZipEntry.
	 * @return The ZipEntry that is created.
	 */
	protected ZipEntry createSafeZipEntry(String name)
	{
		if (name.startsWith("/")) name = name.substring(1); //$NON-NLS-1$
		return new ZipEntry(name);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.clientserver.archiveutils.ISystemArchiveHandler#getStandardName(org.eclipse.rse.services.clientserver.archiveutils.VirtualChild)
	 */
	public String getStandardName(VirtualChild vc)
	{
		if (vc.isDirectory) return vc.fullName + "/"; //$NON-NLS-1$
		return vc.fullName;
	}

	/**
	 * Opens the zipfile that this handler manages.
	 *
	 * @throws IOException in case of an error
	 */
	protected boolean openZipFile() throws IOException
	{
		if (!(_zipfile == null)) closeZipFile();
		_zipfile = new ZipFile(_file);
		return true;
	}

	protected boolean closeZipFile()
	{
		try
		{
			_zipfile.close();
		}
		catch (IOException e)
		{
			System.out.println("Could not close zipfile: " + _file); //$NON-NLS-1$
			System.out.println(e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * If the mod-times of the underlying zip file and the file used to
	 * create the virtualFS are different, update the virtualFS.
	 * @return whether or not the op was successful.
	 * @since org.eclipse.rse.services 3.0
	 */
	protected boolean updateVirtualFSIfNecessary(ISystemOperationMonitor archiveOperationMonitor)
	{
		if (_vfsLastModified != _file.lastModified())
		{
			int mutexLockStatus = SystemReentrantMutex.LOCK_STATUS_NOLOCK;
			try
			{
				mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
				if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
				{
					if (openZipFile())
					{
						buildTree();
						_vfsLastModified = _file.lastModified();
						closeZipFile();
						return true;
					}
					else
					{
						return false;
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				closeZipFile();
			}
			finally
			{
				releaseMutex(mutexLockStatus);
			}
		}
		return true;
	}

	/**
	 * Returns the entry corresponding to <code>name</code> from _zipfile. Never returns
	 * null, but rather, throws an IOException if it cannot find the entry. Tries to retrieve
	 * both <code>name</code> and <code>"/" + name<code>, to accomodate for zipfiles created
	 * in a unix environment. ASSUMES THAT _zipfile IS ALREADY OPEN!
	 */
	protected ZipEntry safeGetEntry(String name) throws IOException
	{
		ZipEntry entry = _zipfile.getEntry(name);
		if (entry == null) entry = _zipfile.getEntry("/" + name); //$NON-NLS-1$
		if (entry == null) throw new IOException("SystemZipHandler.safeGetEntry(): The ZipEntry " + name + " cannot be found in " + _file.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		return entry;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void create() throws SystemMessageException
	{
		try
		{
			// The zipfile is our destination
			ZipOutputStream  dest = new ZipOutputStream(
					new FileOutputStream(_file));
			dest.setMethod(ZipOutputStream.DEFLATED);

			VirtualChild[] vcList = new VirtualChild[0];

			HashSet omissions = new HashSet();
			// the above two statements force recreateZipDeleteEntries to create a dummy entry
			recreateZipDeleteEntries(vcList, dest, omissions, null);
			dest.close();

			openZipFile();
			buildTree();
			closeZipFile();
		}
		catch (IOException e)
		{
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
		}
		_exists = true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public SystemSearchLineMatch[] search(String fullVirtualName, SystemSearchStringMatcher matcher, ISystemOperationMonitor archiveOperationMonitor)
			throws SystemMessageException
	{

		// if the search string is empty or if it is "*", then return no matches
		// since it is a file search
		if (matcher.isSearchStringEmpty() || matcher.isSearchStringAsterisk()) {
			return new SystemSearchLineMatch[0];
		}

		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);

		VirtualChild vc = getVirtualFile(fullVirtualName, archiveOperationMonitor);

		if (!vc.exists() || vc.isDirectory) {
			return new SystemSearchLineMatch[0];
		}

		ZipEntry entry = null;
		SystemSearchLineMatch[] matches = null;

		try {
			openZipFile();
			entry = safeGetEntry(fullVirtualName);
			InputStream is = _zipfile.getInputStream(entry);

			if (is == null)
			{
				return new SystemSearchLineMatch[0];
			}

			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader bufReader = new BufferedReader(isr);

			SystemSearchStringMatchLocator locator = new SystemSearchStringMatchLocator(bufReader, matcher);
			matches = locator.locateMatches();
		} catch (IOException e) {
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
		}

		closeZipFile();

		if (matches == null) {
			return new SystemSearchLineMatch[0];
		}
		else {
			return matches;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.clientserver.archiveutils.ISystemArchiveHandler#exists()
	 */
	public boolean exists()
	{
		return _exists;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.clientserver.archiveutils.ISystemArchiveHandler#getCommentFor(java.lang.String)
	 */
	public String getCommentFor(String fullVirtualName) throws SystemMessageException
	{
		return getCommentFor(fullVirtualName, true);
	}

	/**
	 * same as getCommentFor(String) but you can choose whether or not to leave
	 * the zipfile open after the method is closed
	 */
	public String getCommentFor(String fullVirtualName, boolean closeZipFile) throws SystemMessageException
	{
		if (!_exists) return ""; //$NON-NLS-1$

		ZipEntry entry = null;
		try
		{
			openZipFile();
			fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
			entry = safeGetEntry(fullVirtualName);
		} catch (IOException e) {
			if (closeZipFile) closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
		}
		if (closeZipFile)
			closeZipFile();
		String comment = entry.getComment();
		if (comment == null)
			return ""; //$NON-NLS-1$
		else
			return comment;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.archiveutils.ISystemArchiveHandler#getCompressedSizeFor(java.lang.String)
	 */
	public long getCompressedSizeFor(String fullVirtualName) throws SystemMessageException
	{
		return getCompressedSizeFor(fullVirtualName, true);
	}

	/**
	 * same as getCompressedSizeFor(String) but you can choose whether or not to leave
	 * the zipfile open after the method is closed
	 */
	public long getCompressedSizeFor(String fullVirtualName, boolean closeZipFile) throws SystemMessageException
	{
		if (!_exists) return 0;

		ZipEntry entry = null;
		try
		{
			openZipFile();
			fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
			entry = safeGetEntry(fullVirtualName);
		}
		catch (IOException e)
		{
			if (closeZipFile) closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
		}
		if (closeZipFile) closeZipFile();
		return entry.getCompressedSize();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.archiveutils.ISystemArchiveHandler#getCompressionMethodFor(java.lang.String)
	 */
	public String getCompressionMethodFor(String fullVirtualName) throws SystemMessageException
	{
		return getCompressionMethodFor(fullVirtualName, true);
	}

	/**
	 * same as getCompressionMethodFor(String) but you can choose whether or not to leave
	 * the zipfile open after the method is closed
	 */
	public String getCompressionMethodFor(String fullVirtualName, boolean closeZipFile) throws SystemMessageException
	{
		if (!_exists) return ""; //$NON-NLS-1$

		ZipEntry entry = null;
		try
		{
			openZipFile();
			fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);
			entry = safeGetEntry(fullVirtualName);
		} catch (IOException e) {
			if (closeZipFile) closeZipFile();
			throw new SystemOperationFailedException(IClientServerConstants.PLUGIN_ID, e);
		}
		if (closeZipFile)
			closeZipFile();
		return (new Integer(entry.getMethod())).toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.archiveutils.ISystemArchiveHandler#getArchiveComment()
	 */
	public String getArchiveComment()
	{
		return ""; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.clientserver.archiveutils.ISystemArchiveHandler#getClassification(java.lang.String)
	 */
	public String getClassification(String fullVirtualName) throws SystemMessageException {
		return getClassification(fullVirtualName, true);
	}

	/**
	 * Same as getClassification(String), but you can choose whether to leave the zip file
	 * open after the method is closed.
	 * @see org.eclipse.rse.services.clientserver.archiveutils.ISystemArchiveHandler#getClassification(java.lang.String)
	 */
	public String getClassification(String fullVirtualName, boolean closeZipFile) throws SystemMessageException {

		// default type
		String type = "file"; //$NON-NLS-1$

		if (!_exists) {
			return type;
		}

		fullVirtualName = ArchiveHandlerManager.cleanUpVirtualPath(fullVirtualName);

		// if it's not a class file, we do not classify it
		if (!fullVirtualName.endsWith(".class")) { //$NON-NLS-1$
			return type;
		}

		// class file parser
		BasicClassFileParser parser = null;

		boolean isExecutable = false;

		// get the input stream for the entry
		InputStream stream = null;

		try {
			openZipFile();
			ZipEntry entry = safeGetEntry(fullVirtualName);
			stream = _zipfile.getInputStream(entry);

			// use class file parser to parse the class file
			parser = new BasicClassFileParser(stream);
			parser.parse();

			// query if it is executable, i.e. whether it has main method
			isExecutable = parser.isExecutable();

			if (closeZipFile) {
				closeZipFile();
			}
		} catch (IOException e) {

			if (closeZipFile) {
				closeZipFile();
			}

			return type;
		}

		// if it is executable, then also get qualified class name
		if (isExecutable && parser != null) {
			type = "executable(java"; //$NON-NLS-1$

			String qualifiedClassName = parser.getQualifiedClassName();

			if (qualifiedClassName != null) {
				type = type + ":" + qualifiedClassName; //$NON-NLS-1$
			}

			type = type + ")"; //$NON-NLS-1$
		}

		return type;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void add(File file, String virtualPath, String name, String sourceEncoding, String targetEncoding, ISystemFileTypes registry,
			ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists)
		{
			setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);
		}

		virtualPath = ArchiveHandlerManager.cleanUpVirtualPath(virtualPath);
		if (!file.isDirectory())
		{
			if (exists(virtualPath + "/" + name, archiveOperationMonitor)) //$NON-NLS-1$
			{
				// wrong method
				replace(virtualPath + "/" + name, file, name, archiveOperationMonitor); //$NON-NLS-1$
				setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
				return;
			}
			else
			{
				File[] files = new File[1];
				files[0] = file;
				String[] names = new String[1];
				names[0] = name;
				String[] sourceEncodings = new String[1];
				sourceEncodings[0] = sourceEncoding;
				String[] targetEncodings = new String[1];
				targetEncodings[0] = targetEncoding;
				boolean[] isTexts = new boolean[1];
				isTexts[0] = registry.isText(file);
				add(files, virtualPath, names, sourceEncodings, targetEncodings, isTexts, archiveOperationMonitor);
				setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
			}
		}
		else
		{
			//String sourceName = name;
			HashSet children = new HashSet();
			boolean isCancelled = listAllFiles(file, children, archiveOperationMonitor);
			if (isCancelled)
			{
				throw new SystemOperationCancelledException();
			}
			File[] sources = new File[children.size() + 1];
			String[] newNames = new String[children.size() + 1];
			Object[] kids = children.toArray();
			String[] sourceEncodings = new String[children.size() + 1];
			String[] targetEncodings = new String[children.size() + 1];
			boolean[] isTexts = new boolean[children.size() + 1];
			int charsToTrim = file.getParentFile().getAbsolutePath().length() + 1;
			if (file.getParentFile().getAbsolutePath().endsWith(File.separator)) charsToTrim--; // accounts for root
			for (int i = 0; i < children.size(); i++)
			{
				sources[i] = (File) kids[i];
				newNames[i] = sources[i].getAbsolutePath().substring(charsToTrim);
				newNames[i] = newNames[i].replace('\\','/');
				if (sources[i].isDirectory() && !newNames[i].endsWith("/")) newNames[i] = newNames[i] + "/"; //$NON-NLS-1$ //$NON-NLS-2$

				// this part can be changed to allow different encodings for different files
				sourceEncodings[i] = sourceEncoding;
				targetEncodings[i] = targetEncoding;
				isTexts[i] = registry.isText(sources[i]);
			}
			sources[children.size()] = file;
			newNames[children.size()] = name;
			sourceEncodings[children.size()] = sourceEncoding;
			targetEncodings[children.size()] = targetEncoding;

			isTexts[children.size()] = registry.isText(file);
			if  (!newNames[children.size()].endsWith("/")) newNames[children.size()] = newNames[children.size()] + "/"; //$NON-NLS-1$ //$NON-NLS-2$

			add(sources, virtualPath, newNames, sourceEncodings, targetEncodings, isTexts, archiveOperationMonitor);
			setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public void add(File file, String virtualPath, String name, String sourceEncoding, String targetEncoding, boolean isText,
			ISystemOperationMonitor archiveOperationMonitor) throws SystemMessageException
	{
		if (!_exists)
		{
			setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
			throw new SystemUnexpectedErrorException(IClientServerConstants.PLUGIN_ID);
		}

		virtualPath = ArchiveHandlerManager.cleanUpVirtualPath(virtualPath);

		int mutexLockStatus = SystemReentrantMutex.LOCK_STATUS_NOLOCK;
		try
		{
			mutexLockStatus = _mutex.waitForLock(archiveOperationMonitor, Long.MAX_VALUE);
			if (SystemReentrantMutex.LOCK_STATUS_NOLOCK != mutexLockStatus)
			{
				if (!file.isDirectory())
				{
					String fullVirtualName = getFullVirtualName(virtualPath, name);
					if (exists(fullVirtualName, archiveOperationMonitor))
					{
						replace(fullVirtualName, file, name, archiveOperationMonitor);
						setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
					}
					else
					{
						File[] files = new File[1];
						files[0] = file;
						String[] names = new String[1];
						names[0] = name;
						String[] sourceEncodings = new String[1];
						sourceEncodings[0] = sourceEncoding;
						String[] targetEncodings = new String[1];
						targetEncodings[0] = targetEncoding;
						boolean[] isTexts = new boolean[1];
						isTexts[0] = isText;
						add(files, virtualPath, names, sourceEncodings, targetEncodings, isTexts, archiveOperationMonitor);
						setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
					}
				}
				else
				{
					HashSet children = new HashSet();
					boolean isCancelled = listAllFiles(file, children, archiveOperationMonitor);
					if (isCancelled)
					{
						throw new SystemOperationCancelledException();
					}
					File[] sources = new File[children.size() + 1];
					String[] newNames = new String[children.size() + 1];
					Object[] kids = children.toArray();
					String[] sourceEncodings = new String[children.size() + 1];
					String[] targetEncodings = new String[children.size() + 1];
					boolean[] isTexts = new boolean[children.size() + 1];
					int charsToTrim = file.getParentFile().getAbsolutePath().length() + 1;
					if (file.getParentFile().getAbsolutePath().endsWith(File.separator)) charsToTrim--; // accounts for root
					for (int i = 0; i < children.size(); i++)
					{
						sources[i] = (File) kids[i];
						newNames[i] = sources[i].getAbsolutePath().substring(charsToTrim);
						newNames[i] = newNames[i].replace('\\','/');
						if (sources[i].isDirectory() && !newNames[i].endsWith("/")) newNames[i] = newNames[i] + "/"; //$NON-NLS-1$ //$NON-NLS-2$

						// this part can be changed to allow different encodings for different files
						sourceEncodings[i] = sourceEncoding;
						targetEncodings[i] = targetEncoding;
						isTexts[i] = isText;
					}
					sources[children.size()] = file;
					newNames[children.size()] = name;
					sourceEncodings[children.size()] = sourceEncoding;
					targetEncodings[children.size()] = targetEncoding;
					isTexts[children.size()] = isText;
					if  (!newNames[children.size()].endsWith("/")) newNames[children.size()] = newNames[children.size()] + "/"; //$NON-NLS-1$ //$NON-NLS-2$
					add(sources, virtualPath, newNames, sourceEncodings, targetEncodings, isTexts, archiveOperationMonitor);
					setArchiveOperationMonitorStatusDone(archiveOperationMonitor);
				}
			} else {
				throw new SystemLockTimeoutException(IClientServerConstants.PLUGIN_ID);
			}
		}
		catch (Exception e)
		{

		}
		finally
		{
			releaseMutex(mutexLockStatus);
		}
	}

	/**
	 * Construct the full virtual name of a virtual file from its virtual path and name.
	 * @param virtualPath the virtual path of this virtual file
	 * @param name the name of this virtual file
	 * @return the full virtual name of this virtual file
	 */
	private static String getFullVirtualName(String virtualPath, String name)
	{
		String fullVirtualName = null;
		if (virtualPath == null || virtualPath.length() == 0)
		{
			fullVirtualName = name;
		}
		else
		{
			fullVirtualName = virtualPath + "/" + name;  //$NON-NLS-1$
		}
		return fullVirtualName;
	}

	private void releaseMutex(int mutexLockStatus)
	{
		//We only release the mutex if we acquired it, not borrowed it.
		if (SystemReentrantMutex.LOCK_STATUS_AQUIRED == mutexLockStatus)
		{
			_mutex.release();
		}
	}

	private void setArchiveOperationMonitorStatusDone(ISystemOperationMonitor archiveOperationMonitor)
	{
		//We only set the status of the archive operation monitor to done if it is not been cancelled.
		if (null != archiveOperationMonitor && !archiveOperationMonitor.isCancelled())
		{
			archiveOperationMonitor.setDone(true);
		}
	}
}
