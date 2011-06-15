/*********************************************************************************
 * Copyright (c) 2008 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * David Dykstal (IBM) - initial contribution.
 * David Dykstal (IBM) - [189274] provide import and export operations for profiles
 * David Dykstal (IBM) - [216858] Need the ability to Import/Export RSE connections for sharing
 * David Dykstal (IBM) - [233876] Filters lost after restart
 * David Dykstal (IBM) - [238156] Export/Import Connection doesn't create default filters for the specified connection
 *********************************************************************************/

package org.eclipse.rse.internal.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.core.IRSECoreStatusCodes;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.filters.ISystemFilterPoolManager;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.IPropertySet;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.internal.core.RSECoreMessages;
import org.eclipse.rse.internal.core.filters.HostOwnedFilterPoolPattern;
import org.eclipse.rse.internal.persistence.dom.RSEDOMExporter;
import org.eclipse.rse.internal.persistence.dom.RSEDOMImporter;
import org.eclipse.rse.persistence.IRSEPersistenceManager;
import org.eclipse.rse.persistence.IRSEPersistenceProvider;
import org.eclipse.rse.persistence.dom.IRSEDOMConstants;
import org.eclipse.rse.persistence.dom.RSEDOM;
import org.eclipse.rse.persistence.dom.RSEDOMNode;

/**
 * An envelope holds a version of a DOM that can be used for import and export of host, filter pool, and property set
 * information. The envelope is capable of adding its contents to a profile (an import) and can also be used for generating a 
 * stream of its contents that can be used later for restore (an export).
 */
public class RSEEnvelope {
	
	// IStatus is immutable so we can do this safely
	private static IStatus INVALID_FORMAT = new Status(IStatus.ERROR, RSECorePlugin.PLUGIN_ID, IRSECoreStatusCodes.INVALID_FORMAT, RSECoreMessages.RSEEnvelope_IncorrectFormat, null);
	private static IStatus MODEL_NOT_EXPORTED = new Status(IStatus.ERROR, RSECorePlugin.PLUGIN_ID, RSECoreMessages.RSEEnvelope_ModelNotExported);
	
	private RSEDOM dom = null;
	
	/**
	 * Creates an import/export envelope.
	 */
	public RSEEnvelope() {
	}
	
	/**
	 * Replaces the contents of this envelope with the contents found on the input stream.
	 * The format of the stream is determined by the persistence provider used to write the contents of that stream.
	 * The stream is closed at the end of the operation.
	 * This operation is performed in the thread of the caller.
	 * If asynchronous operation is desired place this invocation inside a job.
	 * @param in the input stream which is read into the envelope.
	 * @param monitor a monitor used for tracking progress and cancellation.
	 * If the monitor is canceled this envelope will be empty.
	 * @throws CoreException if a problem occur reading the stream.
	 */
	public void get(InputStream in, IProgressMonitor monitor) throws CoreException {
		File envelopeFolder = getTemporaryFolder();
		IStatus status = unzip(in, envelopeFolder);
		if (status.isOK()) {
			String providerId = loadProviderId(envelopeFolder);
			IRSEPersistenceManager manager = RSECorePlugin.getThePersistenceManager();
			IRSEPersistenceProvider provider = manager.getPersistenceProvider(providerId);
			if (provider != null) {
				if (provider instanceof IRSEImportExportProvider) {
					IRSEImportExportProvider ieProvider = (IRSEImportExportProvider) provider;
					dom = ieProvider.importRSEDOM(envelopeFolder, monitor);
					if (dom == null) {
						status = INVALID_FORMAT;
					}
				} else {
					// invalid format due to bad persistence provider specified
					status = INVALID_FORMAT;
				}
			} else {
				// invalid format due to provider not installed in this workbench
				status = INVALID_FORMAT;
			}
		}
		deleteFileSystemObject(envelopeFolder);
		if (!status.isOK()) {
			throw new CoreException(status);
		}
	}
	
