/*******************************************************************************
 * Copyright (c) 2002, 2013 IBM Corporation and others.
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
 * David McKnight   (IBM)        - [173518] [refresh] Read only changes are not shown in RSE until the parent folder is refreshed
 * David McKnight   (IBM)        - [186363] get rid of obsolete calls to ISubSystem.connect()
 * David McKnight   (IBM)        - [209660] use parent encoding as default, rather than system encoding
 * David McKnight   (IBM)        - [209593] [api] add support for "file permissions" and "owner" properties for unix files
 * Martin Oberhuber (Wind River) - [220020][api][breaking] SystemFileTransferModeRegistry should be internal
 * Martin Oberhuber (Wind River) - [219975] Fix implementations of clone()
 * David McKnight   (IBM)        - [231209] [api][breaking] IRemoteFile.getSystemConnection() should be changed to IRemoteFile.getHost()
 * David McKnight   (IBM)        - [277911] cached results of remote file query need to be sorted
 * David McKnight   (IBM)        - [289387] Remote Search does not return line nodes in result tree
 * David McKnight (IBM)  - [283033] remoteFileTypes extension point should include "xml" type
 * David McKnight   (IBM)        - [328098] infinite loop when opening file from an UNC path
 * Rob Stryker (Red Hat)         - [393384] isArchive returning true on remote folders ending in .jar
 * David McKnight   (IBM)        - [409785] When copying a file from a network location (e.g. \\myserver\xxx\abc.cpp) to a mapped local location in RSE, workbench hangs on this filecopy
 *******************************************************************************/

package org.eclipse.rse.subsystems.files.core.subsystems;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemContentsType;
import org.eclipse.rse.core.subsystems.IRemoteContainer;
import org.eclipse.rse.core.subsystems.RemoteChildrenContentsType;
import org.eclipse.rse.services.clientserver.StringComparePatternMatcher;
import org.eclipse.rse.services.clientserver.archiveutils.ArchiveHandlerManager;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.IHostFile;
import org.eclipse.rse.services.files.IHostFilePermissions;
import org.eclipse.rse.services.files.IHostFilePermissionsContainer;
import org.eclipse.rse.subsystems.files.core.model.ISystemFileTransferModeRegistry;
import org.eclipse.rse.subsystems.files.core.model.RemoteFileFilterString;
import org.eclipse.rse.subsystems.files.core.model.RemoteFileUtility;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.swt.widgets.Display;

/**
 * A remote file represents a named file on a remote file system. This class
 * works with remote file names that do not include the preceding "<connectionName/subsystemName>"
 * prefix. Such ultimately-qualified names are known as IRemoteFilePath names.
 * <p>
 * Base parent class that supplies all of the
 *  functionality required by the IRemoteFile interface.
 * <p>
 * This base functionality is possible because this is a
 * read-only representation of a remote file ... all actions
 * like delete and rename are handled by the subsystem.
 * <p>
 * For all this base functionality to work, the subsystem
 * that creates this object must call the setter methods to
 * prefill this object with the core required information:
 * <ul>
 *  <li>{@link #RemoteFile(IRemoteFileContext)} constructor sets the parent subsystem
 * </ul>
 * A concrete implementation which extends this class, also needs to ensure
 * that information is properly set by some means such that the {@link IRemoteFile#exists()},
 * {@link IRemoteFile#getAbsolutePath()}, {@link IRemoteFile#getLength()}
 * and similar methods can be implemented.
 */
public abstract class RemoteFile implements IRemoteFile,  IAdaptable, Comparable
{
	protected IRemoteFileContext _context;

    protected String _label;
    protected Object remoteObj;
    protected IRemoteFile _parentFile;

    /* Archived file properties */
    // DKM - let's get rid of these fields
    //  - they should now be in IHostfile
    protected boolean isContainer = false;

    // master hash map
    protected HashMap _contents = new HashMap();

    /* container properties */
    protected boolean _isStale = true;

    // properties
    protected HashMap properties = new HashMap();
    protected HashMap propertyStates = new HashMap();



    /**
     * Constructor that takes a context object containing important information.
     * @param context An object holding contextual information about this object
     * @see org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileContext
     */
    public RemoteFile(IRemoteFileContext context)
    {
    	this._context = context;
    	if ((context!=null) && (context.getParentRemoteFileSubSystem()!=null) &&
    	    !context.getParentRemoteFileSubSystem().isConnected())
    	  try
    	  {
    	  	// deduce active shell from display
    	    IRemoteFileSubSystem ss = context.getParentRemoteFileSubSystem();

			if (Display.getCurrent() == null) {
				ss.connect(new NullProgressMonitor(), false);
			} else {
				ss.connect(false, null);
			}
    	  } catch (Exception exc) {}
    }


