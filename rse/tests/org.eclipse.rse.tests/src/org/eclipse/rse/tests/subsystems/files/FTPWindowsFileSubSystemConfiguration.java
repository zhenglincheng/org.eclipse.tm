/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Martin Oberhuber (Wind River) - initial API and implementation
 *******************************************************************************/

package org.eclipse.rse.tests.subsystems.files;

import org.eclipse.rse.subsystems.files.ftp.FTPFileSubSystemConfiguration;

public class FTPWindowsFileSubSystemConfiguration extends FTPFileSubSystemConfiguration {

	public FTPWindowsFileSubSystemConfiguration() {
		super();
		setIsUnixStyle(false);
	}

}
