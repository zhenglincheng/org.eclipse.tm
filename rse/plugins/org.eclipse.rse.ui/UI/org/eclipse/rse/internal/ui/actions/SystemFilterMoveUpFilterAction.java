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
import java.util.Iterator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterContainer;
import org.eclipse.rse.core.filters.ISystemFilterPoolManager;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.internal.ui.SystemSortableSelection;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.swt.widgets.Shell;


/**
 * The action allows users to move the current filter up in the list
 */
public class SystemFilterMoveUpFilterAction extends SystemBaseAction 
                                 
{

	/**
	 * Constructor
	 */
	public SystemFilterMoveUpFilterAction(Shell parent) 
	{
		super(SystemResources.ACTION_MOVEUP_LABEL,SystemResources.ACTION_MOVEUP_TOOLTIP,
		      RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_MOVEUP_ID),
		      parent);
        allowOnMultipleSelection(true);
		setContextMenuGroup(ISystemContextMenuConstants.GROUP_REORDER);        
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
     * Intercept of parent method. We need to test that the filters
     *  come from the same parent
     */
	public boolean updateSelection(IStructuredSelection selection)
	{
		ISystemFilterContainer prevContainer = null;
		boolean enable = true;
		Iterator e = selection.iterator();
		while (enable && e.hasNext())
		{
			Object selectedObject = e.next();
			ISystemFilter filter = getSystemFilter(selectedObject);
			if (prevContainer != null)
			{
				if (prevContainer != filter.getParentFilterContainer())
					enable = false;
				else
					prevContainer = filter.getParentFilterContainer();			 
			}
			else
				prevContainer = filter.getParentFilterContainer();
			if (enable)
				enable = checkObjectType(filter);		  
		}
		return enable;
	}
	
    /**
     * Called by SystemBaseAction when selection is set.
     * Our opportunity to verify we are allowed for this selected type.
     */
	public boolean checkObjectType(Object selectedObject)
	{
		if (!((selectedObject instanceof ISystemFilter) ||
		      (selectedObject instanceof ISystemFilterReference)))
		  return false;
		ISystemFilter filter = getSystemFilter(selectedObject);
		ISystemFilterContainer fpContainer = filter.getParentFilterContainer();
		int pos = fpContainer.getSystemFilterPosition(filter);
		return (pos>0);		
	}
	
	private ISystemFilter getSystemFilter(Object selectedObject)
	{
		if (selectedObject instanceof ISystemFilter)
		  return (ISystemFilter)selectedObject;
		else
		  return ((ISystemFilterReference)selectedObject).getReferencedFilter();
	}	

	/**
	 * This is the method called when the user selects this action.
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() 
	{
		IStructuredSelection selections = getSelection();
		//SystemFilter filters[] = new SystemFilter[selections.size()];
		//Iterator i = selections.iterator();
		//int idx = 0;
		//while (i.hasNext())		
		  //filters[idx++] = getSystemFilter(i.next());

		SystemSortableSelection[] sortableArray = new SystemSortableSelection[selections.size()];
		Iterator i = selections.iterator();
		int idx = 0;
		ISystemFilter filter = null;
		ISystemFilterContainer fpContainer = null;
		while (i.hasNext())	
		{
		  	sortableArray[idx] = new SystemSortableSelection(getSystemFilter(i.next()));
		  	filter = (ISystemFilter)sortableArray[idx].getSelectedObject();
		  	fpContainer = filter.getParentFilterContainer();
		  	sortableArray[idx].setPosition(fpContainer.getSystemFilterPosition(filter));
		  	idx++;
		}
		SystemSortableSelection.sortArray(sortableArray);
		ISystemFilter[] filters = (ISystemFilter[])SystemSortableSelection.getSortedObjects(sortableArray, new ISystemFilter[sortableArray.length]);
		
		if (idx>0)
		{
		  	ISystemFilterPoolManager fpMgr = filters[0].getSystemFilterPoolManager();
		  	try {
		  		//System.out.println("In SystemFilterMoveUpFilterAction#run(). Calling mgr.moveSystemFilters");
		    	fpMgr.moveSystemFilters(filters,-1);
		  	} catch (Exception exc)
		  	{
		  	}
		}
	}		
}
