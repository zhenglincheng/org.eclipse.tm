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
 * Martin Oberhuber (Wind River) - [168975] Move RSE Events API to Core
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Martin Oberhuber (Wind River) - [183824] Forward SystemMessageException from IRemoteFileSubsystem
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * David Dykstal (IBM) - [160776] format file size according to client system conventions and locale
 * David McKnight (IBM) - [173518] [refresh] Read only changes are not shown in RSE until the parent folder is refreshed
 * Kevin Doyle (IBM) - [197976] Changing a file to read-only when it is open doesn't update local copy
 ********************************************************************************/

package org.eclipse.rse.internal.files.ui.propertypages;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.events.ISystemResourceChangeEvents;
import org.eclipse.rse.core.events.SystemResourceChangeEvent;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.files.ui.resources.SystemEditableRemoteFile;
import org.eclipse.rse.internal.files.ui.FileResources;
import org.eclipse.rse.internal.subsystems.files.core.SystemFileResources;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.RemoteFileIOException;
import org.eclipse.rse.services.files.RemoteFileSecurityException;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IVirtualRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.RemoteFileEncodingManager;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemWidgetHelpers;
import org.eclipse.rse.ui.propertypages.SystemBasePropertyPage;
import org.eclipse.rse.ui.view.ISystemEditableRemoteObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ide.IDEEncoding;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.NumberFormat;


/**
 * The remote property page for remote file properties.
 * This is an output-only page.
 * The plugin.xml file registers this for remote file system objects.
 */
