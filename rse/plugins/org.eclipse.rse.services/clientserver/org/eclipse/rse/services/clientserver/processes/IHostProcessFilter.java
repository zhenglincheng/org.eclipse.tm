/********************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [220041][api][breaking] Get rid of IHostProcessFilter#getStates()
 ********************************************************************************/

package org.eclipse.rse.services.clientserver.processes;


public interface IHostProcessFilter 
{
	/**
	 * Return the process name part of this filter string.
	 */
	public String getName();
	
	/**
	 * Return the username part of this filter string.
	 */
	public String getUsername();
	
	/**
	 * Return the process group id (gid) part of this filter string.
	 */
	public String getGid();
	
	/**
	 * Return the process parent id (ppid) part of this filter string.
	 */
	public String getPpid();
	
	/**
	 * Return the process id (pid) part of this filter string.
	 */
	public String getPid();
	
	/**
	 * Returns true when all process states are selected. The individual state
	 * queries will return false in this case.
	 */
	public boolean getAnyStatus();
	
	/**
	 * Returns the minimum VM size for processes allowed by this filter
	 */
	public String getMinVM();

	/**
	 * Returns the maximum VM size for processes allowed by this filter
	 */
	public String getMaxVM();

	/**
	 * Set the name part of this filter string. This can be simple or
	 * generic, where generic is a name containing one or two asterisks
	 * anywhere in the name.
	 */
	public void setName(String obj);

	/**
	 * Set the user id (uid) part of this filter string. This can be simple or
	 * generic, where generic is a uid containing one or two asterisks anywhere
	 * in the name.
	 */
	public void setUsername(String obj);

	/**
	 * Set the process group id (gid) part of this filter string.
	 */
	public void setGid(String obj);
	
	/**
	 * Set the process parent id part of this filter string.
	 */
	public void setPpid(String obj);
	
	/**
	 * Set the process id part of this filter string.
	 */
	public void setPid(String obj);

	/**
	 * Select all/any process states
	 */
	public void setAnyStatus();
	
	/**
	 * Sets the minimum VM size for processes allowed by this filter
	 */
	public void setMinVM(String strMinVM);

	/**
	 * Sets the maximum VM size for processes allowed by this filter
	 */
	public void setMaxVM(String strMaxVM);
	
	/**
	 * Returns whether this filter allows a process with the status line
	 * <code>status</code> to pass through. The status line contains some of the contents of
	 * the <i>status</i> file contained in the processes numbered directory in
	 * the /proc filesystem. For example, the status line of process 12345 is
	 * the contents of the file <i>/proc/12345/stat</i>.
	 * The status line must be structured as follows:
	 * "pid|name|status|tgid|ppid|tracerpid|uid|username|gid|vmSize|vmRSS"
	 */
	public boolean allows(String status);
	
	/**
	 * Check whether this filter requires that the given state is set.
	 * @param stateCode state code to check. One of the String constants
	 *     in {@link ISystemProcessRemoteConstants#ALL_STATES_STR}.
	 */
	public boolean getSpecificState(String stateCode);
	
	/**
	 * Change this filter such that it requires the given state to be set.
	 * @param stateCode state code to check. One of the String constants
	 *     in {@link ISystemProcessRemoteConstants#ALL_STATES_STR}.
	 */
	public void setSpecificState(String stateCode);
	
	/**
	 * Check whether a given process state String matches this filter.
	 * @param stateString A state String, holding a list of state constants 
	 *     from {@link ISystemProcessRemoteConstants#ALL_STATES_STR},
	 *     separated by comma (",").
	 */
	public boolean satisfiesState(String stateString);
}