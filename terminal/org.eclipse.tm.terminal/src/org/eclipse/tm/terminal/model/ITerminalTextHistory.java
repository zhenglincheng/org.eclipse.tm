/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Michael Scharf (Wind River) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tm.terminal.model;


/**
 * This interface
 *
 * <b>Note:</b> This interface is intended to be implemented by clients.
 */
public interface ITerminalTextHistory {
	/**
	 * @param chars
	 * @param styles
	 */
	void addToHistory(char[] chars,Style[] styles);
}
