/********************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [168975] Move RSE Events API to Core
 * Martin Oberhuber (Wind River) - [184095] Replace systemTypeName by IRSESystemType
 * Martin Oberhuber (Wind River) - [177523] Unify singleton getter methods
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Martin Oberhuber (Wind River) - [189123] Prepare ISystemRegistry for move into non-UI
 * Martin Oberhuber (Wind River) - [175680] Deprecate obsolete ISystemRegistry methods
 ********************************************************************************/

package org.eclipse.rse.core.model;

import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.events.ISystemModelChangeEvent;
import org.eclipse.rse.core.events.ISystemModelChangeListener;
import org.eclipse.rse.core.events.ISystemPreferenceChangeEvent;
import org.eclipse.rse.core.events.ISystemPreferenceChangeListener;
import org.eclipse.rse.core.events.ISystemRemoteChangeEvent;
import org.eclipse.rse.core.events.ISystemRemoteChangeListener;
import org.eclipse.rse.core.events.ISystemResourceChangeEvent;
import org.eclipse.rse.core.events.ISystemResourceChangeListener;
import org.eclipse.rse.core.filters.ISystemFilterStartHere;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.core.subsystems.ISubSystemConfigurationProxy;
import org.eclipse.rse.internal.core.RSECoreRegistry;

/**
 * Registry or front door for all remote system connections.
 * There is a singleton of the class implementation of this interface.
 * To get it, call the {@link org.eclipse.rse.core.RSECorePlugin#getTheSystemRegistry()}.
 * <p>
 * The idea here is that connections are grouped by system profile. At any 
 * time, there is a user-specified number of profiles "active" and connections
 * from each active profile are worked with.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */
public interface ISystemRegistry extends ISchedulingRule, IAdaptable {

	/**
	 * Get the SystemFilterStartHere singleton instance. 
	 * @return the SystemFilterStartHere singleton instance.
	 */
	public ISystemFilterStartHere getSystemFilterStartHere();

	// ----------------------------
	// SUBSYSTEM FACTORY METHODS...
	// ----------------------------            

	/**
	 * Public method to retrieve list of subsystem factory proxies registered by extension points.
	 */
	public ISubSystemConfigurationProxy[] getSubSystemConfigurationProxies();

	/**
	 * Return all subsystem factory proxies matching a subsystem factory category.
	 * @see ISubSystemConfigurationCategories
	 */
	public ISubSystemConfigurationProxy[] getSubSystemConfigurationProxiesByCategory(String factoryCategory);

	/**
	 * Return all subsystem factories.
	 * 
	 * Be careful when you call this, as it activates all subsystem configurations.
	 * @deprecated use {@link #getSubSystemConfigurationProxies()} and filter the
	 *    list of needed subsystem configurations in order to activate only those
	 *    that are really needed.
	 */
	public ISubSystemConfiguration[] getSubSystemConfigurations();

	/**
	 * Return the parent subsystem configuration given a subsystem object.
	 * @deprecated use subsystem.getSubSystemConfiguration()
	 */
	public ISubSystemConfiguration getSubSystemConfiguration(ISubSystem subsystem);

	/**
	 * Return the subsystem configuration, given its plugin.xml-declared id.
	 */
	public ISubSystemConfiguration getSubSystemConfiguration(String id);

	/**
	 * Return all subsystem factories which have declared themselves part of the given category.
	 * <p>
	 * This looks for a match on the "category" of the subsystem factory's xml declaration
	 * in its plugin.xml file. Thus, it is efficient as it need not bring to life a 
	 * subsystem factory just to test its parent class type.
	 * 
	 * @deprecated use {@link #getSubSystemConfigurationProxiesByCategory(String)}
	 *    and instantiate only those subsystem configurations from the proxy
	 *    that are really needed.
	 * 
	 * @see ISubSystemConfigurationCategories
	 */
	public ISubSystemConfiguration[] getSubSystemConfigurationsByCategory(String factoryCategory);

	/**
	 * Return all subsystem factories which support the given system type.
	 * If the type is null, returns all.
	 * @param systemType system type to filter
	 * @param filterDuplicateServiceSubSystemFactories set false by default
	 */
	public ISubSystemConfiguration[] getSubSystemConfigurationsBySystemType(IRSESystemType systemType, boolean filterDuplicateServiceSubSystemFactories);
	
	// ----------------------------------
	// SYSTEMVIEWINPUTPROVIDER METHODS...
	// ----------------------------------

