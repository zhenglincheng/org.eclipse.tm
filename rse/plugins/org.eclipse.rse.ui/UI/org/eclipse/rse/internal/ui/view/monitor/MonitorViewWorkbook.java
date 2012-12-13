/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * Kevin Doyle (IBM) - [177587] createTabItem sets the wrapped selection provider
 * Kevin Doyle 		(IBM)		 - [242431] Register a new unique context menu id, so contributions can be made to all our views
 * Zhou Renjian     (Kortide)    - [282239] Monitor view does not update icon according to connection status
 * David McKnight   (IBM)        - [372674] Enhancement - Preserve state of Remote Monitor view
 *******************************************************************************/

package org.eclipse.rse.internal.ui.view.monitor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.rse.ui.view.SystemTableView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;



/**
 * This is the desktop view wrapper of the System View viewer.
 */
public class MonitorViewWorkbook extends Composite
{


	private CTabFolder _folder;
	private SystemMonitorViewPart _viewPart;

	public MonitorViewWorkbook(Composite parent, SystemMonitorViewPart viewPart)
	{
		super(parent, SWT.NONE);

		_folder = new CTabFolder(this, SWT.NULL);
		_folder.setLayout(new TabFolderLayout());
		_folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		setLayout(new FillLayout());
		_viewPart = viewPart;
	}

	public void dispose()
	{
		
		//if (!_folder.isDisposed())
		{
			for (int i = 0; i < _folder.getItemCount(); i++)
			{
				CTabItem item = _folder.getItem(i);
				if (!item.isDisposed())
				{
					MonitorViewPage page = (MonitorViewPage) item.getData();
					page.dispose();
				}
			}
			_folder.dispose();
		}
		super.dispose();
	}

	public CTabFolder getFolder()
	{
		return _folder;
	}

	public void remove(Object root)
	{
		for (int i = 0; i < _folder.getItemCount(); i++)
		{
			CTabItem item = _folder.getItem(i);
			if (!item.isDisposed())
			{
				MonitorViewPage page = (MonitorViewPage) item.getData();

				if (page != null && root == page.getInput())
				{
					item.dispose();
					page.dispose();

					page = null;
					item = null;
				
					_folder.redraw();

					return;
				}
			}
		}
	}
	
	public void removeDisconnected()
	{
		for (int i = 0; i < _folder.getItemCount(); i++)
		{
			CTabItem item = _folder.getItem(i);
			if (!item.isDisposed())
			{
				MonitorViewPage page = (MonitorViewPage) item.getData();
				if (page != null)
				{
					IAdaptable input = (IAdaptable)page.getInput();
					ISystemViewElementAdapter adapter = (ISystemViewElementAdapter)input.getAdapter(ISystemViewElementAdapter.class);
					if (adapter != null)
					{
						ISubSystem subSystem = adapter.getSubSystem(input);
						if (subSystem != null)
						{
							if (!subSystem.isConnected())
							{
								item.dispose();
								page.dispose();
	
								page = null;
								item = null;
							
								_folder.redraw();
							}
						}
					}
				}
			}
		}
	}


	
	public CTabItem getSelectedTab()
	{
		if (_folder.getItemCount() > 0)
		{
			int index = _folder.getSelectionIndex();
			CTabItem item = _folder.getItem(index);
			return item;
		}

		return null;
	}

	public MonitorViewPage getCurrentTabItem()
	{
		if (_folder.getItemCount() > 0)
		{
			int index = _folder.getSelectionIndex();
			if (index >= 0){
				CTabItem item = _folder.getItem(index);
				return (MonitorViewPage) item.getData();
			}
		}
		return null;
	}

	public void showCurrentPage()
	{
	    _folder.setFocus();
	}

	public Object getInput()
	{
	    MonitorViewPage page = getCurrentTabItem();
		if (page != null)
		{
		    page.setFocus();
			return page.getInput();
		}

		return null;
	}

	public SystemTableView getViewer()
	{
		if (getCurrentTabItem() != null)
		{
			return getCurrentTabItem().getViewer();
		}
		return null;
	}

	public void addItemToMonitor(IAdaptable root, boolean createTab)
	{
		if (!_folder.isDisposed())
		{
			for (int i = 0; i < _folder.getItemCount(); i++)
			{
				CTabItem item = _folder.getItem(i);
				MonitorViewPage page = (MonitorViewPage) item.getData();
				if (page != null && root == page.getInput())
				{
					page.getViewer().refresh();

					if (_folder.getSelectionIndex() != i)
					{
						_folder.setSelection(item);
					}
					updateActionStates();
					//page.setFocus();
					return;
				}
			}

			if (createTab)
			{
				// never shown this, so add it
				createTabItem(root);
			}
		}
	}

	private void createTabItem(IAdaptable root)
	{
		MonitorViewPage monitorViewPage = new MonitorViewPage(_viewPart);

		CTabItem titem = new CTabItem(_folder, SWT.CLOSE);
		setTabTitle(root, titem);
 
		titem.setData(monitorViewPage);
		titem.setControl(monitorViewPage.createTabFolderPage(_folder, _viewPart.getEditorActionHandler()));
		_folder.setSelection(titem );

		monitorViewPage.setInput(root);

		SystemTableView viewer = monitorViewPage.getViewer();
		if (_viewPart != null)
		{
			_viewPart.setActiveViewerSelectionProvider(viewer);
			_viewPart.getSite().registerContextMenu(viewer.getContextMenuManager(), viewer);
			_viewPart.getSite().registerContextMenu(ISystemContextMenuConstants.RSE_CONTEXT_MENU, viewer.getContextMenuManager(), viewer);
		}
		monitorViewPage.setFocus();
		
		titem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Object source = e.getSource();
				if (source instanceof CTabItem) {
					CTabItem currentItem = (CTabItem) source;
					Object data = currentItem.getData();
					if (data instanceof MonitorViewPage) {
						MonitorViewPage page = (MonitorViewPage)data;						
						page.setPollingEnabled(false); // stop polling
						page.dispose();
					}
					updateActionStates();
				}				
			}

		});
	}

	private void setTabTitle(IAdaptable root, CTabItem titem)
	{
		ISystemViewElementAdapter va = (ISystemViewElementAdapter) root.getAdapter(ISystemViewElementAdapter.class);
		if (va != null)
		{
			titem.setText(va.getName(root));
			titem.setImage(va.getImageDescriptor(root).createImage());
		}
	}

	public void setInput(IAdaptable root)
	{
		for (int i = 0; i < _folder.getItemCount(); i++)
		{
			CTabItem item = _folder.getItem(i);
			MonitorViewPage page = (MonitorViewPage) item.getData();
			if (root == page.getInput())
			{
				_folder.setSelection(i);
				page.getViewer().refresh();
				return;
			}
		}
	}

	public void updateActionStates()
	{
		for (int i = 0; i < _folder.getItemCount(); i++)
		{
			CTabItem item = _folder.getItem(i);
			if (!item.isDisposed())
			{
				MonitorViewPage page = (MonitorViewPage) item.getData();
				if (page != null)
				{
					page.updateActionStates();
				}
			}
		}
	}
	
	// Fix bug#282239: Monitor view does not update icon according to connection status 
	public void updateTitleIcon(IAdaptable root)
	{
		for (int i = 0; i < _folder.getItemCount(); i++)
		{
			CTabItem item = _folder.getItem(i);
			if (!item.isDisposed())
			{
				MonitorViewPage page = (MonitorViewPage) item.getData();
				if (page != null && page.getInput() == root)
				{
					setTabTitle(root, item);
					break;
				}
			}
		}
	}
}
