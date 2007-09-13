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
package org.eclipse.tm.internal.terminal.emulator;

import org.eclipse.tm.terminal.model.ITerminalTextData;
import org.eclipse.tm.terminal.model.Style;

/**
 *
 */
public class VT100EmulatorBackend {

	/**
	 * This field holds the number of the column in which the cursor is
	 * logically positioned. The leftmost column on the screen is column 0, and
	 * column numbers increase to the right. The maximum value of this field is
	 * {@link #widthInColumns} - 1. We track the cursor column using this field
	 * to avoid having to recompute it repeatly using StyledText method calls.
	 * <p>
	 * 
	 * The StyledText widget that displays text has a vertical bar (called the
	 * "caret") that appears _between_ character cells, but ANSI terminals have
	 * the concept of a cursor that appears _in_ a character cell, so we need a
	 * convention for which character cell the cursor logically occupies when
	 * the caret is physically between two cells. The convention used in this
	 * class is that the cursor is logically in column N when the caret is
	 * physically positioned immediately to the _left_ of column N.
	 * <p>
	 * 
	 * When fCursorColumn is N, the next character output to the terminal appears
	 * in column N. When a character is output to the rightmost column on a
	 * given line (column widthInColumns - 1), the cursor moves to column 0 on
	 * the next line after the character is drawn (this is how line wrapping is
	 * implemented). If the cursor is in the bottommost line when line wrapping
	 * occurs, the topmost visible line is scrolled off the top edge of the
	 * screen.
	 * <p>
	 */
	private int fCursorColumn;
	private int fCursorLine;
	private Style fDefaultStyle;
	private Style fStyle;
	int fLines;
	int fColumns;
	final private ITerminalTextData fTerminal;
	public VT100EmulatorBackend(ITerminalTextData terminal) {
		fTerminal=terminal;
	}
	
	/**
	 * This method erases all text from the Terminal view. Including the history
	 */
	public void clearAll() {
		synchronized (fTerminal) {
			fTerminal.setDimensions(fLines, fTerminal.getWidth());
			int startLine=toAbsoluteLine(0);
			for (int line = startLine;  line < startLine+fLines; line++) {
				fTerminal.cleanLine(line);
			}
			setStyle(getDefaultStyle());
		}
	}
	/**
	 * Sets the Dimensions of the addressable scroll space of the screen....
	 * Cleans the screen!
	 * @param lines
	 * @param cols
	 */
	public void setDimensions(int lines, int cols) {
		synchronized (fTerminal) {
			fLines=lines;
			fColumns=cols;
			// make the terminal at least as high as we need lines
			fTerminal.setDimensions(Math.max(fLines,fTerminal.getHeight()), fColumns);			
			setCursor(0, 0);
		}
	}
	
	int toAbsoluteLine(int line) {
		synchronized (fTerminal) {
			return fTerminal.getHeight()-fLines+line;
		}
	}
	/**
	 * This method makes room for N characters on the current line at the cursor
	 * position. Text under the cursor moves right without wrapping at the end
	 * of the line.
	 * 01234
	 * 0 123
	 */
	public void insertCharacters(int charactersToInsert) {
		synchronized (fTerminal) {
			int line=toAbsoluteLine(fCursorLine);
			int n=charactersToInsert;
			for (int col = fColumns-1; col >=fCursorColumn+n; col--) {
				char c=fTerminal.getChar(line, col-n);
				Style style=fTerminal.getStyle(line, col-n);
				fTerminal.setChar(line, col,c, style);
			}
			int last=Math.min(fCursorColumn+n, fColumns);
			for (int col = fCursorColumn; col <last; col++) {
				fTerminal.setChar(line, col,'\000', null);
			}
		}
	}

