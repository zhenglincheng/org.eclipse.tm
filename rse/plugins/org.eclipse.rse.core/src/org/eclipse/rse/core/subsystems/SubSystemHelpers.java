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
 * Martin Oberhuber (Wind River) - [cleanup] Add API "since" Javadoc tags
 *******************************************************************************/

package org.eclipse.rse.core.subsystems;

import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterContainer;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.filters.ISystemFilterPoolManager;
import org.eclipse.rse.core.filters.ISystemFilterPoolReference;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.ISystemProfile;

/**
 * Static helper methods for subsystems.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class SubSystemHelpers
{

	/**
	 * Give a filter pool reference, return parent subsystem
	 * Returns this: <pre><code>(SubSystem)poolReference.getProvider()</pre></code>.
	 */
	public static ISubSystem getParentSubSystem(ISystemFilterPoolReference poolReference)
	{
		return (ISubSystem)poolReference.getProvider();
	}

	/**
	 * Give a filter pool, return parent subsystem factory
	 */
	public static ISubSystemConfiguration getParentSubSystemConfiguration(ISystemFilterPool pool)
	{
		return (ISubSystemConfiguration)pool.getProvider();
	}

	/**
	 * Give a filter, return parent subsystem factory
	 */
	public static ISubSystemConfiguration getParentSubSystemConfiguration(ISystemFilter filter)
	{
		return (ISubSystemConfiguration)filter.getProvider();
	}

	/**
	 * Give a filter pool or filter, return parent subsystem factory
	 */
	public static ISubSystemConfiguration getParentSubSystemConfiguration(ISystemFilterContainer container)
	{
		if (container instanceof ISystemFilterPool)
			return getParentSubSystemConfiguration((ISystemFilterPool)container);
		else
			return getParentSubSystemConfiguration((ISystemFilter)container);
	}


	/**
	 * Give a filter pool reference, return parent subsystem factory
	 */
	public static ISubSystemConfiguration getParentSubSystemConfiguration(ISystemFilterPoolReference poolRef)
	{
		ISystemFilterPool pool = poolRef.getReferencedFilterPool();
		if (pool != null)
			return getParentSubSystemConfiguration(pool);
		else
			return null;
	}

	/**
	 * Give a filter reference, return parent subsystem factory
	 */
	public static ISubSystemConfiguration getParentSubSystemConfiguration(ISystemFilterReference filterRef)
	{
		ISystemFilter filter = filterRef.getReferencedFilter();
		if (filter != null)
			return getParentSubSystemConfiguration(filter);
		else
			return null;
	}

	/**
	 * Give a filter pool, return its parent filter pool manager
	 */
	public static ISystemFilterPoolManager getParentSystemFilterPoolManager(ISystemFilterPool pool)
	{
		return pool.getSystemFilterPoolManager();
	}
	/**
	 * Give a filter pool, return its parent profile
	 */
	public static ISystemProfile getParentSystemProfile(ISystemFilterPool pool)
	{
		return getParentSubSystemConfiguration(pool).getSystemProfile(pool);
	}

}
