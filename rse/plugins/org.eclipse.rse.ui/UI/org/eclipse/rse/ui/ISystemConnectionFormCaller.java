/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [184095] Replace systemTypeName by IRSESystemType
 ********************************************************************************/

package org.eclipse.rse.ui;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.swt.widgets.Shell;

/**
 * Interface that any UI that uses the SystemConnectionForm must implement
 */
public interface ISystemConnectionFormCaller {

	/**
	 * Event: the user has selected a system type.
	 * 
	 * @param systemType the type of system selected
	 * @param duringInitialization true if this is being set at page initialization time versus selected by the user
	 */
	public void systemTypeSelected(IRSESystemType systemType, boolean duringInitialization);

	/**
	 * Return the shell hosting this form
	 */
	public Shell getShell();

}