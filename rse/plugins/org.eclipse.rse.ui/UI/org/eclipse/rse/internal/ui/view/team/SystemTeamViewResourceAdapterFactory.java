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
 * David Dykstal (IBM) - [191130] fix unnecessary creation of the remote systems project
 *******************************************************************************/

package org.eclipse.rse.internal.ui.view.team;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.rse.core.SystemResourceManager;
import org.eclipse.rse.core.model.ISystemRegistry;


/**
 * Special adapter factory that maps Remote Systems Framework objects to underlying workbench resources
 */
public class SystemTeamViewResourceAdapterFactory implements IAdapterFactory 
{	
	/**
	 * @see IAdapterFactory#getAdapterList()
	 */
	public Class[] getAdapterList() 
	{
	    return new Class[] {IResource.class};		
	}
	/**
	 * Called by our plugin's startup method to register our adaptable object types 
	 * with the platform. We prefer to do it here to isolate/encapsulate all factory
	 * logic in this one place.
	 */
	public void registerWithManager(IAdapterManager manager)
	{
	    manager.registerAdapters(this, ISystemRegistry.class);
	    //manager.registerAdapters(this, SystemProfile.class); DEFERRED  UNTIL NEXT RELEASE
	}
	/**
	 * @see IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	public Object getAdapter(Object adaptableObject, Class adapterType) 
	{
	      Object adapter = null;
	      if (adaptableObject instanceof ISystemRegistry)
	      {
	        //SystemRegistry sr = (SystemRegistry)adaptableObject; 
	        // [191130] do not force the creation of the project, just return its handle
	        adapter = SystemResourceManager.getRemoteSystemsProject(false);
	      }
	      /* deferred
	      else if (adaptableObject instanceof SystemProfile)
	      {
	      	SystemProfile profile = (SystemProfile)adaptableObject;
	      	adapter = SystemResourceManager.getProfileFolder(profile);
	      }*/
		return adapter;
	}


}
