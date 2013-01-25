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
 * Martin Oberhuber (Wind River) - [168870] refactor org.eclipse.rse.core package of the UI plugin
 ********************************************************************************/

package org.eclipse.rse.internal.ui.actions;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.filters.ISystemFilterPoolReferenceManager;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.swt.widgets.Shell;


/**
 * A selectable filter pool name action.
 * This is typically used to allow users to select a filter pool for referencing
 */
public class SystemFilterPoolReferenceSelectAction extends SystemBaseAction 
                                 
{	
	private ISystemFilterPool pool;
	private ISystemFilterPoolReferenceManager refMgr;
	
	/**
	 * Constructor
	 */
	public SystemFilterPoolReferenceSelectAction(Shell parent, ISystemFilterPool pool, ISystemFilterPoolReferenceManager refMgr) 
	{
		super(pool.getName(), RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_NEWFILTERPOOLREF_ID), parent);
		this.pool = pool;
		this.refMgr = refMgr;
		//setChecked(false);
	}

    /**
     * Set the help context Id (infoPop) for this action. This must be fully qualified by
     *  plugin ID.
     * <p>
     * Same as {@link org.eclipse.rse.ui.actions.SystemBaseAction #setHelp(String)}
     * @see org.eclipse.rse.ui.actions.SystemBaseAction #getHelpContextId()
     */
    public void setHelpContextId(String id)
    {
    	setHelp(id);
    }

	/**
	 * @see SystemBaseAction#updateSelection(IStructuredSelection)
	 */
	public boolean updateSelection(IStructuredSelection selection)
	{
		boolean enable = true;
		return enable;
	}

	/**
	 * This is the method called when the user selects this action.
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() 
	{
        //System.out.println("Pretend to select");
        try 
        {
            refMgr.addReferenceToSystemFilterPool(pool);
        } catch (Exception exc)
        {
        	SystemBasePlugin.logError("Unexpected error adding filter pool reference",exc); //$NON-NLS-1$
        }
	}		
}