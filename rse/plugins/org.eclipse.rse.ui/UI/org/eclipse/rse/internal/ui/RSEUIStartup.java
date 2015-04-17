/********************************************************************************
 * Copyright (c) 2013, 2013 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * David Dykstal (IBM) - [397995] RSEInitJob starts too early
 ********************************************************************************/

package org.eclipse.rse.internal.ui;

import org.eclipse.rse.internal.core.RSEInitJob;
import org.eclipse.ui.IStartup;

public class RSEUIStartup implements IStartup {

	public void earlyStartup() {
       RSEInitJob.getInstance().schedule();
	}

}
