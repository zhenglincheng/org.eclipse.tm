/********************************************************************************
 * Copyright (c) 2006 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * {Name} (company) - description of contribution.
 ********************************************************************************/

package org.eclipse.rse.ui.validators;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

/**
 * A class that only allows keys representing numeric values to be entered. It can be turned off if necessary.
 */
public class SystemNumericVerifyListener implements VerifyListener {
	
	private boolean isOff;
	
	/**
	 * Constructor. The verifier is on by default.
	 */
	public SystemNumericVerifyListener() {
		isOff = false;
	}

	/**
	 * Verifies the event text if it is not turned off.
	 * @see org.eclipse.swt.events.VerifyListener#verifyText(org.eclipse.swt.events.VerifyEvent)
	 */
	public void verifyText(VerifyEvent e) {
		
		if (!isOff) {
			String text = e.text;
			boolean doit = true;

			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);

				if (!Character.isDigit(c)) {
					doit = false;
					break;
				}
			}

			e.doit = doit;
		}
	}
	
	/**
	 * Turns the verifier on or off.
	 * @param isOff <code>true</code> to turn the verifier off, <code>false</code> to turn it on.
	 */
	public void setOff(boolean isOff) {
		this.isOff = isOff;
	}
	
	/**
	 * Returns whether the verifier is on or off.
	 * @return <code>true</code> if the verifier is off, <code>false</code> if it is on.
	 */
	public boolean isOff() {
		return isOff;
	}
}