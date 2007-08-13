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
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;

/**
 * @author Michael.Scharf@scharf-software.com
 *
 */
public class PollingTextCanvasModel extends AbstractTextCanvasModel {
	final ITerminalTextDataSnapshot fSnapshot;
	int fPollInterval=50;
	/**
	 * 
	 */
	public PollingTextCanvasModel(ITerminalTextDataSnapshot snapshot) {
		fSnapshot=snapshot;
		Display.getDefault().timerExec(fPollInterval,new Runnable(){
			public void run() {
				update();
				Display.getDefault().timerExec(fPollInterval,this);
			}});
	}
	void setUpdateTime(int t) {
		fPollInterval=t;
	}
	public void update() {
		// do the poll....
		if(fSnapshot.isOutOfDate()) {
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
	public int getHeight() {
		return fSnapshot.getHeight();
	}
	public int getWidth() {
		return fSnapshot.getWidth();
	}
}
