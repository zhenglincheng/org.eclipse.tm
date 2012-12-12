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
 * Uwe Stieber (Wind River) - Reworked new connection wizard extension point.
 * Martin Oberhuber (Wind River) - [216252] canceled --> cancelled in comments
 *******************************************************************************/

package org.eclipse.rse.ui.wizards;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.Mnemonics;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemWidgetHelpers;
import org.eclipse.rse.ui.messages.ISystemMessageLine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;



/**
 * Abstract class for system wizards pages. Using this class is most effective when used in
 *  conjunction with {@link org.eclipse.rse.ui.wizards.AbstractSystemWizard}.
 * <p> Using this base class for wizards offers the following advantages over just using the 
 * eclipse WizardPage class:
 * <ul>
 *  <li>Designed to work in conjunction with {@link org.eclipse.rse.ui.wizards.AbstractSystemWizard},
 *      propagating settings from the wizard to the individual wizard pages.
 *  <li>Supports using the overall wizard page title as set by {@link org.eclipse.rse.ui.wizards.AbstractSystemWizard#setWizardPageTitle(String)}, 
 *      if no title specified in the constructor.
 *  <li>Supports a {@link #setHelp(String)} method to set the wizard page's overall contextual help
 *  <li>Implements {@link org.eclipse.rse.ui.messages.ISystemMessageLine} so supports setting error messages as
 *       either strings or, preferably, {@link SystemMessage} objects.
 *  <li>Supports an {@link #setInputObject(Object) input-object}, as passed from the wizard, which in turn is passed from the action.
 *  <li>Supports automatic assignment of unique mnemonics for all input-capable controls on the page, to add in accessibility.
 *  <li>Supports setting a default-focus control, which gets initial focus when the page is shown.
 *  <li>Supports helper methods to aid in population of the client area: {@link #addSeparatorLine(Composite, int)}, 
 *      {@link #addFillerLine(Composite,int)} and {@link #addGrowableFillerLine(Composite,int)}.
 * </ul>
 * 
 * <p>To use this class, :</p>
 * <ol>
 *  <li>Subclass it.
 *  <li>Decide whether to use the overall wizard default pages title or have a unique title for this page. Use the appropriate constructor.
 *  <li>If desired, in your constructor call {@link #setHelp(String)} to set the contextual help for this page. Or, just use the default help 
 *      set via {@link org.eclipse.rse.ui.wizards.AbstractSystemWizard#setHelp(String)} in the wizard itself.
 *  <li>Override {@link #createContents(Composite)} to populate the client area. You may find it useful to use the static methods in {@link org.eclipse.rse.ui.SystemWidgetHelpers}.
 *  <li>Override {@link #getInitialFocusControl()} to return the control to gain initial focus on this page, or null if there are no input controls.
 *  <li>Override {@link #performFinish()} to perform input validation when Finish is pressed. Return true if validated ok, false if not.
 *  <li>You may also wish to override {@link #isPageComplete()} to return false if any required inputs are not given. Typically, this also is coded to 
 *       return false if there is an error message showing, which you detect by maintaining a SystemMessage instance variable, set or cleared by your
 *       keystroke validator methods, and performFinish validation routine.
 *  <li>Typically you will also supply protected getter methods to get the user-entered data, for use by your wizard class.
 * </ol>
 * 
 * <p>For error validation when there are multiple input fields on the page, there are two different approaches you can take:</p>
 * <ol>
 *  <li>Just validate each field as data is entered. In the event processor for that field, you clear the message, then set it if an error is
 *   detected in the input data, and also call setPageComplete(errorMessage == null), where errorMessage is your instance variable for the current error.
 *   In performFinish, you then call the validation methods for each of the fields on the page, and if an error is found, position the cursor, set the
 *   error message and then call setPageComplete. <br>
 *   In this approach, only errors in the current field in focus are caught, and errors in other fields are not caught until Finish is pressed.
 * <li>Same as in step 1, but if the error checking for the current field finds no errors, then an overall validation method is called to check
 *   the other fields, passing a parameter identifying the current field so it is skipped for efficiency. The overall validation method calls all
 *   the individual validation methods, except for the one that was asked to be skipped. The performFinish method calls the overall validation method,
 *   passing null so nothing is skipped, and if an error is detected positions the cursor. <br>
 *   In this approach, which is more rigorous, the error checking is always complete for the whole page, so Finish theoretically will never catch an
 *   error, and the page enablement is always completely accurate.
 * </ol>
 * <p>There is no consensus on the approach, although clearly the second one is preferable when it is possible.
 * 
 * @see org.eclipse.rse.ui.wizards.AbstractSystemWizard
 * @see org.eclipse.rse.ui.dialogs.SystemWizardDialog
 * @see org.eclipse.rse.ui.actions.SystemBaseWizardAction
 */
