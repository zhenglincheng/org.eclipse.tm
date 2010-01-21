/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [168975] Move RSE Events API to Core
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Kevin Doyle (IBM) - [196582] ClassCastException when doing copy/paste with Search view open
 * Xuan Chen   (IBM) - [160775] [api] rename (at least within a zip) blocks UI thread
 * David McKnight   (IBM)        - [224313] [api] Create RSE Events for MOVE and COPY holding both source and destination fields
 * David McKnight   (IBM)        - [296877] Allow user to choose the attributes for remote search result
 *******************************************************************************/

package org.eclipse.rse.internal.ui.view;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.events.ISystemRemoteChangeEvent;
import org.eclipse.rse.core.events.ISystemRemoteChangeEvents;
import org.eclipse.rse.core.events.ISystemRemoteChangeListener;
import org.eclipse.rse.core.events.ISystemResourceChangeEvent;
import org.eclipse.rse.core.events.ISystemResourceChangeEvents;
import org.eclipse.rse.core.events.ISystemResourceChangeListener;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.IRemoteObjectIdentifier;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.internal.ui.SystemPropertyResources;
import org.eclipse.rse.internal.ui.actions.SystemCommonDeleteAction;
import org.eclipse.rse.internal.ui.actions.SystemCommonRenameAction;
import org.eclipse.rse.internal.ui.actions.SystemCommonSelectAllAction;
import org.eclipse.rse.internal.ui.actions.SystemOpenExplorerPerspectiveAction;
import org.eclipse.rse.internal.ui.actions.SystemShowInTableAction;
import org.eclipse.rse.internal.ui.actions.SystemSubMenuManager;
import org.eclipse.rse.services.clientserver.StringCompare;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.ISystemDeleteTarget;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.ISystemRenameTarget;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.SystemMenuManager;
import org.eclipse.rse.ui.actions.ISystemAction;
import org.eclipse.rse.ui.actions.SystemRefreshAction;
import org.eclipse.rse.ui.messages.ISystemMessageLine;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.eclipse.rse.ui.model.ISystemShellProvider;
import org.eclipse.rse.ui.view.AbstractSystemViewAdapter;
import org.eclipse.rse.ui.view.ContextObject;
import org.eclipse.rse.ui.view.IContextObject;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.rse.ui.view.ISystemSelectAllTarget;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.rse.ui.view.SystemAdapterHelpers;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.progress.PendingUpdateAdapter;
import org.eclipse.ui.views.properties.IPropertyDescriptor;


/**
 * This subclass of the standard JFace tabletree viewer is used to
 * show a generic tabletree view of the selected object 
 * <p>
 * 
 */
