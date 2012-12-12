/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
 * David McKnight   (IBM) - [225507][api][breaking] RSE dstore API leaks non-API types
 * David McKnight   (IBM) - [264858] [dstore] OpenRSE always picks the first trusted certificate
 * Noriaki Takatsu  (IBM) - [315333] [dstore] Correct Chained Certificates are rejected in the Client
 *******************************************************************************/

package org.eclipse.dstore.internal.core.util.ssl;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.dstore.core.util.ssl.DStoreKeyStore;
import org.eclipse.dstore.core.util.ssl.IDataStoreTrustManager;

public class DataStoreTrustManager implements IDataStoreTrustManager
{
	private KeyStore _keystore;
	private List _untrustedCerts;
	private List _verifyExceptions;
	
	//private X509Certificate _untrustedCert;
	//private Exception _verifyException;
	
	private List _trustedCerts;

	public DataStoreTrustManager()
	{
		_trustedCerts = new ArrayList();
		_untrustedCerts = new ArrayList();
		_verifyExceptions = new ArrayList();
	}
	

	public void setKeystore(String filePath, String password)
	{
		try
		{
			KeyStore ks = DStoreKeyStore.getKeyStore(filePath, password);
			_keystore = ks;
			loadTrustedCertificates();
		}
		catch (Exception e)
		{
			
		}
	}
	
	private void loadTrustedCertificates()
	{
		_trustedCerts.clear();
		try
		{
			Enumeration aliases = _keystore.aliases();
	
			while (aliases.hasMoreElements())
			{
				String alias = (String) (aliases.nextElement());
				
				/* The alias may be either a key or a certificate */
				java.security.cert.Certificate cert = _keystore.getCertificate(alias);
				_trustedCerts.add(cert);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	
	public List getUntrustedCerts()
	{
		return _untrustedCerts;
	}
	
	public List getVerifyExceptions()
	{
		return _verifyExceptions;
	}

	private void checkTrusted(X509Certificate[] certs, String arg1) throws CertificateException
	{
		_untrustedCerts.clear();
		_verifyExceptions.clear();
		

		for (int i = 0; i < certs.length; i++)
		{
			X509Certificate cert = certs[i];
			boolean foundMatch = false;
			if (_trustedCerts.size() > 0)
			{
		
				for (int j = 0; j < _trustedCerts.size() && !foundMatch; j++)
				{
					X509Certificate tcert = (X509Certificate)_trustedCerts.get(j);
					try
					{
						cert.verify(tcert.getPublicKey());
						foundMatch = true;					
					}
					catch (Exception e)
					{		
					}
				}									
			}	
			if (!foundMatch)
			{
				_untrustedCerts.add(cert);
			}
		}	
		if (_trustedCerts.size() == 0 || _untrustedCerts.size() > 0)
		{
			throw new CertificateException();
		}
	}
	
	public void checkClientTrusted(X509Certificate[] certs, String arg1) throws CertificateException
	{
		checkTrusted(certs, arg1);

	}

	public void checkServerTrusted(X509Certificate[] certs, String arg1) throws CertificateException
	{
		checkTrusted(certs,arg1);
	}

	public X509Certificate[] getAcceptedIssuers()
	{
		return null;
	}



}