	/**
	 * This method is called by the connection adapter when the user expands
	 * a connection. This method must return the child objects to show for that
	 * connection.
	 * @param selectedConnection the connection undergoing expansion
	 * @return the list of objects under the connection
	 */
	public Object[] getConnectionChildren(IHost selectedConnection);

	/**
	 * This method is called by the connection adapter when deciding to show a plus-sign
	 * or not beside a connection.
	 * @param selectedConnection the connection being shown in the viewer 
	 * @return true if this connection has children to be shown.
	 */
	public boolean hasConnectionChildren(IHost selectedConnection);
	
	// ----------------------------
	// USER PREFERENCE METHODS...
	// ----------------------------
	/**
	 * Are connection names to be qualified by profile name?
	 */
	public boolean getQualifiedHostNames();

	/**
	 * Set if connection names are to be qualified by profile name
	 */
	public void setQualifiedHostNames(boolean set);

	/**
	 * Reflect the user changing the preference for showing filter pools.
	 */
	public void setShowFilterPools(boolean show);

	/*
	 * Reflect the user changing the preference for showing filter strings.
	 *
	 public void setShowFilterStrings(boolean show);
	 */
	/**
	 * Reflect the user changing the preference for showing new connection prompt
	 */
	public void setShowNewHostPrompt(boolean show);

	// ----------------------------
	// PROFILE METHODS...
	// ----------------------------
	/**
	 * Return singleton profile manager
	 */
	public ISystemProfileManager getSystemProfileManager();

	/**
	 * Return the profiles currently selected by the user as his "active" profiles
	 * @see ISystemProfileManager#getActiveSystemProfiles()
	 */
	public ISystemProfile[] getActiveSystemProfiles();

	/**
	 * Return the profile names currently selected by the user as his "active" profiles
	 * @deprecated use getSystemProfileManager().getActiveSystemProfiles() 
	 *     and get the names out of the returned array 
	 */
	public String[] getActiveSystemProfileNames();

	/**
	 * Return all defined profiles
	 * @deprecated use getSystemProfileManager().getSystemProfiles()
	 */
	public ISystemProfile[] getAllSystemProfiles();

	/**
	 * Return all defined profile names
	 * @deprecated use getSystemProfileManager().getSystemProfiles()
	 *     and get the names out of the returned array 
	 */
	public String[] getAllSystemProfileNames();

	/**
	 * Get a SystemProfile given its name
	 */
	public ISystemProfile getSystemProfile(String profileName);

	/**
	 * Create a SystemProfile given its name and whether or not to make it active
	 */
	public ISystemProfile createSystemProfile(String profileName, boolean makeActive) throws Exception;

	/**
	 * Copy a SystemProfile. All connections connection data is copied.
	 * @param profile Source profile to copy
	 * @param newName Unique name to give copied profile
	 * @param makeActive whether to make the copied profile active or not
	 * @param monitor Progress monitor to reflect each step of the operation    
	 * @return new SystemProfile object
	 */
	public ISystemProfile copySystemProfile(ISystemProfile profile, String newName, boolean makeActive, IProgressMonitor monitor) throws Exception;

	/**
	 * Rename a SystemProfile. Rename is propagated to all subsystem factories so
	 * they can rename their filter pool managers and whatever else is required.
	 */
	public void renameSystemProfile(ISystemProfile profile, String newName) throws Exception;

	/**
	 * Delete a SystemProfile. Prior to physically deleting the profile, we delete all
	 * the connections it has, all the subsystems they have.
	 * <p>
	 * As well, all the filter pools for this profile are deleted, and subsequently any
	 * cross references from subsystems in connections in other profiles are removed.
	 * <p>
	 * A delete event is fired for every connection deleted.
	 */
	public void deleteSystemProfile(ISystemProfile profile) throws Exception;

	/**
	 * Make or unmake the given profile active
	 */
	public void setSystemProfileActive(ISystemProfile profile, boolean makeActive);

	/**
	 * Return the list of connector services provided for the given host
	 * @param conn the host
	 * @return the list of connector services
	 */
	public IConnectorService[] getConnectorServices(IHost conn);

	// ----------------------------
	// SUBSYSTEM METHODS...
	// ----------------------------

	/**
	 * Return list of subsystem objects for a given connection.  If the subsystems have
	 * not all been read into memory, this loads them up
	 */
	public ISubSystem[] getSubSystems(IHost conn);

	/**
	 * Return list of subsystem objects for a given connection.  Use the force
	 * flag to indicate whether or not to restore from disk
	 */
	public ISubSystem[] getSubSystems(IHost conn, boolean force);

