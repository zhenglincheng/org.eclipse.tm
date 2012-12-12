/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * David Dykstal (IBM) - [226561] Add API markup for noextend / noimplement where needed
 * David Dykstal (IBM) - [261486][api] add noextend to interfaces that require it
 *******************************************************************************/

package org.eclipse.rse.core.references;

/**
 * A class to encapsulate the operations required of an object which
 * is merely a reference to another object, something we call a shadow.
 * Such shadows are needed to support a GUI which displays the same
 * object in multiple places. To enable that, it is necessary not to
 * use the same physical object in each UI representation as the UI
 * will only know how to update/refresh the first one it finds.
 * <p>
 * These references are not persistent. Persistent references are managed
 * by the subclass SystemPersistableReferencingObject.
 * @noimplement This interface is not intended to be implemented by clients.
 * The standard implementations are included in the framework.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRSEReferencingObject extends IRSEBaseReferencingObject {

}
