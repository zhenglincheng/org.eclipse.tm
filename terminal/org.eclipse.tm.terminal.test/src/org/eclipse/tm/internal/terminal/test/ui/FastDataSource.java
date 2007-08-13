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
package org.eclipse.tm.internal.terminal.test.ui;

import org.eclipse.tm.terminal.model.Style;
import org.eclipse.tm.terminal.model.StyleColor;

final class FastDataSource extends AbstractLineOrientedDataSource {
	char lines[][]=new char[][]{
			"this is line 123456789 123456789 123456789 123456789 123456789 123456789 ".toCharArray(),
			"this is line 2".toCharArray()
	};

	Style styleNormal=Style.getStyle(StyleColor.getStyleColor("black"),StyleColor.getStyleColor("cyan"));

	Style styles[]=new Style[] {
			styleNormal,
			styleNormal.setBold(true),
			styleNormal.setForground("red"),
			styleNormal.setForground("yellow"),
			styleNormal.setBold(true).setUnderline(true),
			styleNormal.setReverse(true),
			styleNormal.setReverse(true).setBold(true),
			styleNormal.setReverse(true).setUnderline(true)
	};

	int pos;

	public char[] dataSource() {
		return lines[pos%lines.length];
	}

	public Style getStyle() {
		return styles[pos%styles.length];
	}

	public void next() {
		pos++;
	}
}