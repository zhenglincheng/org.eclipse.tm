/********************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation. All rights reserved.
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
 * Javier Montalvo Orús (Symbian) - Bug 149151: New Connection first page should use a Listbox for systemtype
 ********************************************************************************/

package org.eclipse.rse.ui.wizards;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemResources;
import org.eclipse.rse.ui.SystemWidgetHelpers;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * The New Connection Wizard main page that allows selection of system type.
 */
public class RSENewConnectionWizardMainPage extends AbstractSystemWizardPage implements Listener {
	
	protected String parentHelpId;
	protected List textSystemType;
	protected Text descriptionSystemType;
	protected IWizardPage nextPage;
	protected IRSESystemType[] restrictedSystemTypes;

	/**
	 * Constructor.
	 * @param wizard the wizard.
	 * @param title the title of the wizard page.
	 * @param description the description of the wizard page.
	 */
	public RSENewConnectionWizardMainPage(IRSENewConnectionWizard wizard, String title, String description) {
		super(wizard, "NewConnectionSystemType", title, description);
        parentHelpId = RSEUIPlugin.HELPPREFIX + "wncc0000";
	    setHelp(parentHelpId);
	}

	/**
	 * @see org.eclipse.rse.ui.wizards.AbstractSystemWizardPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public Control createContents(Composite parent) {
		
		int nbrColumns = 2;
		Composite composite_prompts = SystemWidgetHelpers.createComposite(parent, nbrColumns);
		SystemWidgetHelpers.setCompositeHelp(composite_prompts, parentHelpId);
		
		String temp = SystemWidgetHelpers.appendColon(SystemResources.RESID_CONNECTION_SYSTEMTYPE_LABEL);
		
		Label labelSystemType = SystemWidgetHelpers.createLabel(composite_prompts, temp);
		labelSystemType.setToolTipText(SystemResources.RESID_CONNECTION_SYSTEMTYPE_TIP);
		
		if (restrictedSystemTypes == null) {
			textSystemType = SystemWidgetHelpers.createSystemTypeListBox(parent, null);
		}
		else {
			String[] systemTypeNames = new String[restrictedSystemTypes.length];
			
			for (int i = 0; i < restrictedSystemTypes.length; i++) {
				systemTypeNames[i] = restrictedSystemTypes[i].getName();
			}
			
			textSystemType = SystemWidgetHelpers.createSystemTypeListBox(parent, null, systemTypeNames);
		}
		
		textSystemType.addListener(SWT.MouseDoubleClick, this);
		
		textSystemType.setToolTipText(SystemResources.RESID_CONNECTION_SYSTEMTYPE_TIP);
		SystemWidgetHelpers.setHelp(textSystemType, RSEUIPlugin.HELPPREFIX + "ccon0003");
		
		textSystemType.addListener(SWT.Selection, this);
		
		descriptionSystemType = SystemWidgetHelpers.createMultiLineTextField(parent,null,30);
		descriptionSystemType.setEditable(false);

		IRSESystemType systemType = RSECorePlugin.getDefault().getRegistry().getSystemType(textSystemType.getSelection()[0]);
		
		if(systemType!=null) {
			descriptionSystemType.setText(systemType.getDescription());
		}
		
		return composite_prompts;
	}
	
	public void restrictToSystemTypes(IRSESystemType[] systemTypes) {
		this.restrictedSystemTypes = systemTypes;
	}

	/**
	 * @see org.eclipse.rse.ui.wizards.AbstractSystemWizardPage#getInitialFocusControl()
	 */
	protected Control getInitialFocusControl() {
		
		if (textSystemType != null) {
			return textSystemType;
		}
		else {
			return null;
		}
	}

	/**
	 * @see org.eclipse.rse.ui.wizards.AbstractSystemWizardPage#performFinish()
	 */
	public boolean performFinish() {
		return true;
	}

	/**
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		
		IWizard wizard = getWizard();
		
		// if the wizard is a new connection wizard, which should always be the case,
		// get the new connection wizard delegate for the selected system type and
		// ask for the main page
		if (wizard instanceof IRSENewConnectionWizard) {
			String systemTypeStr = textSystemType.getSelection()[0];
			IRSENewConnectionWizard newConnWizard = (IRSENewConnectionWizard)wizard;
			newConnWizard.setSelectedSystemType(RSECorePlugin.getDefault().getRegistry().getSystemType(systemTypeStr));
			return newConnWizard.getDelegate().getMainPage();
		}
		else {
			return super.getNextPage();
		}
	}

	/**
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	public void handleEvent(Event event) {
		
		if (((event.type == SWT.Selection) || (event.type == SWT.MouseDoubleClick)) && event.widget == textSystemType) {
			
			IWizard wizard = getWizard();
		
			if (wizard instanceof IRSENewConnectionWizard) {
				String systemTypeStr = textSystemType.getSelection()[0];
				IRSENewConnectionWizard newConnWizard = (IRSENewConnectionWizard)wizard;
				
				IRSESystemType systemType = RSECorePlugin.getDefault().getRegistry().getSystemType(systemTypeStr);
				newConnWizard.setSelectedSystemType(systemType);
				descriptionSystemType.setText(systemType.getDescription());
				
				if (event.type == SWT.MouseDoubleClick) {
					newConnWizard.getContainer().showPage(getNextPage());
				}
			}
		}
	}
}