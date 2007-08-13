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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tm.internal.terminal.test.textcanvas.ITextCanvasModel;
import org.eclipse.tm.internal.terminal.test.textcanvas.PollingTextCanvasModel;
import org.eclipse.tm.internal.terminal.test.textcanvas.TextCanvas;
import org.eclipse.tm.internal.terminal.test.textcanvas.TextLineRenderer;
import org.eclipse.tm.terminal.model.ITerminalTextData;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.TerminalTextDataFactory;

/**
 * adjust columns when table gets resized...
 * 
 */
public class TerminalTextUITest {
	static TextCanvas tn;
	static ITextCanvasModel fgModel;
	static ITerminalTextData fTerminalModel;
	static Label fStatusLabel;
	static volatile int fHeight;
	static volatile int fWidth;
	static DataReader fDataReader;
	static List fDataReaders=new ArrayList();
	private static Text heightText;
	static class Status implements IStatus {
		public void setStatus(final String s) {
			if(!fStatusLabel.isDisposed())
				Display.getDefault().asyncExec(new Runnable(){
					public void run() {
						if(!fStatusLabel.isDisposed())
							fStatusLabel.setText(s);
					}});
		}
		
	}
	public static void main(String[] args) {
		fTerminalModel=TerminalTextDataFactory.makeTerminalTextData();
		
		
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout());
		Composite composite=new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		layout.fill = false;

		composite.setLayout(layout);
		
		addLabel(composite, "maxHeight:");
		final Text maxHeightText=new Text(composite,SWT.BORDER);
		setLayoutData(maxHeightText,30);
		maxHeightText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				synchronized (fTerminalModel) {
					int height=textToInt(maxHeightText);
					if(height<1)
						return;
					if(fTerminalModel.getHeight()>height) {
						fTerminalModel.scroll(0, fTerminalModel.getHeight(), height-fTerminalModel.getHeight());
						fTerminalModel.setDimensions(height,fTerminalModel.getWidth());
						heightText.setText(height+"");
					}
					fTerminalModel.setMaxHeight(height);
				}
			}
		});
		
		addLabel(composite,"heigth:");
		heightText=new Text(composite,SWT.BORDER);
		setLayoutData(heightText,30);
		heightText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				synchronized (fTerminalModel) {
					int height=textToInt(heightText);
					if(height<1)
						return;
					fTerminalModel.setMaxHeight(height);
					maxHeightText.setText(""+height);
					fTerminalModel.setDimensions(height,fTerminalModel.getWidth());
				}
			}
		});
		
		addLabel(composite,"width:");
		final Text widthText=new Text(composite,SWT.BORDER);
		setLayoutData(widthText,30);
		widthText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				synchronized (fTerminalModel) {
					int width=textToInt(widthText);
					if(width>1)
						fTerminalModel.setDimensions(fTerminalModel.getHeight(), width);
				}
			}
		});
		
		addLabel(composite,"throttle:");
		final Text throttleText=new Text(composite,SWT.BORDER);
		setLayoutData(throttleText,30);
		throttleText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				synchronized (fTerminalModel) {
					int throttle=textToInt(throttleText);
					setThrottleForAll(throttle);
				}
			}});
		fHeight=24;
		fWidth=80;
		widthText.setText(fWidth+"");
		maxHeightText.setText(fHeight+"");
		heightText.setText(fHeight+"");
		fTerminalModel.setDimensions(fHeight, fWidth);
		fTerminalModel.setMaxHeight(fHeight);
		ITerminalTextDataSnapshot snapshot=fTerminalModel.makeSnapshot();
//		fgModel=new TextCanvasModel(snapshot);
		fgModel=new PollingTextCanvasModel(snapshot);
		IStatus status=new Status();
		DataReader reader=new DataReader("Line Count",fTerminalModel,new LineCountingDataSource(),status);
		addDataReader(composite, reader);
		reader=new DataReader("Fast",fTerminalModel,new FastDataSource(),status);
		addDataReader(composite, reader);
		reader=new DataReader("Random",fTerminalModel,new RandomDataSource(),status);
		addDataReader(composite, reader);
		for (int i = 0; i < args.length; i++) {
			File file=new File(args[i]);
			reader=new DataReader(file.getName(),fTerminalModel,new FileDataSource(args[i]),status);
			addDataReader(composite, reader);
		}
		final Button stopAllButton=new Button(composite,SWT.CHECK);
		stopAllButton.setText("Stop ALL");
		stopAllButton.addSelectionListener(new SelectionAdapter(){

			public void widgetSelected(SelectionEvent e) {
				boolean stop=stopAllButton.getSelection();
				for (Iterator iterator = fDataReaders.iterator(); iterator.hasNext();) {
					DataReader reader = (DataReader) iterator.next();
					reader.setStop(stop);

			}}});
		stopAllButton.setSelection(reader.isStart());
		
		tn=new TextCanvas(shell,fgModel, SWT.NONE);
		tn.setCellRenderer(new TextLineRenderer(tn,snapshot));
		tn.setLayoutData(new GridData(GridData.FILL_BOTH));

		fStatusLabel=new Label(shell,SWT.NONE);
		fStatusLabel.setLayoutData(new GridData(250,15));
		throttleText.setText("100");
		setThrottleForAll(100);

		if(args.length==0)
			addLabel(composite, "[Files can be added via commandline]");
		shell.setSize(600,300);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
	private static void addLabel(Composite composite,String message) {
		Label label;
		label=new Label(composite, SWT.NONE);
		label.setText(message);
	}
	private static void addDataReader(Composite composite, final DataReader reader) {
		fDataReaders.add(reader);
		final Button button=new Button(composite,SWT.CHECK);
		button.setText(reader.getName());
		button.addSelectionListener(new SelectionAdapter(){

			public void widgetSelected(SelectionEvent e) {
				reader.setStart(button.getSelection());
			}});
		button.setSelection(reader.isStart());
		
	}
	static private void setThrottleForAll(int throttle) {
		for (Iterator iterator = fDataReaders.iterator(); iterator.hasNext();) {
			DataReader reader = (DataReader) iterator.next();
			reader.setThrottleTime(throttle);
		}
	}
	static void setLayoutData(Control c,int width) {
		c.setLayoutData(new RowData(width,-1));
	}
	static int textToInt(Text text) {
		try {
			return Integer.valueOf(text.getText()).intValue();
		} catch (Exception ex) {
			return 0;
		}
	}
}

