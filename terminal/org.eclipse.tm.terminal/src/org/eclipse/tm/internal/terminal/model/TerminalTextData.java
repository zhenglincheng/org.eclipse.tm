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
import java.util.Arrays;
import java.util.List;

import org.eclipse.tm.terminal.model.ITerminalTextData;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.ITerminalTextHistory;
import org.eclipse.tm.terminal.model.LineSegment;
import org.eclipse.tm.terminal.model.Style;

/**
 * This class is thread safe.
 *
 */
public class TerminalTextData implements ITerminalTextData {
	private char[][] fChars;
	private Style[][] fStyle;
	private int fWidth;
	/**
	 * A list of active snapshots
	 */
	public TerminalTextDataSnapshot[] fSnapshots=new TerminalTextDataSnapshot[0];

	public TerminalTextData() {
		fChars=new char[0][];
		fStyle=new Style[0][];
		fWidth=0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#getWidth()
	 */
	synchronized public int getWidth() {
		return fWidth;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#getHeight()
	 */
	synchronized public int getHeight() {
		// no need for an extra variable
		return fChars.length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#setDimensions(int, int)
	 */
	synchronized public void setDimensions(int width, int height) {
		int n=getHeight();
		fStyle=(Style[][]) resizeMatrix(fStyle, width, height);
		fChars=(char[][]) resizeMatrix(fChars, width, height);
		// set dimensions after successful resize!
		fWidth=width;
		sendLinesChangedToSnapshot(0, n);
	}
	/**
	 * @param matrix a two dimensional array.
	 * @param width the new width
	 * @param height the new height
	 * @return a resized version of the matrix (chopped off if needed or filled with 0 or null)
	 */
	private Object resizeMatrix(Object[] matrix, int width, int height) {
		matrix = (Object[]) resizeArray(matrix, height);
		// new array is [height][old_width]
		Class elementType = matrix.getClass().getComponentType().getComponentType();
		for (int i = 0; i < matrix.length; i++) {
			if (matrix[i] == null) {
				matrix[i] = Array.newInstance(elementType, width);
			} else {
				matrix[i] = resizeArray(matrix[i], width);
			}
		}
		// new array is [height][width]
		return matrix;
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
	synchronized public LineSegment[] getLineSegments(int x, int y, int len) {
		// get the styles and chars for this line
		Style[] styles=fStyle[y];
		char[] chars=fChars[y];
		
		int col=x;
		Style style=styles[x];
		int n=x+len;
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
	synchronized public char getChar(int x, int y) {
		return fChars[y][x];
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#getStyle(int, int)
	 */
	synchronized public Style getStyle(int x, int y) {
		return fStyle[y][x];
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#setChar(int, int, char, org.eclipse.tm.internal.terminal.text.Style)
	 */
	synchronized public void setChar(int x, int y, char c, Style style) {
		fChars[y][x]=c;
		fStyle[y][x]=style;		
		sendLineChangedToSnapshots(y);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#setChars(int, int, char[], org.eclipse.tm.internal.terminal.text.Style)
	 */
	synchronized public void setChars(int x, int y, char[] chars, Style style) {
		setChars(x,y,chars,0,chars.length,style);
		sendLineChangedToSnapshots(y);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#setChars(int, int, char[], int, int, org.eclipse.tm.internal.terminal.text.Style)
	 */
	synchronized public void setChars(int x, int y, char[] chars, int start, int len, Style style) {
		int n=Math.min(len, fWidth-x);
		for (int i = 0; i < n; i++) {
			fChars[y][x+i]=chars[i+start];
			fStyle[y][x+i]=style;		
		}
		sendLineChangedToSnapshots(y);
	}
	/**
	 * @return a text representation of the object.
	 * Rows are separated by '\n'. No style information is returned.
	 */
	synchronized public String textToString() {
		StringBuffer buff=new StringBuffer();
		for (int y = 0; y < fChars.length; y++) {
			if(y>0)
				buff.append("\n"); //$NON-NLS-1$
			for (int x = 0; x < fWidth; x++) {
				buff.append(getChar(x, y));
			}
		}
		return buff.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#scroll(int, int, int)
	 */
	synchronized public void scroll(int startRow, int size, int shift) {
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
		sendScrolledToSnapshots(startRow, size, shift);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#scroll(int, int, int, org.eclipse.tm.test.terminal.text.ITerminalTextHistory)
	 */
	synchronized public void scroll(int startRow, int size, int shift, ITerminalTextHistory history) {
		if(shift<0) {
			saveLines(startRow, -shift, history);
		}
		scroll(startRow,size,shift);
	}
	/**
	 * Save lines in the history
	 * @param y
	 * @param len
	 * @param history
	 */
	private void saveLines(int y, int len,ITerminalTextHistory history) {
		for (int i = y; i < y+len; i++) {
			// Important, the lines will not be used anymore,
			// therefore we don't have to clone them!
			history.addToHistory(fChars[i], fStyle[i]);
		}
	}
	/**
	 * Replaces the lines with new empty data
	 * @param y
	 * @param len
	 */
	private void cleanLines(int y, int len) {
		for (int i = y; i < y+len; i++) {
			fChars[i]=new char[fWidth];
			fStyle[i]=new Style[fWidth];
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.tm.internal.terminal.text.ITerminalTextData#toString()
	 */
	public String toString() {
		return textToString();
	}
	/**
	 * Makes a copy of the content of <code>this</code> into <code>copy</code>.
	 * @param copy will have the same content and dimensions as <code>this</code> 
	 */
	synchronized void copyInto(TerminalTextData copy) {
		if(copy.fChars.length!=fChars.length) {
			copy.fChars=new char[fChars.length][];
			copy.fStyle=new Style[fChars.length][];
		}
		copy.fWidth=fWidth;
		for (int i = 0; i < fChars.length; i++) {
			copy.fChars[i]=(char[]) fChars[i].clone();
			copy.fStyle[i]=(Style[]) fStyle[i].clone();
		}
		copy.sendLinesChangedToSnapshot(0,getHeight());
	}
	/**
	 * Makes a copy of the content of <code>this</code> into <code>copy</code> filtered
	 * by <code>linesToCopy</code>.
	 * @param copy <code>this</code> and <code>copy</code> must have the same dimensions
	 * @param linesToCopy <code>length</code> must be the same as {@link #getHeight()}
	 */
	synchronized void copyInto(TerminalTextData copy,boolean [] linesToCopy) {
		for (int i = 0; i < linesToCopy.length; i++) {
			if(linesToCopy[i]) {
				copy.fChars[i]=(char[]) fChars[i].clone();
				copy.fStyle[i]=(Style[]) fStyle[i].clone();
				copy.sendLineChangedToSnapshots(i);
			}
		}
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

	public synchronized ITerminalTextDataSnapshot makeSnapshot() {
		// poor mans approach to modify the array
		ITerminalTextDataSnapshot snapshot=new TerminalTextDataSnapshot(this);
		snapshot.updateSnapshot(false);
		List list=new ArrayList();
		list.addAll(Arrays.asList(fSnapshots));
		list.add(snapshot);
		fSnapshots=(TerminalTextDataSnapshot[]) list.toArray(new TerminalTextDataSnapshot[list.size()]);
		return snapshot;
	}
}
