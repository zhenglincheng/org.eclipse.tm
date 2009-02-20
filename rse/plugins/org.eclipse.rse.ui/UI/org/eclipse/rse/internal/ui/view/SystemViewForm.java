/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 * Kevin Doyle (IBM) - [187553] - Removed code and related methods for toolbar/button bar.
 * Martin Oberhuber (Wind River) - [190271] Move ISystemViewInputProvider to Core
 * David McKnight    (IBM)   - [187711] select SystemView APIs exposed by the ISystemTree interface
 * David McKnight   (IBM)        - [225506] [api][breaking] RSE UI leaks non-API types
 * David McKnight   (IBM)        - [257721] Doubleclick doing special handling and expanding
 *******************************************************************************/
package org.eclipse.rse.internal.ui.view;
import java.util.List;
import java.util.Vector;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.ISystemViewInputProvider;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.ui.dialogs.SystemPromptDialog;
import org.eclipse.rse.ui.messages.ISystemMessageLine;
import org.eclipse.rse.ui.view.ISystemLongRunningRequestListener;
import org.eclipse.rse.ui.view.ISystemTree;
import org.eclipse.rse.ui.view.SystemLongRunningRequestEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;


/**
 * This re-usable widget is for a single widget that displays a 
 *  toolbar underneath which is a systems view tree.
 */
public class SystemViewForm extends Composite implements  ISystemTree
{
	private ToolBar        toolbar = null;
	private ToolBarManager toolbarMgr = null;
	private Button         refreshButton, getListButton;
	private SystemView     tree = null;
	private ISystemMessageLine msgLine = null;
	private boolean        showActions = true;
	private boolean        deferLoading = false;	
	private boolean        requestInProgress = false;
    private ISystemViewInputProvider inputProvider = null;
    private ISystemViewInputProvider emptyProvider = new SystemEmptyListAPIProviderImpl();
    private Vector         requestListeners = null;
	public static final int DEFAULT_WIDTH = 300;
	public static final int DEFAULT_HEIGHT = 300;
	
	// the following allows us to identify dialog/wizard hosting this widget so we can 
	// disable it's close capability while a remote request is in place.
	protected Object  caller;
	protected boolean callerInstanceOfWizardPage, callerInstanceOfSystemPromptDialog;
	
	// viewer filters
	protected ViewerFilter[] initViewerFilters = null;
	
	/**
	 * Constructor
	 * @param shell The owning window
	 * @param parent The owning composite
	 * @param style The swt style to apply to the overall composite. Typically SWT.NULL
	 * @param inputProvider Who is supplying the roots for the system viewer?
     * @param singleSelectionMode Are users allowed to select multiple things at once?
	 * @param msgLine where to show messages and tooltip text
	 */
	public SystemViewForm(Shell shell, Composite parent, int style, ISystemViewInputProvider inputProvider, 
	                      boolean singleSelectionMode, ISystemMessageLine msgLine)
	{
		this(shell, parent, style, inputProvider, singleSelectionMode, msgLine, 1, 1);	
	}
	/**
	 * Constructor when you want to span more than one column or row
	 * @param shell The owning window
	 * @param parent The owning composite
	 * @param style The swt style to apply to the overall composite. Typically SWT.NULL
	 * @param inputProvider Who is supplying the roots for the system viewer?
     * @param singleSelectionMode Are users allowed to select multiple things at once?
	 * @param msgLine where to show messages and tooltip text
	 * @param horizontalSpan how many columns in parent composite to span
	 * @param verticalSpan how many rows in parent composite to span
	 */
	public SystemViewForm(Shell shell, Composite parent, int style, ISystemViewInputProvider inputProvider, 
	                      boolean singleSelectionMode, ISystemMessageLine msgLine,  
	                      int horizontalSpan, int verticalSpan)
	{
		this(shell, parent, style, inputProvider, singleSelectionMode, msgLine, horizontalSpan, verticalSpan, null);
	}
	
