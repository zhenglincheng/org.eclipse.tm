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
 * {Name} (company) - description of contribution.
 *******************************************************************************/

package org.eclipse.rse.internal.ui.propertypages;

import org.eclipse.rse.internal.ui.logging.LoggingPreferencePage;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;


/**
 * The logging preference page for Remote Systems.
 */
public class SystemLoggingPreferencePage extends LoggingPreferencePage {

	/**
	 * @see org.eclipse.rse.logging.LoggingPreferencePage#getPlugin()
	 */
	protected AbstractUIPlugin getPlugin() {
		return RSEUIPlugin.getDefault();
	}
}
