/********************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [168870] refactor org.eclipse.rse.core package of the UI plugin
 ********************************************************************************/

package org.eclipse.rse.internal.ui.view.search;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.rse.ui.view.SystemAdapterHelpers;
import org.eclipse.swt.graphics.Image;


/**
 * This is the label provider for the remote systems search view.
 */
public class SystemSearchViewLabelProvider extends LabelProvider {



	/**
	 * Constructor for SystemSearchViewLabelProvider.
	 */
	public SystemSearchViewLabelProvider() {
		super();
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(Object)
	 */
	public Image getImage(Object element) {
		
		if (element == null) {
			return null;
		}
		
		if (element instanceof IAdaptable) {
			ISystemViewElementAdapter adapter = getViewAdapter(element);
			
			if (adapter != null) {
				ImageDescriptor descriptor = adapter.getImageDescriptor(element);
				
				if (descriptor != null) {
					return descriptor.createImage();
				}
			}
		}
		
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(Object)
	 */
	public String getText(Object element) {
		
		if (element == null) {
			return null;
		}
		
		if (element instanceof IAdaptable) {
			ISystemViewElementAdapter adapter = getViewAdapter(element);
			
			if (adapter != null) {
				return adapter.getText(element);
			}
		}
		
		return null;
	}
	
	/**
	 * Get the adapter for the given object.
	 * @param the object
	 * @return the adapter
	 */
	public ISystemViewElementAdapter getViewAdapter(Object element) 
	{
    	return SystemAdapterHelpers.getViewAdapter(element);
	}
}