	/**
	 * Exports the contents of the envelope to output stream.
	 * The format of the stream is determined by the persistence provider used.
	 * The id of the persistence provider is also recorded in the stream.
	 * The stream is closed at the end of the operation.
	 * This operation is performed in the same thread as the caller.
	 * If asynchronous operation is desired place this invocation inside a job.
	 * @param out the output stream into which the contents of this envelope will be written
	 * @param provider the persistence provider used to write the contents of this envelope
	 * @param monitor a monitor used for tracking progress and cancellation. If the monitor is canceled the 
	 * receiving location is deleted.
	 * @throws CoreException containing a status describing the error, in particular this may be causes by 
	 * an IOException while preparing the contents or if the provider does not support export.
	 */
	public void put(OutputStream out, IRSEPersistenceProvider provider, IProgressMonitor monitor) throws CoreException {
		IStatus status = Status.OK_STATUS;
		if (provider instanceof IRSEImportExportProvider) {
			IRSEImportExportProvider exportProvider = (IRSEImportExportProvider) provider;
			File envelopeFolder = getTemporaryFolder();
			boolean saved = exportProvider.exportRSEDOM(envelopeFolder, dom, monitor);
			if (saved) {
				status = saveProviderId(envelopeFolder, exportProvider);
				if (status.isOK()) {
					status = zip(envelopeFolder, out);
				}
			deleteFileSystemObject(envelopeFolder);
			} else {
				status = MODEL_NOT_EXPORTED;
			}
		} else {
			status = MODEL_NOT_EXPORTED;
		}
		try {
			out.close();
		} catch (IOException e) {
			status = makeStatus(e);
		}
		if (!status.isOK()) {
			throw new CoreException(status);
		}
	}

	/**
	 * Adds a host to the envelope.
	 * If a host of the same name is already present in the envelope the new host will
	 * be renamed prior to adding it.
	 * @param host the host to be added to the envelope
	 */
	public void add(final IHost host) {
		// find and add the host-unique filter pools
		ISubSystem[] subsystems = host.getSubSystems();
		for (int i = 0; i < subsystems.length; i++) {
			ISubSystem subsystem = subsystems[i];
			ISystemFilterPool pool = subsystem.getUniqueOwningSystemFilterPool(false);
			if (pool != null) {
				add(pool);
			}
		}
		// add the host
		String type = IRSEDOMConstants.TYPE_HOST;
		String name = host.getName();
		Runnable action = new Runnable() {
			public void run() {
				RSEDOMExporter.getInstance().createNode(dom, host, true);
			}
		};
		addNode(type, name, action);
	}

	/**
	 * Adds a filter pool to the envelope.
	 * If a filter pool of the same name is already present in the envelope the new filter pool will
	 * be renamed prior to adding it.
	 * @param pool the filter pool to be added to the envelope
	 */
	public void add(final ISystemFilterPool pool) {
		// add the pool
		String type = IRSEDOMConstants.TYPE_FILTER_POOL;
		String name = pool.getName();
		Runnable action = new Runnable() {
			public void run() {
				RSEDOMExporter.getInstance().createNode(dom, pool, true);
			}
		};
		addNode(type, name, action);
	}

	/**
	 * Adds a property set to the envelope.
	 * If a property set of the same name is already present in the envelope the new property set will
	 * be renamed prior to adding it.
	 * @param propertySet the property set to be added to the envelope
	 */
	public void add(final IPropertySet propertySet) {
		// add the property set
		String type = IRSEDOMConstants.TYPE_FILTER_POOL;
		String name = propertySet.getName();
		Runnable action = new Runnable() {
			public void run() {
				RSEDOMExporter.getInstance().createNode(dom, propertySet, true);
			}
		};
		addNode(type, name, action);
	}

