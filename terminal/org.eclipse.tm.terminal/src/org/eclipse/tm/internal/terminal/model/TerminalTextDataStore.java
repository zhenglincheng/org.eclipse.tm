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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.tm.terminal.model.ITerminalTextData;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.LineSegment;
import org.eclipse.tm.terminal.model.Style;

/**
 * This class is thread safe.
 *
 */
public class TerminalTextDataStore implements ITerminalTextData {
	private char[][] fChars;
	private Style[][] fStyle;
	private int fWidth;
	private int fHeight;
	private int fMaxHeight;
	public TerminalTextDataStore() {
		fChars=new char[0][];
		fStyle=new Style[0][];
		fWidth=0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#getWidth()
	 */
	public int getWidth() {
		return fWidth;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#getHeight()
	 */
	public int getHeight() {
		return fHeight;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#setDimensions(int, int)
	 */
	public void setDimensions(int width, int height) {
		// just extend the region
		if(height>fChars.length) {
			int h=4*height/3;
			if(fMaxHeight>0 && h>fMaxHeight)
				h=fMaxHeight;
			fStyle=(Style[][]) resizeArray(fStyle, height);
			fChars=(char[][]) resizeArray(fChars, height);
		}
		// clean the new lines
		if(height>fHeight) {
			for (int i = fHeight; i < height; i++) {
				fStyle[i]=null;
				fChars[i]=null;
			}
		}
		// set dimensions after successful resize!
		fWidth=width;
		fHeight=height;
	}
	/**
	 * Reallocates an array with a new size, and copies the contents of the old
	 * array to the new array.
	 * 
	 * @param origArray the old array, to be reallocated.
	 * @param newSize the new array size.
	 * @return A new array with the same contents (chopped off if needed or filled with 0 or null).
	 */
	private Object resizeArray(Object origArray, int newSize) {
		int oldSize = Array.getLength(origArray);
		if(oldSize==newSize)
			return origArray;
		Class elementType = origArray.getClass().getComponentType();
		Object newArray = Array.newInstance(elementType, newSize);
		int preserveLength = Math.min(oldSize, newSize);
		if (preserveLength > 0)
			System.arraycopy(origArray, 0, newArray, 0, preserveLength);
		return newArray;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#getLineSegments(int, int, int)
	 */
	public LineSegment[] getLineSegments(int x, int y, int len) {
		// get the styles and chars for this line
		Style[] styles=fStyle[y];
		char[] chars=fChars[y];
		int col=x;
		int n=x+len;
		
		// expand the line if needed....
		if(styles==null)
			styles=new Style[n];
		else if(styles.length<n)
			styles=(Style[]) resizeArray(styles, n);

		if(chars==null)
			chars=new char[n];
		else if(chars.length<n)
			chars=(char[]) resizeArray(chars, n);
	
		// and create the line segments
		Style style=styles[x];
		List segments=new ArrayList();
		for (int i = x; i < n; i++) {
			if(styles[i]!=style) {
				segments.add(new LineSegment(col,new String(chars,col,i-col),style));
				style=styles[i];
				col=i;
			}
		}
		if(col < n) {
			segments.add(new LineSegment(col,new String(chars,col,n-col),style));
		}
		return (LineSegment[]) segments.toArray(new LineSegment[segments.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#getChar(int, int)
	 */
	public char getChar(int x, int y) {
		assert x<fWidth;
		if(fChars[y]==null||x>=fChars[y].length)
			return 0;
		return fChars[y][x];
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#getStyle(int, int)
	 */
	public Style getStyle(int x, int y) {
		assert x<fWidth;
		if(fStyle[y]==null || x>=fStyle[y].length)
			return null;
		return fStyle[y][x];
	}
	
	void ensureLineLength(int iLine, int length) {
		if(length>fWidth)
			throw new ArrayIndexOutOfBoundsException();
		if(fChars[iLine]==null) {
			fChars[iLine]=new char[length];
		} else if(fChars[iLine].length<length) {
			fChars[iLine]=(char[]) resizeArray(fChars[iLine],length);
		}
		if(fStyle[iLine]==null) {
			fStyle[iLine]=new Style[length];
		} else if(fStyle[iLine].length<length) {
			fStyle[iLine]=(Style[]) resizeArray(fStyle[iLine],length);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#setChar(int, int, char, org.eclipse.tm.internal.terminal.text.Style)
	 */
	public void setChar(int x, int y, char c, Style style) {
		ensureLineLength(y,x+1);
		fChars[y][x]=c;
		fStyle[y][x]=style;		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#setChars(int, int, char[], org.eclipse.tm.internal.terminal.text.Style)
	 */
	public void setChars(int x, int y, char[] chars, Style style) {
		setChars(x,y,chars,0,chars.length,style);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#setChars(int, int, char[], int, int, org.eclipse.tm.internal.terminal.text.Style)
	 */
	public void setChars(int x, int y, char[] chars, int start, int len, Style style) {
		int n=Math.min(len, fWidth-x);
		ensureLineLength(y,x+n);
		for (int i = 0; i < n; i++) {
			fChars[y][x+i]=chars[i+start];
			fStyle[y][x+i]=style;		
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#scroll(int, int, int)
	 */
	public void scroll(int startRow, int size, int shift) {
		if(shift<0) {
			// move the region up
			// shift is negative!!
			for (int i = startRow; i < startRow+size+shift; i++) {
				fChars[i]=fChars[i-shift];
				fStyle[i]=fStyle[i-shift];
			}
			// then clean the opened lines
			cleanLines(Math.max(0, startRow+size+shift),Math.min(-shift, getHeight()-startRow));
		} else {
			for (int i = startRow+size-1; i >=startRow && i-shift>=0; i--) {
				fChars[i]=fChars[i-shift];
				fStyle[i]=fStyle[i-shift];
			}
			cleanLines(startRow, Math.min(shift, getHeight()-startRow));
		}
	}
	/**
	 * Replaces the lines with new empty data
	 * @param y
	 * @param len
	 */
	private void cleanLines(int y, int len) {
		for (int i = y; i < y+len; i++) {
			fChars[i]=null;
			fStyle[i]=null;
		}
	}
	
	/*
	 * @return a text representation of the object.
	 * Rows are separated by '\n'. No style information is returned.
	 */
	public String toString() {
		StringBuffer buff=new StringBuffer();
		for (int y = 0; y < getHeight(); y++) {
			if(y>0)
				buff.append("\n"); //$NON-NLS-1$
			for (int x = 0; x < fWidth; x++) {
				buff.append(getChar(x, y));
			}
		}
		return buff.toString();
	}


	public ITerminalTextDataSnapshot makeSnapshot() {
		throw new UnsupportedOperationException();
	}

	public void addLine() {
		if(fMaxHeight>0 && getHeight()<fMaxHeight) {
			setDimensions(getWidth(), getHeight()+1);
		} else {
			scroll(0,getHeight(),-1);
		}
	}

	public void copy(ITerminalTextData source) {
		fWidth=source.getWidth();
		int n=source.getHeight();
		if(getHeight()!=n) {
			fChars=new char[n][];
			fStyle=new Style[n][];
		}
		for (int i = 0; i < n; i++) {
			fChars[i]=source.getChars(i);
			fStyle[i]=source.getStyles(i);
		}
		fHeight=n;
	}

	public void copyLines(ITerminalTextData source, int sourceStart, int destStart, boolean[] linesToCopy) {
		for (int i = 0; i < linesToCopy.length; i++) {
			if(linesToCopy[i]) {
				fChars[i+destStart]=source.getChars(i+sourceStart);
				fStyle[i+destStart]=source.getStyles(i+sourceStart);
			}
		}
	}

	public char[] getChars(int line) {
		if(fChars[line]==null)
			return null;
		return (char[]) fChars[line].clone();
	}

	public Style[] getStyles(int line) {
		if(fStyle[line]==null)
			return null;
		return (Style[]) fStyle[line].clone();
	}

	public void setLine(int line, char[] chars, Style[] styles) {
		fChars[line]=(char[]) chars.clone();
		fStyle[line]=(Style[]) styles.clone();
	}

	public void setMaxHeight(int height) {
		fMaxHeight=height;
	}

	public int getMaxHeight() {
		return fMaxHeight;
	}
}
