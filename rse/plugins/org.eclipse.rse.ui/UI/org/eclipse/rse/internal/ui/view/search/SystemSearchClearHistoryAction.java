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

import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.swt.widgets.Shell;


/**
 * This action clears the history in the Remote Search view.
 */
public class SystemSearchClearHistoryAction extends SystemBaseAction {

	private SystemSearchViewPart searchView;

	/**
	 * Constructor for action.
	 * @param searchView the remote search view.
	 * @param shell the shell.
	 */
	public SystemSearchClearHistoryAction(SystemSearchViewPart searchView, Shell shell) {
		super(SystemResources.RESID_SEARCH_CLEAR_HISTORY_LABEL,SystemResources.RESID_SEARCH_CLEAR_HISTORY_TOOLTIP,
			  RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SEARCH_CLEAR_HISTORY_ID), shell);
			  
		this.searchView = searchView;
		RSEUIPlugin.getDefault().getWorkbench().getHelpSystem().setHelp(this, RSEUIPlugin.HELPPREFIX + "search_clear_history"); //$NON-NLS-1$
	}
	
	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		searchView.deleteAllPages();
	}
}
