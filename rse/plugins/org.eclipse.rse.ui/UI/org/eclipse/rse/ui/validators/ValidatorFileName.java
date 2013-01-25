/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.rse.ui.validators;
import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;


/**
 * This class is used in dialogs that prompt for a name that eventually needs to become a file on disk.
 * Relies on Eclipse supplied method to test for folder name validity.
 *
 * The IInputValidator interface is used by jface's
 * InputDialog class and numerous other platform and system classes.
 */
public class ValidatorFileName 
	   extends ValidatorUniqueString implements ISystemValidator
{
	public static final int MAX_FILENAME_LENGTH = 256; 
	
	protected boolean  fUnique;
	protected SystemMessage   msg_Invalid;
	protected IWorkspace workspace = ResourcesPlugin.getWorkspace();
	
	/**
	 * Use this constructor when the name must be unique.
	 * @param existingNameList a collection of existing names to compare against.
	 * This will not be modified by the validator.
	 */
	public ValidatorFileName(Collection existingNameList)
	{
		super(existingNameList, false); // case insensitive uniqueness
		super.setErrorMessages(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_EMPTY),
		                       RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_NOTUNIQUE));  
		fUnique = true;
		msg_Invalid = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_FILENAME_NOTVALID);				
	}
	/**
	 * Use this constructor when the name must be unique. Give the
	 * ctor a string array of existing names to compare against.
	 */
	public ValidatorFileName(String existingNameList[])
	{
		super(existingNameList, false); // case insensitive uniqueness
		super.setErrorMessages(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_EMPTY),
		                       RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_NOTUNIQUE));  
		fUnique = true;
		msg_Invalid = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_FILENAME_NOTVALID);				
	}
	
	/**
	 * Use this constructor when the name need not be unique, and you just want
	 * the syntax checking.
	 */
	public ValidatorFileName()
	{
		super(new String[0], false);
		super.setErrorMessages(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_EMPTY),
		                       RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_NAME_NOTUNIQUE));  
		fUnique = true;
		msg_Invalid = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_FILENAME_NOTVALID);				
	}	
	/**
	 * Supply your own error message text. By default, messages from RSEUIPlugin resource bundle are used.
	 * @param msg_Empty error message when entry field is empty
	 * @param msg_NonUnique error message when value entered is not unique
	 * @param msg_Invalid error message when syntax is not valid
	 */
	public void setErrorMessages(SystemMessage msg_Empty, SystemMessage msg_NonUnique, SystemMessage msg_Invalid)
	{
		super.setErrorMessages(msg_Empty, msg_NonUnique);
		this.msg_Invalid = msg_Invalid;		
	}

	/**
	 * Overridable extension point to check for invalidate characters beyond what Eclipse checks for
	 * @return true if valid, false if not
	 */
	protected boolean checkForBadCharacters(String newText)
	{
		return true;
	}
	
	
	public String toString()
	{
		return "FileNameValidator class"; //$NON-NLS-1$
	}

    // ---------------------------
    // Parent Overrides...
    // ---------------------------
	/**
	 * Validate each character. 
	 * Override of parent method.
	 * Override yourself to refine the error checking.	 
	 */
	public SystemMessage isSyntaxOk(String newText)
	{
	   IStatus rc = workspace.validateName(newText, IResource.FILE);
	   if (rc.getCode() != IStatus.OK)
	     return msg_Invalid;
	   else if ((getMaximumNameLength() > 0) && // defect 42507
	            (newText.length() > getMaximumNameLength()))
	     return msg_Invalid; // TODO: PHIL. MRI. better message.             
	   return checkForBadCharacters(newText) ? null: msg_Invalid;
	}
	

    // ---------------------------
    // ISystemValidator methods...
    // ---------------------------    

    /**
     * Return the max length for file names: 256
     */
    public int getMaximumNameLength()
    {
    	return MAX_FILENAME_LENGTH;
    }

}