	/**
	 * Get those subsystems that are registered against a given connection,
	 * which are an instance of the given interface class.
	 * <p>
	 * This method activates all subsystem configurations of the given
	 * host in order to support checking against the given interface.
	 * If lazy loading is desired, use {@link #getSubSystems(IHost, boolean)}
	 * with a boolean parameter <code>false</code> instead, and check against
	 * the class instance in client code.
	 * </p>
	 * @param connection the connection to check
	 * @param subsystemInterface the interface class to filter against
	 * @return list of matching subsystems
	 */
	public ISubSystem[] getSubsystems(IHost connection, Class subsystemInterface);
	
	/**
	 * Get those subsystems that are registered against a given connection,
	 * which are an instance of ServiceSubSystem for the given serviceType.
	 * <p>
	 * This method activates all subsystem configurations of the given
	 * host in order to support checking against the given interface.
	 * If lazy loading is desired, use {@link #getSubSystems(IHost, boolean)}
	 * with a boolean parameter <code>false</code> instead, and check against
	 * the class instance in client code.
	 * </p>
	 * @param connection the connection to check
	 * @param serviceType the class of service to ask for
	 * @return list of matching subsystems
	 */
	public ISubSystem[] getServiceSubSystems(IHost connection, Class serviceType);

	/**
	 * Resolve a subsystem from it's profile, connection and subsystem name.
	 * 
	 * @deprecated use other search methods in ISystemRegistry
	 * 
	 * @param srcProfileName the name of the profile
	 * @param srcConnectionName the name of the connection
	 * @param subsystemConfigurationId the id of the subsystem
	 * 
	 * @return the subsystem
	 */
	public ISubSystem getSubSystem(String srcProfileName, String srcConnectionName, String subsystemConfigurationId);

	/**
	 * Resolve a subsystem from it's absolute name
	 * 
	 * @param absoluteSubSystemName the name of the subsystem
	 * 
	 * @return the subsystem
	 */
	public ISubSystem getSubSystem(String absoluteSubSystemName);

	/**
	 * Return the absolute name for the specified subsystem
	 * @param subsystem the subsystem to query
	 * @return the absolute name of the subsystem
	 */
	public String getAbsoluteNameForSubSystem(ISubSystem subsystem);

	/**
	 * Return the absolute name for the specified host (connection)
	 * @param connection the host (aka connection) object to query
	 * @return the absolute name of the host
	 */
	public String getAbsoluteNameForConnection(IHost connection);

	/**
	 * Get a list of subsystem objects owned by the subsystem configuration
	 * identified by its given plugin.xml-described id.
	 * <p>
	 * This is a list that of all subsystems for all connections owned by the factory.
	 * Array is never null, but may be of length 0.
	 * </p>
	 * @deprecated use {@link #getSubSystemConfiguration(String).getSubSystems(true)
	 */
	public ISubSystem[] getSubSystems(String factoryId);

	/**
	 * Get a list of subsystem objects for given connection, owned by the subsystem 
	 * configuration identified by its given plugin.xml-described id.
	 * Array will never be null but may be length zero.
	 * @deprecated use {@link #getSubSystemConfiguration(String).getSubSystems(connection, true)
	 */
	public ISubSystem[] getSubSystems(String factoryId, IHost connection);

	/**
	 * Get a list of subsystem objects for given connection, owned by a subsystem factory 
	 * that is of the given category. Array will never be null but may be length zero.
	 * <p>
	 * This looks for a match on the "category" of the subsystem factory's xml declaration
	 *  in its plugin.xml file. 
	 * 
	 * @see org.eclipse.rse.core.model.ISubSystemConfigurationCategories
	 * @deprecated use {@link #getSubSystemConfigurationProxiesByCategory(String)}
	 *    and instantiate only those subsystem configurations from the proxy
	 *    that are really needed. Then, use {@link ISubSystemConfiguration#getSubSystems(boolean)}
	 *    with a parameter true.
	 */
	public ISubSystem[] getSubSystemsBySubSystemConfigurationCategory(String factoryCategory, IHost connection);

	/**
	 * Delete a subsystem object. This code finds the factory that owns it and
	 *  delegates the request to that factory.
	 */
	public boolean deleteSubSystem(ISubSystem subsystem);