public class SystemTableTreeView
	//FIXEM change TreeViewer to TableTreeViewer when Eclipse fixes SWT viewer 
	//extends TableTreeViewer
	extends TreeViewer
	implements IMenuListener,
		ISystemDeleteTarget, ISystemRenameTarget, ISystemSelectAllTarget, 
		ISystemResourceChangeListener, ISystemRemoteChangeListener,
		ISystemShellProvider, ISelectionChangedListener, ISelectionProvider
{

	protected Composite getTableTree()
	{
		// TODO - turn back to table tree
		return getTree();
	}

	// TODO - turn back into tabletree
	// inner class to support cell editing - only use with table	
	private ICellModifier cellModifier = new ICellModifier()
	{
		public Object getValue(Object element, String property)
		{
			ISystemViewElementAdapter adapter = getViewAdapter(element);
			adapter.setPropertySourceInput(element);
			Object value = adapter.getPropertyValue(property);
			if (value == null)
			{
				value = ""; //$NON-NLS-1$
			}
			return value;
		}

		public boolean canModify(Object element, String property)
		{
			boolean modifiable = true;
			return modifiable;
		}

		public void modify(Object element, String property, Object value)
		{
			if (element instanceof TableItem && value != null)
			{
				Object obj = ((TableItem) element).getData();
				ISystemViewElementAdapter adapter = getViewAdapter(obj);
				if (adapter != null)
				{
					adapter.setPropertyValue(property, value);

					SelectionChangedEvent event = new SelectionChangedEvent(SystemTableTreeView.this, getSelection());

					// fire the event
					fireSelectionChanged(event);
				}
			}
		}
	};

	private class HeaderSelectionListener extends SelectionAdapter
	{
	
	    public HeaderSelectionListener()
	    {
	        _upI = RSEUIPlugin.getDefault().getImage(ISystemIconConstants.ICON_SYSTEM_MOVEUP_ID);
	        _downI = RSEUIPlugin.getDefault().getImage(ISystemIconConstants.ICON_SYSTEM_MOVEDOWN_ID);
	    }
	  
	    
		/**
		 * Handles the case of user selecting the
		 * header area.
		 * <p>If the column has not been selected previously,
		 * it will set the sorter of that column to be
		 * the current table view sorter. Repeated
		 * presses on the same column header will
		 * toggle sorting order (ascending/descending).
		 */
		public void widgetSelected(SelectionEvent e)
		{
			Tree table = getTree();
			if (!table.isDisposed())
			{
				// column selected - need to sort
			    TreeColumn tcolumn = (TreeColumn)e.widget;
				int column = table.indexOf(tcolumn);
				SystemTableViewSorter oldSorter = (SystemTableViewSorter) getSorter();
				if (oldSorter != null && column == oldSorter.getColumnNumber())
				{
					oldSorter.setReversed(!oldSorter.isReversed());
					if (tcolumn.getImage() == _upI)
					{
					    tcolumn.setImage(_downI);
					}
					else
					{
					    tcolumn.setImage(_upI);
					}
				} 
				else
				{
					setSorter(new SystemTableViewSorter(column, SystemTableTreeView.this, _columnManager));
					tcolumn.setImage(_downI);
				}
				
				// unset image of other columns
				TreeColumn[] allColumns = table.getColumns();
				for (int i = 0; i < allColumns.length; i++)
				{
				    if (i != column)
				    {
				        if (allColumns[i].getImage() != null)
				        {
				            allColumns[i].setImage(null);
				        }
				    }
				}
				refresh();
			}
		}
	}
	private Object _objectInput;
	//private ArrayList _attributeColumns;
	private TableLayout _layout;
	protected SystemTableTreeViewProvider _provider;
	private HeaderSelectionListener _columnSelectionListener;
	private SystemTableViewColumnManager _columnManager;
	private MenuManager _menuManager;
	private int   _charWidth = 3;
	private SystemTableViewFilter _filter;
	private IPropertyDescriptor[] _uniqueDescriptors;

	// these variables were copied from SystemView to allow for limited support
	// of actions.  I say limited because somethings don't yet work properly.
	protected SystemRefreshAction _refreshAction;
	protected PropertyDialogAction _propertyDialogAction;
	protected SystemOpenExplorerPerspectiveAction _openToPerspectiveAction;
	protected SystemShowInTableAction _showInTableAction;

	// global actions
	// Note the Edit menu actions are set in SystemViewPart. Here we use these
	//   actions from our own popup menu actions.
	protected SystemCommonDeleteAction _deleteAction;
	// for global delete menu item	
	protected SystemCommonRenameAction _renameAction;
	// for common rename menu item	
	protected SystemCommonSelectAllAction _selectAllAction;
	// for common Ctrl+A select-all

	protected boolean _selectionShowRefreshAction;
	protected boolean _selectionShowOpenViewActions;
	protected boolean _selectionShowDeleteAction;
	protected boolean _selectionShowRenameAction;
	protected boolean _selectionEnableDeleteAction;
	protected boolean _selectionEnableRenameAction;

	protected boolean _selectionIsRemoteObject = true;
	protected boolean _selectionFlagsUpdated = false;

	private int[] _lastWidths = null;
	private ISystemMessageLine _messageLine;
	protected boolean     menuListenerAdded = false;
	

    private static final int LEFT_BUTTON = 1;
    private int mouseButtonPressed = LEFT_BUTTON;   

	 private  Image _upI;
	 private  Image _downI;
    
	
	/**
		 * Constructor for the table view
		 * 
		 */
		public SystemTableTreeView(Tree tableTree, ISystemMessageLine msgLine)
		{
			super(tableTree);
			_messageLine = msgLine;
			//_attributeColumns = new ArrayList();
			_layout = new TableLayout();

			_columnManager = new SystemTableViewColumnManager(this);
			_provider = new SystemTableTreeViewProvider(_columnManager);
			_columnSelectionListener = new HeaderSelectionListener();


			setContentProvider(_provider);
			setLabelProvider(_provider);

			_filter = new SystemTableViewFilter();
			addFilter(_filter);
			
			_charWidth = tableTree.getFont().getFontData()[0].getHeight() / 2;
			computeLayout();

			_menuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
			_menuManager.setRemoveAllWhenShown(true);
			_menuManager.addMenuListener(this);
			Menu menu = _menuManager.createContextMenu(tableTree);
			tableTree.setMenu(menu);

			addSelectionChangedListener(this);

			ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
			sr.addSystemResourceChangeListener(this);
			sr.addSystemRemoteChangeListener(this);

			initDragAndDrop();

			tableTree.setVisible(false);
			// key listening for delete press
			getControl().addKeyListener(new KeyAdapter()
			{
				public void keyPressed(KeyEvent e)
				{
					handleKeyPressed(e);
				}
			});
			getControl().addMouseListener(new MouseAdapter() 
					{
					   public void mouseDown(MouseEvent e) 
					   {
					   	    mouseButtonPressed =  e.button;   		  //d40615 	    
					   }
					});		
		}

	public Layout getLayout()
	{
		return _layout;
	}


	public void setViewFilters(String[] filter)
	{
		if (_filter.getFilters() != filter)
		{
			_filter.setFilters(filter);
			refresh();
		}
	}

	public String[] getViewFilters()
	{
		return _filter.getFilters();
	}

	/**
	 * Return the popup menu for the table
	 */
	public Menu getContextMenu()
	{
		return getTableTree().getMenu();
	}
	/**
	 * Return the popup menu for the table
	 */
	public MenuManager getContextMenuManager()
	{
		return _menuManager;
	}

	/**
	 * Called whenever the input for the view changes
	 */
	public void inputChanged(Object newObject, Object oldObject)
	{
		if (newObject instanceof IAdaptable)
		{
			getTableTree().setVisible(true);
			_objectInput = newObject;

			SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();
			provider.getChildren(_objectInput);

			computeLayout();

			// reset the filter
			setViewFilters(null);

			super.inputChanged(newObject, oldObject);

		}
		else if (newObject == null)
		{
			getTableTree().setVisible(false);
			_objectInput = null;
			computeLayout();

			setViewFilters(null);
		}
	}

	public Object getInput()
	{
		return _objectInput;
	}

	/**
	 * Convenience method for retrieving the view adapter for an object 
	 */
	protected ISystemViewElementAdapter getViewAdapter(Object obj)
	{
		return SystemAdapterHelpers.getViewAdapter(obj, this);
	}
	
	public SystemTableViewColumnManager getColumnManager()
	{
	    return _columnManager;
	}

	private IPropertyDescriptor[] getCustomDescriptors(ISystemViewElementAdapter adapter)
	{
	    return _columnManager.getVisibleDescriptors(adapter);
	}
	
	public IPropertyDescriptor[] getUniqueDescriptors()
	{
		return _uniqueDescriptors;
	}
	
	/**
	 * Used to determine what the columns should be on the table.
	 */
	public IPropertyDescriptor[] getVisibleDescriptors(Object object)
		{
			SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();
			Object[] children = provider.getChildren(object);
			return getVisibleDescriptors(children);
		}
		
	private IPropertyDescriptor[] getVisibleDescriptors(Object[] children)
		{			
			if (children != null && children.length > 0)
			{
				IAdaptable child = (IAdaptable) children[0];
				ISystemViewElementAdapter adapter = getViewAdapter(child);
				adapter.setPropertySourceInput(child);
				return getCustomDescriptors(adapter);
			}

			return new IPropertyDescriptor[0];
	}

	
	
	public IPropertyDescriptor getNameDescriptor(Object object)
	{
		SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();
		Object[] children = provider.getChildren(object);
		return getNameDescriptor(children);
	}
	
	private IPropertyDescriptor getNameDescriptor(Object[] children)
		{
			if (children != null && children.length > 0)
			{
				IAdaptable child = (IAdaptable) children[0];
				return getViewAdapter(child).getPropertyDescriptors()[0];
			}

			return null;
		}

	/**
	 * Used to determine the formats of each descriptor.
	 */
	private ArrayList getFormatsIn()
	{
		ArrayList results = new ArrayList();
		SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();
		Object[] children = provider.getChildren(_objectInput);

		if (children != null && children.length > 0)
		{
			IAdaptable child = (IAdaptable) children[0];

			Object adapter = child.getAdapter(ISystemViewElementAdapter.class);
			if (adapter instanceof ISystemViewElementAdapter)
			{
				ISystemViewElementAdapter ad = (ISystemViewElementAdapter) adapter;
				ad.setPropertySourceInput(child);
				IPropertyDescriptor[] descriptors = _uniqueDescriptors;
				for (int i = 0; i < descriptors.length; i++)
				{
					IPropertyDescriptor descriptor = descriptors[i];

					try
					{
						Object key = descriptor.getId();

						Object propertyValue = ad.getPropertyValue(key, false);
						results.add(propertyValue.getClass());
					}
					catch (Exception e)
					{
						results.add(String.class);
					}

				}
			}
		}

		return results;
	}
	protected void computeLayout()
	{
		computeLayout(false);
	}

	private boolean sameDescriptors(IPropertyDescriptor[] descriptors1, IPropertyDescriptor[] descriptors2)
	{
		if (descriptors1 == null || descriptors2 == null)
		{
			return false;
		}
		if (descriptors1.length == descriptors2.length)
		{
			boolean same = true;
			for (int i = 0; i < descriptors1.length && same; i++)
			{
				same = descriptors1[i] == descriptors2[i];
			}
			return same;
		}
		else
		{
			return false;
		}
	}
	
	private CellEditor getCellEditor(Tree parent, IPropertyDescriptor descriptor)
	{
		CellEditor editor = descriptor.createPropertyEditor(parent);
		if (editor instanceof SystemInheritableTextCellEditor)
		{
			((SystemInheritableTextCellEditor) editor).getInheritableEntryField().setAllowEditingOfInheritedText(true);
		}

		return editor;
	}
	
	/**
	 * Determines what columns should be shown in this view. The columns may change
	 * anytime the view input changes.  The columns in the control are modified and
	 * columns may be added or deleted as necessary to make it conform to the 
	 * new data.
	 */
	public void computeLayout(boolean force)
	{
		if (_objectInput == null)
			return;
			
		SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();
		if (provider == null)
			return;
		Object[] children = provider.getChildren(_objectInput);

		// if no children, don't update
		if (children == null || children.length == 0 || (children.length == 1 && children[0] instanceof PendingUpdateAdapter))
		{
			return;
		}
		
		IPropertyDescriptor[] descriptors = getVisibleDescriptors(children);
		IPropertyDescriptor nameDescriptor = getNameDescriptor(children);
		
		int n = descriptors.length; // number of columns we need (name column + other columns)
		if (nameDescriptor != null)
			n += 1;
		if (n == 0)
			return; // there is nothing to lay out!

		
		if (sameDescriptors(descriptors,_uniqueDescriptors) && !force)
		{
			setLastColumnWidths(getCurrentColumnWidths());
			return;
		}
		_uniqueDescriptors = descriptors;
		Tree tree = getTree();
		if (tree == null || tree.isDisposed())
			return;

		// set column attributes, create new columns if necessary
		TreeColumn[] columns = tree.getColumns();
		int numColumns = columns.length; // number of columns in the control
		CellEditor editors[] = new CellEditor[n];
		String headings[] = new String[n];
		String propertyIds[] = new String[n];
		ArrayList formats = getFormatsIn();


		_layout = new TableLayout();
		for (int i = 0; i < n; i++)
		{ // for each column
			String name = null;
			String propertyId = null;
			CellEditor editor = null;
			int alignment = SWT.LEFT;
			int weight = 100;
			if (i == 0)
			{ 
				// this is the first column -- treat it special
				name = SystemPropertyResources.RESID_PROPERTY_NAME_LABEL;
				if (nameDescriptor != null)
				{
					propertyId = (String) nameDescriptor.getId();
					editor = getCellEditor(tree, nameDescriptor);
					weight = 200;
				}
			}
			else
			{ // these columns come from the regular descriptors
				IPropertyDescriptor descriptor = descriptors[i - 1];

				Class format = (Class) formats.get(i - 1);
				name = descriptor.getDisplayName();
				propertyId = (String) descriptor.getId();
				editor = getCellEditor(tree, descriptor);
				if (format != String.class)
					alignment = SWT.RIGHT;
			}
			TreeColumn tc = null;
			if (i >= numColumns)
			{
				tc = new TreeColumn(tree, alignment, i);
				tc.addSelectionListener(_columnSelectionListener);
		
			}
			else
			{
				tc = columns[i];
				tc.setAlignment(alignment);
			}
			_layout.addColumnData(new ColumnWeightData(weight));
			tc.setText(name);
			if (i == 0)
			{
			 //   tc.setImage(_downI);
			}
			headings[i] = name;
			editors[i] = editor;
			propertyIds[i] = propertyId;
		}
		setColumnProperties(propertyIds);
		setCellEditors(editors);
		setCellModifier(cellModifier);

		// dispose of any extra columns the tree control may have
		for (int i = n; i < numColumns; i++)
		{
			columns[i].dispose();
			columns[i] = null;
		}

		// compute column widths
		columns = tree.getColumns();
		numColumns = columns.length;
		Rectangle clientA = tree.getClientArea();
		int totalWidth = clientA.width - 5;
		if (totalWidth <= 0)
		{
		    // find a default
		    totalWidth = 500;
		}
		

		int[] lastWidths = getLastColumnWidths();
		if (numColumns > 1)
		{
			// check if previous widths can be used	
			if (lastWidths != null && lastWidths.length == numColumns)
			{
				
				// use previously established widths
				setCurrentColumnWidths(lastWidths);
			}
			else
			{
			    if (totalWidth > 0)
			    {
					// no previous widths or number of columns has changed - need to calculate
					int averageWidth = totalWidth / numColumns;
					int firstWidth = Math.max(averageWidth, 150);
					averageWidth = (totalWidth - firstWidth) / (numColumns - 1);
					averageWidth = Math.max(averageWidth, 80);
					columns[0].setWidth(firstWidth);
					for (int i = 1; i < numColumns; i++)
					{
						
						columns[i].setWidth(averageWidth);
					}
					setLastColumnWidths(getCurrentColumnWidths());
			    }
			}
			tree.setHeaderVisible(true);
		}
		else
		{ 
			
		    if (numColumns == 1) 
		    {	
		    	int width = totalWidth;
		    	if (lastWidths != null && lastWidths.length == 1)
		    	{
		    		width = (totalWidth > lastWidths[0]) ? totalWidth : lastWidths[0];
		    	}
		    	
		    	
		    	int maxWidth = provider.getMaxCharsInColumnZero() * _charWidth;
		    	if (maxWidth > width)
		    	{
		    		width = maxWidth;
		    	}
		    	
		        if (width > 0)
		        {
		            columns[0].setWidth(width);
		        }
		        tree.setHeaderVisible(false);
		    }
		}
	}

	public int[] getCurrentColumnWidths()
	{
		return new int[0];
	}

	public void setCurrentColumnWidths(int[] widths)
	{		
	}

	public int[] getLastColumnWidths()
	{
		return _lastWidths;
	}

	public void setLastColumnWidths(int[] widths)
	{
		_lastWidths = widths;
	}


	protected void initDragAndDrop()
	{
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] dragtransfers = new Transfer[] { PluginTransfer.getInstance(), TextTransfer.getInstance(), FileTransfer.getInstance(),  EditorInputTransfer.getInstance()};
		Transfer[] droptransfers = new Transfer[] { PluginTransfer.getInstance(), TextTransfer.getInstance(), FileTransfer.getInstance(), EditorInputTransfer.getInstance()};
		
		addDragSupport(ops, dragtransfers, new SystemViewDataDragAdapter(this));
		addDropSupport(ops | DND.DROP_DEFAULT, droptransfers, new SystemViewDataDropAdapter(this));
	}
	/**
	 * Used to asynchronously update the view whenever properties change.
	 */
	public void systemResourceChanged(ISystemResourceChangeEvent event)
	{
		try
		{
			Tree tree = getTree();
			boolean isDisposed = tree.isDisposed();
			if (isDisposed)
			{
				dispose();
				return;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		boolean madeChange = false;
		Object parent = event.getParent();
		Object child = event.getSource();
		int eventType = event.getType();
		switch (eventType)
		{
			case ISystemResourceChangeEvents.EVENT_PROPERTY_CHANGE :
			case ISystemResourceChangeEvents.EVENT_PROPERTYSHEET_UPDATE :
				{
					Widget w = findItem(child);

					if (w != null)
					{
						updateItem(w, child);
					}
				}
				break;
			case ISystemResourceChangeEvents.EVENT_ADD :
			case ISystemResourceChangeEvents.EVENT_ADD_RELATIVE :
				{
					boolean addingConnection = (child instanceof IHost);
					if (_objectInput instanceof ISystemRegistry && addingConnection)
					{
						SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();

						if (provider != null)
						{
							if (!madeChange)
							{
								provider.flushCache();
								madeChange = true;
							}

							computeLayout();
							internalRefresh(_objectInput);
						}
					}
				}
				break;
			case ISystemResourceChangeEvents.EVENT_REFRESH:
				{
			    	Widget w = findItem(parent);
			    	if (w != null)
			    	{
			    	    SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();			    	
			    		if (!madeChange)
						{
							provider.flushCache();
							madeChange = true;
						}
			    		internalRefresh(parent);
			    	}
				}
				break;
			default :
				break;

		}

		if (child == _objectInput || parent == _objectInput)
		{
			SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();

			if (provider != null)
			{
				if (!madeChange)
				{
					//provider.flushCache();
					madeChange = true;
				}

				computeLayout();
				try
				{
					internalRefresh(_objectInput);
				}
				catch (Exception e)
				{
					SystemBasePlugin.logError(e.getMessage());
				}
			}
		}
	}

	/**
	 * This is the method in your class that will be called when a remote resource
	 *  changes. You will be called after the resource is changed.
	 * @see org.eclipse.rse.core.events.ISystemRemoteChangeEvent
	 */
	public void systemRemoteResourceChanged(ISystemRemoteChangeEvent event)
	{
		boolean madeChange = false;
		int eventType = event.getEventType();
		Object remoteResourceParent = event.getResourceParent();
		Object remoteResource = event.getResource();
		//boolean originatedHere = (event.getOriginatingViewer() == this);
		List remoteResourceNames = null;
		if (remoteResource instanceof List)
		{
			remoteResourceNames = (List) remoteResource;
			remoteResource = remoteResourceNames.get(0);
		}
		String remoteResourceParentName = getRemoteResourceAbsoluteName(remoteResourceParent);
		String remoteResourceName = getRemoteResourceAbsoluteName(remoteResource);
		if (remoteResourceName == null)
			return;
		SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();

		switch (eventType)
		{
			// --------------------------
			// REMOTE RESOURCE CHANGED...
			// --------------------------
			case ISystemRemoteChangeEvents.SYSTEM_REMOTE_RESOURCE_CHANGED :
				{
					if (remoteResourceParent == getInput())
					{
						Widget w = findItem(remoteResource);
						if (w != null)
						{
							updateItem(w, remoteResource);
						}

					}
				}
				break;

				// --------------------------
				// REMOTE RESOURCE CREATED...
				// --------------------------
			case ISystemRemoteChangeEvents.SYSTEM_REMOTE_RESOURCE_CREATED :
				{
					String inputResourceName = getRemoteResourceAbsoluteName(getInput());
					if (remoteResourceParentName != null && remoteResourceParentName.equals(inputResourceName))
					{
						if (provider == null)
						{
							return;
						}
						if (!madeChange)
						{
							provider.flushCache();
							madeChange = true;
						}

						refresh();
					}
				}
				break;

				// --------------------------
				// REMOTE RESOURCE DELETED...
				// --------------------------
			case ISystemRemoteChangeEvents.SYSTEM_REMOTE_RESOURCE_DELETED :
				{
					{
						Object dchild = remoteResource;

						ISystemViewElementAdapter dadapt = getViewAdapter(dchild);
						if (dadapt != null)
						{
							ISubSystem dSubSystem = dadapt.getSubSystem(dchild);
							String dkey = dadapt.getAbsoluteName(dchild);

							if (provider != null)
							{
								Object[] children = provider.getChildren(_objectInput);
								for (int i = 0; i < children.length; i++)
								{
									Object existingChild = children[i];
									if (existingChild != null)
									{
										ISystemViewElementAdapter eadapt = getViewAdapter(existingChild);
										ISubSystem eSubSystem = eadapt.getSubSystem(existingChild);

										if (dSubSystem == eSubSystem)
										{
											String ekey = eadapt.getAbsoluteName(existingChild);
											if (ekey.equals(dkey))
											{
												if (!madeChange)
												{
													provider.flushCache();
													madeChange = true;

													// do a full refresh
													refresh();
												}
											}
										}

									}
								}
							}
						}
					}

				}
				break;

				// --------------------------
				// REMOTE RESOURCE RENAMED...
				// --------------------------
			case ISystemRemoteChangeEvents.SYSTEM_REMOTE_RESOURCE_RENAMED :
				{
					String oldName = event.getOldNames()[0]; // right now we're assuming that a rename event is for a single resource
					Object child = event.getResource();

					if (provider != null)
					{
						Object[] previousResults = provider.getCache();
						if (previousResults != null)
						{
							for (int i = 0; i < previousResults.length; i++)
							{
								Object previousResult = previousResults[i];

								if (previousResult == child)
								{
									Widget widget = findItem(previousResult);
									if (widget != null)
									{
										widget.setData(child);
										updateItem(widget, child);
										return;
									}
								}
								else
								{
									String previousName = getViewAdapter(previousResult).getAbsoluteName(previousResult);

									if (previousName != null && previousName.equals(oldName))
									{
										provider.flushCache();
										internalRefresh(_objectInput);
										return;
									}
								}
							}

						}
					}
				}

				break;
		}
	}

	/**
	 * Turn a given remote object reference into a fully qualified absolute name
	 */
	private String getRemoteResourceAbsoluteName(Object remoteResource)
	{
		if (remoteResource == null)
			return null;
		String remoteResourceName = null;
		if (remoteResource instanceof String)
			remoteResourceName = (String) remoteResource;
		else
		{
			ISystemViewElementAdapter ra = getViewAdapter(remoteResource);
			if (ra == null)
				return null;
			remoteResourceName = ra.getAbsoluteName(remoteResource);
		}
		return remoteResourceName;
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		_selectionFlagsUpdated = false;
		   IStructuredSelection sel = (IStructuredSelection)event.getSelection();		
			Object firstSelection = sel.getFirstElement();
			if (firstSelection == null)
			  return;
			
			_selectionFlagsUpdated = false;
			ISystemViewElementAdapter adapter = getViewAdapter(firstSelection);
			if (adapter != null)
			{
			   displayMessage(adapter.getStatusLineText(firstSelection));
			   if ((mouseButtonPressed == LEFT_BUTTON))   
			      adapter.selectionChanged(firstSelection);	
			}  
			else
			  clearMessage();
	}

	public void dispose()
	{
		removeAsListener();

		Composite tree = getTableTree();
		
		boolean isDisposed = tree.isDisposed();
		
		// dispose control if not disposed
		if (!isDisposed) {
			tree.dispose();
		}
	}
	
	/**
	 * Display a message/status on the message/status line
	 */
	public void displayMessage(String msg)
	{
		if (_messageLine != null)
		  _messageLine.setMessage(msg);
	}
	
	/**
	 * Convenience method for retrieving the view adapter for an object's children 
	 */
	public ISystemViewElementAdapter getViewAdapterForContents()
	{
		SystemTableTreeViewProvider provider = (SystemTableTreeViewProvider) getContentProvider();
		if (provider != null)
		{
		Object[] children = provider.getChildren(getInput());
		if (children != null && children.length > 0)
		{
			IAdaptable child = (IAdaptable) children[0];
			return getViewAdapter(child);
		}
		}
		return null;
	}
	
	/**
	 * Clear message/status shown on the message/status line
	 */
	public void clearMessage()
	{
		if (_messageLine != null)
		  _messageLine.clearMessage();
	}
	
	/**
	 * Remove as listener.
	 */
	public void removeAsListener() {
		
		// remove listeners
		removeSelectionChangedListener(this);
		ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
		sr.removeSystemResourceChangeListener(this);
		sr.removeSystemRemoteChangeListener(this);

		// for debugging
		//Composite tree = getTableTree();
		//boolean isDisposed = tree.isDisposed();
	}

	

	/**
	  * Rather than pre-defining this common action we wait until it is first needed,
	  *  for performance reasons.
	  */
	protected PropertyDialogAction getPropertyDialogAction()
	{
		if (_propertyDialogAction == null)
		{
			_propertyDialogAction = new PropertyDialogAction(new SameShellProvider(getShell()), this);
			//propertyDialogAction.setToolTipText(" "); 
		}
		_propertyDialogAction.selectionChanged(getSelection());
		return _propertyDialogAction;
	}

	/**
	 * Return the select All action
	 */
	protected IAction getSelectAllAction()
	{
		if (_selectAllAction == null)
			_selectAllAction = new SystemCommonSelectAllAction(getShell(), this, this);
		return _selectAllAction;
	}

	/**
	 * Rather than pre-defined this common action we wait until it is first needed,
	 *  for performance reasons.
	 */
	protected IAction getRenameAction()
	{
		if (_renameAction == null)
			_renameAction = new SystemCommonRenameAction(getShell(), this);
		return _renameAction;
	}
	/**
	 * Rather than pre-defined this common action we wait until it is first needed,
	 *  for performance reasons.
	 */
	protected IAction getDeleteAction()
	{
		if (_deleteAction == null)
			_deleteAction = new SystemCommonDeleteAction(getShell(), this);
		return _deleteAction;
	}

	/**
	* Return the refresh action
	*/
	protected IAction getRefreshAction()
	{
		if (_refreshAction == null)
			_refreshAction = new SystemRefreshAction(getShell());
		return _refreshAction;
	}
	/*
	 * Get the common "Open to->" action for opening a new Remote System Explorer view,
	 *  scoped to the currently selected object.
	 *
	protected SystemCascadingOpenToAction getOpenToAction()
	{
		if (openToAction == null)
		  openToAction = new SystemCascadingOpenToAction(getShell(),getWorkbenchWindow());
		return openToAction;
	} NOT USED YET */
	/**
	 * Get the common "Open to->" action for opening a new Remote System Explorer view,
	 *  scoped to the currently selected object.
	 */
	protected SystemOpenExplorerPerspectiveAction getOpenToPerspectiveAction()
	{
		if (_openToPerspectiveAction == null)
		{
			IWorkbench desktop = PlatformUI.getWorkbench();
			IWorkbenchWindow win = desktop.getActiveWorkbenchWindow();

			_openToPerspectiveAction = new SystemOpenExplorerPerspectiveAction(getShell(), win);
		}
		//getWorkbenchWindow());
		return _openToPerspectiveAction;
	}

	protected SystemShowInTableAction getShowInTableAction()
	{
		if (_showInTableAction == null)
		{
			_showInTableAction = new SystemShowInTableAction(getShell());
		}
		//getWorkbenchWindow());
		return _showInTableAction;
	}

	public Shell getShell()
	{
		return getTableTree().getShell();
	}

	/**
	 * Required method from ISystemDeleteTarget.
	 * Decides whether to even show the delete menu item.
	 * Assumes scanSelections() has already been called
	 */
	public boolean showDelete()
	{
		if (!_selectionFlagsUpdated)
			scanSelections();
		return _selectionShowDeleteAction;
	}
	/**
	 * Required method from ISystemDeleteTarget
	 * Decides whether to enable the delete menu item. 
	 * Assumes scanSelections() has already been called
	 */
	public boolean canDelete()
	{
		if (!_selectionFlagsUpdated)
			scanSelections();
		return _selectionEnableDeleteAction;
	}

	/*
	 * Required method from ISystemDeleteTarget
	 */
	public boolean doDelete(IProgressMonitor monitor)
	{
		ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		Iterator elements = selection.iterator();
		//int selectedCount = selection.size();
		//Object multiSource[] = new Object[selectedCount];
		//int idx = 0;
		Object element = null;
		//Object parentElement = getSelectedParent();
		ISystemViewElementAdapter adapter = null;
		boolean ok = true;
		boolean anyOk = false;
		Vector deletedVector = new Vector();
		try
		{
			while (ok && elements.hasNext())
			{
				element = elements.next();
				//multiSource[idx++] = element;
				adapter = getViewAdapter(element);
				ok = adapter.doDelete(getShell(), element, monitor);
				if (ok)
				{
					anyOk = true;
					deletedVector.addElement(element);
				}
			}
		}
		catch (SystemMessageException exc)
		{
			SystemMessageDialog.displayErrorMessage(getShell(), exc.getSystemMessage());
			ok = false;
		}
		catch (Exception exc)
		{
			String msg = exc.getMessage();
			if ((msg == null) || (exc instanceof ClassCastException))
				msg = exc.getClass().getName();
			SystemMessageDialog.displayErrorMessage(getShell(), RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_EXCEPTION_DELETING).makeSubstitution(element, msg));
			ok = false;
		}
		if (anyOk)
		{
			Object[] deleted = new Object[deletedVector.size()];
			for (int idx = 0; idx < deleted.length; idx++)
				deleted[idx] = deletedVector.elementAt(idx);
			if (_selectionIsRemoteObject)
				sr.fireRemoteResourceChangeEvent(ISystemRemoteChangeEvents.SYSTEM_REMOTE_RESOURCE_DELETED, deletedVector, null, null, null, this);
			else
				sr.fireEvent(new org.eclipse.rse.core.events.SystemResourceChangeEvent(deleted, ISystemResourceChangeEvents.EVENT_DELETE_MANY, getInput()));
		}
		return ok;
	}

	// ---------------------------
	// ISYSTEMRENAMETARGET METHODS
	// ---------------------------

	/**
	 * Required method from ISystemRenameTarget.
	 * Decides whether to even show the rename menu item.
	 * Assumes scanSelections() has already been called
	 */
	public boolean showRename()
	{
		if (!_selectionFlagsUpdated)
			scanSelections();
		return _selectionShowRenameAction;
	}
	/**
	 * Required method from ISystemRenameTarget
	 * Decides whether to enable the rename menu item. 
	 * Assumes scanSelections() has already been called
	 */
	public boolean canRename()
	{
		if (!_selectionFlagsUpdated)
			scanSelections();
		return _selectionEnableRenameAction;
	}

	// default implementation
	// in default table, parent is input 
	protected Object getParentForContent(Object element)
	{
		return _objectInput;
	}

	/**
	* Required method from ISystemRenameTarget
	*/
	public boolean doRename(String[] newNames)
	{
		ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		Iterator elements = selection.iterator();
		//int selectedCount = selection.size();
		Object element = null;

		ISystemViewElementAdapter adapter = null;
		ISystemRemoteElementAdapter remoteAdapter = null;
		String oldFullName = null;
		boolean ok = true;
		try
		{
			int nameIdx = 0;
			while (ok && elements.hasNext())
			{
				element = elements.next();
				adapter = getViewAdapter(element);
				Object parentElement = getParentForContent(element);

				remoteAdapter = getRemoteAdapter(element);
				if (remoteAdapter != null)
					oldFullName = remoteAdapter.getAbsoluteName(element);
				// pre-rename
				ok = adapter.doRename(getShell(), element, newNames[nameIdx++], null);
				if (ok)
				{
					if (remoteAdapter != null) {
						sr.fireRemoteResourceChangeEvent(ISystemRemoteChangeEvents.SYSTEM_REMOTE_RESOURCE_RENAMED, element, parentElement, remoteAdapter.getSubSystem(element), new String[]{oldFullName}, this);
					}
					else {
						sr.fireEvent(new org.eclipse.rse.core.events.SystemResourceChangeEvent(element, ISystemResourceChangeEvents.EVENT_RENAME, parentElement));
					}
				}
			}
		}
		catch (SystemMessageException exc)
		{
			SystemMessageDialog.displayErrorMessage(getShell(), exc.getSystemMessage());
			ok = false;
		}
		catch (Exception exc)
		{
			//String msg = exc.getMessage();
			//if ((msg == null) || (exc instanceof ClassCastException))
			//  msg = exc.getClass().getName();
			SystemMessageDialog.displayErrorMessage(getShell(), RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_EXCEPTION_RENAMING).makeSubstitution(element, exc),
			//msg),
			exc);
			ok = false;
		}
		return ok;
	}

	/**
	 * Returns the implementation of ISystemRemoteElementAdapter for the given
	 * object.  Returns null if this object is not adaptable to this.
	 */
	protected ISystemRemoteElementAdapter getRemoteAdapter(Object o)
	{
		ISystemRemoteElementAdapter adapter = null;
		if (!(o instanceof IAdaptable))
			adapter = (ISystemRemoteElementAdapter) Platform.getAdapterManager().getAdapter(o, ISystemRemoteElementAdapter.class);
		else
			adapter = (ISystemRemoteElementAdapter) ((IAdaptable) o).getAdapter(ISystemRemoteElementAdapter.class);
		if ((adapter != null) && (adapter instanceof ISystemViewElementAdapter))
			 ((ISystemViewElementAdapter) adapter).setViewer(this);
		return adapter;
	}

	/**
	 * Returns the implementation of IRemoteObjectIdentifier for the given
	 * object.  Returns null if this object is not adaptable to this.
	 * 
	 * @deprecated 	should use {@link #getViewAdapter(Object)} since
	 *     IRemoteObjectIdentifier is not defined in the adapter factories
	 */
	protected IRemoteObjectIdentifier getRemoteObjectIdentifier(Object o) 
	{
		return (IRemoteObjectIdentifier)((IAdaptable)o).getAdapter(IRemoteObjectIdentifier.class);
	}
	
	/**
	* Return true if select all should be enabled for the given object.
	* For a tree view, you should return true if and only if the selected object has children.
	* You can use the passed in selection or ignore it and query your own selection.
	*/
	public boolean enableSelectAll(IStructuredSelection selection)
	{
		return true;
	}
	/**
	 * When this action is run via Edit->Select All or via Ctrl+A, perform the
	 * select all action. For a tree view, this should select all the children 
	 * of the given selected object. You can use the passed in selected object
	 * or ignore it and query the selected object yourself. 
	 */
	public void doSelectAll(IStructuredSelection selection)
	{

		Composite tree = getTableTree();
		
			Tree theTree = (Tree) tree;
			theTree.setSelection(theTree.getItems());
			TreeItem[] items = theTree.getItems();
			Object[] objects = new Object[items.length];
			for (int idx = 0; idx < items.length; idx++)
				objects[idx] = items[idx].getData();
			fireSelectionChanged(new SelectionChangedEvent(this, new StructuredSelection(objects)));

	}

	public void menuAboutToShow(IMenuManager manager)
	{
		SystemView.createStandardGroups(manager);
		
		  if (!menuListenerAdded)
	   	    {
	   	      if (manager instanceof MenuManager)
	   	      {
	   	      	Menu m = ((MenuManager)manager).getMenu();
	   	      	if (m != null)
	   	      	{
	   	      		menuListenerAdded = true;
	   	      		SystemViewMenuListener ml = new SystemViewMenuListener();
	   	      		if (_messageLine != null)
	   	      		  ml.setShowToolTipText(true, _messageLine);
	   	      		m.addMenuListener(ml);
	   	      	}
	   	      }
	   	    }
		fillContextMenu(manager);
	}

	public ISelection getSelection()
	{
		ISelection selection = super.getSelection();
		if (selection == null || selection.isEmpty())
		{
			// make the selection the parent
			ArrayList list = new ArrayList();
			if (_objectInput != null)
			{
				list.add(_objectInput);
				selection = new StructuredSelection(list);
			}
		}

		return selection;
	}

	public void fillContextMenu(IMenuManager menu) {
		
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		
		boolean allSelectionsFromSameParent = true;
		int selectionCount = selection.size();
		
		
		
		if (selectionCount == 0) // nothing selected
		{
			return;		   		    
		}
		else
		{
			
			if (selectionCount == 1) {
				
				if (selection.getFirstElement() == getInput()) {
					//return;
				}
			}
			
		   if (selectionCount > 1)
		   {
			 allSelectionsFromSameParent = sameParent();
			 
			 if (!allSelectionsFromSameParent)
			 {
				if (selectionHasAncestryRelationship())
				{
					// don't show the menu because actions with
					//  multiple select on objects that are ancestors 
					//  of each other is problematic
					// still create the standard groups
					SystemView.createStandardGroups(menu);
					return;
				}
			 }
		   }
			 
			// Partition into groups...
			SystemView.createStandardGroups(menu);

			// ADD COMMON ACTIONS...

			// COMMON RENAME ACTION...
			if (canRename())
			{
				if (showRename())
					menu.appendToGroup(ISystemContextMenuConstants.GROUP_REORGANIZE, getRenameAction());
			}

			// ADAPTER SPECIFIC ACTIONS   	      
			SystemMenuManager ourMenu = new SystemMenuManager(menu);

			Iterator elements = selection.iterator();
			Hashtable adapters = new Hashtable();
			while (elements.hasNext())
			{
				Object element = elements.next();
				ISystemViewElementAdapter adapter = getViewAdapter(element);
				adapters.put(adapter, element); // want only unique adapters
			}
			Enumeration uniqueAdapters = adapters.keys();
			Shell shell = getShell();
			while (uniqueAdapters.hasMoreElements())
			{
				ISystemViewElementAdapter nextAdapter = (ISystemViewElementAdapter) uniqueAdapters.nextElement();
				nextAdapter.addActions(ourMenu, selection, shell, ISystemContextMenuConstants.GROUP_ADAPTERS);

				if (nextAdapter instanceof AbstractSystemViewAdapter)
				{
		
						AbstractSystemViewAdapter aVA = (AbstractSystemViewAdapter)nextAdapter;
						// add remote actions
						aVA.addCommonRemoteActions(ourMenu, selection, shell, ISystemContextMenuConstants.GROUP_ADAPTERS);
						
						// add dynamic menu popups
						aVA.addDynamicPopupMenuActions(ourMenu, selection, shell,  ISystemContextMenuConstants.GROUP_ADDITIONS);
				}
			}

			// wail through all actions, updating shell and selection
			IContributionItem[] items = menu.getItems();
			for (int idx = 0; idx < items.length; idx++)
			{
				if ((items[idx] instanceof ActionContributionItem) && (((ActionContributionItem) items[idx]).getAction() instanceof ISystemAction))
				{
					ISystemAction item = (ISystemAction) (((ActionContributionItem) items[idx]).getAction());
					item.setInputs(getShell(), this, selection);
				}
				else if (items[idx] instanceof SystemSubMenuManager)
				{
					SystemSubMenuManager item = (SystemSubMenuManager) items[idx]; 	
					item.setInputs(getShell(), this, selection);
				}
			}

			// COMMON DELETE ACTION...
			if (canDelete() && showDelete())
			{
				//menu.add(getDeleteAction());
				menu.appendToGroup(ISystemContextMenuConstants.GROUP_REORGANIZE, getDeleteAction());
				((ISystemAction) getDeleteAction()).setInputs(getShell(), this, selection);
				menu.add(new Separator());
			}

			// PROPERTIES ACTION...
			// This is supplied by the system, so we pretty much get it for free. It finds the
			// registered propertyPages extension points registered for the selected object's class type.
			//propertyDialogAction.selectionChanged(selection);		  

			PropertyDialogAction pdAction = getPropertyDialogAction();
			if (pdAction.isApplicableForSelection())
			{

				menu.appendToGroup(ISystemContextMenuConstants.GROUP_PROPERTIES, pdAction);
			}
			// OPEN IN NEW PERSPECTIVE ACTION... if (fromSystemViewPart && showOpenViewActions())
			if (!_selectionIsRemoteObject)
			{
				//SystemCascadingOpenToAction openToAction = getOpenToAction();
				SystemOpenExplorerPerspectiveAction openToPerspectiveAction = getOpenToPerspectiveAction();
				SystemShowInTableAction showInTableAction = getShowInTableAction();
				openToPerspectiveAction.setSelection(selection);
				showInTableAction.setSelection(selection);
				//menu.appendToGroup(ISystemContextMenuConstants.GROUP_OPEN, openToAction.getSubMenu());
				menu.appendToGroup(ISystemContextMenuConstants.GROUP_OPEN, openToPerspectiveAction);
				menu.appendToGroup(ISystemContextMenuConstants.GROUP_OPEN, showInTableAction);

			}


		}
	}
	
	/**
	 * This is called to ensure all elements in a multiple-selection have the same parent in the
	 *  tree viewer. If they don't we automatically disable all actions. 
	 * <p>
	 * Designed to be as fast as possible by going directly to the SWT widgets
	 */
	public boolean sameParent()
	{
		boolean same = true;
		
		Tree tree = getTree();
		
		TreeItem[] items = tree.getSelection();
		
		if ((items == null) || (items.length ==0)) {
		  return true;
		}
		
		TreeItem prevParent = null;
		TreeItem currParent = null;
		
		for (int idx = 0; idx < items.length; idx++)
		{
		   currParent = items[idx].getParentItem();
		             
		   if ((idx>0) && (currParent != prevParent)) {
			 same = false;
			 break;
		   }
		   else
		   {
			 prevParent = currParent;  
		   }
		}
		return same;
	}
	
	private boolean selectionHasAncestryRelationship() {
		Tree tree = getTree();
		
		TreeItem[] items = tree.getSelection();

		for (int idx=0; idx<items.length; idx++)
		{
			TreeItem item = items[idx];
			
			for (int c=0; c < items.length; c++)
			{
				if (item != items[c])
				{					
					if (isAncestorOf(item, items[c], false))
					{
						return true;
					}
				}
			}
		}
		return false;		
	}
	
	/**
	 * Returns whether an item is an ancestor of another item. The ancestor can be direct or indirect.
	 * @param container the item which might be an ancestor.
	 * @param item the child.
	 * @param direct <code>true</code> if the container must be a direct ancestor of the child item,
	 * 				 <code>false</code> otherwise.
	 * @return <code>true</code> if there is an ancestry relationship, <code>false</code> otherwise.
	 */
	private boolean isAncestorOf(TreeItem container, TreeItem item, boolean direct)
	{
		TreeItem[] children = null;
		
		// does not have to be a direct ancestor
		if (!direct) {
			// get the children of the container's parent, i.e. the container's siblings
			// as well as itself
			TreeItem parent = container.getParentItem();
			
			// check if parent is null
			// parent is null if the container is a root item
			if (parent != null) {
				children = parent.getItems();
			}
			else {
				children = getTree().getItems();
			}
		}
		// must be a direct ancestor
		else {
			// get the children of the container
			children = container.getItems();
		}
			
		// go through all the children
		for (int i = 0; i < children.length; i++) {

			TreeItem child = children[i];

			// if one of the children matches the child item, return true
			if (child == item && direct) {
				return true;
			}
			// otherwise, go through children, and see if any of those are ancestors of
			// the child item 
			else if (child.getItemCount() > 0) {
				
				// we check for direct ancestry
				if (isAncestorOf(child, item, true)) {
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * --------------------------------------------------------------------------------
	 * For many actions we have to walk the selection list and examine each selected
	 *  object to decide if a given common action is supported or not.
	 * <p>
	 * Walking this list multiple times while building the popup menu is a performance
	 *  hit, so we have this common method that does it only once, setting instance
	 *  variables for all of the decisions we are in interested in.
	 * --------------------------------------------------------------------------------
	 */
	protected void scanSelections()
	{
		// initial these variables to true. Then if set to false even once, leave as false always...
		_selectionShowRefreshAction = true;
		_selectionShowOpenViewActions = true;
		_selectionShowDeleteAction = true;
		_selectionShowRenameAction = true;
		_selectionEnableDeleteAction = true;
		_selectionEnableRenameAction = true;
		_selectionIsRemoteObject = true;
		_selectionFlagsUpdated = true;

		IStructuredSelection selection = (IStructuredSelection) getSelection();
		Iterator elements = selection.iterator();
		while (elements.hasNext())
		{
			Object element = elements.next();
			ISystemViewElementAdapter adapter = getViewAdapter(element);

			if (_selectionShowRefreshAction)
				_selectionShowRefreshAction = adapter.showRefresh(element);

			if (_selectionShowOpenViewActions)
				_selectionShowOpenViewActions = adapter.showOpenViewActions(element);

			if (_selectionShowDeleteAction)
				_selectionShowDeleteAction = adapter.showDelete(element);

			if (_selectionShowRenameAction)
				_selectionShowRenameAction = adapter.showRename(element);

			if (_selectionEnableDeleteAction)
				_selectionEnableDeleteAction = _selectionShowDeleteAction && adapter.canDelete(element);
			//System.out.println("ENABLE DELETE SET TO " + selectionEnableDeleteAction);

			if (_selectionEnableRenameAction)
				_selectionEnableRenameAction = _selectionShowRenameAction && adapter.canRename(element);

			if (_selectionIsRemoteObject)
				_selectionIsRemoteObject = (getRemoteAdapter(element) != null);
		}

	}


	public void positionTo(String name)
	{
		ArrayList selectedItems = new ArrayList();
		Tree tree = getTree();
		TreeItem topItem = null;
		for (int i = 0; i < tree.getItemCount(); i++)
		{
			TreeItem item = tree.getItem(i);
			Object data = item.getData();
			if (data instanceof IAdaptable)
			{
				ISystemViewElementAdapter adapter = getViewAdapter(data);
				String itemName = adapter.getName(data);

				if (StringCompare.compare(name, itemName, false))
				{
					if (topItem == null)
					{
						topItem = item;
					}
					selectedItems.add(item);
				}
			}
		}

		if (selectedItems.size() > 0)
		{
			TreeItem[] tItems = new TreeItem[selectedItems.size()];
			for (int i = 0; i < selectedItems.size(); i++)
			{
				tItems[i] = (TreeItem) selectedItems.get(i);
			}

			tree.setSelection(tItems);
			tree.setTopItem(topItem);
			setSelection(getSelection(), true);
		}
	}


	protected void handleKeyPressed(KeyEvent event)
	{
		if ((event.character == SWT.DEL) && (event.stateMask == 0) && (((IStructuredSelection) getSelection()).size() > 0))
		{
			scanSelections();
			if (showDelete() && canDelete())
			{
				SystemCommonDeleteAction dltAction = (SystemCommonDeleteAction) getDeleteAction();
				dltAction.setShell(getShell());
				dltAction.setSelection(getSelection());
				dltAction.setViewer(this);
				dltAction.run();
			}
		}
	}
	
	/**
	 * Overridden so that we can pass a wrapper IContextObject into the provider to get children instead 
	 * of the model object, itself
	 */
	protected void createChildren(final Widget widget) 
	{
		if (widget instanceof TreeItem)
		{
		final Item[] tis = getChildren(widget);
		if (tis != null && tis.length > 0) {
			Object data = tis[0].getData();
			if (data != null) {
				return; // children already there!
			}
		}

		BusyIndicator.showWhile(widget.getDisplay(), new Runnable() {
			public void run() {
				// fix for PR 1FW89L7:
				// don't complain and remove all "dummies" ...
				if (tis != null) {
					for (int i = 0; i < tis.length; i++) {
						if (tis[i].getData() != null) {
							disassociate(tis[i]);
							Assert.isTrue(tis[i].getData() == null,
									"Second or later child is non -null");//$NON-NLS-1$

						}
						tis[i].dispose();
					}
				}
				Object d = widget.getData();
				if (d != null) 
				{
					Object parentElement = getContextObject((TreeItem)widget);
					Object[] children = getSortedChildren(parentElement);
					if (children != null)
					{
						for (int i = 0; i < children.length; i++) 
						{	
							createTreeItem(widget, children[i], -1);
						}
					}
				}
			}

		});
		}
		else
		{
			super.createChildren(widget);
		}
	}
	
	
	/**
	 * Get the containing filter reference for an item
	 * @param item the item to get the filter reference for
	 * @return the filter reference
	 */
	public ISystemFilterReference getContainingFilterReference(TreeItem item)
	{
		Object data = item.getData();
		if (data instanceof ISystemFilterReference)
		{
			return (ISystemFilterReference)data;
		}
		else
		{
			TreeItem parent = item.getParentItem();
			if (parent != null)
			{
				return getContainingFilterReference(parent);
			}
			else				
			{
				Object input = getInput();
				if (input instanceof ISystemFilterReference)
				{
					return (ISystemFilterReference)input;
				}
				else
				{
					return null;
				}
			}
		}
	}
	
	/**
	 * Get the containing subsystem from an item
	 * @param item the item to get the subsystem for
	 * @return the subsystem
	 */
	public ISubSystem getContainingSubSystem(TreeItem item)
	{
		Object data = item.getData();
		if (data instanceof ISubSystem)
		{
			return (ISubSystem)data;
		}
		else
		{
			TreeItem parent = item.getParentItem();
			if (parent != null)
			{
				return getContainingSubSystem(parent);
			}
			else				
			{
				Object input = getInput();
				if (input instanceof ISubSystem)
				{
					return (ISubSystem)input;
				}
				else
				{
					return null;
				}
			}
		}
	}
	
	/**
	 * Get the context object from a tree item
	 * @param item the item to get the context for
	 * @return the context object
	 */
	public IContextObject getContextObject(TreeItem item)
	{
		Object data = item.getData();
		ISystemFilterReference filterReference = getContainingFilterReference(item);
		if (filterReference != null)
		{
			return new ContextObject(data, filterReference.getSubSystem(), filterReference);
		}
		else
		{
			ISubSystem subSystem = getContainingSubSystem(item);
			if (subSystem != null)
			{
				return new ContextObject(data, subSystem);
			}
			else
			{				
				return new ContextObject(data);
			}
		}
	}
	
	/**
	 * Overrides the standard viewer method to get the model object from the context object
	 */
	public void add(Object parentElementOrTreePath, Object[] childElements) {
		Assert.isNotNull(parentElementOrTreePath);
		assertElementsNotNull(childElements);
		
		if (parentElementOrTreePath instanceof IContextObject)
		{
			parentElementOrTreePath = ((IContextObject)parentElementOrTreePath).getModelObject();
		}
		super.add(parentElementOrTreePath, childElements);
	}
}
