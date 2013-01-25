/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others.
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

package org.eclipse.rse.internal.dstore.universal.miners.command.patterns;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OutputPattern
{


	private Pattern _pattern;
	private String _objType;
	private ArrayList _matchOrder;

	public OutputPattern(String objType, String matchOrder, Pattern thePattern)
	{
		_objType = objType;
		_pattern = thePattern;

		_matchOrder = new ArrayList();
		//Here we add a dummy first element to the ArrayList, to mimick how the PatternMatcher stores it's 
		//matches (starting with group 1).
		_matchOrder.add(null);

		int index = 0;
		int nextSpace = 0;
		//Walk the matchOrder string parsing out words and adding them to _matchOrder...Could use StringTokenizer
		//but this seem much simpler.
		while ((nextSpace = matchOrder.indexOf(" ", index)) > 0) //$NON-NLS-1$
		{
			_matchOrder.add(matchOrder.substring(index, nextSpace).toLowerCase());
			index = nextSpace;
			while ((index < matchOrder.length()) && (matchOrder.charAt(index) == ' '))
				index++;
		}
		_matchOrder.add(matchOrder.substring(index, matchOrder.length()).toLowerCase());

	}

	public ParsedOutput matchLine(String theLine)
	{
		Matcher matcher = null;
		try
		{
			matcher = _pattern.matcher(theLine);
			if (!matcher.matches())
				return null;
		}
		catch (StringIndexOutOfBoundsException e)
		{
			e.printStackTrace();
			//Getting an exception here, when theLine is an empty line for some patterns..should probably investigate, 
			//but for now we'll just handle it...
			return null;
		}

		String fileString = ""; //$NON-NLS-1$
		String lineString = ""; //$NON-NLS-1$

		//Groups start at 1 (group 0 is the entire match).
		for (int i = 1; i < _matchOrder.size(); i++)
		{
			String mStr = (String)_matchOrder.get(i);
			if (mStr.equals("file")) //$NON-NLS-1$
			{
				fileString = matcher.group(i);
			}
			else if (mStr.equals("line")) //$NON-NLS-1$
			{
				lineString = matcher.group(i);
			}
		}
		int line = 1;
		if (lineString.length() > 0)
		{
			try
			{
				line = Integer.parseInt(lineString);
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
		}

		return new ParsedOutput(_objType, theLine, fileString, line, 1);
	}

}