	// ----------------------------
	// CONNECTION METHODS...
	// ----------------------------
	/**
	 * Return the first connection to localhost we can find. While we always create a default one in
	 *  the user's profile, it is possible that this profile is not active or the connection was deleted.
	 *  However, since any connection to localHost will usually do, we just search all active profiles
	 *  until we find one, and return it. <br>
	 * If no localhost connection is found, this will return null. If one is needed, it can be created 
	 *  easily by calling {@link #createLocalHost(ISystemProfile, String, String)}.
	 */
	public IHost getLocalHost();

	/**
	 * Return all connections in all active profiles.
	 */
	public IHost[] getHosts();

	/**
	 * Return all connections in a given profile.
	 */
	public IHost[] getHostsByProfile(ISystemProfile profile);

	/**
	 * Return all connections in a given profile name.
	 * @deprecated use {@link #getSystemProfile(String)} and
	 *     {@link #getHostsByProfile(ISystemProfile)}
	 */
	public IHost[] getHostsByProfile(String profileName);

	/**
	 * Return all connections for which there exists one or more
	 * subsystems owned by a given subsystem configuration.
	 * @see #getSubSystemConfiguration(String)
	 */
	public IHost[] getHostsBySubSystemConfiguration(ISubSystemConfiguration config);

	/**
	 * Return all connections for which there exists one or more
	 * subsystems owned  by a given subsystem configuration,
	 * identified by configuration Id.
	 * @deprecated use {@link #getSubSystemConfiguration(String)} and
	 *     {@link #getHostsBySubSystemConfiguration(ISubSystemConfiguration)}
	 */
	public IHost[] getHostsBySubSystemConfigurationId(String configId);

	/**
	 * Return all connections for which there exists one or more
	 * subsystems owned by any a given subsystem configuration
	 * that is of the given category.
	 * <p>
	 * This looks for a match on the "category" of the subsystem
	 * configuration's xml declaration in its plugin.xml file.
	 * Thus, it is efficient as it need not bring to life a 
	 * subsystem configuration just to test its parent class type.
	 * 
	 * @see org.eclipse.rse.core.model.ISubSystemConfigurationCategories
	 */
	public IHost[] getHostsBySubSystemConfigurationCategory(String factoryCategory);

	/**
	 * Returns all connections for all active profiles, for the given system type.
	 * If the specified system type is null, an empty array is returned.
	 * In order to get an IRSESystemType, use
	 * <code>RSECorePlugin.getTheCoreRegistry().{@link RSECoreRegistry#getSystemTypeById(String) getSystemTypeById(String)}</code>
	 * 
	 * @param systemType The system type instance.
	 * @return The list of connections or an empty array.
	 */
	public IHost[] getHostsBySystemType(IRSESystemType systemType);
	
	/**
	 * Return all connections for all active profiles, for the given system types.
	 * 
	 * In order to get an IRSESystemType, use
	 * <code>RSECorePlugin.getTheCoreRegistry().{@link RSECoreRegistry#getSystemTypeById(String) getSystemTypeById(String)}</code>
	 */
	public IHost[] getHostsBySystemTypes(IRSESystemType[] systemTypes);

	/**
	 * Return a SystemConnection object given a system profile containing it, 
	 *   and a connection name uniquely identifying it.
	 */
	public IHost getHost(ISystemProfile profile, String connectionName);

	/**
	 * Return the zero-based position of a SystemConnection object within
	 * its profile.
	 */
	public int getHostPosition(IHost conn);

	/**
	 * Return the number of SystemConnection objects within the given profile.
	 */
	public int getHostCount(ISystemProfile profile);

	/**
	 * Return the number of SystemConnection objects within the given profile.
	 * @deprecated use {@link #getSystemProfile(String)} with
	 *     {@link #getHostCount(ISystemProfile)}
	 */
	public int getHostCount(String profileName);

	/**
	 * Return the number of SystemConnection objects within the given 
	 * connection's owning profile.
	 */
	public int getHostCountWithinProfile(IHost conn);

	/**
	 * Return the number of SystemConnection objects within all active
	 * profiles.
	 */
	public int getHostCount();

	/**
	 * Return a vector of previously-used connection names in the given named profile.
	 * @return Vector of String objects.
	 * @deprecated use {@link #getHostAliasNames(ISystemProfile)}
	 */
	public Vector getHostAliasNames(String profileName);

	/**
	 * Return a vector of previously-used connection names in the given profile.
	 * @return Vector of String objects.
	 */
	public Vector getHostAliasNames(ISystemProfile profile);

	/**
	 * Return a vector of previously-used connection names in all active profiles.
	 */
	public Vector getHostAliasNamesForAllActiveProfiles();

