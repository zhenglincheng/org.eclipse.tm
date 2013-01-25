package org.eclipse.rse.internal.useractions.ui.uda;

/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * The action allows users to paste a user action or type from the clipboard
 */
public class SystemUDTreeActionPaste extends SystemBaseAction {
	private SystemUDBaseTreeView parentTreeView;

	/**
	 * Constructor 
	 */
	public SystemUDTreeActionPaste(SystemUDBaseTreeView parentTreeView) {
		super(SystemUDAResources.RESID_UDA_ACTION_PASTE_LABEL, SystemUDAResources.RESID_UDA_ACTION_PASTE_TOOLTIP, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_TOOL_PASTE), null);
		allowOnMultipleSelection(false);
		this.parentTreeView = parentTreeView;
		setContextMenuGroup(ISystemContextMenuConstants.GROUP_REORGANIZE);
		setHelp(RSEUIPlugin.HELPPREFIX + "udap0000"); //$NON-NLS-1$
	}

	/**
	 * We override from parent to do unique checking.
	 * We intercept to ensure this is isn't the last action/type
	 */
	public boolean updateSelection(IStructuredSelection selection) {
		return parentTreeView.canPaste();
	}

	/**
	 * This is the method called when the user selects this action.
	 */
	public void run() {
		parentTreeView.doPaste();
	}
}
