/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 * David McKnight   (IBM)        - [216252] MessageFormat.format -> NLS.bind
 *******************************************************************************/

package org.eclipse.rse.internal.ui.view;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A slight variation of the Eclipse-supplied ComboBoxCellEditor class, which
 * allows the input array to be changed dynamically.
 * <p>
 * A cell editor that presents a list of items in a combo box. The cell editor's
 * value is the zero-based index of the selected item.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class SystemComboBoxCellEditor extends CellEditor
{
	private boolean readOnly = false;
	/**
	 * The list of items to present in the combo box.
	 */
	private String[] items;

	/**
	 * The zero-based index of the selected item.
	 */
	private int selection;

	/**
	 * The custom combo box control.
	 */
	private CCombo comboBox;
	/**
	 * Creates a new cell editor with a combo containing the given
	 * list of choices and parented under the given control. The cell
	 * editor value is the zero-based index of the selected item.
	 * Initially, the cell editor has no cell validator and
	 * the first item in the list is selected.
	 *
	 * @param parent the parent control
	 * @param items the list of strings for the combo box
	 */
	public SystemComboBoxCellEditor(Composite parent, String[] items)
	{
		super(parent);
		selection = 0;
		if (items != null)
			setItems(items);
		else
			setValueValid(true);
	}
	/**
	 * Creates a new cell editor with a combo containing the given
	 * list of choices and parented under the given control. The cell
	 * editor value is the zero-based index of the selected item.
	 * Initially, the cell editor has no cell validator and
	 * the first item in the list is selected.
	 *
	 * @param parent the parent control
	 * //@param whether or not this is readonly
	 */
	//public SystemComboBoxCellEditor(Composite parent, boolean readOnly)
	public SystemComboBoxCellEditor(Composite parent)
	{
		super(parent);
		selection = 0;
		//this.readOnly = readOnly;
		setValueValid(true);
	}
	/**
	 * Change the input items
	 */
	public void setItems(String[] items)
	{
		this.items = items;
		populateComboBoxItems();
		setValueValid(true);
	}
	/* (non-Javadoc)
	 * Method declared on CellEditor.
	 */
	protected Control createControl(Composite parent)
	{
		if (!readOnly)
			comboBox = new CCombo(parent, SWT.NONE);
		else
			comboBox = new CCombo(parent, SWT.READ_ONLY);
		comboBox.setFont(parent.getFont());

		comboBox.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				keyReleaseOccured(e);
			}
		});

		comboBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				// DKM - only process this when there is a selection change
				//   fix for defect 138324
				if (selection == comboBox.getSelectionIndex() && comboBox.getText().equals(comboBox.getItem(comboBox.getSelectionIndex())))
					return; // no change

				// must set the selection before getting value
				selection = comboBox.getSelectionIndex();
				Object newValue = doGetValue();


				boolean newValidState = isCorrect(newValue);
				if (newValidState) {
					doSetValue(newValue);
				} else {
					// try to insert the current value into the error message.
					setErrorMessage(
							NLS.bind(getErrorMessage(), items[selection]));
				}
				fireApplyEditorValue();
			}
		});

		comboBox.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
				}
			}
		});

		return comboBox;
	}
	/**
	 * The <code>ComboBoxCellEditor</code> implementation of
	 * this <code>CellEditor</code> framework method returns
	 * the zero-based index of the current selection.
	 *
	 * @return the zero-based index of the current selection wrapped
	 *  as an <code>Integer</code>
	 */
	protected Object doGetValue()
	{
		return new Integer(selection);
	}
	/* (non-Javadoc)
	 * Method declared on CellEditor.
	 */
	protected void doSetFocus()
	{
		comboBox.setFocus();
	}
	/**
	 * The <code>ComboBoxCellEditor</code> implementation of
	 * this <code>CellEditor</code> framework method
	 * accepts a zero-based index of a selection.
	 *
	 * @param value the zero-based index of the selection wrapped
	 *   as an <code>Integer</code>
	 */
	protected void doSetValue(Object value)
	{
		if (!(value instanceof Integer))
		{
			String[] items = comboBox.getItems();
			for (int i = 0; i < items.length; i++)
			{
				String item = items[i];
				if (item.equals(value))
				{
					selection = i;
				}
			}
			return;
		}

		//Assert.isTrue(comboBox != null && (value instanceof Integer));
		int newselection = ((Integer) value).intValue();
		if (newselection != selection)
		{
			selection = newselection;
		}
		String curText = comboBox.getText();
		if (!comboBox.getItem(selection).equals(curText))
		{
			comboBox.select(selection);
		}
	}
	/**
	 * Add the items to the combo box.
	 */
	private void populateComboBoxItems()
	{
		if (comboBox != null && items != null)
		{
			comboBox.removeAll();
			for (int i = 0; i < items.length; i++)
				comboBox.add(items[i], i);
			setValueValid(true);
		}
	}
}