public abstract class AbstractSystemWizardPage 
	   extends WizardPage
	   implements ISystemWizardPage, ISystemMessageLine
{
	// state
	private Object input;
	private ISystemMessageLine msgLine;
	private String  helpId;
	private Composite parentComposite;
	private SystemMessage pendingMessage, pendingErrorMessage;
	
	/**
	 * Constructor when a unique page title is desired.
	 * @param wizard - the page wizard.
	 * @param pageName - the untranslated ID of this page. Not really used.
	 * @param pageTitle - the translated title of this page. Appears below the overall wizard title. 
	 * @param pageDescription - the translated description of this page. Appears to the right of the page title.
	 */
	public AbstractSystemWizardPage(IWizard wizard, String pageName, String pageTitle, String pageDescription) {
		super(pageName);
		setWizard(wizard);
		
		if (pageTitle != null)
			setTitle(pageTitle);
		else if (wizard instanceof AbstractSystemWizard)
			setTitle(((AbstractSystemWizard)wizard).getWizardPageTitle());
		
		setDescription(pageDescription);
	}
	/**
	 * Constructor when the overall wizard page title is desired, as specified in
	 * {@link org.eclipse.rse.ui.wizards.AbstractSystemWizard#setWizardPageTitle(String)}.
	 * <p>
	 * It is a somewhat common design pattern to use the same title for all pages in a wizard, and this makes it easy to
	 * do that.
	 * <p>
	 * Your wizard must extend AbstractSystemWizard, and you must have called setWizardPageTitle.
	 * @param wizard - the page's wizard.
	 * @param pageName - the untranslated ID of this page. Not really used.
	 * @param pageDescription - the translated description of this page. Appears to the right of the page title.
	 */
	public AbstractSystemWizardPage(IWizard wizard,
  								    String pageName, String pageDescription)
	{
		this(wizard, pageName, null, pageDescription);
	}
	
	// ------------------------
	// CONFIGURATION METHODS...
	// ------------------------
	
    /**
     * Configuration method. <br>
     * For setting the overall help for the wizard page.
     * <p>
     * This id is stored, and then applied to each of the input-capable
     * controls in the main composite returned from createContents.
     */
    public void setHelp(String helpId)
    {
    	if (parentComposite != null)
    	  SystemWidgetHelpers.setHelp(parentComposite, helpId);		
    	  //SystemWidgetHelpers.setCompositeHelp(parentComposite, helpId, helpIdPerControl);		
    	//System.out.println("Setting help to " + helpId);
    	this.helpId = helpId;
    }
     
	/**
     * Configuration method. <br>
	 * For explicitly setting input object. Automatically propogated by the parent wizard. 
	 */
	public void setInputObject(Object inputObject)
	{
		this.input = inputObject;		
	}

	// ------------------------
	// GETTER METHODS...
	// ------------------------
	/**
     * Getter method. <br>
	 * For explicitly getting input object.
	 */
	public Object getInputObject()
	{
		return input;
	}
    /**
     * Getter method. <br>
     * Return the help Id as set in {@link #setHelp(String)}
     */
    public String getHelpContextId()
    {
    	return helpId;
    }
    /**
     * Getter method. <br>
     * Return this page's message line so it can be passed to re-usable widgets that need it
     */	
	public ISystemMessageLine getMessageLine()
	{
		 //return msgLine;
		 return this;
	}		
		
    // ----------------
    // ABSTRACT METHODS
    // ---------------- 
	/**
     * Abstract method. <br>
	 * Create the page contents here.
	 * <p>
	 * You may find it useful to use the static methods in {@link org.eclipse.rse.ui.SystemWidgetHelpers}.
	 * If you do keystroke validation, you should call {@link #setErrorMessage(SystemMessage)} if you detect errors, and also
	 *  {@link #setPageComplete(boolean)} to affect the enablement of the next and finish buttons.
	 * 
	 * @see org.eclipse.rse.ui.SystemWidgetHelpers
	 */
	public abstract Control createContents(Composite parent);

	/**
     * Abstract method. <br>
	 * Return the Control to be given initial focus.
	 * <p>
	 * Child classes must override this, but can return null.
	 */
	protected abstract Control getInitialFocusControl();

    /**
     * Abstract method. <br>
     * Perform error checking of the page contents, returning true only if there are no errors. 
     * <p>Called by the main wizard when the user presses Finish. The operation will be cancelled if 
     * this method returns false for any page.
     */
	public abstract boolean performFinish();    

    // -----------------------
    // PARENT-OVERRIDE METHODS
    // ----------------------- 
	
	public void dispose() {
		super.dispose();
		
		// Once the page is disposed, the widgets are not longer accessible
		msgLine = null;
		input = null;
		parentComposite = null;
		pendingMessage = null;
		pendingErrorMessage = null;
	}
	
	/**
	 * Parent override. <br>
	 * Creates the wizard's UI component.
	 * We set mnemonics. Child classes should NOT USE THIS.
	 * Child classes should override {@link #createContents(Composite)}, which this calls.
	 */
	public void createControl(Composite parent) 
	{
// dwd		parentComposite = parent;
		Composite myComposite = new Composite(parent, SWT.NONE);
		myComposite.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, true);
		myComposite.setLayoutData(gd);
		parentComposite = myComposite;
		Control c = createContents(myComposite);
		if (c instanceof Composite)
		{
		  applyMnemonics((Composite)c);
		  parentComposite = (Composite)c;
		  if (helpId != null)
    	    SystemWidgetHelpers.setHelp(parentComposite, helpId); 
    	   // SystemWidgetHelpers.setCompositeHelp((Composite)c, helpId, helpIdPerControl);		    
		}
		else if (c instanceof Button)
		{
			Mnemonics ms = new Mnemonics();
			ms.setMnemonic((Button)c);
		}		

		msgLine = new RSEDialogPageMessageLine(this);
		if (pendingMessage != null) setMessage(pendingMessage);
		if (pendingErrorMessage != null) setErrorMessage(pendingErrorMessage);

		setControl(myComposite);
	}
	
	/**
	 * Apply mnemonic to the content composite.
	 * @param c the composite.
	 */
	protected void applyMnemonics(Composite c) {
		SystemWidgetHelpers.setWizardPageMnemonics(c);		
	}

    /**
	 * Parent override. <br>
     * We intercept to give the initial-focus-control focus.
     */
    public void setVisible(boolean visible) 
    {
	    super.setVisible(visible);	
	    if (visible)
	    {
	   	  Control c = getInitialFocusControl();
	   	  if (c != null)
	   	    c.setFocus();
	    }
    }   
	
  // -----------------------------
  // ISystemMessageLine methods...
  // -----------------------------

  /* (non-Javadoc)
   * @see org.eclipse.rse.ui.messages.ISystemMessageLine#clearErrorMessage()
   */
  public void clearErrorMessage() 
	{
		if (msgLine != null && getControl() != null && !getControl().isDisposed())
			msgLine.clearErrorMessage();
	}

  /* (non-Javadoc)
   * @see org.eclipse.rse.ui.messages.ISystemMessageLine#clearMessage()
   */
	public void clearMessage() 
	{
		if (msgLine != null && getControl() != null && !getControl().isDisposed())
			msgLine.clearMessage();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.messages.ISystemMessageLine#getSystemErrorMessage()
	 */
	public SystemMessage getSystemErrorMessage() 
	{
		if (msgLine!=null)
		  return msgLine.getSystemErrorMessage();
	    else
		  return null;
	}		

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.messages.ISystemMessageLine#setErrorMessage(org.eclipse.rse.services.clientserver.messages.SystemMessage)
	 */
	public void setErrorMessage(SystemMessage message) 
	{
		if (msgLine!=null)
		{
			if (message != null)
			  msgLine.setErrorMessage(message);
			else
			  msgLine.clearErrorMessage();
		}
	    else // not configured yet
	        pendingErrorMessage = message;
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.messages.ISystemMessageLine#setErrorMessage(java.lang.Throwable)
	 */
	public void setErrorMessage(Throwable exc)
	{
		if (msgLine != null)
		  msgLine.setErrorMessage(exc);
		else
		{
		   SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_ERROR_UNEXPECTED);
		   msg.makeSubstitution(exc);
           pendingErrorMessage = msg;			
		}
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.messages.ISystemMessageLine#setMessage(org.eclipse.rse.services.clientserver.messages.SystemMessage)
	 */
	public void setMessage(SystemMessage message) 
	{
		if (msgLine!=null)
			msgLine.setMessage(message);
	    else // not configured yet
	        pendingMessage = message;
	}

	/**
	 * <b>Note:</b> Never override this method as long the {@link ISystemMessageLine}
	 * construct is not eliminated from RSE! Overriding does lead easely to {@link StackOverflowError}s
	 * if the subclass implementation does call the wrong methods!
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#setErrorMessage(java.lang.String)
	 */
	public void setErrorMessage(String message) {
		super.setErrorMessage(message);
	}

	/**
	 * <b>Note:</b> Never override this method as long the {@link ISystemMessageLine}
	 * construct is not eliminated from RSE! Overriding does lead easily to {@link StackOverflowError}s
	 * if the subclass implementation does call the wrong methods!
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#setMessage(java.lang.String, int)
	 */
	public void setMessage(String newMessage, int newType) {
		super.setMessage(newMessage, newType);
	}
	
	/**
	 * <b>Note:</b> Never override this method as long the {@link ISystemMessageLine}
	 * construct is not eliminated from RSE! Overriding does lead easily to {@link StackOverflowError}s
	 * if the subclass implementation does call the wrong methods!
	 * 
	 * @see org.eclipse.jface.dialogs.DialogPage#setMessage(java.lang.String)
	 */
	public void setMessage(String message) {
		super.setMessage(message);
	}
    
    // ---------------
    // HELPER METHODS
    // ---------------  

    /**
     * Helper method <br>
     * Add a separator line. This is a physically visible line.
     */
	protected void addSeparatorLine(Composite parent, int nbrColumns)
	{
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);	
	    GridData data = new GridData();
	    data.horizontalSpan = nbrColumns;
	    data.horizontalAlignment = GridData.FILL;
	    separator.setLayoutData(data);		
	}
	/**
     * Helper method <br>
	 * Add a spacer line
	 */
	protected void addFillerLine(Composite parent, int nbrColumns)
	{
		Label filler = new Label(parent, SWT.LEFT);	
	    GridData data = new GridData();
	    data.horizontalSpan = nbrColumns;
	    data.horizontalAlignment = GridData.FILL;
	    filler.setLayoutData(data);		
	}
	/**
     * Helper method <br>
	 * Add a spacer line that grows in height to absorb extra space
	 */
	protected void addGrowableFillerLine(Composite parent, int nbrColumns)
	{
		Label filler = new Label(parent, SWT.LEFT);	
	    GridData data = new GridData();
	    data.horizontalSpan = nbrColumns;
	    data.horizontalAlignment = GridData.FILL;
	    data.verticalAlignment = GridData.FILL;
        data.grabExcessVerticalSpace = true;
	    filler.setLayoutData(data);		
	}

    // ----------------
    // INTERNAL METHODS
    // ---------------- 
	
    /**
	 * Internal method <br>
     * On Finish, when an error is detected, position to the given
     * control. The trick though is to give this page focus if it
     * doesn't have it.
     */
    protected void setFocus(Control control)
    {
    	if (this != getContainer().getCurrentPage())
    	  getContainer().showPage(this);
    	if ((control!=null) && !control.isDisposed())
    	  control.setFocus();
    }
}