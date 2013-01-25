/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - adapted template for daytime example.
 * Xuan Chen     (IBM) - [223126] [api][breaking] Remove API related to User Actions in RSE Core/UI
 *******************************************************************************/

package org.eclipse.rse.examples.daytime.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import org.eclipse.rse.examples.daytime.Activator;
import org.eclipse.rse.examples.daytime.DaytimeResources;
import org.eclipse.rse.examples.daytime.service.IDaytimeService;
import org.eclipse.rse.examples.daytime.subsystems.DaytimeSubSystem;
import org.eclipse.rse.ui.SystemMenuManager;
import org.eclipse.rse.ui.view.AbstractSystemViewAdapter;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;

/**
 * The DaytimeResourceAdapter fulfills the interface required by the Remote Systems
 * View, and delegates UI requests to the underlying data model (DaytimeResource).
 */
public class DaytimeResourceAdapter extends AbstractSystemViewAdapter implements
		ISystemRemoteElementAdapter {

	public DaytimeResourceAdapter() {
		super();
	}

	public void addActions(SystemMenuManager menu,
			IStructuredSelection selection, Shell parent, String menuGroup) {
	}

	public ImageDescriptor getImageDescriptor(Object element) {
		return Activator.getDefault().getImageDescriptor(Activator.ICON_ID_DAYTIME);
	}

	public String getText(Object element) {
		return ((DaytimeResource)element).getDaytime();
	}

	public String getAbsoluteName(Object object) {
		//Not used since we dont support clipboard copy, rename or filtering
		//FIXME absolute name must remain unique for the object over its lifetime
		return "daytime:"+getText(object); //$NON-NLS-1$
	}

	public String getType(Object element) {
		return DaytimeResources.Daytime_Resource_Type;
	}

	public Object getParent(Object element) {
		return null; // not really used, which is good because it is ambiguous
	}

	public boolean hasChildren(IAdaptable element) {
		return false;
	}

	public Object[] getChildren(IAdaptable element, IProgressMonitor monitor) {
		return null;
	}

	protected Object internalGetPropertyValue(Object key) {
		return null;
	}

	protected IPropertyDescriptor[] internalGetPropertyDescriptors() {
		return null;
	}

	// --------------------------------------
	// ISystemRemoteElementAdapter methods...
	// --------------------------------------

	public String getAbsoluteParentName(Object element) {
		// not really applicable as we have no unique hierarchy
		return "root"; //$NON-NLS-1$ 
	}

	public String getSubSystemConfigurationId(Object element) {
		// as declared in extension in plugin.xml
		return "daytime.tcp"; //$NON-NLS-1$  
	}

	public String getRemoteTypeCategory(Object element) {
		// Course grained. Same for all our remote resources.
		return "daytime"; //$NON-NLS-1$ 
	}

	public String getRemoteType(Object element) {
		// Fine grained. Unique to this resource type.
		return "daytime"; //$NON-NLS-1$ 
	}

	public String getRemoteSubType(Object element) {
		// Very fine grained. We don't use it.
		return null; 
	}

	public boolean refreshRemoteObject(Object oldElement, Object newElement) {
		DaytimeResource oldTime = (DaytimeResource)oldElement;
		DaytimeResource newTime = (DaytimeResource)newElement;
		newTime.setDaytime(oldTime.getDaytime());
		return false; // If daytime objects held references to their time string, we'd have to return true
	}

	public Object getRemoteParent(Object element, IProgressMonitor monitor) throws Exception {
		return null; // leave as null if this is the root 
	}

	public String[] getRemoteParentNamesInUse(Object element, IProgressMonitor monitor)
			throws Exception {
		DaytimeSubSystem ourSS = (DaytimeSubSystem)getSubSystem(element);
		IDaytimeService service = ourSS.getDaytimeService();
		String time = service.getTimeOfDay();
		String[] allLabels = new String[] { time };
		return allLabels; // Return list of all labels
	}
}
