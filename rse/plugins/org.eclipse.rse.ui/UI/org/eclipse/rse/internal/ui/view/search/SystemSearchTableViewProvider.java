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

package org.eclipse.rse.internal.ui.view.search;

import org.eclipse.rse.internal.ui.view.SystemTableTreeViewProvider;
import org.eclipse.rse.internal.ui.view.SystemTableViewColumnManager;

public class SystemSearchTableViewProvider extends SystemTableTreeViewProvider
{

   public SystemSearchTableViewProvider(SystemTableViewColumnManager columnManager)
   {
	   super(columnManager);
   }
   
	public String getText(Object object)
	{
		String text = getAdapterFor(object).getName(object);
		/** FIXME - IREmoteFile is systems.core independent now
		if (object instanceof IRemoteFile) {
			IRemoteFile parent = ((IRemoteFile)object).getParentRemoteFile();
			String absolutePath = getAdapterFor(parent).getAbsoluteName(parent);
			return text + " - " + absolutePath;
		}
		else {
			return text;
		}
		*/
		return text;
	}
}
