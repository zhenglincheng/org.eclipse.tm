/********************************************************************************
 * Copyright (c) 2002, 2013 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Rupen Mardirossian (IBM)		 - [208435] When validating name, check for previously used names for multiple renaming instances
 * David Dykstal (IBM) - [231828] make rename dialog a bit wider
 * David McKnight    (IBM)      - [251148] {0} appears in Collision Dialog
 * David McKnight    (IBM)      - [399083] Rename of workstation file name not possibleby lowering the case of one character of the initial name
 ********************************************************************************/

package org.eclipse.rse.ui.dialogs;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.core.filters.ISystemFilterPoolReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.internal.ui.view.SystemViewResources;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.SystemWidgetHelpers;
import org.eclipse.rse.ui.messages.ISystemMessageLine;
import org.eclipse.rse.ui.validators.ISystemValidator;
import org.eclipse.rse.ui.validators.ISystemValidatorUniqueString;
import org.eclipse.rse.ui.validators.ValidatorConnectionName;
import org.eclipse.rse.ui.validators.ValidatorUniqueString;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.rse.ui.view.SystemAdapterHelpers;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;



/**
 * Dialog for renaming a single resource. Used when only one item selected.
 * <p>
 * This is a re-usable dialog that you can use  directly, or via the {@link org.eclipse.rse.internal.ui.actions.SystemCommonRenameAction}
 *  action.
 * <p>
 * To use this dialog, you must call setInputObject with a StructuredSelection of the objects to be renamed.
 * If those objects adapt to {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter} or
 * {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter}, the dialog will offer built-in error checking.
 * <p>
 * If the input object does not adapt to org.eclipse.rse.ui.view.ISystemRemoteElementAdapter or ISystemViewElementAdapter, then you
 * should call {@link #setNameValidator(org.eclipse.rse.ui.validators.ISystemValidator)} to
 * specify a validator that is called to verify the user-typed new name is valid. Further, to show the type value
 * of the input object, it should implement {@link org.eclipse.rse.ui.dialogs.ISystemTypedObject}.
 * <p>
 * This dialog does not do the actual renames. Rather, it will return the user-typed new name. This is
 * queriable via {@link #getNewName()}, after testing that {@link #wasCancelled()} is false.
 *
 * @see org.eclipse.rse.internal.ui.actions.SystemCommonRenameAction
 */
