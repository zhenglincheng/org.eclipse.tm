/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation. All rights reserved.
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
 * {Name} (company) - description of contribution.
 ********************************************************************************/

package org.eclipse.rse.ui;

/**
 * You own views launchable from the Remote System Explorer. You must register yourself
 *  with RSEUIPlugin via the registerViewSupplier method.
 */
public interface ISystemViewSupplier 
{



    /**
     * Close or reset views prior to full refresh after team synch
     */
    public void closeViews();
    
    /**
     * Restore views prior to full refresh after team synch
     */
    public void openViews();

}