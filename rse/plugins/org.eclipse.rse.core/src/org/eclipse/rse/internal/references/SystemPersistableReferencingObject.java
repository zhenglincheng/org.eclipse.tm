/********************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation. All rights reserved.
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
 * David Dykstal (IBM) - [224671] [api] org.eclipse.rse.core API leaks non-API types
 ********************************************************************************/

package org.eclipse.rse.internal.references;

import org.eclipse.rse.core.references.IRSEBasePersistableReferenceManager;
import org.eclipse.rse.core.references.IRSEBasePersistableReferencedObject;
import org.eclipse.rse.core.references.IRSEPersistableReferencingObject;
import org.eclipse.rse.core.references.SystemReferencingObject;

/**
 * This class represents an object that references another object in the model.
 * The reference is persistable.
 * <p>
 * @see org.eclipse.rse.core.references.IRSEPersistableReferencingObject
 */
// DWD Change this name to SystemPersistableReference? Ditto for the interface.
public abstract class SystemPersistableReferencingObject extends SystemReferencingObject implements IRSEPersistableReferencingObject {

	protected String referencedObjectName = null;
	protected IRSEBasePersistableReferenceManager _referenceManager;

	/**
	 * Create a new referencing object.
	 */
	protected SystemPersistableReferencingObject() {
		super();
	}

	/**
	 * Set the persistable referenced object name
	 */
	public void setReferencedObjectName(String newReferencedObjectName) {
		referencedObjectName = newReferencedObjectName;
	}

	/**
	 * Set the in-memory reference to the master object.
	 * This implementation also extracts that master object's name and calls
	 * setReferencedObjectName as part of this method call.
	 * @see org.eclipse.rse.core.references.IRSEBasePersistableReferencingObject#setReferencedObject(IRSEBasePersistableReferencedObject)
	 */
	public void setReferencedObject(IRSEBasePersistableReferencedObject obj) {
		getHelper().setReferencedObject(obj);
		setReferencedObjectName(obj.getReferenceName());
	}

	/**
	 * Get the persistable referenced object name.
	 */
	public String getReferencedObjectName() {
		return referencedObjectName;
	}

	/**
	 * @return The reference manager for this reference. 
	 */
	public IRSEBasePersistableReferenceManager getParentReferenceManager() {
		return _referenceManager;
	}

	/**
	 * Sets the reference manager for this reference. Must be done when this reference is created.
	 */
	public void setParentReferenceManager(IRSEBasePersistableReferenceManager newParentReferenceManager) {
		_referenceManager = newParentReferenceManager;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (referencedObjectName: "); //$NON-NLS-1$
		result.append(referencedObjectName);
		result.append(')');
		return result.toString();
	}

}