	/**
	 * 	Erases from cursor to end of screen, including cursor position. Cursor does not move.
	 */
	public void eraseToEndOfScreen() {
		synchronized (fTerminal) {
			eraseLineToEnd();
			for (int line = toAbsoluteLine(fCursorLine+1); line < toAbsoluteLine(fLines); line++) {
				fTerminal.cleanLine(line);
			}
		}
		
	}
	/**
	 * Erases from beginning of screen to cursor, including cursor position. Cursor does not move.
	 */
	public void eraseToCursor() {
		synchronized (fTerminal) {
			for (int line = toAbsoluteLine(0); line < toAbsoluteLine(fCursorLine); line++) {
				fTerminal.cleanLine(line);
			}
			eraseLineToCursor();
		}
	}
	/**
	 * Erases complete display. All lines are erased and changed to single-width. Cursor does not move.
	 */
	public void eraseAll() {
		synchronized (fTerminal) {
			for (int line = toAbsoluteLine(0); line < toAbsoluteLine(fLines); line++) {
				fTerminal.cleanLine(line);
			}
		}
	}
	/**
	 * Erases complete line.
	 */
	public void eraseLine() {
		synchronized (fTerminal) {
			fTerminal.cleanLine(toAbsoluteLine(fCursorLine));
		}
	}
	/**
	 * Erases from cursor to end of line, including cursor position.
	 */
	public void eraseLineToEnd() {
		synchronized (fTerminal) {
			int line=toAbsoluteLine(fCursorLine);
			for (int col = fCursorColumn; col < fColumns; col++) {
				fTerminal.setChar(line, col, '\000', null);
			}
		}
	}	
	/**
	 * Erases from beginning of line to cursor, including cursor position.
	 */
	public void eraseLineToCursor() {
		synchronized (fTerminal) {
			int line=toAbsoluteLine(fCursorLine);
			for (int col = 0; col <= fCursorColumn; col++) {
				fTerminal.setChar(line, col, '\000', null);
			}
		}
	}	

	/**
	 * Inserts n lines at line with cursor. Lines displayed below cursor move down. 
	 * Lines moved past the bottom margin are lost. This sequence is ignored when 
	 * cursor is outside scrolling region.
	 * @param n the number of lines to insert
	 */
	public void insertLines(int n) {
		synchronized (fTerminal) {
			if(!isCusorInScrollingRegion())
				return;
			assert n>0;
			int line=toAbsoluteLine(fCursorLine);
			int nLines=fTerminal.getHeight()-line;
			fTerminal.scroll(line, nLines, n);
		}
	}
	/**
	 * Deletes n characters, starting with the character at cursor position. 
	 * When a character is deleted, all characters to the right of cursor move 
	 * left. This creates a space character at right margin. This character 
	 * has same character attribute as the last character moved left.
	 * @param n
	 * 012345
	 * 0145xx
	 */
	public void deleteCharacters(int n) {
		synchronized (fTerminal) {
			int line=toAbsoluteLine(fCursorLine);
			for (int col = fCursorColumn+n; col < fColumns; col++) {
				char c=fTerminal.getChar(line, col);
				Style style=fTerminal.getStyle(line, col);
				fTerminal.setChar(line, col-n,c, style);
			}
			int first=Math.max(fCursorColumn, fColumns-n);
			for (int col = first; col <fColumns; col++) {
				fTerminal.setChar(line, col,'\000', null);
			}
		}
	}
	/**
	 * Deletes n lines, starting at line with cursor. As lines are deleted, 
	 * lines displayed below cursor move up. Lines added to bottom of screen 
	 * have spaces with same character attributes as last line moved up. This 
	 * sequence is ignored when cursor is outside scrolling region.
	 * @param n the number of lines to delete
	 */
	public void deleteLines(int n) {
		synchronized (fTerminal) {
			if(!isCusorInScrollingRegion())
				return;
			assert n>0;
			int line=toAbsoluteLine(fCursorLine);
			int nLines=fTerminal.getHeight()-line;
			fTerminal.scroll(line, nLines, -n);
		}
	}
	private boolean isCusorInScrollingRegion() {
		// TODO Auto-generated method stub
		return true;
	}

	public Style getDefaultStyle() {
		synchronized (fTerminal) {
			return fDefaultStyle;
		}
	}

	public void setDefaultStyle(Style defaultStyle) {
		synchronized (fTerminal) {
			fDefaultStyle = defaultStyle;
		}
	}

