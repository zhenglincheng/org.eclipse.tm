/********************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others. All rights reserved.
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
 * Michael Berger (IBM) - Fixing 140408 - FTP upload does not work
 * Javier Montalvo Orus (Symbian) - Fixing 140323 - provided implementation for delete, move and rename.
 * Javier Montalvo Orus (Symbian) - Bug 140348 - FTP did not use port number
 * Michael Berger (IBM) - Fixing 140404 - FTP new file creation does not work
 * Javier Montalvo Orus (Symbian) - Migrate to apache commons net FTP client
 * Javier Montalvo Orus (Symbian) - Fixing 161211 - Cannot expand /pub folder as anonymous on ftp.wacom.com
 * Javier Montalvo Orus (Symbian) - Fixing 161238 - [ftp] expand "My Home" node on ftp.ibiblio.org as anonymous fails
 * Javier Montalvo Orus (Symbian) - Fixing 160922 - create folder/file fails for FTP service
 * David Dykstal (IBM) - Fixing 162511 - FTP file service does not process filter strings correctly
 * Javier Montalvo Orus (Symbian) - Fixing 162511 - FTP file service does not process filter strings correctly
 * Javier Montalvo Orus (Symbian) - Fixing 162782 - File filter does not display correct result in RC3
 * Javier Montalvo Orus (Symbian) - Fixing 162878 - New file and new folder dialogs don't work in FTP in a folder with subfolders
 * Javier Montalvo Orus (Symbian) - Fixing 162585 - [FTP] fetch children cannot be cancelled
 * Javier Montalvo Orus (Symbian) - Fixing 161209 - Need a Log of ftp commands
 * Javier Montalvo Orus (Symbian) - Fixing 163264 - FTP Only can not delete first subfolder
 * Michael Scharf (Wind River) - Fix 164223 - Wrong call for setting binary transfer mode
 * Martin Oberhuber (Wind River) - Add Javadoc for getFTPClient(), modify move() to use single connected session
 * Javier Montalvo Orus (Symbian) - Fixing 164009 - FTP connection shows as connected when login fails
 * Javier Montalvo Orus (Symbian) - Fixing 164306 - [ftp] FTP console shows plaintext passwords
 * Javier Montalvo Orus (Symbian) - Fixing 161238 - [ftp] connections to VMS servers are not usable
 * Javier Montalvo Orus (Symbian) - Fixing 164304 - [ftp] cannot connect to wftpd server on Windows
 * Javier Montalvo Orus (Symbian) - Fixing 165471 - [ftp] On wftpd-2.0, "." and ".." directory entries should be hidden
 * Javier Montalvo Orus (Symbian) - Fixing 165476 - [ftp] On warftpd-1.65 in MSDOS mode, cannot expand drives
 * Javier Montalvo Orus (Symbian) - Fixing 168120 - [ftp] root filter resolves to home dir
 * Javier Montalvo Orus (Symbian) - Fixing 169680 - [ftp] FTP files subsystem and service should use passive mode
 * Javier Montalvo Orus (Symbian) - Fixing 174828 - [ftp] Folders are attempted to be removed as files
 * Javier Montalvo Orus (Symbian) - Fixing 176216 - [api] FTP should provide API to allow clients register their own FTPListingParser
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Javier Montalvo Orus (Symbian) - improved autodetection of FTPListingParser
 * Javier Montalvo Orus (Symbian) - [187096] Drag&Drop + Copy&Paste shows error message on FTP connection
 * Javier Montalvo Orus (Symbian) - [187531] Improve exception thrown when Login Failed on FTP
 * Javier Montalvo Orus (Symbian) - [187862] Incorrect Error Message when creating new file in read-only directory
 * Javier Montalvo Orus (Symbian) - [194204] Renaming Files/Folders moves them sometimes
 * Javier Montalvo Orus (Symbian) - [192724] New Filter with Show Files Only still shows folders
 * Martin Oberhuber (Wind River) - [192724] Fixed logic to filter folders if FILE_TYPE_FOLDERS
 * Javier Montalvo Orus (Symbian) - [191048] Remote files locally listed and being removed by other users should be reported as missing
 * Javier Montalvo Orus (Symbian) - [195677] Rename fails on WFTPD-2.03
 * Javier Montalvo Orus (Symbian) - [197105] Directory listing fails on Solaris when special devices are in a directory
 * Javier Montalvo Orus (Symbian) - [197758] Unix symbolic links are not classified as file vs. folder
 * Javier Montalvo Orus (Symbian) - [198182] FTP export problem: RSEF8057E: Error occurred while exporting FILENAME: Operation failed. File system input or output error
 * Javier Montalvo Orus (Symbian) - [192610] EFS operations on an FTP connection make Eclipse freeze
 * Javier Montalvo Orus (Symbian) - [195830] RSE performs unnecessary remote list commands
 * Martin Oberhuber (Wind River) - [198638] Fix invalid caching
 * Martin Oberhuber (Wind River) - [198645] Fix case sensitivity issues
 * Martin Oberhuber (Wind River) - [192610] Fix thread safety for delete(), upload(), setReadOnly() operations
 * Martin Oberhuber (Wind River) - [199548] Avoid touching files on setReadOnly() if unnecessary
 * Javier Montalvo Orus (Symbian) - [199243] Renaming a file in an FTP-based EFS folder hangs all of Eclipse
 * Martin Oberhuber (Wind River) - [203306] Fix Deadlock comparing two files on FTP
 * Martin Oberhuber (Wind River) - [204669] Fix ftp path concatenation on systems using backslash separator
 * Martin Oberhuber (Wind River) - [203490] Fix NPE in FTPService.getUserHome()
 * Martin Oberhuber (Wind River) - [203500] Support encodings for FTP paths
 * Javier Montalvo Orus (Symbian) - [208912] Cannot expand /C on a VxWorks SSH Server
 ********************************************************************************/

