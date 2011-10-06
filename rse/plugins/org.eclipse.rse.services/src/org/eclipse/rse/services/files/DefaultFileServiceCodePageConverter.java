/********************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight.
 *
 * Contributors:
 * David McKnight    (IBM)  -[209704] [api][dstore] Ability to override default encoding conversion needed.
 * David McKnight    (IBM)  -[220379] [api] Provide a means for contributing custom BIDI encodings
 * David McKnight    (IBM)  -[280453] [performance] DefaultFileServiceCodePageConverter is wasteful with main memory
 ********************************************************************************/
package org.eclipse.rse.services.files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.eclipse.rse.services.clientserver.SystemEncodingUtil;

/**
 * @since 3.0
 */
public class DefaultFileServiceCodePageConverter implements
		IFileServiceCodePageConverter {

	public byte[] convertClientStringToRemoteBytes(String remotePath, String clientString,
			String remoteEncoding, IFileService fs) {
		try
		{
			return clientString.getBytes(remoteEncoding);
		}
		catch (Exception e)
		{
		}
		return clientString.getBytes();
	}
	
	public void convertFileFromRemoteEncoding(String remotePath, File file, String remoteEncoding,
			String localEncoding, IFileService fs) {
		FileInputStream inputStream = null;
		FileOutputStream outStream = null;
		// read in the file
		try
		{
			// decoder to go from remote encoding to UTF8
			Charset rmtCharset = Charset.forName(remoteEncoding);
			CharsetDecoder rmtDecoder = rmtCharset.newDecoder();

			inputStream = new FileInputStream(file);
			
			int fileLength = (int)file.length();
			BufferedInputStream bufInputStream = new BufferedInputStream(inputStream, fileLength);
			
			String utf8Encoding = SystemEncodingUtil.ENCODING_UTF_8;
			boolean convertToUFT8 = localEncoding.equalsIgnoreCase(utf8Encoding);

			if (fileLength > 0){
				int MAX_READ = 10000;
				if (fileLength <= MAX_READ){ // read the whole file at once

					byte[] buffer = new byte[fileLength];
					bufInputStream.read(buffer, 0, fileLength);
					bufInputStream.close();
					inputStream.close();
					
					ByteBuffer rmtBuf = ByteBuffer.wrap(buffer, 0, fileLength);
						
					// convert from the remote encoding
					CharBuffer decodedBuf = null;
					decodedBuf = rmtDecoder.decode(rmtBuf);
									
					outStream = new FileOutputStream(file);
					if (!convertToUFT8){
						// for conversion to the local encoding
						Charset charset = Charset.forName(localEncoding);
						CharsetEncoder encoder = charset.newEncoder();
						byte[] localBuffer = null;
						// convert to the specified local encoding
						ByteBuffer lclBuf = encoder.encode(decodedBuf);
						localBuffer = lclBuf.array();
						
						
						// use the limit rather than the array length to avoid unwanted nulls
						outStream.write(localBuffer, 0, lclBuf.limit());
					}
					else { // already UTF-8, no encoding necessary		
						OutputStreamWriter writer = new OutputStreamWriter(outStream, utf8Encoding);				
						writer.write(decodedBuf.array(), 0, decodedBuf.limit());	
						writer.flush();
					}

					
				}
				else { // read and write sections of file at a time
					int inOffset = 0;
					int outOffset = 0;
					
					File altFile = new File(file.getAbsolutePath() + "~"); //$NON-NLS-1$
					outStream = new FileOutputStream(altFile); // using alternate file because we're writing while reading
					OutputStreamWriter writer = null;
					if (convertToUFT8){ // create a writer for this case
						writer = new OutputStreamWriter(outStream, utf8Encoding);
					}
					while (inOffset < fileLength){
						int readSize = MAX_READ;
						if (inOffset +  MAX_READ > fileLength){
							readSize = fileLength - inOffset;
						}
						
						byte[] buffer = new byte[readSize];
						inputStream.read(buffer, 0, readSize);
						inOffset += readSize;
						ByteBuffer rmtBuf = ByteBuffer.wrap(buffer, 0, readSize);		
						
						// convert from the remote encoding
						CharBuffer decodedBuf = null;
						decodedBuf = rmtDecoder.decode(rmtBuf);

						if (!convertToUFT8){
							// for conversion to the local encoding
							Charset charset = Charset.forName(localEncoding);
							CharsetEncoder encoder = charset.newEncoder();
							byte[] localBuffer = null;
							// convert to the specified local encoding
							ByteBuffer lclBuf = encoder.encode(decodedBuf);
							localBuffer = lclBuf.array();
											
							// use the limit rather than the array length to avoid unwanted nulls
							int writeSize = lclBuf.limit();
							outStream.write(localBuffer, 0, writeSize);
							outOffset += writeSize;
						}
						else {											
							writer.write(decodedBuf.array(), 0, decodedBuf.limit());	
							writer.flush();
						}
					}
					inputStream.close();	
					outStream.close();
					altFile.renameTo(file);
				}

			}
		} catch (Exception e) {
			// outstream could not be written properly: report
			throw new RuntimeException(e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ioe) {
				}
			}
			if (outStream != null) {
				try {
					outStream.close();
				} catch (IOException ioe) {
				}
			}
		}
	}



	public boolean isServerEncodingSupported(String remoteEncoding,
			IFileService fs) {
		return true;
	}

	/**
	 * to make another converter take precedence over this, supply a
	 * code page converter returning a lower number (i.e. higher priority)
	 */
	public int getPriority(String remoteEString, IFileService fs){
		return 1000;
	}


}
