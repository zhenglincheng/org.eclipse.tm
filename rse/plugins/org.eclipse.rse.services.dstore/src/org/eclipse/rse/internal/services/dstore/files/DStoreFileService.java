/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 * Kevin Doyle (IBM) - Fix 183870 - Display File Exists Error
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Xuan Chen        (IBM)        - [189681] [dstore][linux] Refresh Folder in My Home messes up Refresh in Root
 * Kushal Munir (IBM) - [189352] Replace with appropriate line end character on upload
 * David McKnight   (IBM)        - [190803] Canceling a long-running dstore job prints "InterruptedException" to stdout
 * David McKnight   (IBM)        - [196035] Wrapper SystemMessageExceptions for createFile and createFolder with RemoteFileSecurityException
 * Kevin Doyle 		(IBM)		 - [191548] Deleting Read-Only directory removes it from view and displays no error
 * Xuan Chen        (IBM)        - [202670] [Supertransfer] After doing a copy to a directory that contains folders some folders name's display "deleted"
 * Xuan Chen        (IBM)        - [190824] Incorrect result for DStore#getSeparator() function when parent is "/"
 * David McKnight   (IBM)        - [207095] check for null datastore
 * David McKnight   (IBM)        - [207178] changing list APIs for file service and subsystems
 * David McKnight   (IBM)        - [162195] new APIs for upload multi and download multi
 * David McKnight   (IBM)        - [209423] Fix for null pointer - filter attributes need unique ids
 * David McKnight   (IBM)        - [209552] API changes to use multiple and getting rid of deprecated
 * David McKnight   (IBM)        - [210109] store constants in IFileService rather than IFileServiceConstants
 * David McKnight   (IBM)        - [210812] for text transfer, need to honour the preference (instead of straight binary)
 * David McKnight   (IBM)        - [209704] [api] Ability to override default encoding conversion needed.
 * Kevin Doyle		(IBM)		 - [208778] [efs][api] RSEFileStore#getOutputStream() does not support EFS#APPEND
 * David McKnight   (IBM)        - [196624] dstore miner IDs should be String constants rather than dynamic lookup
 * David McKnight   (IBM)        - [209704] added supportsEncodingConversion()
 * Xuan Chen        (IBM)        - [209827] Update DStore command implementation to enable cancelation of archive operations
 * David McKnight   (IBM)        - [209593] [api] add support for "file permissions" and "owner" properties for unix files
 * David McKnight   (IBM)        - [216252] MessageFormat.format -> NLS.bind
 * David McKnight   (IBM)        - [216252] use SimpleSystemMessage instead of getMessage()
 * David McKnight   (IBM)        - [220547] [api][breaking] SimpleSystemMessage needs to specify a message id and some messages should be shared
 * Radoslav Gerganov (ProSyst)   - [216195] [dstore] Saving empty file fails
 * David McKnight    (IBM)       - [220379] [api] Provide a means for contributing custom BIDI encodings
 * David McKnight    (IBM)       - [225573] [dstore] client not falling back to single operation when missing batch descriptors (due to old server)
 * Martin Oberhuber (Wind River) - [226262] Make IService IAdaptable
 * David McKnight     (IBM)      - [227406][api][dstore] need apis for getting buffer size in IDataStoreProvider
 * David McKnight     (IBM)      - [229610] [api] File transfers should use workspace text file encoding
 * David McKnight     (IBM)      - [221211] [api][breaking][files] need batch operations to indicate which operations were successful
 *******************************************************************************/

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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.dstore.core.model.DE;
import org.eclipse.dstore.core.model.DataElement;
import org.eclipse.dstore.core.model.DataStore;
import org.eclipse.dstore.core.model.DataStoreAttributes;
import org.eclipse.dstore.core.model.DataStoreResources;
import org.eclipse.dstore.core.model.IDataStoreProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.dstore.universal.miners.IUniversalDataStoreConstants;
import org.eclipse.rse.dstore.universal.miners.UniversalByteStreamHandler;
import org.eclipse.rse.internal.services.dstore.Activator;
import org.eclipse.rse.internal.services.dstore.IDStoreMessageIds;
import org.eclipse.rse.internal.services.dstore.ServiceResources;
import org.eclipse.rse.services.clientserver.FileTypeMatcher;
import org.eclipse.rse.services.clientserver.IMatcher;
import org.eclipse.rse.services.clientserver.IServiceConstants;
import org.eclipse.rse.services.clientserver.ISystemFileTypes;
import org.eclipse.rse.services.clientserver.NamePatternMatcher;
import org.eclipse.rse.services.clientserver.PathUtility;
import org.eclipse.rse.services.clientserver.SystemEncodingUtil;
import org.eclipse.rse.services.clientserver.archiveutils.ArchiveHandlerManager;
import org.eclipse.rse.services.clientserver.messages.CommonMessages;
import org.eclipse.rse.services.clientserver.messages.ICommonMessageIds;
import org.eclipse.rse.services.clientserver.messages.SimpleSystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.dstore.AbstractDStoreService;
import org.eclipse.rse.services.dstore.util.DownloadListener;
import org.eclipse.rse.services.dstore.util.FileSystemMessageUtil;
import org.eclipse.rse.services.files.CodePageConverterManager;
import org.eclipse.rse.services.files.HostFilePermissions;
import org.eclipse.rse.services.files.IFilePermissionsService;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.files.IFileServiceCodePageConverter;
import org.eclipse.rse.services.files.IHostFile;
import org.eclipse.rse.services.files.IHostFilePermissions;
import org.eclipse.rse.services.files.IHostFilePermissionsContainer;
import org.eclipse.rse.services.files.PendingHostFilePermissions;
import org.eclipse.rse.services.files.RemoteFileCancelledException;
import org.eclipse.rse.services.files.RemoteFileIOException;
import org.eclipse.rse.services.files.RemoteFileSecurityException;

public class DStoreFileService extends AbstractDStoreService implements IFileService, IFilePermissionsService
{

	protected org.eclipse.dstore.core.model.DataElement _uploadLogElement = null;
	protected Map _fileElementMap;
	protected Map _dstoreFileMap;
	
	private int _bufferUploadSize = IUniversalDataStoreConstants.BUFFER_SIZE;
	private int _bufferDownloadSize = IUniversalDataStoreConstants.BUFFER_SIZE;
	
	protected ISystemFileTypes _fileTypeRegistry;
	private String remoteEncoding;


	protected boolean unixStyle = false;

	private static String[] _filterAttributes =  {
		"attributes",  //$NON-NLS-1$
		"filter", //$NON-NLS-1$
		"filter.id", //$NON-NLS-1$
		"doc", //$NON-NLS-1$
		"", //$NON-NLS-1$
		"", //$NON-NLS-1$
		DataStoreResources.FALSE,
		"2"}; //$NON-NLS-1$

	public DStoreFileService(IDataStoreProvider dataStoreProvider, ISystemFileTypes fileTypeRegistry)
	{
		super(dataStoreProvider);
		_fileElementMap = new HashMap();
		_dstoreFileMap = new HashMap();
		_fileTypeRegistry = fileTypeRegistry;
	}

	public void uninitService(IProgressMonitor monitor)
	{
		_fileElementMap.clear();
		_dstoreFileMap.clear();
		_uploadLogElement = null;
		super.uninitService(monitor);
	}

	public String getName()
	{
		return ServiceResources.DStore_File_Service_Label;
	}

	public String getDescription()
	{
		return ServiceResources.DStore_File_Service_Description;
	}

