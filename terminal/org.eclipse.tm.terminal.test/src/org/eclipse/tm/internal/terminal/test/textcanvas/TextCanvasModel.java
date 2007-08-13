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

import org.eclipse.swt.widgets.Display;
import org.eclipse.tm.internal.terminal.test.textcanvas.AbstractTextCanvasModel;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot.SnapshotOutOfDateListener;

/**
 * @author Michael.Scharf@scharf-software.com
 *
 */
public class TextCanvasModel extends AbstractTextCanvasModel {
	final ITerminalTextDataSnapshot fSnapshot;
//	final ITerminalTextDataReadOnly fModel;
	int fN;
	private long t0=System.currentTimeMillis();
	int time=500;
	/**
	 * 
	 */
	public TextCanvasModel(ITerminalTextDataSnapshot snapshot) {
		fSnapshot=snapshot;
		// do a 50 ms polling
		fSnapshot.addListener(new SnapshotOutOfDateListener(){
			public void snapshotOutOfDate(ITerminalTextDataSnapshot snapshot) {
				scheduleUpdate();				
			}});
	}
	public int getHeight() {
		return fSnapshot.getHeight();
	}
	public int getWidth() {
		return fSnapshot.getWidth();
	}
	public void update() {
		// TODO schedule only on snapshot events
		if(fSnapshot.isOutOfDate()) {
			// TODO fire only if really changed
			int w=fSnapshot.getWidth();
			int h=fSnapshot.getHeight();
			fSnapshot.updateSnapshot(false);
			if(w!=fSnapshot.getWidth()|| h!=fSnapshot.getHeight())
				fireDimensionsChanges();

			int y=fSnapshot.getFirstChangedLine();
			// has any line changed?
			if(y<Integer.MAX_VALUE) {
				int height=fSnapshot.getLastChangedLine()-y+1;
				fireCellRangeChanged(0, y, fSnapshot.getWidth(), height);
			}
		}
	}
	private void scheduleUpdate() {
		long t=time-System.currentTimeMillis()-t0;
		Runnable runnable=new Runnable(){
			public void run() {
				System.out.println(System.currentTimeMillis());
				update();
				t0=System.currentTimeMillis();
			}};
		System.out.println("t="+t+" t0="+t0+ " dt="+(System.currentTimeMillis()-t0));
		if(t>0)
			Display.getDefault().timerExec((int)t,runnable);
		else
			Display.getDefault().asyncExec(runnable);
	}
}
