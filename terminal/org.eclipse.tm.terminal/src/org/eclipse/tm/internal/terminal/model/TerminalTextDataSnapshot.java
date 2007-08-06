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
 * This class is <b>not</b> threadsafe!
 */
class TerminalTextDataSnapshot implements ITerminalTextDataSnapshot {
	/**
	 * Collects the changes of the {@link ITerminalTextData}
	 *
	 */
	static class Change {
		/**
		 * The first line changed
		 */
		int fFirstChangedLine;
		/**
		 * The last line changed
		 */
		int fLastChangedLine;
		int fScrollY;
		int fScrollN;
		int fScrollShift;
		/**
		 * true, if scrolling should not tracked anymore
		 */
		boolean fScrollDontTrack;
		/**
		 * The lines that need to be copied
		 * into the snapshot (lines that have
		 * not changed don't have to be copied)
		 */
		boolean[] fChangedLines;
		
		public Change(int nLines) {
			fChangedLines=new boolean[nLines];
			fFirstChangedLine=nLines;
			fLastChangedLine=-1;
		}
		/**
		 * @param y might bigger than the number of lines....
		 */
		public void markLineChanged(int y) {
			if(y<fFirstChangedLine)
				fFirstChangedLine=y;
			if(y>fLastChangedLine)
				fLastChangedLine=y;
			// in case the terminal got resized we expand 
			// don't remember the changed line because
			// there is nothing to copy
			if(y<fChangedLines.length) {
				fChangedLines[y]=true;
			}
		}
		/**
		 * Marks all lines in the range as changed
		 * @param y >=0
		 * @param n might be out of range
		 */
		void markLinesChanged(int y, int n) {
			if(n>0) {
				// do not exceed the bounds of fChangedLines
				// the terminal might have been resized and 
				// we can only keep changes for the size of the
				// previous terminal
				int m=Math.min(n+y-1, fChangedLines.length-1);
				for (int i = y+1; i < m; i++) {
					fChangedLines[i]=true;
				}
				// this sets fFirstChangedLine as well
				markLineChanged(y);
				// this sets fLastChangedLine as well
				markLineChanged(y+n-1);
			}
		}
		/**
		 * Marks all lines within the scrolling region
		 * changed and resets the scrolling information
		 */
		void convertScrollingIntoChanges() {
			markLinesChanged(fScrollY,fScrollN);
			fScrollY=0;
			fScrollN=0;
			fScrollShift=0;
		}
		/**
		 * @return true if something has changed
		 */
		public boolean hasChanged() {
			if(fFirstChangedLine!=fChangedLines.length || fLastChangedLine>0 || fScrollShift!=0)
				return true;
			return false;
		}
		/**
		 * @param y
		 * @param n
		 * @param shift
		 */
		public void scroll(int y, int n, int shift) {
			// let's track only negative shifts
			if(fScrollDontTrack) {
				// we are in a state where we cannot track scrolling
				// so let's simply mark the scrolled lines as changed
				markLinesChanged(y, n);
			} else if(shift>=0) {
				// we cannot handle positive scroll
				// forget about clever caching of scroll events
				doNotTrackScrollingAnymore();
				// mark all lines inside the scroll region as changed
				markLinesChanged(y, n);
			} else {
				// we have already scrolled
				if(fScrollShift<0) {
					// we have already scrolled
					if(fScrollY==y && fScrollN==n) {
						// we are scrolling the same region again?
						fScrollShift+=shift;
						scrollChangesLinesWithNegativeShift(y,n,shift);
					} else {
						// mark all lines in the old scroll region as changed
						doNotTrackScrollingAnymore();
						// mark all lines changed, because
						markLinesChanged(y, n);
					}
				} else {
					// first scroll in this change -- we just notify it
					fScrollY=y;
					fScrollN=n;
					fScrollShift=shift;
					scrollChangesLinesWithNegativeShift(y,n,shift);
				}
			}
		}
		/**
		 * Some incompatible scrolling occurred. We cannot do the
		 * scroll optimization anymore...
		 */
		private void doNotTrackScrollingAnymore() {
			if(fScrollN>0) {
				// convert the current scrolling into changes
				markLinesChanged(fScrollY, fScrollN);
				fScrollY=0;
				fScrollN=0;
				fScrollShift=0;
			}
			// don't be clever on scrolling anymore
			fScrollDontTrack=true;
		}
		/**
		 * Scrolls the changed lines data
		 *
		 * @param y
		 * @param n
		 * @param shift must be negative!
		 */
		private void scrollChangesLinesWithNegativeShift(int y, int n, int shift) {
			// assert shift <0;
			// scroll the region
			
			// don't run out of bounds!
			int m=Math.min(y+n+shift, fChangedLines.length+shift);
			for (int i = y; i < m; i++) {
				fChangedLines[i]=fChangedLines[i-shift];
				// move the first changed line up.
				// We don't have to move the maximum down,
				// because with a shift scroll, the max is moved
				// my the next loop in this method
				if(i<fFirstChangedLine && fChangedLines[i]) {
					fFirstChangedLine=i;
				}
			}
			// mark the "opened" lines as changed
			for (int i = Math.max(0,y+n+shift); i < y+n; i++) {
				markLineChanged(i);
			}
		}
		/**
		 * Mark all lines changed
		 */
		public void setAllChanged(int height) {
			fScrollY=0;
			fScrollN=0;
			fScrollShift=0;
			fFirstChangedLine=0;
			fLastChangedLine=height-1;
			// no need to keep an array of changes anymore
			fChangedLines=new boolean[0];
		}
	}
	/**
	 * The changes of the current snapshot relative to the
	 * previous snapshot
	 */
	volatile Change fCurrentChanges;
	/**
	 * Keeps track of changes that happened since the current
	 * snapshot has been made.
	 */
	Change fFutureChanges;
	/**
	 * Is used as lock and is the reference to the terminal we take snapshots from.
	 */
	final TerminalTextData fTerminal;
	/**
	 * A snapshot copy of of fTerminal
	 */
	// snapshot does not need internal synchronisation
	final TerminalTextData fSnapshot;
	// this variable is synchronized on fTerminal!
	private SnapshotNeedUpdateListener[] fListener=new SnapshotNeedUpdateListener[0];
	// this variable is synchronized on fTerminal!
	private boolean fListenersNeedNotify;

