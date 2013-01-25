/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * {Name} (company) - description of contribution.
 *******************************************************************************/

package org.eclipse.rse.internal.services.clientserver.java;

/**
 * This class represents a field.
 */
public class FieldInfo extends AbstractCommonInfo {

	/**
	 * Constructor.
	 * @param accessFlags the access flags.
	 * @param nameIndex the name index.
	 * @param descriptorIndex the descriptor index.
	 * @param attributesCount the number of attributes.
	 * @param attributes the attributes.
	 */
	public FieldInfo(int accessFlags, int nameIndex, int descriptorIndex, int attributesCount, AbstractAttributeInfo[] attributes) {
		super(accessFlags, nameIndex, descriptorIndex, attributesCount, attributes);
	}
}
