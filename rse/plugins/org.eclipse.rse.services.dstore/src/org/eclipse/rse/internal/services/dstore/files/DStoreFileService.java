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
 * Kevin Doyle (IBM) - Fix 183870 - Display File Exists Error
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Xuan Chen        (IBM)        - [189681] [dstore][linux] Refresh Folder in My Home messes up Refresh in Root
 ********************************************************************************/

package org.eclipse.rse.internal.services.dstore.files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dstore.core.model.DE;
import org.eclipse.dstore.core.model.DataElement;
import org.eclipse.dstore.core.model.DataStore;
import org.eclipse.dstore.core.model.DataStoreAttributes;
import org.eclipse.dstore.core.model.DataStoreResources;
import org.eclipse.dstore.core.model.IDataStoreProvider;
import org.eclipse.rse.dstore.universal.miners.IUniversalDataStoreConstants;
import org.eclipse.rse.dstore.universal.miners.UniversalByteStreamHandler;
import org.eclipse.rse.dstore.universal.miners.UniversalFileSystemMiner;
import org.eclipse.rse.internal.services.dstore.ServiceResources;
import org.eclipse.rse.services.clientserver.FileTypeMatcher;
import org.eclipse.rse.services.clientserver.IMatcher;
import org.eclipse.rse.services.clientserver.IServiceConstants;
import org.eclipse.rse.services.clientserver.ISystemFileTypes;
import org.eclipse.rse.services.clientserver.NamePatternMatcher;
import org.eclipse.rse.services.clientserver.PathUtility;
import org.eclipse.rse.services.clientserver.archiveutils.ArchiveHandlerManager;
import org.eclipse.rse.services.clientserver.messages.ISystemMessageProvider;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.dstore.AbstractDStoreService;
import org.eclipse.rse.services.dstore.util.DownloadListener;
import org.eclipse.rse.services.dstore.util.FileSystemMessageUtil;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.files.IHostFile;

public class DStoreFileService extends AbstractDStoreService implements IFileService
{

	protected org.eclipse.dstore.core.model.DataElement _uploadLogElement = null;
	protected Map _fileElementMap;
	protected Map _dstoreFileMap;
	
	private int _bufferUploadSize = IUniversalDataStoreConstants.BUFFER_SIZE;
	private int _bufferDownloadSize = IUniversalDataStoreConstants.BUFFER_SIZE;
	protected ISystemFileTypes _fileTypeRegistry;
	private String remoteEncoding;
	
	private static String _percentMsg = SystemMessage.sub(SystemMessage.sub(SystemMessage.sub(ServiceResources.DStore_Service_Percent_Complete_Message, "&0", "{0}"), "&1", "{1}"), "&2", "{2}");	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

	private static String[] _filterAttributes =  {
		"attributes",  //$NON-NLS-1$
		"filter", //$NON-NLS-1$
		"filter.id", //$NON-NLS-1$
		"doc", //$NON-NLS-1$
		"", //$NON-NLS-1$
		"", //$NON-NLS-1$
		DataStoreResources.FALSE,
		"2"}; //$NON-NLS-1$
	
	public DStoreFileService(IDataStoreProvider dataStoreProvider, ISystemFileTypes fileTypeRegistry, ISystemMessageProvider msgProvider)
	{
		super(dataStoreProvider, msgProvider);
		_fileElementMap = new HashMap();
		_dstoreFileMap = new HashMap();
		_fileTypeRegistry = fileTypeRegistry;
	}
	
	public void uninitService(IProgressMonitor monitor)
	{
		super.uninitService(monitor);
		_fileElementMap.clear();
		_dstoreFileMap.clear();
	}
	
	public String getName()
	{
		return ServiceResources.DStore_File_Service_Label;
	}
	
	public String getDescription()
	{
		return ServiceResources.DStore_File_Service_Description;
	}
	
	public void setBufferUploadSize(int size)
	{
		_bufferUploadSize = size;
	}
	
	public void setBufferDownloadSize(int size)
	{
		_bufferDownloadSize = size;
	}
	
	protected int getBufferUploadSize()
	{
		return _bufferUploadSize;
	}
	
	protected int getBufferDownloadSize()
	{
		return _bufferDownloadSize;
	}
	
	protected String getMinerId()
	{
		return UniversalFileSystemMiner.MINER_ID;
	}
	
	protected String getByteStreamHandlerId()
	{
		return UniversalByteStreamHandler.class.getName();
	}
	
	protected String getDataStoreRoot()
	{
		return getDataStore().getAttribute(DataStoreAttributes.A_LOCAL_PATH);
	}
	

	protected String prepareForDownload(String localPath)
	{
		int index = localPath.lastIndexOf(File.separator);
		String parentDir = localPath.substring(0, index + 1);

		// change local root for datastore so that the file is downloaded
		// at the specified location
		setDataStoreRoot(parentDir);

		String dataStoreLocalPath = localPath.substring(index + 1);

		if (!dataStoreLocalPath.startsWith("/")) //$NON-NLS-1$
			dataStoreLocalPath = "/" + dataStoreLocalPath; //$NON-NLS-1$

		return dataStoreLocalPath;
	}

	protected void setDataStoreRoot(String root)
	{
		getDataStore().setAttribute(DataStoreAttributes.A_LOCAL_PATH, root);
	}
	
