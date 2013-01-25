/********************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and International Business Machines Corporation. All rights reserved.
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
 * Martin Oberhuber (Wind River) - Moved from org.eclipse.rse.services.shells
 ********************************************************************************/

package org.eclipse.rse.internal.services.shells;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.rse.services.shells.ParsedOutput;

public class CommandPattern
{


	private Pattern _pattern;
	private ArrayList _outputPatterns;

	public CommandPattern(Pattern theCommandPattern)
	{
		_pattern = theCommandPattern;
		_outputPatterns = new ArrayList();
	}

	public void addOutputPattern(OutputPattern op)
	{
		_outputPatterns.add(op);
	}

	public boolean matchCommand(String theLine)
	{
		return _pattern.matcher(theLine).matches();
	}

	public ParsedOutput matchLine(String theLine)
	{
		int patterns = _outputPatterns.size();
		ParsedOutput matchedOutput;
		OutputPattern curPattern;
		for (int i = 0; i < patterns; i++)
		{
			curPattern = (OutputPattern) _outputPatterns.get(i);
			matchedOutput = curPattern.matchLine(theLine);
			if (matchedOutput != null)
				return matchedOutput;
		}
		return null;
	}

}