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

	public TerminalTextData() {
		this(new TerminalTextDataStore());
	}
	public TerminalTextData(ITerminalTextData data) {
		fData=data;
	}
	synchronized public int getWidth() {
		return fData.getWidth();
	}
	synchronized public int getHeight() {
		// no need for an extra variable
		return fData.getHeight();
	}
	synchronized public void setDimensions(int width, int height) {
		int h=getHeight();
		int w=getWidth();
		if(w==width && h==height)
			return;
		fData.setDimensions(width, height);
		// determine what has changed
		if(w==width) {
			if(h<height)
				sendLinesChangedToSnapshot(h, height-h);
			else
				sendLinesChangedToSnapshot(height,h-height);
		} else {
			sendLinesChangedToSnapshot(0, h);
		}
	}
	synchronized public LineSegment[] getLineSegments(int x, int y, int len) {
		return fData.getLineSegments(x, y, len);
	}
	synchronized public char getChar(int x, int y) {
		return fData.getChar(x, y);
	}
	synchronized public Style getStyle(int x, int y) {
		return fData.getStyle(x, y);
	}
	synchronized public void setChar(int x, int y, char c, Style style) {
		fData.setChar(x, y, c, style);
		sendLineChangedToSnapshots(y);
	}
	synchronized public void setChars(int x, int y, char[] chars, Style style) {
		fData.setChars(x, y, chars, style);
		sendLineChangedToSnapshots(y);
	}
	synchronized public void setChars(int x, int y, char[] chars, int start, int len, Style style) {
		fData.setChars(x, y, chars, start, len, style);
		sendLineChangedToSnapshots(y);
	}
	/**
	 * @return a text representation of the object.
	 * Rows are separated by '\n'. No style information is returned.
	 */
	synchronized public String textToString() {
		return fData.toString();
	}

	synchronized public void scroll(int startRow, int size, int shift) {
		fData.scroll(startRow, size, shift);
		sendScrolledToSnapshots(startRow, size, shift);
	}
	synchronized public String toString() {
		return textToString();
	}
	/**
	 * @param y notifies snapshots that line y has changed
	 */
	protected void sendLineChangedToSnapshots(int y) {
		for (int i = 0; i < fSnapshots.length; i++) {
			fSnapshots[i].markLineChanged(y);
		}
	}
	/**
	 * Notify snapshots that multiple lines have changed
	 * @param y changed line
	 * @param n number of changed lines
	 */
	protected void sendLinesChangedToSnapshot(int y,int n) {
		for (int i = 0; i < fSnapshots.length; i++) {
			fSnapshots[i].markLinesChanged(y, n);
		}
	}
	
	/**
	 * Notify snapshot that a region was scrolled
	 * @param startRow
	 * @param size
	 * @param shift
	 */
	protected void sendScrolledToSnapshots(int startRow,int size, int shift) {
		for (int i = 0; i < fSnapshots.length; i++) {
			fSnapshots[i].scroll(startRow, size, shift);
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

	synchronized public ITerminalTextDataSnapshot makeSnapshot() {
		// poor mans approach to modify the array
		ITerminalTextDataSnapshot snapshot=new TerminalTextDataSnapshot(this);
		snapshot.updateSnapshot(false);
		List list=new ArrayList();
		list.addAll(Arrays.asList(fSnapshots));
		list.add(snapshot);
		fSnapshots=(TerminalTextDataSnapshot[]) list.toArray(new TerminalTextDataSnapshot[list.size()]);
		return snapshot;
	}
	synchronized public void addLine() {
		int h=getHeight();
		fData.addLine();
		// was is an append or a scroll?
		if(getHeight()>h) {
			//the line was appended 
			sendLinesChangedToSnapshot(h, 1);
		} else {
			// the line was scrolled
			sendScrolledToSnapshots(0, h, -1);
		}
			
	}

	synchronized public void copy(ITerminalTextData source) {
		fData.copy(source);
	}

	synchronized public void copyLines(ITerminalTextData source, int sourceStart, int destStart, boolean[] linesToCopy) {
		fData.copyLines(source, sourceStart, destStart, linesToCopy);
	}
	synchronized public char[] getChars(int line) {
		return fData.getChars(line);
	}
	synchronized public Style[] getStyles(int line) {
		return fData.getStyles(line);
	}
	public int getMaxHeight() {
		return fData.getMaxHeight();
	}
	public void setMaxHeight(int height) {
		fData.setMaxHeight(height);
	}
}