	/**
	 * Constructor when you want to span more than one column or row
	 * @param shell The owning window
	 * @param parent The owning composite
	 * @param style The swt style to apply to the overall composite. Typically SWT.NULL
	 * @param inputProvider Who is supplying the roots for the system viewer?
     * @param singleSelectionMode Are users allowed to select multiple things at once?
	 * @param msgLine where to show messages and tooltip text
	 * @param horizontalSpan how many columns in parent composite to span
	 * @param verticalSpan how many rows in parent composite to span
	 * @param initViewerFilters the initial viewer filters to apply.
	 */
	public SystemViewForm(Shell shell, Composite parent, int style, ISystemViewInputProvider inputProvider, 
	                      boolean singleSelectionMode, ISystemMessageLine msgLine,  
	                      int horizontalSpan, int verticalSpan, ViewerFilter[] initViewerFilters)
	{
		super(parent, style);	
		this.inputProvider = inputProvider;
		this.msgLine = msgLine;
		callerInstanceOfWizardPage = (caller instanceof WizardPage);
		callerInstanceOfSystemPromptDialog = (caller instanceof SystemPromptDialog);				
		prepareComposite(1, horizontalSpan, verticalSpan);
    
	    // set viewer filters
	    this.initViewerFilters = initViewerFilters;
	    
	    createSystemView(shell, inputProvider, singleSelectionMode);
	    
	}
	
	/**
	 * Return the toolbar widget manager
	 */
	public ToolBarManager getToolBarManager()
	{
		return toolbarMgr;
	}
	/**
	 * Return the system view tree viewer
	 */
	public ISystemTree getSystemTree()
	{
		return tree;
	}
	/**
	 * Return the system view tree viewer tree widget
	 */
	public Tree getTreeControl()
	{
		return tree.getTree();
	}

    /**
     * Set the tree's tooltip text
     */
    public void setToolTipText(String tip)
    {
    	tree.getTree().setToolTipText(tip);
    }
        
    /**
     * Refresh contents
     */
    public void refresh()
    {
    	tree.refreshAll();
    }

    /**
     * Reset contents
     */
    public void reset(ISystemViewInputProvider inputProvider)
    {
    	this.inputProvider = inputProvider;
    	if (deferLoading)
    	{
    	  tree.setSelection(null);
    	  tree.setInputProvider(emptyProvider);
    	}
    	else
    	{
    	  tree.setSelection(null);
    	  tree.setInputProvider(inputProvider);
    	}
    }

	/*
	 * Turn off right-click actions
	 *
	 NOW SET VIA INPUT PROVIDER METHODS
	public void setShowActions(boolean show)
	{
		this.showActions = show;
		if (tree != null)
		  tree.setShowActions(show);
	}*/
	    
	/**
	 * Disable/Enable all the child controls.
	 */
	public void setEnabled(boolean enabled)
	{
		if (toolbar != null)
		  toolbar.setEnabled(enabled);
		tree.setEnabled(enabled);
		//if ((tree != null) && (tree.getTree() != null))
		//  tree.getTree().setEnabled(enabled);		
		//super.setEnabled(enabled);
	}
	/**
	 * Register a listener interested in an item is selected in the system view
     * @see #removeSelectionChangedListener(ISelectionChangedListener)
     */
    public void addSelectionChangedListener(ISelectionChangedListener listener) 
    {
	    tree.addSelectionChangedListener(listener);
    }
    /** 
     * Remove a previously set system view selection listener.
     * @see #addSelectionChangedListener(ISelectionChangedListener)
     */
    public void removeSelectionChangedListener(ISelectionChangedListener listener) 
    {
	    tree.removeSelectionChangedListener(listener);
    }
    /**
     * Add a listener that is informed when a remote list request starts and stops.
     * This allows for the listener to do things like disable the Close button
     */
    public void addListRequestListener(ISystemLongRunningRequestListener listener)
    {
    	if (requestListeners == null)
    	  requestListeners = new Vector();
    	requestListeners.addElement(listener);
    }
    /**
     * Add a listener that is informed when a remote list request starts and stops.
     * This allows for the listener to do things like disable the Close button
     */
    public void removeListRequestListener(ISystemLongRunningRequestListener listener)
    {
    	if (requestListeners != null)
    	  requestListeners.removeElement(listener);
    }
    
