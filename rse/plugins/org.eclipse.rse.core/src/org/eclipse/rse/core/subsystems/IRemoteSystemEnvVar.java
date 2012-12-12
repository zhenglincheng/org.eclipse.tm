/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
 * David McKnight  (IBM)  - [208813] removing deprecated APIs
 *******************************************************************************/

package org.eclipse.rse.core.subsystems;

public interface IRemoteSystemEnvVar {

	/**
	 * @generated This field/method will be replaced during code generation 
	 * @return The value of the Name attribute
	 * The name of the environment variable to set at connect time
	 */
	String getName();

	/**
	 * @generated This field/method will be replaced during code generation 
	 * @param value The new value of the Name attribute
	 */
	void setName(String value);

	/**
	 * @generated This field/method will be replaced during code generation 
	 * @return The value of the Value attribute
	 * The value to set this environment variable to at connect time
	 */
	String getValue();

	/**
	 * @generated This field/method will be replaced during code generation 
	 * @param value The new value of the Value attribute
	 */
	void setValue(String value);

}
