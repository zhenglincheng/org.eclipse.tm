/*******************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [182454] improve getAbsoluteName() documentation
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * David McKnight   (IBM)        - [216252] [api][nls] Resource Strings specific to subsystems should be moved from rse.ui into files.ui / shells.ui / processes.ui where possible
 * David McKnight   (IBM)        - [220547] [api][breaking] SimpleSystemMessage needs to specify a message id and some messages should be shared
 * Xuan Chen        (IBM)        - [223126] [api][breaking] Remove API related to User Actions in RSE Core/UI
 *******************************************************************************/

package org.eclipse.rse.internal.files.ui.view;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.services.clientserver.messages.CommonMessages;
import org.eclipse.rse.services.search.IHostSearchResultSet;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteSearchResult;
import org.eclipse.rse.subsystems.files.core.subsystems.RemoteSearchResultsContentsType;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemMenuManager;
import org.eclipse.rse.ui.view.AbstractSystemViewAdapter;
import org.eclipse.rse.ui.view.ISystemEditableRemoteObject;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.rse.ui.view.ISystemRemoveElementAdapter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

/**
 * Adapter for a search result set.
 */
public class SystemViewRemoteSearchResultSetAdapter extends AbstractSystemViewAdapter implements ISystemRemoteElementAdapter, ISystemRemoveElementAdapter
{
	/**
	 * Constructor.
	 */
	public SystemViewRemoteSearchResultSetAdapter()
	{
	}

	/**
	 * No actions are provided on a search results container
	 */
	public void addActions(SystemMenuManager menu, IStructuredSelection selection, Shell shell, String menuGroup)
	{
	}
	
	/**
	 * Returns false
	 */
	public boolean canEdit(Object obj)
	{
		return false;
	}
	
	/**
	 * Returns null since a search results container can't be edited
	 */
	public ISystemEditableRemoteObject getEditableRemoteObject(Object obj)
	{
		return null;
	}

