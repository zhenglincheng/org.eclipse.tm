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
package org.eclipse.tm.internal.terminal.test.textcanvas;

public interface ITextCanvasModel {
	void addCellCanvasModelListener(ITextCanvasModelListener listener);
	void removeCellCanvasModelListener(ITextCanvasModelListener listener);
	int getWidth();
	int getHeight();
	/**
	 * must be called from the UI thread
	 */
	void update();
	
	boolean isCursorOn();
	int getCursorLine();
	int getCursorColumn();
}