	protected DataElement findUploadLog()
	{
	    DataElement minerInfo = getMinerElement();
		if (_uploadLogElement ==  null || _uploadLogElement.getDataStore() != getDataStore())
		{
		    _uploadLogElement = getDataStore().find(minerInfo, DE.A_NAME, "universal.uploadlog", 2); //$NON-NLS-1$
		}
		return _uploadLogElement;
	}
	
	protected DataElement getAttributes(String fileNameFilter, boolean showHidden)
	{
		DataElement attributes = getDataStore().createTransientObject(_filterAttributes);
		String version = IServiceConstants.VERSION_1;
		StringBuffer buffer = new StringBuffer();
		String filter = ((fileNameFilter == null) ? "*" : fileNameFilter); //$NON-NLS-1$
		buffer.append(version).append(IServiceConstants.TOKEN_SEPARATOR).append(filter).append(IServiceConstants.TOKEN_SEPARATOR).append(showHidden);
		attributes.setAttribute(DE.A_SOURCE, buffer.toString());
		return attributes;
	}

	

	public boolean upload(InputStream inputStream, String remoteParent, String remoteFile, boolean isBinary,
			String hostEncoding, IProgressMonitor monitor)
	{
		BufferedInputStream bufInputStream = null;

	
		boolean isCancelled = false;

	
	
		try
		{	
			
//			DataElement uploadLog = findUploadLog();
			findUploadLog();
//			listener = new FileTransferStatusListener(remotePath, shell, monitor, getConnectorService(), ds, uploadLog);
	//		ds.getDomainNotifier().addDomainListener(listener);

			int buffer_size = getBufferUploadSize();
			
			// read in the file
			bufInputStream = new BufferedInputStream(inputStream, buffer_size);

			boolean first = true;
			byte[] buffer = new byte[buffer_size];
			byte[] convBytes;
			int numToRead = 0;

			int available = bufInputStream.available();

			long totalSent = 0;

			// upload bytes while available
			while (available > 0 && !isCancelled)
			{
	
				
				numToRead = (available < buffer_size) ? available : buffer_size;

				int bytesRead = bufInputStream.read(buffer, 0, numToRead);

				if (bytesRead == -1)
					break;
					
				totalSent += bytesRead;

				String byteStreamHandlerId = getByteStreamHandlerId();
				String remotePath = remoteParent + getSeparator(remoteParent) + remoteFile;
				
				if (!isBinary && hostEncoding != null) 
				{
					String tempStr = new String(buffer, 0, bytesRead);

					// hack for zOS - \r causes problems for compilers
//					if (osName != null && (osName.startsWith("z") || osName.equalsIgnoreCase("aix")))
//					{
//						tempStr = tempStr.replace('\r', ' ');
//					}

					convBytes = tempStr.getBytes(hostEncoding);
				
					if (first)
					{ // send first set of bytes
						first = false;
						getDataStore().replaceFile(remotePath, convBytes, convBytes.length, true, byteStreamHandlerId);
					}
					else
					{ // append subsequent segments
						getDataStore().replaceAppendFile(remotePath, convBytes, convBytes.length, true, byteStreamHandlerId);
					}
				}
				else // binary
				{
					if (first)
					{ // send first set of bytes
						first = false;
						getDataStore().replaceFile(remotePath, buffer, bytesRead, true, byteStreamHandlerId);
					}
					else
					{ // append subsequent segments
						getDataStore().replaceAppendFile(remotePath, buffer, bytesRead, true, byteStreamHandlerId);
					}
				}			
		
				
				if (monitor != null)
				{

					isCancelled = monitor.isCanceled();

				}

				available = bufInputStream.available();
			}
//			if (listener.uploadHasFailed())
//			{
//				showUploadFailedMessage(listener, source);
//			}
//			else
			{
		//	    transferSuccessful = true;
			}
		}

		catch (FileNotFoundException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//			throw new RemoteFileIOException(e);
			return false;
		}
		catch (UnsupportedEncodingException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//			throw new RemoteFileIOException(e);
			return false;
		}
		catch (IOException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//			throw new RemoteFileIOException(e);
			return false;
		}
		catch (Exception e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//			throw new RemoteFileIOException(e);
			return false;
		}
		finally
		{

			try
			{

				if (bufInputStream != null)
					bufInputStream.close();

				if (isCancelled)
				{
					return false;
					//throw new RemoteFileCancelledException();
				}
			}
			catch (IOException e)
			{
//				UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//				throw new RemoteFileIOException(e);
				return false;
			}

		}
		
		return true;
	}

