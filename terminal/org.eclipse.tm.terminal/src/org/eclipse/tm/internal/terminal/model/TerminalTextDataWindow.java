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

import org.eclipse.tm.terminal.model.ITerminalTextData;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.LineSegment;
import org.eclipse.tm.terminal.model.Style;

/**
 * This class stores the data only within a window {@link #setWindow(int, int)} and 
 * {@link #getWindowOffset()} and {@link #getWindowSize()}. Everything outside 
 * the is <code>char=='\000'</code> and <code>style=null</code>.
 *
 */
public class TerminalTextDataWindow implements ITerminalTextData {
	final ITerminalTextData fData;
	int fWindowOffset;
	int fWindowSize;
	int fHeight;
	int fMaxHeight;
	public TerminalTextDataWindow(ITerminalTextData data) {
		fData=data;
	}
	public TerminalTextDataWindow() {
		this(new TerminalTextDataStore());
	}
	/**
	 * @param line
	 * @return true if the line is within the window
	 */
	boolean isInWindow(int line) {
		return line>=fWindowOffset && line<fWindowOffset+fWindowSize;
	}
	public char getChar(int line, int column) {
		if(!isInWindow(line))
			return 0;
		return fData.getChar(line-fWindowOffset, column);
	}

	public char[] getChars(int line) {
		if(!isInWindow(line))
			return null;
		return fData.getChars(line-fWindowOffset);
	}

	public int getHeight() {
		return fHeight;
	}

	public LineSegment[] getLineSegments(int line, int startCol, int numberOfCols) {
		if(!isInWindow(line))
			return new LineSegment[]{new LineSegment(startCol,new String(new char[numberOfCols]),null)};
		return fData.getLineSegments(line-fWindowOffset, startCol, numberOfCols);
	}

	public int getMaxHeight() {
		return fMaxHeight;
	}

	public Style getStyle(int line, int column) {
		if(!isInWindow(line))
			return null;
		return fData.getStyle(line-fWindowOffset, column);
	}

	public Style[] getStyles(int line) {
		if(!isInWindow(line))
			return null;
		return fData.getStyles(line-fWindowOffset);
	}

	public int getWidth() {
		return fData.getWidth();
	}

	public ITerminalTextDataSnapshot makeSnapshot() {
		throw new UnsupportedOperationException();
	}
	public void addLine() {
		if(fMaxHeight>0 && getHeight()<fMaxHeight) {
			setDimensions(getHeight()+1, getWidth());
		} else {
			scroll(0,getHeight(),-1);
		}
	}
	public void copy(ITerminalTextData source) {
		// we inherit the dimensions of the source
		setDimensions(source.getHeight(), source.getWidth());
		int n=Math.min(fWindowSize, source.getHeight()-fWindowOffset);
		if(n>0)
			fData.copyRange(source, fWindowOffset, 0, n);
	}
	public void copyRange(ITerminalTextData source, int sourceStart, int destStart, int length) {
		int n=length;
		int dStart=destStart-fWindowOffset;
		int sStart=sourceStart;
		// if start outside our range, cut the length to copy
		if(dStart<0) {
			n+=dStart;
			sStart-=dStart;
			dStart=0;
		}
		// do not exceed the window size
		n=Math.min(n,fWindowSize);
		if(n>0)
			fData.copyRange(source, sStart, dStart, n);
		
	}
	public void copySelective(ITerminalTextData source, int sourceStart, int destStart, boolean[] linesToCopy) {
		int n=linesToCopy.length;
		int dStart=destStart-fWindowOffset;
		int sStart=sourceStart;
		// the offset into linesToCopy
		int offset=0;
		// if start outside our range, cut the length to copy
		if(dStart<0) {
			n+=dStart;
			sStart-=dStart;
			offset=-dStart;
			dStart=0;
		}
		// do not exceed the window size
		n=Math.min(n,fWindowSize);
		// do the copying line by line
		for (int i = 0; i < n; i++) {
			if(linesToCopy[i+offset])
				fData.copyRange(source, sStart+i, dStart+i, 1);
		}
	}
	public void scroll(int startLine, int size, int shift) {
		int n=size;
		int start=startLine-fWindowOffset;
		// if start outside our range, cut the length to copy
		if(start<0) {
			n+=start;
			start=0;
		}
		n=Math.min(n,fWindowSize);
		// do not exceed the window size
		if(n>0)
			fData.scroll(start, n, shift);
	}
	public void setChar(int line, int column, char c, Style style) {
		if(!isInWindow(line))
			return;
		fData.setChar(line-fWindowOffset, column, c, style);
	}
	public void setChars(int line, int column, char[] chars, int start, int len, Style style) {
		if(!isInWindow(line))
			return;
		fData.setChars(line-fWindowOffset, column, chars, start, len, style);
	}
	public void setChars(int line, int column, char[] chars, Style style) {
		if(!isInWindow(line))
			return;
		fData.setChars(line-fWindowOffset, column, chars, style);
	}
	public void setDimensions(int height, int width) {
		fData.setDimensions(fWindowSize, width);
		fHeight=height;
	}
	public void setMaxHeight(int height) {
		fMaxHeight=height;
	}
	public void setWindow(int offset, int size) {
		fWindowOffset=offset;
		fWindowSize=size;
		fData.setDimensions(fWindowSize, getWidth());
	}
	public int getWindowOffset() {
		return fWindowOffset;
	}
	public int getWindowSize() {
		return fWindowSize;
	}
	public void setHeight(int height) {
		fHeight = height;
	}
}
