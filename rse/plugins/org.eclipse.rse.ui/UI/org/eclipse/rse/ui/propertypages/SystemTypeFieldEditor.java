/********************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others. All rights reserved.
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
 * David Dykstal (IBM) - moved SystemPreferencesManager to a new package
 *                     - created and used RSEPreferencesManager
 * Martin Oberhuber (Wind River) - [184095] Replace systemTypeName by IRSESystemType
 * Martin Oberhuber (Wind River) - [177523] Unify singleton getter methods
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Martin Oberhuber (Wind River) - [186779] Fix IRSESystemType.getAdapter()
 * Martin Oberhuber (Wind River) - [196963][181939] avoid subsystem plugin activation just for enablement checking
 * David Dykstal (IBM) - [231943] make "true" and "false" translatable
 * David McKnight   (IBM)        - [210149] [accessibility] Remote Systems Preference page not fully accessible with keyboard
 ********************************************************************************/

package org.eclipse.rse.ui.propertypages;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.RSEPreferencesManager;
import org.eclipse.rse.core.subsystems.ISubSystemConfigurationProxy;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.ui.RSESystemTypeAdapter;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemWidgetHelpers;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;


/**
 * This is a field type editor for the Remote Systems preference page,
 *   used for setting system type preferences.
 */
public class SystemTypeFieldEditor extends FieldEditor
	implements ICellModifier, ITableLabelProvider, IStructuredContentProvider
{
	private Table table;
	private GridData tableData;
	private TableViewer tableViewer;
	private CellEditor enabledCellEditor, userIdCellEditor;
    private static final char KEYVALUE_DELIMITER='=';
    private static final char KEYVALUEPAIR_DELIMITER=';';
    public static final char EACHVALUE_DELIMITER='+';
    private Hashtable keyValues;
    private IRSESystemType[] systemTypes;

    private boolean enabledStateChanged = false;

	private static final int COLUMN_NAME = 0;
	private static final int COLUMN_ENABLED = 1;
	private static final int COLUMN_USERID = 2;
	private static final String P_NAME = "name"; //$NON-NLS-1$
	private static final String P_ENABLED = "enabled"; //$NON-NLS-1$
	private static final String P_DESC = "desc"; //$NON-NLS-1$
	private static final String P_USERID = "userid"; //$NON-NLS-1$
	private static final String columnHeaders[] =
	{
		   SystemResources.RESID_PREF_SYSTYPE_COLHDG_NAME,
		   SystemResources.RESID_PREF_SYSTYPE_COLHDG_ENABLED,
		   SystemResources.RESID_PREF_SYSTYPE_COLHDG_USERID,
		   SystemResources.RESID_PREF_SYSTYPE_COLHDG_DESC
	};
	private static ColumnLayoutData columnLayouts[] =
	{
		new ColumnWeightData(20,80,true),
		new ColumnWeightData(20,15,true),
		new ColumnWeightData(20,100,true),
		new ColumnWeightData(55,280,true)
	};
	// give each column a property value to identify it
	private static final String[] tableColumnProperties =
	{
		P_NAME, P_ENABLED, P_USERID, P_DESC
	};

	private static final boolean[] enabledStates = {Boolean.TRUE.booleanValue(), Boolean.FALSE.booleanValue()};
    private static final String[] enabledStateStrings = {SystemResources.SystemTypeFieldEditor_true, SystemResources.SystemTypeFieldEditor_false};
    
    
    private TableKeyListener _keyListener;
	private class TableKeyListener implements Listener {
		public void handleEvent(Event e){
			if (e.character == SWT.SPACE){ // space character toggles				
				TableItem[] selList = table.getSelection();
				IRSESystemType type = getSelectedItem(selList);
				Integer value = new Integer(type.isEnabled() ?  1: 0);				
				modify(selList[0], P_ENABLED, value);			
			}
			else if (e.keyCode == SWT.F2) {
				TableItem[] selList = table.getSelection();
				IRSESystemType type = getSelectedItem(selList);
				tableViewer.editElement(type, COLUMN_USERID);
			}		
		}
		
		private IRSESystemType getSelectedItem(TableItem[] selList){
			if (selList.length == 1){
				TableItem sel = selList[0];
				Object data = sel.getData();
				IRSESystemType row = (IRSESystemType)data;
				return row;
			}	
			return null;
		}
	};
    
    
	/**
	 * Constructor
	 *
	 * @param name the name of the preference this field editor works on
	 * @param labelText the label text of the field editor
	 * @param parent the parent of the field editor's control
	 */
	public SystemTypeFieldEditor(String name, String labelText, Composite parent)
	{
		super(name, labelText, parent);		
	}

	public void dispose() {
		table.removeListener(SWT.KeyDown, _keyListener);
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
	 */
	protected void adjustForNumColumns(int numColumns)
	{
		((GridData)table.getLayoutData()).horizontalSpan = numColumns;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doFillIntoGrid(org.eclipse.swt.widgets.Composite, int)
	 */
	protected void doFillIntoGrid(Composite parent, int numColumns)
	{
        table = createTable(parent);
        ((GridData)table.getLayoutData()).horizontalSpan = numColumns;
        tableViewer = new TableViewer(table);
        createColumns();
	    tableViewer.setColumnProperties(tableColumnProperties);
	    tableViewer.setCellModifier(this);
	    CellEditor editors[] = new CellEditor[columnHeaders.length];
	    userIdCellEditor = new TextCellEditor(table);
	    enabledCellEditor = new ComboBoxCellEditor(table, enabledStateStrings, SWT.READ_ONLY); // DWD should consider a checkbox for this.
	    editors[COLUMN_USERID] = userIdCellEditor;
	    editors[COLUMN_ENABLED] = enabledCellEditor;
	    tableViewer.setCellEditors(editors);

        tableViewer.setLabelProvider(this);
        tableViewer.setContentProvider(this);
        tableViewer.setInput(new Object());
                
		_keyListener = new TableKeyListener();
		table.addListener(SWT.KeyDown, _keyListener);
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doLoad()
	 */
	protected void doLoad()
	{
    	if (systemTypes == null)
    		systemTypes = getSystemTypes(false);

		String value = RSEPreferencesManager.getSystemTypeValues();
		keyValues = null;

	    if ((value == null) || (value.length() == 0))
	    {
    		keyValues = new Hashtable();
	    }
	    else
	    {
	    	keyValues = parseString(value);
	    }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
	 */
	protected void doLoadDefault()
	{
		// when Defaults button pressed, we re-read the system types from disk
		systemTypes = getSystemTypes(true);
		keyValues.clear();
		tableViewer.refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doStore()
	 */
	protected void doStore()
	{
		if (systemTypes != null)
		{
			String s = createString(keyValues);

			if (s != null) {
				RSEPreferencesManager.setSystemTypeValues(s);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#getNumberOfControls()
	 */
	public int getNumberOfControls()
	{
		return 1;
	}

	/*
	 * @see FieldEditor.setEnabled(boolean,Composite).
	 */
	public void setEnabled(boolean enabled, Composite parent)
	{
		if (table != null)
			table.setEnabled(enabled);
	}

	/*
	 * @see FieldEditor.isValid().
	 */
	public boolean isValid()
	{
		return true;
	}


	// ----------------
	// local methods...
	// ----------------

	private Table createTable(Composite parent)
    {
	   table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
  	   table.setLinesVisible(true);
	   tableData = new GridData();
	   tableData.horizontalAlignment = GridData.FILL;
	   tableData.grabExcessHorizontalSpace = true;
	   tableData.widthHint = 410;
	   tableData.heightHint= 30;
	   tableData.verticalAlignment = GridData.FILL;
	   tableData.grabExcessVerticalSpace = true;
	   table.setLayoutData(tableData);

	   SystemWidgetHelpers.setHelp(table, RSEUIPlugin.HELPPREFIX+"systype_preferences"); //$NON-NLS-1$
	   return table;
    }

    private void createColumns()
    {
        TableLayout layout = new TableLayout();
        table.setLayout(layout);
        table.setHeaderVisible(true);
	    for (int i = 0; i < columnHeaders.length; i++)
	    {
		   layout.addColumnData(columnLayouts[i]);
		   TableColumn tc = new TableColumn(table, SWT.NONE,i);
		   tc.setResizable(columnLayouts[i].resizable);
		   tc.setText(columnHeaders[i]);
	    }
    }

	/**
	 * Parse out list of key-value pairs into a hashtable
	 */
	private static Hashtable parseString(String allvalues)
	{
		StringTokenizer tokens = new StringTokenizer(allvalues, makeString(KEYVALUE_DELIMITER, KEYVALUEPAIR_DELIMITER));
		Hashtable keyValues = new Hashtable(10);
		int count = 0;
		String token1=null;
		String token2=null;
		while (tokens.hasMoreTokens())
		{
			count++;
			if ((count % 2) == 0) // even number
			{
			  token2 = tokens.nextToken();
			  keyValues.put(token1, token2);
			}
			else
			  token1 = tokens.nextToken();
		}
		return keyValues;
	}

	private static String makeString(char charOne, char charTwo)
	{
		StringBuffer s = new StringBuffer(2);
		s.append(charOne);
		s.append(charTwo);
		return s.toString();
	}

	/**
	 * Convert hashtable of key-value pairs into a single string
	 */
	public static String createString(Hashtable keyValues)
	{
		if (keyValues == null)
			return null;
		Enumeration keys = keyValues.keys();
		StringBuffer sb = new StringBuffer();
		while (keys.hasMoreElements())
		{
			String key = (String)keys.nextElement();
			String value = (String)keyValues.get(key);
			if ((value != null) && (value.length()>0))
			{
				sb.append(key);
				sb.append(KEYVALUE_DELIMITER);
				sb.append(value);
				sb.append(KEYVALUEPAIR_DELIMITER);
			}
		}

		return sb.toString();
	}

	/**
	 * Retrieve an array of currently known system types.
	 * @param restoreDefaults restore the default values for the system types
	 * @return The list of system types known to be in existence
	 */
	private IRSESystemType[] getSystemTypes(boolean restoreDefaults) {
		IRSESystemType[] types = RSECorePlugin.getTheCoreRegistry().getSystemTypes();
		ArrayList list = new ArrayList();
		if (systemTypes == null || restoreDefaults) {
			//Only get system types with at least one configuration registered.
			//Do not consider enabled state according to the system type
			//adapter, because this field editor is used for the preference
			//page where enablement is made.
			ISubSystemConfigurationProxy[] proxies = RSECorePlugin.getTheSystemRegistry().getSubSystemConfigurationProxies();
			for (int i = 0; i < types.length; i++) {
				for (int j=0; j<proxies.length; j++) {
					if (proxies[j].appliesToSystemType(types[i])) {
						list.add(types[i]);
						break;
					}
				}
			}
		}
		types = new IRSESystemType[list.size()];
		for (int i = 0; i < list.size(); i++) {
			types[i] = (IRSESystemType) (list.get(i));
		}
		return types;
	}

    // ------------------------
    // ICellModifier methods...
    // ------------------------

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
	 */
	public boolean canModify(Object element, String property)
	{
		if (property.equals(P_ENABLED))
		{
			return true;
		}
		else if (property.equals(P_USERID))
		{
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
	 */
	public Object getValue(Object element, String property)
	{
		IRSESystemType row = (IRSESystemType)element;
		RSESystemTypeAdapter adapter = (RSESystemTypeAdapter)(row.getAdapter(RSESystemTypeAdapter.class));
		Object value = ""; //$NON-NLS-1$

		if (property.equals(P_NAME))
			value = row.getLabel();
		else if (property.equals(P_ENABLED))
			value = (row.isEnabled() ? new Integer(0) : new Integer(1));
		else if (property.equals(P_USERID))
			value = (adapter.getDefaultUserId(row) == null) ? "" : adapter.getDefaultUserId(row); //$NON-NLS-1$
		else
			value = (row.getDescription() == null) ? "" : row.getDescription(); //$NON-NLS-1$

		return value;
	}

	public boolean enabledStateChanged()
	{
		return enabledStateChanged;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	public void modify(Object element, String property, Object value)
	{
		IRSESystemType row = (IRSESystemType)(((TableItem)element).getData());

		if (property.equals(P_ENABLED))
		{
		    Integer val = (Integer)value;
		    RSEPreferencesManager.setIsSystemTypeEnabled(row, enabledStates[val.intValue()]);
			enabledStateChanged = true;
		}
		else if (property.equals(P_USERID))
		{
			RSEPreferencesManager.setDefaultUserId(row, (String) value);
		}
		else
			return;

		keyValues.put(row.getId(), "");		 //$NON-NLS-1$
		tableViewer.update(row, null);
	}

    // ------------------------------
    // ITableLabelProvider methods...
    // ------------------------------

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex)
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(Object element, int columnIndex)
	{
		IRSESystemType currType = (IRSESystemType)element;

		if (columnIndex == COLUMN_NAME)
			return currType.getLabel();
		else if (columnIndex == COLUMN_ENABLED) {
			int n = 0;
			if (currType.isEnabled() == enabledStates[1]) {
				n = 1;
			}
			return enabledStateStrings[n];
		} else if (columnIndex == COLUMN_USERID) {
			RSESystemTypeAdapter adapter = (RSESystemTypeAdapter) (currType.getAdapter(RSESystemTypeAdapter.class));
			return (adapter.getDefaultUserId(currType)==null ? "" : adapter.getDefaultUserId(currType)); //$NON-NLS-1$
		}
		else
			return (currType.getDescription()==null ? "" : currType.getDescription()); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener)
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property)
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener)
	{
	}

    // -------------------------------------
    // IStructuredContentProvider methods...
    // -------------------------------------

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement)
	{
		if (systemTypes == null)
			systemTypes = getSystemTypes(false);
		return systemTypes;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
	}

	// ----------------
	// Other methods...
	// ----------------
    /**
     * Set the tooltip text
     */
    public void setToolTipText(String tip)
    {
    	table.setToolTipText(tip);
    }

//    public static Hashtable initSystemTypePreferences(IPreferenceStore store, IRSESystemType[] systemTypes)
//    {
//    	/* this must stay in synch with the SystemPreferencesManager */
//		String value = store.getString(ISystemPreferencesConstants.SYSTEMTYPE_VALUES);
//		Hashtable keyValues = null;
//	    if ((value == null) || (value.length()==0)) // not initialized yet?
//	    {
//	    	return null;
//	    	// nothing to do, as we have read from systemTypes extension points already
//	    }
//	    else
//	    {
//	    	keyValues = parseString(value);
//	    	// we have now a hashtable, where the keys are the system type names,
//	    	//  and the values are the column-value attributes for that type, separated
//	    	//  by a '+' character: enabled+userid. eg: "true+bob"
//			Enumeration keys = keyValues.keys();
//			while (keys.hasMoreElements())
//			{
//				String key = (String)keys.nextElement();
//				String attributes = (String)keyValues.get(key);
//				String attr1="true", attr2=""; //$NON-NLS-1$ //$NON-NLS-2$
//				if ((attributes != null) && (attributes.length()>0))
//				{
//					StringTokenizer tokens = new StringTokenizer(attributes, Character.toString(EACHVALUE_DELIMITER));
//					if (tokens.hasMoreTokens())
//					{
//						attr1 = tokens.nextToken();
//						if (tokens.hasMoreTokens())
//						{
//							attr2 = tokens.nextToken();
//						}
//						else
//						{
//							attr2 = "null"; //$NON-NLS-1$
//						}
//					}
//				}
//				// find this system type in the array...
//				IRSESystemType matchingType = RSECorePlugin.getTheCoreRegistry().getSystemType(key);
//				RSESystemTypeAdapter adapter = (RSESystemTypeAdapter)(matchingType.getAdapter(RSESystemTypeAdapter.class));
//
//				// update this system type's attributes as per preferences...
//				{
//					adapter.setIsEnabled(matchingType, attr1.equals("true")); //$NON-NLS-1$
//					if (!attr2.equals("null")) //$NON-NLS-1$
//						adapter.setDefaultUserId(matchingType, attr2);
//				}
//			}
//	    }
//	    return keyValues;
//    }
}