	public boolean upload(File file, String remoteParent, String remoteFile, boolean isBinary,
			String srcEncoding, String hostEncoding, IProgressMonitor monitor)
	{
		FileInputStream inputStream = null;
		BufferedInputStream bufInputStream = null;

	
		boolean isCancelled = false;
		boolean transferSuccessful = false;

		long totalBytes = file.length();
	
		try
		{	
			// if the file is empty, create new empty file on host
			if (totalBytes == 0)
			{
				IHostFile created = createFile(remoteParent, remoteFile, monitor);
				return created.exists();
			}
		
			if (monitor != null)
			{
				monitor.setTaskName(file.getName());
				//subMonitor = new SubProgressMonitor(monitor, (int)totalBytes);
			}

			
//			DataElement uploadLog = findUploadLog();
			findUploadLog();
//			listener = new FileTransferStatusListener(remotePath, shell, monitor, getConnectorService(), ds, uploadLog);
	//		ds.getDomainNotifier().addDomainListener(listener);

			int buffer_size = getBufferUploadSize();
			
			// read in the file
			inputStream = new FileInputStream(file);
			bufInputStream = new BufferedInputStream(inputStream, buffer_size);

			boolean first = true;
			byte[] buffer = new byte[buffer_size];
			byte[] convBytes;
			int numToRead = 0;

			int available = bufInputStream.available();

			long totalSent = 0;

			// upload bytes while available
			while (available > 0 && !isCancelled)
			{
				
				numToRead = (available < buffer_size) ? available : buffer_size;

				int bytesRead = bufInputStream.read(buffer, 0, numToRead);

				if (bytesRead == -1)
					break;
					
				totalSent += bytesRead;

				String byteStreamHandlerId = getByteStreamHandlerId();
				String remotePath = remoteParent + getSeparator(remoteParent) + remoteFile;
				
				if (!isBinary && srcEncoding != null && hostEncoding != null) 
				{
					String tempStr = new String(buffer, 0, bytesRead, srcEncoding);

					// hack for zOS - \r causes problems for compilers
//					if (osName != null && (osName.startsWith("z") || osName.equalsIgnoreCase("aix")))
//					{
//						tempStr = tempStr.replace('\r', ' ');
//					}

					convBytes = tempStr.getBytes(hostEncoding);
				
					if (first)
					{ // send first set of bytes
						first = false;
						getDataStore().replaceFile(remotePath, convBytes, convBytes.length, true, byteStreamHandlerId);
					}
					else
					{ // append subsequent segments
						getDataStore().replaceAppendFile(remotePath, convBytes, convBytes.length, true, byteStreamHandlerId);
					}
				}
				else // binary
				{
					if (first)
					{ // send first set of bytes
						first = false;
						getDataStore().replaceFile(remotePath, buffer, bytesRead, true, byteStreamHandlerId);
					}
					else
					{ // append subsequent segments
						getDataStore().replaceAppendFile(remotePath, buffer, bytesRead, true, byteStreamHandlerId);
					}
				}			
		
				
				if (/*display != null &&*/ monitor != null)
				{
					long percent = (totalSent * 100) / totalBytes;

			
					StringBuffer totalSentBuf = new StringBuffer();
					totalSentBuf.append((totalSent / IUniversalDataStoreConstants.KB_IN_BYTES));
					totalSentBuf.append(" KB"); //$NON-NLS-1$
					
					StringBuffer totalBuf = new StringBuffer();
					totalBuf.append(totalBytes / IUniversalDataStoreConstants.KB_IN_BYTES);
					totalBuf.append(" KB"); //$NON-NLS-1$
					
					StringBuffer percentBuf = new StringBuffer();
					percentBuf.append(percent);
					percentBuf.append("%"); //$NON-NLS-1$
								
					monitor.worked(bytesRead);
					
					String str = MessageFormat.format(_percentMsg, new Object[] {totalSentBuf, totalBuf, percentBuf});
					monitor.subTask(str);					
	



					isCancelled = monitor.isCanceled();
				}

				available = bufInputStream.available();
			}
	//		if (listener.uploadHasFailed())
		//	{
		//		showUploadFailedMessage(listener, source);
		//	}
		//	else
			{
			    transferSuccessful = true;
			}
		}

		catch (FileNotFoundException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//			throw new RemoteFileIOException(e);
			return false;
		}
		catch (UnsupportedEncodingException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//			throw new RemoteFileIOException(e);
			return false;
		}
		catch (IOException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//			throw new RemoteFileIOException(e);
			return false;
		}
		catch (Exception e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//			throw new RemoteFileIOException(e);
			return false;
		}
		finally
		{

			try
			{

				if (bufInputStream != null)
					bufInputStream.close();

				if (isCancelled)
				{
					return false;
					//throw new RemoteFileCancelledException();
				}
			}
			catch (IOException e)
			{
//				UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
//				throw new RemoteFileIOException(e);
				return false;
			}

			if (totalBytes > 0)
			{
			    if (transferSuccessful)
			    {
	

//					try
//					{
//						listener.waitForUpdate(null, 2);
//
//					}
//					catch (InterruptedException e)
//					{
//						UniversalSystemPlugin.logError(CLASSNAME + " InterruptedException while waiting for command", e);
//					}
					
			    }
			
				//ds.getDomainNotifier().removeDomainListener(listener);

//				if (listener.uploadHasFailed())
//				{
//					showUploadFailedMessage(listener, source);
//				}
			}
		}
		
		return true;
	}


