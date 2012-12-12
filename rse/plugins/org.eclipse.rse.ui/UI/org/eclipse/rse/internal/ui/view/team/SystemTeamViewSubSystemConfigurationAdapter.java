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
 * Martin Oberhuber (Wind River) - [182454] improve getAbsoluteName() documentation
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Xuan Chen        (IBM)        - [223126] [api][breaking] Remove API related to User Actions in RSE Core/UI
 *******************************************************************************/

package org.eclipse.rse.internal.ui.view.team;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.filters.ISystemFilterPoolManager;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.internal.ui.actions.SystemFilterWorkWithFilterPoolsAction;
import org.eclipse.rse.internal.ui.view.SystemViewResources;
import org.eclipse.rse.ui.SystemMenuManager;
import org.eclipse.rse.ui.view.AbstractSystemViewAdapter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.PropertyDescriptor;


/**
 * Adapter for displaying and processing SystemTeamViewSubSystemConfigurationNode objects in tree views, such as
 *  the Team view.
 */
public class SystemTeamViewSubSystemConfigurationAdapter 
       extends AbstractSystemViewAdapter
{
	private boolean actionsCreated = false;
	//private Hashtable categoriesByProfile = new Hashtable();	
	private SystemFilterWorkWithFilterPoolsAction wwPoolsAction;
	
	// -------------------
	// property descriptors
	// -------------------
	private static PropertyDescriptor[] propertyDescriptorArray = null;
	
	
	/**
	 * Returns any actions that should be contributed to the popup menu
	 * for the given element.
	 * @param menu The menu to contribute actions to
	 * @param selection The window's current selection.
	 * @param shell Shell of viewer
	 * @param menuGroup recommended menu group to add actions to. If added to another group, you must be sure to create that group first.
	 */
	public void addActions(SystemMenuManager menu, IStructuredSelection selection, Shell shell, String menuGroup)
	{
		if (!actionsCreated)
		  createActions();	    

		SystemTeamViewSubSystemConfigurationNode ssfNode = (SystemTeamViewSubSystemConfigurationNode)selection.getFirstElement();
		SystemTeamViewCategoryNode category = ssfNode.getParentCategory();
		String categoryType = category.getMementoHandle();
		
		if (categoryType.equals(SystemTeamViewCategoryNode.MEMENTO_FILTERPOOLS) && ssfNode.getSubSystemConfiguration().supportsFilters())
		{		  
			wwPoolsAction.reset();
			wwPoolsAction.setShell(shell);
			wwPoolsAction.setFilterPoolManagerProvider(ssfNode.getSubSystemConfiguration());
			ISystemFilterPoolManager[] poolMgrs = new ISystemFilterPoolManager[1];
			poolMgrs[0] = ssfNode.getSubSystemConfiguration().getFilterPoolManager(ssfNode.getProfile());
			wwPoolsAction.setFilterPoolManagers(poolMgrs);
			menu.add(menuGroup, wwPoolsAction);
		}	    
	}
	private void createActions()
	{
		actionsCreated = true;
		
// FIXME - user actions and compile actions no longer coupled to core		
//		wwActionsAction = new SystemWorkWithUDAsAction(null, true);
//		wwCmdsAction = new SystemWorkWithCompileCommandsAction(null, true);
		wwPoolsAction = new SystemFilterWorkWithFilterPoolsAction(null, false);
	}
	
	/**
	 * Returns an image descriptor for the image. More efficient than getting the image.
	 * @param element The element for which an image is desired
	 */
	public ImageDescriptor getImageDescriptor(Object element)
	{				
	    return ((SystemTeamViewSubSystemConfigurationNode)element).getImageDescriptor();
	}
	
	/**
	 * Return the label for this object
	 */
	public String getText(Object element)
	{
		return ((SystemTeamViewSubSystemConfigurationNode)element).getLabel();
	}

	/**
	 * Return the name of this object, which may be different than the display text ({#link #getText(Object)}.
	 * <p>
	 * Called by common rename and delete actions.
	 */
	public String getName(Object element)
	{
		return ((SystemTeamViewSubSystemConfigurationNode)element).getLabel();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.IRemoteObjectIdentifier#getAbsoluteName(java.lang.Object)
	 */
	public String getAbsoluteName(Object element)
	{
		SystemTeamViewSubSystemConfigurationNode factory = (SystemTeamViewSubSystemConfigurationNode)element;
		return factory.getProfile().getName() + "." + factory.getParentCategory().getLabel() + factory.getLabel(); //$NON-NLS-1$
	}
		
	/**
	 * Return the type label for this object
	 */
	public String getType(Object element)
	{
		return SystemViewResources.RESID_PROPERTY_TEAM_SSFACTORY_TYPE_VALUE;
	}	
	
	/**
	 * Return the string to display in the status line when the given object is selected.
	 */
	public String getStatusLineText(Object element)
	{
		SystemTeamViewSubSystemConfigurationNode factory = (SystemTeamViewSubSystemConfigurationNode)element;
		return SystemResources.RESID_TEAMVIEW_SUBSYSFACTORY_VALUE + ": " + factory.getLabel(); //$NON-NLS-1$
	}
			
	/**
	 * Return the parent of this object. We return the RemoteSystemsConnections project
	 */
	public Object getParent(Object element)
	{
		SystemTeamViewSubSystemConfigurationNode factory = (SystemTeamViewSubSystemConfigurationNode)element;
		return factory.getParentCategory();
	}
	
	/**
	 * Return the children of this profile. 
	 */
	public Object[] getChildren(IAdaptable element, IProgressMonitor monitor)
	{		
		SystemTeamViewSubSystemConfigurationNode ssfNode = (SystemTeamViewSubSystemConfigurationNode)element;
		SystemTeamViewCategoryNode category = ssfNode.getParentCategory();
		ISystemProfile profile = ssfNode.getProfile();
		String categoryType = category.getMementoHandle();
		ISubSystemConfiguration ssf = ssfNode.getSubSystemConfiguration();
		if (categoryType.equals(SystemTeamViewCategoryNode.MEMENTO_FILTERPOOLS))
		{
			return profile.getFilterPools(ssf);
		}
		else if (categoryType.equals(SystemTeamViewCategoryNode.MEMENTO_USERACTIONS))
		{
			/* FIXME
			SystemUDActionElement[] children = profile.getUserActions(ssf);
			for (int idx=0; idx<children.length; idx++)
			{
				children[idx].setData(ssfNode);
			}
			return children;
			*/
			return null;
		}
		else if (categoryType.equals(SystemTeamViewCategoryNode.MEMENTO_COMPILECMDS))
		{
			/* FIXME
			SystemCompileType[] types = profile.getCompileCommandTypes(ssf);
			if (types != null)
			{
				SystemTeamViewCompileTypeNode[] typeNodes = new SystemTeamViewCompileTypeNode[types.length];
				for (int idx=0; idx<types.length; idx++)
					typeNodes[idx] = new SystemTeamViewCompileTypeNode(ssfNode, types[idx]);
				return typeNodes;
			}
			else
			*/
				return null;
		}
		else
			return null;
	}
		
	/**
	 * Return true if this profile has children. We return true.
	 */
	public boolean hasChildren(IAdaptable element)
	{
		SystemTeamViewSubSystemConfigurationNode ssConfNode = (SystemTeamViewSubSystemConfigurationNode)element;
		SystemTeamViewCategoryNode category = ssConfNode.getParentCategory();
		//ISystemProfile profile = ssConfNode.getProfile();
		String categoryType = category.getMementoHandle();
		//ISubSystemConfiguration ssConf = ssConfNode.getSubSystemConfiguration();
		if (categoryType.equals(SystemTeamViewCategoryNode.MEMENTO_FILTERPOOLS))
			return true;
		else  if (categoryType.equals(SystemTeamViewCategoryNode.MEMENTO_USERACTIONS))
		{
			/* FIXME
			return (profile.getUserActions(ssConf).length > 0);
			*/
			return false;
		}
		else if (categoryType.equals(SystemTeamViewCategoryNode.MEMENTO_COMPILECMDS))
		{
			/* FIXME
			return (profile.getCompileCommandTypes(ssConf).length > 0);
			*/
			return false;
		}
		else 	
			return false;
	}

    // Property sheet descriptors defining all the properties we expose in the Property Sheet
	/**
	 * Return our unique property descriptors, which getPropertyDescriptors adds to the common properties.
	 */
	protected org.eclipse.ui.views.properties.IPropertyDescriptor[] internalGetPropertyDescriptors()
	{
		if (propertyDescriptorArray == null)
		{
			/*
		  	propertyDescriptorArray = new PropertyDescriptor[1];
		 	RSEUIPlugin plugin = RSEUIPlugin.getDefault();
		 	int idx = 0;
		  	// status
		  	propertyDescriptorArray[idx] = new PropertyDescriptor(ISystemPropertyConstants.P_IS_ACTIVE, 
																SystemViewResources.RESID_PROPERTY_PROFILESTATUS_LABEL);
		  	propertyDescriptorArray[idx].setDescription(SystemViewResources.RESID_PROPERTY_PROFILESTATUS_DESCRIPTION));	      
		  	++idx;	
		  	*/      
		}		
		return propertyDescriptorArray;
	}
	
	/**
	 * Returns the current value for the named property.
	 * The parent handles P_TEXT and P_TYPE only, and we augment that here. 
	 * @param	key - the name of the property as named by its property descriptor
	 * @return	the current value of the property
	 */
	public Object internalGetPropertyValue(Object key)
	{
		/*		
		if (name.equals(P_IS_ACTIVE))
		{			
			boolean active = RSECorePlugin.getTheSystemRegistry().getSystemProfileManager().isSystemProfileActive(profile.getName());
			if (active)
				return SystemViewResources.RESID_PROPERTY_PROFILESTATUS_ACTIVE_LABEL);
			else
				return SystemViewResources.RESID_PROPERTY_PROFILESTATUS_NOTACTIVE_LABEL);		  
		}
		else
		*/		
		  return null;
	}	
	
    
	// ------------------------------------------------------------
	// METHODS FOR SAVING AND RESTORING EXPANSION STATE OF VIEWER...
	// ------------------------------------------------------------
	/**
	 * Return what to save to disk to identify this element in the persisted list of expanded elements.
	 */
	public String getMementoHandle(Object element)
	{
		SystemTeamViewSubSystemConfigurationNode factory = (SystemTeamViewSubSystemConfigurationNode)element;	
		return factory.getMementoHandle(); 
	}
	/**
	 * Return a short string to uniquely identify the type of resource. 
	 */
	public String getMementoHandleKey(Object element)
	{
		SystemTeamViewSubSystemConfigurationNode factory = (SystemTeamViewSubSystemConfigurationNode)element;	
		return factory.getProfile().getName() + "." + factory.getParentCategory().getLabel() + "." + factory.getLabel(); //$NON-NLS-1$  //$NON-NLS-2$ 
	}

	/**
	 * This is a local RSE artifact so returning false
	 * 
	 * @param element the object to check
	 * @return false since this is not remote
	 */
	public boolean isRemote(Object element) {
		return false;
	}
}
