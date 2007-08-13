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
	public TextLineRenderer(TextCanvas c, ITerminalTextDataSnapshot snapshot) {
		fCanvas=c;
		fSnapshot=snapshot;
		fSelectionBackgroundColor = c.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		fSelectionForegooundColor = c.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);

	}
	int fN=0;
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

	public void drawLine(GC gc, int row, int x, int y, int colFirst, int colLast) {
		if(row>=fSnapshot.getHeight() || colFirst>=fSnapshot.getWidth() || colFirst-colLast==0)
			return;
		colLast=Math.min(colLast, fSnapshot.getWidth());
		LineSegment[] segments=fSnapshot.getLineSegments(row, colFirst, colLast-colFirst);
		Font font=gc.getFont();
		Color bg=gc.getBackground();
		Color fg=gc.getForeground();
		for (int i = 0; i < segments.length; i++) {
			LineSegment segment=segments[i];
			Style style=segment.getStyle();
			Color c=fStyleMap.getForegrondColor(style);
			if(c!=fg) {
				fg=c;
				gc.setForeground(fg);
			}
			c=fStyleMap.getBackgroundColor(style);
			if(c!=bg) {
				bg=c;
				gc.setBackground(bg);
			}
			Font f=fStyleMap.getFont(style);
			if(f!=font) {
				font=f;
				gc.setFont(font);
			}

			String text=segment.getText();
			int offset=(segment.getColumn()-colFirst)*getCellWidth();
			text=text.replace('\000', ' ');
			gc.drawString(text,x+offset,y,false);
		}
	}
	public void setVisibleRectangle(int startLine, int startCol, int height, int width) {
		fSnapshot.setInterestWindow(Math.max(0,startLine), Math.max(1,Math.min(fSnapshot.getHeight(),height)));
		
	}
}