	/**
	 * Returns the associated icon for a search handle
	 */
	public ImageDescriptor getImageDescriptor(Object element)
	{ 
		ImageDescriptor imageDescriptor= RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_SEARCH_RESULT_ID);
		return imageDescriptor;
	}

	/**
	 * Returns null since a search handle has no parent
	 */
	public Object getParent(Object element)
	{
		return null;
	}

	/**
	 * Returns the display text for a search handle.   The display text indicates the search string as well as
	 * whether the operation is in progress on not.
	 */
	public String getText(Object element) {
		
		if (element instanceof IHostSearchResultSet) {
			
			IHostSearchResultSet set = (IHostSearchResultSet)element;
			String name = set.getName();
			String msg = null;
				
			if (set.isRunning()) {
				msg = CommonMessages.MSG_OPERATION_RUNNING;
			}
			else if (set.isFinished()) {
				msg = CommonMessages.MSG_OPERATION_FINISHED;
			}
			else if (set.isCancelled()) {
				msg = CommonMessages.MSG_OPERTION_STOPPED;
			}
			else if (set.isDisconnected()) {
				msg = CommonMessages.MSG_OPERATION_DISCONNECTED;
			}
			
			if (msg != null)
			{
				msg = NLS.bind(msg, name);
				return msg;
			}
		}
		
		return null;
	}

	/**
	 * Returns the type property of a search.
	 */
	public String getType(Object element){
		return null;	
	}

	/**
	 * Returns the search results for the given search handle
	 */
	public Object[] getChildren(IAdaptable element, IProgressMonitor monitor) {
		
		if (element instanceof IHostSearchResultSet) {
			IHostSearchResultSet output = (IHostSearchResultSet)element;
			return output.getAllResults();
		}
		
		return null;
	}

	/**
	 * Returns <code>true</code> if it has children, otherwise returns <code>false</code>.
	 */
	public boolean hasChildren(IAdaptable element) {
		
		if (element instanceof IHostSearchResultSet) {
			int num = ((IHostSearchResultSet)element).getNumOfResults();
			return num > 0;
		}
		
		return false;
	}
	
	/**
	 * Returns false since a search handle can't be edited
	 */
	public boolean handleDoubleClick(Object element) {
		return false;
	}

	/**
	 * Returns the associated file subsystem for this search operation
	 */
	public ISubSystem getSubSystem(Object element) {
		return null;
	} 
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.IRemoteObjectIdentifier#getAbsoluteName(java.lang.Object)
	 */
	public String getAbsoluteName(Object element)
	{
		//FIXME this is forbidden by the API
		return null;
	} 
	
	public String getAbsoluteParentName(Object element)
	{
		return null;
	} 

	public String getSubSystemConfigurationId(Object element)
	{
		return null;
	} 
	
	public String getRemoteTypeCategory(Object element)
	{
		return null;
	} 
	
	public String getRemoteType(Object element)
	{
		return null;
	} 
	
	public String getRemoteSubType(Object element)
	{
		return null;
	} 
	
	
	public String getRemoteSubSubType(Object element)
	{
		return null;
	} 
	
	/**
	 * Return the source type of the selected object. Typically, this only makes sense for compilable
	 *  source members. For non-compilable remote objects, this typically just returns null.
	 */
	public String getRemoteSourceType(Object element)
	{
		return null;
	}		
	
	public boolean refreshRemoteObject(Object oldElement, Object newElement)
	{
		return false;
	}

   /**
	* Given a remote object, returns it remote parent object. Eg, given a file, return the folder
	*  it is contained in.
	* 
	*/
	public Object getRemoteParent(Object element, IProgressMonitor monitor) throws Exception
	{
		return null;
	}

	/**
	 * Given a remote object, return the unqualified names of the objects contained in that parent. This is
	 *  used for testing for uniqueness on a rename operation, for example. Sometimes, it is not 
	 *  enough to just enumerate all the objects in the parent for this purpose, because duplicate
	 *  names are allowed if the types are different, such as on iSeries. In this case return only 
 	 *  the names which should be used to do name-uniqueness validation on a rename operation.
	 */
	public String[] getRemoteParentNamesInUse(Object element, IProgressMonitor monitor) throws Exception
	{
		return null;
	}

	/**
	 * Return our unique property descriptors
	 */
	protected IPropertyDescriptor[] internalGetPropertyDescriptors()
	{
		return new IPropertyDescriptor[0];
	}
	
	/**
	 * Return our unique property values
	 */
	protected Object internalGetPropertyValue(Object key)
	{
		return null;
	}

	/**
	 * Return a filter string that corresponds to this object.
	 * @param object the object to obtain a filter string for
	 * @return the corresponding filter string if applicable
	 */
	public String getFilterStringFor(Object object)
	{
		return null;
	}
	
	/**
	 * @see org.eclipse.rse.ui.view.ISystemViewElementAdapter#canDelete(java.lang.Object)
	 */
	public boolean canDelete(Object element) {
		return false;
	}
	
	/**
	 * @see org.eclipse.rse.ui.view.ISystemViewElementAdapter#showDelete(java.lang.Object)
	 */
	public boolean showDelete(Object element) {
		return false;
	}
	
	/**
	 * @see org.eclipse.rse.ui.view.ISystemRemoveElementAdapter#remove(java.lang.Object, java.lang.Object)
	 */
	public boolean remove(Object element, Object child) {
		
		if (element instanceof IHostSearchResultSet) {
			IHostSearchResultSet set = (IHostSearchResultSet)element;
			
			// if the child is an IRemoteFile
			if (child instanceof IRemoteFile) {
				set.removeResult(child);
				return true;
			}
			// if child is a result leaf, remove it from its parent
			else if (child instanceof IRemoteSearchResult) {
				IRemoteSearchResult result = (IRemoteSearchResult)child;
				IRemoteFile parent = (IRemoteFile)(result.getParent());
				
				// get contents of parent
				Object[] contents = parent.getContents(RemoteSearchResultsContentsType.getInstance(), result.getMatchingSearchString().toString());
				List contentsList = new ArrayList();
				
				// go through array and add all entries to list that do not match the result
				for (int i = 0; i < contents.length; i++) {
					
					if (contents[i] != result) {
						contentsList.add(contents[i]);
					}
				}
				
				// now set the contents of the parent with the result removed
				parent.setContents(RemoteSearchResultsContentsType.getInstance(), result.getMatchingSearchString().toString(), contentsList.toArray());
				
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}
	
	/**
	 * @see org.eclipse.rse.ui.view.ISystemRemoveElementAdapter#removeAllChildren(java.lang.Object)
	 */
	public boolean removeAllChildren(Object element) {
		
		if (element == null) {
			return false;
		}
		
		if (element instanceof IHostSearchResultSet) {
			IHostSearchResultSet set = (IHostSearchResultSet)element;
			set.removeAllResults();
			return true;
		}
		else {
			return false;
		}
	}
	
}
