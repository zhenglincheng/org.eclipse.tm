/********************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [168975] Move RSE Events API to Core
 * Martin Oberhuber (Wind River) - [215820] Move SystemRegistry implementation to Core
 ********************************************************************************/

package org.eclipse.rse.internal.core.model;
import org.eclipse.rse.core.events.ISystemResourceChangeEvent;
import org.eclipse.rse.core.events.ISystemResourceChangeListener;
import org.eclipse.swt.widgets.Display;


/**
 * To support posted events versus synchronous events, this class encapsulates
 * the code to execute via the run() method.
 * <p>
 * The post behavior is accomplished by calling the asyncExec method in the SWT
 * widget Display class. The Display object comes from calling getDisplay() on
 * the shell which we get by calling getShell on the given listener. 
 * <p>
 * By having a separate class we can support multiple simultaneous post event
 * requests by instantiating this class for each request.
 */
public class SystemPostableEventNotifier implements Runnable 
{
	private ISystemResourceChangeEvent event = null;
    private ISystemResourceChangeListener listener = null;

	/**
	 * Constructor when the request is to post one event to one listener
	 */
	public SystemPostableEventNotifier(ISystemResourceChangeListener listener, ISystemResourceChangeEvent event)
	{
		this.event = event;
		this.listener = listener;
		// fix for 150919
		Display d = Display.getDefault();
		//Display d = listener.getShell().getDisplay();
		//d.asyncExec(this);
		d.syncExec(this);
	}
	
    // -----------------------------    
    // java.lang.Runnable methods...
    // -----------------------------    
    public void run()
    {
    	if (listener != null)
          listener.systemResourceChanged(event);    	
    }
	
}