    /**
     * Return the selection of the tree viewer
     */
    public ISelection getSelection()
    {
    	return tree.getSelection();
    }
	

	// --------------------------------------------	
	// ISystemTree methods to facilitate our GUI...
	//  ... all these are delegated to the SystemView tree
	// --------------------------------------------
    /**
     * Returns true if any of the selected items are currently expanded
     */
    public boolean areAnySelectedItemsExpanded()
    {
    	return tree.areAnySelectedItemsExpanded();
    }    
    /**
     * Returns true if any of the selected items are expandable but not yet expanded
     */
    public boolean areAnySelectedItemsExpandable()
    {
    	return tree.areAnySelectedItemsExpandable();
    }
	/**
	 * This is called to ensure all elements in a multiple-selection have the same parent in the
	 *  tree viewer. If they don't we automatically disable all actions. 
	 * <p>
	 * Designed to be as fast as possible by going directly to the SWT widgets
	 */
	public boolean sameParent()
	{
		return tree.sameParent();
	}
	/**
	 * This is called to accurately get the parent object for the current selection
	 *  for this viewer. 
	 * <p>
	 * The getParent() method in the adapter is very unreliable... adapters can't be sure
	 * of the context which can change via filtering and view options.
	 */
	public Object getSelectedParent()
	{
		return tree.getSelectedParent();
	}
	/**
	 * This returns the element immediately before the first selected element in this tree level.
	 * Often needed for enablement decisions for move up actions.
	 */
	public Object getPreviousElement()
	{
		 return tree.getPreviousElement();
	}
	/**
	 * This returns the element immediately after the last selected element in this tree level
	 * Often needed for enablement decisions for move down actions.
	 */
	public Object getNextElement()
	{
		 return tree.getNextElement();
	}

	/**
	 * This is called to walk the tree back up to the roots and return the visible root
	 *  node for the first selected object.
	 */
	public Object getRootParent()
	{
		return tree.getRootParent();
	}
	/**
	 * This returns an array containing each element in the tree, up to but not including the root.
	 * The array is in reverse order, starting at the leaf and going up.
	 */
	public Object[] getElementNodes(Object element)
	{
		return tree.getElementNodes(element);
	}
	/**
	 * Helper method to determine if a given object is currently selected.
	 * Does consider if a child node of the given object is currently selected.
	 */
	public boolean isSelectedOrChildSelected(Object parentElement)
	{
		return tree.isSelectedOrChildSelected(parentElement);
	}

	/**
	 * Return the number of immediate children in the tree, for the given tree node
	 */
    public int getChildCount(Object element)
	{
        return tree.getChildCount(element);
	}

    /**
     * Called when a property is updated and we need to inform the Property Sheet viewer.
     * There is no formal mechanism for this so we simulate a selection changed event as
     *  this is the only event the property sheet listens for.
     */
    public void updatePropertySheet()
    {
        tree.updatePropertySheet();
    }
	
    /**
     * Called to select an object within the tree, and optionally expand it
     */   
    public void select(Object element, boolean expand)
    {
        tree.select(element, expand);
    }
	
    /**
     * Returns the tree item of the first selected object. Used for setViewerItem in a resource
     *  change event.
     */
    public Item getViewerItem()
    {
    	return tree.getViewerItem();
    }	
    
    /**
     * Returns true if it is ok to close the dialog or wizard page. Returns false if there
     *  is a remote request currently in progress.
     */
    public boolean okToClose()
    {
    	return !requestInProgress; //d43433
    }

	// -----------------------
	// INTERNAL-USE METHODS...
	// -----------------------
	/**
	 * Prepares this composite control and sets the default layout data.
	 * @param Number of columns the new group will contain.     
	 */
	protected Composite prepareComposite(int numColumns,
	                                     int horizontalSpan, int verticalSpan)	
	{
		Composite composite = this;
		//GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;		
		layout.verticalSpacing = 0;		
		composite.setLayout(layout);
		//GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
	    data.grabExcessHorizontalSpace = true;        
	    data.grabExcessVerticalSpace = true;	    
	    data.widthHint = DEFAULT_WIDTH;	    
	    data.heightHint = DEFAULT_HEIGHT;	
	    data.horizontalSpan = horizontalSpan;
	    data.verticalSpan = verticalSpan;    
		composite.setLayoutData(data);
		return composite;
	}
	