package org.eclipse.rse.internal.services.files.ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.rse.core.model.IPropertySet;
import org.eclipse.rse.internal.services.files.ftp.parser.IFTPClientConfigFactory;
import org.eclipse.rse.internal.services.files.ftp.parser.IFTPClientConfigProxy;
import org.eclipse.rse.services.Mutex;
import org.eclipse.rse.services.clientserver.FileTypeMatcher;
import org.eclipse.rse.services.clientserver.IMatcher;
import org.eclipse.rse.services.clientserver.NamePatternMatcher;
import org.eclipse.rse.services.clientserver.PathUtility;
import org.eclipse.rse.services.clientserver.messages.IndicatorException;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.AbstractFileService;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.files.IHostFile;
import org.eclipse.rse.services.files.RemoteFileCancelledException;
import org.eclipse.rse.services.files.RemoteFileIOException;
import org.eclipse.rse.services.files.RemoteFileSecurityException;
import org.eclipse.rse.services.files.RemoteFolderNotEmptyException;

public class FTPService extends AbstractFileService implements IFileService, IFTPService
{
	private FTPClient _ftpClient;
	private FTPFile[] _ftpFiles;
	
	private Mutex _commandMutex = new Mutex();
	
	private String    _userHome;
	private transient String _hostName;
	private transient String _userId;
	private transient String _password;
	private transient int _portNumber;
	private transient String _controlEncoding; //Encoding to be used for file and path names
	
	private OutputStream _ftpLoggingOutputStream;
	private IPropertySet _ftpPropertySet;
	private Exception _exception;
	
	private boolean _isBinaryFileType = true;
	private boolean _isPassiveDataConnectionMode = false;
	private IFTPClientConfigFactory _entryParserFactory;
	private IFTPClientConfigProxy _clientConfigProxy;
	
	//workaround to access FTPHostFile objects previously retrieved from the server
	//to avoid accessing the remote target when not necessary (bug 195830)
	//In the future, it would be better that the IHostFile object were passed from
	//the upper layer instead of the folder and file name.
	//See bug 162950.
	private String _fCachePreviousParent;
	private long _fCachePreviousTimestamp;
	private Map _fCachePreviousFiles = new HashMap();
	private static long FTP_STATCACHE_TIMEOUT = 200; //msec
	
	
	private static class FTPBufferedInputStream extends BufferedInputStream {
		
		private FTPClient client;
		
		/**
		 * Creates a BufferedInputStream and saves its argument, the input stream, for later use. An internal buffer array is created.
		 * @param in the underlying input stream.
		 * @param client the FTP client.
		 */
		public FTPBufferedInputStream(InputStream in, FTPClient client) {
			super(in);
			this.client = client;
		}

		/**
		 * Creates a BufferedInputStream  and saves its argument, the input stream, for later use. An internal buffer array of the given size is created.
		 * @param in the underlying input stream.
		 * @param size the buffer size.
		 * @param client the FTP client.
		 */
		public FTPBufferedInputStream(InputStream in, int size, FTPClient client) {
			super(in, size);
			this.client = client;
		}

		/**
		 * Closes the underlying input stream and completes the FTP transaction.
		 * @see java.io.BufferedInputStream#close()
		 */
		public void close() throws IOException {
			super.close();
			client.completePendingCommand();
			client.logout();
		}
	}
	
	private class FTPBufferedOutputStream extends BufferedOutputStream {
		
		private FTPClient client;
		
		/**
		 * Creates a new buffered output stream to write data to the specified underlying output stream with a default 512-byte buffer size.
		 * @param out the underlying output stream.
		 * @param client the FTP client.
		 */
		public FTPBufferedOutputStream(OutputStream out, FTPClient client) {
			super(out);
			this.client = client;
		}

		/**
		 * Creates a new buffered output stream to write data to the specified underlying output stream with the specified buffer size.
		 * @param out the underlying output stream.
		 * @param size the buffer size.
		 * @param client the FTP client.
		 */
		public FTPBufferedOutputStream(OutputStream out, int size, FTPClient client) {
			super(out, size);
			this.client = client;
		}

		/**
		 * Closes the underlying output stream and completes the FTP transaction.
		 * @see java.io.FilterOutputStream#close()
		 */
		public void close() throws IOException {
			super.close();
			client.completePendingCommand();
			client.logout();
		}
	}
	
