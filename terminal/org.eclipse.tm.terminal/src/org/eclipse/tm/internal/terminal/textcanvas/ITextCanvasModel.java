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
package org.eclipse.tm.internal.terminal.textcanvas;

public interface ITextCanvasModel {
	void addCellCanvasModelListener(ITextCanvasModelListener listener);
	void removeCellCanvasModelListener(ITextCanvasModelListener listener);
	/**
	 * @return the number of columns the terminal has
	 */
	int getWidth();
	/**
	 * @return the number of lines the terminal has
	 */
	int getHeight();
	/**
	 * must be called from the UI thread
	 */
	void update();
	
	/**
	 * @return true when the cursor is shown (used for blinking cursors)
	 */
	boolean isCursorOn();
	/**
	 * Show/Hide the cursor.
	 * @param visible
	 */
	void setCursorEnabled(boolean visible);
	
	/**
	 * @return true if the cursor is shown.
	 */
	boolean isCursorEnabled();
	
	/**
	 * @return the line of the cursor 
	 */
	int getCursorLine();
	/**
	 * @return the column of the cursor
	 */
	int getCursorColumn();
}