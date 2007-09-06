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
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * A cell oriented Canvas. Maintains a list of "cells".
 * It can either be vartically or horizontally scrolled.
 * The CellRenderer is responsible for painting the cell.
 */
public class TextCanvas extends GridCanvas {
	protected final ITextCanvasModel fCellCanvasModel;
	/** Renders the cells */
	private ILinelRenderer fCellRenderer;
	private boolean fAutoRevealCursor;
	/**
	 * Create a new CellCanvas with the given SWT stylebits.
	 * (SWT.H_SCROLL and SWT.V_SCROLL are automtically added).
	 */
	public TextCanvas(Composite parent, ITextCanvasModel model, int style) {
		super(parent, style | SWT.H_SCROLL | SWT.V_SCROLL);
		fCellCanvasModel=model;
		fCellCanvasModel.addCellCanvasModelListener(new ITextCanvasModelListener(){
			public void cellSizeChanged() {
				setCellWidth(fCellRenderer.getCellWidth());
				setCellHeight(fCellRenderer.getCellHeight());

				calculateGrid();
				
			}
			public void rangeChanged(int col, int line, int width, int height) {
				repaintRange(col,line,width,height);
				
			}
			public void dimesnionsChanged(int cols, int rows) {
				calculateGrid();
			}
		});
		addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				calculateGrid();
			}
		});
		addFocusListener(new FocusListener(){

			public void focusGained(FocusEvent e) {
				fCellCanvasModel.setCursorEnabled(true);
			}
			public void focusLost(FocusEvent e) {
				fCellCanvasModel.setCursorEnabled(false);
			}});
		addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
//				switch(e.keyCode) {
//					case SWT.ARROW_DOWN:
//						selectValidIndex(getFocusCell()+getCols());
//						break;
//					case SWT.ARROW_UP:
//						selectValidIndex(getFocusCell()-getCols());
//						break;
//					case SWT.ARROW_LEFT:
//						selectValidIndex(getFocusCell()-1);
//						break;
//					case SWT.ARROW_RIGHT:
//						selectValidIndex(getFocusCell()+1);
//						break;
//					default:
//						break;
//				}
			}
			public void keyReleased(KeyEvent e) {
			}
		});
		addMouseListener(new MouseListener(){
			public void mouseDoubleClick(MouseEvent e) {
			}
			public void mouseDown(MouseEvent e) {
				if(e.button==1) { // left button
				}
			}
			public void mouseUp(MouseEvent e) {				
			}
		});
	}
	public void setCellRenderer(ILinelRenderer cellRenderer) {
		fCellRenderer = cellRenderer;
		setCellWidth(fCellRenderer.getCellWidth());
		setCellHeight(fCellRenderer.getCellHeight());
	}
	public ILinelRenderer getCellRenderer() {
		return fCellRenderer;
	}
	private void calculateGrid() {
		setVirtualExtend(getCols()*getCellWidth(),getRows()*getCellHeight());
		// scroll to end
		if(fAutoRevealCursor)
			setVirtualOrigin(0,getRows()*getCellHeight());
		// make sure the scroll area is correct:
		scrollY(getVerticalBar());
		scrollX(getHorizontalBar());

		updateViewRectangle();
		getParent().layout();
		redraw();
	}
	/**
	 * 
	 * @return true if the cursor should be shown on output....
	 */
	public boolean isAutoRevealCursor() {
		return fAutoRevealCursor;
	}
	/**
	 * If set then if the size changes  
	 * @param autoRevealCursor 
	 */
	public void setAutoRevealCursor(boolean autoRevealCursor) {
		fAutoRevealCursor=autoRevealCursor;
	}
	protected void repaintRange(int col, int line, int width, int height) {
		Point origin=cellToOriginOnScreen(col,line);
		Rectangle r=new Rectangle(origin.x,origin.y,width*getCellWidth(),height*getCellHeight());
		repaint(r);
	}
	protected void drawLine(GC gc, int line, int x, int y, int colFirst, int colLast) {
		fCellRenderer.drawLine(fCellCanvasModel, gc,line,x,y,colFirst, colLast);
		
	}
	protected void visibleCellRectangleChanged(int x, int y, int width, int height) {
		fCellRenderer.setVisibleRectangle(y,x,height,width);
		fCellCanvasModel.update();
		update();
	}
	protected int getCols() {
		return fCellCanvasModel.getWidth();
	}
	protected int getRows() {
		return fCellCanvasModel.getHeight();
	}
}

