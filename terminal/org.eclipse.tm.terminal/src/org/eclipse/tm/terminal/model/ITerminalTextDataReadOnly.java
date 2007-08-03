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


public interface ITerminalTextDataReadOnly {

	/**
	 * @return the width of the terminal
	 */
	int getWidth();

	/**
	 * @return the height of the terminal
	 */
	int getHeight();

	LineSegment[] getLineSegments(int x, int y, int len);

	/**
	 * @param x x must be >=0 and < width
	 * @param y y must be >=0 and < height
	 * @return the character at x,y
	 */
	char getChar(int x, int y);

	/**
	 * @param x x must be >=0 and < width
	 * @param y y must be >=0 and < height
	 * @return style at x,y or null
	 */
	Style getStyle(int x, int y);

	/**
	 * Creates a new instance of {@link ITerminalTextDataSnapshot} that
	 * can be used to track changes. Make sure to call {@link ITerminalTextDataSnapshot#detach()}
	 * if you don't need the snapshots anymore.
	 * @return a new instance of {@link ITerminalTextDataSnapshot} that "listens" to changes of
	 * <code>this</code>.
	 */
	public ITerminalTextDataSnapshot makeSnapshot();

}