	/**
	 * Set the buffer upload size
	 * @param size the new size
	 */
	public void setBufferUploadSize(int size)
	{
		_bufferUploadSize = size;
	}

	/**
	 * Set the buffer download size
	 * @param size the new size
	 */
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
		return IUniversalDataStoreConstants.UNIVERSAL_FILESYSTEM_MINER_ID;
	}

	protected String getByteStreamHandlerId()
	{
		return UniversalByteStreamHandler.class.getName();
	}

	protected String getDataStoreRoot()
	{
		DataStore ds = getDataStore();
		if (ds != null)
			return ds.getAttribute(DataStoreAttributes.A_LOCAL_PATH);
		return null;
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
		DataStore ds = getDataStore();
		if (ds != null)
			ds.setAttribute(DataStoreAttributes.A_LOCAL_PATH, root);
	}

	protected DataElement findUploadLog()
	{
	    DataElement minerInfo = getMinerElement();
	    DataStore ds = getDataStore();
		if (_uploadLogElement ==  null || _uploadLogElement.getDataStore() != ds)
		{
			if (ds != null)
			{
				_uploadLogElement = ds.find(minerInfo, DE.A_NAME, "universal.uploadlog", 2); //$NON-NLS-1$
			}
			else
			{
				return null;
			}
		}
		return _uploadLogElement;
	}



	protected DataElement getAttributes(String fileNameFilter, boolean showHidden)
	{
		DataStore ds = getDataStore();
		if (ds != null)
		{
			String[] clonedAttributes = (String[])_filterAttributes.clone();
			clonedAttributes[DE.A_ID] = fileNameFilter;

			DataElement attributes = ds.createTransientObject(clonedAttributes);
			String version = IServiceConstants.VERSION_1;
			StringBuffer buffer = new StringBuffer();
			String filter = ((fileNameFilter == null) ? "*" : fileNameFilter); //$NON-NLS-1$
			buffer.append(version).append(IServiceConstants.TOKEN_SEPARATOR).append(filter).append(IServiceConstants.TOKEN_SEPARATOR).append(showHidden);
			attributes.setAttribute(DE.A_SOURCE, buffer.toString());
			return attributes;
		}
		else
		{
			return null;
		}
	}



	public boolean upload(InputStream inputStream, String remoteParent, String remoteFile, boolean isBinary,
			String hostEncoding, IProgressMonitor monitor) throws SystemMessageException
	{
		BufferedInputStream bufInputStream = null;

		boolean isCancelled = false;

		try
		{
			String byteStreamHandlerId = getByteStreamHandlerId();
			String remotePath = remoteParent + getSeparator(remoteParent) + remoteFile;

			// create an empty file on the host and append data to it later
			// this handles the case of uploading empty files as well
			getDataStore().replaceFile(remotePath, new byte[] {}, 0, isBinary, byteStreamHandlerId);

//			DataElement uploadLog = findUploadLog();
			findUploadLog();
//			listener = new FileTransferStatusListener(remotePath, shell, monitor, getConnectorService(), ds, uploadLog);
	//		ds.getDomainNotifier().addDomainListener(listener);

			int buffer_size = getBufferUploadSize();

			// read in the file
			bufInputStream = new BufferedInputStream(inputStream, buffer_size);

			byte[] buffer = new byte[buffer_size];
			byte[] convBytes;
			int numToRead = 0;

			int available = bufInputStream.available();


			// line separator of local machine
			String localLineSep = System.getProperty("line.separator"); //$NON-NLS-1$

			// line separator of remote machine
			String targetLineSep = "\n"; //$NON-NLS-1$

			if (!unixStyle) {
				targetLineSep = "\r\n"; //$NON-NLS-1$
			}

			int localLineSepLength = localLineSep.length();

			long totalSent = 0;

			// upload bytes while available
			while (available > 0 && !isCancelled)
			{


				numToRead = (available < buffer_size) ? available : buffer_size;

				int bytesRead = bufInputStream.read(buffer, 0, numToRead);

				if (bytesRead == -1)
					break;

				totalSent += bytesRead;

				if (!isBinary && hostEncoding != null)
				{
					String tempStr = new String(buffer, 0, bytesRead);

					// if the line end characters of the local and remote machines are different, we need to replace them
					if (!localLineSep.equals(targetLineSep)) {

						int index = tempStr.indexOf(localLineSep);

						StringBuffer buf = new StringBuffer();

						boolean lineEndFound = false;
						int lastIndex = 0;

						while (index != -1) {
							buf = buf.append(tempStr.substring(lastIndex, index));
							buf = buf.append(targetLineSep);

							if (!lineEndFound) {
								lineEndFound = true;
							}

							lastIndex = index+localLineSepLength;

							index = tempStr.indexOf(localLineSep, lastIndex);
						}

						if (lineEndFound) {
							buf = buf.append(tempStr.substring(lastIndex));
							tempStr = buf.toString();
						}
					}


					convBytes = tempStr.getBytes(hostEncoding);

					// append subsequent segments
					getDataStore().replaceAppendFile(remotePath, convBytes, convBytes.length, true, byteStreamHandlerId);
				}
				else // binary
				{
					// append subsequent segments
					getDataStore().replaceAppendFile(remotePath, buffer, bytesRead, true, byteStreamHandlerId);
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
			throw new RemoteFileIOException(e);
		}
		catch (UnsupportedEncodingException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
			throw new RemoteFileIOException(e);
		}
		catch (IOException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
			throw new RemoteFileIOException(e);			
		}
		catch (Exception e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
			throw new RemoteFileIOException(e);
		}
		finally
		{

			try
			{

				if (bufInputStream != null)
					bufInputStream.close();
			}
			catch (IOException e)
			{
//				UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
				throw new RemoteFileIOException(e);
			}


			if (isCancelled)
			{
				throw new RemoteFileCancelledException();
			}
		}

		return true;
	}


	public boolean upload(File file, String remoteParent, String remoteFile, boolean isBinary,
			String srcEncoding, String hostEncoding, IProgressMonitor monitor) throws SystemMessageException
	{
		FileInputStream inputStream = null;
		BufferedInputStream bufInputStream = null;


		boolean isCancelled = false;
		boolean transferSuccessful = false;

		long totalBytes = file.length();

		try
		{
			String byteStreamHandlerId = getByteStreamHandlerId();
			String remotePath = remoteParent + getSeparator(remoteParent) + remoteFile;

			// create an empty file and append data to it later
			// this handles the case of uploading empty files as well
			getDataStore().replaceFile(remotePath, new byte[] {}, 0, isBinary, byteStreamHandlerId);

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

			byte[] buffer = new byte[buffer_size];
			byte[] convBytes;
			int numToRead = 0;

			int available = bufInputStream.available();

			long totalSent = 0;

			// line separator of local machine
			String localLineSep = System.getProperty("line.separator"); //$NON-NLS-1$

			// line separator of remote machine
			String targetLineSep = "\n"; //$NON-NLS-1$

			if (!unixStyle) {
				targetLineSep = "\r\n"; //$NON-NLS-1$
			}

			int localLineSepLength = localLineSep.length();

			IFileServiceCodePageConverter codePageConverter = CodePageConverterManager.getCodePageConverter(hostEncoding, this);

			// upload bytes while available
			while (available > 0 && !isCancelled)
			{

				numToRead = (available < buffer_size) ? available : buffer_size;

				int bytesRead = bufInputStream.read(buffer, 0, numToRead);

				if (bytesRead == -1)
					break;

				totalSent += bytesRead;

				if (!isBinary && srcEncoding != null && hostEncoding != null)
				{
					String tempStr = new String(buffer, 0, bytesRead, srcEncoding);

					// if the line end characters of the local and remote machines are different, we need to replace them
					if (!localLineSep.equals(targetLineSep)) {

						int index = tempStr.indexOf(localLineSep);

						StringBuffer buf = new StringBuffer();

						boolean lineEndFound = false;
						int lastIndex = 0;

						while (index != -1) {
							buf = buf.append(tempStr.substring(lastIndex, index));
							buf = buf.append(targetLineSep);

							if (!lineEndFound) {
								lineEndFound = true;
							}

							lastIndex = index+localLineSepLength;

							index = tempStr.indexOf(localLineSep, lastIndex);
						}

						if (lineEndFound) {
							buf = buf.append(tempStr.substring(lastIndex));
							tempStr = buf.toString();
						}
					}


					convBytes = codePageConverter.convertClientStringToRemoteBytes(remotePath, tempStr, hostEncoding, this);

					// append subsequent segments
					getDataStore().replaceAppendFile(remotePath, convBytes, convBytes.length, true, byteStreamHandlerId);
				}
				else // binary
				{
					// append subsequent segments
					getDataStore().replaceAppendFile(remotePath, buffer, bytesRead, true, byteStreamHandlerId);
				}


				if (/*display != null &&*/ monitor != null)
				{
					double percent = (totalSent * 1.0) / totalBytes;
					monitor.worked(bytesRead);
					String str = MessageFormat.format(
						ServiceResources.DStore_Service_Percent_Complete_Message,
						new Object[] {
							new Long(totalSent / IUniversalDataStoreConstants.KB_IN_BYTES),
							new Long(totalBytes / IUniversalDataStoreConstants.KB_IN_BYTES),
							new Double(percent)
						});
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
			throw new RemoteFileIOException(e);
		}
		catch (UnsupportedEncodingException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
			throw new RemoteFileIOException(e);
		}
		catch (IOException e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
			throw new RemoteFileIOException(e);
		}
		catch (Exception e)
		{
//			UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
			throw new RemoteFileIOException(e);
		}
		finally
		{

			try
			{

				if (bufInputStream != null)
					bufInputStream.close();


			}
			catch (IOException e)
			{
//				UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error writing file " + remotePath, e);
				throw new RemoteFileIOException(e);
			}
			
			if (isCancelled)
			{
				throw new RemoteFileCancelledException();
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
		DataStore ds = getDataStore();
		DataElement universaltemp = getMinerElement();

		//int mode = isBinary ? IUniversalDataStoreConstants.BINARY_MODE : IUniversalDataStoreConstants.TEXT_MODE;
		int mode = IUniversalDataStoreConstants.BINARY_MODE;

		if (!makeSureLocalExists(localFile))
		{
			FileNotFoundException e = new FileNotFoundException();
			throw new RemoteFileIOException(e);
		}


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
//long t1 = System.currentTimeMillis();
		DataElement status = ds.command(queryCmd, argList, subject);
		if (status == null)
		{
			System.out.println("no download descriptor for "+remoteElement); //$NON-NLS-1$
		}
		try
		{
			DownloadListener dlistener = new DownloadListener(status, localFile, remotePath, fileLength, monitor);
			if (!dlistener.isDone())
			{
				try
				{
					dlistener.waitForUpdate();
				}
				
				catch (InterruptedException e)
				{
					// cancel monitor if it's still not cancelled
					if (monitor != null && !monitor.isCanceled())
					{
						monitor.setCanceled(true);
					}

					//InterruptedException is used to report user cancellation, so no need to log
					//This should be reviewed (use OperationCanceledException) with bug #190750
				}
			}
		}

		catch (Exception e)
		{
			throw new RemoteFileIOException(e);
		}

	//long t2 = System.currentTimeMillis();
//	System.out.println("time="+(t2 - t1)/1000);
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
				throw new RemoteFileIOException(e);
			}
		}

		List resultList = remoteElement.getNestedData();
		DataElement resultChild = null;

		for (int i = 0; i < resultList.size(); i++)
		{

			resultChild = (DataElement) resultList.get(i);

			if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_SUCCESS_TYPE))
			{
				if (!isBinary){ // do standard conversion if this is text!
					String localEncoding = SystemEncodingUtil.getInstance().getLocalDefaultEncoding();

					IFileServiceCodePageConverter codePageConverter = CodePageConverterManager.getCodePageConverter(encoding, this);

					codePageConverter.convertFileFromRemoteEncoding(remotePath, localFile, encoding, localEncoding, this);
				}



				return true;
			}
			else if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_FILE_NOT_FOUND_EXCEPTION))
			{
				localFile.delete();

				String msgTxt = ServiceResources.FILEMSG_SECURITY_ERROR;
				String msgDetails = NLS.bind(ServiceResources.FILEMSG_SECURITY_ERROR_DETAILS, IUniversalDataStoreConstants.DOWNLOAD_RESULT_FILE_NOT_FOUND_EXCEPTION);
				SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
						IDStoreMessageIds.FILEMSG_SECURITY_ERROR,
						IStatus.ERROR, msgTxt, msgDetails);
				throw new SystemMessageException(msg);
			}
			else if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_UNSUPPORTED_ENCODING_EXCEPTION))
			{
				//SystemMessage msg = getMessage();
				//throw new SystemMessageException(msg);
				UnsupportedEncodingException e = new UnsupportedEncodingException(resultChild.getName());
				//UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error reading file " + remotePath, e);
				throw new RemoteFileIOException(e);
			}

			else if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_IO_EXCEPTION))
			{
				localFile.delete();

				String msgTxt = ServiceResources.FILEMSG_SECURITY_ERROR;
				String msgDetails = NLS.bind(ServiceResources.FILEMSG_SECURITY_ERROR_DETAILS, IUniversalDataStoreConstants.DOWNLOAD_RESULT_IO_EXCEPTION);
				SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
						IDStoreMessageIds.FILEMSG_SECURITY_ERROR,
						IStatus.ERROR, msgTxt, msgDetails);

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

	private boolean makeSureLocalExists(File localFile)
	{
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
			return false;
		}
		return true;
	}

	/**
	 * Default implementation - just iterate through each file
	 */
	public boolean downloadMultiple(String[] remoteParents, String[] remoteFiles,
			File[] localFiles, boolean[] isBinaries, String[] hostEncodings,
			IProgressMonitor monitor) throws SystemMessageException
	{
		boolean result = true;


		List downloadListeners = new ArrayList();
		List remoteElements = new ArrayList();

		DataStore ds = getDataStore();
		DataElement universaltemp = getMinerElement();

		// get the subjects
		String[] paths = getPathsFor(remoteParents, remoteFiles);
		DataElement[] des = getElementsFor(paths);

		DataElement queryCmd = null;
		DataElement bufferSizeElement = null;

		// if any elements are unresolved, do a query on them
		List unresolved = new ArrayList();
		for (int d = 0; d < des.length; d++)
		{
			DataElement de = des[d];
			if (de.getType().equals(IUniversalDataStoreConstants.UNIVERSAL_FILTER_DESCRIPTOR))
			{
				unresolved.add(de);
			}
		}
		// query the unresolved
		if (!unresolved.isEmpty())
		{
			String[] parents = new String[unresolved.size()];
			String[] names = new String[unresolved.size()];
			for (int u = 0; u < unresolved.size(); u++)
			{
				DataElement de = (DataElement)unresolved.get(u);
				parents[u] = de.getValue();
				names[u] = de.getName();
			}

			// I think the de should be reused since getElement should find it?
			getFileMultiple(parents, names, monitor);
		}


		// kick off all downloads
		for (int i = 0; i < des.length && result == true; i++)
		{
			int mode = IUniversalDataStoreConstants.BINARY_MODE;
			DataElement de = des[i];
			String remotePath = paths[i];

			File localFile = localFiles[i];
			String hostEncoding = hostEncodings[i];

			if (!makeSureLocalExists(localFile))
			{
				FileNotFoundException e = new FileNotFoundException();
				throw new RemoteFileIOException(e);
			}

			long fileLength = DStoreHostFile.getFileLength(de.getSource());
			if (monitor != null)
			{
				monitor.beginTask(remotePath, (int)fileLength);
			}

			DataElement remoteElement = ds.createObject(universaltemp, de.getType(), remotePath, String.valueOf(mode));
			DataElement localElement = ds.createObject(universaltemp, de.getType(), localFile.getAbsolutePath(), hostEncoding);

			// only do this once
			if (bufferSizeElement == null)
				bufferSizeElement = ds.createObject(universaltemp, "buffer_size", "" + getBufferDownloadSize(), ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			// only do this once
			if (queryCmd == null)
				queryCmd = getCommandDescriptor(de,IUniversalDataStoreConstants.C_DOWNLOAD_FILE);


			ArrayList argList = new ArrayList();
			argList.add(remoteElement);
			argList.add(localElement);
			argList.add(bufferSizeElement);

			DataElement subject = ds.createObject(universaltemp, de.getType(), remotePath, String.valueOf(mode));

			DataElement status = ds.command(queryCmd, argList, subject);

			DownloadListener dlistener = new DownloadListener(status, localFile, remotePath, fileLength, monitor);
			downloadListeners.add(dlistener);
			remoteElements.add(remoteElement);
		}

		// all downloads have been started
		// now wait for each to complete
		for (int j = 0; j < downloadListeners.size(); j++)
		{
			DownloadListener dlistener = (DownloadListener)downloadListeners.get(j);
			try
			{
				if (!dlistener.isDone())
				{
					try
					{
						dlistener.waitForUpdate();
					}
					catch (InterruptedException e)
					{
						// cancel monitor if it's still not cancelled
						if (monitor != null && !monitor.isCanceled())
						{
							monitor.setCanceled(true);
						}

						//InterruptedException is used to report user cancellation, so no need to log
						//This should be reviewed (use OperationCanceledException) with bug #190750
					}
				}
			}
			catch (Exception e)
			{
				throw new RemoteFileIOException(e);
			}

			// now wait till we have all the bytes local
			File localFile = localFiles[j];
			long localBytes = localFile.length();
			long lastLocalBytes = 0;
			long fileLength = dlistener.getTotalLength();
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
					throw new RemoteFileIOException(e);
				}
			}

			DataElement remoteElement = (DataElement)remoteElements.get(j);
			List resultList = remoteElement.getNestedData();
			DataElement resultChild = null;

			if (resultList != null)
			{
				for (int i = 0; i < resultList.size(); i++)
				{

					resultChild = (DataElement) resultList.get(i);

					if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_SUCCESS_TYPE))
					{
						// do standard conversion if this is text!
						if (!isBinaries[i]){ // do standard conversion if this is text!
							String localEncoding = SystemEncodingUtil.getInstance().getLocalDefaultEncoding();
							IFileServiceCodePageConverter codePageConverter = CodePageConverterManager.getCodePageConverter(hostEncodings[i], this);

							codePageConverter.convertFileFromRemoteEncoding(remoteElement.getName(), localFile, hostEncodings[i], localEncoding, this);
						}

						result = true;
					}
					else if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_FILE_NOT_FOUND_EXCEPTION))
					{
						localFile.delete();

						String msgTxt = ServiceResources.FILEMSG_SECURITY_ERROR;
						String msgDetails = NLS.bind(ServiceResources.FILEMSG_SECURITY_ERROR_DETAILS, IUniversalDataStoreConstants.DOWNLOAD_RESULT_FILE_NOT_FOUND_EXCEPTION);
						SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
								IDStoreMessageIds.FILEMSG_SECURITY_ERROR,
								IStatus.ERROR, msgTxt, msgDetails);
						throw new SystemMessageException(msg);
					}
					else if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_UNSUPPORTED_ENCODING_EXCEPTION))
					{
						//SystemMessage msg = getMessage();
						//throw new SystemMessageException(msg);
						//UnsupportedEncodingException e = new UnsupportedEncodingException(resultChild.getName());
						//UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error reading file " + remotePath, e);
						//throw new RemoteFileIOException(e);
						result = false;
					}

					else if (resultChild.getType().equals(IUniversalDataStoreConstants.DOWNLOAD_RESULT_IO_EXCEPTION))
					{
						localFile.delete();
						String msgTxt = ServiceResources.FILEMSG_SECURITY_ERROR;
						String msgDetails = NLS.bind(ServiceResources.FILEMSG_SECURITY_ERROR_DETAILS, IUniversalDataStoreConstants.DOWNLOAD_RESULT_IO_EXCEPTION);
						SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
								IDStoreMessageIds.FILEMSG_SECURITY_ERROR,
								IStatus.ERROR, msgTxt, msgDetails);
						throw new SystemMessageException(msg);
						//IOException e = new IOException(resultChild.getName());
						//UniversalSystemPlugin.logError(CLASSNAME + "." + "copy: " + "error reading file " + remotePath, e);
						//throw new RemoteFileIOException(e);
					}
					else
					{
						result = false;
					}
				}

				if (monitor != null)
				{
					//monitor.done();
				}
			}
		}
		return result;
	}

	/**
	 * Default implementation - just iterate through each file
	 */
	public boolean uploadMultiple(File[] localFiles, String[] remoteParents,
			String[] remoteFiles, boolean[] isBinaries, String[] srcEncodings,
			String[] hostEncodings, IProgressMonitor monitor)
			throws SystemMessageException
	{
		boolean result = true;
		for (int i = 0; i < localFiles.length && result == true; i++)
		{
			File localFile = localFiles[i];
			String remoteParent = remoteParents[i];
			String remoteFile = remoteFiles[i];

			boolean isBinary = isBinaries[i];
			String srcEncoding = srcEncodings[i];
			String hostEncoding = hostEncodings[i];
			result = upload(localFile, remoteParent, remoteFile, isBinary, srcEncoding, hostEncoding, monitor);
		}
		return result;
	}

	private DataElement getSubjectFor(String remoteParent, String name)
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
		return de;
	}

	private DataElement[] getSubjectsFor(String[] remoteParents, String[] names)
	{
		List subjects = new ArrayList();
		for (int i = 0; i < remoteParents.length; i++)
		{
			DataElement de = getSubjectFor(remoteParents[i], names[i]);
			subjects.add(de);
		}
		return (DataElement[])subjects.toArray(new DataElement[subjects.size()]);
	}

	public IHostFile getFile(String remoteParent, String name, IProgressMonitor monitor)
	{
		DataElement de = getSubjectFor(remoteParent, name);

		// with 207095, it's possible to get here unconnected such that there is no element
		if (de != null) {
			dsQueryCommand(de, null,  IUniversalDataStoreConstants.C_QUERY_GET_REMOTE_OBJECT, monitor);
			//getFile call should also need to convert this DataElement into a HostFile using
			//convertToHostFile() call.  This way, this DataElement will be put into _fileMap.
			return convertToHostFile(de);
		}
		else {
			return null;
		}
	}

	/**
	 * Mass query of individual files
	 */
	public IHostFile[] getFileMultiple(String remoteParents[], String names[], IProgressMonitor monitor)
		throws SystemMessageException
	{
		DataElement[] subjects = getSubjectsFor(remoteParents, names);

		// construct default array of commands
		String[] queryStrings = new String[remoteParents.length];
		for (int i = 0; i < queryStrings.length; i++)
		{
			queryStrings[i] = IUniversalDataStoreConstants.C_QUERY_GET_REMOTE_OBJECT;
		}

		dsQueryCommandMulti(subjects, null, queryStrings, monitor);

		return convertToHostFiles(subjects, "*");		 //$NON-NLS-1$
	}

	/**
	 * Returns what the next part of the path should be, given the current
	 * path as parentPath. Returns different separators based on whether the path
	 * appears to be a windows, linux, or virtual path.
	 * Pass in null to just get the default separator.
	 */
	protected String getSeparator(String parentPath)
	{
		if (parentPath == null || parentPath.length() < 1) return "/"; //$NON-NLS-1$
		if (parentPath.length() == 1)
		{
			//deal with the case where parentPath has only one character here
			//since the code below assumes parentPath has at least two characters.
			if (parentPath.charAt(0) == '/')
			{
				return "";  //$NON-NLS-1$
			}
			else
			{
				//If only one character, but not '/', just return "/" as default.  But this should not happen.
				return "/"; //$NON-NLS-1$
			}
		}

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
			if (element != null && !element.isDeleted() && element.getType() != null)
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

		if (null != monitor && monitor.isCanceled())
		{
			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
					ICommonMessageIds.MSG_OPERATION_CANCELLED,
					IStatus.CANCEL, CommonMessages.MSG_OPERATION_CANCELLED);
			//This operation has been cancelled by the user.
			throw new SystemMessageException(msg);
		}

		if (FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.SUCCESS))
			return new DStoreHostFile(de);
		else if (FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.FAILED_WITH_EXIST))
		{
			String msgTxt = ServiceResources.FILEMSG_CREATE_FILE_FAILED_EXIST;
			String msgDetails = NLS.bind(ServiceResources.FILEMSG_CREATE_FILE_FAILED_EXIST_DETAILS, remotePath);
			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
					IDStoreMessageIds.FILEMSG_CREATE_FILE_FAILED_EXIST,
					IStatus.ERROR, msgTxt, msgDetails);
			throw new SystemMessageException(msg);
		}
		else
		{
			// for 196035 - throwing security exception instead of message exception
			String msgTxt = ServiceResources.FILEMSG_CREATE_FILE_FAILED;
			String msgDetails = ServiceResources.FILEMSG_CREATE_FILE_FAILED_DETAILS;
			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
					IDStoreMessageIds.FILEMSG_CREATE_FILE_FAILED,
					IStatus.ERROR, msgTxt, msgDetails);


			Exception e= new SystemMessageException(msg);
			RemoteFileSecurityException messageException = new RemoteFileSecurityException(e);
			 throw messageException;
		}
	}

	public IHostFile createFolder(String remoteParent, String folderName, IProgressMonitor monitor) throws SystemMessageException
	{
		String remotePath = remoteParent + getSeparator(remoteParent) + folderName;
		DataElement de = getElementFor(remotePath);

		DataElement status = dsStatusCommand(de, IUniversalDataStoreConstants.C_CREATE_FOLDER, monitor);

		if (status == null) return null;

		if (null != monitor && monitor.isCanceled())
		{
			//This operation has been cancelled by the user.
			throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
					ICommonMessageIds.MSG_OPERATION_CANCELLED,
					IStatus.CANCEL,
					CommonMessages.MSG_OPERATION_CANCELLED));
		}

		if (FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.SUCCESS))
			return new DStoreHostFile(de);
		else if(FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.FAILED_WITH_EXIST))
		{
			String msgTxt = ServiceResources.FILEMSG_CREATE_FOLDER_FAILED_EXIST;
			String msgDetails = NLS.bind(ServiceResources.FILEMSG_CREATE_FOLDER_FAILED_EXIST_DETAILS, remotePath);
			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
					IDStoreMessageIds.FILEMSG_CREATE_FOLDER_FAILED_EXIST,
					IStatus.ERROR, msgTxt, msgDetails);
			throw new SystemMessageException(msg);
		}
		else
		{
			String msgTxt = ServiceResources.FILEMSG_CREATE_FILE_FAILED;
			String msgDetails = NLS.bind(ServiceResources.FILEMSG_CREATE_FILE_FAILED_DETAILS, remotePath);
			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
					IDStoreMessageIds.FILEMSG_CREATE_FILE_FAILED,
					IStatus.ERROR, msgTxt, msgDetails);

			// for 196035 - throwing security exception instead of message exception
			Exception e= new SystemMessageException(msg);
			RemoteFileSecurityException messageException = new RemoteFileSecurityException(e);
			 throw messageException;
			//throw new SystemMessageException(getMessage("RSEF1304").makeSubstitution(remotePath)); //$NON-NLS-1$
		}

	}

	public boolean delete(String remoteParent, String fileName, IProgressMonitor monitor) throws SystemMessageException
	{
		String remotePath = remoteParent + getSeparator(remoteParent) + fileName;
		DataElement de = getElementFor(remotePath);
		DataElement status = dsStatusCommand(de, IUniversalDataStoreConstants.C_DELETE, monitor);
		if (status == null) 
			throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
					ICommonMessageIds.MSG_ERROR_UNEXPECTED,
					IStatus.ERROR,
					CommonMessages.MSG_ERROR_UNEXPECTED));

		if (null != monitor && monitor.isCanceled())
		{
			//This operation has been cancelled by the user.
			throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
					ICommonMessageIds.MSG_OPERATION_CANCELLED,
					IStatus.CANCEL,
					CommonMessages.MSG_OPERATION_CANCELLED));
		}
		String sourceMsg = FileSystemMessageUtil.getSourceMessage(status);
		// When running a server older than 2.0.1 success is not set for directories, so we must
		// check if the source message is an empty string
		if (sourceMsg.equals(IServiceConstants.SUCCESS) || sourceMsg.equals("")) { //$NON-NLS-1$
			return true;
		} else {
			String msgTxt = NLS.bind(ServiceResources.FILEMSG_DELETE_FILE_FAILED, FileSystemMessageUtil.getSourceLocation(status));
			String msgDetails = ServiceResources.FILEMSG_DELETE_FILE_FAILED_DETAILS;
			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
					IDStoreMessageIds.FILEMSG_DELETE_FILE_FAILED,
					IStatus.ERROR, msgTxt, msgDetails);

			throw new SystemMessageException(msg);
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
		if (status != null)
		{
			if (null != monitor && monitor.isCanceled())
			{
				//This operation has been cancelled by the user.
				throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
						ICommonMessageIds.MSG_OPERATION_CANCELLED,
						IStatus.CANCEL, CommonMessages.MSG_OPERATION_CANCELLED));
			}
			String sourceMsg = FileSystemMessageUtil.getSourceMessage(status);
			// When running a server older than 2.0.1 success is not set for directories, so we must
			// check if the source message is an empty string
			if (sourceMsg.equals(IServiceConstants.SUCCESS) || sourceMsg.equals("")) { //$NON-NLS-1$
				return true;
			} else {
				String msgTxt = NLS.bind(ServiceResources.FILEMSG_DELETE_FILE_FAILED, FileSystemMessageUtil.getSourceLocation(status));
				String msgDetails = ServiceResources.FILEMSG_DELETE_FILE_FAILED_DETAILS;
				SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
						IDStoreMessageIds.FILEMSG_DELETE_FILE_FAILED,
						IStatus.ERROR, msgTxt, msgDetails);

				throw new SystemMessageException(msg);
			}
		}
		else {
			// no delete batch descriptor so need to fall back to single command approach
			boolean result = true;
			for (int i = 0; i < remoteParents.length && result; i++){
				String parent = remoteParents[i];
				String name = fileNames[i];
				result = delete(parent, name, monitor);
			}
			return result;

		}
	}

	public boolean rename(String remoteParent, String oldName, String newName, IProgressMonitor monitor) throws SystemMessageException
	{
		String oldPath, newPath = null;
 		// if remoteParent is null or empty then we are doing a move
 		if (remoteParent == null || remoteParent == "") //$NON-NLS-1$
 		{
 			oldPath = oldName;
 		 	newPath = newName;
 		}
 		else
 		{
 			 oldPath = remoteParent + getSeparator(remoteParent) + oldName;
 			 newPath = remoteParent + getSeparator(remoteParent) + newName;
 		}

 		DataElement de = getElementFor(oldPath);


		// new servers use the full path
 		de.setAttribute(DE.A_SOURCE, newPath);


		DataElement status = dsStatusCommand(de, IUniversalDataStoreConstants.C_RENAME, monitor);

		if (status != null && status.getAttribute(DE.A_SOURCE).equals("failed"))
		{
			// in the patch for bug 196211, a change was made to the UniversalFileSystemMiner that
			// had the rename expecting the full path in the A_SOURCE attribute itself
			// prior to that, we constructed the target file name from the value and the source
			// I must have missed that while reviewing

			// this is our attempt at recovering, but doing the operation with the old
			// format.  Old servers only used the name for the source attribute on C_RENAME
			de.setAttribute(DE.A_SOURCE, newName);

			// trying again
			status = dsStatusCommand(de, IUniversalDataStoreConstants.C_RENAME, monitor);
		}


		if (status == null)
			throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
					ICommonMessageIds.MSG_ERROR_UNEXPECTED,
					IStatus.ERROR,
					CommonMessages.MSG_ERROR_UNEXPECTED));
		
		if (null != monitor && monitor.isCanceled())
		{
			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
					ICommonMessageIds.MSG_OPERATION_CANCELLED,
					IStatus.CANCEL, CommonMessages.MSG_OPERATION_CANCELLED);
			//This operation has been cancelled by the user.
			throw new SystemMessageException(msg);
		}
		if (FileSystemMessageUtil.getSourceMessage(status).equals(IServiceConstants.SUCCESS))
			return true;
		else
		{
			String msgTxt = NLS.bind(ServiceResources.FILEMSG_RENAME_FILE_FAILED, FileSystemMessageUtil.getSourceLocation(status));
			String msgDetails = ServiceResources.FILEMSG_RENAME_FILE_FAILED_DETAILS;
			SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
					IDStoreMessageIds.FILEMSG_RENAME_FILE_FAILED,
					IStatus.ERROR, msgTxt, msgDetails);

			throw new SystemMessageException(msg);
		}
	}

	public boolean rename(String remoteParent, String oldName, String newName, IHostFile oldFile, IProgressMonitor monitor) throws SystemMessageException
	{
		boolean retVal = rename(remoteParent, oldName, newName, monitor);
		String newPath = remoteParent + getSeparator(remoteParent) + newName;
		oldFile.renameTo(newPath);
		return retVal;
	}

	protected boolean moveByCopy(String srcParent, String srcName, String tgtParent, String tgtName, IProgressMonitor monitor) throws SystemMessageException
	{
		boolean movedOk = false;

		if (copy(srcParent, srcName, tgtParent, tgtName, monitor))
		{
			try
			{
				movedOk = delete(srcParent, srcName, monitor);
			}
			catch (SystemMessageException exc)
			{
				if (null != monitor && monitor.isCanceled())
				{
					//This mean the copy operation is ok, but delete operation has been cancelled by user.
					//The delete() call will take care of recovered from the cancel operation.
					//So we need to make sure to remove the already copied file/folder.
					getFile(tgtParent, tgtName, null); //need to call getFile first to put this object into DataElement map first
					                                   //otherwise it type will default to FilterObject, and could not be deleted properly for virtual object.
					delete(tgtParent, tgtName, null);
				}
				throw exc;
			}
		}

	 	return movedOk;
	}

	public boolean move(String srcParent, String srcName, String tgtParent, String tgtName, IProgressMonitor monitor) throws SystemMessageException
	{
		String src = srcParent + getSeparator(srcParent) + srcName;
		String tgt = tgtParent + getSeparator(tgtParent) + tgtName;
		boolean isVirtual = ArchiveHandlerManager.isVirtual(src) || ArchiveHandlerManager.isVirtual(tgt);
		boolean isArchive = ArchiveHandlerManager.getInstance().isRegisteredArchive(tgt);
		if (isVirtual || isArchive)
		{
			return moveByCopy(srcParent, srcName, tgtParent, tgtName, monitor);
		}
		else
		{
			boolean movedOk = false;
			try
			{
				movedOk = rename("", src, tgt, monitor); //$NON-NLS-1$
			}
			catch (SystemMessageException e)
			{
				return moveByCopy(srcParent, srcName, tgtParent, tgtName, monitor);
			}
			// movedOk should never be false otherwise the last DataElement status was null
			if (!movedOk)
			{
				movedOk = moveByCopy(srcParent, srcName, tgtParent, tgtName, monitor);
			}
			return movedOk;
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

					String msgTxt = NLS.bind(ServiceResources.FILEMSG_COPY_FILE_FAILED, srcName);
					String msgDetails = ServiceResources.FILEMSG_COPY_FILE_FAILED_DETAILS;
					SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
							IDStoreMessageIds.FILEMSG_COPY_FILE_FAILED,
							IStatus.ERROR, msgTxt, msgDetails);

					throw new SystemMessageException(msg);
				}
			}
			catch (InterruptedException e)
			{
				if (monitor != null && monitor.isCanceled())
				{
					//This operation has been cancelled by the user.
					throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
							ICommonMessageIds.MSG_OPERATION_CANCELLED,
							IStatus.CANCEL, CommonMessages.MSG_OPERATION_CANCELLED));
				}
				// cancel monitor if it's still not cancelled
				if (monitor != null && !monitor.isCanceled())
				{
					monitor.setCanceled(true);
				}
			}
			return true;
		}
		else {
			throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
					ICommonMessageIds.MSG_ERROR_UNEXPECTED,
					IStatus.ERROR,
					CommonMessages.MSG_ERROR_UNEXPECTED));
		}
	}

	public boolean copyBatch(String[] srcParents, String[] srcNames, String tgtParent, IProgressMonitor monitor) throws SystemMessageException
	{
		DataStore ds = getDataStore();

		DataElement tgtDE = getElementFor(tgtParent);
		if (tgtDE.getType().equals(IUniversalDataStoreConstants.UNIVERSAL_FILTER_DESCRIPTOR))
		{
			// get the property queried object
			dsQueryCommand(tgtDE, IUniversalDataStoreConstants.C_QUERY_GET_REMOTE_OBJECT, monitor);
		}
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

					String msgTxt = NLS.bind(ServiceResources.FILEMSG_COPY_FILE_FAILED, srcNames[0]);
					String msgDetails = ServiceResources.FILEMSG_COPY_FILE_FAILED_DETAILS;
					SystemMessage msg = new SimpleSystemMessage(Activator.PLUGIN_ID,
							IDStoreMessageIds.FILEMSG_COPY_FILE_FAILED,
							IStatus.ERROR, msgTxt, msgDetails);

					throw new SystemMessageException(msg);
				}
			}
			catch (InterruptedException e)
			{
				if (monitor != null && monitor.isCanceled())
				{
					//This operation has been cancelled by the user.
					throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
							ICommonMessageIds.MSG_OPERATION_CANCELLED,
							IStatus.CANCEL, CommonMessages.MSG_OPERATION_CANCELLED));
				}
				// cancel monitor if it's still not cancelled
				if (monitor != null && !monitor.isCanceled())
				{
					monitor.setCanceled(true);
				}

				//InterruptedException is used to report user cancellation, so no need to log
				//This should be reviewed (use OperationCanceledException) with bug #190750
			}
			return true;
		}
		else {
			// no copy batch descriptor so need to fall back to single command approach
			boolean result = true;
			for (int i = 0; i < srcParents.length && result; i++){
				String parent = srcParents[i];
				String name = srcNames[i];
				result = copy(parent, name, tgtParent, name, monitor);
			}
			return result;

		}
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

	private String getQueryString(int fileType)
	{
		String queryString = null;
		switch (fileType)
		{
		case IFileService.FILE_TYPE_FILES:
			queryString = IUniversalDataStoreConstants.C_QUERY_VIEW_FILES;
			break;

		case IFileService.FILE_TYPE_FOLDERS:
			queryString = IUniversalDataStoreConstants.C_QUERY_VIEW_FOLDERS;
		break;

		case IFileService.FILE_TYPE_FILES_AND_FOLDERS:
		default:
			queryString = IUniversalDataStoreConstants.C_QUERY_VIEW_ALL;
			break;
		}
		return queryString;
	}

	private String[] getQueryStrings(int[] fileTypes)
	{
		String[] queryStrings = new String[fileTypes.length];
		for (int i = 0; i < fileTypes.length; i++)
		{
			switch (fileTypes[i])
			{
			case IFileService.FILE_TYPE_FILES:
				queryStrings[i] = IUniversalDataStoreConstants.C_QUERY_VIEW_FILES;
				break;

			case IFileService.FILE_TYPE_FOLDERS:
				queryStrings[i] = IUniversalDataStoreConstants.C_QUERY_VIEW_FOLDERS;
			break;

			case IFileService.FILE_TYPE_FILES_AND_FOLDERS:
			default:
				queryStrings[i] = IUniversalDataStoreConstants.C_QUERY_VIEW_ALL;
				break;
			}
		}
		return queryStrings;
	}

	public IHostFile[] list(String remoteParent, String fileFilter, int fileType, IProgressMonitor monitor)
	{
		String queryString = getQueryString(fileType);
		return fetch(remoteParent, fileFilter, queryString, monitor);
	}


	public IHostFile[] listMultiple(String[] remoteParents,
			String[] fileFilters, int[] fileTypes, IProgressMonitor monitor)
			throws SystemMessageException
	{
		String[] queryStrings = getQueryStrings(fileTypes);

		return fetchMulti(remoteParents, fileFilters, queryStrings, monitor);
	}

	public IHostFile[] listMultiple(String[] remoteParents,
			String[] fileFilters, int fileType, IProgressMonitor monitor)
			throws SystemMessageException
	{
		String queryString = getQueryString(fileType);

		// create array of the same query string
		String[] queryStrings = new String[remoteParents.length];
		for (int i = 0; i < remoteParents.length; i++)
		{
			queryStrings[i] = queryString;
		}

		return fetchMulti(remoteParents, fileFilters, queryStrings, monitor);
	}

	protected String[] getPathsFor(String[] remoteParents, String[] remoteFiles)
	{
		String[] results = new String[remoteParents.length];
		String sep = null;
		for (int i = 0; i < remoteParents.length; i++)
		{
			String remoteParent = remoteParents[i];
			String remoteFile = remoteFiles[i];
			if (sep == null)
			{
				sep = getSeparator(remoteParent);
			}

			results[i] = remoteParent + sep + remoteFile;
		}
		return results;
	}


	protected DataElement[] getElementsFor(String[] paths)
	{
		DataElement[] results = new DataElement[paths.length];
		for (int i = 0; i < paths.length; i++)
		{
			results[i] = getElementFor(paths[i]);
		}
		return results;
	}

	protected DataElement getElementFor(String path)
	{
		if (!isInitialized())
		{
			waitForInitialize(null);
		}

		DataStore ds = getDataStore();

		// with 207095, it's possible to get here when disconnected and no dstore
		if (ds == null){
			return null;
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
			element = ds.createObject(universaltemp, IUniversalDataStoreConstants.UNIVERSAL_FILTER_DESCRIPTOR, normalizedPath, normalizedPath, "", false); //$NON-NLS-1$
		}
		return element;
	}


	/**
	 * Get a dstore IHostFile object for the given absolute path, provided
	 * that the file object has been accessed before and is available in our
	 * file map.
	 * @param path absolute file path identifying the remote object
	 * @return Requested file object or <code>null</code> if there isn't one mapped right now
	 */
	public IHostFile getHostFile(String path)
	{
		return (IHostFile)_dstoreFileMap.get(path);
	}

	protected IHostFile[] fetch(String remoteParent, String fileFilter, String queryType, IProgressMonitor monitor)
	{
		DataStore ds = getDataStore();
		if (ds == null)
		{
			return new IHostFile[0];
		}

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

	/**
	 * Fetch multiple results (for different parents an filters
	 *
	 * @param remoteParents the parents to query
	 * @param fileFilters the filters for each parent to query
	 * @param queryTypes the type of queries (for each parent) - files, folders, both, etc
	 * @param monitor the progress monitor
	 * @return the results
	 */
	protected IHostFile[] fetchMulti(String[] remoteParents, String[] fileFilters, String[] queryTypes, IProgressMonitor monitor)
	{
		DataStore ds = getDataStore();
		if (ds == null)
		{
			return new IHostFile[0];
		}

		ArrayList[] argses = new ArrayList[remoteParents.length];
		DataElement subjects[] = new DataElement[remoteParents.length];

		for (int i = 0; i < remoteParents.length; i++)
		{
			// create filter descriptor
			DataElement deObj = getElementFor(remoteParents[i]);
			if (deObj == null)
			{
				DataElement universaltemp = getMinerElement();
				deObj = ds.createObject(universaltemp, IUniversalDataStoreConstants.UNIVERSAL_FILTER_DESCRIPTOR, remoteParents[i], remoteParents[i], "", false); //$NON-NLS-1$
			}
			subjects[i] = deObj;

			DataElement attributes = getAttributes(fileFilters[i], true);
			ArrayList args = new ArrayList(1);
			args.add(attributes);
			argses[i] = args;
		}

		List consolidatedResults = dsQueryCommandMulti(subjects, argses, queryTypes, monitor);
		List convertedResults = new ArrayList();
		for (int r = 0; r < consolidatedResults.size(); r++)
		{
			IHostFile[] results = convertToHostFiles((DataElement[])consolidatedResults.get(r), fileFilters[r]);
			for (int c = 0; c < results.length; c++)
			{
				convertedResults.add(results[c]);
			}
		}

		return (IHostFile[])convertedResults.toArray(new IHostFile[consolidatedResults.size()]);
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
		if (ds != null)
		{
			DataElement setCmd = getCommandDescriptor(de, IUniversalDataStoreConstants.C_SET_LASTMODIFIED);
			if (setCmd != null)
			{
				// first modify the source attribute to temporarily be the date field
				de.setAttribute(DE.A_SOURCE, timestamp + "");			 //$NON-NLS-1$
				ds.command(setCmd, de, true);
				return true;
			}
		}
		throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
					ICommonMessageIds.MSG_ERROR_UNEXPECTED,
					IStatus.ERROR,
					CommonMessages.MSG_ERROR_UNEXPECTED));
	}

	public boolean setReadOnly(String parent, String name,
			boolean readOnly, IProgressMonitor monitor) throws SystemMessageException
	{
		String remotePath = parent + getSeparator(parent) + name;
		DataElement de = getElementFor(remotePath);
		DataStore ds = de.getDataStore();
		if (ds != null)
		{
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
					throw new RemoteFileIOException(e);
				}
				return true;
			}
		}
		throw new SystemMessageException(new SimpleSystemMessage(Activator.PLUGIN_ID,
				ICommonMessageIds.MSG_ERROR_UNEXPECTED,
				IStatus.ERROR,
				CommonMessages.MSG_ERROR_UNEXPECTED));
	}

	/**
	 * Queries the remote system for the platform encoding.
	 * @see org.eclipse.rse.services.files.IFileService#getEncoding(org.eclipse.core.runtime.IProgressMonitor)
	 * @since 2.0
	 */
	public String getEncoding(IProgressMonitor monitor) throws SystemMessageException {

		if (remoteEncoding == null) {

			DataStore ds = getDataStore();
			if (ds != null)
			{
				DataElement encodingElement = ds.createObject(null, IUniversalDataStoreConstants.UNIVERSAL_TEMP_DESCRIPTOR, ""); //$NON-NLS-1$

				DataElement queryCmd = ds.localDescriptorQuery(encodingElement.getDescriptor(),IUniversalDataStoreConstants.C_SYSTEM_ENCODING);

				DataElement status = ds.command(queryCmd, encodingElement, true);

				try {
					getStatusMonitor(ds).waitForUpdate(status);
				}
				catch (Exception e) {
					throw new RemoteFileIOException(e);
				}

				remoteEncoding = encodingElement.getValue();
			}
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
		int options = isBinary ? IFileService.NONE : IFileService.TEXT_MODE;
		return getOutputStream(remoteParent, remoteFile, options, monitor);
	}


	public OutputStream getOutputStream(String remoteParent, String remoteFile, int options, IProgressMonitor monitor) throws SystemMessageException {
		String remotePath = remoteParent + getSeparator(remoteParent) + remoteFile;
		int mode;

		if ((options & IFileService.TEXT_MODE) == 0)
		{
			mode = IUniversalDataStoreConstants.BINARY_MODE;
		}
		else
		{
			mode = IUniversalDataStoreConstants.TEXT_MODE;
		}

		DStoreOutputStream outputStream = new DStoreOutputStream(getDataStore(), remotePath, getEncoding(monitor), mode, unixStyle, options);
		return outputStream;
	}

	/**
	 * Sets whether this is a Unix-style file system or a Windows-style file system. The
	 * default is Windows if this is not called. The creator of this class should call this to set the type of the file system.
	 * @param isUnixStyle <code>true<code> if this is a Unix-style file system, <code>false</code> otherwise.
	 */
	public void setIsUnixStyle(boolean isUnixStyle) {
		this.unixStyle = isUnixStyle;
	}

	public boolean supportsEncodingConversion(){
		return true;
	}


	public IHostFilePermissions getFilePermissions(IHostFile rfile, IProgressMonitor monitor)
			throws SystemMessageException {
		DStoreHostFile file = (DStoreHostFile)rfile;
		IHostFilePermissions result = file.getPermissions();
		if (result == null || result instanceof PendingHostFilePermissions){
			/*
			 *  // for now, leaving this to the adapter since it needs to prevent duplicate jobs
			if (result == null) { // create a pending one
				result = new PendingHostFilePermissions();
				file.setPermissions(result);
			}
			*/

			DataElement remoteFile = file.getDataElement();

			DataElement status = dsStatusCommand(remoteFile, IUniversalDataStoreConstants.C_QUERY_FILE_PERMISSIONS, monitor);
			if (status != null) {
				int permissionsInt = 0;

				// access string in the form <octal permissions>|<user>|<group>
				String permissionsStr = status.getSource();

				if (permissionsStr != null && permissionsStr.length() > 0) {
					String[] permAttributes = permissionsStr.split("\\"+IServiceConstants.TOKEN_SEPARATOR);	 //$NON-NLS-1$

					// permissions bits
					String accessString = permAttributes[0];
					try
					{
						int accessInt = Integer.parseInt(accessString, 8);
					    permissionsInt = accessInt; // leave permissions in decimal
					}
					catch (Exception e){
						throw new RemoteFileIOException(e);
					}

					// user
					String user = permAttributes[1];

					// group
					String group = permAttributes[2];

					result = new HostFilePermissions(permissionsInt, user, group);
					file.setPermissions(result);
				}
			}
		}

		return result;
	}

	public void setFilePermissions(IHostFile file,
			IHostFilePermissions permissions, IProgressMonitor monitor)
			throws SystemMessageException {
		DataElement remoteFile = ((DStoreHostFile)file).getDataElement();

		ArrayList args = new ArrayList();
		int bits = permissions.getPermissionBits();
		String permissionsInOctal = Integer.toOctalString(bits); // from decimal to octal
		String user = permissions.getUserOwner();
		String group = permissions.getGroupOwner();

		String permissionsStr = permissionsInOctal + '|' + user + '|' + group;
		DataElement newPermissionsElement = getDataStore().createObject(null, "permissions", permissionsStr); //$NON-NLS-1$
		args.add(newPermissionsElement);

		DataElement status = dsStatusCommand(remoteFile, args, IUniversalDataStoreConstants.C_SET_FILE_PERMISSIONS, monitor);
		if (status != null)
		{
			// check status to make sure the file really changed
			if (file instanceof IHostFilePermissionsContainer){
				((IHostFilePermissionsContainer)file).setPermissions(permissions); // set to use new permissions
			}

		}
	}

	public int getCapabilities(IHostFile file) {
		int capabilities = 0;
		// dstore supports setting and getting
		if (file == null){
			capabilities = IFilePermissionsService.FS_CAN_GET_ALL | IFilePermissionsService.FS_CAN_SET_ALL;
		}
		else if (file instanceof DStoreVirtualHostFile){
			// no virtual support right now
			return capabilities;
		}
		else {

			DataElement remoteFile = ((DStoreHostFile)file).getDataElement();
			DataElement getCmd = getCommandDescriptor(remoteFile, IUniversalDataStoreConstants.C_QUERY_FILE_PERMISSIONS);
			DataElement setCmd = getCommandDescriptor(remoteFile, IUniversalDataStoreConstants.C_SET_FILE_PERMISSIONS);

			if (getCmd != null){
				capabilities = capabilities | IFilePermissionsService.FS_CAN_GET_ALL;
			}
			if (setCmd != null){
				capabilities = capabilities | IFilePermissionsService.FS_CAN_SET_ALL;
			}
		}
		return capabilities;
	}



}