	public Style getStyle() {
		synchronized (fTerminal) {
			if(fStyle==null)
				return fDefaultStyle;
			return fStyle;
		}
	}
	/**
	 * Sets the style to be used from now on
	 * @param style
	 */
	public void setStyle(Style style) {
		synchronized (fTerminal) {
			fStyle=style;
		}
	}
	/**
	 * This method displays a subset of the newly-received text in the Terminal
	 * view, wrapping text at the right edge of the screen and overwriting text
	 * when the cursor is not at the very end of the screen's text.
	 * <p>
	 * 
	 * There are never any ANSI control characters or escape sequences in the
	 * text being displayed by this method (this includes newlines, carriage
	 * returns, and tabs).
	 * <p>
	 */
	public void appendString(String buffer) {
		synchronized (fTerminal) {
			char[] chars=buffer.toCharArray();
			int line=toAbsoluteLine(fCursorLine);
			int i=0;
			while (i < chars.length) {
				int n=Math.min(fColumns-fCursorColumn,chars.length-i);
				fTerminal.setChars(line, fCursorColumn, chars, i, n, fStyle);
				int col=fCursorColumn+n;
				i+=n;
				// wrap needed?
				if(col>=fColumns) {
					if(fCursorLine+1>=fLines) {
						fTerminal.addLine();
					} else {
						setCursorLine(fCursorLine+1);
					}
					line=toAbsoluteLine(fCursorLine);
					setCursorColumn(0);
				} else {
					setCursorColumn(col);
				}
			}
		}
	}
	/**
	 * Process a newline (Control-J) character. A newline (NL) character just
	 * moves the cursor to the same column on the next line, creating new lines
	 * when the cursor reaches the bottom edge of the terminal. This is
	 * counter-intuitive, especially to UNIX programmers who are taught that
	 * writing a single NL to a terminal is sufficient to move the cursor to the
	 * first column of the next line, as if a carriage return (CR) and a NL were
	 * written.
	 * <p>
	 * 
	 * UNIX terminals typically display a NL character as a CR followed by a NL
	 * because the terminal device typically has the ONLCR attribute bit set
	 * (see the termios(4) man page for details), which causes the terminal
	 * device driver to translate NL to CR + NL on output. The terminal itself
	 * (i.e., a hardware terminal or a terminal emulator, like xterm or this
	 * code) _always_ interprets a CR to mean "move the cursor to the beginning
	 * of the current line" and a NL to mean "move the cursor to the same column
	 * on the next line".
	 * <p>
	 */
	public void processNewline() {
		synchronized (fTerminal) {
			if(fCursorLine+1>=fLines) {
				fTerminal.addLine();
			} else {
				setCursorLine(fCursorLine+1);
			}
		}
	}
	/**
	 * This method returns the relative line number of the line containing the
	 * cursor. The returned line number is relative to the topmost visible line,
	 * which has relative line number 0.
	 * 
	 * @return The relative line number of the line containing the cursor.
	 */
	public int getCursorLine() {
		synchronized (fTerminal) {
			return fCursorLine;
		}
	}
	public int getCursorColumn() {
		synchronized (fTerminal) {
			return fCursorColumn;
		}
	}
	/**
	 * This method moves the cursor to the specified line and column. Parameter
	 * <i>targetLine</i> is the line number of a screen line, so it has a
	 * minimum value of 0 (the topmost screen line) and a maximum value of
	 * heightInLines - 1 (the bottommost screen line). A line does not have to
	 * contain any text to move the cursor to any column in that line.
	 */
	public void setCursor(int targetLine, int targetColumn) {
		synchronized (fTerminal) {
			setCursorLine(targetLine);
			setCursorColumn(targetColumn);
		}
	}

	public void setCursorColumn(int targetColumn) {
		synchronized (fTerminal) {
			if(targetColumn<0)
				targetColumn=0;
			else if(targetColumn>=fColumns)
				targetColumn=fColumns-1;
			fCursorColumn=targetColumn;
			// We make the assumption that nobody is changing the
			// terminal cursor except this class!
			// This assumption gives a huge performance improvement
			fTerminal.setCursorColumn(targetColumn);
		}
	}

	public void setCursorLine(int targetLine) {
		synchronized (fTerminal) {
			if(targetLine<0)
				targetLine=0;
			else if(targetLine>=fLines)
				targetLine=fLines-1;
			fCursorLine=targetLine;
			// We make the assumption that nobody is changing the
			// terminal cursor except this class!
			// This assumption gives a huge performance improvement
			fTerminal.setCursorLine(toAbsoluteLine(targetLine));
		}
	}

	public int getLines() {
		synchronized (fTerminal) {
			return fLines;
		}
	}

	public int getColumns() {
		synchronized (fTerminal) {
			return fColumns;
		}
	}
}