	/**
	 * Merges the contents of the envelope into the profile.
	 * @param profile the profile which is updated with the changes. The profile may be active or inactive.
	 */
	public void mergeWith(ISystemProfile profile) throws CoreException {
		List hostNodes = new ArrayList(10);
		List filterPoolNodes = new ArrayList(10);
		List propertySetNodes = new ArrayList(10);
		Map hostMap = new HashMap(10); // associates an original host name with a final host name
		Map filterPoolMap = new HashMap(10); // associates an original filter pool name with a final filter pool name
		String originalProfileName = getOriginalProfileName();
		if (dom != null) {
			RSEDOMNode[] children = dom.getChildren();
			for (int i = 0; i < children.length; i++) {
				RSEDOMNode child = children[i];
				String nodeType = child.getType();
				if (nodeType.equals(IRSEDOMConstants.TYPE_HOST)) {
					hostNodes.add(child);
				} else if (nodeType.equals(IRSEDOMConstants.TYPE_FILTER_POOL)) {
					filterPoolNodes.add(child);
				} else if (nodeType.equals(IRSEDOMConstants.TYPE_PROPERTY_SET)) {
					propertySetNodes.add(child);
				} else {
					throw new IllegalArgumentException("invalid dom node type"); //$NON-NLS-1$
				}
			}
			// create host rename map
			for (Iterator z = hostNodes.iterator(); z.hasNext();) {
				RSEDOMNode hostNode = (RSEDOMNode) z.next();
				String originalName = hostNode.getName();
				String finalName = getFinalHostName(profile, originalName);
				hostMap.put(originalName, finalName);
			}
			// create filter pool rename map
			for (Iterator z = filterPoolNodes.iterator(); z.hasNext();) {
				RSEDOMNode filterPoolNode = (RSEDOMNode) z.next();
				String originalName = filterPoolNode.getName();
				String subsystemConfigurationId = filterPoolNode.getAttribute(IRSEDOMConstants.ATTRIBUTE_ID).getValue();
				String finalName = getFinalFilterPoolName(profile, originalName, subsystemConfigurationId, hostMap);
				filterPoolMap.put(originalName, finalName);
			}
			// merge the hosts
			for (Iterator z = hostNodes.iterator(); z.hasNext();) {
				RSEDOMNode hostNode = (RSEDOMNode) z.next();
				mergeHost(profile, hostNode, hostMap, filterPoolMap, originalProfileName);
			}
			// merge the filter pools
			for (Iterator z = filterPoolNodes.iterator(); z.hasNext();) {
				RSEDOMNode filterPoolNode = (RSEDOMNode) z.next();
				mergeFilterPool(profile, filterPoolNode, filterPoolMap);
			}
			// TODO create the property sets
		}
	}
	
	/**
	 * Derive a host name from the original host node. Tries the original name and then a sequence of
	 * names derived from the original name. The first non-conflicting name is chosen.
	 * @param profile the profile in which to look for conflicting names
	 * @param originalName the original host name
	 * @return a name derived from the original name that does not yet exist in the profile
	 */
	private String getFinalHostName(ISystemProfile profile, String originalName) {
		int n = 0;
		ISystemRegistry registry = RSECorePlugin.getTheSystemRegistry();
		String finalName = originalName;
		while (registry.getHost(profile, finalName) != null) {
			n++;
			finalName = originalName + "-" + n; //$NON-NLS-1$
		}
		return finalName;
	}
	
	/**
	 * Derive a new filter pool name given the original name and its subsystem configuration id.
	 * Connection private (host owned) filter pool names are of the form: CN-[host-name]-[configuration-id].
	 * Subsystem default filter pool names are of the form: [profile-name]:[configuration-id].
	 * User defined filter pool names are exactly what the user specifies.
	 * The resulting filter pool name must not already exist in the profile.
	 * @param profile the profile in which to look for conflicting filter pool names
	 * @param originalName the original filter pool profile name
	 * @param subsystemConfigurationId the subsystem configuration id of this profile
	 * @param hostMap the map of renamed host nodes. Needed to rename connection private filter pools.
	 * @return a filter pool name derived from the original name that does not yet exist in the profile.
	 */
	private String getFinalFilterPoolName(ISystemProfile profile, String originalName, String subsystemConfigurationId, Map hostMap) {
		String finalName = null;
		HostOwnedFilterPoolPattern pattern = new HostOwnedFilterPoolPattern(subsystemConfigurationId);
		String originalHostName = pattern.extract(originalName);
		if (originalHostName != null) {
			// connection private filter pools are renamed according to their host
			String finalHostName = (String) hostMap.get(originalHostName);
			if (finalHostName != null) {
				finalName = pattern.make(finalHostName);
			}
		}
		if (finalName == null) {
			// all other filter pools are renamed if necessary so as to not collide with existing ones
			int n = 0;
			finalName = originalName;
			ISystemFilterPool filterPool = getFilterPool(profile, finalName, subsystemConfigurationId);
			while (filterPool != null) {
				n++;
				finalName = originalName + "-" + n; //$NON-NLS-1$
				filterPool = getFilterPool(profile, finalName, subsystemConfigurationId);
			}
		}
		return finalName;
	}
	