	/**
	 * Return array of previously specified host names for a given system type.
	 * After careful consideration, it is decided that if the system type is null,
	 * then no host names should be returned. Previously all for all types were returned.
	 */
	public String[] getHostNames(IRSESystemType systemType);

	/**
	 * Convenience method to create a local connection, as it often that one is needed
	 *  for access to the local file system.
	 * @param profile - the profile to create this connection in. If null is passed, we first
	 *   try to find the default private profile and use it, else we take the first active profile.
	 * @param name - the name to give this profile. Must be unique and non-null.
	 * @param userId - the user ID to use as the default for the subsystems. Can be null.
	 */
	public IHost createLocalHost(ISystemProfile profile, String name, String userId);

	/**
	 * Create a host object, sometimes called a "connection", 
	 * given the containing profile and given all the possible attributes.
	 * The profile is then scheduled to be persisted.
	 * <p>
	 * This method:
	 * <ul>
	 * <li>creates and saves a new connection within the given profile
	 * <li>calls all subsystem factories to give them a chance to create a subsystem instance
	 * <li>fires an ISystemResourceChangeEvent event of type EVENT_ADD to all registered listeners
	 * </ul>
	 * <p>
	 * @param profileName Name of the system profile the connection is to be added to.
	 * @param systemType system type matching one of the system types
	 *     defined via the systemTypes extension point.
	 * @param connectionName unique connection name.
	 * @param hostName ip name of host.
	 * @param description optional description of the connection. Can be null.
	 * @param defaultUserId userId to use as the default for the subsystems.
	 * @param defaultUserIdLocation one of the constants in {@link org.eclipse.rse.core.IRSEUserIdConstants}
	 * that tells us where to store the user Id
	 * @param newConnectionWizardPages when called from the New Connection wizard this is union of the list of additional
	 * wizard pages supplied by the subsystem factories that pertain to the specified system type. Else null.
	 * @return SystemConnection object, or null if it failed to create. This is typically
	 * because the connectionName is not unique. Call getLastException() if necessary.
	 */
	public IHost createHost(String profileName, IRSESystemType systemType, String connectionName, String hostName, String description, String defaultUserId, int defaultUserIdLocation,
			ISystemNewConnectionWizardPage[] newConnectionWizardPages) throws Exception;

	/**
	 * Create a connection object. This is a simplified version
	 * <p>
	 * THE RESULTING CONNECTION OBJECT IS ADDED TO THE LIST OF EXISTING CONNECTIONS FOR YOU, IN
	 *  THE PROFILE YOU SPECIFY. THE PROFILE IS ALSO SAVED TO DISK.
	 * <p>
	 * This method:
	 * <ul>
	 *  <li>creates and saves a new connection within the given profile
	 *  <li>calls all subsystem factories to give them a chance to create a subsystem instance
	 *  <li>fires an ISystemResourceChangeEvent event of type EVENT_ADD to all registered listeners
	 * </ul>
	 * <p>
	 * @param profileName Name of the system profile the connection is to be added to.
	 * @param systemType system type matching one of the system types
	 *     defined via the systemTypes extension point.
	 * @param connectionName unique connection name.
	 * @param hostName ip name of host.
	 * @param description optional description of the connection. Can be null.
	 * @return SystemConnection object, or null if it failed to create. This is typically
	 *   because the connectionName is not unique. Call getLastException() if necessary.
	 */
	public IHost createHost(String profileName, IRSESystemType systemType, String connectionName, String hostName, String description) throws Exception;

	/**
	 * Create a connection object. This is a very simplified version that defaults to the user's
	 *  private profile, or the first active profile if there is no private profile.
	 * <p>
	 * THE RESULTING CONNECTION OBJECT IS ADDED TO THE LIST OF EXISTING CONNECTIONS FOR YOU, IN
	 *  THE DEFAULT PRIVATE PROFILE, WHICH IS SAVED TO DISK.
	 * <p>
	 * This method:
	 * <ul>
	 *  <li>creates and saves a new connection within the given profile
	 *  <li>calls all subsystem factories to give them a chance to create a subsystem instance
	 *  <li>fires an ISystemResourceChangeEvent event of type EVENT_ADD to all registered listeners
	 * </ul>
	 * <p>
	 * @param systemType system type matching one of the system types
	 *     defined via the systemTypes extension point.
	 * @param connectionName unique connection name.
	 * @param hostAddress ip name of host.
	 * @param description optional description of the connection. Can be null.
	 * @return SystemConnection object, or null if it failed to create. This is typically
	 *   because the connectionName is not unique. Call getLastException() if necessary.
	 */
	public IHost createHost(IRSESystemType systemType, String connectionName, String hostAddress, String description) throws Exception;
	
