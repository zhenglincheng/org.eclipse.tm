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
 * David Dykstal (IBM) - generalized Vector argumnents to Collections
 * Kevin Doyle (IBM) - 174776: perform required sorting of Collection arguments
 * David Dykstal (IBM) - 174776: disallowed sorting of input arguments, used copies
 *******************************************************************************/

package org.eclipse.rse.ui.validators;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;


/**
 * This class is used in dialogs that prompt for a string
 * that has to be unique. Unless you use the constructor that takes another
 * IInputValidator as input, no syntax checking is done other than checking
 * the input is non-empty and unique.
 *
 * The IInputValidator interface is implemented by our parent and it
 * is used by jface's InputDialog class and property sheet window.
 */
public class ValidatorUniqueString 
	   implements ISystemValidator, ISystemValidatorUniqueString
	              //,IInputValidator, ICellEditorValidator ... ISystemValidator extends these
{
	
	public static final boolean CASE_SENSITIVE = true;
	public static final boolean CASE_INSENSITIVE = false;
	public static final char QUOTE = '\"';
	private static final String[] EMPTY_LIST = new String[0];
	
	protected ISystemValidator syntaxValidator;
	protected boolean          caseSensitive;
	protected boolean          useUpperCase;
	protected String           existingList[];
	protected SystemMessage    msg_Empty;
	protected SystemMessage    msg_NonUnique;	
	protected SystemMessage    currentMessage;
	
	/**
	 * @param existingList a collection of existing strings to compare against.
	 * Note that toString() is used to get the string from each item.
	 * @param caseSensitive true if comparisons are to be case sensitive, false if case insensitive.	 
	 */
	public ValidatorUniqueString(Collection existingList, boolean caseSensitive)
	{
		this.caseSensitive = caseSensitive;		
		setExistingNamesList(existingList);
		// initialize error messages
		setErrorMessages(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_ENTRY_EMPTY),
		                 RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_ENTRY_NOTUNIQUE));
	}
	/**
	 * Constructor accepting an Array. 
	 * @param existingList An array containing list of existing strings to compare against.
	 * @param caseSensitive true if comparisons are to be case sensitive, false if case insensitive.
	 */
	public ValidatorUniqueString(String existingList[], boolean caseSensitive)
	{
		this.caseSensitive = caseSensitive;		
		init(existingList, caseSensitive);
		// initialize error messages
		setErrorMessages(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_ENTRY_EMPTY),
		                 RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_ENTRY_NOTUNIQUE));
	}
	/**
	 * Constructor accepting a collection and another validator to use for the syntax checking.
	 * @param existingList A collection of existing strings to compare against.
	 * Note that toString() is used to get the string from each item.	 
	 * @param caseSensitive true if comparisons are to be case sensitive, false if case insensitive.
	 * @param syntaxValidator Another IInputValidator who does the job of checking the syntax. After
	 *         checking for non-nullness and uniqueness, this validator is used to 
	 *         check for syntax.
	 */
	public ValidatorUniqueString(Collection existingList, boolean caseSensitive, 
	                             ISystemValidator syntaxValidator)
	{
		this(existingList, caseSensitive);
		this.syntaxValidator = syntaxValidator;
	}
	/**
	 * Constructor accepting an Array and another validator to use for the syntax checking.
	 * @param existingList An array containing list of existing strings to compare against.
	 * @param caseSensitive true if comparisons are to be case sensitive, false if case insensitive.
	 * @param syntaxValidator Another IInputValidator who does the job of checking the syntax. After
	 *         checking for non-nullness and uniqueness, this validator is used to 
	 *         check for syntax.
	 */
	public ValidatorUniqueString(String existingList[], boolean caseSensitive, 
	                             ISystemValidator syntaxValidator)
	{
		this(existingList, caseSensitive);
		this.syntaxValidator = syntaxValidator;
	}
	/**
	 * Reset whether this is a case-sensitive list or not
	 */
	public void setCaseSensitive(boolean caseSensitive)
	{
		this.caseSensitive = caseSensitive;
	}
	/**
	 * For case-insensitive, we typically fold to lowercase, affecting what 
	 *  this user sees in the substitution value of error messages.
	 * Call this so the value substituted is uppercase vs lowercase.
	 */
	public void setUseUpperCase()
	{
		this.useUpperCase = true;
	}
	
	/**
	 * Reset the collection of existing names.
	 * The collection will not be modified by the validator.
	 */
	public void setExistingNamesList(Collection newList)
	{		
		if (newList == null)
		  existingList = null;
		else
		{
		  existingList = new String[newList.size()];
		  newList.toArray(existingList);
		  init(existingList, caseSensitive);
		}
	}
	/**
	 * Reset the existing names list. 
	 */
	public void setExistingNamesList(String[] existingList)
	{		
		if (existingList == null)
		  this.existingList = null;
		else
		  init(existingList, caseSensitive);
	}
	
	/**
	 * Return the existing names list. This will be a case-normalized and sorted list.
	 */
	public String[] getExistingNamesList()
	{
		return existingList;
	}
	
	/**
	 * Initialize sorted array.
	 */
	private void init(String names[], boolean caseSensitive) {
		if (names == null) {
			existingList = EMPTY_LIST;
		} else {
			existingList = new String[names.length];
			System.arraycopy(names, 0, existingList, 0, names.length);
			// TODO (dwd 20070530) this should be using ICU4J to fold case.
			if (!caseSensitive) { // speed up comparison by converting to all lowercase
				for (int idx = 0; idx < existingList.length; idx++) {
					String string = existingList[idx];
					boolean quoted = (string.indexOf(QUOTE) != -1);
					if (!quoted) {
						existingList[idx] = string.toLowerCase();
					} else {
						existingList[idx] = quotedToLowerCase(string);
					}
				}
			}
			Arrays.sort(existingList); // Arrays is a utility class in java.util. New in JDK 1.2
		}
	}
	 
	/**
	 * Special-case way to fold non-quoted parts of a string to lowercase
	 */
	public static String quotedToLowerCase(String input)
	{
		StringBuffer buffer = new StringBuffer(input.length());
		//System.out.println("INPUT : " + input);
		boolean inQuotes = false;
		for (int idx=0; idx<input.length(); idx++)
		{
			char c = input.charAt(idx);
			if (c == QUOTE)
			  inQuotes = !inQuotes;
			else if (!inQuotes)
			  c = Character.toLowerCase(c);
			buffer.append(c);
		}
		//System.out.println("OUTPUT: " + buffer.toString());
		return buffer.toString();
	}
	
	/**
	 * Supply your own error message text. By default, messages from RSEUIPlugin resource bundle are used.
	 * @param msg_Empty error message when entry field is empty or null if to keep the default
	 * @param msg_NonUnique error message when value entered is not unique or null if to keep the default
	 */
	public void setErrorMessages(SystemMessage msg_Empty, SystemMessage msg_NonUnique)
	{
		if (msg_Empty != null)
  		  this.msg_Empty     = msg_Empty;
  	    if (msg_NonUnique != null)
		  this.msg_NonUnique = msg_NonUnique;		
	}

	/**
	 * Override in child to do your own syntax checking.
	 */
	public SystemMessage isSyntaxOk(String newText)
	{
		return null;
	}

	public String toString()
	{
		return "UniqueNameValidator class"; //$NON-NLS-1$
	}

    // --------------------------
    // Internal helper methods...
    // --------------------------

    /**
     * Helper method to substitute data into a message
     */
	protected String doMessageSubstitution(SystemMessage msg, String substitution)
	{
        currentMessage = msg;
		if (msg.getNumSubstitutionVariables() > 0)
		{
			if (!useUpperCase)
		      return msg.makeSubstitution(substitution).getLevelOneText();
		    else
		      return msg.makeSubstitution(substitution.toUpperCase()).getLevelOneText();
		}
		else
		  return msg.getLevelOneText();
	}
	
	/**
	 * Helper method to set the current system message and return its level one text
	 */
	protected String getSystemMessageText(SystemMessage msg)
	{
		currentMessage = msg;
		if (msg != null)
		  return msg.getLevelOneText();
		else
		  return null;
	}

    // ---------------------------
    // ISystemValidator methods...
    // ---------------------------
    
	/**
	 * Validates the given string. Returns the error message
	 * used if the given string isn't valid. A return value
	 * <code>null</code> or a string of length zero indicates
	 * that the value is valid.
	 * Note this is called per keystroke, by the platform.
	 * @deprecated You should be using {@link #validate(String)} and SystemMessage objects
	 */
	public String isValid(String newText)
	{
        currentMessage = null;        
		newText = newText.trim();
		if (newText.length() == 0)
		  currentMessage = msg_Empty;
		else 
		{
		  if (!caseSensitive && (existingList!=null))
		  {
		    if (newText.indexOf(QUOTE)!=-1)
		      newText = quotedToLowerCase(newText);
		    else
		      newText = newText.toLowerCase();
		  }
		  /*
		      if (!caseSensitive && (existingList!=null) && (Arrays.binarySearch(existingList,newText) >= 0))
		    return msg_NonUnique.getLevelOneText();
	  	  else if (caseSensitive && (existingList!=null) && (Arrays.binarySearch(existingList,newText) >= 0))
		    return msg_NonUnique.getLevelOneText();		  
		  */
	  	  if ((existingList!=null) && (Arrays.binarySearch(existingList,newText) >= 0))
		    currentMessage = msg_NonUnique;
		  else if (syntaxValidator!=null) 
		  {
		    String msg = syntaxValidator.isValid(newText);
		    if (msg != null)
		    {
		    	currentMessage = syntaxValidator.getSystemMessage();
		    	if (currentMessage == null) // tsk, tsk
		    	  return msg; 
		    }
		  }
		  else 
		    currentMessage = isSyntaxOk(newText);
		}
        return (currentMessage == null) ? null : doMessageSubstitution(currentMessage, newText);
	}
		
	/**
	 * As required by ICellEditor
	 */
	public String isValid(Object newValue)
	{
		if (newValue instanceof String)
		  return isValid((String)newValue);
		else
		{
		  currentMessage = null;
		  return null;
		}
	}	

    /**
     * Return the max length for this name, or -1 if no max
     */
    public int getMaximumNameLength()
    {
    	return -1;
    }

    /**
     * When isValid returns non-null, call this to get the SystemMessage object for the error 
     *  versus the simple string message.
     */
    public SystemMessage getSystemMessage()
    {
    	return currentMessage;
    }
	
    /**
     * For convenience, this is a shortcut to calling:
     * <pre><code>
     *  if (isValid(text) != null)
     *    msg = getSystemMessage();
     * </code></pre>
     */
    public SystemMessage validate(String text)
    {
    	if (isValid(text) != null)
    	  return currentMessage;
    	else
    	  return null;
    }	
}
