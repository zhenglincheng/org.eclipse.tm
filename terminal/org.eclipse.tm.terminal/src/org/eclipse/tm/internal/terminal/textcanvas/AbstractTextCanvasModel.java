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

import org.eclipse.swt.graphics.Point;
import org.eclipse.tm.terminal.model.ITerminalTextDataReadOnly;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;

abstract public class AbstractTextCanvasModel implements ITextCanvasModel {
	protected List fListeners = new ArrayList();
	private int fCursorLine;
	private int fCursorColumn;
	private boolean fShowCursor;
	private long fCursorTime;
	private boolean fCursorIsEnabled;
	private int fStartLine;
	private int fEndLine;
	private int fStartCoumn;
	private int fEndColumn;
	private final ITerminalTextDataSnapshot fSnapshot;
	private ITerminalTextDataSnapshot fSelectionSnapshot;
	private int fLines;

	public AbstractTextCanvasModel(ITerminalTextDataSnapshot snapshot) {
		fSnapshot=snapshot;
		fLines=fSnapshot.getHeight();
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
	protected void fireDimensionsChanged( int width,int height) {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			ITextCanvasModelListener listener = (ITextCanvasModelListener) iter.next();
			listener.dimensionsChanged(width,height);
		}
		
	}
	protected void fireTerminalDataChanged() {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			ITextCanvasModelListener listener = (ITextCanvasModelListener) iter.next();
			listener.terminalDataChanged();
		}
		
	}
	public ITerminalTextDataReadOnly getTerminalText() {
		return fSnapshot;
	}
	protected ITerminalTextDataSnapshot getSnapshot() {
		return fSnapshot;
	}
	protected void updateSnapshot() {
		if(fSnapshot.isOutOfDate()) {
			fSnapshot.updateSnapshot(false);
			if(fSnapshot.hasTerminalChanged())
				fireTerminalDataChanged();
			// TODO why does hasDimensionsChanged not work??????
			//			if(fSnapshot.hasDimensionsChanged())
			//				fireDimensionsChanged();
			if(fLines!=fSnapshot.getHeight()) {
				fireDimensionsChanged(fSnapshot.getWidth(),fSnapshot.getHeight());
				fLines=fSnapshot.getHeight();
			}
			int y=fSnapshot.getFirstChangedLine();
			// has any line changed?
			if(y<Integer.MAX_VALUE) {
				int height=fSnapshot.getLastChangedLine()-y+1;
				fireCellRangeChanged(0, y, fSnapshot.getWidth(), height);
			}
		}
	}
	/**
	 * must be called from the UI thread
	 */
	public void update() {
		// do the poll....
		updateSnapshot();
		updateSelection();
		updateCursor();
	}


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
	public void setVisibleRectangle(int startLine, int startCol, int height, int width) {
		fSnapshot.setInterestWindow(Math.max(0,startLine), Math.max(1,Math.min(fSnapshot.getHeight(),height)));
		update();
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
	
	public Point getSelectionEnd() {
		if(fStartLine<0)
			return null;
		else
			return new Point(fEndColumn, fEndLine);
	}

	public Point getSelectionStart() {
		if (fStartLine < 0)
			return null;
		else
			return new Point(fStartCoumn,fStartLine);
	}

	public void setSelection(int startLine, int endLine, int startColumn, int endColumn) {
		assert(startLine<0 || startLine<=endLine);
		if(startLine>=0) {
			if(fSelectionSnapshot==null) {
				fSelectionSnapshot=fSnapshot.getTerminalTextData().makeSnapshot();
				fSelectionSnapshot.updateSnapshot(true);
			}
		} else if(fSelectionSnapshot!=null) {
			fSelectionSnapshot.detach();
			fSelectionSnapshot=null;
		}
		int oldStart=fStartLine;
		int oldEnd=fEndLine;
		fStartLine = startLine;
		fEndLine = endLine;
		fStartCoumn = startColumn;
		fEndColumn = endColumn;
		if(fSelectionSnapshot!=null) {
			fSelectionSnapshot.setInterestWindow(0, fSelectionSnapshot.getTerminalTextData().getHeight());
		}
		int changedStart;
		int changedEnd;
		if(oldStart<0) {
			changedStart=fStartLine;
			changedEnd=fEndLine;
		} else if(fStartLine<0) {
			changedStart=oldStart;
			changedEnd=oldEnd;
		} else {
			changedStart=Math.min(oldStart, fStartLine);
			changedEnd=Math.max(oldEnd, fEndLine);
		}
		if(changedStart>=0) {
			fireCellRangeChanged(0, changedStart, fSnapshot.getWidth(), changedEnd-changedStart+1);
		}
	}

	public boolean hasLineSelection(int line) {
		if (fStartLine < 0)
			return false;
		else
			return line >= fStartLine && line <= fEndLine;
	}
	
	public String getSelectedText() {
		if(fStartLine<0 || fSelectionSnapshot==null)
			return ""; //$NON-NLS-1$
		if(fStartLine<0 || fSelectionSnapshot==null)
			return ""; //$NON-NLS-1$
		StringBuffer buffer=new StringBuffer();
		for (int line = fStartLine; line <= fEndLine; line++) {
			String text;
			char[] chars=fSelectionSnapshot.getChars(line);
			if(chars!=null) {
				text=new String(chars);
				if(line==fEndLine)
					text=text.substring(0, Math.min(fEndColumn,text.length()));
				if(line==fStartLine)
					text=text.substring(Math.min(fStartCoumn,text.length()));
				// get rid of the empty space at the end of the lines
				text=text.replaceAll("\000+$","");  //$NON-NLS-1$//$NON-NLS-2$
				// null means space
				text=text.replace('\000', ' ');
			} else {
				text=""; //$NON-NLS-1$
			}
			buffer.append(text);
			if(line < fEndLine)
				buffer.append('\n');
		}
		return buffer.toString();
	}
	private void updateSelection() {
		if (fSelectionSnapshot != null && fSelectionSnapshot.isOutOfDate()) {
			// let's see if the selection text has changed since the last snapshot
			String oldSelection = getSelectedText();
			fSelectionSnapshot.updateSnapshot(true);
			// has the selection moved?
			if (fSelectionSnapshot != null && fStartLine >= 0 && fSelectionSnapshot.getScrollWindowSize() > 0) {
				int start = fStartLine + fSelectionSnapshot.getScrollWindowShift();
				int end = fEndLine + fSelectionSnapshot.getScrollWindowShift();
				if (start < 0)
					if (end >= 0)
						start = 0;
					else
						start = -1;
				setSelection(start, end, fStartCoumn, fEndColumn);
			}
			// have lines inside the selection changed?
			if (fSelectionSnapshot != null && fSelectionSnapshot.getFirstChangedLine() <= fEndLine &&
					fSelectionSnapshot.getLastChangedLine() >= fStartLine) {
				// has the selected text changed?
				String newSelection = getSelectedText();
				if (!oldSelection.equals(newSelection))
					setSelection(-1, -1, -1, -1);
			}
			// update the observed window...
			if (fSelectionSnapshot != null)
				// todo make -1 to work!
				fSelectionSnapshot.setInterestWindow(0, fSelectionSnapshot.getTerminalTextData().getHeight());
		}
	}

}