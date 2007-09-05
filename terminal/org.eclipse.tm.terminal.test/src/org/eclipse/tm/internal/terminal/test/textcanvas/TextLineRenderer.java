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
package org.eclipse.tm.internal.terminal.test.textcanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.LineSegment;
import org.eclipse.tm.terminal.model.Style;

/**
 * @author Michael.Scharf@scharf-software.com
 *
 */
public class TextLineRenderer implements ILinelRenderer {
	TextCanvas fCanvas;
	ITerminalTextDataSnapshot fSnapshot;
	StyleMap fStyleMap=new StyleMap();
	// TODO use the selection colors
	Color fSelectionBackgroundColor;
	Color fSelectionForegooundColor;
	Color fBackgroundColor;
	public TextLineRenderer(TextCanvas c, ITerminalTextDataSnapshot snapshot) {
		fCanvas=c;
		fSnapshot=snapshot;
		fSelectionBackgroundColor = c.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
//		fBackgroundColor = c.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		fBackgroundColor = c.getDisplay().getSystemColor(SWT.COLOR_YELLOW);

	}
	int fN=0;
	private boolean fHasFocus;
	String getLabel(int index) {
		return ""+(index%10);
	}
	/* (non-Javadoc)
	 * @see com.imagicus.thumbs.view.ICellRenderer#getCellWidth()
	 */
	public int getCellWidth() {
		return fStyleMap.getFontWidth();
	}
	/* (non-Javadoc)
	 * @see com.imagicus.thumbs.view.ICellRenderer#getCellHeight()
	 */
	public int getCellHeight() {
		return fStyleMap.getFontHeight();
	}
	static Font getBoldFont(Font font) {
		FontData fontDatas[] = font.getFontData();
		FontData data = fontDatas[0];
		return new Font(Display.getCurrent(), data.getName(), data.getHeight(), data.getStyle()|SWT.BOLD);
	}

	public void drawLine(ITextCanvasModel model, GC gc, int row, int x, int y, int colFirst, int colLast) {
		if(row<0 || row>=fSnapshot.getHeight() || colFirst>=fSnapshot.getWidth() || colFirst-colLast==0) {
			fillBackground(gc, x, y, getCellWidth()*(colFirst-colLast), getCellHeight());
		} else {
			colLast=Math.min(colLast, fSnapshot.getWidth());
			LineSegment[] segments=fSnapshot.getLineSegments(row, colFirst, colLast-colFirst);
			for (int i = 0; i < segments.length; i++) {
				LineSegment segment=segments[i];
				Style style=segment.getStyle();
				setupGC(gc, style);
				String text=segment.getText();
				drawText(gc, x, y, colFirst, segment.getColumn(), text);
				drawCursor(model, gc, row, x, y, colFirst);
			}
		}
	}
	
	protected void fillBackground(GC gc, int x, int y, int width, int height) {
		Color bg=gc.getBackground();
		gc.setBackground(getBackgroundColor());
		gc.fillRectangle (x,y,width,height);
		gc.setBackground(bg);
		
	}

	private Color getBackgroundColor() {
		return fBackgroundColor;
	}
	private void drawCursor(ITextCanvasModel model, GC gc, int row, int x, int y, int colFirst) {
		if(!model.isCursorOn() || !hasFocus())
			return;
		int cursorLine=model.getCursorLine();
			
		if(row==cursorLine) {
			int cursorColumn=model.getCursorColumn();
			if(cursorColumn<fSnapshot.getWidth()) {
				Style style=fSnapshot.getStyle(row, cursorColumn);
				if(style==null) {
					style=Style.getStyle("BLACK", "WHITE");
				}
				style=style.setReverse(!style.isReverse());
				setupGC(gc,style);
				String text=""+fSnapshot.getChar(row, cursorColumn);
				drawText(gc, x, y, colFirst, cursorColumn, text);
			}
		}
	}
	private void drawText(GC gc, int x, int y, int colFirst, int col, String text) {
		int offset=(col-colFirst)*getCellWidth();
		text=text.replace('\000', ' ');
		gc.drawString(text,x+offset,y,false);
	}
	private void setupGC(GC gc, Style style) {
		Color c=fStyleMap.getForegrondColor(style);
		if(c!=gc.getForeground()) {
			gc.setForeground(c);
		}
		c=fStyleMap.getBackgroundColor(style);
		if(c!=gc.getBackground()) {
			gc.setBackground(c);
		}
		Font f=fStyleMap.getFont(style);
		if(f!=gc.getFont()) {
			gc.setFont(f);
		}
	}
	public void setVisibleRectangle(int startLine, int startCol, int height, int width) {
		fSnapshot.setInterestWindow(Math.max(0,startLine), Math.max(1,Math.min(fSnapshot.getHeight(),height)));
		
	}
	public boolean hasFocus() {
		return fHasFocus;
	}
	public void setFocus(boolean focus) {
		fHasFocus=focus;
		
	}
}
