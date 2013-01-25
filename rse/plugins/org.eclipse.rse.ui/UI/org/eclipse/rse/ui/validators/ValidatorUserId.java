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
 * This class is used in dialogs that prompt for a userId.
 * This does very basic userId validation, just to ensure there are no problems when the
 *  user Id is saved in the preferences. This means restricting use of a couple special characters
 *  that would mess up the key/value processing of the preference data.
 *
 * The IInputValidator interface is used by jface's
 * InputDialog class and numerous other platform and system classes.
 */
public class ValidatorUserId 
	   extends ValidatorSpecialChar
{
	/**
	 * Constructor
	 */
	public ValidatorUserId(boolean isEmptyAllowed)
	{
		super("=;", isEmptyAllowed, RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_USERID_NOTVALID), RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_USERID_EMPTY)); //$NON-NLS-1$
	}

	/**
	 * We could do additional syntax checking here if we decide to.
	 * This method is called by parent class if all other error checking passes.
	 */
	public SystemMessage isSyntaxOk(String newText)
	{
		return null;
	}
}