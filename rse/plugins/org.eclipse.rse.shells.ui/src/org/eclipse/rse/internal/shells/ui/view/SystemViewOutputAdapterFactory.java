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
 * Martin Oberhuber (Wind River) - [180519][api] declaratively register adapter factories
 * Martin Oberhuber (Wind River) - [168975] Move RSE Events API to Core
 ********************************************************************************/

package org.eclipse.rse.internal.shells.ui.view;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.rse.core.subsystems.ISystemDragDropAdapter;
import org.eclipse.rse.shells.ui.view.SystemViewRemoteOutputAdapter;
import org.eclipse.rse.subsystems.shells.core.subsystems.IRemoteCommandShell;
import org.eclipse.rse.subsystems.shells.core.subsystems.IRemoteError;
import org.eclipse.rse.subsystems.shells.core.subsystems.IRemoteOutput;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;


/**
 * This factory maps requests for an adapter object from a given
 *  element object. This is for the universal command subsystem.
 */
public class SystemViewOutputAdapterFactory implements IAdapterFactory 
{
	private SystemViewRemoteOutputAdapter outputAdapter = new SystemViewRemoteOutputAdapter();
	private SystemViewRemoteErrorAdapter  errorAdapter  = new SystemViewRemoteErrorAdapter();
	
	/**
	 * @see IAdapterFactory#getAdapterList()
	 */
	public Class[] getAdapterList() 
	{
	    return new Class[] {ISystemViewElementAdapter.class, ISystemDragDropAdapter.class, ISystemRemoteElementAdapter.class, IPropertySource.class, IWorkbenchAdapter.class, IActionFilter.class};		
	}
	
//	/**
//	 * Register this factory with the Platform's Adapter Manager.
//	 * Can be used for explicit registration, but we prefer doing it 
//	 * declaratively in plugin.xml so this is currently not used. 
//	 */
//	public void registerWithManager(IAdapterManager manager)
//	{
//		manager.registerAdapters(this, IRemoteError.class);
//	    manager.registerAdapters(this, IRemoteOutput.class);
//	    manager.registerAdapters(this, IRemoteCommandShell.class);
//	}
	
	/**
	 * @see IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	public Object getAdapter(Object adaptableObject, Class adapterType) 
	{
	    Object adapter = null;
	    if (adaptableObject instanceof IRemoteError)
	    	adapter = errorAdapter;
	    else if (adaptableObject instanceof IRemoteOutput)
	      	adapter = outputAdapter;
	    else if (adaptableObject instanceof IRemoteCommandShell)
	      	adapter = outputAdapter;

	    if ((adapter != null) && (adapterType == IPropertySource.class))
	    {	
	        ((ISystemViewElementAdapter)adapter).setPropertySourceInput(adaptableObject);
	    }		
	    else if (adapter == null)
	    {
	    	SystemBasePlugin.logWarning("No adapter found for object of type: " + adaptableObject.getClass().getName()); //$NON-NLS-1$
	    }	      	    
		return adapter;
	}


}