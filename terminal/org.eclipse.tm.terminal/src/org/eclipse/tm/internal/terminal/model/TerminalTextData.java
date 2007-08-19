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
 * This class is thread safe.
 *
 */
public class TerminalTextData implements ITerminalTextData {
	final ITerminalTextData fData;
	/**
	 * A list of active snapshots
	 */
	public TerminalTextDataSnapshot[] fSnapshots=new TerminalTextDataSnapshot[0];
	private int fCursorColumn;
	private int fCursorLine;

	public TerminalTextData() {
		this(new TerminalTextDataFastScroll());

//		this(new TerminalTextDataStore());
	}
	public TerminalTextData(ITerminalTextData data) {
		fData=data;
	}
	/*synchronized*/ public int getWidth() {
		return fData.getWidth();
	}
	/*synchronized*/ public int getHeight() {
		// no need for an extra variable
		return fData.getHeight();
	}
	/*synchronized*/ public void setDimensions(int height, int width) {
		int h=getHeight();
		int w=getWidth();
		if(w==width && h==height)
			return;
		fData.setDimensions(height, width);
		sendDimensionsChanged(h, w, height, width);
	}
	private void sendDimensionsChanged(int oldHeight, int oldWidth, int newHeight, int newWidth) {
		// determine what has changed
		if(oldWidth==newWidth) {
			if(oldHeight<newHeight)
				sendLinesChangedToSnapshot(oldHeight, newHeight-oldHeight);
			else
				sendLinesChangedToSnapshot(newHeight,oldHeight-newHeight);
		} else {
			sendLinesChangedToSnapshot(0, oldHeight);
		}
		sendDimensionsChanged();
	}
	/*synchronized*/ public LineSegment[] getLineSegments(int line, int column, int len) {
		return fData.getLineSegments(line, column, len);
	}
	/*synchronized*/ public char getChar(int line, int column) {
		return fData.getChar(line, column);
	}
	/*synchronized*/ public Style getStyle(int line, int column) {
		return fData.getStyle(line, column);
	}
	/*synchronized*/ public void setChar(int line, int column, char c, Style style) {
		fData.setChar(line, column, c, style);
		sendLineChangedToSnapshots(line);
	}
	/*synchronized*/ public void setChars(int line, int column, char[] chars, Style style) {
		fData.setChars(line, column, chars, style);
		sendLineChangedToSnapshots(line);
	}
	/*synchronized*/ public void setChars(int line, int column, char[] chars, int start, int len, Style style) {
		fData.setChars(line, column, chars, start, len, style);
		sendLineChangedToSnapshots(line);
	}
	/*synchronized*/ public void scroll(int startLine, int size, int shift) {
		fData.scroll(startLine, size, shift);
		sendScrolledToSnapshots(startLine, size, shift);
	}
	/*synchronized*/ public String toString() {
		return fData.toString();
	}
	private void sendDimensionsChanged() {
		for (int i = 0; i < fSnapshots.length; i++) {
			fSnapshots[i].markDimensionsChanged();
		}
	}
	/**
	 * @param line notifies snapshots that line line has changed
	 */
	protected void sendLineChangedToSnapshots(int line) {
		for (int i = 0; i < fSnapshots.length; i++) {
			fSnapshots[i].markLineChanged(line);
		}
	}
	/**
	 * Notify snapshots that multiple lines have changed
	 * @param line changed line
	 * @param n number of changed lines
	 */
	protected void sendLinesChangedToSnapshot(int line,int n) {
		for (int i = 0; i < fSnapshots.length; i++) {
			fSnapshots[i].markLinesChanged(line, n);
		}
	}
	
	/**
	 * Notify snapshot that a region was scrolled
	 * @param startLine
	 * @param size
	 * @param shift
	 */
	protected void sendScrolledToSnapshots(int startLine,int size, int shift) {
		for (int i = 0; i < fSnapshots.length; i++) {
			fSnapshots[i].scroll(startLine, size, shift);
		}
	}
	/**
	 * Removes the snapshot from the @observer@ list
	 * @param snapshot
	 */
	protected void removeSnapshot(TerminalTextDataSnapshot snapshot) {
		// poor mans approach to modify the array
		List list=new ArrayList();
		list.addAll(Arrays.asList(fSnapshots));
		list.remove(snapshot);
		fSnapshots=(TerminalTextDataSnapshot[]) list.toArray(new TerminalTextDataSnapshot[list.size()]);
	}

	/*synchronized*/ public ITerminalTextDataSnapshot makeSnapshot() {
		// poor mans approach to modify the array
		TerminalTextDataSnapshot snapshot=new TerminalTextDataSnapshot(this);
		snapshot.markDimensionsChanged();
		List list=new ArrayList();
		list.addAll(Arrays.asList(fSnapshots));
		list.add(snapshot);
		fSnapshots=(TerminalTextDataSnapshot[]) list.toArray(new TerminalTextDataSnapshot[list.size()]);
		return snapshot;
	}
	/*synchronized*/ public void addLine() {
		int oldHeight=getHeight();
		fData.addLine();
		// was is an append or a scroll?
		int newHeight=getHeight();
		if(newHeight>oldHeight) {
			//the line was appended 
			sendLinesChangedToSnapshot(oldHeight, 1);
			int width=getWidth();
			sendDimensionsChanged(oldHeight, width, newHeight, width);

		} else {
			// the line was scrolled
			sendScrolledToSnapshots(0, oldHeight, -1);
		}
			
	}

	/*synchronized*/ public void copy(ITerminalTextData source) {
		fData.copy(source);
		fCursorLine=source.getCursorLine();
		fCursorColumn=source.getCursorColumn();
	}

	/*synchronized*/ public void copyLine(ITerminalTextData source, int sourceLine, int destLine) {
		fData.copyLine(source, sourceLine, destLine);
	}
	/*synchronized*/ public void copyRange(ITerminalTextData source, int sourceStartLine, int destStartLine, int length) {
		fData.copyRange(source, sourceStartLine, destStartLine, length);
	}
	/*synchronized*/ public char[] getChars(int line) {
		return fData.getChars(line);
	}
	/*synchronized*/ public Style[] getStyles(int line) {
		return fData.getStyles(line);
	}
	/*synchronized*/ public int getMaxHeight() {
		return fData.getMaxHeight();
	}
	/*synchronized*/ public void setMaxHeight(int height) {
		fData.setMaxHeight(height);
	}
	/*synchronized*/ public void cleanLine(int line) {
		fData.cleanLine(line);
		sendLineChangedToSnapshots(line);
	}
	/*synchronized*/ public int getCursorColumn() {
		return fCursorColumn;
	}
	/*synchronized*/ public int getCursorLine() {
		return fCursorLine;
	}
	/*synchronized*/ public void setCursorColumn(int column) {
		fCursorColumn=column;
	}
	/*synchronized*/ public void setCursorLine(int line) {
		fCursorLine=line;
	}
}