	/**
	 * Set a IPropertySet containing pairs of keys and values with 
	 * the FTP Client preferences<br/>
	 * Supported keys and values are:<br/>
	 * <table border="1">
	 * <tr><th>KEY</th><th>VALUE</th><th>Usage</th></tr>
	 * <tr><th>"passive"</th><th>"true" | "false"</th><th>Enables FTP passive mode</th></tr>
	 * </table>
	 * 
	 * @see org.eclipse.rse.core.model.IPropertySet
	 * @param ftpPropertySet FTP Client Preference Properties to set 
	 */
	public void setPropertySet(IPropertySet ftpPropertySet)
	{
		_ftpPropertySet = ftpPropertySet;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.IService#getName()
	 */
	public String getName()
	{
		return FTPServiceResources.FTP_File_Service_Name;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.IService#getDescription()
	 */
	public String getDescription()
	{
		return FTPServiceResources.FTP_File_Service_Description;
	}
	
	public void setHostName(String hostname)
	{
		_hostName = hostname;
	}
	
	public void setPortNumber(int portNumber) {
		_portNumber = portNumber;
	}
	
	public void setUserId(String userId)
	{
		_userId = userId;
	}
	
	public void setPassword(String password)
	{
		_password = password;
	}
	
	public void setLoggingStream(OutputStream  ftpLoggingOutputStream)
	{
		 _ftpLoggingOutputStream =  ftpLoggingOutputStream;
	}
	
	public void setFTPClientConfigFactory(IFTPClientConfigFactory entryParserFactory)
	{
		_entryParserFactory = entryParserFactory;
	}
	
	/**
     * Set the character encoding to be used on the FTP command channel.
     * The encoding must be compatible with ASCII since FTP commands will
     * be sent with the same encoding. Therefore, wide
     * (16-bit) encodings are not supported.
     * @param encoding Encoding to set
     */ 
	public void setControlEncoding(String encoding)
	{
		_controlEncoding = encoding;
	}

	/**
	 * Check whether the given Unicode String can be properly represented with the
	 * specified control encoding. Throw a SystemMessageException if it turns out
	 * that information would be lost.
	 * @param s String to check
	 * @return the original String or a quoted or re-coded version if possible
	 * @throws SystemMessageException if information is lost
	 */
	protected String checkEncoding(String s) throws SystemMessageException {
		String encoding = _controlEncoding!=null ? _controlEncoding : getFTPClient().getControlEncoding();
		try {
			byte[] bytes = s.getBytes(encoding);
			String decoded = new String(bytes, encoding);
			if (!s.equals(decoded)) {
				int i=0;
				int lmax = Math.min(s.length(), decoded.length()); 
				while( (i<lmax) && (s.charAt(i)==decoded.charAt(i))) {
					i++;
				}
				//String sbad=s.substring(Math.max(i-2,0), Math.min(i+2,lmax));
				char sbad = s.charAt(i);
				//FIXME Need to externalize this message in 3.0
				String msg = "Cannot express character \'"+sbad+"\'(0x"+Integer.toHexString(sbad)  +") with " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "encoding \""+encoding+"\". "; //$NON-NLS-1$ //$NON-NLS-2$ 
				msg += "Please specify a different encoding in host properties.";  //$NON-NLS-1$
				throw new UnsupportedEncodingException(msg);
			}
			return s;
		} catch(UnsupportedEncodingException e) {
			try {
				//SystemMessage msg = new SystemMessage("RSE","F","9999",'E',e.getMessage(),"Please specify a different encoding in host properties."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				SystemMessage msg = new SystemMessage("RSE","F","9999",'E',e.getMessage(),""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				//throw new RemoteFileIOException(new SystemMessageException(msg));
				throw new SystemMessageException(msg);
			} catch(IndicatorException ind) {
				throw new RemoteFileIOException(e);
			}
		}
	}
	
	public void connect() throws RemoteFileSecurityException,IOException
	{
		
		if (_ftpClient == null)
		{
			_ftpClient = new FTPClient();
			// Encoding of control connection
			if(_controlEncoding!=null) {
				_ftpClient.setControlEncoding(_controlEncoding);
			}
		}

		if(_ftpLoggingOutputStream!=null)
		{
			_ftpClient.registerSpyStream(_ftpLoggingOutputStream);
		}
		
		if (_portNumber == 0) {
			_ftpClient.connect(_hostName);
		} else {
			_ftpClient.connect(_hostName, _portNumber);
		}
		
		int userReply = _ftpClient.user(_userId);
		
		if(FTPReply.isPositiveIntermediate(userReply))
		{
			//intermediate response, provide password and hide it from the console
			
			String newLine = System.getProperty("line.separator"); //$NON-NLS-1$
			
			_ftpClient.registerSpyStream(null);
			
			_ftpLoggingOutputStream.write(("PASS ******"+newLine).getBytes()); //$NON-NLS-1$
			int passReply = _ftpClient.pass(_password);
			_ftpLoggingOutputStream.write((_ftpClient.getReplyString()+newLine).getBytes());
			
			if(_ftpLoggingOutputStream!=null)
			{
				_ftpClient.registerSpyStream(_ftpLoggingOutputStream);
			}
			
			if(!FTPReply.isPositiveCompletion(passReply))
			{
				String lastMessage = _ftpClient.getReplyString();
				disconnect();
				throw new RemoteFileSecurityException(new Exception(lastMessage));
			}
		}
		else if(!FTPReply.isPositiveCompletion(userReply))
		{
			String lastMessage = _ftpClient.getReplyString();
			disconnect();
			throw new RemoteFileSecurityException(new Exception(lastMessage));
		}
		
		//System parser
		
		String systemName = _ftpClient.getSystemName();
		
		_ftpClient.setParserFactory(_entryParserFactory);
		_clientConfigProxy = _entryParserFactory.getFTPClientConfig(_ftpPropertySet.getPropertyValue("parser"),systemName);  //$NON-NLS-1$
		
		if(_clientConfigProxy!=null)
		{
			_ftpClient.configure(_clientConfigProxy.getFTPClientConfig());
		}
		else
		{
			//UNIX parsing by default if no suitable parser found
			_ftpClient.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));
		}
		
		// Initial active/passive mode. This action will be refreshed later using setDataConnectionMode()
		if(_ftpPropertySet.getPropertyValue("passive").equalsIgnoreCase("true")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			_ftpClient.enterLocalPassiveMode();
			_isPassiveDataConnectionMode = true;
		}
		else
		{
			_ftpClient.enterLocalActiveMode();
			_isPassiveDataConnectionMode = false;
		}
		
		// Initial ASCII/Binary mode. This action will be refreshed later using setFileType()
		_ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		_isBinaryFileType = true;
		
		_userHome = _ftpClient.printWorkingDirectory();
		
		//For VMS, normalize the home location
		if(_userHome.indexOf(':')!=-1 && _userHome.indexOf(']')!=-1)
		{
			_userHome = _userHome.replaceAll(":\\[", "/"); //$NON-NLS-1$ //$NON-NLS-2$
			_userHome = '/'+_userHome.substring(0,_userHome.lastIndexOf(']'));
		}
		
		//Just to be safe
		clearCache(null);
	}
	
	public void disconnect()
	{
		clearCache(null);
		try
		{
			getFTPClient().logout();
			_ftpClient = null;
		}
		catch (IOException e)
		{
		}
		finally {
			_ftpClient = null;
		}
		
	}
	
	/**
	 * Returns the commons.net FTPClient for this session.
	 * 
	 * As a side effect, it also checks the connection 
	 * by sending a NOOP to the remote side, and initiates
	 * a connect in case the NOOP throws an exception.
	 * 
	 * @return The commons.net FTPClient.
	 */
	public FTPClient getFTPClient()
	{
		if (_ftpClient == null)
		{
			_ftpClient = new FTPClient();
			// Encoding of control connection
			if(_controlEncoding!=null) {
				_ftpClient.setControlEncoding(_controlEncoding);
			}
		}
		
		if(_hostName!=null)
		{
			try{
				_ftpClient.sendNoOp();
			}catch (IOException e){
				try {
					connect();
				} catch (Exception e1) {}
			}
		}
		
		setDataConnectionMode();
		
		return _ftpClient; 
	}
	