	/**
	 * Creates a host and its contained subsystems and filter pool references.
	 * @param profile the profile in which to create the new host.
	 * @param hostNode the host node from which to base the host definition 
	 * @param hostMap the rename map for host nodes
	 * @param filterPoolMap the rename map for filter pools, used when fixing filter pool references
	 * @param originalProfileName the original profile name, used when fixing filter pool references
	 * @return a host object created in the profile from the hostNode
	 */
	private IHost mergeHost(ISystemProfile profile, RSEDOMNode hostNode, Map hostMap, Map filterPoolMap, String originalProfileName) {
		String originalHostName = hostNode.getName();
		String finalHostName = (String) hostMap.get(originalHostName);
		hostNode.setName(finalHostName);
		fixSubsystems(hostNode, profile, filterPoolMap, originalProfileName);
		RSEDOMImporter importer = RSEDOMImporter.getInstance();
		IHost host = importer.restoreHost(profile, hostNode);
		return host;
	}
	
	/**
	 * Examines the subsystems in this host node.
	 * Removes those that have no equivalent subsystem configuration installed in this workbench.
	 * Logs this condition.
	 * Examines the filter pool references for each found subsystem and updates them if necessary.
	 * @param hostNode The host node containing the subsystem references
	 * @param profile The profile into which the host is being imported
	 * @param filterPoolMap The map that accounts for filter pool renames. Used to fix up filter pool references.
	 * @param originalProfileName the name of the profile that this host was created in. Used to fix up filter pool references.
	 * @see #fixFilterPoolReferences(RSEDOMNode, ISystemProfile, Map)
	 */
	private void fixSubsystems(RSEDOMNode hostNode, ISystemProfile profile, Map filterPoolMap, String originalProfileName) {
		ISystemRegistry registry = RSECorePlugin.getTheSystemRegistry();
		RSEDOMNode connectorServiceNodes[] = hostNode.getChildren(IRSEDOMConstants.TYPE_CONNECTOR_SERVICE);
		for (int i = 0; i < connectorServiceNodes.length; i++) {
			RSEDOMNode connectorServiceNode = connectorServiceNodes[i];
			RSEDOMNode subsystemNodes[] = connectorServiceNode.getChildren(IRSEDOMConstants.TYPE_SUBSYSTEM);
			for (int j = 0; j < subsystemNodes.length; j++) {
				RSEDOMNode subsystemNode = subsystemNodes[j];
				String subsystemConfigurationId = getSubsystemConfigurationId(subsystemNode);
				String subsystemName = subsystemNode.getName();
				ISubSystemConfiguration subsystemConfiguration = registry.getSubSystemConfiguration(subsystemConfigurationId);
				if (subsystemConfiguration == null) {
					// remove this subsystemNode from this connector service
					connectorServiceNode.removeChild(subsystemNode);
					// log that this subsystem cannot be restored
					String template = "The subsystem {0} cannot be imported. The subsystem configuration with identifier {1} is not installed."; //$NON-NLS-1$
					String message = NLS.bind(template, subsystemName, subsystemConfigurationId);
					IStatus status = new Status(IStatus.INFO, RSECorePlugin.PLUGIN_ID, message);
					RSECorePlugin.getDefault().getLog().log(status);
				} else {
					subsystemConfiguration.getFilterPoolManager(profile, true);
					fixFilterPoolReferences(subsystemNode, profile, filterPoolMap, originalProfileName);
				}
			}
		}
	}
	
