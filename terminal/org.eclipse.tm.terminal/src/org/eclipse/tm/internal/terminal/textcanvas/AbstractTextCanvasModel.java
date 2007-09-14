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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;

abstract public class AbstractTextCanvasModel implements ITextCanvasModel {

	protected List fListeners = new ArrayList();
	private int fCursorLine;
	private int fCursorColumn;
	private boolean fShowCursor;
	private long fCursorTime;
	private boolean fCursorIsEnabled;

	public AbstractTextCanvasModel() {
		super();
	}
	protected abstract ITerminalTextDataSnapshot getSnapshot();
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
	protected void fireDimensionsChanged() {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			ITextCanvasModelListener listener = (ITextCanvasModelListener) iter.next();
			listener.dimensionsChanged(getWidth(),getHeight());
		}
		
	}
	protected void fireTerminalDataChanged() {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			ITextCanvasModelListener listener = (ITextCanvasModelListener) iter.next();
			listener.terminalDataChanged();
		}
		
	}

	abstract public int getHeight();

	abstract public int getWidth();

	abstract public void update();


	public int getCursorColumn() {
		return fCursorColumn;
	}

	public int getCursorLine() {
		return fCursorLine;
	}

	public boolean isCursorOn() {
		return fShowCursor && fCursorIsEnabled;
	}
	/**
	 * should be called regularly to draw an update of the
	 * blinking cursor
	 */
	protected void updateCursor() {
		if(!fCursorIsEnabled)
			return;
		int cursorLine=getSnapshot().getCursorLine();
		int cursorColumn=getSnapshot().getCursorColumn();
		// if cursor at the end put it to the end of the
		// last line...
		if(cursorLine>=getSnapshot().getHeight()) {
			cursorLine=getSnapshot().getHeight()-1;
			cursorColumn=getSnapshot().getWidth()-1;
		}
		// has the cursor moved?
		if(fCursorLine!=cursorLine || fCursorColumn!=cursorColumn) {
			// hide the old cursor!
			fShowCursor=false;
			// clean the previous cursor
			fireCellRangeChanged(fCursorColumn, fCursorLine, 1, 1);
			// the cursor is shown when it moves!
			fShowCursor=true;
			fCursorTime=System.currentTimeMillis();
			fCursorLine=cursorLine;
			fCursorColumn=cursorColumn;
			// and draw the new cursor
			fireCellRangeChanged(fCursorColumn, fCursorLine, 1, 1);
		} else {
			long t=System.currentTimeMillis();
			// TODO make the cursor blink time customisable
			if(t-fCursorTime>500) {
				fShowCursor=!fShowCursor;
				fCursorTime=t;
				fireCellRangeChanged(fCursorColumn, fCursorLine, 1, 1);
			}
		}
	}
	protected void showCursor(boolean show) {
		fShowCursor=true;
	}
	public void setCursorEnabled(boolean visible) {
		fCursorTime=System.currentTimeMillis();
		fShowCursor=visible;
		fCursorIsEnabled=visible;
		fireCellRangeChanged(fCursorColumn, fCursorLine, 1, 1);
	}
	public boolean isCursorEnabled() {
		return fCursorIsEnabled;
	}
}