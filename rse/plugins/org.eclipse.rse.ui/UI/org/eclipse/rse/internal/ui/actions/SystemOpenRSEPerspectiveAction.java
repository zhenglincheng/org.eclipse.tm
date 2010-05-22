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
 * {Name} (company) - description of contribution.
 *******************************************************************************/

package org.eclipse.rse.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.rse.internal.ui.view.SystemPerspectiveHelpers;


/**
 * Open the RSE perspective (used in welcome.xml)
 */
public class SystemOpenRSEPerspectiveAction extends Action {
	

       public SystemOpenRSEPerspectiveAction() 
       {
              super();
       }

       /**
        * @see Action#run()
        */
       public void run() 
       {
       		  SystemPerspectiveHelpers.openRSEPerspective();
       }

}