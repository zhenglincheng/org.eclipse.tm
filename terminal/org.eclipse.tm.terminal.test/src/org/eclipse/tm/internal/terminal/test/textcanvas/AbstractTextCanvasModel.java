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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract public class AbstractTextCanvasModel implements ITextCanvasModel {

	protected List fListeners = new ArrayList();

	public AbstractTextCanvasModel() {
		super();
	}

	public void addCellCanvasModelListener(ITextCanvasModelListener listener) {
		fListeners.add(listener);
	}

	public void removeCellCanvasModelListener(ITextCanvasModelListener listener) {
		fListeners.remove(listener);	
	}

	protected void fireCellRangeChanged(int x, int y, int width, int height) {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			ITextCanvasModelListener listener = (ITextCanvasModelListener) iter.next();
			listener.rangeChanged(x, y, width, height);
		}
		
	}
	protected void fireDimensionsChanges() {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			ITextCanvasModelListener listener = (ITextCanvasModelListener) iter.next();
			listener.dimesnionsChanged(getWidth(),getHeight());
		}
		
	}

	abstract public int getHeight();

	abstract public int getWidth();

}