	/**
	 * Clones the main FTP client connection, providing a separate client connected to the FTP server.
	 * 
	 * @param isBinary true if the FTPClient has to be using binary mode for data transfer, otherwise ASCII mode will be used
	 * @return A new commons.net FTPClient connected to the same server. After usage it has to be disconnected.
	 * @throws IOException
	 */
	private FTPClient cloneFTPClient(boolean isBinary) throws IOException
	{
		FTPClient ftpClient = new FTPClient();
		boolean ok=false;
		try {
			ftpClient.setControlEncoding(_ftpClient.getControlEncoding());
			ftpClient.connect(_ftpClient.getRemoteAddress());
			ftpClient.login(_userId,_password);
			
			if (_clientConfigProxy != null) {
				ftpClient.configure(_clientConfigProxy.getFTPClientConfig());
			} else {
				// UNIX parsing by default if no suitable parser found
				ftpClient.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));
			}

			if (_isPassiveDataConnectionMode) {
				ftpClient.enterLocalPassiveMode();
			}

			if (isBinary) {
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			} else {
				ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
			}
			ftpClient.registerSpyStream(_ftpLoggingOutputStream);
			ok=true;
		} finally {
			//disconnect the erroneous ftpClient, but forward the exception
			if (!ok) {
				try {
					ftpClient.disconnect();
				} catch(Throwable t) { /*ignore*/ }
			}
		}
		return ftpClient;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#getFile(String, String, IProgressMonitor)
	 */
	public IHostFile getFile(String remoteParent, String fileName, IProgressMonitor monitor) throws SystemMessageException
	{
		return getFileInternal(remoteParent, fileName, monitor);
	}

	
	/**
	 * Return FTPHostFile object for a given parent dir and file name.
	 * This is different than {@link #getFile(String, String, IProgressMonitor)}
	 * in order to ensure we always return proper FTPHostFile type.
	 * 
	 * @see org.eclipse.rse.services.files.IFileService#getFile(String, String, IProgressMonitor)
	 */
	protected FTPHostFile getFileInternal(String remoteParent, String fileName, IProgressMonitor monitor) throws SystemMessageException
	{
    	remoteParent = checkEncoding(remoteParent);
    	fileName = checkEncoding(fileName);
		if (monitor!=null){
			if (monitor.isCanceled()) {
				throw new RemoteFileCancelledException();
			}	
		}
		
		//Try the cache first, perhaps there is no need to acquire the Mutex
		//The cache is case sensitive only on purpose. For case insensitive matches
		//A fresh LIST is required.
		//
	    //In the future, it would be better that the
	    //IHostFile object were passed from the upper layer instead of the
	    //folder and file name (Bug 162950)
		synchronized(_fCachePreviousFiles) {
			if (_fCachePreviousParent == null ? remoteParent==null : _fCachePreviousParent.equals(remoteParent)) {
				Object result = _fCachePreviousFiles.get(fileName);
				if (result!=null) {
					long diff = System.currentTimeMillis() - _fCachePreviousTimestamp;
					//System.out.println("FTPCache: "+diff+", "+remoteParent+", "+fileName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if (diff < FTP_STATCACHE_TIMEOUT) {
						return (FTPHostFile)result;
					}
				}
			}
		}
		
		FTPHostFile file = null;
		if(_commandMutex.waitForLock(monitor, Long.MAX_VALUE))
		{
		
			try{
			
				//try to retrieve the file
				_ftpClient = getFTPClient();
				
				if(!_ftpClient.changeWorkingDirectory(remoteParent))
				{
					throw new RemoteFileIOException(new Exception(_ftpClient.getReplyString()));
				}
				
				if(!listFiles(monitor))
				{
					throw new RemoteFileCancelledException();
				}
				
				synchronized(_fCachePreviousFiles) {
					cacheFiles(remoteParent);

					//Bug 198645: try exact match first
					Object o = _fCachePreviousFiles.get(fileName);
					if (o!=null) return (FTPHostFile)o;

					//try case insensitive match (usually never executed)
					if (!isCaseSensitive()) {
						for (int i = 0; i < _ftpFiles.length; i++) {
							String tempName = _ftpFiles[i].getName();
							if(tempName.equalsIgnoreCase(fileName)) {
								file = (FTPHostFile)_fCachePreviousFiles.get(tempName);
								break;
							}
						}
					}

				}
				
				// if not found, create new object with non-existing flag
				if(file == null)
				{
					file = new FTPHostFile(remoteParent,fileName, false, false, 0, 0, false);
				}
			
			
			}catch (Exception e){
				throw new RemoteFileIOException(e);
			} finally {
				_commandMutex.release();
		    }
		}
		
		return file;
	}
	
	public boolean isConnected()
	{
		boolean isConnected = false;
		
		if(_ftpClient!=null) {
			isConnected =  _ftpClient.isConnected();
		}
		
		return isConnected;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.AbstractFileService#internalFetch(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String, int)
	 */
	protected IHostFile[] internalFetch(String parentPath, String fileFilter, int fileType, IProgressMonitor monitor) throws SystemMessageException
	{
    	parentPath = checkEncoding(parentPath);
		if (monitor!=null){
			if (monitor.isCanceled()) {
				throw new RemoteFileCancelledException();
			}	
		}
		
		List results = new ArrayList();
		
		if(_commandMutex.waitForLock(monitor, Long.MAX_VALUE))
		{
			try
			{
				if (fileFilter == null)
				{
					fileFilter = "*"; //$NON-NLS-1$
				}
				IMatcher filematcher = null;
				if (fileFilter.endsWith(",")) {  //$NON-NLS-1$
					String[] types = fileFilter.split(",");  //$NON-NLS-1$
					filematcher = new FileTypeMatcher(types, true);
				} else {
					filematcher = new NamePatternMatcher(fileFilter, true, true);
				}

				_ftpClient = getFTPClient();
				if(!_ftpClient.changeWorkingDirectory(parentPath))
				{
					throw new RemoteFileIOException(new Exception(_ftpClient.getReplyString()));
				}
				
				if(!listFiles(monitor))
				{
					throw new RemoteFileCancelledException();
				}
				
				synchronized (_fCachePreviousFiles) {
					cacheFiles(parentPath);
					
					for(int i=0; i<_ftpFiles.length; i++)
					{
						if(_ftpFiles[i]==null)
						{
							continue;
						}
						
						String rawListLine = _ftpFiles[i].getRawListing()+System.getProperty("line.separator"); //$NON-NLS-1$
						_ftpLoggingOutputStream.write(rawListLine.getBytes());
						
						String name = _ftpFiles[i].getName();
						FTPHostFile f = (FTPHostFile)_fCachePreviousFiles.get(name);
						
						if (isRightType(fileType,f)) {
							
							if (name.equals(".") || name.equals("..")) { //$NON-NLS-1$ //$NON-NLS-2$
								//Never return the default directory names
								continue;
							} else if (f.isDirectory() && fileType!=FILE_TYPE_FOLDERS) {
								//get ALL directory names (unless looking for folders only)
								results.add(f);
							} else if (filematcher.matches(name)) { 
								//filter all others by name.
								results.add(f);
							}
						}
					}
				}
				_ftpLoggingOutputStream.write(System.getProperty("line.separator").getBytes()); //$NON-NLS-1$
			}
			catch (Exception e)
			{			
				throw new RemoteFileIOException(e);
			} finally {
				_commandMutex.release();
		    }
		}
		
		return (IHostFile[])results.toArray(new IHostFile[results.size()]);
	}
	
	
	private char getSeparator()
	{
		return PathUtility.getSeparator(_userHome).charAt(0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#upload(org.eclipse.core.runtime.IProgressMonitor, java.io.File, java.lang.String, java.lang.String, boolean, java.lang.String, java.lang.String)
	 */
	public boolean upload(File localFile, String remoteParent, String remoteFile, boolean isBinary, String srcEncoding, String hostEncoding, IProgressMonitor monitor) throws SystemMessageException
	{ 
		boolean retValue = true;
    	remoteParent = checkEncoding(remoteParent);
    	remoteFile = checkEncoding(remoteFile);
		
		if (monitor!=null){
			if (monitor.isCanceled()) {
				throw new RemoteFileCancelledException();
			}	
		}
		else{
				monitor = new NullProgressMonitor();
		}

		MyProgressMonitor progressMonitor = new MyProgressMonitor(monitor);
		progressMonitor.init(0, localFile.getName(), remoteFile, localFile.length());
		FileInputStream input = null;
		OutputStream output = null;

		try {
			if(_commandMutex.waitForLock(monitor, Long.MAX_VALUE))
			{
				try
				{
					FTPClient ftpClient = getFTPClient();
					clearCache(remoteParent);
					ftpClient.changeWorkingDirectory(remoteParent);
					setFileType(isBinary);
					
					input =  new FileInputStream(localFile);
					output = ftpClient.storeFileStream(remoteFile);
					if (output!=null) {
						long bytes=0;
						byte[] buffer = new byte[4096];
						
						int readCount;
						while((readCount = input.read(buffer)) > 0)
						{
							bytes+=readCount;
							output.write(buffer, 0, readCount);
							progressMonitor.count(readCount);
							if (monitor!=null){
								if (monitor.isCanceled()) {
									retValue = false;
									break;
								}	
							}
						}
						if (retValue) {
							output.flush();
						}
						output.close();
						output = null;
						ftpClient.completePendingCommand();
					} else {
						throw new Exception(ftpClient.getReplyString());
					}
					if(retValue==false)	{
						ftpClient.deleteFile(remoteFile);
					}
				}
				finally {
					_commandMutex.release();
					try {
						if (input!=null) input.close();
					} finally {
						if (output!=null) output.close(); 
					}
			    }
			}
		} catch(Exception e) {
			throw new RemoteFileIOException(e);
		} finally {
			progressMonitor.end();
		}
		
		return retValue;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#upload(org.eclipse.core.runtime.IProgressMonitor, java.io.InputStream, java.lang.String, java.lang.String, boolean, java.lang.String)
	 */
	public boolean upload(InputStream stream, String remoteParent, String remoteFile, boolean isBinary, String hostEncoding, IProgressMonitor monitor) throws SystemMessageException
	{
		boolean retValue = true;
    	remoteParent = checkEncoding(remoteParent);
    	remoteFile = checkEncoding(remoteFile);
		
		try
		{
			BufferedInputStream bis = new BufferedInputStream(stream);
			File tempFile = File.createTempFile("ftpup", "temp"); //$NON-NLS-1$ //$NON-NLS-2$
			FileOutputStream os = new FileOutputStream(tempFile);
			BufferedOutputStream bos = new BufferedOutputStream(os);
	
			 byte[] buffer = new byte[4096];
			 int readCount;
			 while( (readCount = bis.read(buffer)) > 0) 
			 {
			      bos.write(buffer, 0, readCount);
			      if (monitor!=null){
					if (monitor.isCanceled()) {
						retValue = false;
						break;
					}	
				}
			 }
			 bos.close();
			 
			 if(retValue == true){
				retValue = upload(tempFile, remoteParent, remoteFile, isBinary, "", hostEncoding, monitor); //$NON-NLS-1$
			 }
			 
		}
		catch (Exception e) {
			throw new RemoteFileIOException(e);
	  }
		
	  return retValue;
	
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#download(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String, java.io.File, boolean, java.lang.String)
	 */
	public boolean download(String remoteParent, String remoteFile, File localFile, boolean isBinary, String hostEncoding, IProgressMonitor monitor) throws SystemMessageException
	{
		boolean retValue = true;
		
		if (monitor!=null){
			if (monitor.isCanceled()) {
				throw new RemoteFileCancelledException();
			}	
		}
		
		IHostFile remoteHostFile = getFile(remoteParent, remoteFile, monitor);
		MyProgressMonitor progressMonitor = new MyProgressMonitor(monitor);
		progressMonitor.init(0, remoteFile, localFile.getName(), remoteHostFile.getSize());
		OutputStream output = null;
		InputStream input = null;
		
		try {
			if(_commandMutex.waitForLock(monitor, Long.MAX_VALUE))
			{
				try
				{
					FTPClient ftpClient = getFTPClient();
					ftpClient.changeWorkingDirectory(remoteParent);
					setFileType(isBinary);
					
					input = ftpClient.retrieveFileStream(remoteFile);
					
					if(input != null)
					{
						if (!localFile.exists())
						{
							File localParentFile = localFile.getParentFile();
							if (!localParentFile.exists())
							{
								localParentFile.mkdirs();
							}
							localFile.createNewFile();
						}
						
						output = new FileOutputStream(localFile);
						byte[] buffer = new byte[4096];
						int readCount;
						while((readCount = input.read(buffer)) > 0)
						{
							output.write(buffer, 0, readCount);
							progressMonitor.count(readCount);
							if (monitor!=null){
								if (monitor.isCanceled()) {
									retValue = false;
									break;
								}	
							}
						}
						
						if (retValue) output.flush();
						input.close();
						input = null;
						ftpClient.completePendingCommand();
						
					}
					else
					{
						throw new RemoteFileIOException(new Exception(ftpClient.getReplyString()));
					}
				}
				finally
				{
					_commandMutex.release();
					try { 
						if (input!=null) input.close(); 
					} finally {
						if (output!=null) output.close();
					}
				}
			}
		} catch (FileNotFoundException e) {
			throw new RemoteFileIOException(e);
		} catch (IOException e) {
			throw new RemoteFileIOException(e);
		} finally {
			progressMonitor.end();
		}
		
		return retValue;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#getUserHome()
	 */
	public IHostFile getUserHome()
	{
		if (_userHome==null) {
			//As per bug 204710, this may be called before we are connected.
			//Returning null in this case is safest, see also SftpFileService.
			return null;
		}
		return new FTPHostFile("",_userHome,true,true,0,0,true); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#getRoots(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IHostFile[] getRoots(IProgressMonitor monitor) 
	{	
		
		IHostFile[] hostFile;
		
		if(_userHome.startsWith("/")) //$NON-NLS-1$
		{
			hostFile = new IHostFile[]{new FTPHostFile(null, "/", true, true, 0, 0, true)}; //$NON-NLS-1$
		}
		else
		{
			hostFile = new IHostFile[]{new FTPHostFile(null, _userHome, true, true, 0, 0, true)};
		}
		
		return hostFile;	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#delete(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String)
	 */
	public boolean delete(String remoteParent, String fileName, IProgressMonitor monitor) throws SystemMessageException {
		boolean hasSucceeded = false;
    	remoteParent = checkEncoding(remoteParent);
    	fileName = checkEncoding(fileName);
				
		MyProgressMonitor progressMonitor = new MyProgressMonitor(monitor);
		progressMonitor.init(FTPServiceResources.FTP_File_Service_Deleting_Task+fileName, 1);  
		
		IHostFile file = getFile(remoteParent, fileName, monitor);
			
		boolean isFile = file.isFile();
		
		if(_commandMutex.waitForLock(monitor, Long.MAX_VALUE)) {
			try {
				FTPClient ftpClient = getFTPClient();
				
				clearCache(remoteParent);
				hasSucceeded = FTPReply.isPositiveCompletion(ftpClient.cwd(remoteParent));
				
				if(hasSucceeded)
				{
					if(isFile)
					{
						hasSucceeded = ftpClient.deleteFile(fileName);
					}
					else
					{
						hasSucceeded = ftpClient.removeDirectory(fileName);
					}
				}
				
				if(!hasSucceeded){
					throw new Exception(ftpClient.getReplyString()+" ("+fileName+")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				else
				{
					progressMonitor.worked(1);
				}
				
			}
			catch (Exception e) {
				if(isFile){
					throw new RemoteFileIOException(e);
				}
				else{
					throw new RemoteFolderNotEmptyException(e);
				}
			} finally {
				_commandMutex.release();
			}
		}
		progressMonitor.end();

		return hasSucceeded;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#rename(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String, java.lang.String)
	 */
	public boolean rename(String remoteParent, String oldName, String newName, IProgressMonitor monitor) throws SystemMessageException {
		boolean success = false;
    	remoteParent = checkEncoding(remoteParent);
    	oldName = checkEncoding(oldName);
    	newName = checkEncoding(newName);
		
		if(_commandMutex.waitForLock(monitor, Long.MAX_VALUE))
		{
			try {
				FTPClient ftpClient = getFTPClient(); 
				clearCache(remoteParent);
				
				if(!ftpClient.changeWorkingDirectory(remoteParent))
				{
					throw new RemoteFileIOException(new Exception(ftpClient.getReplyString()));
				}
				
				success = ftpClient.rename(oldName, newName);
				
				if(!success)
				{
					throw new Exception(ftpClient.getReplyString());
				}
				
			} catch (Exception e) {
				throw new RemoteFileIOException(e);
			}finally {
				_commandMutex.release();
			}
		}

		return success;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#rename(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String, java.lang.String, org.eclipse.rse.services.files.IHostFile)
	 */
	public boolean rename(String remoteParent, String oldName, String newName, IHostFile oldFile, IProgressMonitor monitor) {
		boolean hasSucceeded = false;
				
		oldFile.renameTo(newName);

		return hasSucceeded;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#move(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public boolean move(String srcParent, String srcName, String tgtParent, String tgtName, IProgressMonitor monitor) throws SystemMessageException{
		boolean success = false;
    	srcParent = checkEncoding(srcParent);
    	srcName = checkEncoding(srcName);
    	tgtParent = checkEncoding(tgtParent);
    	tgtName = checkEncoding(tgtName);

		if(_commandMutex.waitForLock(monitor, Long.MAX_VALUE))
		{
			try{
				FTPClient ftpClient = getFTPClient(); 
			
				String source = concat(srcParent,srcName);
				String target = concat(tgtParent,tgtName);
					
				clearCache(srcParent);
				clearCache(tgtParent);
				success = ftpClient.rename(source, target);
				
				if(!success)
				{
					throw new Exception(ftpClient.getReplyString());
				}
			
			}catch (Exception e) {
				throw new RemoteFileIOException(e);
			}finally {
				_commandMutex.release();
			}
			
		}
		return success;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#createFolder(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String)
	 */
	public IHostFile createFolder(String remoteParent, String folderName, IProgressMonitor monitor) throws SystemMessageException
	{
		remoteParent = checkEncoding(remoteParent);
		folderName = checkEncoding(folderName);
		if(_commandMutex.waitForLock(monitor, Long.MAX_VALUE))
		{
			try
			{
				FTPClient ftpClient = getFTPClient();
				clearCache(remoteParent);
				if(!ftpClient.changeWorkingDirectory(remoteParent))
				{
					throw new Exception(ftpClient.getReplyString()+" ("+remoteParent+")");  //$NON-NLS-1$  //$NON-NLS-2$
				}
				
				if(!ftpClient.makeDirectory(folderName))
				{
					throw new RemoteFileIOException(new Exception(ftpClient.getReplyString()+" ("+folderName+")"));  //$NON-NLS-1$  //$NON-NLS-2$
				}
							
			}
			catch (Exception e)	{
				throw new RemoteFileSecurityException(e);
			}finally {
				_commandMutex.release();
			}
			
		}

		return getFile(remoteParent, folderName, monitor);
	}

    /* (non-Javadoc)
     * @see org.eclipse.rse.services.files.IFileService#createFile(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String)
     */
    public IHostFile createFile(String remoteParent, String fileName, IProgressMonitor monitor) throws SystemMessageException{
    	remoteParent = checkEncoding(remoteParent);
    	fileName = checkEncoding(fileName);
    	try {
			File tempFile = File.createTempFile("ftp", "temp");  //$NON-NLS-1$  //$NON-NLS-2$
			tempFile.deleteOnExit();
			boolean success = upload(tempFile, remoteParent, fileName, _isBinaryFileType, null, null, monitor);
			
			if(!success)
			{
				throw new RemoteFileIOException(new Exception(getFTPClient().getReplyString()));
			}
		}
		catch (Exception e) {			
			throw new RemoteFileSecurityException(e);
		}

		return getFile(remoteParent, fileName, monitor);
	}
    	
    public boolean copy(String srcParent, String srcName, String tgtParent, String tgtName, IProgressMonitor monitor) throws SystemMessageException  
	{
    	throw new RemoteFileIOException(new Exception(FTPServiceResources.FTP_File_Service_Copy_Not_Supported)); 
    }
	
	public boolean copyBatch(String[] srcParents, String[] srcNames, String tgtParent, IProgressMonitor monitor) throws SystemMessageException 
	{
		boolean hasSucceeded = false;
		
		for(int i=0; i<srcNames.length; i++)
		{
			hasSucceeded = copy(srcParents[i], srcNames[i], tgtParent, srcNames[i], monitor);
			if(!hasSucceeded)
			{
				break;
			}
		}
		
		return hasSucceeded;
	}

	public void initService(IProgressMonitor monitor)
	{
	}
	
	public void uninitService(IProgressMonitor monitor)
	{
	}

	public boolean isCaseSensitive()
	{
		//TODO find out whether remote is case sensitive or not
		return true;
	}
	
	/**
	 * Internal method to list files.
	 * MUST ALWAYS be called from _commandMutex protected region.
	 */
	private boolean listFiles(IProgressMonitor monitor) throws Exception
	{
		boolean result = true;
		
		_exception = null;
		
		Thread listThread = new Thread(new Runnable(){

			public void run() {
				try {
					
					_ftpFiles = null;
					
					if(_clientConfigProxy!=null)
					{
						_ftpFiles = _ftpClient.listFiles(_clientConfigProxy.getListCommandModifiers());
					}
					else
					{
						_ftpFiles = _ftpClient.listFiles();
					}
					
					
				} catch (IOException e) {
					_exception = e;
				}
			}});
		
		if(monitor != null)
		{
			if(!monitor.isCanceled())
				listThread.start();
			else
				return false;
			
			//wait
			
			while(!monitor.isCanceled() && listThread.isAlive())		
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
			
			//evaluate result
			
			if(monitor.isCanceled() && listThread.isAlive())
			{
				Thread killThread = listThread;
				listThread = null;
				killThread.interrupt();
					
				_ftpClient.completePendingCommand();
				
				throw new RemoteFileIOException(_exception);
			}
		
		}
		else
		{
			listThread.start();
			listThread.join();
			if(_exception!=null)
			{
				throw new RemoteFileIOException(_exception);
			}
				
		}

		return result;
	}
	
	private void cacheFiles(String parentPath) {
		synchronized (_fCachePreviousFiles) {
			_fCachePreviousFiles.clear();
			_fCachePreviousTimestamp = System.currentTimeMillis();
			_fCachePreviousParent = parentPath;
			
			for(int i=0; i<_ftpFiles.length; i++) {
				if(_ftpFiles[i]==null) {
					continue;
				}
				FTPHostFile f = new FTPHostFile(parentPath, _ftpFiles[i]);
				String name = f.getName();
				if(f.isLink()) {
					if(name.indexOf('.') < 0) {
						//modify FTPHostFile to be shown as a folder
						f.setIsDirectory(true);
					}
				}
				_fCachePreviousFiles.put(name, f);
			}
		}
	}
	
	/** Clear the statCache.
	 * @param parentPath path to clear. If <code>null, clear
	 *    all caches.
	 */
	private void clearCache(String parentPath) {
		synchronized (_fCachePreviousFiles) {
			if (parentPath==null || parentPath.equals(_fCachePreviousParent)) {
				_fCachePreviousFiles.clear();
			}
		}
	}
	
	private class MyProgressMonitor
	{
		  private IProgressMonitor fMonitor;
		  private double fWorkPercentFactor;
		  private Long fMaxWorkKB;
		  private long fWorkToDate;
		  
		  public MyProgressMonitor(IProgressMonitor monitor) {
			  fMonitor = monitor;
		  }
		  
		  public void init(int op, String src, String dest, long max){
			  fWorkPercentFactor = 1.0 / max;
			  fMaxWorkKB = new Long(max / 1024L);
			  fWorkToDate = 0;
			  String srcFile = new Path(src).lastSegment();
			  String desc = srcFile;
			  fMonitor.beginTask(desc, (int)max);
		  }
		  
		  public void init(String label, int max){
			  fMonitor.beginTask(label, max);
		  }
		  		  
		  public boolean count(long count){
			  fWorkToDate += count;
			  Long workToDateKB = new Long(fWorkToDate / 1024L);
			  Double workPercent = new Double(fWorkPercentFactor * fWorkToDate);
			  String subDesc = MessageFormat.format(
					 FTPServiceResources.FTP_File_Service_Monitor_Format,  
					  new Object[] {
						workToDateKB, fMaxWorkKB, workPercent	  
					  });
			  fMonitor.subTask(subDesc);
		      fMonitor.worked((int)count);
		      return !(fMonitor.isCanceled());
		  }
		  
		  public void worked(int work){
			  fMonitor.worked(work);
		  }
		  
		  public void end(){
			  fMonitor.done();
		  }
	}


	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#setLastModified(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String, long)
	 */
	public boolean setLastModified(String parent, String name,
			long timestamp, IProgressMonitor monitor) throws SystemMessageException
	{
		// not applicable for FTP
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.IFileService#setReadOnly(org.eclipse.core.runtime.IProgressMonitor, java.lang.String, java.lang.String, boolean)
	 */
	public boolean setReadOnly(String parent, String name,
			boolean readOnly, IProgressMonitor monitor) throws SystemMessageException {
		
		boolean result = false;
		FTPHostFile file = getFileInternal(parent,name, monitor);
		
		int userPermissions = file.getUserPermissions();
		int groupPermissions = file.getGroupPermissions();
		int otherPermissions = file.getOtherPermissions();
		
		int oldPermissions = userPermissions * 100 + groupPermissions * 10 + otherPermissions;
		if(readOnly) {
			userPermissions &= 5; // & 101b
			groupPermissions &= 5; // & 101b
			otherPermissions &= 5; // & 101b
		} else {
			userPermissions |= 2; // | 010b
		}
		int newPermissions = userPermissions * 100 + groupPermissions * 10 + otherPermissions;

		if (newPermissions==oldPermissions) {
			result = true;
		} else if(_commandMutex.waitForLock(monitor, Long.MAX_VALUE)) {
			try {
				clearCache(parent);
				result =_ftpClient.sendSiteCommand("CHMOD "+newPermissions+" "+file.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IOException e) {
				result = false;
			} finally {
				_commandMutex.release();
			}
		}
		
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.AbstractFileService#getInputStream(java.lang.String, java.lang.String, boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public InputStream getInputStream(String remoteParent, String remoteFile, boolean isBinary, IProgressMonitor monitor) throws SystemMessageException {
		
		if (monitor != null && monitor.isCanceled()){
			throw new RemoteFileCancelledException();
		}

		InputStream stream = null;
		
		try {
			FTPClient ftpClient = cloneFTPClient(isBinary);
			ftpClient.changeWorkingDirectory(remoteParent);
			stream = new FTPBufferedInputStream(ftpClient.retrieveFileStream(remoteFile), ftpClient);
		}
		catch (Exception e) {			
			throw new RemoteFileIOException(e);
		}
			
		return stream;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.services.files.AbstractFileService#getOutputStream(java.lang.String, java.lang.String, boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public OutputStream getOutputStream(String remoteParent, String remoteFile, boolean isBinary, IProgressMonitor monitor) throws SystemMessageException {
    	remoteParent = checkEncoding(remoteParent);
    	remoteFile = checkEncoding(remoteFile);
				
		if (monitor != null && monitor.isCanceled()){
			throw new RemoteFileCancelledException();
		}
		
		OutputStream stream = null;
		
		try {
			FTPClient ftpClient = cloneFTPClient(isBinary);
			clearCache(remoteParent);
			ftpClient.changeWorkingDirectory(remoteParent);
			stream = new FTPBufferedOutputStream(ftpClient.storeFileStream(remoteFile), ftpClient);
		}
		catch (Exception e) {
			throw new RemoteFileIOException(e);
		}
		
		return stream;
	}
	
	private void setDataConnectionMode()
	{
		if(_ftpPropertySet != null)
		{
			if(_ftpPropertySet.getPropertyValue("passive").equalsIgnoreCase("true") && !_isPassiveDataConnectionMode) //$NON-NLS-1$ //$NON-NLS-2$
			{
				_ftpClient.enterLocalPassiveMode();
				_isPassiveDataConnectionMode = true;
			}
			else if(_ftpPropertySet.getPropertyValue("passive").equalsIgnoreCase("false") && _isPassiveDataConnectionMode) //$NON-NLS-1$ //$NON-NLS-2$
			{
				_ftpClient.enterLocalActiveMode();
				_isPassiveDataConnectionMode = false;
			}
		}
	}

	private void setFileType(boolean isBinaryFileType) throws IOException
	{
		if(!isBinaryFileType && _isBinaryFileType)
		{
			_ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
			_isBinaryFileType = isBinaryFileType;
		} else if(isBinaryFileType && !_isBinaryFileType)
		{
			_ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			_isBinaryFileType = isBinaryFileType;
		}
	}
	
	/**
	 * Concatenate a parent directory with a file name to form a new proper path name.
	 * @param parentDir path name of the parent directory.
	 * @param fileName file name to concatenate.
	 * @return path name concatenated from parent directory and file name.
	 * 
	 */
	protected String concat(String parentDir, String fileName) {
		StringBuffer path = new StringBuffer(parentDir);
		if (!parentDir.endsWith(String.valueOf(getSeparator()))) 
		{
			path.append(getSeparator());
		}
		path.append(fileName);
		return path.toString();
	}

}