    /**
     * Update an existing host given the new information.
     * This method:
     * <ul>
     *  <li>calls the setXXX methods on the given host object, updating the information in it.
     *  <li>save the host's host pool to disk
     *  <li>fires an ISystemResourceChangeEvent event of type EVENT_CHANGE to all registered listeners
     *  <li>if the system type or host name is changed, calls disconnect on each associated subsystem.
     *       We must do this because a host name changes fundamentally affects the connection, 
     *       rendering any information currently displayed under
     *       that host obsolete.
     * </ul>
     * <p>
     * @param host the host to be updated
	 * @param systemType system type matching one of the system types
	 *     defined via the systemTypes extension point.
     * @param connectionName unique connection name.
     * @param hostName ip name of host.
     * @param description optional description of the host. Can be null.
     * @param defaultUserId userId to use as the default for the subsystems under this host.
     * @param defaultUserIdLocation one of the constants in {@link org.eclipse.rse.core.IRSEUserIdConstants}
     *   that tells us where to set the user Id
     */
    public void updateHost(IHost host, IRSESystemType systemType, String connectionName,
                                 String hostName, String description,
                                 String defaultUserId, int defaultUserIdLocation);
    
	/**
	 * Creates subsystems for a given host and subsystem configurations.
	 * @param host the host.
	 * @param configurations the subsystem configurations.
	 * @return the array of subsystems corresponding to the array of given configurations.
	 * @since 2.0
	 */
	public ISubSystem[] createSubSystems(IHost host, ISubSystemConfiguration[] configurations);

	/**
	 * Update the workoffline mode for a connection.
	 * 
	 * @param conn SystemConnection to change
	 * @param offline true if connection should be set offline, false if it should be set online
	 */
	public void setHostOffline(IHost conn, boolean offline);

	/**
	 * Delete an existing connection. 
	 * <p>
	 * Lots to do here:
	 * <ul>
	 *   <li>Delete all subsystem objects for this connection, including their file's on disk.
	 *   <li>Delete the connection from memory.
	 *   <li>Delete the connection's folder from disk.
	 * </ul>
	 * Assumption: firing the delete event is done elsewhere. Specifically, the doDelete method of SystemView.
	 */
	public void deleteHost(IHost conn);

	/**
	 * Renames an existing connection. 
	 * <p>
	 * Lots to do here:
	 * <ul>
	 *   <li>Reset the conn name for all subsystem objects for this connection
	 *   <li>Rename the connection in memory.
	 *   <li>Rename the connection's folder on disk.
	 * </ul>
	 * Assumption: firing the rename event is done elsewhere. Specifically, the doRename method of SystemView.
	 */
	public void renameHost(IHost conn, String newName) throws Exception;

	/**
	 * Move existing connections a given number of positions in the same profile.
	 * If the delta is negative, they are all moved up by the given amount. If 
	 * positive, they are all moved down by the given amount.<p>
	 * <ul>
	 * <li>After the move, the pool containing the moved connection is saved to disk.
	 * <li>The connection's name must be unique in pool.
	 * <li>Fires a single ISystemResourceChangeEvent event of type EVENT_MOVE, if the pool is the private pool.
	 * </ul>
	 * @param conns Array of SystemConnections to move.
	 * @param delta new zero-based position for the connection
	 */
	public void moveHosts(String profileName, IHost conns[], int delta);

	/**
	 * Copy a SystemConnection. All subsystems are copied, and all connection data is copied.
	 * @param conn The connection to copy
	 * @param targetProfile What profile to copy into
	 * @param newName Unique name to give copied profile
	 * @param monitor Progress monitor to reflect each step of the operation
	 * @return new SystemConnection object
	 */
	public IHost copyHost(IHost conn, ISystemProfile targetProfile, String newName, IProgressMonitor monitor) throws Exception;

	/**
	 * Move a SystemConnection to another profile.
	 * All subsystems are moved, and all connection data is moved.
	 * This is actually accomplished by doing a copy operation first,
	 * and if successful deleting the original.
	 * @param conn The connection to move
	 * @param targetProfile What profile to move into
	 * @param newName Unique name to give copied profile. Typically this is the same as the original name, but 
	 *                will be different on name collisions
	 * @param monitor Progress monitor to reflect each step of the operation
	 * @return new SystemConnection object
	 */
	public IHost moveHost(IHost conn, ISystemProfile targetProfile, String newName, IProgressMonitor monitor) throws Exception;

