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
import org.eclipse.tm.terminal.model.ITerminalTextDataReadOnly;
import org.eclipse.tm.terminal.model.Style;
import org.eclipse.tm.terminal.model.StyleColor;

public class TerminalTextTestHelper {
	static public String toSimple(ITerminalTextDataReadOnly term) {
		return toMultiLineText(term).replaceAll("\000", " ").replaceAll("\n", "");
		
	}
	static public String toMultiLineText(ITerminalTextDataReadOnly term) {
		StringBuffer buff=new StringBuffer();
		int width=term.getWidth();
		for (int y = 0; y < term.getHeight(); y++) {
			if(y>0)
				buff.append("\n"); //$NON-NLS-1$
			for (int x = 0; x < width; x++) {
				buff.append(term.getChar(x, y));
			}
		}
		return buff.toString();
	}
	static public String toSimple(String str) {
		return str.replaceAll("\000", " ").replaceAll("\n", "");
		
	}
	/**
	 * @param term
	 * @param s each character is one line
	 */
	static public void fillSimple(ITerminalTextData term, String s) {
		Style style=Style.getStyle(StyleColor.getStyleColor("fg"), StyleColor.getStyleColor("bg"), false, false, false, false);
		term.setDimensions(1, s.length());
		for (int i = 0; i < s.length(); i++) {
			char c=s.charAt(i);
			term.setChar(0, i, c, style.setForground(StyleColor.getStyleColor(""+c)));
		}
	}
	/**
	 * @param term
	 * @param s lines separated by \n. The terminal will automatically
	 * resized to fit the text.
	 */
	static public void fill(ITerminalTextData term, String s) {
		int width=0;
		int len=0;
		int height=0;
		for (int i = 0; i < s.length(); i++) {
			char c=s.charAt(i);
			if(c=='\n') {
				width=Math.max(width,len);
				len=0;
			} else {
				if(len==0)
					height++;
				len++;
			}
		}
		term.setDimensions(width, height);
		fill(term,0,0,s);
	}
	
	static public void fill(ITerminalTextData term, int x, int y, String s) {
		int xx=x;
		int yy=y;
		Style style=Style.getStyle(StyleColor.getStyleColor("fg"), StyleColor.getStyleColor("bg"), false, false, false, false);
		for (int i = 0; i < s.length(); i++) {
			char c=s.charAt(i);
			if(c=='\n') {
				yy++;
				xx=x;
			} else {
				term.setChar(xx, yy, c, style.setForground(StyleColor.getStyleColor(""+c)));
				xx++;
			}
		}
	}

}
