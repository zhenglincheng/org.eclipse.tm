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
package org.eclipse.tm.terminal.model;


/**
 * A writable matrix of characters and {@link Style}. This is intended  to be the
 * low level representation of the text of a Terminal. Higher layers are responsible
 * to fill the text and styles into this representation. 
 * 
 * <p><b>Note: </b> Implementations of this interface has to be thread safe.
 * <p><b>Note: </b> This interface is not intended to be implemented by clients.
 */
public interface ITerminalTextData extends ITerminalTextDataReadOnly {

	/**
	 * Sets the dimensions of the data. If the dimensions are smaller than the current
	 * dimensions, the lines and rows will be chopped. If the dimensions are bigger, then
	 * the new elements will be filled with 0 chars and null Style.
	 * 
	 * @param width
	 * @param height
	 */
	void setDimensions(int width, int height);

	void setMaxHeight(int height);
	int getMaxHeight();

	/**
	 * Set a single character and the associated {@link Style}.
	 * 
	 * @param x x must be >=0 and < width
	 * @param y y must be >=0 and < height
	 * @param c the new character at this position
	 * @param style the style or null
	 */
	void setChar(int x, int y, char c, Style style);

	/**
	 * Set an array of characters showing in the same {@link Style}.
	 * 
	 * @param x x must be >=0 and < width
	 * @param y y must be >=0 and < height
	 * @param chars the new characters at this position
	 * @param style the style or null
	 */
	void setChars(int x, int y, char[] chars, Style style);

	/**
	 * Set a subrange of an array of characters showing in the same {@link Style}.
	 * 
	 * @param x x must be >=0 and < width
	 * @param y y must be >=0 and < height
	 * @param chars the new characters at this position
	 * @param start the start index in the chars array
	 * @param len the number of characters to insert. Characters beyond width are not inserted.
	 * @param style the style or null
	 */
	void setChars(int x, int y, char[] chars, int start, int len, Style style);
	

//	/**
//	 * @param line
//	 * @return true if this line belongs to the previous line but is simply
//	 * wrapped. 
//	 */
//	boolean isWrappedLine(int line);
//	
//	/**
//	 * Makes this line an extension to the previous line. Wrapped lines get folded back
//	 * when the width of the terminal changes
//	 * @param line
//	 * @param extendsPreviousLine
//	 */
//	void setWrappedLine(int line, boolean extendsPreviousLine);
	
	/**
	 * Shifts some lines up or down. The "empty" space is filled with <code>'\000'</code> chars 
	 * and <code>null</code> {@link Style}
	 * <p>To illustrate shift, here is some sample data:
	 * <pre>
	 * 0 aaaa
	 * 1 bbbb
	 * 2 cccc
	 * 3 dddd
	 * 4 eeee
	 * </pre>
	 * 
	 * Shift a region of 3 lines <b>up</b> by one line <code>shift(1,3,-1)</code>
	 * <pre>
	 * 0 aaaa
	 * 1 cccc
	 * 2 dddd
	 * 3 
	 * 4 eeee
	 * </pre>
	 * 
	 * 
	 * Shift a region of 3 lines <b>down</b> by one line <code>shift(1,3,1)</code>
	 * <pre>
	 * 0 aaaa
	 * 1 
	 * 2 bbbb
	 * 3 cccc
	 * 4 eeee
	 * </pre>
	 * @param startRow the start row of the shift
	 * @param size the number of lines to shift
	 * @param shift how much scrolling is done. New scrolled area is filled with <code>'\000</code>'.
	 * Negative number means scroll down, positive scroll up (see example above).
	 */
	void scroll(int startRow, int size, int shift);
	
	void addLine();
	void copy(ITerminalTextData source);
	void copyRange(ITerminalTextData source, int sourceStart, int destStart,int length);
	void copySelective(ITerminalTextData source,int sourceStart, int destStart, boolean[] linesToCopy);
}