	/**
	 * Return true if any subsystem supports connecting.
	 * @param conn the connection.
	 * @return <code>true</code> if any subsystem supports connecting, <code>false</code> otherwise.
	 */
	public boolean isAnySubSystemSupportsConnect(IHost conn);

	/**
	 * Return true if any of the subsystems for the given connection are
	 * currently connected.
	 */
	public boolean isAnySubSystemConnected(IHost conn);

	/**
	 * Return true if all of the subsystems for the given connection are
	 * currently connected.
	 */
	public boolean areAllSubSystemsConnected(IHost conn);

	/**
	 * Disconnect all subsystems for the given connection, if they are
	 * currently connected.
	 */
	public void disconnectAllSubSystems(IHost conn);

	/**
	 * Inform the world when the connection status changes for a subsystem
	 * within a connection.
	 * Update properties for the subsystem and its connection.
	 */
	public void connectedStatusChange(ISubSystem subsystem, boolean connected, boolean wasConnected);

	/**
	 * Inform the world when the connection status changes for a subsystem
	 * within a connection.
	 * Update properties for the subsystem and its connection.
	 */
	public void connectedStatusChange(ISubSystem subsystem, boolean connected, boolean wasConnected, boolean collapseTree);

	// ----------------------------
	// RESOURCE EVENT METHODS...
	// ----------------------------            

	/**
	 * Register your interest in being told when a system resource such as a connection is changed.
	 */
	public void addSystemResourceChangeListener(ISystemResourceChangeListener l);

	/**
	 * De-Register your interest in being told when a system resource such as a connection is changed.
	 */
	public void removeSystemResourceChangeListener(ISystemResourceChangeListener l);

	/**
	 * Query if the ISystemResourceChangeListener is already listening for SystemResourceChange events
	 */
	public boolean isRegisteredSystemResourceChangeListener(ISystemResourceChangeListener l);

	/**
	 * Notify all listeners of a change to a system resource such as a connection.
	 * You would not normally call this as the methods in this class call it when appropriate.
	 */
	public void fireEvent(ISystemResourceChangeEvent event);

	/**
	 * Notify a specific listener of a change to a system resource such as a connection.
	 */
	public void fireEvent(ISystemResourceChangeListener l, ISystemResourceChangeEvent event);

	// ----------------------------
	// MODEL RESOURCE EVENT METHODS...
	// ----------------------------            

	/**
	 * Register your interest in being told when an RSE model resource is changed.
	 * These are model events, not GUI-optimized events.
	 */
	public void addSystemModelChangeListener(ISystemModelChangeListener l);

	/**
	 * De-Register your interest in being told when an RSE model resource is changed.
	 */
	public void removeSystemModelChangeListener(ISystemModelChangeListener l);

	/**
	 * Notify all listeners of a change to a system model resource such as a connection.
	 * You would not normally call this as the methods in this class call it when appropriate.
	 */
	public void fireEvent(ISystemModelChangeEvent event);

	/**
	 * Notify all listeners of a change to a system model resource such as a connection.
	 * This one takes the information needed and creates the event for you.
	 */
	public void fireModelChangeEvent(int eventType, int resourceType, Object resource, String oldName);

	/**
	 * Notify a specific listener of a change to a system model resource such as a connection.
	 */
	public void fireEvent(ISystemModelChangeListener l, ISystemModelChangeEvent event);

	// --------------------------------
	// REMOTE RESOURCE EVENT METHODS...
	// --------------------------------            

	/**
	 * Register your interest in being told when a remote resource is changed.
	 * These are model events, not GUI-optimized events.
	 */
	public void addSystemRemoteChangeListener(ISystemRemoteChangeListener l);

	/**
	 * De-Register your interest in being told when a remote resource is changed.
	 */
	public void removeSystemRemoteChangeListener(ISystemRemoteChangeListener l);

	/**
	 * Notify all listeners of a change to a remote resource such as a file.
	 * You would not normally call this as the methods in this class call it when appropriate.
	 */
	public void fireEvent(ISystemRemoteChangeEvent event);
	