	/**
	 * Examines the filter pool references in this subsystem node. If any are found that are in the originating
	 * profile these are updated to the profile into which we are importing.
	 * @param hostNode The host node containing the filter pool references
	 * @param profile The profile into which the host is being imported
	 * @param filterPoolMap The map that accounts for filter pool renames. Used to fix up filter pool references.
	 */
	private void fixFilterPoolReferences(RSEDOMNode subsystemNode, ISystemProfile profile, Map filterPoolMap, String originalProfileName) {
		String subsystemConfigurationId = getSubsystemConfigurationId(subsystemNode);
		RSEDOMNode filterPoolReferenceNodes[] = subsystemNode.getChildren(IRSEDOMConstants.TYPE_FILTER_POOL_REFERENCE);
		if (originalProfileName != null) {
			String newProfileName = profile.getName();
			for (int i = 0; i < filterPoolReferenceNodes.length; i++) {
				RSEDOMNode filterPoolReferenceNode = filterPoolReferenceNodes[i];
				String parts[] = parseFilterPoolReferenceName(filterPoolReferenceNode);
				if (parts.length == 2) {
					String originalFilterPoolName = parts[1];
					String finalFilterPoolName = null;
					if (isDefaultSubsystemFilterPoolName(originalFilterPoolName, originalProfileName, subsystemConfigurationId)) {
						finalFilterPoolName = makeDefaultSubsystemFilterPoolName(newProfileName, subsystemConfigurationId);
					} else {
						finalFilterPoolName = (String) filterPoolMap.get(originalFilterPoolName);
						if (finalFilterPoolName == null) {
							finalFilterPoolName = originalFilterPoolName;
						}
					}
					String qualifiedFilterPoolName = makeFilterPoolReferenceName(newProfileName, finalFilterPoolName);
					filterPoolReferenceNode.setName(qualifiedFilterPoolName);
				}
			}
		}
	}
	
	/**
	 * Examines the dom and, by heuristic, attempts to determine the original profile name.
	 * This name can be used to re-parent any filter pool references as they are merged with 
	 * the new profile.
	 * @return a non-null profile name.
	 */
	private String getOriginalProfileName() {
		String originalProfileName = getOriginalProfileName(dom);
		if (originalProfileName == null) {
			originalProfileName = "DOM"; //$NON-NLS-1$
		}
		return originalProfileName;
	}
	
	/**
	 * This is a heuristic.
	 * Traverse the node and its children looking for filter pool references.
	 * If a connection private filter pool reference is found then assume its qualifying profile name is the original profile.
	 * @param node the node to begin the tree traversal.
	 * @return the first profile name found that meets the criteria or null.
	 */
	private String getOriginalProfileName(RSEDOMNode node) {
		String result = null;
		if (isFilterPoolReference(node)) {
			if (isConnectionPrivateFilterPoolReference(node)) {
				String names[] = parseFilterPoolReferenceName(node);
				result = names[0];
			}
		}
		if (result == null) {
			RSEDOMNode children[] = node.getChildren();
			for (int i = 0; i < children.length; i++) {
				RSEDOMNode child = children[i];
				result = getOriginalProfileName(child);
				if (result != null) break;
			}
		}
		return result;
	}
	
	/**
	 * Extracts and returns the subsystem configuration id from a subsystem node in a host definition
	 * @param subsystemNode a subsystem node
	 * @return the subsystem configuration id of that subsystem node
	 */
	private String getSubsystemConfigurationId(RSEDOMNode subsystemNode) {
		String subsystemConfigurationId = subsystemNode.getAttribute(IRSEDOMConstants.ATTRIBUTE_TYPE).getValue();
		return subsystemConfigurationId;
	}
	
	/**
	 * Tests if a node is a filter pool reference node.
	 * @param node a DOM node
	 * @return true if and only if the node is a filter pool reference.
	 */
	private boolean isFilterPoolReference(RSEDOMNode node) {
		boolean result = node.getType().equals(IRSEDOMConstants.TYPE_FILTER_POOL_REFERENCE);
		return result;
	}
	
	/**
	 * Tests if a filter pool name matches the pattern for a default subsystem filter pool name
	 * @param filterPoolName the name to test
	 * @param profileName the profile the name should be a part of
	 * @param subsystemConfigurationId the subsystem configuration id of the subsystem we are testing
	 * @return true if the filter pool name matches the pattern of a default subsystem filter pool name
	 */
	private boolean isDefaultSubsystemFilterPoolName(String filterPoolName, String profileName, String subsystemConfigurationId) {
		String defaultSubsystemFilterPoolName = makeDefaultSubsystemFilterPoolName(profileName, subsystemConfigurationId);
		boolean result = filterPoolName.equals(defaultSubsystemFilterPoolName);
		return result;
	}
	