	TerminalTextDataSnapshot(TerminalTextData terminal) {
		fSnapshot = new TerminalTextData();
		fTerminal = terminal;
		fCurrentChanges = new Change(fTerminal.getHeight());
		fFutureChanges = new Change(fTerminal.getHeight());
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
			fFutureChanges=new Change(fTerminal.getHeight());
			// and update the snapshot
			if(fSnapshot.getHeight()!=fTerminal.getHeight()||fSnapshot.getWidth()!=fTerminal.getWidth()) {
				// if the dimensions have changed, we need a full copy
				fTerminal.copyInto(fSnapshot);
				// and we mark all lines as changed
				fCurrentChanges.setAllChanged(fTerminal.getHeight());
			} else {
				// first we do the scroll on the copy
				fSnapshot.scroll(fCurrentChanges.fScrollY, fCurrentChanges.fScrollN, fCurrentChanges.fScrollShift);
				// and then create the snapshot of the changed lines
				fTerminal.copyInto(fSnapshot,fCurrentChanges.fChangedLines);
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
		return fCurrentChanges.fFirstChangedLine;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#getLastChangedLine()
	 */
	public int getLastChangedLine() {
		return fCurrentChanges.fLastChangedLine;
	}

	public boolean hasLineChanged(int y) {
		if(y<fCurrentChanges.fChangedLines.length)
			return fCurrentChanges.fChangedLines[y];
		// since the height of the terminal could
		// have changed but we have tracked only changes
		// of the previous terminal height, any line outside
		// the the range of the previous height has changed
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#getScrollChangeY()
	 */
	public int getScrollChangeY() {
		return fCurrentChanges.fScrollY;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#getScrollChangeN()
	 */
	public int getScrollChangeN() {
		return fCurrentChanges.fScrollN;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ITerminalTextDataSnapshot#getScrollChangeShift()
	 */
	public int getScrollChangeShift() {
		return fCurrentChanges.fScrollShift;
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
	 * @param y
	 * @param n
	 * @param shift
	 */
	void scroll(int y, int n, int shift) {
		fFutureChanges.scroll(y,n,shift);
		notifyListers();
	}
	/**
	 * Notifies listeners about the change
	 */
	private void notifyListers() {
		// this code has to be called from a block synchronized on fTerminal
		if(fListenersNeedNotify) {
			for (int i = 0; i < fListener.length; i++) {
				fListener[i].changed();
			}
			fListenersNeedNotify=false;
		}
	}
	public ITerminalTextDataSnapshot makeSnapshot() {
		return fSnapshot.makeSnapshot();
	}

	synchronized public void addListener(SnapshotNeedUpdateListener listener) {
		List list=new ArrayList();
		list.addAll(Arrays.asList(fListener));
		list.add(listener);
		fListener=(SnapshotNeedUpdateListener[]) list.toArray(new SnapshotNeedUpdateListener[list.size()]);
	}

	synchronized public void removeListener(SnapshotNeedUpdateListener listener) {
		List list=new ArrayList();
		list.addAll(Arrays.asList(fListener));
		list.remove(listener);
		fListener=(SnapshotNeedUpdateListener[]) list.toArray(new SnapshotNeedUpdateListener[list.size()]);
	}
	public String textToString() {
		return fSnapshot.textToString();
	}

	public String toString() {
		return fSnapshot.textToString();
	}
}


