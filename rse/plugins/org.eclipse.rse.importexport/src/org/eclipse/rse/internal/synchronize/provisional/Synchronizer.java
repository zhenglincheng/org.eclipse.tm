/*******************************************************************************
 * Copyright (c) 2008 Takuya Miyamoto and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Takuya Miyamoto - initial API and implementation
 *******************************************************************************/
package org.eclipse.rse.internal.synchronize.provisional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.rse.internal.importexport.files.UniFilePlus;
import org.eclipse.rse.internal.synchronize.ISynchronizeData;
import org.eclipse.rse.internal.synchronize.filesystem.FileSystemProvider;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;

public class Synchronizer implements ISynchronizer {
	private ISynchronizeData data;
	private ISynchronizeConnectionManager connector;

	/**
	 * TODO in the future, remoteRoot is probably needed for import or export
	 */
	private UniFilePlus remoteRoot;

	public Synchronizer(ISynchronizeData data) {
		this.data = data;
		this.connector = new SynchronizeConnectionManager();
	}

	public boolean run(ISynchronizeOperation operation) {
		IProject[] projects = null;
		List<IResource> elements = data.getElements();
		Set<IProject> projectSet = new HashSet<IProject>();
		
		for (IResource resource : elements) {
			projectSet.add(resource.getProject());
		}

		// get resources to synchronize in the type of Array.
		projects = projectSet.toArray(new IProject[projectSet.size()]);

		try {
			// if user request new synchronization, previous mapping are
			// removed.
			if (data.getSynchronizeType() == ISynchronizeOperation.SYNC_MODE_OVERRIDE_DEST || 
					data.getSynchronizeType() == ISynchronizeOperation.SYNC_MODE_OVERRIDE_SOURCE || 
					data.getSynchronizeType() == ISynchronizeOperation.SYNC_MODE_UI_REVIEW_INITIAL) {
				for (int i = 0; i < projects.length; i++) {
					connector.disconnect(projects[i]);
				}

			}
			
			// create new connection for each project
			for (int i = 0; i < projects.length; i++) {
				IProject project = projects[i];
				connector.connect(project);
				FileSystemProvider provider = (FileSystemProvider) RepositoryProvider.getProvider(project);
				String destination = data.getDestination();
				provider.setTargetLocation(data.getDestination());
				this.remoteRoot = provider.getRemoteRootFolder();
			}

			// run actual synchronize operation.
			// TODO currently, not support last synchronization date.
			operation.synchronize(data.getElements(), remoteRoot.remoteFile, null, null, data.getSynchronizeType());
		} catch (TeamException e1) {
			e1.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return true;
	}
}