	private boolean isConnectionPrivateFilterPoolReference(RSEDOMNode filterPoolReferenceNode) {
		String parts[] = parseFilterPoolReferenceName(filterPoolReferenceNode);
		boolean result = false;
		if (parts.length == 2) {
			String filterPoolName = parts[1];
			result = filterPoolName.startsWith("CN-"); //$NON-NLS-1$
		}
		return result;
	}
	
	private String[] parseFilterPoolReferenceName(RSEDOMNode filterPoolReferenceNode) {
		String name = filterPoolReferenceNode.getName();
		String result[] = name.split("___"); //$NON-NLS-1$
		return result;
	}
	
	private String makeFilterPoolReferenceName(String profileName, String filterPoolName) {
		String result = profileName + "___" + filterPoolName; //$NON-NLS-1$
		return result;
	}
	
	private String makeDefaultSubsystemFilterPoolName(String profileName, String subsystemConfigurationId) {
		String defaultSubsystemFilterPoolName = profileName + ":" + subsystemConfigurationId; //$NON-NLS-1$
		return defaultSubsystemFilterPoolName;
	}
	
	private ISystemFilterPool mergeFilterPool(ISystemProfile profile, RSEDOMNode filterPoolNode, Map filterPoolMap) {
		String originalName = filterPoolNode.getName();
		String subsystemConfigurationId = filterPoolNode.getAttribute(IRSEDOMConstants.ATTRIBUTE_ID).getValue();
		String finalName = (String) filterPoolMap.get(originalName);
		filterPoolNode.setName(finalName);
		RSEDOMImporter importer = RSEDOMImporter.getInstance();
		ISystemFilterPool filterPool = importer.restoreFilterPool(profile, filterPoolNode);
		HostOwnedFilterPoolPattern pattern = new HostOwnedFilterPoolPattern(subsystemConfigurationId);
		String hostName = pattern.extract(finalName);
		if (hostName != null) {
			filterPool.setOwningParentName(hostName);
		}
		return filterPool;
	}
	
	private ISystemFilterPool getFilterPool(ISystemProfile profile, String filterPoolName, String subsystemConfigurationId) {
		ISystemRegistry registry = RSECorePlugin.getTheSystemRegistry();
		ISubSystemConfiguration subsystemConfiguration = registry.getSubSystemConfiguration(subsystemConfigurationId);
		ISystemFilterPoolManager manager = subsystemConfiguration.getFilterPoolManager(profile);
		ISystemFilterPool filterPool = manager.getSystemFilterPool(filterPoolName);
		return filterPool;
	}
	