	/**
	 * Notify all listeners of a change to a remote resource such as a file.
	 * This one takes the information needed and creates the event for you.
	 * @param eventType - one of the constants from {@link org.eclipse.rse.core.events.ISystemRemoteChangeEvents}
	 * @param resource - the remote resource object, or absolute name of the resource as would be given by calling getAbsoluteName on its remote adapter
	 * @param resourceParent - the remote resource's parent object, or absolute name, if that is known. If it is non-null, this will aid in refreshing occurences of that parent.
	 * @param subsystem - the subsystem which contains this remote resource. This allows the search for impacts to be 
	 *   limited to subsystems of the same parent factory, and to connections with the same hostname as the subsystem's connection.
	 * @param oldName - on a rename operation, this is the absolute name of the resource prior to the rename
	 */
	public void fireRemoteResourceChangeEvent(int eventType, Object resource, Object resourceParent, ISubSystem subsystem, String oldName);

	/**
	 * Notify all listeners of a change to a remote resource such as a file.
	 * This one takes the information needed and creates the event for you.
	 * @param eventType - one of the constants from {@link org.eclipse.rse.core.events.ISystemRemoteChangeEvents}
	 * @param resource - the remote resource object, or absolute name of the resource as would be given by calling getAbsoluteName on its remote adapter
	 * @param resourceParent - the remote resource's parent object, or absolute name, if that is known. If it is non-null, this will aid in refreshing occurences of that parent.
	 * @param subsystem - the subsystem which contains this remote resource. This allows the search for impacts to be 
	 *   limited to subsystems of the same parent factory, and to connections with the same hostname as the subsystem's connection.
	 * @param oldName - on a rename operation, this is the absolute name of the resource prior to the rename
	 * @param originatingViewer - optional. If set, this gives the viewer a clue that it should select the affected resource after refreshing its parent. 
	 *    This saves sending a separate event to reveal and select the new created resource on a create event, for example.
	 */
	public void fireRemoteResourceChangeEvent(int eventType, Object resource, Object resourceParent, ISubSystem subsystem, String oldName, Object originatingViewer);

	/**
	 * Notify a specific listener of a change to a remote resource such as a file.
	 */
	public void fireEvent(ISystemRemoteChangeListener l, ISystemRemoteChangeEvent event);

	// ----------------------------
	// PREFERENCE EVENT METHODS...
	// ----------------------------            

	/**
	 * Register your interest in being told when a system preference changes
	 */
	public void addSystemPreferenceChangeListener(ISystemPreferenceChangeListener l);

	/**
	 * De-Register your interest in being told when a system preference changes
	 */
	public void removeSystemPreferenceChangeListener(ISystemPreferenceChangeListener l);

	/**
	 * Notify all listeners of a change to a system preference
	 * You would not normally call this as the methods in this class call it when appropriate.
	 */
	public void fireEvent(ISystemPreferenceChangeEvent event);

	/**
	 * Notify a specific listener of a change to a system preference 
	 */
	public void fireEvent(ISystemPreferenceChangeListener l, ISystemPreferenceChangeEvent event);

	// ----------------------------
	// MISCELLANEOUS METHODS...
	// ----------------------------

	/**
	 * Returns filter references associated with this resource under the subsystem
	 */
	public List findFilterReferencesFor(Object resource, ISubSystem subsystem);

	/**
	 * Returns filter references associated with this resource under the subsystem
	 */
	public List findFilterReferencesFor(Object resource, ISubSystem subsystem, boolean onlyCached);

	/**
	 * Marks all filters for this subsystem as stale to prevent caching
	 * @param subsystem
	 */
	public void invalidateFiltersFor(ISubSystem subsystem);

	/**
	 * Marks all filters for this subsystem the contain resourceParent as stale to prevent caching
	 * @param resourceParent 
	 * @param subsystem
	 */
	public void invalidateFiltersFor(Object resourceParent, ISubSystem subsystem);

	/**
	 * Return last exception object caught in any method, or null if no exception.
	 * This has the side effect of clearing the last exception.
	 */
	public Exception getLastException();

	// ----------------------------
	// SAVE / RESTORE METHODS...
	// ----------------------------            

	/**
	 * Save everything! 
	 */
	public boolean save();

	/**
	 * Save specific connection pool
	 * @return true if saved ok, false if error encountered. If false, call getLastException().
	 */
	public boolean saveHostPool(ISystemHostPool pool);

	/**
	 * Save specific connection
	 * @return true if saved ok, false if error encountered. If false, call getLastException().
	 */
	public boolean saveHost(IHost conn);

	/**
	 * Restore all connections within active profiles
	 * @return true if restored ok, false if error encountered. If false, call getLastException().
	 */
	public boolean restore();
}