public class SystemFilePropertyPage extends SystemBasePropertyPage
       implements SelectionListener
{
	
	protected Label labelNamePrompt, labelTypePrompt, labelPathPrompt, labelSizePrompt, 
	                labelModifiedPrompt;
	//protected Button cbReadablePrompt, cbWritablePrompt;
	protected Button cbReadonlyPrompt, cbHiddenPrompt;
	protected Label labelName, labelType, labelPath, labelSize, labelModified, labelReadable, labelWritable, labelHidden;
	protected Button defaultEncodingButton, otherEncodingButton;
	protected Combo otherEncodingCombo;
	protected String errorMessage;	
    protected boolean initDone = false;
    protected boolean wasReadOnly = false;
    
    private boolean encodingFieldAdded = false;
    private String defaultEncoding = null;
    private boolean isValidBefore = true;
       	
	/**
	 * Constructor for SystemFilterPropertyPage
	 */
	public SystemFilePropertyPage()
	{
		super();
	}
	/**
	 * Create the page's GUI contents.
	 */
	protected Control createContentArea(Composite parent)
	{
		IRemoteFile file = getRemoteFile();

		// Inner composite
		int nbrColumns = 2;
		Composite composite_prompts = SystemWidgetHelpers.createComposite(parent, nbrColumns);	

		// Name display
		labelNamePrompt = SystemWidgetHelpers.createLabel(
			composite_prompts, SystemFileResources.RESID_PP_FILE_NAME_LABEL, SystemFileResources.RESID_PP_FILE_NAME_TOOLTIP);
		labelName = SystemWidgetHelpers.createLabel(
			composite_prompts, ""); //$NON-NLS-1$
	    labelName.setToolTipText(SystemFileResources.RESID_PP_FILE_NAME_TOOLTIP);

		// Type display
		labelTypePrompt = SystemWidgetHelpers.createLabel(
			composite_prompts, SystemFileResources.RESID_PP_FILE_TYPE_LABEL, SystemFileResources.RESID_PP_FILE_TYPE_TOOLTIP);
		labelType = SystemWidgetHelpers.createLabel(
			composite_prompts, ""); //$NON-NLS-1$
	    labelType.setToolTipText(SystemFileResources.RESID_PP_FILE_TYPE_TOOLTIP);

		// Path display
		if (!file.isRoot())
		{
		  labelPathPrompt = SystemWidgetHelpers.createLabel(
			composite_prompts, SystemFileResources.RESID_PP_FILE_PATH_LABEL, SystemFileResources.RESID_PP_FILE_PATH_TOOLTIP);
		  labelPath = SystemWidgetHelpers.createLabel(
			composite_prompts, ""); //$NON-NLS-1$
	      labelPath.setToolTipText(SystemFileResources.RESID_PP_FILE_PATH_TOOLTIP);
		}

		// Size display
		if (!file.isDirectory())
		{
		  labelSizePrompt = SystemWidgetHelpers.createLabel(
			composite_prompts, SystemFileResources.RESID_PP_FILE_SIZE_LABEL, SystemFileResources.RESID_PP_FILE_SIZE_TOOLTIP);
		  labelSize = SystemWidgetHelpers.createLabel(
			composite_prompts, ""); //$NON-NLS-1$
	      labelSize.setToolTipText(SystemFileResources.RESID_PP_FILE_SIZE_TOOLTIP);
		}

		// Modified display
		if (!file.isRoot())
		{
		  labelModifiedPrompt = SystemWidgetHelpers.createLabel(
			composite_prompts, SystemFileResources.RESID_PP_FILE_MODIFIED_LABEL, SystemFileResources.RESID_PP_FILE_MODIFIED_TOOLTIP);
		  labelModified = SystemWidgetHelpers.createLabel(
			composite_prompts, ""); //$NON-NLS-1$
	      labelModified.setToolTipText(SystemFileResources.RESID_PP_FILE_MODIFIED_TOOLTIP);
		}

		// Readonly display
		if (!file.isRoot())
		{
		  if (file.showReadOnlyProperty())		   	  
		  {
		    cbReadonlyPrompt = SystemWidgetHelpers.createCheckBox(
			  composite_prompts, null, SystemFileResources.RESID_PP_FILE_READONLY_LABEL, SystemFileResources.RESID_PP_FILE_READONLY_TOOLTIP);
		  }
		}  

        /*
		// Readable display
		if (!file.isRoot())
		{
		  cbReadablePrompt = SystemWidgetHelpers.createCheckBox(
			composite_prompts, null, SystemFileResources.RESID_PP_FILE_READABLE_ROOT);
		}

		// Writable display
		if (!file.isRoot())
		{
		  cbWritablePrompt = SystemWidgetHelpers.createCheckBox(
			composite_prompts, null, SystemFileResources.RESID_PP_FILE_WRITABLE_ROOT);
		}
		*/

		// Hidden display
		if (!file.isRoot())
		{
		  cbHiddenPrompt = SystemWidgetHelpers.createCheckBox(
			composite_prompts, null, SystemFileResources.RESID_PP_FILE_HIDDEN_LABEL, SystemFileResources.RESID_PP_FILE_HIDDEN_TOOLTIP);
	      //((GridData)cbHiddenPrompt.getLayoutData()).horizontalSpan = nbrColumns;
		}
		
		// check if an encodings field should be added. Add only if the subsystem
		// indicates that it supports encodings
		if (file.getParentRemoteFileSubSystem().supportsEncoding()) {
			
			SystemWidgetHelpers.createLabel(composite_prompts, "", 2); //$NON-NLS-1$
			
			// encoding field
			Group encodingGroup = SystemWidgetHelpers.createGroupComposite(composite_prompts, 2, SystemFileResources.RESID_PP_FILE_ENCODING_GROUP_LABEL);
			GridData data = new GridData();
			data.horizontalSpan = 2;
			data.horizontalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;
			data.verticalAlignment = SWT.BEGINNING;
			data.grabExcessVerticalSpace = false;
			encodingGroup.setLayoutData(data);
			
			SelectionAdapter defaultButtonSelectionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateEncodingGroupState(defaultEncodingButton.getSelection());
					updateValidState();
				}
			};
		
			// default encoding field
			defaultEncoding = file.getParentRemoteFileSubSystem().getRemoteEncoding();
			String defaultEncodingLabel = SystemFileResources.RESID_PP_FILE_ENCODING_DEFAULT_LABEL;
			int idx = defaultEncodingLabel.indexOf('%');
			
			if (idx != -1) {
				defaultEncodingLabel = defaultEncodingLabel.substring(0, idx) + file.getParentRemoteFileSubSystem().getRemoteEncoding() + defaultEncodingLabel.substring(idx+2);
			}
			
			defaultEncodingButton = SystemWidgetHelpers.createRadioButton(encodingGroup, null, defaultEncodingLabel, SystemFileResources.RESID_PP_FILE_ENCODING_DEFAULT_TOOLTIP);
			data = new GridData();
			data.horizontalSpan = 2;
			defaultEncodingButton.setLayoutData(data);
			defaultEncodingButton.addSelectionListener(defaultButtonSelectionListener);
			
	        Composite otherComposite = new Composite(encodingGroup, SWT.NONE);
	        GridLayout otherLayout = new GridLayout();
	        otherLayout.numColumns = 2;
	        otherLayout.marginWidth = 0;
	        otherLayout.marginHeight = 0;
	        otherComposite.setLayout(otherLayout);
	        otherComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

			// other encoding field
			otherEncodingButton = SystemWidgetHelpers.createRadioButton(otherComposite, null, SystemFileResources.RESID_PP_FILE_ENCODING_OTHER_LABEL, SystemFileResources.RESID_PP_FILE_ENCODING_OTHER_TOOLTIP);
			
			SelectionAdapter otherButtonSelectionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateEncodingGroupState(!otherEncodingButton.getSelection());
					updateValidState();
				}
			};
			
			otherEncodingButton.addSelectionListener(otherButtonSelectionListener);

			// other encoding combo
			otherEncodingCombo = SystemWidgetHelpers.createCombo(otherComposite, null, SystemFileResources.RESID_PP_FILE_ENCODING_ENTER_TOOLTIP);
			data = new GridData();
			data.horizontalAlignment = SWT.BEGINNING;
			data.grabExcessHorizontalSpace = true;
			otherEncodingCombo.setLayoutData(data);

			otherEncodingCombo.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					updateValidState();
				}
			});

			otherEncodingCombo.addKeyListener(new KeyAdapter(){
				public void keyReleased(KeyEvent e) {
					updateValidState();
				}
			});
			
			SystemWidgetHelpers.createLabel(encodingGroup, ""); //$NON-NLS-1$

			SystemWidgetHelpers.createLabel(composite_prompts, "", 2); //$NON-NLS-1$
			
			encodingFieldAdded = true;
		}
		else {
			encodingFieldAdded = false;
		}
        
	    if (!initDone)	
	      doInitializeFields();		  
	      
	    if (!file.isRoot() && file.showReadOnlyProperty())	    
		  cbReadonlyPrompt.addSelectionListener(this);
        
		return composite_prompts;
	}
	
	/**
	 * Update the encoding group state.
	 * @param useDefault whether to update the state with default option on. <code>true</code> if the default option
	 * should be on, <code>false</code> if it should be off.
	 */
	private void updateEncodingGroupState(boolean useDefault) {
		defaultEncodingButton.setSelection(useDefault);
		otherEncodingButton.setSelection(!useDefault);
		
		if (useDefault) {
			otherEncodingCombo.setText(getDefaultEncoding());
		}
		
		otherEncodingCombo.setEnabled(!useDefault);
		updateValidState();
	}
	
	/**
	 * Updates the valid state of the encoding group.
	 */
	private void updateValidState() {
		boolean isValid = isEncodingValid();
		
		if (isValid != isValidBefore) {
			isValidBefore = isValid;
			
			if (isValidBefore) {
				clearErrorMessage();
			}
			else {
				// TODO make this a SystemMessage
				setErrorMessage(FileResources.MESSAGE_ENCODING_NOT_SUPPORTED);
			}
		}
	}
	
	/**
	 * Returns the default encoding.
	 * @return the default encoding
	 */
	protected String getDefaultEncoding() {
		return defaultEncoding;
	}
	
	/**
	 * Returns the currently selected encoding.
	 * @return the currently selected encoding.
	 */
	protected String getSelectedEncoding() {
		if (defaultEncodingButton.getSelection()) {
			return defaultEncoding;
		}
		
		return otherEncodingCombo.getText();
	}

	/**
	 * Returns whether the encoding is valid.
	 * @return <code>true</code> if the encoding is valid, <code>false</code> otherwise.
	 */
	private boolean isEncodingValid() {
		return defaultEncodingButton.getSelection() || isEncodingValid(otherEncodingCombo.getText());
	}
	
	/**
	 * Returns whether or not the given encoding is valid.
	 * @param encoding the encoding.
	 * @return <code>true</code> if the encoding is valid, <code>false</code> otherwise.
	 */
	private boolean isEncodingValid(String encoding) {
		try {
			return Charset.isSupported(encoding);
		}
		catch (IllegalCharsetNameException e) {
			return false;
		}
	}
	
	/**
	 * Get the input remote file object
	 */
	protected IRemoteFile getRemoteFile()
	{
		Object element = getElement();
		return ((IRemoteFile)element);
	}

	/**
	 * Initialize values of input fields based on input
	 */
	protected void doInitializeFields()
	{
		initDone = true;
		IRemoteFile file = getRemoteFile();
		// name
		String name = file.getName();
		if (name.length() > 100) 
		{
			String shortName = name.substring(0, 97).concat("...");				 //$NON-NLS-1$
			labelName.setText(shortName);			
		}	
		else
		{
			labelName.setText(name);
		}
					
		// type
		if (file.isRoot())
		  labelType.setText(SystemFileResources.RESID_PP_FILE_TYPE_ROOT_VALUE);
		else if (file.isDirectory())
		  labelType.setText(SystemFileResources.RESID_PP_FILE_TYPE_FOLDER_VALUE);
		else
		  labelType.setText(SystemFileResources.RESID_PP_FILE_TYPE_FILE_VALUE);
	    // path
	    if (labelPath != null)
	    {
	      String path = file.getParentPath();
	      if (path != null)
	        labelPath.setText(file.getParentPath());
	    }
	    // size
	    if (labelSize != null) {
	    	NumberFormat fmt = NumberFormat.getIntegerInstance();
	    	String formattedNumber = fmt.format(file.getLength());
	    	labelSize.setText(formattedNumber);
	    }
	    // modified
	    if (labelModified != null)
	    {
	      Date date = file.getLastModifiedDate();
	      if (date != null)
	      {
			DateFormat datefmt = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL);
			String formattedDate = datefmt.format(date);
	        labelModified.setText(formattedDate);
	      }
	    }
	    // readonly
	    if (file.showReadOnlyProperty()) {
	      if (cbReadonlyPrompt != null)
	      {
	        cbReadonlyPrompt.setSelection(!file.canWrite());
	        wasReadOnly = !file.canWrite();
	        if (file instanceof IVirtualRemoteFile)
              cbReadonlyPrompt.setEnabled(false);
	      }
	    }  
        /*
	    // readable
	    if (cbReadablePrompt != null)
	    {
	      cbReadablePrompt.setSelection(file.canRead());
          cbReadablePrompt.setEnabled(false);
	    }
	    // writable
	    if (cbWritablePrompt != null)
	    {
 	      cbWritablePrompt.setSelection(file.canWrite());
          cbWritablePrompt.setEnabled(false); 
	    }
	    */
	    // hidden
	    if (cbHiddenPrompt != null)
	    {
	      cbHiddenPrompt.setSelection(file.isHidden());
          cbHiddenPrompt.setEnabled(false); 
	    }
	    
	    // the file encoding group
	    if (encodingFieldAdded) {
	    	List encodings = IDEEncoding.getIDEEncodings();
	    	String[] encodingStrings = new String[encodings.size()];
	    	encodings.toArray(encodingStrings);
	    	otherEncodingCombo.setItems(encodingStrings);

	    	String encoding = file.getEncoding();

	    	// if the encoding is the same as the default encoding, then we want to choose the default encoding option
	    	if (encoding.equalsIgnoreCase(defaultEncoding)) {
	    		updateEncodingGroupState(true);
	    	}
	    	// otherwise choose the other encoding option
	    	else {
	    		otherEncodingCombo.setText(encoding);
	    		updateEncodingGroupState(false);
	    	}
	    }
	}
	
	/**
	 * Called by parent when user presses OK
	 */
	public boolean performOk()
	{
		boolean ok = super.performOk();
		boolean readOnlySelected = cbReadonlyPrompt != null ? cbReadonlyPrompt.getSelection() : false;
		if (ok && (cbReadonlyPrompt!=null) && 
				((readOnlySelected && !wasReadOnly) || 
				 (!readOnlySelected && wasReadOnly)))
		{
   		  try
    	  {
   		   IRemoteFile remoteFile = getRemoteFile();

           // get old can write attribute
           boolean oldCanWrite = remoteFile.canWrite();

           //set readonly                   
           remoteFile.getParentRemoteFileSubSystem().setReadOnly(remoteFile,readOnlySelected, new NullProgressMonitor());

           // get the new can write attribute
           boolean updatedValue = remoteFile.canWrite();

           // check if the file is open in an editor
           SystemEditableRemoteFile editable = new SystemEditableRemoteFile(remoteFile);
           if (editable.checkOpenInEditor() != ISystemEditableRemoteObject.NOT_OPEN) {
        	   // Need to keep local copy and remote copies up to date
        	   editable.setReadOnly(readOnlySelected);
           }
                     
           // if the values haven't changed, then we need to     
           // refresh  
           ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry(); 
           
           remoteFile.markStale(true);
           
           // oldCanWrite and updatedValue may not be the same depending on the underlying file service
           // If the file service updates the underlying object, then there is no need for a remote refresh
           if (oldCanWrite == updatedValue)
           {        	   
        	   if (remoteFile.isDirectory())
        	   {
        		   sr.fireEvent(new SystemResourceChangeEvent(remoteFile.getParentRemoteFile(),ISystemResourceChangeEvents.EVENT_REFRESH_REMOTE, null));
        	   }
        	   else
        	   {
        		   sr.fireEvent(new SystemResourceChangeEvent(remoteFile,ISystemResourceChangeEvents.EVENT_REFRESH_REMOTE, null));
        	   }
           }
           else
           {	
        	   sr.fireEvent(new SystemResourceChangeEvent(remoteFile,ISystemResourceChangeEvents.EVENT_PROPERTY_CHANGE,null));     	     
           }
    	  }
   		  catch (RemoteFileIOException exc) {
             setMessage(RSEUIPlugin.getPluginMessage(ISystemMessages.FILEMSG_IO_ERROR));
    	  }
   		  catch (RemoteFileSecurityException exc) {
             setMessage(RSEUIPlugin.getPluginMessage(ISystemMessages.FILEMSG_SECURITY_ERROR));
    	  }
   		  catch (SystemMessageException e) {
   			  setMessage(e.getSystemMessage());
   		  }
		}
		
	    // set the encoding
		if (encodingFieldAdded) {
			RemoteFileEncodingManager.getInstance().setEncoding(getRemoteFile().getParentRemoteFileSubSystem().getHost().getHostName(), getRemoteFile().getAbsolutePath(), getSelectedEncoding());
		}
		
		return ok;
	}
	
    /**
     * Validate all the widgets on the page
	 * <p>
	 * Subclasses should override to do full error checking on all
	 *  the widgets on the page.
     */
    protected boolean verifyPageContents()
    {
    	return true;
    }

    public void widgetDefaultSelected(SelectionEvent event)
    {
    	
    }
    public void widgetSelected(SelectionEvent event)
    {
    	
    }

}