	private IStatus saveProviderId(File parent, IRSEImportExportProvider provider) {
		IStatus status = Status.OK_STATUS;
		String providerId = provider.getId();
		File idFile = new File(parent, "provider.id"); //$NON-NLS-1$
		try {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(idFile));
			out.write(providerId);
			out.close();
		} catch (IOException e) {
			status = makeStatus(e);
		}
		return status;
	}
	
	private String loadProviderId(File parent) throws CoreException {
		String providerId = null;
		File idFile = new File(parent, "provider.id"); //$NON-NLS-1$
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(idFile)));
			providerId = in.readLine();
			in.close();
		} catch (IOException e) {
			IStatus status = INVALID_FORMAT;
			throw new CoreException(status);
		}
		return providerId;
	}
	
	private void addNode(String type, String name, Runnable action) {
		ensureDOM();
		RSEDOMNode existingNode = dom.getChild(type, name);
		if (existingNode != null) {
			dom.removeChild(existingNode);
		}
		action.run();
	}

	private void ensureDOM() {
		if (dom == null) {
			dom = new RSEDOM("dom"); //$NON-NLS-1$
		}
	}
	
	private String generateName(List usedNames) {
		String prefix = "env_"; //$NON-NLS-1$
		int n = 0;
		String name = prefix + n;
		while (usedNames.contains(name)) {
			n += 1;
			name = prefix + n;
		}
		return name;
	}
	
	private IStatus zip(File source, OutputStream target) {
		IStatus status = Status.OK_STATUS;
		try {
			ZipOutputStream out = new ZipOutputStream(target);
			zipEntry(out, source, ""); //$NON-NLS-1$
			out.close();
		} catch (IOException e) {
			status = makeStatus(e);
		}
		return status;
	}
	
	private void zipEntry(ZipOutputStream out, File file, String entryName) {
		if (file.isDirectory()) {
			zipDirectoryEntry(out, file, entryName);
		} else {
			zipFileEntry(out, file, entryName);
		}
	}

	private void zipDirectoryEntry(ZipOutputStream out, File file, String entryName) {
		String fileName = file.getName();
		if (!(fileName.equals(".") || fileName.equals(".."))) { //$NON-NLS-1$ //$NON-NLS-2$
			if (entryName.length() > 0) {
				try {
					ZipEntry entry = new ZipEntry(entryName + "/"); //$NON-NLS-1$
					out.putNextEntry(entry);
					out.closeEntry();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				File child = files[i];
				String childName = child.getName();
				String childEntryName = entryName + "/" + childName; //$NON-NLS-1$
				zipEntry(out, child, childEntryName);
			}
		}
	}
	
	private void zipFileEntry(ZipOutputStream out, File file, String entryName) {
		try {
			ZipEntry entry = new ZipEntry(entryName);
			out.putNextEntry(entry);
			byte[] buffer = new byte[4096];
			FileInputStream in = new FileInputStream(file);
			for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
				out.write(buffer, 0, n);
			}
			in.close();
			out.closeEntry();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private IStatus unzip(InputStream in, File root) {
		IStatus status = Status.OK_STATUS;
		try {
			ZipInputStream inZip = new ZipInputStream(in);
			ZipEntry entry = inZip.getNextEntry();
			while (entry != null) {
				String name = entry.getName();
				File target = new File(root, name);
				if (entry.isDirectory()) {
					target.mkdir();
				} else {
					byte[] buffer = new byte[4096];
					FileOutputStream out = new FileOutputStream(target);
					for (int n = inZip.read(buffer); n >= 0; n = inZip.read(buffer)) {
						out.write(buffer, 0, n);
					}
					out.close();
				}
				entry = inZip.getNextEntry();
			}
		} catch (FileNotFoundException e) {
			status = makeStatus(e);
		} catch (ZipException e) {
			RSECorePlugin.getDefault().getLogger().logError(RSECoreMessages.RSEEnvelope_IncorrectFormat, e);
			status = INVALID_FORMAT;
		} catch (IOException e) {
			status = makeStatus(e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				status = makeStatus(e);
			}
		}
		return status;
	}
	
	private IStatus deleteFileSystemObject(File file) {
		IStatus status = Status.OK_STATUS;
		String fileName = file.getName();
		if (!(fileName.equals(".") || fileName.equals(".."))) { //$NON-NLS-1$ //$NON-NLS-2$
			if (file.exists()) {
				if (file.isDirectory()) {
					File[] files = file.listFiles();
					for (int i = 0; i < files.length; i++) {
						File child = files[i];
						deleteFileSystemObject(child);
					}
				}
				file.delete();
			}
		}
		return status;
	}
	
	private IStatus makeStatus(Exception e) {
		IStatus status = new Status(IStatus.ERROR, RSECorePlugin.PLUGIN_ID, "Unexpected exception", e); //$NON-NLS-1$
		return status;
	}

	/**
	 * @return a file handle to a temporary directory
	 */
	private File getTemporaryFolder() {
		IPath stateLocation = RSECorePlugin.getDefault().getStateLocation();
		File stateFolder = new File(stateLocation.toOSString());
		File envelopesFolder = new File(stateFolder, "envelopes"); //$NON-NLS-1$
		envelopesFolder.mkdir();
		List envelopeNames = Arrays.asList(envelopesFolder.list());
		String envelopeName = generateName(envelopeNames);
		File envelopeFolder = new File(envelopesFolder, envelopeName);
		envelopeFolder.mkdir();
		return envelopeFolder;
	}
	
}
