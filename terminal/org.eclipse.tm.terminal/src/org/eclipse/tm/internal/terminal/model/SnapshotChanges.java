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
	private int fScrollWindowStartLine;
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
	private int fInterestWindowStartLine;

	
	public SnapshotChanges(int nLines) {
		fChangedLines=new boolean[nLines];
		fFirstChangedLine=nLines;
		fLastChangedLine=-1;
	}
	boolean isInInterestWindow(int line, int size) {
		if(fInterestWindowSize<=0)
			return true;
		if(line+size<=fInterestWindowStartLine || line>=fInterestWindowStartLine+fInterestWindowSize)
			return false;
		return true;
	}
	boolean isInInterestWindow(int line) {
		if(fInterestWindowSize<=0)
			return true;
		if(line<fInterestWindowStartLine || line>=fInterestWindowStartLine+fInterestWindowSize)
			return false;
		return true;
	}
	/**
	 * @param line
	 * @return the line within the window
	 */
	int fitLineToWindow(int line) {
		if(fInterestWindowSize<=0)
			return line;
		if(line<fInterestWindowStartLine)
			return fInterestWindowStartLine;
		return line;
	}
	int fitSizeToWindow(int line, int size) {
		if(fInterestWindowSize<=0)
			return size;
		if(line<fInterestWindowStartLine) {
			size-=fInterestWindowStartLine-line;
		}
		if(line+size>fInterestWindowStartLine+fInterestWindowSize)
			size=fInterestWindowStartLine+fInterestWindowSize-line;
		return size;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#markLineChanged(int)
	 */
	public void markLineChanged(int line) {
		if(!isInInterestWindow(line))
			return;
		line=fitLineToWindow(line);
		if(line<fFirstChangedLine)
			fFirstChangedLine=line;
		if(line>fLastChangedLine)
			fLastChangedLine=line;
		// in case the terminal got resized we expand 
		// don't remember the changed line because
		// there is nothing to copy
		if(line<fChangedLines.length) {
			fChangedLines[line]=true;
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#markLinesChanged(int, int)
	 */
	public void markLinesChanged(int line, int n) {
		if(n<=0 || !isInInterestWindow(line,n))
			return;
		// do not exceed the bounds of fChangedLines
		// the terminal might have been resized and 
		// we can only keep changes for the size of the
		// previous terminal
		line=fitLineToWindow(line);
		n=fitSizeToWindow(line, n);
		int m=Math.min(line+n-1, fChangedLines.length-1);
		for (int i = line+1; i < m; i++) {
			fChangedLines[i]=true;
		}
		// this sets fFirstChangedLine as well
		markLineChanged(line);
		// this sets fLastChangedLine as well
		markLineChanged(line+n-1);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#convertScrollingIntoChanges()
	 */
	public void convertScrollingIntoChanges() {
		markLinesChanged(fScrollWindowStartLine,fScrollWindowSize);
		fScrollWindowStartLine=0;
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
	public void scroll(int startLine, int size, int shift) {
		// let's track only negative shifts
		if(fScrollDontTrack) {
			// we are in a state where we cannot track scrolling
			// so let's simply mark the scrolled lines as changed
			markLinesChanged(startLine, size);
		} else if(shift>=0) {
			// we cannot handle positive scroll
			// forget about clever caching of scroll events
			doNotTrackScrollingAnymore();
			// mark all lines inside the scroll region as changed
			markLinesChanged(startLine, size);
		} else {
			// we have already scrolled
			if(fScrollWindowShift<0) {
				// we have already scrolled
				if(fScrollWindowStartLine==startLine && fScrollWindowSize==size) {
					// we are scrolling the same region again?
					fScrollWindowShift+=shift;
					scrollChangesLinesWithNegativeShift(startLine,size,shift);
				} else {
					// mark all lines in the old scroll region as changed
					doNotTrackScrollingAnymore();
					// mark all lines changed, because
					markLinesChanged(startLine, size);
				}
			} else {
				// first scroll in this change -- we just notify it
				fScrollWindowStartLine=startLine;
				fScrollWindowSize=size;
				fScrollWindowShift=shift;
				scrollChangesLinesWithNegativeShift(startLine,size,shift);
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
			markLinesChanged(fScrollWindowStartLine, fScrollWindowSize);
			fScrollWindowStartLine=0;
			fScrollWindowSize=0;
			fScrollWindowShift=0;
		}
		// don't be clever on scrolling anymore
		fScrollDontTrack=true;
	}
	/**
	 * Scrolls the changed lines data
	 *
	 * @param line
	 * @param n
	 * @param shift must be negative!
	 */
	private void scrollChangesLinesWithNegativeShift(int line, int n, int shift) {
		// assert shift <0;
		// scroll the region
		
		// don't run out of bounds!
		int m=Math.min(line+n+shift, fChangedLines.length+shift);
		for (int i = line; i < m; i++) {
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
		for (int i = Math.max(0,line+n+shift); i < line+n; i++) {
			markLineChanged(i);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#setAllChanged(int)
	 */
	public void setAllChanged(int height) {
		fScrollWindowStartLine=0;
		fScrollWindowSize=0;
		fScrollWindowShift=0;
		fFirstChangedLine=fitLineToWindow(0);
		fLastChangedLine=fitSizeToWindow(0, height)-1;
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
	 * @see org.eclipse.tm.internal.terminal.model.ISnapshotChanges#getScrollWindowStartLine()
	 */
	public int getScrollWindowStartLine() {
		return fScrollWindowStartLine;
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
	public boolean hasLineChanged(int line) {
		if(!isInInterestWindow(line))
			return false;
		if(line<fChangedLines.length)
			return fChangedLines[line];
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


	public int getInterestWindowStartLine() {
		return fInterestWindowStartLine;
	}

	public void setInterestWindow(int startLine, int size) {
		fInterestWindowStartLine=startLine;
		fInterestWindowSize=size;
	}

}