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
package org.eclipse.tm.internal.terminal.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.tm.terminal.model.ITerminalTextData;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.LineSegment;
import org.eclipse.tm.terminal.model.Style;

/**
 * The public methods of this class have to be called from one thread! 
 *
 * Threading considerations:
 * This class is <b>not threadsafe</b>!
 */
class TerminalTextDataSnapshot implements ITerminalTextDataSnapshot {
	/**
	 * The changes of the current snapshot relative to the
	 * previous snapshot
	 */
	volatile ISnapshotChanges fCurrentChanges;
	/**
	 * Keeps track of changes that happened since the current
	 * snapshot has been made.
	 */
	ISnapshotChanges fFutureChanges;
	/**
	 * Is used as lock and is the reference to the terminal we take snapshots from.
	 */
	final TerminalTextData fTerminal;
	/**
	 * A snapshot copy of of fTerminal
	 */
	// snapshot does not need internal synchronisation
	final ITerminalTextData fSnapshot;
	// this variable is synchronized on fTerminal!
	private SnapshotOutOfDateListener[] fListener=new SnapshotOutOfDateListener[0];
	// this variable is synchronized on fTerminal!
	private boolean fListenersNeedNotify;
	private int fInterestWindowSize;
	private int fInterestWindowStartRow;

	TerminalTextDataSnapshot(TerminalTextData terminal) {
		fSnapshot = new TerminalTextDataStore();
		fTerminal = terminal;
		fCurrentChanges = new SnapshotChanges(fTerminal.getHeight());
		fFutureChanges = new SnapshotChanges(fTerminal.getHeight());
		fListenersNeedNotify=true;

	}
	
	public void detach() {
		fTerminal.removeSnapshot(this);
	}

	public boolean hasChanged() {
		// this is called from fTerminal, therefore we lock on fTerminal
		synchronized (fTerminal) {
			return fFutureChanges.hasChanged();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#snapshot()
	 */
	public void updateSnapshot(boolean detectScrolling) {
		// make sure terminal does not change while we make the snapshot
		synchronized (fTerminal) {
			// let's make the future changes current
			fCurrentChanges=fFutureChanges;
			fFutureChanges=new SnapshotChanges(fTerminal.getHeight());
			// and update the snapshot
			if(fSnapshot.getHeight()!=fTerminal.getHeight()||fSnapshot.getWidth()!=fTerminal.getWidth()) {
				// if the dimensions have changed, we need a full copy
				fSnapshot.copy(fTerminal);
				// and we mark all lines as changed
				fCurrentChanges.setAllChanged(fTerminal.getHeight());
			} else {
				// first we do the scroll on the copy
				fSnapshot.scroll(fCurrentChanges.getScrollWindowStartRow(), fCurrentChanges.getScrollWindowSize(), fCurrentChanges.getScrollWindowShift());
				// and then create the snapshot of the changed lines
				fCurrentChanges.copyChangedLines(fSnapshot, fTerminal);
			}
			fListenersNeedNotify=true;
		}
		if(!detectScrolling) {
			// let's pretend there was no scrolling and
			// convert the scrolling into line changes
			fCurrentChanges.convertScrollingIntoChanges();
		}
	}

	public char getChar(int x, int y) {
		return fSnapshot.getChar(x, y);
	}

	public int getHeight() {
		return fSnapshot.getHeight();
	}

	public LineSegment[] getLineSegments(int x, int y, int len) {
		return fSnapshot.getLineSegments(x, y, len);
	}

	public Style getStyle(int x, int y) {
		return fSnapshot.getStyle(x, y);
	}

	public int getWidth() {
		return fSnapshot.getWidth();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#getFirstChangedLine()
	 */
	public int getFirstChangedLine() {
		return fCurrentChanges.getFirstChangedLine();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#getLastChangedLine()
	 */
	public int getLastChangedLine() {
		return fCurrentChanges.getLastChangedLine();
	}

	public boolean hasLineChanged(int y) {
		return fCurrentChanges.hasLineChanged(y);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#getScrollChangeY()
	 */
	public int getScrollWindowStartRow() {
		return fCurrentChanges.getScrollWindowStartRow();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#getScrollChangeN()
	 */
	public int getScrollWindowSize() {
		return fCurrentChanges.getScrollWindowSize();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#getScrollChangeShift()
	 */
	public int getScrollWindowShift() {
		return fCurrentChanges.getScrollWindowShift();
	}
	
	/**
	 * Announces a change in line y
	 * @param y
	 */
	void markLineChanged(int y) {
		// threading
		fFutureChanges.markLineChanged(y);
		notifyListers();
	}
	/**
	 * Announces a change of n lines beginning with line y
	 * @param y
	 * @param n
	 */
	void markLinesChanged(int y,int n) {
		fFutureChanges.markLinesChanged(y,n);
		notifyListers();
	}
	/**
	 * @param startRow
	 * @param size
	 * @param shift
	 */
	void scroll(int startRow, int size, int shift) {
		fFutureChanges.scroll(startRow,size,shift);
		notifyListers();
	}
	/**
	 * Notifies listeners about the change
	 */
	private void notifyListers() {
		// this code has to be called from a block synchronized on fTerminal
		synchronized (fTerminal) {
			if(fListenersNeedNotify) {
				for (int i = 0; i < fListener.length; i++) {
					fListener[i].snapshotOutOfDate(this);
				}
				fListenersNeedNotify=false;
			}
		}
	}
	public ITerminalTextDataSnapshot makeSnapshot() {
		return fSnapshot.makeSnapshot();
	}

	synchronized public void addListener(SnapshotOutOfDateListener listener) {
		List list=new ArrayList();
		list.addAll(Arrays.asList(fListener));
		list.add(listener);
		fListener=(SnapshotOutOfDateListener[]) list.toArray(new SnapshotOutOfDateListener[list.size()]);
	}

	synchronized public void removeListener(SnapshotOutOfDateListener listener) {
		List list=new ArrayList();
		list.addAll(Arrays.asList(fListener));
		list.remove(listener);
		fListener=(SnapshotOutOfDateListener[]) list.toArray(new SnapshotOutOfDateListener[list.size()]);
	}
	public String toString() {
		return fSnapshot.toString();
	}


	public int getInterestWindowSize() {
		return fInterestWindowSize;
	}


	public int getInterestWindowStartRow() {
		return fInterestWindowStartRow;
	}

	public void setInterestWindow(int startRow, int size) {
		fInterestWindowStartRow=startRow;
		fInterestWindowSize=size;
		notifyListers();
	}


	public char[] getChars(int line) {
		return fSnapshot.getChars(line);
	}


	public Style[] getStyles(int line) {
		return fSnapshot.getStyles(line);
	}
}


