/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
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

package org.eclipse.rse.internal.files.ui.actions;

import org.eclipse.compare.internal.ICompareContextIds;
import org.eclipse.rse.internal.files.ui.FileResources;
import org.eclipse.swt.widgets.Shell;



public class SystemCompareWithEditionAction extends SystemEditionAction 
{	

	public SystemCompareWithEditionAction(Shell parent) 
	{
		super(parent, 
				FileResources.ACTION_COMPAREWITH_HISTORY_LABEL,
				FileResources.ACTION_COMPAREWITH_HISTORY_TOOLTIP, 
			"org.eclipse.compare.internal.CompareWithEditionAction",  //$NON-NLS-1$
			false); 
			
		this.fHelpContextId= ICompareContextIds.COMPARE_WITH_EDITION_DIALOG;
	}
}
