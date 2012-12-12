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
 * Martin Oberhuber (Wind River) - [168975] Move RSE Events API to Core
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Martin Oberhuber (Wind River) - [175680] Deprecate obsolete ISystemRegistry methods
 * David Dykstal (IBM) - [189858] Removed the remote systems project in the team view
 * Martin Oberhuber (Wind River) - [190271] Move ISystemViewInputProvider to Core
 ********************************************************************************/

package org.eclipse.rse.internal.ui.view.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.model.ISystemViewInputProvider;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.model.ISystemShellProvider;
import org.eclipse.rse.ui.view.IContextObject;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.model.WorkbenchContentProvider;


/**
 * Content provider for the RSE's Team view part.
 */
public class SystemTeamViewContentProvider extends WorkbenchContentProvider 
{
	private SystemTeamViewInputProvider inputProvider = null;
	private Viewer viewer;
	/**
	 * Constructor
	 */
	public SystemTeamViewContentProvider()
	{
	}
	
	/**
	 * Return the children of the given node, when it is expanded
	 */
	public Object[] getChildren(Object element) 
	{
		Object[] children;
		// for the project root node, return the SystemProfile objects
		if (element instanceof IProject) 
		{
			//IProject rseProject = (IProject)element;
			ISystemProfile[] profiles = RSECorePlugin.getTheSystemRegistry().getSystemProfileManager().getSystemProfiles();
			children = profiles;
			//return profiles;
		}
		else
		{
			ISystemViewElementAdapter adapter = getSystemViewAdapter(element);
			if (adapter != null)
				children = adapter.getChildren((IAdaptable)element, new NullProgressMonitor());
			else
				children = super.getChildren(element);
		}
		/*
		String name = element.getClass().getName();
		if (element instanceof SystemTeamViewSubSystemConfigurationNode)
		{
			SystemTeamViewSubSystemConfigurationNode ssfNode = (SystemTeamViewSubSystemConfigurationNode)element;
			name = ssfNode.getParentCategory().getLabel() + "." + ssfNode.getSubSystemConfiguration().getName();
		}
		else if (element instanceof SystemTeamViewCategoryNode)
		{
			SystemTeamViewCategoryNode catNode = (SystemTeamViewCategoryNode)element;
			name = catNode.getLabel();
		}
		System.out.println("  "+Integer.toString(counter-1)+". In getChildren for object '"+name +"', returned "+((children==null)?"null":Integer.toString(children.length)));
		*/
		return children;
	}	

	/**
	 * Return the parent of the given node
	 */
	public Object getParent(Object element) 
	{
		if (element instanceof ISystemProfile) 
//			return SystemResourceManager.getRemoteSystemsProject();
			return null;
		ISystemViewElementAdapter adapter = getSystemViewAdapter(element);
		if (adapter != null)
			return adapter.getParent(element);
		return super.getParent(element);
	}

	/**
	 * Return true if given element has children.
	 */
	public boolean hasChildren(Object element) 
	{
		if (element instanceof IContextObject)
		{
			element = ((IContextObject)element).getModelObject();
		}
		boolean children = false;
		if (element instanceof IProject) 
			children = (getChildren(element).length > 0);
		else
		{
			ISystemViewElementAdapter adapter = getSystemViewAdapter(element);
			if (adapter != null)
		 		children = adapter.hasChildren((IAdaptable)element);
		 	else
				children = super.hasChildren(element);
		}
		/* debug info
		String name = element.getClass().getName();
		if (element instanceof SystemTeamViewSubSystemConfigurationNode)
		{
			SystemTeamViewSubSystemConfigurationNode ssfNode = (SystemTeamViewSubSystemConfigurationNode)element;
			name = ssfNode.getParentCategory().getLabel() + "." + ssfNode.getSubSystemConfiguration().getName();
		}
		else if (element instanceof SystemTeamViewCategoryNode)
		{
			SystemTeamViewCategoryNode catNode = (SystemTeamViewCategoryNode)element;
			name = catNode.getLabel();
		}
		System.out.println(Integer.toString(counter++)+". In hasChildren for object: '"+name+"', returned "+children);
		*/
		return children;
	}

