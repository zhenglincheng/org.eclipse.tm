package org.eclipse.tm.internal.terminal.model;

import org.eclipse.tm.terminal.model.ITerminalTextData;

public interface ISnapshotChanges {

	/**
	 * @param y might bigger than the number of lines....
	 */
	void markLineChanged(int y);

	/**
	 * Marks all lines in the range as changed
	 * @param y >=0
	 * @param n might be out of range
	 */
	void markLinesChanged(int y, int n);

	/**
	 * Marks all lines within the scrolling region
	 * changed and resets the scrolling information
	 */
	void convertScrollingIntoChanges();

	/**
	 * @return true if something has changed
	 */
	boolean hasChanged();

	/**
	 * @param startRow
	 * @param size
	 * @param shift
	 */
	void scroll(int startRow, int size, int shift);

	/**
	 * Mark all lines changed
	 */
	void setAllChanged(int height);

	int getFirstChangedLine();

	int getLastChangedLine();

	int getScrollWindowStartRow();

	int getScrollWindowSize();

	int getScrollWindowShift();

	boolean hasLineChanged(int y);

	void copyChangedLines(ITerminalTextData dest, ITerminalTextData source);

	/**
	 * @param startRow -1 means follow the end of the data
	 * @param size number of lines to follow
	 */
	void setInterestWindow(int startRow, int size);
	int getInterestWindowStartRow();
	int getInterestWindowSize();

}