	protected void createSystemView(Shell shell, ISystemViewInputProvider inputProvider, boolean singleSelectionMode)
	{
		// TREE
        int style = (singleSelectionMode ? SWT.SINGLE : SWT.MULTI) | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
		tree = new SystemView(shell, this, style, deferLoading ? emptyProvider : inputProvider, msgLine, initViewerFilters);        
	    GridData treeData = new GridData();
	    treeData.horizontalAlignment = GridData.FILL;
	    treeData.verticalAlignment = GridData.FILL;	    
	    treeData.grabExcessHorizontalSpace = true;
	    treeData.grabExcessVerticalSpace = true;	    
	    treeData.widthHint = 300;        
	    treeData.heightHint= 200;
	    tree.getTree().setLayoutData(treeData);  	  	    		
	    tree.setShowActions(showActions);
	    
	    // for bug 257721, when using system view from a dialog, by default, we don't let adapter handle double-click
	    tree.allowAdapterToHandleDoubleClick(false);
	}
	
	protected void addOurMouseListener()
	{
	   MouseListener mouseListener = new MouseAdapter() 
	   {
		   public void mouseDown(MouseEvent e) 
		   {
			   //requestActivation();
		   }
	   };	
	   toolbar.addMouseListener(mouseListener);
	}

    /**
     * Fire long running request listener event
     */
    protected void fireRequestStartEvent()
    {
    	if (requestListeners != null)
    	{
    		SystemLongRunningRequestEvent event = new SystemLongRunningRequestEvent(); 
    		for (int idx=0; idx<requestListeners.size(); idx++)
    		   ((ISystemLongRunningRequestListener)requestListeners.elementAt(idx)).startingLongRunningRequest(event);
    	}
    }

    /**
     * Fire long running request listener event
     */
    protected void fireRequestStopEvent()
    {
    	if (requestListeners != null)
    	{
    		SystemLongRunningRequestEvent event = new SystemLongRunningRequestEvent(); 
    		for (int idx=0; idx<requestListeners.size(); idx++)
    		   ((ISystemLongRunningRequestListener)requestListeners.elementAt(idx)).endingLongRunningRequest(event);
    	}
    }

    

    /**
     * wrapper method to satisfy the ISystemTree interface
     */
	public void createTreeItems(TreeItem widget, Object[] children)
	{
		tree.createTreeItems(widget, children);
	}
	
    /**
     * wrapper method to satisfy the ISystemTree interface
     */
	public List findAllRemoteItemReferences(Object element,
			Object elementObject, List matches) {
		return tree.findAllRemoteItemReferences(element, elementObject, matches);
	}
	
    /**
     * wrapper method to satisfy the ISystemTree interface
     */
	public Item findFirstRemoteItemReference(Object remoteObject,
			Item parentItem) {
		return tree.findFirstRemoteItemReference(remoteObject, parentItem);
	}
	
    /**
     * wrapper method to satisfy the ISystemTree interface
     */
	public Tree getTree() {
		return tree.getTree();
	}
	
    /**
     * wrapper method to satisfy the ISystemTree interface
     */
	public ISystemFilterReference revealAndExpand(ISubSystem parentSubSystem,
			ISystemFilter filter) {
		return tree.revealAndExpand(parentSubSystem, filter);
	}
	public void addDoubleClickListener(IDoubleClickListener listener) {
		tree.addDoubleClickListener(listener);
	}
	public void addFilter(ViewerFilter filter) {
		tree.addFilter(filter);
		
	}
	public void expandTo(Object parentObject, Object remoteObject) {
		tree.expandTo(parentObject, remoteObject);
		
	}
	public void expandTo(String filterString) {
		tree.expandTo(filterString);
		
	}
	public boolean isExpandable(Object elementOrTreePath) {
		return tree.isExpandable(elementOrTreePath);
	}
	public void setAutoExpandLevel(int level) {
		tree.setAutoExpandLevel(level);
	}
	
}