	/**
	 * Return the roots elements to display in the tree initially.
	 * For us, this is the RSE singleton project.
	 */
	public Object[] getElements(Object element) 
	{
		if (inputProvider == null)
		  return new Object[0];
		else
		  return inputProvider.getRoots(); // returns our single RSE project
	}

	/**
	 * View is going away: dispose of any local resources
	 */
	public void dispose() 
	{
		super.dispose();
		// we need to remove this provider from the Workspace 
		// ResourceChange listeners list. The parent dispose will
		// not do this for us.
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/**
	 * This hooks this content provider as an IResourceChangeListener.<br>
	 * We will not use parent code.
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) 
	{
		this.viewer = viewer;
		
		// TODO DKM - replace this with appropriate thing
		// super.viewer = viewer;	
		
		// TODO DKM - get rid of inputChanged.  I put it here temporarily so that there's a way to set super.viewer in 3.0
		super.inputChanged(viewer, oldInput, newInput);
		
		//System.out.println("inside inputChanged. oldInput = " + oldInput + ", newInput = " + newInput);
		if (newInput != null) 
		{
			if (newInput instanceof SystemTeamViewInputProvider)
			{
			  inputProvider = (SystemTeamViewInputProvider)newInput;
			  /*
			  getResourceDeltaHandler().registerTreeViewer((TreeViewer)viewer);
		      ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				     //IResourceChangeEvent.POST_AUTO_BUILD
				     //IResourceChangeEvent.PRE_AUTO_BUILD
				     IResourceChangeEvent.POST_CHANGE
				     // IResourceChangeEvent.PRE_CLOSE
					 //| IResourceChangeEvent.PRE_DELETE
					 //| IResourceChangeEvent.POST_AUTO_BUILD
				);
			  */
			}		
		}
	}

	/**
	 * Returns the implementation of ISystemViewElement for the given
	 * object.  Returns null if the adapter is not defined or the
	 * object is not adaptable.
	 */
	protected ISystemViewElementAdapter getSystemViewAdapter(Object o) 
	{
		ISystemViewElementAdapter adapter = null;    	
		if (o == null)
		{
			SystemBasePlugin.logWarning("ERROR: null passed to getAdapter in SystemTeamViewContentProvider"); //$NON-NLS-1$
			return null;    	  
		}
		if (!(o instanceof IAdaptable)) 
			adapter = (ISystemViewElementAdapter)Platform.getAdapterManager().getAdapter(o,ISystemViewElementAdapter.class);
		else
			adapter = (ISystemViewElementAdapter)((IAdaptable)o).getAdapter(ISystemViewElementAdapter.class);
		//if (adapter == null)
		//	RSEUIPlugin.logWarning("ADAPTER IS NULL FOR ELEMENT OF TYPE: " + o.getClass().getName());
		if ((adapter!=null) && (viewer != null))
		{    	
			Shell shell = null;
			if (viewer instanceof ISystemShellProvider)
				shell = ((ISystemShellProvider)viewer).getShell();
			else if (viewer != null)
				shell = viewer.getControl().getShell();
			if (shell != null)
				adapter.setShell(shell);
			adapter.setViewer(viewer);
			if (viewer.getInput() instanceof ISystemViewInputProvider)
			{
				ISystemViewInputProvider inputProvider = (ISystemViewInputProvider)viewer.getInput();
				adapter.setInput(inputProvider);
			}
		}
		else if (viewer == null)
			SystemBasePlugin.logWarning("VIEWER IS NULL FOR SystemTeamViewContentProvider");    	 //$NON-NLS-1$
		return adapter;
	}
}