    /**
     * Set the filter string resolved to get this object
     */
    public void setFilterString(RemoteFileFilterString filterString)
    {
    	_context.setFilterString(filterString);
    }


    public void setLabel(String newLabel)
    {
    	_label = newLabel;
    }


    // ------------------------------------------------------------------------
    // GETTER METHODS. ALL FULLY IMPLEMENTED ASSUMING SETTERS HAVE BEEN CALLED.
    // ------------------------------------------------------------------------

    /**
     * Return the context associated with this remote file
     */
    public IRemoteFileContext getContext()
    {
    	return _context;
    }

    /**
     * Return the parent subsystem
     */
    public IRemoteFileSubSystem getParentRemoteFileSubSystem()
    {
    	return _context.getParentRemoteFileSubSystem();
    }
    /**
     * Return the parent subsystem factory
     */
    public IRemoteFileSubSystemConfiguration getParentRemoteFileSubSystemConfiguration()
    {
    	IRemoteFileSubSystem ss = _context.getParentRemoteFileSubSystem();
    	if (ss == null)
    	  return null;
    	else
    	  return ss.getParentRemoteFileSubSystemConfiguration();
    }

    public void setParentRemoteFile(IRemoteFile parentFile)
    {
    	this._parentFile = parentFile;
    }

    /**
     * Return the parent remote file object expanded to get this object, or null if no such parent
     */
    public IRemoteFile getParentRemoteFile()
    {
    	if (this._parentFile == null)
    	{
    		if (isRoot())
    		{
    			return null;
    		}

	    	IRemoteFile parentFile = null;

//	    		_context.getParentRemoteFile();

	    	String pathOnly = getParentPath();
	    	if (pathOnly != null)
	    	{
	     	  IRemoteFileSubSystem ss = _context.getParentRemoteFileSubSystem();
	    	  if (ss != null)
	    	  {
	    		 IProgressMonitor monitor = new NullProgressMonitor();
	    	  	try {
		    	  	char sep = getSeparatorChar();
		    	  	if (pathOnly.length() == 0)
		    	  	  parentFile = ss.getRemoteFileObject(pathOnly, monitor);
		    	  	else if (pathOnly.length() == 1)
		    	  	{
		    	  		if (pathOnly.charAt(0) == sep)
		    	  		{
		    	  			parentFile = ss.getRemoteFileObject(pathOnly, monitor); // root file
		    	  		}
		    	  	}
		    	  	else if (!(pathOnly.charAt(pathOnly.length()-1)==sep))
		              parentFile = ss.getRemoteFileObject(pathOnly+sep, monitor);
		            else
		              parentFile = ss.getRemoteFileObject(pathOnly, monitor);
	    	  	} catch (SystemMessageException e) {
	    	  		SystemBasePlugin.logError("RemoteFileImpl.getParentRemoteFile()", e); //$NON-NLS-1$
	    	  	}
	    	  }
	    	}
	    	else
	    	{
	    	}
	    	this._parentFile = parentFile;
    	}
    	return this._parentFile;
    }
    /**
     * Return the filter string resolved to get this object
     */
    public RemoteFileFilterString getFilterString()
    {
    	return _context.getFilterString();
    }
    /**
     * If this is a folder, it is possible that it is listed as part of a multiple filter string
     *  filter. In this case, when the folder is expanded, we want to filter the file names to
     *  show all the files that match any of the filter strings that have the same parent path.
     * <p>
     * This method supports that by returning all the filter strings in the filter which have the
     *  same parent path as was used to produce this file.
     */
    public RemoteFileFilterString[] getAllFilterStrings()
    {
    	return _context.getAllFilterStrings();
    }
    /**
     * Return the separator character for this file system: \ or /.
     * Queries it from the subsystem factory.
     */
    public char getSeparatorChar()
    {
    	IRemoteFileSubSystemConfiguration ssf = getParentRemoteFileSubSystemConfiguration();
    	if (ssf != null)
    	  return ssf.getSeparatorChar();
    	else
    	  return java.io.File.separatorChar;
    }
    /**
     * Return the separator character for this file system, as a string: "\" or "/".
     * Queries it from the subsystem factory.
     */
    public String getSeparator()
    {
    	IRemoteFileSubSystemConfiguration ssf = getParentRemoteFileSubSystemConfiguration();
    	if (ssf != null)
    	  return ssf.getSeparator();
    	else
    	  return java.io.File.separator;
    }
	/**
	 * Return as a string the line separator character values
	 */
	public String getLineSeparator()
	{
    	IRemoteFileSubSystemConfiguration ssf = getParentRemoteFileSubSystemConfiguration();
    	if (ssf != null)
    	  return ssf.getLineSeparator();
    	else
    	  return System.getProperty("line.separator"); //$NON-NLS-1$
	}
    /**
     * Return if this is a file on Unix, versus windows say
     * Queries it from the subsystem factory.
     */
    public boolean isUnix()
    {
    	IRemoteFileSubSystemConfiguration ssf = getParentRemoteFileSubSystemConfiguration();
    	if (ssf != null)
    	  return ssf.isUnixStyle();
    	else
    	  return System.getProperty("os.name").toLowerCase().startsWith("windows"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean isLink()
    {
    	String classifyString = getClassification();

        if (classifyString == null)
        {
            return false;
        }
    	else if (classifyString.indexOf("symbolic link") > -1) //$NON-NLS-1$
    	{
    		return true;
    	}
    	return false;
    }


    public boolean isExecutable()
    {
    	String classifyString = getClassification();

        if (classifyString == null)
        {
            return false;
        }
    	else if (classifyString.indexOf("executable") > -1) //$NON-NLS-1$
    	{
    		return true;
    	}
    	return false;
    }


	public boolean isArchive()
	{
		File file = new File(getAbsolutePath());
		return isFile() && ArchiveHandlerManager.getInstance().isArchive(file);
	}

   	/**
	 * Return the connection this remote file is from.
	 *
	 * @since 3.0 renamed getSystemConnection() to getHost()
	 */
    public IHost getHost()
    {
    	IRemoteFileSubSystem ss = _context.getParentRemoteFileSubSystem();
    	if (ss == null)
    	  return null;
    	else
    	  return ss.getHost();
    }

    /**
     * Get fully qualified connection and file name: connection:\path\file
     * Note the separator character between the profile name and the connection name is always '.'
     * Note the separator character between the connection and qualified-file is always ':'
     */
    public String getAbsolutePathPlusConnection()
    {
    	IHost conn = getHost();
    	if (conn == null)
    	  return getAbsolutePath();
    	else
    	  return conn.getSystemProfileName()+'.'+conn.getAliasName() + CONNECTION_DELIMITER + getAbsolutePath();
    }

	/**
	 * @see IRemoteFile#getLabel()
	 */
	public String getLabel()
	{
		if (_label != null)
		{
			return _label;
		}
		return getName();
	}


	/**
	 * @see IRemoteFile#isBinary()
	 */
	public boolean isBinary()
	{
		if (isDirectory())
		  return false;
		
		else {
			ISystemFileTransferModeRegistry reg = RemoteFileUtility.getSystemFileTransferModeRegistry();
			return reg.isBinary(this) || reg.isXML(this); // xml files also transfer as binary
		}
	}

	/**
	 * @see IRemoteFile#isText()
	 */
	public boolean isText()
	{
		if (isDirectory())
		  return false;
		else
		  return RemoteFileUtility.getSystemFileTransferModeRegistry().isText(this);
	}




	/**
	 * @see IRemoteFile#getLastModifiedDate()
	 */
	public Date getLastModifiedDate()
	{
		return new Date(getLastModified());
	}


	/**
	 * Return the extension part of a file name.
	 * Eg, for abc.java, return "java"
	 */
	public String getExtension()
	{
		String nameOnly = getName();
		if (nameOnly == null)
		  return null;
		int idx = nameOnly.lastIndexOf('.');
		if (idx >= 0)
		  return nameOnly.substring(idx+1);
        return null; // TODO - why null?
	}


	/**
	 * Return the cached copy of this remote file.  The returned IFile must be used for read-only
	 * purposes since no locks are acquired on the remote file.
	 *
	 * @return IFile The cached copy of this file if it exists AND it is upto date.  null is returned if a local
	 * cached copy of this file is not available or the local cached copy is not upto date (last modified
	 * timestamp comparison.)
	 */
// FIXME - core and ui separate now (editor is ui)
//	public IFile getCachedCopy() throws SystemMessageException
//	{
//		if (SystemRemoteEditManager.getDefault().doesRemoteEditProjectExist())
//		{
//			IResource replica = UniversalFileTransferUtility.getTempFileFor(this);
//			if (replica != null && replica.exists())
//			{
//				return (IFile)replica;
//			}
//		}
//		return null;
//	}

    // -----------------------
    // HOUSEKEEPING METHODS...
    // -----------------------
    public String toString()
    {
    	return getName();
    }

    /**
	 * This is the method required by the IAdaptable interface.
	 * Given an adapter class type, return an object castable to the type, or
	 *  null if this is not possible.
	 * <p>
	 * By default this returns Platform.getAdapterManager().getAdapter(this, adapterType);
	 * This in turn results in the default subsystem adapter SystemViewSubSystemAdapter,
	 * in package org.eclipse.rse.ui.view.
	 */
    public Object getAdapter(Class adapterType)
    {
   	    return Platform.getAdapterManager().getAdapter(this, adapterType);
    }

    // -------------------------------
    // java.util.Comparable methods...
    // -------------------------------
    /**
     * Compare one remote file to another. This enables us to sort the files so they
     * are shown folders-first, and in alphabetical order.
     */
    public int compareTo(Object other) throws ClassCastException
    {
    	IRemoteFile otherFile = (IRemoteFile)other;
        if (isDirectory() && !otherFile.isDirectory())
          return -1; // we are a folder so we are less than a file
        else if (!isDirectory() && otherFile.isDirectory())
          return 1; // we are a file so we are more than a folder
    	String comp1 = getName();

        String comp2 = otherFile.getName();
        if (comp2 == null)
          comp2 = otherFile.getParentPath();
    	return comp1.toLowerCase().compareTo(comp2.toLowerCase());
    }

    // ==================================
    // for a proxy mimicing java.io.File
    // ==================================

    public void setFile (Object obj)
    {
    	remoteObj = obj;
    }

    public Object getFile()
    {
    	return remoteObj;
    }

	public boolean isAncestorOf(IRemoteFile file)
	{
		String separator = this.getSeparator();
		if (this instanceof IVirtualRemoteFile) separator = "/"; //$NON-NLS-1$
		if (this.isArchive()) separator = ArchiveHandlerManager.VIRTUAL_SEPARATOR;

		return file.getAbsolutePathPlusConnection().startsWith(this.getAbsolutePathPlusConnection() + separator);
	}

	public boolean isDescendantOf(IRemoteFile file)
	{
		String separator = file.getSeparator();
		if (this instanceof IVirtualRemoteFile) separator = "/"; //$NON-NLS-1$
		if (file.isArchive()) separator = ArchiveHandlerManager.VIRTUAL_SEPARATOR;

		return this.getAbsolutePathPlusConnection().startsWith(file.getAbsolutePathPlusConnection() + separator);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemContainer#hasContents(org.eclipse.rse.core.model.ISystemContentsType)
	 */
	public boolean hasContents(ISystemContentsType contentsType)
	{
		boolean result = _contents.containsKey(contentsType);

// 		KM: comment out this code to prevent us picking up wrong cache
//		KM: defect 45072
//		if (!result)
//		{
//			if (contentsType == RemoteFileChildrenContentsType.getInstance())
//			{
//				return hasContents(RemoteChildrenContentsType.getInstance());
//			}
//			else if (contentsType == RemoteFolderChildrenContentsType.getInstance())
//			{
//				return hasContents(RemoteChildrenContentsType.getInstance());
//			}
//		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.IRemoteContainer#hasContents(org.eclipse.rse.core.model.ISystemContentsType, java.lang.String)
	 */
	public boolean hasContents(ISystemContentsType contentsType, String filter) {
		HashMap filters = (HashMap)(_contents.get(contentsType));

		if (filters == null) {
			return false;
		}

		if (filter == null) {
			filter = "*"; //$NON-NLS-1$
		}

		boolean result = containsFilterKey(filters, filter);

// 		KM: comment out this code to prevent us picking up wrong cache
//		KM: defect 45072
//		if (!result)
//		{
//			if (contentsType == RemoteFileChildrenContentsType.getInstance())
//			{
//				return hasContents(RemoteChildrenContentsType.getInstance(), filter);
//			}
//			else if (contentsType == RemoteFolderChildrenContentsType.getInstance())
//			{
//				return hasContents(RemoteChildrenContentsType.getInstance(), filter);
//			}
//		}

		return result;
	}

	protected boolean containsFilterKey(HashMap filters, String filter)
	{
		if (filters.containsKey(filter))
		{
			return true;
		}
		else
		{

			Set keySet = filters.keySet();
			Object[] keyArray = keySet.toArray();
			for (int i = 0; i < keyArray.length; i++)
			{
				String key = (String)keyArray[i];
				StringComparePatternMatcher matcher = new StringComparePatternMatcher(key);
				if (matcher.stringMatches(filter))
				{
					return true;
				}
				/*
				StringComparePatternMatcher matcher = new StringComparePatternMatcher(filter);
				if (matcher.stringMatches(key))
				{
					return true;
				}
				*/
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.model.ISystemContainer#getContents(org.eclipse.rse.core.model.ISystemContentsType)
	 */
	public Object[] getContents(ISystemContentsType contentsType)
	{
		return getContents(contentsType, "*"); //$NON-NLS-1$
	}

	private Object[] combine(Object[] set1, Object[] set2)
	{
		ArrayList result = new ArrayList(set1.length + set2.length);

		for (int i = 0; i < set1.length; i++)
		{
			result.add(set1[i]);
		}

		for (int j = 0; j < set2.length; j++)
		{
			result.add(set2[j]);
		}

		return result.toArray(new IRemoteFile[result.size()]);
	}

	/*
	private Object[] getFiles(Object[] filesAndFolders)
	{
		List results = new ArrayList();
		for (int i = 0; i < filesAndFolders.length; i++)
		{
			IRemoteFile fileOrFolder = (IRemoteFile)filesAndFolders[i];
			boolean supportsArchiveManagement = fileOrFolder.getParentRemoteFileSubSystem().getParentRemoteFileSubSystemConfiguration().supportsArchiveManagement();
			if (fileOrFolder.isFile() || (fileOrFolder.isArchive() && supportsArchiveManagement))
			{
				results.add(fileOrFolder);
			}
		}
		return results.toArray();
	}
	*/

	private Object[] getFolders(Object[] filesAndFolders)
	{
		List results = new ArrayList();
		for (int i = 0; i < filesAndFolders.length; i++)
		{
			IRemoteFile fileOrFolder = (IRemoteFile)filesAndFolders[i];
			boolean supportsArchiveManagement = fileOrFolder.getParentRemoteFileSubSystem().getParentRemoteFileSubSystemConfiguration().supportsArchiveManagement();
			if (!fileOrFolder.isFile() || (fileOrFolder.isArchive() && supportsArchiveManagement))
			{
				results.add(fileOrFolder);
			}
		}
		return results.toArray();
	}


	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.IRemoteContainer#getContents(org.eclipse.rse.core.model.ISystemContentsType, java.lang.String)
	 */
	public Object[] getContents(ISystemContentsType contentsType, String filter)
	{
		HashMap filters = (HashMap)(_contents.get(contentsType));
		Object[] results = null;
		if (filters == null || filters.isEmpty())
		{
			if (contentsType == RemoteChildrenContentsType.getInstance())
			{
				if (hasContents(RemoteFileChildrenContentsType.getInstance()) && hasContents(RemoteFolderChildrenContentsType.getInstance()))
				{
					// implies both files and folders
					Object[] folders = getContents(RemoteFolderChildrenContentsType.getInstance(), filter);
					Object[] files = getContents(RemoteFileChildrenContentsType.getInstance(), filter);

					return combine(folders, files);
				}
			}
			else if  (contentsType == RemoteFileChildrenContentsType.getInstance())
			{
				if (hasContents(RemoteChildrenContentsType.getInstance()))
				{
					Object[] filesAndFolders = getContents(RemoteChildrenContentsType.getInstance());
					return filesAndFolders;
				}
			}
			else if (contentsType == RemoteFolderChildrenContentsType.getInstance())
			{
				if (hasContents(RemoteChildrenContentsType.getInstance()))
				{
					Object[] filesAndFolders = getContents(RemoteChildrenContentsType.getInstance());
					return getFolders(filesAndFolders);
				}
			}
			return null;
		}

		if (filter == null) {
			filter = "*"; //$NON-NLS-1$
		}

		if (filters.containsKey(filter)){
			Object[] filterResults = (Object[])filters.get(filter);
			results = filterResults;			
		}
		else {
			ArrayList calculatedResults = new ArrayList();
	
			StringComparePatternMatcher fmatcher = new StringComparePatternMatcher(filter);
	
			// the filter may be a subset of existing filters
			Object[] keySet = filters.keySet().toArray();
	
			for (int i = 0; i < keySet.length; i++) {
	
				String key = (String)keySet[i];
	
				// KM: we need to match with the key to ensure that the filter is a subset
				StringComparePatternMatcher matcher = new StringComparePatternMatcher(key);
	
				if (matcher.stringMatches(filter)) {
					// get all children, i.e. the superset
					Object[] all = (Object[]) filters.get(key);
	
					if (all != null) {
	
						for (int s = 0; s < all.length; s++) {
	
							Object subContent = all[s];
	
							if (!calculatedResults.contains(subContent)) {
	
								if (subContent instanceof IRemoteFile) {
	
									IRemoteFile temp = (IRemoteFile) subContent;
	
									if (temp.isFile()) {
										String compareTo = null;
										boolean filterForFileTypes = isFilterForFileTypes(filter);
	
										if (!filterForFileTypes) {
											compareTo = temp.getName();
										}
										else {
											compareTo = temp.getExtension();
										}
	
										// match with the filter to take out those
										// that do not match the filter
										if (compareTo != null && fmatcher.stringMatches(compareTo)) {
											calculatedResults.add(subContent);
										}
									}
									else {
										calculatedResults.add(subContent);
									}
								}
								else {
									calculatedResults.add(subContent);
								}
							}
						}
					}
				}				
			}
			results = calculatedResults.toArray();
		}

		if (!isFile()){
			try { // search results aren't comparable so we need to catch exception
				Arrays.sort(results);
			}
			catch (Exception e){			
			}
		}
		return results;
	}

	/**
	 * Returns whether filter is for file types.
	 * @param filter the filter.
	 * @return <code>true</code> if filter is for file types, <code>false</code> otherwise.
	 */
	private boolean isFilterForFileTypes(String filter) {

		if (filter.endsWith(",")) { //$NON-NLS-1$
			return true;
		}
		else {
			return false;
		}
	}

	public void setIsContainer(boolean con) {
		isContainer = con;
	}

	 /*
     * Replace occurrences of cached object with new object
     */
    public void replaceContent(Object oldObject, Object newObject)
    {
    	HashMap filters = (HashMap)(_contents.get(RemoteChildrenContentsType.getInstance()));
    	if (filters != null)
    	{
	    	Collection values = filters.values();
	    	Object[] valuesArray = values.toArray();
	    	for (int i = 0; i < valuesArray.length; i++)
	    	{
	    		Object[] children = (Object[])valuesArray[i];
	    		for (int j = 0; j < children.length; j++)
	    		{
	    			if (children[j]== oldObject)
	    			{
	    				children[j]= newObject;
	    			}
	    		}
	    	}
    	}
    }



	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.IRemoteContainer#setContents(org.eclipse.rse.core.model.ISystemContentsType, java.lang.String, java.lang.Object[])
	 */
	public void setContents(ISystemContentsType contentsType, String filter, Object[] con) {

		if (filter == null) {
			filter = "*"; //$NON-NLS-1$
		}

	// DKM - consider all containers so that we can tell if an empty one
	// has been queried before (i.e. via IRemoteFile[0] as it's contents vs null)
//		if (con != null && con.length > 0)
		{
			isContainer = true;
		}

		HashMap filters = (HashMap)(_contents.get(contentsType));

		if (filters == null)
		{
			filters = new HashMap();
		}

		if (isContainer)
		{
			filters.put(filter, con);
			_contents.put(contentsType, filters);
		}

		// set parent folders
		if (isContainer && con != null)
		{
			for (int i = 0; i < con.length; i++)
			{
				if (con[i] instanceof RemoteFile)
				{
					RemoteFile rFile = (RemoteFile)con[i];
					rFile.setParentRemoteFile(this);
				}
			}
		}
	}

	/**
	 * @see org.eclipse.rse.core.subsystems.IRemoteContainer#isStale()
	 */
	public boolean isStale()
	{
		return _isStale || !exists();
	}



	/**
	 * @see org.eclipse.rse.core.subsystems.IRemoteContainer#markStale(boolean)
	 */
	public void markStale(boolean isStale)
	{
		markStale(isStale, true);
	}

	/**
	 * @see org.eclipse.rse.core.subsystems.IRemoteContainer#markStale(boolean)
	 */
	public void markStale(boolean isStale, boolean clearCache)
	{
		_isStale = isStale;

		if (isStale && clearCache)
		{

			Iterator iter = _contents.keySet().iterator();

			while (iter.hasNext()) {
				Object contentsType = iter.next();
				if (contentsType instanceof ISystemContentsType)
				{
				    if (!((ISystemContentsType)contentsType).isPersistent())
				    {
						HashMap filters = (HashMap)(_contents.get(contentsType));

						if (filters != null)
						{
							filters.clear();
						}
				    }
				}
			}

			if (_parentFile != null)
			{
				_parentFile.markStale(true, false);
			}


		}
	}

	public void copyContentsTo(IRemoteContainer target)
	{
	    Iterator iter = _contents.keySet().iterator();

		while (iter.hasNext())
		{
			Object contentsType = iter.next();
			if (contentsType instanceof ISystemContentsType)
			{
			    ISystemContentsType ct = (ISystemContentsType)contentsType;
			    if (ct.isPersistent())
			    {
			        HashMap filters = (HashMap)(_contents.get(ct));

			        Iterator fiter = filters.keySet().iterator();

					while (fiter.hasNext())
					{
					    Object filter = fiter.next();
					    Object fcontents = filters.get(filter);
					    if (fcontents != null && fcontents instanceof Object[])
					    {
					        target.setContents(ct, (String)filter, (Object[])fcontents);
					    }
					}
			    }
			}
		}
	}

	/**
	 * @see org.eclipse.rse.core.subsystems.IRemotePropertyHolder#getProperties(java.lang.String[])
	 */
	public Object[] getProperties(String[] keys) {

		Object[] values = new Object[keys.length];

		for (int i = 0; i < keys.length; i++) {
			values[i] = properties.get(keys[i]);
		}

		return values;
	}

	/**
	 * @see org.eclipse.rse.core.subsystems.IRemotePropertyHolder#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * @see org.eclipse.rse.core.subsystems.IRemotePropertyHolder#isPropertyStale(java.lang.String)
	 */
	public boolean isPropertyStale(String key) {

		Boolean b = (Boolean)(propertyStates.get(key));

		if (b == null) {
			return false;
		}
		else {
			return b.booleanValue();
		}
	}

	/**
	 * @see org.eclipse.rse.core.subsystems.IRemotePropertyHolder#markAllPropertiesStale()
	 */
	public void markAllPropertiesStale() {
		Iterator iter = propertyStates.keySet().iterator();

		while (iter.hasNext()) {
			String key = (String)(iter.next());
			markPropertyStale(key);
		}
	}

	/**
	 * @see org.eclipse.rse.core.subsystems.IRemotePropertyHolder#markPropertyStale(java.lang.String)
	 */
	public void markPropertyStale(String key) {
		propertyStates.put(key, Boolean.FALSE);
	}

	/**
	 * @see org.eclipse.rse.core.subsystems.IRemotePropertyHolder#setProperties(java.lang.String[], java.lang.Object[])
	 */
	public void setProperties(String[] keys, Object[] values) {

		for (int i = 0; i < keys.length; i++) {
			setProperty(keys[i], values[i]);
		}
	}

	/**
	 * @see org.eclipse.rse.core.subsystems.IRemotePropertyHolder#setProperty(java.lang.String, java.lang.Object)
	 */
	public void setProperty(String key, Object value) {
		properties.put(key, value);
		propertyStates.put(key, Boolean.TRUE);
	}

	public String getComment()
	{
		return ""; //$NON-NLS-1$
	}

	/*
	 *  (non-Javadoc)
	 * @see ISchedulingRule#contains(ISchedulingRule)
	 */
	public boolean contains(ISchedulingRule rule) {
		if (this == rule)
			return true;
		if (rule instanceof MultiRule) {
			MultiRule multi = (MultiRule) rule;
			ISchedulingRule[] children = multi.getChildren();
			for (int i = 0; i < children.length; i++)
				if (!contains(children[i]))
					return false;
			return true;
		}
		if (rule instanceof IResource)
			return true;
		if (rule instanceof IRemoteFile)
		{
			RemoteFile rf = (RemoteFile)rule;
			return getHostName().equals(rf.getHostName()) &&
		       rf.getAbsolutePath().startsWith(getAbsolutePath());
		}
		//if (!(rule instanceof RemoteFileSchedulingRule))
		//	return false;
		if (rule instanceof RemoteFileSchedulingRule)
		{
		return getHostName().equals(((RemoteFileSchedulingRule) rule).getHostName()) &&
		       ((RemoteFileSchedulingRule) rule).getAbsolutePath().startsWith(getAbsolutePath());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see ISchedulingRule#isConflicting(ISchedulingRule)
	 */
	public boolean isConflicting(ISchedulingRule rule)
	{
		if (rule instanceof RemoteFile)
		{
			String otherPath = ((RemoteFile)rule).getAbsolutePath();
			String path = this.getAbsolutePath();
			String otherHost = ((RemoteFile)rule).getHostName();
			return getHostName().equals(otherHost) && path.startsWith(otherPath) || otherPath.startsWith(path);
		}
		else if (rule instanceof RemoteFileSchedulingRule)
		{
			String otherPath = ((RemoteFileSchedulingRule) rule).getAbsolutePath();
			String path = this.getAbsolutePath();
			String otherHost = ((RemoteFileSchedulingRule) rule).getHostName();
			return getHostName().equals(otherHost) && path.startsWith(otherPath) || otherPath.startsWith(path);
		}
		else
		{
			return false;
		}
	}

	public boolean equals(Object otherObj)
	{
		if (otherObj instanceof RemoteFile)
		{
			RemoteFile other = (RemoteFile)otherObj;
			if  (this == other) return true;

			String otherPath = other.getAbsolutePath();
			String path = this.getAbsolutePath();
			//String otherHost = other.getHostName();

			IRemoteFileSubSystem ss1 = this.getParentRemoteFileSubSystem();
			IRemoteFileSubSystem ss2 = other.getParentRemoteFileSubSystem();
			
			String alias1 = ""; //$NON-NLS-1$
			if (ss1 != null){
				alias1 = ss1.getHostAliasName();
			}
			
			String alias2 = "";
			if (ss2 != null){
				alias2 = ss2.getHostAliasName();
			}
			
			return alias1.equals(alias2) && path.equals(otherPath);
		}
		else
		{
			return false;
		}
	}

	public String getHostName()
	{
		return getHost().getHostName();
	}



	private String getParentPathFor(String path)
	{
		String separator = getSeparator();

		if (path.equals(separator))
		{
			return null; // no parent of root
		}

		int lastSep = path.lastIndexOf(separator);

		if (lastSep == 0) // root is the parent (on unix)
		{
			return separator;
		}
		else if (path.equals("\\")){ //$NON-NLS-1$
			return null; // for mapped network folder, there may not be a drive
		}
		else if (lastSep > 0)
		{
			return path.substring(0, lastSep);
		}
		else
		{
			return null;
		}
	}

	/**
	 * Returns the encoding of the remote file. If a user specified value does not exist, then we check
	 * it's ancestry for an encoding.  Otherwise the encoding of the parent subsystem is returned.
	 * @see IRemoteFile#getEncoding()
	 */
	public String getEncoding() {
		String hostName = getParentRemoteFileSubSystem().getHost().getHostName();
		String path = getAbsolutePath();
		String encoding = RemoteFileEncodingManager.getInstance().getEncoding(hostName, path);

		// ask the parent folder
		if (encoding == null) {
			if (_parentFile != null)
			{
				encoding = _parentFile.getEncoding();
			}
			else
			{

				// manually extra parents
				String parentPath = getParentPathFor(path);
				while (parentPath != null && encoding == null)
				{
					encoding = RemoteFileEncodingManager.getInstance().getEncoding(hostName, parentPath);
					parentPath = getParentPathFor(parentPath);
				}

				if (encoding == null) // no encoding found - fall back to system
				{
					encoding = getParentRemoteFileSubSystem().getRemoteEncoding();
				}
			}
		}

		return encoding;
	}
	/**
	 * Sets the encoding of the remote file. It sets the encoding of the remote file in {@link RemoteFileEncodingManager}.
	 * @param encoding the encoding to be set for the remote file.
	 */
	public void setEncoding(String encoding) {
		RemoteFileEncodingManager.getInstance().setEncoding(getHostName(), getAbsolutePath(), encoding);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.0
	 */
	public IHostFilePermissions getPermissions() {
		IHostFile hostFile = getHostFile();
		if (hostFile instanceof IHostFilePermissionsContainer){
			return ((IHostFilePermissionsContainer)hostFile).getPermissions();
		}
		return null;
	}
}