	public boolean download(String remoteParent, String remoteFile, File localFile, boolean isBinary,
			String encoding, IProgressMonitor monitor) throws SystemMessageException
	{
		DataElement universaltemp = getMinerElement();

		if (!localFile.exists())
		{
			File parentDir = localFile.getParentFile();
			parentDir.mkdirs();
		}

		try
		{
			if (localFile.exists())
				localFile.delete();
			localFile.createNewFile();
		}
		catch (IOException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error creating local file " + destination, e);
//			throw new RemoteFileIOException(e);
			return false;
		}

/*		int mode;

		if (isBinary)
		{
			mode = IUniversalDataStoreConstants.BINARY_MODE;
		}
		else
		{
			mode = IUniversalDataStoreConstants.TEXT_MODE;
		}*/

		// just download as binary since we do not have to convert to UTF-8 anyway
		// the miner does a binary download anyway
		int mode = IUniversalDataStoreConstants.BINARY_MODE;
		
		DataStore ds = getDataStore();
		String remotePath = remoteParent + getSeparator(remoteParent) + remoteFile;
		
		DataElement de = getElementFor(remotePath);
		if (de.getType().equals(IUniversalDataStoreConstants.UNIVERSAL_FILTER_DESCRIPTOR))
		{
			// need to refetch
			DStoreHostFile hostFile = (DStoreHostFile)getFile(remoteParent, remoteFile, monitor);
			de = hostFile._element;
		}
		long fileLength = DStoreHostFile.getFileLength(de.getSource());
		if (monitor != null)
		{
			monitor.beginTask(remotePath, (int)fileLength);
		}
		
		
		DataElement remoteElement = ds.createObject(universaltemp, de.getType(), remotePath, String.valueOf(mode));					
		DataElement localElement = ds.createObject(universaltemp, de.getType(), localFile.getAbsolutePath(), encoding);
		
		DataElement bufferSizeElement = ds.createObject(universaltemp, "buffer_size", "" + getBufferDownloadSize(), ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		DataElement queryCmd = getCommandDescriptor(de,IUniversalDataStoreConstants.C_DOWNLOAD_FILE);

		ArrayList argList = new ArrayList();
		argList.add(remoteElement);
		argList.add(localElement);
		argList.add(bufferSizeElement);
		
		DataElement subject = ds.createObject(universaltemp, de.getType(), remotePath, String.valueOf(mode));
		
		DataElement status = ds.command(queryCmd, argList, subject);
		if (status == null)
		{
			System.out.println("no download descriptor for "+remoteElement); //$NON-NLS-1$
		}
		try
		{
			DownloadListener dlistener = new DownloadListener(status, localFile, remotePath, fileLength, monitor);
			try
			{
				dlistener.waitForUpdate();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			//getStatusMonitor(ds).waitForUpdate(status, monitor);
		}
		catch (Exception e)
		{
			return false;
		}

		// now wait till we have all the bytes local
		long localBytes = localFile.length();
		long lastLocalBytes = 0;
		while (localBytes < fileLength && (monitor == null || !monitor.isCanceled()) && lastLocalBytes != localBytes)
		{
			try
			{
				lastLocalBytes= localBytes;
				Thread.sleep(100);
				localBytes = localFile.length();
				
			}
			catch (Exception e)
			{				
			}
		}
		
		List resultList = remoteElement.getNestedData();
		DataElement resultChild = null;

		for (int i = 0; i < resultList.size(); i++)
		{

			resultChild = (DataElement) resultList.get(i);

			if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_SUCCESS_TYPE))
			{
				return true;
			}
			else if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_FILE_NOT_FOUND_EXCEPTION))
			{
				localFile.delete();
				SystemMessage msg = getMessage("RSEF1001").makeSubstitution(IUniversalDataStoreConstants.DOWNLOAD_RESULT_FILE_NOT_FOUND_EXCEPTION); //$NON-NLS-1$
				throw new SystemMessageException(msg);
			}
			else if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_UNSUPPORTED_ENCODING_EXCEPTION))
			{
				//SystemMessage msg = getMessage();
				//throw new SystemMessageException(msg);
				//UnsupportedEncodingException e = new UnsupportedEncodingException(resultChild.getName());
				//UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error reading file " + remotePath, e);
				//throw new RemoteFileIOException(e);
			}
			
			else if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_IO_EXCEPTION))
			{
				localFile.delete();
				SystemMessage msg = getMessage("RSEF1001").makeSubstitution(IUniversalDataStoreConstants.DOWNLOAD_RESULT_IO_EXCEPTION); //$NON-NLS-1$
				throw new SystemMessageException(msg);
				//IOException e = new IOException(resultChild.getName());
				//UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error reading file " + remotePath, e);
				//throw new RemoteFileIOException(e);
			}
		}

		if (monitor != null)
		{
			//monitor.done();
		}
		return true;
	}

	public IHostFile getFile(String remoteParent, String name, IProgressMonitor monitor)
	{
		DataElement de = null;
		if (name.equals(".") && name.equals(remoteParent)) //$NON-NLS-1$
		{
			de = getElementFor(name);
		}
		else
		{
			StringBuffer buf = new StringBuffer(remoteParent);
			String sep = getSeparator(remoteParent);
			if (sep.length()>0 && !remoteParent.endsWith(sep)) {
			    buf.append(sep);
			}
			buf.append(name);
			de = getElementFor(buf.toString());
		}
		dsQueryCommand(de, IUniversalDataStoreConstants.C_QUERY_GET_REMOTE_OBJECT, monitor);
		return new DStoreHostFile(de);
	}

	/**
	 * Returns what the next part of the path should be, given the current
	 * path as parentPath. Returns different separators based on whether the path
	 * appears to be a windows, linux, or virtual path.
	 * Pass in null to just get the default separator.
	 */
	protected String getSeparator(String parentPath)
	{
		if (parentPath == null || parentPath.length() < 2) return "/"; //$NON-NLS-1$
		if (parentPath.endsWith(ArchiveHandlerManager.VIRTUAL_SEPARATOR))
			return ""; //$NON-NLS-1$
		if (parentPath.endsWith(ArchiveHandlerManager.VIRTUAL_CANONICAL_SEPARATOR))
			return "/"; //$NON-NLS-1$
		if (parentPath.charAt(1) == ':') //Windows path
			if (parentPath.indexOf(ArchiveHandlerManager.VIRTUAL_CANONICAL_SEPARATOR) != -1)
				if (parentPath.endsWith("/")) //$NON-NLS-1$
					return ""; //already ends in separator //$NON-NLS-1$
				else return "/"; //$NON-NLS-1$
			else if (ArchiveHandlerManager.getInstance().isArchive(new File(parentPath)))
				return ArchiveHandlerManager.VIRTUAL_SEPARATOR;
			else
				if (parentPath.endsWith("\\")) //$NON-NLS-1$
					return ""; //already ends in separator //$NON-NLS-1$
				else return "\\"; //$NON-NLS-1$
		else if (parentPath.charAt(0) == '/') //UNIX path
			if (ArchiveHandlerManager.getInstance().isArchive(new File(parentPath)))
				return ArchiveHandlerManager.VIRTUAL_SEPARATOR;
			else
				if (parentPath.endsWith("/")) //$NON-NLS-1$
					return ""; //already ends in separator //$NON-NLS-1$
				else return "/"; //$NON-NLS-1$
		else return "/"; //unrecognized path //$NON-NLS-1$
	}
	
	protected IHostFile convertToHostFile(DataElement element)
	{
		String type = element.getType();
		IHostFile file = null;
		if (type.equals(IUniversalDataStoreConstants.UNIVERSAL_VIRTUAL_FILE_DESCRIPTOR) ||
				type.equals(IUniversalDataStoreConstants.UNIVERSAL_VIRTUAL_FOLDER_DESCRIPTOR))
		{
			file = new DStoreVirtualHostFile(element);
		}						
		else
		{
			file = new DStoreHostFile(element);
		}
		String path =  file.getAbsolutePath();
		_fileElementMap.put(path, element);
		_dstoreFileMap.put(path, file);
		return file;
	}
	protected IHostFile[] convertToHostFiles(DataElement[] elements, String fileFilter)
	{
		IMatcher filematcher = null;
		if (fileFilter.endsWith(",")) { //$NON-NLS-1$
			String[] types = fileFilter.split(","); //$NON-NLS-1$
			filematcher = new FileTypeMatcher(types, true);
		} else {
			filematcher = new NamePatternMatcher(fileFilter, true, true);
		}
		ArrayList results = new ArrayList(elements.length);
		for (int i = 0; i < elements.length; i++)
		{
			DataElement element = elements[i];
			if (!element.isDeleted())
			{				
				String type = element.getType();
				// filter files
				if (type.equals(IUniversalDataStoreConstants.UNIVERSAL_FILE_DESCRIPTOR) || type.equals(IUniversalDataStoreConstants.UNIVERSAL_VIRTUAL_FILE_DESCRIPTOR))
				{
					if (filematcher.matches(element.getName()))
					{
						results.add(convertToHostFile(element));
					}
				}
				else
				{
					results.add(convertToHostFile(element));
				}
			}
		}
		return (IHostFile[]) results.toArray(new IHostFile[results.size()]);
	}
	


	public IHostFile getUserHome()
	{
		return getFile(".", ".",null); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IHostFile createFile(String remoteParent, String fileName, IProgressMonitor monitor) throws SystemMessageException
	{
		String remotePath = remoteParent + getSeparator(remoteParent) + fileName;
		DataElement de = getElementFor(remotePath);
		
		
		DataElement status = dsStatusCommand(de, IUniversalDataStoreConstants.C_CREATE_FILE, monitor);

		if (status == null) return null;
		if (FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.SUCCESS)) 
			return new DStoreHostFile(de);
		else if (FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.FAILED_WITH_EXIST))
		{
			throw new SystemMessageException(getMessage("RSEF1303").makeSubstitution(remotePath)); //$NON-NLS-1$
		}
		else
		{
			throw new SystemMessageException(getMessage("RSEF1302").makeSubstitution(remotePath)); //$NON-NLS-1$
		}	
	}

	public IHostFile createFolder(String remoteParent, String folderName, IProgressMonitor monitor) throws SystemMessageException
	{
		String remotePath = remoteParent + getSeparator(remoteParent) + folderName;
		DataElement de = getElementFor(remotePath);
		
		DataElement status = dsStatusCommand(de, IUniversalDataStoreConstants.C_CREATE_FOLDER, monitor);

		if (status == null) return null;
		if (FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.SUCCESS)) 
			return new DStoreHostFile(de);
		else if(FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.FAILED_WITH_EXIST))
		{
			throw new SystemMessageException(getMessage("RSEF1309").makeSubstitution(remotePath)); //$NON-NLS-1$
		}
		else
		{
			throw new SystemMessageException(getMessage("RSEF1304").makeSubstitution(remotePath)); //$NON-NLS-1$
		}	

	}

	public boolean delete(String remoteParent, String fileName, IProgressMonitor monitor) throws SystemMessageException
	{
		String remotePath = remoteParent + getSeparator(remoteParent) + fileName;
		DataElement de = getElementFor(remotePath);
		DataElement status = dsStatusCommand(de, IUniversalDataStoreConstants.C_DELETE, monitor);
		if (status == null) return false;
		if (de.getType().equals(IUniversalDataStoreConstants.UNIVERSAL_FILE_DESCRIPTOR))
		{
			if (FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.SUCCESS)) return true;
			else throw new SystemMessageException(getMessage("RSEF1300").makeSubstitution(FileSystemMessageUtil.getSourceLocation(status)));	 //$NON-NLS-1$
		}
		else
		{
			return true;
		}

	}
	
	public boolean deleteBatch(String[] remoteParents, String[] fileNames, IProgressMonitor monitor) throws SystemMessageException
	{
		if (remoteParents.length == 1) return delete(remoteParents[0], fileNames[0], monitor);
		
		ArrayList dataElements = new ArrayList(remoteParents.length);
		for (int i = 0; i < remoteParents.length; i++)
		{
			String remotePath = remoteParents[i] + getSeparator(remoteParents[i]) + fileNames[i];
			DataElement de = getElementFor(remotePath);
			if (de != null) dataElements.add(de);
		}	
		DataElement status = dsStatusCommand((DataElement) dataElements.get(0), dataElements, IUniversalDataStoreConstants.C_DELETE_BATCH, monitor);
		if (status == null) return false;
		if (FileSystemMessageUtil.getSourceMessage(status).startsWith(IServiceConstants.FAILED))
			throw new SystemMessageException(getMessage("RSEF1300").makeSubstitution(FileSystemMessageUtil.getSourceLocation(status))); //$NON-NLS-1$
		else return true;
	}

	public boolean rename(String remoteParent, String oldName, String newName, IProgressMonitor monitor) throws SystemMessageException
	{
		String remotePath = remoteParent + getSeparator(remoteParent) + oldName;
		DataElement de = getElementFor(remotePath);
		de.setAttribute(DE.A_SOURCE, newName);
		
		DataElement status = dsStatusCommand(de, IUniversalDataStoreConstants.C_RENAME, monitor);

		if (status == null) return false;
		if (FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.SUCCESS)) 
			return true;
		else
		{
			throw new SystemMessageException(getMessage("RSEF1301").makeSubstitution(FileSystemMessageUtil.getSourceLocation(status))); //$NON-NLS-1$
		}	
	}
	
	public boolean rename(String remoteParent, String oldName, String newName, IHostFile oldFile, IProgressMonitor monitor) throws SystemMessageException
	{
		boolean retVal = rename(remoteParent, oldName, newName, monitor);
		String newPath = remoteParent + getSeparator(remoteParent) + newName;
		oldFile.renameTo(newPath);
		return retVal;
	}

	public boolean move(String srcParent, String srcName, String tgtParent, String tgtName, IProgressMonitor monitor) throws SystemMessageException
	{
//		String src = srcParent + getSeparator(srcParent) + srcName;
//		String tgt = tgtParent + getSeparator(tgtParent) + tgtName;
//		boolean isVirtual = ArchiveHandlerManager.isVirtual(src) || ArchiveHandlerManager.isVirtual(tgt);
		//if (isVirtual || isArchive)
		{
			if (copy(srcParent, srcName, tgtParent, tgtName, monitor))
			{
				try
				{
					delete(srcParent, srcName, monitor);
				}
				catch (Exception e)
				{
					return false;
				}
				return true;
			}
			return false;
		}

/*
		// handle special characters in source and target strings 
		StringBuffer srcBuf = new StringBuffer(src);
		StringBuffer tgtBuf = new StringBuffer(tgt);
		
		for (int i = 0; i < srcBuf.length(); i++)
		{
			char c = srcBuf.charAt(i);
			
			boolean isSpecialChar = isSpecialChar(c);
			
			if (isSpecialChar)
			{
				srcBuf.insert(i, "\\");
				i++;
			}
		}

		for (int i = 0; i < tgtBuf.length(); i++)
		{
			char c = tgtBuf.charAt(i);
			
			boolean isSpecialChar = isSpecialChar(c);
			
			if (isSpecialChar)
			{
				tgtBuf.insert(i, "\\");
				i++;
			}
		}

		src = "\"" + srcBuf.toString() + "\"";
		tgt = "\"" + tgtBuf.toString() + "\"";

		if (systemType.equals(SYSTEMTYPE_WINDOWS))
		{
			if (sourceFolderOrFile.isDirectory() && sourceFolderOrFile.getAbsolutePath().charAt(0) != targetFolder.getAbsolutePath().charAt(0))
			{
				// special case - move across drives
				command = "xcopy " + src + " " + tgt + " /S /E /K /O /Q /H /I && rmdir /S /Q " + src;
			}
			else
			{
				command = "move " + src + " " + tgt;
			}
		}
		else
		{
			command = "mv " + src + " " + tgt;
		}

		UniversalCmdSubSystemImpl cmdSubSystem = getUniversalCmdSubSystem();
		IRemoteFile runFile = sourceFolderOrFile;

		if (cmdSubSystem != null)
		{
			try
			{
				done = cmdSubSystem.runRemoteCommand(runFile, command);
				runFile.getParentRemoteFile().markStale(true);
				runFile.markStale(true);
		
			}
			catch (InterruptedException e)
			{
				done = false;
			}
		}
		else
			SystemPlugin.logWarning(CLASSNAME + " cmdSubSystem is null in move");

		return done;
		*/

	}
	
	/**
	 * Checks whether the given character is a special character in the shell. A special character is
	 * '$', '`', '"' and '\'.
	 * @param c the character to check.
	 * @return <code>true</code> if the character is a special character, <code>false</code> otherwise.
	 */
	protected boolean isSpecialChar(char c)  {
		   
		if ((c == '$') || (c == '`') || (c == '"') || (c == '\\')) {
			return true;
		}
		else {
			return false;
		}
	}

	public boolean copy(String srcParent, String srcName, String tgtParent, String tgtName, IProgressMonitor monitor) throws SystemMessageException
	{
		DataStore ds = getDataStore();
		String srcRemotePath = srcParent + getSeparator(srcParent) + srcName;
		DataElement srcDE = getElementFor(srcRemotePath);
		
		DataElement tgtDE = getElementFor(tgtParent);
		if (tgtDE.getType().equals(IUniversalDataStoreConstants.UNIVERSAL_FILTER_DESCRIPTOR))
		{
			dsQueryCommand(tgtDE, IUniversalDataStoreConstants.C_QUERY_GET_REMOTE_OBJECT, monitor);
		}
		
		DataElement cpCmd = getCommandDescriptor(tgtDE, IUniversalDataStoreConstants.C_COPY);
	
		if (cpCmd != null)
		{
			ArrayList args = new ArrayList();
			args.add(srcDE);
			DataElement nameObj = ds.createObject(null, "name", tgtName); //$NON-NLS-1$
			args.add(nameObj);
			DataElement status = ds.command(cpCmd, args, tgtDE, true);
			

			try
			{
				getStatusMonitor(ds).waitForUpdate(status, monitor);
				
				if (status.getAttribute(DE.A_SOURCE).equals(IServiceConstants.FAILED)) {

					throw new SystemMessageException(getMessage("RSEF1306").makeSubstitution(srcName)); //$NON-NLS-1$
					/*
					// for an unexpected error, we don't have an error message from the server
					if (errMsg.equals(UNEXPECTED_ERROR)) {
						msg = SystemPlugin.getPluginMessage(MSG_ERROR_UNEXPECTED).getLevelOneText();
					}
					else {
						msg = errMsg;
					}
					
					
					throw new RemoteFileIOException(new Exception(msg));
					/*/

				}
			}
			catch (InterruptedException e)
			{
//				UniversalSystemPlugin.logError(CLASSNAME + " InterruptedException while waiting for command", e);
			}
			return true;
		}
		return false;
	}

	public boolean copyBatch(String[] srcParents, String[] srcNames, String tgtParent, IProgressMonitor monitor) throws SystemMessageException
	{
		DataStore ds = getDataStore();
		
		DataElement tgtDE = getElementFor(tgtParent);
		DataElement cpCmd = getCommandDescriptor(tgtDE, IUniversalDataStoreConstants.C_COPY_BATCH);

		if (cpCmd != null)
		{
			ArrayList args = new ArrayList();
			for (int i = 0; i < srcParents.length; i++)
			{
				String srcRemotePath = srcParents[i] + getSeparator(srcParents[i]) + srcNames[i];
				DataElement srcDE = getElementFor(srcRemotePath);
				args.add(srcDE);
			}
			DataElement status = ds.command(cpCmd, args, tgtDE, true);

			try
			{
				getStatusMonitor(ds).waitForUpdate(status, monitor);
				
				if (status.getAttribute(DE.A_SOURCE).equals(IServiceConstants.FAILED)) {
					throw new SystemMessageException(getMessage("RSEF1306").makeSubstitution(srcNames[0])); //$NON-NLS-1$
				}
			}
			catch (InterruptedException e)
			{
//				UniversalSystemPlugin.logError(CLASSNAME + " InterruptedException while waiting for command", e);
			}
			return true;
		}
		return false;
	}




	
	public IHostFile[] getRoots(IProgressMonitor monitor)
	{
		if (!isInitialized())
		{
			waitForInitialize(null);
		}
		DataStore ds = getDataStore();
		DataElement universaltemp = getMinerElement();
	
		// create filter descriptor
		DataElement deObj = ds.createObject(universaltemp, IUniversalDataStoreConstants.UNIVERSAL_FILTER_DESCRIPTOR, "", "", "", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		DataElement[] results = dsQueryCommand(deObj, IUniversalDataStoreConstants.C_QUERY_ROOTS, monitor);
		
		return convertToHostFiles(results, "*"); //$NON-NLS-1$
	}
	
	

	
	public IHostFile[] getFolders(String remoteParent, String fileFilter, IProgressMonitor monitor)
	{
		return fetch(remoteParent, fileFilter, IUniversalDataStoreConstants.C_QUERY_VIEW_FOLDERS, monitor);
	}
	
	public IHostFile[] getFiles(String remoteParent, String fileFilter, IProgressMonitor monitor)
	{
		return fetch(remoteParent, fileFilter, IUniversalDataStoreConstants.C_QUERY_VIEW_FILES, monitor);
	}
	
	public IHostFile[] getFilesAndFolders(String remoteParent, String fileFilter, IProgressMonitor monitor)
	{
		return fetch(remoteParent, fileFilter, IUniversalDataStoreConstants.C_QUERY_VIEW_ALL, monitor);
	}
	
	protected DataElement getElementFor(String path)
	{
		if (!isInitialized())
		{
			waitForInitialize(null);
		}
	
		
		String normalizedPath = PathUtility.normalizeUnknown(path);
		DataElement element = (DataElement)_fileElementMap.get(normalizedPath);
		if (element != null && element.isDeleted())
		{
			_fileElementMap.remove(normalizedPath);
			element = null;
		}
		if (element == null || element.isDeleted())
		{
			DataElement universaltemp = getMinerElement();
			element = getDataStore().createObject(universaltemp, IUniversalDataStoreConstants.UNIVERSAL_FILTER_DESCRIPTOR, normalizedPath, normalizedPath, "", false); //$NON-NLS-1$
		}
		return element;
	}
	
	
	/**
	 * 
	 * @param path
	 * @return could be null if there isn't one mapped right now
	 */
	public IHostFile getHostFile(String path)
	{
		return (IHostFile)_dstoreFileMap.get(path);
	}
	
	protected IHostFile[] fetch(String remoteParent, String fileFilter, String queryType, IProgressMonitor monitor)
	{
		DataStore ds = getDataStore();
	
	
		// create filter descriptor
		DataElement deObj = getElementFor(remoteParent);
		if (deObj == null)
		{
			DataElement universaltemp = getMinerElement();
			ds.createObject(universaltemp, IUniversalDataStoreConstants.UNIVERSAL_FILTER_DESCRIPTOR, remoteParent, remoteParent, "", false); //$NON-NLS-1$
		}
		
		DataElement attributes = getAttributes(fileFilter, true);
		ArrayList args = new ArrayList(1);
		args.add(attributes);
		
		DataElement[] results = dsQueryCommand(deObj, args, queryType, monitor);		
		return convertToHostFiles(results, fileFilter);
	}

	public boolean isCaseSensitive()
	{
		return true;
	}

	public boolean setLastModified(String parent, String name,
			long timestamp, IProgressMonitor monitor) throws SystemMessageException 
	{
		String remotePath = parent + getSeparator(parent) + name;
		DataElement de = getElementFor(remotePath);
		DataStore ds = de.getDataStore();

		
		DataElement setCmd = getCommandDescriptor(de, IUniversalDataStoreConstants.C_SET_LASTMODIFIED);
		if (setCmd != null)
		{
			// first modify the source attribute to temporarily be the date field
			de.setAttribute(DE.A_SOURCE, timestamp + "");			 //$NON-NLS-1$
			ds.command(setCmd, de, true);
			return true;
		}
		
		return false;
	}

	public boolean setReadOnly(String parent, String name,
			boolean readOnly, IProgressMonitor monitor) throws SystemMessageException 
	{
		String remotePath = parent + getSeparator(parent) + name;
		DataElement de = getElementFor(remotePath);
		DataStore ds = de.getDataStore();

		
		DataElement setCmd = getCommandDescriptor(de, IUniversalDataStoreConstants.C_SET_READONLY);
		if (setCmd != null)
		{
			String flag = readOnly ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
			de.setAttribute(DE.A_SOURCE, flag);
			DataElement status = ds.command(setCmd, de, true);
			try
			{
				getStatusMonitor(ds).waitForUpdate(status);
			}
			catch (Exception e)
			{
				
			}
			return true;
		}
		return false;
	}

	/**
	 * Queries the remote system for the platform encoding.
	 * @see org.eclipse.rse.services.files.IFileService#getEncoding(org.eclipse.core.runtime.IProgressMonitor)
	 * @since 2.0
	 */
	public String getEncoding(IProgressMonitor monitor) throws SystemMessageException {
		
		if (remoteEncoding == null) {
			
			DataStore ds = getDataStore();

			DataElement encodingElement = ds.createObject(null, IUniversalDataStoreConstants.UNIVERSAL_TEMP_DESCRIPTOR, ""); //$NON-NLS-1$

			DataElement queryCmd = ds.localDescriptorQuery(encodingElement.getDescriptor(),IUniversalDataStoreConstants.C_SYSTEM_ENCODING);

			DataElement status = ds.command(queryCmd, encodingElement, true);

			try {
				getStatusMonitor(ds).waitForUpdate(status);
			}
			catch (Exception e) {
			}
			
			remoteEncoding = encodingElement.getValue();
		}

		return remoteEncoding;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#getInputStream(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String, boolean)
	 */
	public InputStream getInputStream(String remoteParent, String remoteFile, boolean isBinary, IProgressMonitor monitor) throws SystemMessageException 
	{
		String remotePath = remoteParent + getSeparator(remoteParent) + remoteFile;
		int mode;

		if (isBinary)
		{
			mode = IUniversalDataStoreConstants.BINARY_MODE;
		}
		else
		{
			mode = IUniversalDataStoreConstants.TEXT_MODE;
		}
		DStoreInputStream inputStream = new DStoreInputStream(getDataStore(), remotePath, getMinerElement(), getEncoding(monitor), mode);
		return inputStream;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#getOutputStream(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String, boolean)
	 */
	public OutputStream getOutputStream(String remoteParent, String remoteFile, boolean isBinary, IProgressMonitor monitor) throws SystemMessageException {
		String remotePath = remoteParent + getSeparator(remoteParent) + remoteFile;
		int mode;

		if (isBinary)
		{
			mode = IUniversalDataStoreConstants.BINARY_MODE;
		}
		else
		{
			mode = IUniversalDataStoreConstants.TEXT_MODE;
		}
		DStoreOutputStream outputStream = new DStoreOutputStream(getDataStore(), remotePath, getEncoding(monitor), mode);
		return outputStream;
	}
}