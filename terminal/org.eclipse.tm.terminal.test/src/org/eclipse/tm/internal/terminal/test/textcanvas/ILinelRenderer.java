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

import org.eclipse.swt.graphics.GC;

/**
 * @author Michael.Scharf@scharf-software.com
 *
 */
public interface ILinelRenderer {
	int getCellWidth();
	int getCellHeight();
	void drawLine(ITextCanvasModel model, GC gc, int line, int x, int y, int colFirst, int colLast);
	/**
	 * This is is 
	 * @param startLine
	 * @param startCol
	 * @param height
	 * @param width
	 */
	void setVisibleRectangle(int startLine, int startCol, int height, int width);
}