public class SystemRenameSingleDialog extends SystemPromptDialog
                                implements
                                           Runnable
{

	public static final boolean COLLISION_MODE = true;

	private Button overwriteRadio;
	//private Button renameRadio;
	private boolean overwriteMode = true;

	private Composite renameGroup;

    private Text newName;
    private String promptLabel;
    private String promptTip;
    private String newNameString;
    private String inputName =  ""; //$NON-NLS-1$
    private Label resourceTypePrompt, resourceTypeValue, verbiageLabel, renameLabel;
    private SystemMessage errorMessage;
    private ISystemValidator nameValidator;
    private ValidatorUniqueString uniqueNameValidator;
    private boolean initialized = false;
    private boolean copyCollisionMode = false;
    private boolean isRemote = true;
    private ISystemViewElementAdapter adapter = null;
    private Object inputElement = null;
    private String description = null;
    private List _namesInUse = new ArrayList();

	/**
	 * Constructor
	 */
	public SystemRenameSingleDialog(Shell shell)
	{
		this(shell, SystemResources.RESID_RENAME_TITLE);
		String singleTitle = SystemResources.RESID_RENAME_SINGLE_TITLE;
		if (!singleTitle.startsWith("Missing")) // TODO: remove test after next mri rev         	 //$NON-NLS-1$
			setTitle(singleTitle);
	}
	/**
	 * Constructor with a title
	 */
	public SystemRenameSingleDialog(Shell shell, String title)
	{
		super(shell, title);

		//pack();
		setBlockOnOpen(true);
		setHelp(RSEUIPlugin.HELPPREFIX+"drns0000"); //$NON-NLS-1$
	}

	/**
	 * Constructor with an input object and validator
	 * This constructor is in copy/move dialogs when there is a collision
	 * @param shell The parent dialog
	 * @param copyCollisionMode true if this is being called because of a name collision on a copy or move operation
	 * @param inputObject The object that is being renamed, or on a copy/move the object in the target container which already exists. Used to get the old name and the name validator
	 * @param nameValidator The name validator to use. Can be null, in which case it is queried from the adapter of the input object
	 */
	public SystemRenameSingleDialog(Shell shell, boolean copyCollisionMode, Object inputObject, ISystemValidator nameValidator)
	{
		this(shell);
		setInputObject(inputObject);
		setCopyCollisionMode(copyCollisionMode);
		setNameValidator(nameValidator);

	}
	/**
	 * Constructor with an input object and validator This constructor is in
	 * copy/move dialogs when there is a collision
	 * 
	 * @param shell The parent dialog
	 * @param copyCollisionMode true if this is being called because of a name
	 *            collision on a copy or move operation
	 * @param inputObject The object that is being renamed, or on a copy/move
	 *            the object in the target container which already exists. Used
	 *            to get the old name and the name validator
	 * @param nameValidator The name validator to use. Can be null, in which
	 *            case it is queried from the adapter of the input object
	 * @param nameInUse the List of names that have been previously selected for
	 *            other files that are to be renamed for multiple file renaming.
	 * @since 3.0 added nameInUse argument
	 */
	public SystemRenameSingleDialog(Shell shell, boolean copyCollisionMode, Object inputObject, ISystemValidator nameValidator, List nameInUse)
	{
		this(shell);
		setInputObject(inputObject);
		setCopyCollisionMode(copyCollisionMode);
		setNameValidator(nameValidator);
		_namesInUse = nameInUse;

	}
	/**
	 * Set the label and tooltip of the prompt. The default is "New name:"
	 */
	public void setPromptLabel(String label, String tooltip)
	{
		this.promptLabel = label;
		this.promptTip = tooltip;
	}

	/**
	 * Indicate this dialog is the result of a copy/move name collision.
	 * Affects the title, verbiage at the top of the dialog, and context help.
	 */
	public void setCopyCollisionMode(boolean copyCollisionMode)
	{
		if (copyCollisionMode)
		{
			if (this.inputObject != null && this.inputObject instanceof IHost)
			{
	  	      	setHelp(RSEUIPlugin.HELPPREFIX+"dccc0000"); //$NON-NLS-1$
			}
			else
			{
				setHelp(RSEUIPlugin.HELPPREFIX+"drns0001"); //$NON-NLS-1$
			}
  	      	setTitle(SystemResources.RESID_COLLISION_RENAME_TITLE);
		}
		else if (this.copyCollisionMode) // from true to false
		{
  	      	setHelp(RSEUIPlugin.HELPPREFIX+"drns0000"); //$NON-NLS-1$
			String singleTitle = SystemResources.RESID_RENAME_SINGLE_TITLE;
			if (!singleTitle.startsWith("Missing")) // TODO: remove test after next mri rev         	 //$NON-NLS-1$
				setTitle(singleTitle);
			else
  	      		setTitle(SystemResources.RESID_RENAME_TITLE); // older string we know exists
		}
		this.copyCollisionMode = copyCollisionMode;
	}
	/**
	 * Query if this dialog is the result of a copy/move name collision.
	 * Affects the title, verbiage at the top of the dialog, and context help.
	 */
	public boolean getCopyCollisionMode()
	{
		return copyCollisionMode;
	}


    /**
     * Set the validator for the new name,as supplied by the adaptor for name checking.
     * Overrides the default which is to query it from the object's adapter.
     */
    public void setNameValidator(ISystemValidator nameValidator)
    {
    	this.nameValidator = nameValidator;
    }

	/**
	 * Create message line. Intercept so we can set msg line of form.
	 */
	protected ISystemMessageLine createMessageLine(Composite c)
	{
		/*ISystemMessageLine msgLine =*/ super.createMessageLine(c);
		//form.setMessageLine(msgLine);
		return fMessageLine;
	}


	/**
	 * @see SystemPromptDialog#getInitialFocusControl()
	 */
	protected Control getInitialFocusControl()
	{
		//uSystem.out.println("here! " + (newName == null));
		return newName;
	}

	/**
	 * @see SystemPromptDialog#createInner(Composite)
	 */
	protected Control createInner(Composite parent)
	{
		// Inner composite
		int nbrColumns = 1;
		Composite composite = SystemWidgetHelpers.createComposite(parent, nbrColumns);

		Object inputObject = getInputObject();

        if (copyCollisionMode)
        {
          // VERBIAGE
          verbiageLabel = SystemWidgetHelpers.createLabel(composite, " ", nbrColumns); //$NON-NLS-1$
          SystemWidgetHelpers.createLabel(composite, " ", nbrColumns); //filler //$NON-NLS-1$
        }
       	else if (description != null)
        {
           // VERBIAGE
          verbiageLabel = SystemWidgetHelpers.createLabel(composite, description, nbrColumns);
          SystemWidgetHelpers.createLabel(composite, " ", nbrColumns); //filler //$NON-NLS-1$
        }

		if (copyCollisionMode)
		{
			overwriteRadio = SystemWidgetHelpers.createRadioButton(composite, this, SystemResources.RESID_SIMPLE_RENAME_RADIO_OVERWRITE_LABEL, SystemResources.RESID_SIMPLE_RENAME_RADIO_OVERWRITE_TOOLTIP);
			overwriteRadio.setSelection(true);
			/*renameRadio =*/ SystemWidgetHelpers.createRadioButton(composite, this, SystemResources.RESID_SIMPLE_RENAME_RADIO_RENAME_LABEL, SystemResources.RESID_SIMPLE_RENAME_RADIO_RENAME_TOOLTIP);
		}

		int nbrRenameColumns = 2;
		// BEGIN RENAME
		renameGroup = SystemWidgetHelpers.createComposite(composite, nbrRenameColumns);

        // RESOURCE TYPE
        resourceTypePrompt = SystemWidgetHelpers.createLabel(renameGroup, SystemResources.RESID_SIMPLE_RENAME_RESOURCEPROMPT_LABEL);
	    resourceTypeValue = SystemWidgetHelpers.createLabel(renameGroup, ""); //$NON-NLS-1$
	    resourceTypeValue.setToolTipText(SystemResources.RESID_SIMPLE_RENAME_RESOURCEPROMPT_TOOLTIP);


        // PROMPT
        if (promptLabel == null)
        {
			String labelText = copyCollisionMode ? SystemResources.RESID_COLLISION_RENAME_LABEL : SystemResources.RESID_SIMPLE_RENAME_PROMPT_LABEL;
			labelText = SystemWidgetHelpers.appendColon(labelText);
			renameLabel = SystemWidgetHelpers.createLabel(renameGroup, labelText);
       }
		else
		{
			renameLabel = SystemWidgetHelpers.createLabel(renameGroup, promptLabel);
		}
		newName = SystemWidgetHelpers.createTextField(renameGroup, null);
		((GridData)newName.getLayoutData()).widthHint = 300; // make this wider than usual so error messages are not truncated
		if (promptTip != null) {
			newName.setToolTipText(promptTip);
		}

       // END RENAME



		if (inputObject != null)
		{
		   initializeInput();
		}

		// init ok to disabled, until they type a new name
		setPageComplete(false);

		// add keystroke listeners...
		newName.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validateNameInput();
				}
			}
		);

		if (copyCollisionMode)
			{
			   enableRename(false);
			}


		return composite;
	}


	/**
	 * Override of parent. Must pass selected object onto the form for initializing fields.
	 * Called by SystemDialogAction's default run() method after dialog instantiated.
	 */
	public void setInputObject(Object inputObject)
	{
		//System.out.println("INSIDE SETINPUTOBJECT: " + inputObject + ", "+inputObject.getClass().getName());
		super.setInputObject(inputObject);
		if (newName != null)
		{
			initializeInput();
		}
	}

	private void initializeInput()
	{
		if (!initialized)
		{
		  inputElement = getInputElement(inputObject);
          adapter = getViewAdapter(inputElement);
          if (adapter != null)
		    inputName = adapter.getName(inputElement);
		  else if (inputElement instanceof ISystemTypedObject)
		    inputName = ((ISystemTypedObject)inputElement).getName();
		  else if (inputElement instanceof IResource)
		    inputName = ((IResource)inputElement).getName();
		  else if (inputElement instanceof String)
		    inputName = (String)inputElement;
		  newName.setText(inputName);

		  if (copyCollisionMode)
		  {
			  String msg = NLS.bind(SystemResources.RESID_COLLISION_RENAME_VERBIAGE, inputName);
			  verbiageLabel.setText(msg);
		  }



		  if ((nameValidator == null) && (adapter != null))
		    nameValidator = adapter.getNameValidator(inputElement);
		  if (nameValidator != null)
		  {
		  	int maxLen = nameValidator.getMaximumNameLength();
		  	if (maxLen != -1)
		  	  newName.setTextLimit(maxLen);
		  }
		  // test if we need a unique name validator
		  Shell shell = getShell();
		  Display display = shell.getDisplay();
		  if (display != null)
		    display.asyncExec(this);
		  else
            run();

          // the rename action for system filter pool reference selections is really
          //  a rename of the actual pool, versus the reference...
          if (inputElement instanceof ISystemFilterPoolReference)
          {
            inputElement = ((ISystemFilterPoolReference)inputElement).getReferencedFilterPool();
            adapter = getViewAdapter(inputElement);
          }

          if (adapter != null)
            resourceTypeValue.setText(adapter.getType(inputElement));
          else if (inputElement instanceof ISystemTypedObject)
            resourceTypeValue.setText(((ISystemTypedObject)inputElement).getType());
    	  else if (inputElement instanceof IResource)
    	  {
    			if ((inputElement instanceof IFolder) || (inputElement instanceof IProject))
    			  resourceTypeValue.setText(SystemViewResources.RESID_PROPERTY_FILE_TYPE_FOLDER_VALUE);
    			else
    			  resourceTypeValue.setText(SystemViewResources.RESID_PROPERTY_FILE_TYPE_FILE_VALUE);
    	  }
          initialized = true;
		}
	}

	/**
	 * Runnable method
	 */
	public void run()
	{
		uniqueNameValidator = getUniqueNameValidator(inputElement, nameValidator);
	}

	/**
	 * Given an input element and externally-suppplied name validator for it, determine if we
	 *  need to augment that validator with one that will check for uniqueness, and if so
	 *  create and return that uniqueness validator
	 */
	protected ValidatorUniqueString getUniqueNameValidator(Object inputElement, ISystemValidator nameValidator)
	{
		ValidatorUniqueString uniqueNameValidator = null;
		ISystemRemoteElementAdapter ra = getRemoteAdapter(inputElement);
		if (ra != null)
		{
		   isRemote = true;
           String[] names = null;
           boolean debug = false;
           boolean caseSensitive = ra.getSubSystem(inputElement).getSubSystemConfiguration().isCaseSensitive();
           boolean needUniqueNameValidator = !(nameValidator instanceof ISystemValidatorUniqueString);
           if (!needUniqueNameValidator)
           {
           	 String[] existingNames = ((ISystemValidatorUniqueString)nameValidator).getExistingNamesList();
		  	 needUniqueNameValidator = ((existingNames == null) || (existingNames.length==0));
           }
		   if (needUniqueNameValidator)
		   {
    		  // Set the busy cursor to all shells.
    		  super.setBusyCursor(true);
		  	  try {
		  	  		Shell shell = getShell();
		  	  		IRunnableContext irc = RSEUIPlugin.getTheSystemRegistryUI().getRunnableContext();
					RSEUIPlugin.getTheSystemRegistryUI().clearRunnableContext();
		  	        names = ra.getRemoteParentNamesInUse(inputElement, null);
					RSEUIPlugin.getTheSystemRegistryUI().setRunnableContext(shell, irc);
		  	  } catch (Exception exc) {SystemBasePlugin.logError("Exception getting parent's child names in rename dialog",exc);}		  			 //$NON-NLS-1$
		  	  if ((names != null) && (names.length>0))
		  	  {
		  		    uniqueNameValidator = new ValidatorUniqueString(names,caseSensitive);
		            uniqueNameValidator.setErrorMessages(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_EMPTY),
		                                                 RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_NOTUNIQUE));
		            if (debug)
		            {
		  		      System.out.println("Name validator set. Names = "); //$NON-NLS-1$
		  		      for (int idx=0; idx<names.length; idx++)
		  		         System.out.println("..."+idx+": "+names[idx]); //$NON-NLS-1$ //$NON-NLS-2$
		            }
		  	  }
    		  // Restore cursor
    		  super.setBusyCursor(false);
		   }
		}
		else
		   isRemote = false;
		newName.setFocus();
		return uniqueNameValidator;
	}

    /**
     * Returns the selected element given the current input, which is
     * an IStructuredSelection.
     */
    protected Object getInputElement(Object inputObject)
    {
    	if (inputObject instanceof IStructuredSelection)
    	{
    		inputObject = ((IStructuredSelection)inputObject).getFirstElement();
    	}
    	if (inputObject instanceof SystemSimpleContentElement)
    	{
    		inputObject = ((SystemSimpleContentElement)inputObject).getData();
    	}
		return inputObject;
    }
    /**
     * Returns the implementation of ISystemViewElement for the given
     * object.  Returns null if the adapter is not defined or the
     * object is not adaptable.
     */
    protected ISystemViewElementAdapter getViewAdapter(Object o)
    {
    	return SystemAdapterHelpers.getViewAdapter(o);
    }
    /**
     * Returns the implementation of ISystemRemoteElement for the given
     * object.  Returns null if this object does not adaptable to this.
     */
    protected ISystemRemoteElementAdapter getRemoteAdapter(Object o)
    {
    	return SystemAdapterHelpers.getRemoteAdapter(o);
    }

	/**
	 * Called when user presses OK button.
	 * Return true to close dialog.
	 * Return false to not close dialog.
	 */
	protected boolean processOK()
	{
		newNameString = newName.getText();
		if (!isRemote)
			newNameString = newNameString.trim();
		else
			newNameString = trimTrailing(newNameString); // defect 43173
		boolean closeDialog = verify();
		if (closeDialog)
		{
			if (inputElement instanceof IHost)
			{
				closeDialog = ValidatorConnectionName.validateNameNotInUse(newNameString, getShell());
				if (!closeDialog)
					newName.setFocus();
			}
		}
		if (closeDialog)
		{
			setOutputObject(newNameString);
		}
		return closeDialog;
	}
    /**
     * Trim leading blanks
     */
    public static String trimTrailing(String text)
    {
    	return ("."+text).trim().substring(1); //$NON-NLS-1$
    }

	/**
	 * Verifies all input.
	 * @return true if there are no errors in the user input
	 */
	public boolean verify()
	{
		if (copyCollisionMode && overwriteMode)
			return true;

		SystemMessage errMsg = null;
		Control controlInError = null;
		clearErrorMessage();
		errMsg = validateNameInput(newNameString);
		if (errMsg != null) {
			controlInError = newName;
			controlInError.setFocus();
		}
		else if (isRemote && checkIfWillBeFilteredOut(inputElement, newNameString)) {
			return false;
		}
		return (errMsg == null);
	}

	/**
	 * If renaming a remote object, which is listed in a tree view under an expanded filter,
	 *  this tests to see if the rename will cause that object to suddenly not meet the filtering
	 *  criteria of the parent filter, and hence disappear. If so, issue warning message before
	 *  proceeding.
	 * @return true if will be filtered out and user chose to cancel operation
	 */
	protected boolean checkIfWillBeFilteredOut(Object remoteObject, String newName)
	{
		// after breaking my pick on this, I have decide it simply is too much work, and
        //  too error-prone, to do. The reason is that we really cannot accurately determine
        //  if the new name will meet the criteria of the filter, short of re-resolving the
        //  filter and looking for the new name. That is just too expensive, I think. Phil.
        //  See defect 42094.
        return false;
        /*
		boolean ok = false;
		// step 1: check if we are invoked from a tree view
		if (inputTreeViewer == null)
		  return false;
		// step 2: check if the parent of the selected object is a filter
		if (inputTreeViewer.getSelectedParent() instanceof SystemFilterReference)
		{
			SystemFilterReference parentFilterRef = (SystemFilterReference)inputTreeViewer.getSelectedParent();
			SystemFilter parentFilter = parentFilterRef.getReferencedFilter();
			// step 3: check if the new name meets the criteria of the filter
			SubSystem ss = getRemoteAdapter(remoteObject).getSubSystem(remoteObject);
			boolean matches = ss.doesFilterMatch(parentFilter, newName);
			if (!matches)
			{
				// todo: issue warning msg 1311, and allow user to cancel operation
			}
		}
		return ok;
        */
	}

  	/**
  	 * Called directly as user types.
	 */
	protected SystemMessage validateNameInput()
	{
		newNameString = newName.getText();
		if (!isRemote)
		  newNameString = newNameString.trim();
		else
		  newNameString = trimTrailing(newNameString); // defect 43173


		return validateNameInput(newNameString);
	}
  	/**
	 * Called directly from verify.
	 */
	protected SystemMessage validateNameInput(String theNewName)
	{
		boolean windowsDiffersByCase = false;
	    errorMessage= null;

	    if (theNewName == null)
	    {
	    	errorMessage = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_EMPTY);
	    }
	    else
	    {
		if (nameValidator != null)
	      errorMessage= nameValidator.validate(theNewName);
		else if (theNewName.length() == 0)
		  errorMessage = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_EMPTY);
		if (errorMessage == null)
		{
          if (adapter != null && adapter.namesAreEqual(inputElement, theNewName)){
       		  errorMessage = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_RENAME_OLDEQUALSNEW).makeSubstitution(inputName);       		 
       		  // need to allow changes in case though on Windows
       		  IHost host = adapter.getSubSystem(inputElement).getHost();
        	  if (host.getSystemType().isWindows()){
        		  String originalName = adapter.getName(inputElement);
        		  if (!originalName.equals(theNewName)){ // doesn't match case
        			 // therefore this is legal
        			  windowsDiffersByCase = true;
        			  errorMessage = null;
        		  }
        	  }
          }
	    }
	    }

		if ((errorMessage == null) && (uniqueNameValidator != null)){
		  errorMessage = uniqueNameValidator.validate(theNewName);
		  if (errorMessage != null && windowsDiffersByCase){
			  errorMessage = null;
		  }
		}
		if(_namesInUse != null && _namesInUse.size()>0)
		{
			for(int i=0;i<_namesInUse.size();i++)
			{
				if(theNewName.equals(_namesInUse.get(i)))
				{
					errorMessage = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_ENTRY_NOTUNIQUE);
				}
			}
		}
		if (errorMessage != null)
		  setErrorMessage(errorMessage);
		else
		  clearErrorMessage();
		setPageComplete();
		return errorMessage;
	}

	/**
	 * This method can be called by the dialog or wizard page host, to decide whether to enable
	 * or disable the next, final or ok buttons. It returns true if the minimal information is
	 * available and is correct.
	 */
	public boolean isPageComplete()
	{
		boolean pageComplete = false;

		if (copyCollisionMode && overwriteMode)
				{
					return true;
				}


		if (errorMessage == null)
		{
		  String theNewName = newName.getText().trim();
		  pageComplete = (theNewName.length() > 0);
		  if (pageComplete && adapter != null)
		  {
		  	pageComplete = !adapter.namesAreEqual(inputElement, theNewName);
		  	//System.out.println("back from namesAreEqual: " + pageComplete);

			if (!pageComplete){
				IHost host = adapter.getSubSystem(inputElement).getHost();
				boolean isWindows = host.getSystemType().isWindows();
				if (isWindows){
					String originalName = adapter.getName(inputElement);
					if (!originalName.equals(theNewName)){ // doesn't match case
						pageComplete = true;
					}
				}
			}
		  }
		}
		return pageComplete;
	}

	/**
	 * Inform caller of page-complete status of this form
	 */
	public void setPageComplete()
	{
		setPageComplete(isPageComplete());
	}

	/**
	 * Returns the user-entered new name
	 */
	public String getNewName()
	{
		return newNameString;
	}

	/**
	 * Returns the user-entered new name as an array for convenience to ISystemRenameTarget hosts.
	 */
	public String[] getNewNameArray()
	{
		String[] newNames = new String[1];
		newNames[0] = newNameString;
		return newNames;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getDescription()
	{
		return this.description;
	}

	public void handleEvent(Event e)
	{
		Widget source = e.widget;
		if (source == overwriteRadio)
		{
				enableRename(!overwriteRadio.getSelection());
		}
	}

	private void enableRename(boolean flag)
	{
		if (newName != null)
		{
			renameLabel.setEnabled(flag);
			newName.setEnabled(flag);

			resourceTypePrompt.setEnabled(flag);
			resourceTypeValue.setEnabled(flag);

			overwriteMode = !flag;
			setPageComplete();

		}
	}
}