package org.eclipse.tm.internal.terminal.model;

import org.eclipse.tm.terminal.model.ITerminalTextData;


/**
 * Collects the changes of the {@link ITerminalTextData}
 *
 */
public class SnapshotChanges implements ISnapshotChanges {
	/**
	 * The first line changed
	 */
	private int fFirstChangedLine;
	/**
	 * The last line changed
	 */
	private int fLastChangedLine;
	private int fScrollWindowStartRow;
	private int fScrollWindowSize;
	private int fScrollWindowShift;
	/**
	 * true, if scrolling should not tracked anymore
	 */
	private boolean fScrollDontTrack;
	/**
	 * The lines that need to be copied
	 * into the snapshot (lines that have
	 * not changed don't have to be copied)
	 */
	private boolean[] fChangedLines;
	
	private int fInterestWindowSize;
	private int fInterestWindowStartRow;

	
	public SnapshotChanges(int nLines) {
		fChangedLines=new boolean[nLines];
		fFirstChangedLine=nLines;
		fLastChangedLine=-1;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#markLineChanged(int)
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
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#markLinesChanged(int, int)
	 */
	public void markLinesChanged(int y, int n) {
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
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#convertScrollingIntoChanges()
	 */
	public void convertScrollingIntoChanges() {
		markLinesChanged(fScrollWindowStartRow,fScrollWindowSize);
		fScrollWindowStartRow=0;
		fScrollWindowSize=0;
		fScrollWindowShift=0;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#hasChanged()
	 */
	public boolean hasChanged() {
		if(fFirstChangedLine!=fChangedLines.length || fLastChangedLine>0 || fScrollWindowShift!=0)
			return true;
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#scroll(int, int, int)
	 */
	public void scroll(int startRow, int size, int shift) {
		// let's track only negative shifts
		if(fScrollDontTrack) {
			// we are in a state where we cannot track scrolling
			// so let's simply mark the scrolled lines as changed
			markLinesChanged(startRow, size);
		} else if(shift>=0) {
			// we cannot handle positive scroll
			// forget about clever caching of scroll events
			doNotTrackScrollingAnymore();
			// mark all lines inside the scroll region as changed
			markLinesChanged(startRow, size);
		} else {
			// we have already scrolled
			if(fScrollWindowShift<0) {
				// we have already scrolled
				if(fScrollWindowStartRow==startRow && fScrollWindowSize==size) {
					// we are scrolling the same region again?
					fScrollWindowShift+=shift;
					scrollChangesLinesWithNegativeShift(startRow,size,shift);
				} else {
					// mark all lines in the old scroll region as changed
					doNotTrackScrollingAnymore();
					// mark all lines changed, because
					markLinesChanged(startRow, size);
				}
			} else {
				// first scroll in this change -- we just notify it
				fScrollWindowStartRow=startRow;
				fScrollWindowSize=size;
				fScrollWindowShift=shift;
				scrollChangesLinesWithNegativeShift(startRow,size,shift);
			}
		}
	}
	/**
	 * Some incompatible scrolling occurred. We cannot do the
	 * scroll optimization anymore...
	 */
	private void doNotTrackScrollingAnymore() {
		if(fScrollWindowSize>0) {
			// convert the current scrolling into changes
			markLinesChanged(fScrollWindowStartRow, fScrollWindowSize);
			fScrollWindowStartRow=0;
			fScrollWindowSize=0;
			fScrollWindowShift=0;
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
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#setAllChanged(int)
	 */
	public void setAllChanged(int height) {
		fScrollWindowStartRow=0;
		fScrollWindowSize=0;
		fScrollWindowShift=0;
		fFirstChangedLine=0;
		fLastChangedLine=height-1;
		// no need to keep an array of changes anymore
		fChangedLines=new boolean[0];
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#getFirstChangedLine()
	 */
	public int getFirstChangedLine() {
		return fFirstChangedLine;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#getLastChangedLine()
	 */
	public int getLastChangedLine() {
		return fLastChangedLine;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#getScrollWindowStartRow()
	 */
	public int getScrollWindowStartRow() {
		return fScrollWindowStartRow;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#getScrollWindowSize()
	 */
	public int getScrollWindowSize() {
		return fScrollWindowSize;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#getScrollWindowShift()
	 */
	public int getScrollWindowShift() {
		return fScrollWindowShift;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#hasLineChanged(int)
	 */
	public boolean hasLineChanged(int y) {
		if(y<fChangedLines.length)
			return fChangedLines[y];
		// since the height of the terminal could
		// have changed but we have tracked only changes
		// of the previous terminal height, any line outside
		// the the range of the previous height has changed
		return true;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#copyChangedLines(org.eclipse.tm.terminal.model.ITerminalTextData, org.eclipse.tm.terminal.model.ITerminalTextData)
	 */
	public void copyChangedLines(ITerminalTextData dest, ITerminalTextData source) {
		dest.copySelective(source,0,0,fChangedLines);
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
	}

}