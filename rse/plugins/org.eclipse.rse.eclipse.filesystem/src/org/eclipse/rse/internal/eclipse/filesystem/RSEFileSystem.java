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
 * David Dykstal (IBM) - 168977: refactoring IConnectorService and ServerLauncher hierarchies
 * Kushal Munir (IBM) - moved to internal package
 * Martin Oberhuber (Wind River) - [181917] EFS Improvements: Avoid unclosed Streams,
 *    - Fix early startup issues by deferring FileStore evaluation and classloading,
 *    - Improve performance by RSEFileStore instance factory and caching IRemoteFile.
 *    - Also remove unnecessary class RSEFileCache and obsolete branding files.
 ********************************************************************************/

package org.eclipse.rse.internal.eclipse.filesystem;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;

public class RSEFileSystem extends FileSystem 
{
	private static RSEFileSystem _instance = new RSEFileSystem();

	/**
	 * Default constructor.
	 */
	public RSEFileSystem() {
		super();
	}
	
	/**
	 * Return the singleton instance of this file system.
	 * @return the singleton instance of this file system.
	 */
	public static RSEFileSystem getInstance() {
		return _instance;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileSystem#canDelete()
	 */
	public boolean canDelete() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileSystem#canWrite()
	 */
	public boolean canWrite() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileSystem#getStore(java.net.URI)
	 */
	public IFileStore getStore(URI uri) {
		try  {
			return RSEFileStore.getInstance(uri);
		} 
		catch (Exception e) {
			//Could be an URI format exception
			return EFS.getNullFileSystem().getStore(uri);
		}
	}

	/**
	 * Return an URI uniquely naming an RSE remote resource.
	 * @param hostNameOrAddr host name or IP address of remote system
	 * @param absolutePath absolute path to resource as valid on the remote system
	 * @return an URI uniquely naming the remote resource.
	 */
	public static URI getURIFor(String hostNameOrAddr, String absolutePath) {
		//FIXME backslashes are valid in UNIX file names. This is not correctly handled yet.
		if (absolutePath.charAt(0) != '/') {
			absolutePath = "/" + absolutePath.replace('\\', '/'); //$NON-NLS-1$
		}
		try {
			return new URI("rse", hostNameOrAddr, absolutePath, null); //$NON-NLS-1$
		}
		catch (URISyntaxException e) 
		{
			throw new RuntimeException(e);
		}
	}
}