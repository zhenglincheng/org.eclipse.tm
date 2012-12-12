/***************************************************************************************************
 * Copyright (c) 2008, 2010 Mirko Raner.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Mirko Raner - [196337] Adapted from org.eclipse.tm.terminal.ssh/SshSettingsPage
 **************************************************************************************************/

package org.eclipse.tm.internal.terminal.local;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.tm.internal.terminal.local.launch.LocalTerminalLaunchUtilities;
import org.eclipse.tm.internal.terminal.local.ui.DependentHeightComposite;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsPage;
import org.eclipse.tm.internal.terminal.provisional.api.Logger;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * The class {@link LocalTerminalSettingsPage} is an implementation {@link ISettingsPage} for
 * local program connections.
 *
 * @author Mirko Raner
 * @version $Revision: 1.4 $
 */
public class LocalTerminalSettingsPage
implements ISettingsPage, ISelectionChangedListener, SelectionListener {

	private ILocalTerminalSettings settings;
	private TableViewer viewer;
	private Button buttonEdit;
	private Button buttonNew;
	private Button buttonDelete;

	/**
	 * Creates a new {@link LocalTerminalSettingsPage} that reflects the settings of the specified
	 * {@link ILocalTerminalSettings} object.
	 *
	 * @param settings the {@link ILocalTerminalSettings}
	 */
	public LocalTerminalSettingsPage(ILocalTerminalSettings settings) {

		this.settings = settings;
	}

	/**
	 * Creates the {@link org.eclipse.swt.widgets.Control} for the settings page.
	 * (NOTE: contrary to the common pattern, this method does not actually return the Control it
	 * created)
	 *
	 * @param parent the parent {@link Composite} into which the control is to be inserted
	 *
	 * @see ISettingsPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {

		ILaunchConfiguration defaultConfiguration;
		defaultConfiguration = LocalTerminalLaunchUtilities.createDefaultLaunchConfiguration();
		Composite enclosing = parent.getParent();
		Layout enclosingLayout = enclosing.getLayout();
		int extra = 0;
		if (enclosingLayout instanceof GridLayout) {

			extra = -2*((GridLayout)enclosingLayout).marginHeight-2;
		}
		Composite composite = new DependentHeightComposite(parent, SWT.NONE, enclosing, extra);
		//
		// TODO: This is a HACK that ensures proper resizing of the settings page inside the
		//       StackLayout of the PageBook. The following code makes implicit assumptions about
		//       the internal layout of surrounding widgets. This is something that should be
		//       properly addressed in the framework (maybe in the PageBook class).

		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = layout.verticalSpacing = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		Label label = new Label(composite, SWT.NONE);
		label.setText(LocalTerminalMessages.launchConfiguration);
		label.setLayoutData(new GridData());

		// Create list of available launch configurations:
		//
		Composite tableAndButtons = new Composite(composite, SWT.NONE);
		tableAndButtons.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		tableAndButtons.setLayout(layout);
		Table table = new Table(tableAndButtons, SWT.BORDER);
		viewer = new TableViewer(table);
		viewer.setLabelProvider(new LocalTerminalLaunchLabelProvider());
		viewer.setContentProvider(new LocalTerminalLaunchListProvider());
		viewer.setInput(new Object());
		viewer.addSelectionChangedListener(this);
		table.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 0, 3));
		buttonNew = pushButton(tableAndButtons, LocalTerminalMessages.labelNew, false);
		buttonEdit = pushButton(tableAndButtons, LocalTerminalMessages.labelEdit, false);
		buttonEdit.setEnabled(settings.getLaunchConfigurationName() != null);
		buttonDelete = pushButton(tableAndButtons, LocalTerminalMessages.labelDelete, true);
		buttonDelete.setEnabled(settings.getLaunchConfigurationName() != null);
		//
		// NOTE: echo and line separator settings were moved to the launch configuration!

		// NOTE: loadSettings() is actually NOT called by the framework but needs to be called
		//       by the settings page itself
		// TODO: this should be fixed in the framework; otherwise there is really no point
		//       in having it be a part of the ISettingsPage interface
		//
		loadSettings();
		if (defaultConfiguration != null) {

			// If there is only one configuration (the default one), then make sure it gets
			// selected:
			//
			viewer.setSelection(new StructuredSelection(defaultConfiguration), true);
		}
	}

	/**
	 * Loads the settings from the internal {@link ILocalTerminalSettings} object.
	 * This method will update the UI to reflect the current settings.
	 *
	 * @see ISettingsPage#loadSettings()
	 */
	public void loadSettings() {

		String configurationName = settings.getLaunchConfigurationName();
		ILaunchConfiguration configuration;
		try {

			configuration = LocalTerminalUtilities.findLaunchConfiguration(configurationName);
		}
		catch (CoreException couldNotFindLaunchConfiguration) {

			configuration = null;
		}
		if (settings.getLaunchConfigurationName() != null && configuration != null) {

			viewer.setSelection(new StructuredSelection(configuration), true);
		}
	}

	/**
	 * Saves the settings that are currently displayed in the UI to the internal
	 * {@link ILocalTerminalSettings} object.
	 *
	 * @see ISettingsPage#saveSettings()
	 */
	public void saveSettings() {

		if (viewer != null && !viewer.getSelection().isEmpty()) {

			IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
			Object element = selection.getFirstElement();
			if (element instanceof ILaunchConfiguration) {

				String launchConfiguration = ((ILaunchConfiguration)element).getName();
				settings.setLaunchConfigurationName(launchConfiguration);
			}
		}
	}

	/**
	 * Checks if the current settings are valid for starting a terminal session.
	 * This method will only return <code>true</code> if a launch configuration is selected.
	 *
	 * @return <code>true</code> if a launch configuration has been selected, <code>false</code>
	 * otherwise
	 */
	public boolean validateSettings() {

		return viewer != null && !viewer.getSelection().isEmpty();
	}

	/**
	 * Enables or disables the Edit... and Delete buttons depending on whether a launch
	 * configuration is currently selected in the viewer.
	 *
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {

		buttonEdit.setEnabled(!event.getSelection().isEmpty());
		buttonDelete.setEnabled(!event.getSelection().isEmpty());
	}

	/**
	 * Handles default button clicks for the Edit... and New.. buttons. This method will simply
	 * pass on the call to {@link #widgetSelected(SelectionEvent)}.
	 *
	 * @param event the {@link SelectionEvent}
	 *
	 * @see SelectionListener#widgetDefaultSelected(SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent event) {

		widgetSelected(event);
	}

	/**
	 * Handles default button clicks for the Edit... and New.. buttons.
	 *
	 * @param event the {@link SelectionEvent}
	 *
	 * @see SelectionListener#widgetSelected(SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent event) {

		ILaunchConfiguration configuration = null;
		Widget widget = event.widget;
		if (widget == null) {

			return;
		}
		if (widget.equals(buttonNew)) {

			try {

				ILaunchConfigurationWorkingCopy newLaunch;
				newLaunch = LocalTerminalLaunchUtilities.createNewLaunchConfigurationWorkingCopy();
				configuration = newLaunch.doSave();
			}
			catch (CoreException couldNotCreateNewLaunchConfiguration) {

				Logger.logException(couldNotCreateNewLaunchConfiguration);
			}
		}
		if (widget.equals(buttonEdit) || configuration != null) {

			ILaunchGroup group;
			IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
			if (configuration == null) {

				configuration = (ILaunchConfiguration)selection.getFirstElement();
			}
			group = DebugUITools.getLaunchGroup(configuration, ILaunchManager.RUN_MODE);
			String groupID = group.getIdentifier();
			DebugUITools.openLaunchConfigurationDialog(getShell(), configuration, groupID, null);
			//
			// TODO: handle return value (maybe start terminal right away if "Run" was selected)
			// - a return value of Window.CANCEL indicates that "Close" was selected
			// - a return value of Window.OK indicates that "Run" was selected

			viewer.refresh();
			viewer.setSelection(new StructuredSelection(configuration), true);
			//
			// TODO: handle renamed configurations; setSelection(...) will not work if the user
			//       renamed the configuration in the dialog (apparently, because renaming actually
			//       creates a different ILaunchConfiguration object, rather than just renaming the
			//       existing one)
		}
		if (widget.equals(buttonDelete)) {

			String title = LocalTerminalMessages.questionTitleDeleteLaunchConfiguration;
			String question = LocalTerminalMessages.questionDeleteLaunchConfiguration;
			if (MessageDialog.openQuestion(getShell(), title, question)) {

				IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
				configuration = (ILaunchConfiguration)selection.getFirstElement();
				try {

					configuration.delete();
				}
				catch (CoreException exception) {

					ErrorDialog.openError(getShell(), null, null, exception.getStatus());
				}
				viewer.refresh();
			}
		}
	}

	static Shell getShell() {

		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
		if (activeWindow != null) {

			return activeWindow.getShell();
		}
		IWorkbenchWindow[] allWindows = workbench.getWorkbenchWindows();
		return allWindows.length > 0? allWindows[0].getShell():null;
	}

	//------------------------------------ PRIVATE SECTION ---------------------------------------//

	private Button pushButton(Composite parent, String label, boolean grabVertical) {

		GridData layoutData;
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		layoutData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING|GridData.HORIZONTAL_ALIGN_FILL);
		layoutData.grabExcessVerticalSpace = grabVertical;
		button.setLayoutData(layoutData);
		button.addSelectionListener(this);
		return button;
	}
}
