/********************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation. All rights reserved.
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
 * {Name} (company) - description of contribution.
 ********************************************************************************/

package org.eclipse.rse.ui.validators;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;


/**
 * This class is used to verify a user-entered source type. This is typically
 *  subsystem-dependent, such as "*.cpp" for a universal file subsystem. 
 *  However, this class is defined to be easily subclassed.
 */
public class ValidatorSourceType extends ValidatorUniqueString 
       implements ISystemValidator
{
	public static final int MAX_SRCTYPE_LENGTH = 50; // max name for a src type
		
	protected SystemMessage msg_Invalid;	
	
	/**
	 * Constructor. You must specify if src types are case-sensitive or not.
	 * Typically, you will also call setExistingNames to set the list of existing src types 
	 *  for uniqueness-validation.
	 */
	public ValidatorSourceType(boolean caseSensitive)
	{
		super(new String[0], caseSensitive);
        init();
	}	
	
	private void init()
	{
		super.setErrorMessages(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_SRCTYPE_EMPTY),
		                        RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_SRCTYPE_NOTUNIQUE));  
		msg_Invalid = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_SRCTYPE_NOTVALID);
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
	 * Overridable method for invalidate character check, beyond what this class offers
	 * @return true if valid, false if not
	 */
	protected boolean checkForBadCharacters(String newText)
	{
		return true;
	}
	
	public String toString()
	{
		return "ValidatorSourceType class"; //$NON-NLS-1$
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
	   if (newText.length() > getMaximumNameLength())
	     currentMessage = msg_Invalid;           
	   else
	     currentMessage = checkForBadCharacters(newText) ? null: msg_Invalid;
	   return currentMessage;
	}

    
    // ---------------------------
    // ISystemValidator methods...
    // ---------------------------
    
    /**
     * Return the max length for folder names: 50
     */
    public int getMaximumNameLength()
    {
    	return MAX_SRCTYPE_LENGTH;
    }

	
}