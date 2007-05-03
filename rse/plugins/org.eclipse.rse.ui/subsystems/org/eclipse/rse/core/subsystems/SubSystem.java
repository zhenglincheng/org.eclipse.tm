/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - 141803: Fix cpu usage 100% while connecting
 * David Dykstal (IBM) - 168870: moved SystemPreferencesManager to a new package
 * David Dykstal (IBM) - 168870: created and used RSEPreferencesManager
 * Martin Oberhuber (Wind River) - [175262] IHost.getSystemType() should return IRSESystemType 
 * David Dykstal (IBM) - 142806: refactoring persistence framework
 ********************************************************************************/

package org.eclipse.rse.core.subsystems;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.RSEPreferencesManager;
import org.eclipse.rse.core.SystemBasePlugin;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.filters.ISystemFilterPoolManager;
import org.eclipse.rse.core.filters.ISystemFilterPoolReference;
import org.eclipse.rse.core.filters.ISystemFilterPoolReferenceManager;
import org.eclipse.rse.core.filters.ISystemFilterPoolReferenceManagerProvider;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.filters.ISystemFilterString;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.IPropertySet;
import org.eclipse.rse.core.model.IRSECallback;
import org.eclipse.rse.core.model.IRSEPersistableContainer;
import org.eclipse.rse.core.model.ISystemModelChangeEvents;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.model.RSEModelObject;
import org.eclipse.rse.internal.ui.GenericMessages;
import org.eclipse.rse.model.ISystemResourceChangeEvents;
import org.eclipse.rse.model.SystemResourceChangeEvent;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.dialogs.SystemPromptDialog;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * This class is designed to be subclassed. Its role is three-fold:
 * <ol>
 *   <li>Storing of tool-specific persistent properties per connection.
 *   <li>Accessing of an IConnectorService object to enable the subsystem's connect and disconnect actions.
 *   <li>Doing actual remote accessing. This usually just involves overriding the <code>internalResolveFilterString</code> methods to
 *         populate the remote resources shown when the subsystem's filters are expanded.
 * </ol>
 * <p>
 * This is the base class that subsystem suppliers subclass.
 * Each instance of this class represents a subsystem instance for a particular connection.
 * <p>
 * When a {@link IHost} is created, this subsystem's factory will be asked to create an
 * instance of its subsystem. If desired, your GUI may also allow users to create additional
 * instances.
 * <p>
 * There are only a handleful of methods to implement in child classes (and indeed most of these
 *  are supplied as empty, so you only override those you want to support).
 * These are required:
 * <ul>
 * <li>{@link #getConnectorService()}. 
 * </ul>
 * These are optional:
 * <ul>
 * <li>{@link #getObjectWithAbsoluteName(String)}
 * <li>{@link #internalResolveFilterString(IProgressMonitor monitor, String filterString)}
 * <li>{@link #internalResolveFilterString(IProgressMonitor monitor, Object parent, String filterString)}
 * <li>{@link #internalGetProperty(IProgressMonitor monitor, Object subject, String key)}
 * <li>{@link #internalSetProperty(IProgressMonitor monitor, Object subject, String key, String value)}
 * <li>{@link #internalGetProperties(IProgressMonitor monitor, Object subject, String[] keys)}
 * <li>{@link #internalSetProperties(IProgressMonitor monitor, Object subject, String[] keys, String[] values)}
 * </ul>
 * 
 */

public abstract class SubSystem extends RSEModelObject implements IAdaptable, ISystemFilterPoolReferenceManagerProvider, ISystemResourceChangeEvents, ISubSystem
{


	protected static final String SUBSYSTEM_FILE_NAME = "subsystem"; //$NON-NLS-1$

	//protected transient SubSystemConfiguration parentFactory = null;	
    protected static final int OPERATION_RESOLVE_ABSOLUTE = 0;
    protected static final int OPERATION_RESOLVE_ABSOLUTES= 1;
    protected static final int OPERATION_RESOLVE_RELATIVE = 2;
    protected static final int OPERATION_RUN_COMMAND      = 3;
    protected static final int OPERATION_GET_PROPERTY     = 4;
    protected static final int OPERATION_SET_PROPERTY     = 5;
    protected static final int OPERATION_GET_PROPERTIES   = 6;
    protected static final int OPERATION_SET_PROPERTIES   = 7;
    protected static final int OPERATION_CONNECT          = 8;
    protected static final int OPERATION_DISCONNECT       = 9;
	protected static final int OPERATION_RUN_SHELL = 10;
	protected static final int OPERATION_SEND_COMMAND_TO_SHELL = 11;
	protected static final int OPERATION_CANCEL_SHELL = 12;
	protected static final int OPERATION_REMOVE_SHELL = 13;
	
    protected ISubSystemConfiguration parentSubSystemConfiguration;
    protected String           previousUserIdKey;
	
    protected Shell shell = null;
    protected boolean supportsConnecting = true;
    protected boolean sortResults = true;
    protected boolean runInThread = true;
     	 
    protected boolean cancelable = true; 	 
    protected boolean doConnection = false; 	
    protected ProgressMonitorDialog pmDialog;
    protected String saveFileName;
	protected IConnectorService _connectorService = null;
	
	protected boolean _connectionError = false;
	protected boolean _disconnecting = false;

	protected IHost   _host;
		

	protected String _name = null;
	protected String _subsystemConfigurationId = null;
	protected boolean _hidden = false;

	
	/**
	 * @generated This field/method will be replaced during code generation.
	 */
	protected ISystemFilterPoolReferenceManager filterPoolReferenceManager = null;

	private class NullRunnableContext implements IRunnableContext {
		public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
			IProgressMonitor monitor = new NullProgressMonitor();
			runnable.run(monitor);
		}
	}
	

	public class SystemMessageDialogRunnable implements Runnable
	{
		private SystemMessageDialog _dlg;
		public SystemMessageDialogRunnable(SystemMessageDialog dlg)
		{
			_dlg = dlg;
		}
		
		public void run()
		{
			_dlg.open();
		}
	}

	protected SubSystem(IHost host, IConnectorService connectorService) 
	{
		super();
		_host = host;
		_connectorService = connectorService;
		_connectorService.registerSubSystem(this);
	}

    /**
     * Internal method to select the appropriate command subsystem when there are multiple defined for this connection.
     * The default implementation is to return the first, but child classes can refine this. Input is always an array of
     * length greater than one.
     */
    protected ISubSystem selectCommandSubSystem(ISubSystem[] allCmdSubSystems)
    {
    	if (allCmdSubSystems != null && allCmdSubSystems.length > 0)
    		return allCmdSubSystems[0];
    	return null;
    }		
	
    /**
     * Return the parent subsystem factory that owns this subsystem.
     */
    public ISubSystemConfiguration getSubSystemConfiguration()
    {
    	return parentSubSystemConfiguration;
    }
    /**
     * Return the parent subsystem factory that owns this subsystem.
     */
    public void setSubSystemConfiguration(ISubSystemConfiguration ssf)
    {
    	parentSubSystemConfiguration = ssf;
    	supportsConnecting = ssf.supportsSubSystemConnect();
    	//System.out.println("subsystem supports connecting? " + supportsConnecting);
    }
    /**
     * Set the parent connection that owns this subsystem.
     */
    public void setHost(IHost conn)
    {
    	_host = conn;
    	previousUserIdKey = getPreferencesKey();
    }

    /**
     * Return true if userId and password should be forced to uppercase.
     * INTERACTIVEcut to calling same method in parent connection.
     */
    public boolean forceUserIdToUpperCase()
    {
    	/* defect 43219
    	if (parentSubSystemConfiguration != null)
    	  return parentSubSystemConfiguration.forceUserIdToUpperCase();
    	else
    	  return true;
    	*/
    	IHost currConn = getHost();
    	if (currConn != null)
    	  return currConn.getForceUserIdToUpperCase();
    	else
    	  return false;    	
    }

    /**
     * The profile is being renamed, so we are called to do our thing with
     * any information stored that is keyed by the profile name.
     * <p>
     * This is called AFTER the profile is renamed!
     */
    public void renamingProfile(String oldName, String newName)
    {
    	String userIdValue = null;
    	if (previousUserIdKey!=null)
   	      userIdValue = getLocalUserId(previousUserIdKey);
    	// if the userId attribute held a preference store key of the form profileName.connectionName.subsystemName,
    	// we have to delete that key entry from the preference store and re-gen a new keyed entry with the same
    	// value (the actual user id) the old keyed entry held.
    	String newKey = getPreferencesKey(newName, getHostAliasName());
    	if ((userIdValue != null) && (userIdValue.length()>0))
    	{
    		RSEPreferencesManager.clearUserId(previousUserIdKey);
    		RSEPreferencesManager.setUserId(newKey, userIdValue); // store old value with new preference key
    	}
    	previousUserIdKey = newKey;
    	
    	// now we need to potentially rename any filter pool associated with this connection...
		ISystemProfile profile = getSystemProfile();
		ISystemFilterPoolManager mgr = getSubSystemConfiguration().getFilterPoolManager(profile);
		ISystemFilterPool[] pools = mgr.getSystemFilterPools();
		boolean done = false;
		if (pools != null)
		{
			for (int idx = 0; !done && (idx < pools.length); idx++)
			{
				if ( (pools[idx].getOwningParentName()!=null) && // a connection-related pool				    	
					 (pools[idx].getOwningParentName().equals(getHostAliasName())) ) // this connection!
				{
					done = true;
					try
					{
						// re-gen name: %1 Filter Pool for connection %2, where %1 is profile name...
						mgr.renameSystemFilterPool(pools[idx], getConnectionOwnedFilterPoolName(newName, getHostAliasName()));
					}
					catch (Exception exc)
					{
						String msg = "Unexpected error renaming connection-specific filter pool " + getConnectionOwnedFilterPoolName(newName, getHostAliasName()); //$NON-NLS-1$
						SystemBasePlugin.logError(msg, exc);
						System.err.println(msg + ": " + exc); //$NON-NLS-1$
					}
				}
			}
		}    	
    }
    /**
     * The connection is being renamed, so we are called to do our thing with
     * any information stored that is keyed by the connection name.
     */
    public void renamingConnection(String newName)
    {
    	String userIdValue = null;
    	if (previousUserIdKey != null)
 	      userIdValue = getLocalUserId(previousUserIdKey);  // see if we previous stored a value for this connection     	
    	// if the userId attribute held a preference store key of the form profileName.connectionName.subsystemName,
    	// we have to delete that key entry from the preference store and re-gen a new keyed entry with the same
    	// value (the actual user id) the old keyed entry held.
    	String newKey = getPreferencesKey(getSystemProfileName(), newName);    	
    	if ((userIdValue != null) && (userIdValue.length()>0))
    	{
    		RSEPreferencesManager.clearUserId(previousUserIdKey);
    		RSEPreferencesManager.setUserId(newKey, userIdValue); // store old value with new preference key
    	}
    	previousUserIdKey = newKey;
    	 
    	// rename the connection-private filter pool, if it exists:
    	ISystemFilterPool privatePool = getConnectionPrivateFilterPool(false); // false => don't create if not found
    	if (privatePool != null)
    	{
    		ISystemFilterPoolManager mgr = getSubSystemConfiguration().getFilterPoolManager(getSystemProfile());
    		privatePool.setOwningParentName(newName);
    		try {    		   	
				mgr.renameSystemFilterPool(privatePool, getConnectionOwnedFilterPoolName(getSystemProfileName(), newName));
    		}
    		catch (Exception exc)
    		{
    			SystemBasePlugin.logError("Error renaming conection-private pool to: "+newName, exc); //$NON-NLS-1$
    		}
    	}
    }
	/**
	 * Private method called when the parent connection is being deleted, so
	 * the subsystem can do any pre-death cleanup it needs to.
	 * <p>
	 * What we need to do is delete our entry in the preference store for our userId.
	 */
	public void deletingConnection()
	{
    	String oldUserId = null;
    	if (previousUserIdKey != null)
    	  oldUserId = getLocalUserId(previousUserIdKey);
    	// if the userId attribute held a preference store key of the form profileName.connectionName.subsystemName,
    	// we have to delete that key entry from the preference store and re-gen a new keyed entry with the same
    	// value (the actual user id) the old keyed entry held.
    	if (oldUserId != null)
    	{
   	      RSEPreferencesManager.clearUserId(previousUserIdKey);
    	}		
		// delete the connection-private filter pool, if it exists:
		ISystemFilterPool privatePool = getConnectionPrivateFilterPool(false); // false => don't create if not found
		if (privatePool != null)
		{
			ISystemFilterPoolManager mgr = getSubSystemConfiguration().getFilterPoolManager(getSystemProfile());
			try {    		   	
				mgr.deleteSystemFilterPool(privatePool);
			}
			catch (Exception exc)
			{
				SystemBasePlugin.logError("Error deleting conection-private pool for: "+getHostAliasName(), exc); //$NON-NLS-1$
			}					
		}
	}

	/**
	 * Returns the value of this subsystem's local user id if it is not null. If it
	 * is null, it returns the parent connection object's default user Id. It in turn
	 * queries the preferences if its local value is null.
     * <p>
     * In fact, we now don't store the user Id in the subsystem object itself, but rather store it in the
     * user preferences, so that such things are not shared among the team on a synchronize operation.
     * This is transparent to callers of this method however, as this method resolves from the preferences.
     *
	 * @see org.eclipse.rse.core.model.IHost#getDefaultUserId()
	 * @see #getLocalUserId()
	 * @see #clearLocalUserId()
	 * @return The value of the UserId attribute
	 */
    public String getUserId()
    {
    	String uid = getLocalUserId();
    	if ((uid == null) || (uid.length()==0))
    	{    		
    	  IHost conn = getHost();
    	  uid = conn.getDefaultUserId();
    	}
    	return uid;
    }

    /**
     * Helper method to compute a unique name for a given subsystem instance
     */
    protected String getPreferencesKey()
    {
    	if ((_host==null) || (getName()==null))
    	  return null;
    	return getPreferencesKey(getSystemProfileName(), getHostAliasName());
    }
    /**
     * Helper method to compute a unique name for a given subsystem instance, given a profile and connection name
     */
    protected String getPreferencesKey(String profileName, String connectionName)
    {
   	    String key = profileName + "." + connectionName + "." + getName(); //$NON-NLS-1$ //$NON-NLS-2$
   	    //System.out.println("in SubSystemImpl.getPreferencesKey(): Subsystem key name: " + key);
   	    return key;
    }

    /**
     * Internal-use method for getting the local user ID, without resolution.
     */
    protected String getLocalUserId(String key)
    {
    	String uid = null;
    	if ((key!=null) && (key.length()>0))
    	{    	
    	  uid = RSEPreferencesManager.getUserId(key); // resolve from preferences	
    	}
    	return uid;    	
    }

    /**
     * Alternative to getUserId when we don't want to resolve it from parent connection.
     * This is used when showing the properties.
     * <p>
     * Unlike getUserId() this one does not defer to the connection's default user Id if
     * the subsystem's userId attribute is null.
     * <p>
     * To set the local user Id, simply call setUserId(String id). To clear it, call
     * {@link #clearLocalUserId()}.
     * <p>
	 * @see org.eclipse.rse.core.model.IHost#getDefaultUserId()
	 * @see #clearLocalUserId()
	 * @see #getUserId()
     */
    public String getLocalUserId()
    {
    	return getLocalUserId(getPreferencesKey());
    }

    /**
     * Called to clear the local user Id such that subsequent requests to getUserId() will
     * return the parent connection's default user Id. Sets the user Id attribute for this
     * subsystem to null.
     * <p>
	 * @see org.eclipse.rse.core.model.IHost#getDefaultUserId()
	 * @see #getUserId()
	 * @see #getLocalUserId()
     */
    public void clearLocalUserId()
    {
    	if (previousUserIdKey != null)
     	  RSEPreferencesManager.clearUserId(previousUserIdKey);    	
    	IConnectorService system = getConnectorService();
    	if (system != null)
    	  system.clearCredentials();
    }

    /**
     * This is a helper method you can call when performing actions that must be certain there
     * is a connection. If there is no connection it will attempt to connect, and if that fails
     * will throw a SystemMessageException you can easily display to the user by using a method
     * in it.
     */
    public void checkIsConnected() throws SystemMessageException
    {
        if (!isConnected())
        {
			try
			{
				Display display = Display.getCurrent();
				if (display != null)
				{
					// deduce active shell from display
					Shell shell = display.getActiveShell();
					if (shell != null && !shell.isDisposed())
					{
						connect();
					}
					else
					{
						connect();
					}
				}
				else
				{
					// Not on UI-thread
					connect();
				}				
            }
            catch (Exception e)
            {
                if (e instanceof SystemMessageException)
                {
                    throw (SystemMessageException) e;
                }
                else
                    if (e instanceof InterruptedException)
                    {
                        SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_CANCELLED);
                        msg.makeSubstitution(getHost().getAliasName());
                        throw new SystemMessageException(msg);
                    }
                    else
                    {
                        SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED);
                        throw new SystemMessageException(msg);
                    }
            }
        }
    }

    /**
	 * This is the method required by the IAdaptable interface.
	 * Given an adapter class type, return an object castable to the type, or
	 *  null if this is not possible.
	 * <p>
	 * By default this returns Platform.getAdapterManager().getAdapter(this, adapterType);
	 * This in turn results in the default subsystem adapter SystemViewSubSystemAdapter,
	 * in package org.eclipse.rse.ui.view.
	 */
    public Object getAdapter(Class adapterType)
    {
   	    return Platform.getAdapterManager().getAdapter(this, adapterType);	
    }    

	/**
	 * Return the system profile object this subsystem is associated with.
	 */
	public ISystemProfile getSystemProfile()
	{
		if (_host != null)
		  return _host.getSystemProfile();
		else
		  return null;
	}

	/**
	 * Return the name of system profile object this subsystem is associated with.
	 */
	public String getSystemProfileName()
	{
		ISystemProfile profile = getSystemProfile();
		if (profile != null)
		  return profile.getName();
		else
		  return null;
	}

	/**
	 * Return the connection object this subsystem is associated with.
	 */
	public IHost getHost()
	{
		return _host;
	}

	/**
	 * Return the name of system connection object this subsystem is associated with.
	 */
	public String getHostAliasName()
	{
		IHost connection = getHost();
		if (connection != null)
		  return connection.getAliasName();
		else
		  return null;
	}

	/**
	 * Return the owning filter pool that is unique to this provider.
	 * From SystemFilterPoolReferenceManagerProvider interface. We map to 
	 * a call to {@link #getConnectionPrivateFilterPool(boolean)}.
	 */
	public ISystemFilterPool getUniqueOwningSystemFilterPool(boolean createIfNotFound)
	{
		return getConnectionPrivateFilterPool(createIfNotFound);
	}
	
	/**
	 * Find or create a new filter pool, unique to this subsystem's connection. This will be 
	 * in the same profile as the connection and it will follow a naming convention that ties
	 * it to the connection.
	 * @param createIfNotFound - true to create the pool if it doesn't exist
	 * @return the filter pool that was found or created
	 */
	public ISystemFilterPool getConnectionPrivateFilterPool(boolean createIfNotFound) {
		ISystemFilterPool pool = null;
		ISubSystemConfiguration config = getSubSystemConfiguration();
		ISystemProfile profile = getSystemProfile();
		ISystemFilterPoolManager fpm = config.getFilterPoolManager(profile);
		ISystemFilterPool[] allPoolsInProfile = fpm.getSystemFilterPools();
		IHost host = getHost();
		String hostName = host.getAliasName();
		if (allPoolsInProfile != null) {
			for (int idx = 0; idx < allPoolsInProfile.length; idx++) {
				String poolOwnerName = allPoolsInProfile[idx].getOwningParentName();
				if ((poolOwnerName != null) && (poolOwnerName.equals(hostName))) {
					pool = allPoolsInProfile[idx];
				}
			}
		}
		if ((pool == null) && createIfNotFound) {
			try {
				String profileName = profile.getName();
				pool = fpm.createSystemFilterPool(getConnectionOwnedFilterPoolName(profileName, hostName), true); // true=>is deletable by user
				if (pool != null) {
					pool.setNonRenamable(true);
					pool.setOwningParentName(hostName);
					pool.commit();
					ISystemFilterPoolReferenceManager fprm = getSystemFilterPoolReferenceManager();
					if (fprm.getReferenceToSystemFilterPool(pool) == null) {
						fprm.addReferenceToSystemFilterPool(pool);
					}
				}
			} catch (Exception exc) {
				SystemBasePlugin.logError("Error creating connection-private filter pool for connection: " + hostName, exc); //$NON-NLS-1$
			}
		}
		return pool;
	}
	
	/**
	 * Constructs the name of a connection specific filter pool from its parts.
	 * @param profileName the name of the profile that contains this filter pool.
	 * @param connectionName the name of the connection the "owns" this filter pool.
	 * @return the name for the connection-owned filter pool. 
	 */
	public String getConnectionOwnedFilterPoolName(String profileName, String connectionName) {
		/*
		 * Need to keep this name short and not translatable 
		 * since it names a team sharable resource. Not qualified by the profile
		 * name since that is implicit by being in a profile.
		 */
		String name = "CN-" + connectionName;  //$NON-NLS-1$
		return name;
	}

	// -------------------------
	// Filter Testing Methods...
	// -------------------------
	/**
	 * Override this method if you support typed filters. Given an absolute remote object name,
	 *  you can test if this filter's type matches that of the remote object. This is called as
	 *  a pre-test in the following methods.
	 */
	protected boolean doesFilterTypeMatch(ISystemFilter filter, String remoteObjectAbsoluteName)
	{
		return true;
	}

    /**
     * Return true if the given filter lists the contents of the given remote object.
     *  For example, if given a folder, return true if any of the filter strings in this filter 
     *  lists the contents of that folder. Used in impact analysis when a remote object is 
     *  created, deleted, renamed, copied or moved, so as to establish which filters need to be
     *  refreshed or collapsed (if the folder is deleted, say).
     * <p>
     * Subclasses do not need to override this method. Rather, this method walks each
     *  filter string and calls doesFilterStringListContentsOf(...), and that is the method
     *  that child classes must override.
     */
    public boolean doesFilterListContentsOf(ISystemFilter filter, String remoteObjectAbsoluteName)
    {
    	if (filter.isPromptable()) // || !doesFilterTypeMatch(filter, remoteObjectAbsoluteName))
    	  return false;
    	boolean would = false;
    	ISystemFilterString[] strings = filter.getSystemFilterStrings();
    	if (strings != null)
    	  for (int idx=0; !would && (idx<strings.length); idx++)
    	     would = doesFilterStringListContentsOf(strings[idx], remoteObjectAbsoluteName);
    	return would;
    }
    /**
     * Return true if the given filter string lists the contents of the given remote object.
     *  For example, if given a folder, return true if the given filter string
     *  lists the contents of that folder. Used in impact analysis when a remote object is 
     *  created, deleted, renamed, copied or moved, so as to establish which filters need to be
     *  refreshed or collapsed (if the folder is deleted, say).
     * <p>
     * This should only return true if the filter string directly lists the contents of the given
     *  object, versus indirectly.
     * <p>
     * Subclasses should override this.
     */
    public boolean doesFilterStringListContentsOf(ISystemFilterString filterString, String remoteObjectAbsoluteName)
    {
    	return false;
    }

    /**
     * Return true if the given remote object name will pass the filtering criteria for any of 
     *  the filter strings in this filter.
     * <p>
     * Subclasses do not need to override this method. Rather, this method walks each
     *  filter string and calls doesFilterStringMatch(...), and that is the method
     *  that child classes must override.
     */
    public boolean doesFilterMatch(ISystemFilter filter, String remoteObjectAbsoluteName)
    {
    	if (filter.isPromptable() || !doesFilterTypeMatch(filter, remoteObjectAbsoluteName))
    	  return false;
    	boolean would = false;

    	String[] strings = filter.getFilterStrings();
    	if (strings != null)
    	{
    	  for (int idx=0; !would && (idx<strings.length); idx++)
    	  {
    	  	 if (strings[idx].equals("*")) //$NON-NLS-1$
    	  	   would = true;
    	  	 else if (strings[idx].equals("./*")) //$NON-NLS-1$
    	  	   would = true;
    	  	 else
    	       would = doesFilterStringMatch(strings[idx], remoteObjectAbsoluteName, filter.areStringsCaseSensitive());
    	  }
    	}
    	return would;
    }
    /**
     * Return true if the given remote object name will pass the filtering criteria for 
     *  the given filter string.
     * <p>
     * Subclasses need to override this.
     * If in doubt, return true.
     */
    public boolean doesFilterStringMatch(String filterString, String remoteObjectAbsoluteName, boolean caseSensitive)
    {
    	return true;
    }

	// -------------------------------------
	// GUI methods 
	// -------------------------------------
	/**
	 * Return the single property page to show in the tabbed notebook for the
	 *  for SubSystem property of the parent Connection. Return null if no 
	 *  page is to be contributed for this. You are limited to a single page,
	 *  so you may have to compress. It is recommended you prompt for the port
	 *  if applicable since the common base subsystem property page is not shown
	 *  To help with this you can use the SystemPortPrompt widget.
	 */
    public PropertyPage getPropertyPage(Composite parent)
    {
    	return null;
    }
    
	// --------------------------------------------------------------------------------------
	// Methods for encoding and decoding remote objects for drag and drop, and clipboard copy
	// --------------------------------------------------------------------------------------
		
	
	/**
	 * For drag and drop, and clipboard, support of remote objects.
	 * <p>
	 * Return the object within the subsystem that corresponds to
	 * the specified unique ID.  Because each subsystem maintains it's own
	 * objects, it's the responsability of the subsystem to determine
	 * how an ID (or key) for a given object maps to the real object.
	 * By default this returns null. 
	 * <p>
	 * This is the functional opposite of {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter#getAbsoluteName(Object)}.
	 */
	public Object getObjectWithAbsoluteName(String key) throws Exception
	{
		// by default, the subsystem will attempt to find a filter reference for the key
		Object filterRef = getFilterReferenceWithAbsoluteName(key);
		if (filterRef != null)
		{
			return filterRef;
		}
		return null;	
	}
	
	/**
	 * Return the filter reference that corresponds to the specified key.  If there
	 * is no such filter reference, return null;
	 * @param key the absolute name for an object.
	 * @return a filter reference if there is one matching the key
	 */
	protected Object getFilterReferenceWithAbsoluteName(String key)
	{
		//		figure out if there is a filter
		String filterID = key;
		try
		{
			ISystemFilterPoolReferenceManager filterMgr = getFilterPoolReferenceManager();
			ISystemRegistry registry = RSEUIPlugin.getTheSystemRegistry();
			ISubSystemConfiguration factory = registry.getSubSystemConfiguration(this);

			int indexOfDot = filterID.indexOf('.');
			if (indexOfDot > 0)
			{
				String mgrName = filterID.substring(0, indexOfDot);

				ISystemFilterPoolManager mgr = factory.getSystemFilterPoolManager(mgrName);

				int indexOfDot2 = filterID.indexOf('.', indexOfDot + 1);
				if (mgr != null && indexOfDot2 > 0)
				{
					String filterPoolName = filterID.substring(indexOfDot + 1, indexOfDot2);

					ISystemFilterPool filterPool = mgr.getSystemFilterPool(filterPoolName);

					String filterName = filterID.substring(indexOfDot2 + 1, filterID.length());
					if (filterPool != null)
					{
						ISystemFilter filter = filterPool.getSystemFilter(filterName);
						ISystemFilterReference ref = filterMgr.getSystemFilterReference(this, filter);
					    if (ref != null)
						{
							// if we get here, it's a filter
							return ref;
						}
					}
				 }
			}
		}
		catch (Exception e)
		{
		}
		return null;
	}
	
    
	// ---------------------------------------------------
	// Methods for business partners to add their own 
	//  persisted attributes to the subsystem object...
	// ---------------------------------------------------    
	
	/**
	 * @deprecated use property sets directly now
	 */
	public void setVendorAttribute(String vendor, String attributeName, String attributeValue)
	{
		IPropertySet set = getPropertySet(vendor);
		if (set == null)
		{
			set = createPropertySet(vendor, ""); //$NON-NLS-1$
		}
		set.addProperty(attributeName, attributeValue);
	}
	/**
	 * @deprecated use property sets directly now
	 */
	public String getVendorAttribute(String vendor, String attributeName)
	{
		IPropertySet set = getPropertySet(vendor);
		if (set != null)
		{
			return set.getPropertyValue(attributeName);
		}
		else
		{
			return null;
		}
	} 

 
	// ---------------------------------------------------
	// Methods for ISVs to add their own 
	// persisted attributes to the subsystem object...
	// ---------------------------------------------------    
	
	/**
	 * @deprecated
	 */
	public void setRemoteAttribute(String attributeName, String attributeValue)
	{
		IPropertySet set = getPropertySet("Remote"); //$NON-NLS-1$
		if (set == null)
		{
			set = createPropertySet("Remote", getDescription()); //$NON-NLS-1$
		}
		set.addProperty(attributeName, attributeValue);
	}
	/**
	 * @deprecated 
	 */
	public String getRemoteAttribute(String attributeName)
	{
		IPropertySet set = getPropertySet("Remote"); //$NON-NLS-1$
		if (set != null)
		{
			return set.getPropertyValue(attributeName);
		}
		else
		{
			return null;
		}
	}    
	
	
	
	
	
    // ------------------
    // Utility methods...
    // ------------------
    
    /**
     * Return the system type for this connection.
     * FIXME Return an IRSESystemType instead
     * @deprecated
     */
    public String getSystemType()
    {
    	IHost conn = getHost();
    	if (conn == null || conn.getSystemType()==null)
    	  return null;
    	else
    	  return conn.getSystemType().getName();
    }
    /**
     * Return the host name for the connection this system's subsystem is associated with
     */
    public String getHostName()
    {
    	IHost conn = getHost();
    	if (conn == null)
    	  return null;
    	else
    	  return conn.getHostName();
    }

	/**
	 * Display message on message thread
	 */
	protected void displayAsyncMsg(SystemMessageException msg)
	{
		DisplayErrorMessageJob job = new DisplayErrorMessageJob(getShell(), msg);
		job.setPriority(Job.INTERACTIVE);
		job.setSystem(true);
		job.schedule();
	}
	/**
	 * Nested class which extends WorkbenchJob to allow us to show an error message, which is a GUI operation,
	 *  from a non-GUI thread. This is done by creating an instance of this class and then scheduling it.
	 */
	public static class DisplayErrorMessageJob extends WorkbenchJob 
	{
		private Shell shell;
		private SystemMessageException msgExc;
		
		/**
		 * Constructor
		 */
		public DisplayErrorMessageJob(Shell shell, SystemMessageException msgExc)
		{
			super(""); //$NON-NLS-1$
			this.shell = shell; //FIXME remove this
			this.msgExc = msgExc;
		}
		
		/**
		 * @see UIJob#run(IProgressMonitor monitor)
		 */
		public IStatus runInUIThread(IProgressMonitor monitor) 
		{
			if ((shell != null) && (shell.isDisposed() || !shell.isEnabled() || !shell.isVisible()))
			  shell = null;
			if (shell == null)
			{
		        Shell[] shells = Display.getCurrent().getShells();
				for (int i = 0; i < shells.length && shell == null; i++)
					if (!shells[i].isDisposed() && shells[i].isVisible() && shells[i].isEnabled())
						shell = shells[i];
			}
			if (shell != null)
				SystemMessageDialog.displayMessage(shell, msgExc);
			return Status.OK_STATUS;
		}		
	}
	
	// ---------------------------------------------------
	// SystemFilterPoolReferenceManagerProvider methods...
	// ---------------------------------------------------
	/**
	 * Return the system filter pool reference manager, as per the
	 * interface SystemFilterPoolReferenceManagerProvider
	 */
	public ISystemFilterPoolReferenceManager getSystemFilterPoolReferenceManager()
	{
		return getFilterPoolReferenceManager();
	}	
    /*
     * Helper method to fire an event...
     */
    protected void fireEvent(SystemResourceChangeEvent event)
    {
    	RSEUIPlugin.getTheSystemRegistry().fireEvent(event);
    }
    /*
     * Helper method to fire a reference event...
     */
    protected void fireEvent(SystemResourceChangeEvent event, Object grandParent)
    {
    	event.setGrandParent(grandParent);
    	RSEUIPlugin.getTheSystemRegistry().fireEvent(event);
    }
    /*
     * Helper method to create and then fire an event...
     */
    protected void fireEvent(Object src, int eventId, Object parent)
    {    	
    	fireEvent(new SystemResourceChangeEvent(src, eventId, parent));
    } 	
    /*
     * Helper method to create and then fire an event...
     */
    protected void fireEvent(Object[] multiSrc, int eventId, Object parent)
    {    	
    	fireEvent(new SystemResourceChangeEvent(multiSrc, eventId, parent));
    } 	
    /*
     * Helper method to create and then fire an event...
     */
    protected void fireEvent(Object[] src, int eventId, Object parent, int position)
    {
    	SystemResourceChangeEvent event = new SystemResourceChangeEvent(src, eventId, parent);
    	event.setPosition(position);
    	fireEvent(event);
    } 	

    /*
     * Helper method to create and then fire a reference event...
     */
    protected void fireEvent(Object src, int eventId, Object parent, Object grandParent)
    {    	
    	fireEvent(new SystemResourceChangeEvent(src, eventId, parent), grandParent);
    } 	
    // -------------------------------
    // FILTER POOL REFERENCE EVENTS...
    // -------------------------------

    /**
	 * A new filter pool reference has been created. Fire the appropriate events for this.
	 */
	public void filterEventFilterPoolReferenceCreated(ISystemFilterPoolReference newPoolRef) {
		if (getSubSystemConfiguration().showFilterPools()) {
			fireEvent(newPoolRef, EVENT_ADD, this);
			fireEvent(newPoolRef, EVENT_REVEAL_AND_SELECT, this);
		} else {
			ISystemFilterPool pool = newPoolRef.getReferencedFilterPool();
			if (pool != null && pool.getSystemFilterCount() > 0) {
				ISystemFilterReference[] filterRefs = newPoolRef.getSystemFilterReferences(this);
				fireEvent(filterRefs, EVENT_ADD_MANY, this, -1); // -1 means append to end
			}
		}
		try {
			getSubSystemConfiguration().saveSubSystem(this);
			RSEUIPlugin.getTheSystemRegistry().fireModelChangeEvent(ISystemModelChangeEvents.SYSTEM_RESOURCE_ADDED, ISystemModelChangeEvents.SYSTEM_RESOURCETYPE_FILTERPOOLREF, newPoolRef, null);
		} catch (Exception exc) {
			SystemBasePlugin.logError("Error saving subsystem " + getName(), exc); //$NON-NLS-1$
		}
	}

	/**
     * A filter pool reference has been deleted
     */
    public void filterEventFilterPoolReferenceDeleted(ISystemFilterPoolReference filterPoolRef)
    {
    	if (getSubSystemConfiguration().showFilterPools())
    	{
    	  	fireEvent(filterPoolRef, EVENT_DELETE, this);    		
    	}
    	else if (filterPoolRef.getReferencedFilterPool().getSystemFilterCount()>0)
    	{    	
    	  	ISystemFilterReference[] filterRefs = filterPoolRef.getSystemFilterReferences(this);
    	  	fireEvent(filterRefs, EVENT_DELETE_MANY, this);
    	}

        try {
           getSubSystemConfiguration().saveSubSystem(this);
		   // fire model change event in case any BP code is listening...
		   RSEUIPlugin.getTheSystemRegistry().fireModelChangeEvent(ISystemModelChangeEvents.SYSTEM_RESOURCE_REMOVED, ISystemModelChangeEvents.SYSTEM_RESOURCETYPE_FILTERPOOLREF, filterPoolRef, null);		
        }
        catch (Exception exc)
        {
           SystemBasePlugin.logError("Error saving subsystem "+getName(),exc); //$NON-NLS-1$
        }
    }
    /**
     * A single filter pool reference has been reset to reference a new pool
     */
    public void filterEventFilterPoolReferenceReset(ISystemFilterPoolReference filterPoolRef)
    {
    	fireEvent(filterPoolRef, EVENT_PROPERTYSHEET_UPDATE, this); // we assume its a move operation so little impact
        try {
           getSubSystemConfiguration().saveSubSystem(this);
		   // fire model change event in case any BP code is listening...
		   RSEUIPlugin.getTheSystemRegistry().fireModelChangeEvent(ISystemModelChangeEvents.SYSTEM_RESOURCE_CHANGED, ISystemModelChangeEvents.SYSTEM_RESOURCETYPE_FILTERPOOLREF, filterPoolRef, null);		
        }
        catch (Exception exc)
        {
           SystemBasePlugin.logError("Error saving subsystem "+getName(),exc); //$NON-NLS-1$
        }    	
    }
    /**
     * All filter pool references has been reset. Happens after SelectFilterPools dialog
     */
    public void filterEventFilterPoolReferencesReset()
    {
    	fireEvent(this, EVENT_CHANGE_CHILDREN, this);
        try 
        {
           	getSubSystemConfiguration().saveSubSystem(this);           
           	ISystemFilterPoolReference[] poolRefs = getFilterPoolReferenceManager().getSystemFilterPoolReferences();
           	for (int idx=0; idx<poolRefs.length; idx++)
		   		RSEUIPlugin.getTheSystemRegistry().fireModelChangeEvent(ISystemModelChangeEvents.SYSTEM_RESOURCE_CHANGED, ISystemModelChangeEvents.SYSTEM_RESOURCETYPE_FILTERPOOLREF, poolRefs[idx], null);           
        }
        catch (Exception exc)
        {
           	SystemBasePlugin.logError("Error saving subsystem "+getName(),exc); //$NON-NLS-1$
        }
    }
    /**
     * A filter pool reference has been renamed (ie, its reference filter pool renamed)
     */
    public void filterEventFilterPoolReferenceRenamed(ISystemFilterPoolReference poolRef, String oldName)
    {
    	if (getSubSystemConfiguration().showFilterPools())
    	{
    	  	fireEvent(poolRef, EVENT_RENAME, this);
    	}
    	else
    	{
    	  	//fireEvent(filterPoolRef, EVENT_PROPERTYSHEET_UPDATE, this);
    	}
        try {
           	getSubSystemConfiguration().saveSubSystem(this);
        }
        catch (Exception exc)
        {
           	SystemBasePlugin.logError("Error saving subsystem "+getName(),exc); //$NON-NLS-1$
        }
    }
    /**
     * One or more filter pool references have been re-ordered within their manager
     */
    public void filterEventFilterPoolReferencesRePositioned(ISystemFilterPoolReference[] poolRefs, int delta)
    {
    	fireEvent(poolRefs, EVENT_MOVE_MANY, this, delta);    	
        try {
           getSubSystemConfiguration().saveSubSystem(this);
		   // fire model change event in case any BP code is listening...
		   for (int idx=0; idx<poolRefs.length; idx++)
		   	RSEUIPlugin.getTheSystemRegistry().fireModelChangeEvent(ISystemModelChangeEvents.SYSTEM_RESOURCE_REORDERED, ISystemModelChangeEvents.SYSTEM_RESOURCETYPE_FILTERPOOLREF, poolRefs[idx], null);		
        }
        catch (Exception exc)
        {
           SystemBasePlugin.logError("Error saving subsystem "+getName(),exc); //$NON-NLS-1$
        }
    }
    // -------------------------------
    // FILTER REFERENCE EVENTS...
    // -------------------------------
    /**
     * A new filter has been created. This is called when a filter pool reference is selected and a new filter
     *  is created, so that the provider can expand the selected filter pool reference and reveal the new filter
     *  within the selected pool reference.
     * <p>
     * Only the selected node should be expanded if not already. All other references to this pool will already
     * have been informed of the new addition, and will have refreshed their children but not expanded them.
     */
    public void filterEventFilterCreated(Object selectedObject, ISystemFilter newFilter)
    {    	
    	fireEvent(newFilter,EVENT_REVEAL_AND_SELECT,selectedObject);
    }
    // ---------------------------------
    // FILTER STRING REFERENCE EVENTS...
    // ---------------------------------
    /**
     * A new filter string has been created. This is called when a filter reference is selected and a new filter
     *  string is created, so that the provider can expand the selected filter reference and reveal the new filter
     *  string within the selected filter reference.
     * <p>
     * Only the selected node should be expanded if not already. All other references to this filter will already
     * have been informed of the new addition, and will have refreshed their children but not expanded them.
     */
    public void filterEventFilterStringCreated(Object selectedObject, ISystemFilterString newFilterString)
    {
    	fireEvent(newFilterString,EVENT_REVEAL_AND_SELECT,selectedObject);
    }

    /**
     * Represents an operation that can be performed by the subsystem. Since this class
     * extends Job, it is run on a separate thread, but reports progress to the Main thread.
     * Takes care of some common error handling and Status creation for SubSystem Operations.
     * @author mjberger
     */
    protected abstract class SubSystemOperationJob extends Job
    {
    	protected Object[] runOutputs;
    	protected String[] runOutputStrings;
    	protected boolean  _hasStarted = false;
    	
    	public class ContextRunnable implements IRunnableWithProgress
    	{
    		private SubSystemOperationJob _job;
    		private IStatus _status;
    		
    		public ContextRunnable(SubSystemOperationJob job)
    		{
    			_job = job;
    		}
    		
    		public void run(IProgressMonitor monitor)
    		{
    			_status = _job.run(monitor);  		
    		}
    		
    		public IStatus getStatus()
    		{
    			return _status;
    		}
    	}
    	
    	public SubSystemOperationJob(String operationName)
    	{
    		super(operationName + " (" + GenericMessages.RSESubSystemOperation_message + ")");    	 //$NON-NLS-1$ //$NON-NLS-2$
    	}

    	/**
    	 * Override this method with the actual operation performed by your subsystem operation. Make sure to
    	 * report progress to the Progress monitor.
    	 * @throws InterruptedException if the user presses cancel
    	 * @throws InvocationTargetException if there is some error performing the operation
    	 * @throws Exception if there is some other error
    	 */
    	public abstract void performOperation(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException, Exception;
    	
    	/**
    	 * SubSystemOperationJobs are designed to be run synchronously - if you require output from them. Clients can query any output
    	 * using getOutputs() or getOutputStrings().
    	 */
    	public Object[] getOutputs()
    	{
    		return runOutputs;
    	}

    	/**
    	 * SubSystemOperationJobs are designed to be run synchronously - if you require output from them. Clients can query any output
    	 * using getOutputs() or getOutputStrings().
    	 */
    	public String[] getOutputStrings()
    	{
    		return runOutputStrings;
    	}
    	
    	public IStatus runInContext(IRunnableContext context)
    	{
    		_hasStarted = true;
    		ContextRunnable conRun = new ContextRunnable(this);
    		try
    		{
    			context.run(false, true, conRun);
    			return conRun.getStatus();
    		}
    		catch (Exception e)
    		{  
          	  	String excMsg = e.getMessage();
          	  	if ((excMsg == null) || (excMsg.length()==0))
          	  		excMsg = "Exception " + e.getClass().getName(); //$NON-NLS-1$
          	  	SystemMessage sysMsg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_OPERATION_FAILED);
          	  	sysMsg.makeSubstitution(excMsg);
          	  	return new Status(IStatus.ERROR, RSEUIPlugin.PLUGIN_ID, IStatus.OK, sysMsg.getLevelOneText(), e);
    		}
    	}
    	
    	public boolean hasStarted()
    	{
    		return _hasStarted;
    	}
    	
    	public IStatus run(IProgressMonitor monitor)
    	{
    		_hasStarted = true;
    		try
    		{
	    		performOperation(monitor); 
    			
    	    	if (monitor.isCanceled()) // sometimes our kids don't do this like they should!
    	    	{
    	    		return Status.CANCEL_STATUS;
    	    	}
    	    	monitor.done();
    	    	return Status.OK_STATUS;
    		}
            catch(java.lang.InterruptedException exc)
            {
               monitor.done();
               return Status.CANCEL_STATUS;
            }						
            catch(InvocationTargetException exc)
            {
            	//exc.printStackTrace();
            	monitor.done();
          	  	String excMsg = exc.getTargetException().getMessage();
          	  	if ((excMsg == null) || (excMsg.length()==0))
          	  		excMsg = "Exception " + exc.getTargetException().getClass().getName(); //$NON-NLS-1$
          	  	return new Status(IStatus.ERROR, RSEUIPlugin.PLUGIN_ID, IStatus.OK, excMsg, exc.getTargetException());
            }
            catch(Exception exc)
            {
                monitor.done();
          	  	String excMsg = exc.getMessage();
          	  	if ((excMsg == null) || (excMsg.length()==0))
          	  		excMsg = "Exception " + exc.getClass().getName(); //$NON-NLS-1$
          	  	SystemMessage sysMsg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_OPERATION_FAILED);
          	  	sysMsg.makeSubstitution(excMsg);
          	  	return new Status(IStatus.ERROR, RSEUIPlugin.PLUGIN_ID, IStatus.OK, sysMsg.getLevelOneText(), exc);
           }            
    	}
    }

    /**
     * Represents the subsystem operation of resolving absolute filter strings.
     */
    protected class ResolveAbsoluteJob extends SubSystemOperationJob
    {
    	protected String _filterString;
    	
    	/**
    	 * Create a new ResolveAbsoluteJob
    	 * @param filterString the absolute filter string to resolve 
    	 */
    	public ResolveAbsoluteJob(String filterString)
    	{
    		super(GenericMessages.RSESubSystemOperation_Resolve_filter_strings_message);
    		_filterString = filterString;
    	}
    	
    	public void performOperation(IProgressMonitor mon) throws InterruptedException, InvocationTargetException, Exception
    	{
    		String msg = null;
    		int totalWorkUnits = IProgressMonitor.UNKNOWN;
    		
    	    msg = getResolvingMessage(_filterString);
    	    
    	    if (!implicitConnect(false, mon, msg, totalWorkUnits)) throw new Exception(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED).makeSubstitution(getHostName()).getLevelOneText());
    	    runOutputs = internalResolveFilterString(mon, _filterString);  		
    	}
    }

    /**
     * Represents the subsystem operation of resolving a set of absolute filter strings.
     */
    protected class ResolveAbsolutesJob extends SubSystemOperationJob
    {
    	protected String _filterString;
    	protected String[] _filterStrings;
    	
    	/**
    	 * Create a new ResolveAbsolutesJob
    	 * @param filterStrings the set of absolute filter strings to resolve 
    	 */
    	public ResolveAbsolutesJob(String filterString, String[] filterStrings)
    	{
    		super(GenericMessages.RSESubSystemOperation_Resolve_filter_strings_message);
    		_filterString = filterString;
    		_filterStrings = filterStrings;
    	}
    	
    	public void performOperation(IProgressMonitor mon) throws InterruptedException, InvocationTargetException, Exception
    	{
    		String msg = null;
    		int totalWorkUnits = IProgressMonitor.UNKNOWN;
    		
    	    msg = getResolvingMessage(_filterString);

    	    if (!implicitConnect(false, mon, msg, totalWorkUnits)) throw new Exception(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED).makeSubstitution(getHostName()).getLevelOneText());
    	    runOutputs = internalResolveFilterStrings(mon, _filterStrings);
    	}
    }

    /**
     * Represents the subsystem operation of resolving relative filter strings.
     */
    protected class ResolveRelativeJob extends SubSystemOperationJob
    {
    	protected String _filterString;
    	protected Object _parent;

    	/**
    	 * Create a new ResolveRelativeJob
    	 * @param filterString the relative filter string to resolve
    	 * @param parent the object within which the relative filter string will be resolved
    	 */
    	public ResolveRelativeJob(String filterString, Object parent)
    	{
    		super(GenericMessages.RSESubSystemOperation_Resolve_filter_strings_message);
    		_filterString = filterString;
    		_parent = parent;
    	}
    	
    	public void performOperation(IProgressMonitor mon) throws InterruptedException, InvocationTargetException, Exception
    	{
    		String msg = null;
    		int totalWorkUnits = IProgressMonitor.UNKNOWN;
    		
    	    if (_filterString == null)
    	    {
    	    	// DKM - we shouldn't be using parent context for filter strings because 
    	    	// now we have multiple contexts for the same resources
    	    	_filterString = "*"; //$NON-NLS-1$
    	    }
    	    msg = getResolvingMessage(_filterString);

    	    if (!implicitConnect(false, mon, msg, totalWorkUnits)) throw new Exception(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED).makeSubstitution(getHostName()).getLevelOneText());
    	    runOutputs = internalResolveFilterString(mon, _parent, _filterString);
    	}
    }

    /**
     * Represents the subsystem operation of getting a property value from a remote object.
     */
    protected class GetPropertyJob extends SubSystemOperationJob
    {
    	protected Object _subject;
    	protected String _key;

    	/**
    	 * Constructs a new GetPropertyJob
    	 * @param subject The object whose property will be queried
    	 * @param key The property to query
    	 */
    	public GetPropertyJob(Object subject, String key)
    	{
    		super(GenericMessages.RSESubSystemOperation_Get_property_message);
    		_subject = subject;
    		_key = key;
    	}
    	
    	public void performOperation(IProgressMonitor mon) throws InterruptedException, InvocationTargetException, Exception
    	{
    		String msg = null;	
    		int totalWorkUnits = IProgressMonitor.UNKNOWN;
    		
    	    msg = getQueryingMessage(_key);
    	    
    	    if (!implicitConnect(false, mon, msg, totalWorkUnits)) throw new Exception(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED).makeSubstitution(getHostName()).getLevelOneText());   		
    	    runOutputStrings = new String[] {internalGetProperty(mon, _subject, _key)};
    	}
    }

    /**
     * Represents the subsystem operation of setting a property of a remote object.
     */
    protected class SetPropertyJob extends SubSystemOperationJob
    {
    	protected Object _subject;
    	protected String _key;
    	protected String _value;
    	
    	/**
    	 * Constructs a new SetPropertyJob
    	 * @param subject the object whose property is to be set
    	 * @param key the property to set
    	 * @param value the new value for the property
    	 */
    	public SetPropertyJob(Object subject, String key, String value)
    	{
    		super(GenericMessages.RSESubSystemOperation_Set_property_message);
    		_subject = subject;
    		_key = key;
    		_value = value;
    	}
    	
    	public void performOperation(IProgressMonitor mon) throws InterruptedException, InvocationTargetException, Exception
    	{
    		String msg = null;	
    		int totalWorkUnits = IProgressMonitor.UNKNOWN;
    	    msg = getSettingMessage(_key);

    	    if (!implicitConnect(false, mon, msg, totalWorkUnits)) throw new Exception(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED).makeSubstitution(getHostName()).getLevelOneText());    		
    	    runOutputs = new Object[] {internalSetProperty(mon, _subject, _key, _value)};
    	}
    }

    /**
     * Represents the subsystem operation of getting a set of property values from a remote object.
     */
    protected class GetPropertiesJob extends SubSystemOperationJob
    {
    	protected Object _subject;
    	protected String[] _keys;
    	
    	/**
    	 * Constructs a new GetPropertiesJob
    	 * @param subject the object on which to perform the property query
    	 * @param keys the properties to query
    	 */
    	public GetPropertiesJob(Object subject, String[] keys)
    	{
    		super(GenericMessages.RSESubSystemOperation_Get_properties_message);
    		_subject = subject;
    		_keys = keys;
    	}
    	
    	public void performOperation(IProgressMonitor mon) throws InterruptedException, InvocationTargetException, Exception
    	{
    		String msg = null;
    		int totalWorkUnits = IProgressMonitor.UNKNOWN;    		
    	    msg = getQueryingMessage();

    	    if (!implicitConnect(false, mon, msg, totalWorkUnits)) throw new Exception(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED).makeSubstitution(getHostName()).getLevelOneText());    		
    	    runOutputStrings = internalGetProperties(mon, _subject, _keys);
    	}
    }

    /**
     * Represents the subsystem operation of setting a set of properties of a remote object.
     */
    protected class SetPropertiesJob extends SubSystemOperationJob
    {
    	protected Object _subject;
    	protected String[] _keys;
    	protected String[] _values;
    	
    	/**
    	 * Constructs a new SetPropertiesJob
    	 * @param subject the object whose properties are to be set
    	 * @param keys the set of properties to set
    	 * @param values the set of new values for the properties, corresponding with <code>keys</code>
    	 */
    	public SetPropertiesJob(Object subject, String[] keys, String[] values)
    	{
    		super(GenericMessages.RSESubSystemOperation_Set_properties_message);
    		_subject = subject;
    		_keys = keys;
    		_values = values;
    	}
    	
    	public void performOperation(IProgressMonitor mon) throws InterruptedException, InvocationTargetException, Exception
    	{
    		String msg = null;
    		int totalWorkUnits = IProgressMonitor.UNKNOWN;
    		
    	    msg = getSettingMessage();

    	    if (!implicitConnect(false, mon, msg, totalWorkUnits)) throw new Exception(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED).makeSubstitution(getHostName()).getLevelOneText());    		
    	    runOutputs = new Object[] {internalSetProperties(mon, _subject, _keys, _values)};
    	}
    }

	/**
	 * Use this job to connect from a non-UI thread. Since this extends UIJob it will
	 * run on the UI thread when scheduled.
	 */
	private class ConnectFromBackgroundJob extends UIJob {
		public ConnectFromBackgroundJob() {
			super(GenericMessages.RSESubSystemOperation_Connect_message);
		}
		public IStatus runInUIThread(IProgressMonitor monitor) {
			IStatus result = Status.OK_STATUS;
			try {
				connect(false);
			} catch (InterruptedException e) {
				result = Status.CANCEL_STATUS;
			} catch (Exception e) {
				e.printStackTrace();
				String excMsg = e.getMessage();
				if ((excMsg == null) || (excMsg.length() == 0)) excMsg = "Exception " + e.getClass().getName(); //$NON-NLS-1$
				SystemMessage sysMsg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED);
				sysMsg.makeSubstitution(getHostName(), excMsg);
				result = new Status(IStatus.ERROR, RSEUIPlugin.PLUGIN_ID, IStatus.OK, sysMsg.getLevelOneText(), e);
			}
			return result;
		}
	}
	
    /**
     * Represents the subsystem operation of connecting the subsystem to the remote machine.
     */
    public class ConnectJob extends SubSystemOperationJob
    {
    	private SubSystem _ss;
    	private IRSECallback _callback;
    	
    	public ConnectJob(SubSystem ss, IRSECallback callback)
    	{
    		super(GenericMessages.RSESubSystemOperation_Connect_message);
    		_ss = ss;
    		_callback = callback;
    	}
    	
    	public void performOperation(IProgressMonitor mon) throws InterruptedException, Exception
    	{
    		String msg = null;
    		int totalWorkUnits = IProgressMonitor.UNKNOWN;
    		
            msg = SubSystemConfiguration.getConnectingMessage(getHostName(), getConnectorService().getPort());        	
            SystemBasePlugin.logInfo(msg);

    	    if (!implicitConnect(true, mon, msg, totalWorkUnits)) throw new Exception(RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED).makeSubstitution(getHostName()).getLevelOneText());    		
    	    internalConnect(mon);

    	    ISystemRegistry registry = RSEUIPlugin.getTheSystemRegistry();
			registry.connectedStatusChange(_ss, true, false);
			
			if (_callback != null)
			{
				IStatus status = Status.OK_STATUS;
				if (!isConnected())
				{
					status = Status.CANCEL_STATUS;
				}
				_callback.done(status, null);
			}
    	}
    }

    /**
     * Represents the subsystem operation of disconnecting the subsystem to the remote machine.
     */
    protected class DisconnectJob extends SubSystemOperationJob
    {
    	public class PostDisconnect implements Runnable
    	{
    		
    		public void run()
    		{
    	    	getConnectorService().reset();
    	        ISystemRegistry sr = RSEUIPlugin.getDefault().getSystemRegistry();	
    	        sr.connectedStatusChange(_subsystem, false, true, _collapseTree);
    		}	
    		
    	}
    	
    	private boolean _collapseTree;
    	private SubSystem _subsystem;
    	public DisconnectJob(boolean collapseTree, SubSystem subsystem)
    	{
    		super(GenericMessages.RSESubSystemOperation_Disconnect_message);
    		_collapseTree = collapseTree;
    		_subsystem = subsystem;
    	}
    	
    	public void performOperation(IProgressMonitor mon) throws InterruptedException, Exception
    	{
    	    internalDisconnect(mon);
    	    _disconnecting = false;
    	    _connectionError = false;
    	    Display.getDefault().asyncExec(new PostDisconnect());
    	}
    	
    }

    /**
     * Represents the operation of changing the connection status of this subsystem.
     */
    protected class ChangeStatusJob extends UIJob
    {
    	private final ISubSystem _subsystem;
    	
    	public ChangeStatusJob(ISubSystem ss)
    	{
    		super(GenericMessages.RSESubSystemOperation_Notifying_registry_message);
    		_subsystem = ss;
    	}
    	
    	public IStatus runInUIThread(IProgressMonitor monitor)
    	{
			final ISystemRegistry sr = RSEUIPlugin.getDefault().getSystemRegistry();	
    		sr.connectedStatusChange(_subsystem, true, false);
    		return Status.OK_STATUS;
    	}
    }
	
    protected boolean implicitConnect(boolean isConnectOperation, IProgressMonitor mon, String msg, int totalWorkUnits) throws SystemMessageException, InvocationTargetException, InterruptedException
    {
		boolean didConnection = false;
        if ( doConnection && !isConnected())// caller wants to do connection first as part operation
        {
			if (isOffline() || (supportsCaching() && getCacheManager().isRestoreFromMemento()))
			{
				if (!supportsCaching())
				{
				   // offline and no caching support so throw exception
				   SystemMessage sMsg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_OFFLINE_CANT_CONNECT);
				   sMsg.makeSubstitution(getHost().getAliasName());
				   throw new SystemMessageException(sMsg);
				}
					
				// we are not either offline or we support caching and are restoring from memento so 
				// postpone connecting until we determine the information cannot be retrieved from the cache,
				// it is left to individual api calls to determine this           	 
				doConnection = false;
			}
        	else
        	{
        		doConnection = false; // reset
				didConnection = true;
				mon.beginTask(SubSystemConfiguration.getConnectingMessage(getHostName(), getConnectorService().getPort()), totalWorkUnits);
				internalConnect(mon);
				mon.worked(1);
				 
				ChangeStatusJob job = new ChangeStatusJob(this); 
				job.setSystem(true);
				job.setPriority(Job.INTERACTIVE);
				job.schedule();			 
        	}
        }
        else
        {
        	doConnection = false;
        }
        
        if (isConnected() || isConnectOperation || isOffline() || (supportsCaching() && getCacheManager().isRestoreFromMemento()))
        {
        	if (!didConnection)
	           	mon.beginTask(msg, (totalWorkUnits==1) ? IProgressMonitor.UNKNOWN : totalWorkUnits);
	        else
	           	mon.setTaskName(msg);
        	return true;
        }
	    else // the implicit connect must have failed
	    {
	       	mon.done();
	       	return false;
	    }    	
    }
    
	protected void showOperationMessage(Exception exc, Shell shell)
	{
		if (exc instanceof java.lang.InterruptedException)
		  showOperationCancelledMessage(shell);
		else if (exc instanceof java.lang.reflect.InvocationTargetException)
		  showOperationErrorMessage(shell, ((java.lang.reflect.InvocationTargetException)exc).getTargetException());
		else
		  showOperationErrorMessage(shell, exc);
	}
	
    /**
     * Show an error message when the connection fails.
     * Shows a common message by default.
     * Overridable.
     */
    protected void showConnectErrorMessage(Shell shell, String hostName, int port, Throwable exc)
    {
    	 SystemMessage msg = null;
    	 if (exc instanceof SystemMessageException)
    	 {
    	 	SystemBasePlugin.logError("Connection error", exc); //$NON-NLS-1$
    	 	msg = ((SystemMessageException) exc).getSystemMessage();
    	 }
    	 else if (exc instanceof java.net.UnknownHostException)
    	 {
    	   SystemBasePlugin.logError("Connection error", exc);    	 //$NON-NLS-1$
           msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_UNKNOWNHOST);
           msg.makeSubstitution(hostName);
    	 }
    	 else
    	 {
     	   SystemBasePlugin.logError("Connection error", exc); //$NON-NLS-1$
           msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_FAILED);
           msg.makeSubstitution(hostName, exc);
         }
    	 
    	 SystemMessageDialog msgDlg = new SystemMessageDialog(shell, msg);
    	 msgDlg.setException(exc);
    	 msgDlg.open();
    }	
    /**
     * Show an error message when the user cancels the connection.
     * Shows a common message by default.
     * Overridable.
     */
    protected void showConnectCancelledMessage(Shell shell, String hostName, int port)
    {
         //SystemMessage.displayMessage(SystemMessage.MSGTYPE_ERROR, shell, RSEUIPlugin.getResourceBundle(),
         //                             ISystemMessages.MSG_CONNECT_CANCELLED, hostName);
         SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_CANCELLED);
         msg.makeSubstitution(hostName);
    	 SystemMessageDialog msgDlg = new SystemMessageDialog(shell, msg);
    	 msgDlg.open();     	
    }	
    /**
     * Show an error message when the disconnection fails.
     * Shows a common message by default.
     * Overridable.
     */
    protected void showDisconnectErrorMessage(Shell shell, String hostName, int port, Exception exc)
    {
         //SystemMessage.displayMessage(SystemMessage.MSGTYPE_ERROR,shell,RSEUIPlugin.getResourceBundle(),
         //                             ISystemMessages.MSG_DISCONNECT_FAILED,
         //                             hostName, exc.getMessage()); 	
         //RSEUIPlugin.logError("Disconnect failed",exc); // temporary
    	 SystemMessageDialog msgDlg = new SystemMessageDialog(shell,
    	            RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_DISCONNECT_FAILED).makeSubstitution(hostName,exc));
    	 msgDlg.setException(exc);
    	 msgDlg.open();
    }	
    /**
     * Show an error message when the user cancels the disconnection.
     * Shows a common message by default.
     * Overridable.
     */
    protected void showDisconnectCancelledMessage(Shell shell, String hostName, int port)
    {
         //SystemMessage.displayMessage(SystemMessage.MSGTYPE_ERROR, shell, RSEUIPlugin.getResourceBundle(),
         //                             ISystemMessages.MSG_DISCONNECT_CANCELLED, hostName);
    	 SystemMessageDialog msgDlg = new SystemMessageDialog(shell,
    	            RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_DISCONNECT_CANCELLED).makeSubstitution(hostName));
    	 msgDlg.open();
    }	


    /**
     * Helper method to return the message "Resolving to &1..."
     */
    protected static String getResolvingMessage(String filterString)
    {
    	String msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_RESOLVE_PROGRESS).makeSubstitution(filterString).getLevelOneText();
    	return msg;
    }
    /**
     * Helper method to return the message "Running command &1..."
     */
    protected static String getRunningMessage(String cmd)
    {
    	return RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_RUN_PROGRESS).makeSubstitution(cmd).getLevelOneText();   		
    }
    /**
     * Helper method to return the message "Querying &1..."
     */
    protected static String getQueryingMessage(String key)
    {
    	return RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_QUERY_PROGRESS).makeSubstitution(key).getLevelOneText();   		
    }
    /**
     * Helper method to return the message "Setting &1..."
     */
    protected static String getSettingMessage(String key)
    {
    	return RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_SET_PROGRESS).makeSubstitution(key).getLevelOneText();   		
    }
    /**
     * Helper method to return the message "Querying properties..."
     */
    protected static String getQueryingMessage()
    {
    	return RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_QUERY_PROPERTIES_PROGRESS).getLevelOneText();   		
    }
    /**
     * Helper method to return the message "Setting properties..."
     */
    protected static String getSettingMessage()
    {
    	return RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_SET_PROPERTIES_PROGRESS).getLevelOneText();   		
    }

    /**
     * Show an error message when the operation fails.
     * Shows a common message by default, unless the exception is of type SystemMessageException,
     *   in which case the message is retrieved via getSystemMessage() and it is shown.
     * Overridable.
     */
    protected void showOperationErrorMessage(Shell shell, Throwable exc)
    {
    	SystemMessage sysMsg = null;
    	if (exc instanceof SystemMessageException)
    	{
    	        displayAsyncMsg((SystemMessageException)exc);
    	  //sysMsg = ((SystemMessageException)exc).getSystemMessage();
    	}
    	else
    	{
    	  String excMsg = exc.getMessage();
    	  if ((excMsg == null) || (excMsg.length()==0))
    	    excMsg = "Exception " + exc.getClass().getName(); //$NON-NLS-1$
          sysMsg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_OPERATION_FAILED);
          sysMsg.makeSubstitution(excMsg);
    	
    	
    	 SystemMessageDialog msgDlg = new SystemMessageDialog(shell, sysMsg);
         msgDlg.setException(exc);
    	 msgDlg.open();
         //RSEUIPlugin.logError("Operation failed",exc); now done successfully in msgDlg.open()
    	}
 
    }	
    /**
     * Show an error message when the user cancels the operation.
     * Shows a common message by default.
     * Overridable.
     */
    protected void showOperationCancelledMessage(Shell shell)
    {
    	SystemMessageDialog msgDlg = new SystemMessageDialog(shell, RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_OPERATION_CANCELLED));
    	msgDlg.open();
    }	


    // ----------------------------------
	// PUBLIC METHODS HANDLED FOR YOU....
	// ----------------------------------
	/**
	 * Return true if this subsystem is currently connected to its remote system.
	 * If the subsystem configuration indicates its subsystems support connecting, then
	 *  this method will call getSystem().isConnect().
	 */
	public boolean isConnected()
	{
		IConnectorService system = getConnectorService();
		if (system != null)
		  return system.isConnected();
		else if (!supportsConnecting)
		  return true;
		else
		  return false;
	}
	
	/**
	 * Returns the offline property for this subsystem's System Connection.   
	 */
	public boolean isOffline()
	{
		return getHost().isOffline();
	}
    /**
     * CHILD CLASSES MAY OVERRIDE THIS.
     * By default it returns true iff we support filters and there are filter pool references.
     */
    public boolean hasChildren()
    {
    	if (getSubSystemConfiguration().supportsFilters())
    	{
    	  ISystemFilterPoolReferenceManager mgr = getSystemFilterPoolReferenceManager();
    	  if (mgr == null)
    	    return false;
    	  else
    	    return mgr.getSystemFilterPoolReferenceCount() > 0;
    	}
    	else
    	  return false;
    }
    /**
     * Return the children of this subsystem, to populate the GUI subtree of this subsystem.
     * By default, this method:
     * <ul>
     *   <li>Returns the filter pool references of this subsystem, if supportsFilters() is true for our factory.
     *   <li>If supportsFilters() is false from our factory, returns null
     * </ul>
     * So, be sure to override this method IF you do not support filters.
     */
    public Object[] getChildren()
    {
    	if (getSubSystemConfiguration().supportsFilters())
    	{
    	  ISystemFilterPoolReferenceManager mgr = getSystemFilterPoolReferenceManager();
    	  if (mgr == null)
    	    return null;
    	  else
    	  {
    	  	boolean showFilterPools = getSubSystemConfiguration().showFilterPools();
    	  	if (showFilterPools)
    	      return mgr.getSystemFilterPoolReferences();
    	    else
    	    {
    	      ISystemFilterReference[] allRefs = mgr.getSystemFilterReferences(this);
    	      return allRefs;
    	    }
    	  }
    	}
    	else
    	  return null;
    }

    /**
     * Resolve an <i>absolute</i> filter string. This is only applicable if the subsystem
     *  factory reports true for {@link org.eclipse.rse.core.subsystems.SubSystemConfiguration#supportsFilters()}, 
     *  which is the default. Otherwise, {@link org.eclipse.rse.core.subsystems.SubSystem#getChildren()}
     *  is called when the subsystem itself is expanded.
     * <p>
     * When a user <u>expands a filter</u> this method is invoked for each filter string and the 
     *  results are concatenated and displayed to the user. You can affect the post-concatenated
     *  result by overriding {@link #sortResolvedFilterStringObjects(Object[])} if you desire to
     *  sort the result, say, or pick our redundancies.
     * <p>
     * The resulting objects are displayed in the tree in the Remote System {@link org.eclipse.rse.internal.ui.view.SystemView view}. 
     * There are <u>two requirements</u> on the returned objects:</p>
     * <ol>
     *   <li>They must implement {@link org.eclipse.core.runtime.IAdaptable}.
     *   <li>Their must be an RSE {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter remote-adapter} registered
     *        for the object's class or interface type. Further, if this subsystem is {@link org.eclipse.rse.core.subsystems.SubSystem#isHidden() visible}
     *        in the RSE, which is the default, then there must also be an RSE {@link org.eclipse.rse.ui.view.ISystemViewElementAdapter GUI-adapter} registered
     *        with the platform. The base class implementation of this interface is {@link org.eclipse.rse.ui.view.AbstractSystemViewAdapter}.
     * </ol>
     * <p>A good place to start with your remote-resource classes to subclasss {@link org.eclipse.rse.core.subsystems.AbstractResource}, as it
     * already implements IAdaptable, and maintains a reference to this owning subsystem, which helps when 
     * implementing the {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter remote-adapter}.
     * <p>
     * Be sure to register your adapter factory in your plugin's startup method.
     * <p>
     * <b>You do not need to override this, as it does the progress monitor and error message
     *  displaying for you. Just override internalResolveFilterString.</b>
     * <p>
     * @param filterString filter pattern for objects to return.
     * @return the results of resolving the filter string. 
     * 
     * @deprecated use resolveFilterString(IProgressMonitor monitor, String filterString) instead
     */
    public Object[] resolveFilterString(String filterString)
           throws Exception
    {
        boolean ok = true;
    	if (!isConnected())
    	  ok = promptForPassword();
        if (ok)
        {
        		return internalResolveFilterString(new NullProgressMonitor(), filterString);
        }
        else
        {
        	return null;
        }
    }
    /**
     * Resolve multiple absolute filter strings. This is only applicable if the subsystem
     *  factory reports true for supportsFilters().
     * <p>
     * This is the same as {@link #resolveFilterString(String)} but takes an array of
     *  filter strings versus a single filter string.
     * <p>
     * The default implementation of this simply calls {@link #internalResolveFilterStrings(IProgressMonitor, String[])}.
     * <p>
     * After successful resolve, the sort method is called to sort the concatenated results before
     *  returning them.
     *
     * @param filterStrings array of filter patterns for objects to return.
     * @return Array of objects that are the result of resolving all the filter strings
     * 
     * @deprecated should use resolveFilterStrings(IProgressMonitor monitor, String[] filterStrings) instead
     */
    public Object[] resolveFilterStrings(String[] filterStrings)
           throws Exception
    {
        
    	boolean ok = true;
        
        if ((filterStrings == null) || (filterStrings.length == 0)) {
        	SystemBasePlugin.logInfo("Filter strings are null"); //$NON-NLS-1$
        	return null;
        }
        
    	if (!isConnected()) {
    	  ok = promptForPassword();
    	}
        
        if (ok)
        {
        	return internalResolveFilterStrings(new NullProgressMonitor(), filterStrings);
        }
        else
        {
        	return null;
        }
        
    }
    
    protected void scheduleJob(SubSystemOperationJob job, ISchedulingRule rule) throws InterruptedException
    {
    	IRunnableContext context = getRunnableContext(/*shell*/); // dwd needed for side effect or for prompt?
    	if (context instanceof SystemPromptDialog)
    	{
    		IStatus status = job.runInContext(context);
    		if (!status.isOK())
    		{
    			showOperationErrorMessage(shell, status.getException());
    		}
    		return;
    	}
    	job.setPriority(Job.INTERACTIVE);
	    //job.setUser(true);
    	if (rule != null)
    	{
    		job.setRule(rule);
    	}
	    job.schedule();
    }

    /**
     * Sort the concatenated list of all objects returned by resolving one or more
     *  filter strings.
     * The default implementation does nothing. Child classes can override if they wish
     *  to show their resulting objects sorted.
     */
    protected Object[] sortResolvedFilterStringObjects(Object[] input)
    {
    	return input;
    }
    
    
    /**
     * Modal thread version of resolve filter strings
     * Resolve an absolute filter string. This is only applicable if the subsystem
     *  factory reports true for supportsFilters().
     * <p>
     * When a user expands a filter containing filter strings, this method is
     *  invoked for each filter string.
     * <p>
     * The resulting objects are displayed in the remote system view tree. They
     *  can be anything, but at a minimum must support IAdaptable in order to
     *  drive the property sheet. You can just defer the getAdapter request to
     *  the platform's Adapter manager if desired.
     * <p>
     * You should supply an adapter class for the returned object's class,
     *  to render objects in the Remote System Explorer view. It will uses a
     *  label and content provider that defers all requests to the adapter,
     *  which it gets by querying the platform's adapter manager for the object
     *  type. Be sure to register your adapter factory.
     *
     * @param monitor the process monitor associated with this operation
     * @param filterString filter pattern for objects to return.
     * @return Array of objects that are the result of this filter string
     */
    public Object[] resolveFilterString(IProgressMonitor monitor, String filterString) throws Exception
    {
        boolean ok = true;
    	if (!isConnected())
    	  ok = promptForPassword();
        if (ok)
        {
            Object[] results = internalResolveFilterString(monitor, filterString);
            if (sortResults && (results!=null))
                results = sortResolvedFilterStringObjects(results);
            return results;
        }
        else
        {
            return null;
        }
    }

    /**
     * Modal thread version of resolve filter strings
     * Resolve an absolute filter string. This is only applicable if the subsystem
     *  factory reports true for supportsFilters().
     * <p>
     * When a user expands a filter containing filter strings, this method is
     *  invoked for each filter string.
     * <p>
     * The resulting objects are displayed in the remote system view tree. They
     *  can be anything, but at a minimum must support IAdaptable in order to
     *  drive the property sheet. You can just defer the getAdapter request to
     *  the platform's Adapter manager if desired.
     * <p>
     * You should supply an adapter class for the returned object's class,
     *  to render objects in the Remote System Explorer view. It will uses a
     *  label and content provider that defers all requests to the adapter,
     *  which it gets by querying the platform's adapter manager for the object
     *  type. Be sure to register your adapter factory.
     *
     * @param monitor the process monitor associated with this operation
     * @param filterStrings filter patterns for objects to return.
     * @return Array of objects that are the result of this filter string
     */
    public Object[] resolveFilterStrings(IProgressMonitor monitor, String[] filterStrings)
    throws Exception
    {
        boolean ok = true;
    	if (!isConnected())
    	  ok = promptForPassword();
        if (ok)
        {
            Object[] results = internalResolveFilterStrings(monitor, filterStrings);
            if (sortResults && (results!=null))
                results = sortResolvedFilterStringObjects(results);
            return results;
        }
        else
        {
            return null;
        }
    }

    /**
     * Modal thread version of resolve filter strings
     * Resolve an absolute filter string. This is only applicable if the subsystem
     *  factory reports true for supportsFilters().
     * <p>
     * When a user expands a filter containing filter strings, this method is
     *  invoked for each filter string.
     * <p>
     * The resulting objects are displayed in the remote system view tree. They
     *  can be anything, but at a minimum must support IAdaptable in order to
     *  drive the property sheet. You can just defer the getAdapter request to
     *  the platform's Adapter manager if desired.
     * <p>
     * You should supply an adapter class for the returned object's class,
     *  to render objects in the Remote System Explorer view. It will uses a
     *  label and content provider that defers all requests to the adapter,
     *  which it gets by querying the platform's adapter manager for the object
     *  type. Be sure to register your adapter factory.
     *
     * @param monitor the process monitor associated with this operation
     * @param parent the object to query
     * @param filterString filter pattern for objects to return.
     * @return Array of objects that are the result of this filter string
     */
	public Object[] resolveFilterString(IProgressMonitor monitor, Object parent, String filterString)
    throws Exception
    {
	    boolean ok = true;
	    if (!isConnected())
	        ok = promptForPassword();
 
	    if (ok)
	    {
	        Object[] results= internalResolveFilterString(monitor, parent, filterString);
	        if (sortResults && (results!=null))
                results =  sortResolvedFilterStringObjects(results);
	        return results;
	    }
	    else
	    {
	        return null;
	    }
    }
    
    /**
     * Resolve a <i>relative</i> filter string. 
     * <p>
     * When a user <u>expands a remote resource</u> this method is invoked and the 
     *  results are potentially sorted and displayed to the user. You can affect the sorting
     *  behaviour by overriding {@link #sortResolvedFilterStringObjects(Object[])} if you desire to
     *  sort the result, say, or pick our redundancies.
     * <p>
     * The resulting objects are displayed in the tree in the Remote System {@link org.eclipse.rse.internal.ui.view.SystemView view}. 
     * There are <u>two requirements</u> on the returned objects:</p>
     * <ol>
     *   <li>They must implement {@link org.eclipse.core.runtime.IAdaptable}.
     *   <li>Their must be an RSE {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter remote-adapter} registered
     *        for the object's class or interface type. Further, if this subsystem is {@link org.eclipse.rse.core.subsystems.SubSystem#isHidden() visible}
     *        in the RSE, which is the default, then there must also be an RSE {@link org.eclipse.rse.ui.view.ISystemViewElementAdapter GUI-adapter} registered
     *        with the platform. The base class implementation of this interface is {@link org.eclipse.rse.ui.view.AbstractSystemViewAdapter}.
     * </ol>
     * <p>A good place to start with your remote-resource classes to subclasss {@link org.eclipse.rse.core.subsystems.AbstractResource}, as it
     * already implements IAdaptable, and maintains a reference to this owning subsystem, which helps when 
     * implementing the {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter remote-adapter}.
     * <p>
     * Be sure to register your adapter factory in your plugin's startup method.
     * <p>
     * <b>You do not need to override this, as it does the progress monitor and error message
     *  displaying for you. Override internalResolveFilterString instead. </b>
     * <p>
     * @param parent Object that is being expanded.
     * @param filterString filter pattern for children of parent. Typically just "*".
     * 
     * @deprecated use resolveFilterString(IProgressMonitor monitor, Object parent, String filterString) instead
     */
    public Object[] resolveFilterString(Object parent, String filterString)
           throws Exception
    {
        boolean ok = true;
    	if (!isConnected())
    	  ok = promptForPassword();
        if (ok)
        {
        	return internalResolveFilterString(new NullProgressMonitor(), parent, filterString);
        }
        else
        {
    	    return null;
        }
    }
    
	
	
	

	/**
	 * Provide list of executed commands on subsystem.This is only applicable if the subsystem factory reports
     *  true for supportsCommands().
	 */
	public String[] getExecutedCommands()
	{
		return null;
	}
	

    /**
     * Set a remote property. Subsystems interpret as they wish. Eg, this might be to set
     *  a remote environment variable. This is only applicable if the subsystem factory reports
     *  true for supportsProperties().
     * @param subject Identifies which object to get the properties of
     * @param key Identifies property to set
     * @param value Value to set property to
     * @return Object interpretable by subsystem. Might be a Boolean, or the might be new value for confirmation.
     * 
     * @deprecated this shouldn't be used
     */
    public Object setProperty(Object subject, String key, String value)
           throws Exception
    {
       return null;
    }

    /**
     * Get a remote property. Subsystems interpret as they wish. Eg, this might be to get
     *  a remote environment variable. This is only applicable if the subsystem factory reports
     *  true for supportsProperties().
     * @param subject Identifies which object to get the properties of
     * @param key Identifies property to get value of
      * @return String The value of the requested key.
      * 
      * @deprecated this shouldn't be used
     */
    public String getProperty(Object subject, String key)
           throws Exception
    {
       return null;
    }

    /**
     * Set multiple remote properties. Subsystems interpret as they wish. Eg, this might be to set
     *  a number of remote environment variables. This is only applicable if the subsystem factory reports
     *  true for supportsProperties().
     * @param subject identifies which object to get the properties of.
     * @param keys the array of propertie keys to set.
     * @param values the array of values to set. The value at a certain index corresponds to the property key at the same index. 
     * @return Object interpretable by subsystem. Might be a Boolean, or the might be new values for confirmation.
     * 
     * @deprecated this shouldn't be used
     */
    public Object setProperties(Object subject, String[] keys, String[] values)
           throws Exception
    {
       return null;
    }

    /**
     * Initialize this subsystem instance after the corresponding {@link IConnectorService} connect method finishes.
     * This method should be overridden if any initialization for the subsystem needs
     * to occur at this time
     * <p> The default implementation currently does nothing, but overriding methods should call super.
     * @param monitor a monitor that can be used to show progress or provide cancellation.
     */
    public void initializeSubSystem(IProgressMonitor monitor) {
    }
    
    /**
     * Uninitialize this subsystem just after disconnect.
     * The default implementation currently does nothing.
     * Overriding methods should call super.
     * @param monitor a progress monitor that can be used to show uninitialization progress can provide cancellation.
     */
    public void uninitializeSubSystem(IProgressMonitor monitor) {
    }

    /**
	 * Attempt to connect to the remote system when a Shell is not available.
	 * You do not need to override this, as it does the progress monitor and error message
	 * displaying for you.
	 * <p>
	 * Override internalConnect if you want, but by default it calls getSystem().connect(IProgressMonitor).
	 * 
	 * @deprecated 
	 */
	public void connect() throws Exception {
		if (!isConnected()) {
			if (Display.getCurrent() == null) {
				ConnectFromBackgroundJob job = new ConnectFromBackgroundJob();
				job.setPriority(Job.INTERACTIVE);
				job.schedule();
				job.join();
			} else {
				connect(false);
			}
//			Display display = Display.getCurrent();
//			while (job.getResult() == null) {
//				while (display != null && display.readAndDispatch()) {
//					//Process everything on event queue
//				}
//				if (job.getResult() == null) Thread.sleep(200);
//			}
		}
	}


	
	/**
	 * Required for Bug 176603
	 * 
	 * @deprecated
	 */
	public void connect(IProgressMonitor monitor) throws Exception 
	{
		connect(monitor, false);
	}
	
	/**
	 * Connect to a remote system with a monitor.
	 * Required for Bug 176603
	 *
	 * @param monitor the progress monitor
	 * @param forcePrompt indicates whether to prompt even if password is in memory
	 */
	public void connect(IProgressMonitor monitor, boolean forcePrompt) throws Exception 
	{				
		if (!isConnected()) 
		{
			String msg = null;

	        msg = SubSystemConfiguration.getConnectingMessage(getHostName(), getConnectorService().getPort());        	
	        SystemBasePlugin.logInfo(msg);
	        monitor.beginTask(msg, 10);
	        final boolean promptForPassword = forcePrompt;
	        
			final Exception[] exception=new Exception[1];
			exception[0]=null;
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					try 
					{
						promptForPassword(promptForPassword);
					} catch(Exception e) {
						exception[0]=e;
					}
				}
			});
			
			try {
				getConnectorService().connect(monitor);
			} catch(Exception e) {
				e.printStackTrace();
			}
			if (isConnected()) {
				final SubSystem ss = this;
				//Notify connect status change
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						RSEUIPlugin.getTheSystemRegistry().connectedStatusChange(ss, true, false);
					}
				});
			}
			monitor.done();
		}
	}
	
	
	public void connect(IRSECallback callback) throws Exception
	{
		connect(false, callback);	
	}
	
	/**
	 * @deprecated
	 */
	public void connect(boolean forcePrompt) throws Exception 
	{
		connect(forcePrompt, null);
	}
	
	/**
	 * Connect to the remote system, optionally forcing a signon prompt even if the password
	 * is cached in memory or on disk.
	 * You do not need to override this, as it does the progress monitor and error message
	 *  displaying for you.
	 * <p>
	 * Override internalConnect if you want, but by default it calls getSystem().connect(IProgressMonitor).
	 * 
	 * @param forcePrompt Forces the signon prompt to be displayed even if a valid password in cached in memory
	 * or saved on disk.
	 */
	public void connect(boolean forcePrompt, IRSECallback callback) throws Exception {
		// yantzi: artemis60, (defect 53082) check that the connection has not been deleted before continuing,
		// this is a defenisve measure to protect against code that stores a handle to subsystems but does 
		// not do this check
		ISystemRegistry registry = RSEUIPlugin.getTheSystemRegistry();
		IHost host = getHost();
		String hostName = host.getAliasName();
		ISystemProfile profile = getSystemProfile();
		if (registry.getHost(profile, hostName) == null) { // connection no longer exists
			SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECTION_DELETED);
			msg.makeSubstitution(hostName);
			throw new SystemMessageException(msg);
		}
		// yantzi: artemis 6.0, offline support
		if (isOffline()) {
			SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_OFFLINE_CANT_CONNECT);
			msg.makeSubstitution(hostName);
			throw new SystemMessageException(msg);
		}
		//DY operation = OPERATION_CONNECT;
		if (!isConnected() && supportsConnecting) {
			getRunnableContext(/*shell*/); // needed only for side effect of setting shell to the shell for the active workbench window
//dwd			IRunnableContext runnableContext = getRunnableContext(shell);
//dwd			if (runnableContext instanceof ProgressMonitorDialog) {
//dwd				((ProgressMonitorDialog) runnableContext).setCancelable(true);
//dwd			}
			getConnectorService().acquireCredentials(forcePrompt); // prompt for userid and password    
			ConnectJob job = new ConnectJob(this, callback);
			scheduleJob(job, null);
		}
	}
    
 

    /**
     * A convenience method, fully equivalent to promptForPassword(false).
     */
    public boolean promptForPassword() throws Exception
    {
    	return promptForPassword(false);
    }

    /**
     * Prompt the user for a password to the remote system. The primary request was something else,
     * but we have detected the user is not connected so we prompt for password outside
     * of the progress monitor, then set a flag to do the connection within the progress
     * monitor.
     * @param force true if the prompting should be forced, false if prompting can be skipped if credentials have been stored.
     * @return true if the credentials are obtained
     */
    public boolean promptForPassword(boolean force) throws Exception
    {
    	boolean ok = false;
    	if (!supportsConnecting)
    	  return true;
    	
    	if (isOffline())
    	{
    		// offline so don't bother prompting
    		doConnection = true;	// this gets handled later when it comes time to connect
    		return true;
    	}
    	else if (supportsCaching() && getCacheManager().isRestoreFromMemento())
    	{
			doConnection = true;	// this gets handled later when it comes time to connect
			return true;    		
    	}
    	
    	try
    	{
    	  getConnectorService().acquireCredentials(force); // prompt for password
    	  doConnection = true;
    	  ok = true;
    	}
    	catch (InterruptedException exc) // user cancelled
    	{
    	  throw exc;
    	}    	
    	catch (Exception exc)
    	{
    	  showConnectErrorMessage(shell, getHostName(), getConnectorService().getPort(), exc);
    	}
    	return ok;
    }

    /**
     * Disconnect from the remote system
     * You do not need to override this, as it does the progress monitor and error message
     *  displaying for you.
     * <p>
     * Override internalDisconnect if you want, but by default it calls getSystem().disconnect(IProgressMonitor).
     * 
     */
    public void disconnect() throws Exception
    {
    	disconnect(true);
    }

    /**
     * Disconnect from the remote system
     * You do not need to override this, as it does the progress monitor and error message
     *  displaying for you.
     * <p>
     * Override internalDisconnect if you want, but by default it calls getSystem().disconnect(IProgressMonitor).
     * 
      * @param collapseTree collapse the tree in the system view
     */
    public void disconnect(boolean collapseTree) throws Exception
    {
    	_disconnecting = true;
    	if (!isConnected() || !supportsConnecting)
    	{
    	    // disconnected but may not have notified viewers (i.e. network problem)
    	    ISystemRegistry sr = RSEUIPlugin.getDefault().getSystemRegistry();	
            sr.connectedStatusChange(this, false, true, collapseTree);
    	  return;      	 	
    	}
    	/*
    	try
    	{
    		*/
    	
    		DisconnectJob job = new DisconnectJob(collapseTree, this);
    		job.schedule();
    		/*
    	}
    	catch (InterruptedException exc)
    	{
    	  if (shell != null)    		
            showDisconnectCancelledMessage(shell, getHostName(), getConnectorService().getPort());
    	  throw exc;
    	}    	
    	finally
    	{
    		_disconnecting = false;
    		_connectionError = false;
    	}
    	*/
    }

    /**
     * Get a remote property. Subsystems interpret as they wish. Eg, this might be to get
     *  a remote environment variable. This is only applicable if the subsystem factory reports
     *  true for supportsProperties().
     * @param subject Identifies which object to get the properties of
     * @param keys the array of property keys.
     * @return the values for the given property keys.
     * 
     * @deprecated this shouldn't be used
     */
    public String[] getProperties(Object subject, String[] keys)
           throws Exception
    {
        return null;
    }

    // ----------------------------------
	// METHODS THAT MUST BE OVERRIDDEN...
	// ----------------------------------

	/**
	 * Return the {@link org.eclipse.rse.core.subsystems.IConnectorService IConnectorService} object that represents the live connection for this system.
	 * This must return an object that implements {@link IConnectorService}. A good starting point for that
	 *  is the base class {@link AbstractConnectorService}.
	 * <p>If you only have a single subsystem class, you may override this method to return the
	 *  IConnectorService object that manages the connect/disconnect actions. If, on the other hand,
	 *  you have multiple subsystem classes that desire to share a single IConnectorService connection,
	 *  (ie, share the same communications pipe) then do not override this. By default, this
	 *  calls getSystemManager() which returns an {@link AbstractConnectorServiceManager} object that manages maintaining a singleton
	 *  IConnectorService object per system connection. You should subclass AbstractSystemManager,
	 *  and override getSystemManager() to return a singleton instance of that subclass.
	 * <p>Default implementation:</p>
	 * <pre><code>
	 *   return getSystemManager().getSystemObject(this);
	 * </code></pre>
	 * 
	 * <p>We recommending using a subclass of {@link AbstractConnectorServiceManager} even for single
	 * subsystems, because it doesn't hurt and allows easy growth if new subsystems
	 * are added in the future.
	 */
	public IConnectorService getConnectorService()
	{
		return _connectorService;
	}
	
	public void setConnectorService(IConnectorService connectorService)
	{
		if (_connectorService != connectorService)
		{
			_connectorService = connectorService;
			_connectorService.registerSubSystem(this);
			setDirty(true);
		}		
		else
		{
			// register the subsystem
			_connectorService.registerSubSystem(this);
		}
	}
	
	

	
	/**
	 * Check if the SubSystem supports caching.  This is the default implementation
	 * which returns false.  Subclasses must override to support caching.
	 */
	public boolean supportsCaching()
	{
		return false;
	}
	
	/**
	 * Return the CacheManager for this subsystem.  This is the default implementation
	 * which just returns null.
	 * 
	 * @see #supportsCaching() 
	 */
	public ICacheManager getCacheManager()
	{
		return null;
	}	
	
	/**
	 * Connect to the remote host. This is called by the run(IProgressMonitor monitor) method.
	 * <p>
	 * DO NOT OVERRIDE THIS. Rather, this calls connect(IProgressMonitor) in the 
	 * IConnectorService class that is returned from getConnectorService().
	 * <p>
	 * Your connect method in your IConnectorService class must follow these IRunnableWithProgress rules:
	 * <ul>
	 *   <li>if the user cancels (monitor.isCanceled()), throw new InterruptedException()
	 *   <li>if something else bad happens, throw new java.lang.reflect.InvocationTargetException(exc) 
	 *       - well, actually you can throw anything and we'll wrap it here in an InvocationTargetException
	 *   <li>do not worry about calling monitor.done() ... caller will do that.
	 * </ul>
	 * 
	 */
	protected void internalConnect(IProgressMonitor monitor)
         throws InvocationTargetException, InterruptedException
    {
		try 
		{			
		  getConnectorService().connect(monitor);
		}
		catch(InvocationTargetException exc) {
			throw exc;
		}
		catch (InterruptedException exc) {
			throw exc;
		}
		catch (Exception exc) {
			throw new InvocationTargetException(exc);
		}
    }
	
	/**
	 * Actually disconnect from the remote host. This is called by the run(IProgressMonitor monitor) method.
	 * <p>
	 * You DO NOT OVERRIDE THIS. Rather, this calls connect(IProgressMonitor) in your 
	 * IConnectorService class that is returned from getConnectorService(). That is where your code to disconnect should go!
	 * <p>
	 * Your disconnect method in your IConnectorService class must follow these IRunnableWithProgress rules:
	 * <ul>
	 *   <li>if the user cancels (monitor.isCanceled()), throw new InterruptedException()
	 *   <li>if the host is unknown, throw new java.lang.reflect.InvocationTargetException(new java.net.UnknownHostException));
	 *   <li>if something else bad happens, throw new java.lang.reflect.InvocationTargetException(exc);
	 *       - well, actually you can throw anything and we'll wrap it here in an InvocationTargetException
	 *   <li>do not worry about calling monitor.done() ... caller will do that!
	 * </ul>
	 */
	protected void internalDisconnect(IProgressMonitor monitor)
         throws InvocationTargetException, InterruptedException
    {
		try 
		{			
		  getConnectorService().disconnect(monitor);
		}
		catch (InterruptedException exc) {
			throw exc;
		}
		catch (InvocationTargetException exc) {
			throw exc;
		}
		catch(Exception exc) {
		  throw new InvocationTargetException(exc);
		}
    }

	/**
     * Resolve an <i>absolute</i> filter string. This is only applicable if the subsystem
     *  factory reports true for {@link org.eclipse.rse.core.subsystems.SubSystemConfiguration#supportsFilters()}, 
     *  which is the default. Otherwise, {@link org.eclipse.rse.core.subsystems.SubSystem#getChildren()}
     *  is called when the subsystem itself is expanded.
     * <p>
     * When a user <u>expands a filter</u> this method is invoked for each filter string and the 
     *  results are concatenated and displayed to the user. You can affect the post-concatenated
     *  result by overriding {@link #sortResolvedFilterStringObjects(Object[])} if you desire to
     *  sort the result, say, or pick our redundancies.
     * <p>
     * The resulting objects are displayed in the tree in the Remote System {@link org.eclipse.rse.internal.ui.view.SystemView view}. 
     * There are <u>two requirements</u> on the returned objects:</p>
     * <ol>
     *   <li>They must implement {@link org.eclipse.core.runtime.IAdaptable}.
     *   <li>Their must be an RSE {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter remote-adapter} registered
     *        for the object's class or interface type. Further, if this subsystem is {@link org.eclipse.rse.core.subsystems.SubSystem#isHidden() visible}
     *        in the RSE, which is the default, then there must also be an RSE {@link org.eclipse.rse.ui.view.ISystemViewElementAdapter GUI-adapter} registered
     *        with the platform. The base class implementation of this interface is {@link org.eclipse.rse.ui.view.AbstractSystemViewAdapter}.
     * </ol>
     * <p>A good place to start with your remote-resource classes to subclasss {@link org.eclipse.rse.core.subsystems.AbstractResource}, as it
     * already implements IAdaptable, and maintains a reference to this owning subsystem, which helps when 
     * implementing the {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter remote-adapter}.
     * <p>
     * Be sure to register your adapter factory in your plugin's startup method.
	 * <p>
	 * Actually resolve an absolute filter string. This is called by the
	 *  run(IProgressMonitor monitor) method, which in turn is called by resolveFilterString.
	 * <p>
	 * As per IRunnableWithProgress rules:
	 * <ul>
	 *   <li>if the user cancels (monitor.isCanceled()), throw new InterruptedException()
	 *   <li>if something else bad happens, throw new java.lang.reflect.InvocationTargetException(exc);
	 *   <li>do not worry about calling monitor.done() ... caller will do that!
	 * </ul>
	 * YOU MUST OVERRIDE THIS IF YOU SUPPORT FILTERS!
	 */
	protected Object[] internalResolveFilterString(IProgressMonitor monitor, String filterString)
         throws java.lang.reflect.InvocationTargetException,
                java.lang.InterruptedException
    {
    	return null;
    }
    /**
     * Resolve multiple absolute filter strings. This is only applicable if the subsystem
     *  factory reports true for supportsFilters().
     * <p>
     * This is the same as {@link #internalResolveFilterString(IProgressMonitor, Object, String)} but takes an array of
     *  filter strings versus a single filter string.
     * <p>
     * The default implementation of this simply calls {@link #internalResolveFilterString(IProgressMonitor, String)}
     *  once for each filter string, and concatenates the result. The method sortResolvedFilterStringObject
     *  is called on the concatenated result, given subclasses an opportunity to sort the result.
     * <p>
     * After successful resolve, the sort method is called to sort the concatenated results before
     *  returning them.
     * @param monitor the progress monitor we are running under
     * @param filterStrings array of filter patterns for objects to return.
     * @return Array of objects that are the result of resolving all the filter strings
     */
    public Object[] internalResolveFilterStrings(IProgressMonitor monitor, String[] filterStrings)
         throws java.lang.reflect.InvocationTargetException,
                java.lang.InterruptedException
    {
        Object[] children = null;
		Vector vChildren = new Vector();
        for (int idx=0; idx<filterStrings.length; idx++)
        {		     	
        	if (monitor != null)
        	{
           		monitor.setTaskName(getResolvingMessage(filterStrings[idx]));
        	}
		   children = internalResolveFilterString(monitor, filterStrings[idx]);
		   //monitor.worked(1);
		   if (children != null)
		     addResolvedFilterStringObjects(vChildren, children, filterStrings, idx);
        }
        int nbrChildren = vChildren.size();
        children = new Object[nbrChildren];
        for (int idx=0; idx<nbrChildren; idx++)
           children[idx] = vChildren.elementAt(idx);
        return children;
    }
    
    /**
     * Overridable extension point for adding the results of a filter string
     *  to the overall list of results. 
     * <p>
     * Can be used to filter out redundant entries in the concatenated list, if this
     *  is desired.
     */
    protected void addResolvedFilterStringObjects(Vector allChildrenSoFar, Object[] childrenForThisFilterString,
                                                    String[] allFilterStrings, int currFilterStringIndex)
    {
    	for (int jdx = 0; jdx<childrenForThisFilterString.length; jdx++)
		    allChildrenSoFar.addElement(childrenForThisFilterString[jdx]);
    }

	/**
     * Resolve a <i>relative</i> filter string. 
     * <p>
     * When a user <u>expands a remote resource</u> this method is invoked and the 
     *  results are potentially sorted and displayed to the user. You can affect the sorting
     *  behaviour by overriding {@link #sortResolvedFilterStringObjects(Object[])} if you desire to
     *  sort the result, say, or pick our redundancies.
     * <p>
     * The resulting objects are displayed in the tree in the Remote System {@link org.eclipse.rse.internal.ui.view.SystemView view}. 
     * There are <u>two requirements</u> on the returned objects:</p>
     * <ol>
     *   <li>They must implement {@link org.eclipse.core.runtime.IAdaptable}.
     *   <li>Their must be an RSE {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter remote-adapter} registered
     *        for the object's class or interface type. Further, if this subsystem is {@link org.eclipse.rse.core.subsystems.SubSystem#isHidden visible}
     *        in the RSE, which is the default, then there must also be an RSE {@link org.eclipse.rse.ui.view.ISystemViewElementAdapter GUI-adapter} registered
     *        with the platform. The base class implementation of this interface is {@link org.eclipse.rse.ui.view.AbstractSystemViewAdapter}.
     * </ol>
     * <p>A good place to start with your remote-resource classes to subclasss {@link org.eclipse.rse.core.subsystems.AbstractResource}, as it
     * already implements IAdaptable, and maintains a reference to this owning subsystem, which helps when 
     * implementing the {@link org.eclipse.rse.ui.view.ISystemRemoteElementAdapter remote-adapter}.
     * <p>
     * Be sure to register your adapter factory in your plugin's startup method.
     * <p>
	 * This is called by the run(IProgressMonitor monitor) method, which in turn is called by resolveFilterString.
	 * <p>
	 * As per IRunnableWithProgress rules:
	 * <ul>
	 *   <li>if the user cancels (monitor.isCanceled()), throw new InterruptedException()
	 *   <li>if something else bad happens, throw new java.lang.reflect.InvocationTargetException(exc);
	 *   <li>do not worry about calling monitor.done() ... caller will do that!
	 * </ul>
	 * YOU MUST OVERRIDE THIS IF YOU SUPPORT FILTERS!
	 */
	protected Object[] internalResolveFilterString(IProgressMonitor monitor, Object parent, String filterString)
         throws java.lang.reflect.InvocationTargetException,
                java.lang.InterruptedException
    {
    	return null;
    }
    /**
     * Called by resolveFilterString when given null for the filter string, meaning we defer 
     *  getting a filter string until later, where we query it from the parent. In this case 
     *  we need the first filter string for the progress monitor msg.<br>
     * Override if needed. By default we return "*";
     */
    protected String getFirstParentFilterString(Object parent)
    {
    	return "*"; //$NON-NLS-1$
    }	


	/**
	 * Actually get a remote property. This is called by the
	 *  run(IProgressMonitor monitor) method, which in turn is called by getProperty(...).
	 * <p>
	 * As per IRunnableWithProgress rules:
	 * <ul>
	 *   <li>if the user cancels (monitor.isCanceled()), throw new InterruptedException()
	 *   <li>if something else bad happens, throw new java.lang.reflect.InvocationTargetException(exc);
	 *   <li>do not worry about calling monitor.done() ... caller will do that!
	 * </ul>
	 * YOU MUST OVERRIDE THIS IF YOU SUPPORT PROPERTIES!
	 */
	protected String internalGetProperty(IProgressMonitor monitor, Object subject, String key)
         throws java.lang.reflect.InvocationTargetException,
                java.lang.InterruptedException
    {
    	return null;
    }

	/**
	 * Actually set a remote property. This is called by the
	 *  run(IProgressMonitor monitor) method, which in turn is called by setProperty(...).
	 * <p>
	 * As per IRunnableWithProgress rules:
	 * <ul>
	 *   <li>if the user cancels (monitor.isCanceled()), throw new InterruptedException()
	 *   <li>if something else bad happens, throw new java.lang.reflect.InvocationTargetException(exc);
	 *   <li>do not worry about calling monitor.done() ... caller will do that!
	 * </ul>
	 * YOU MUST OVERRIDE THIS IF YOU SUPPORT PROPERTIES!
	 */
	protected Object internalSetProperty(IProgressMonitor monitor, Object subject, String key, String value)
         throws java.lang.reflect.InvocationTargetException,
                java.lang.InterruptedException
    {
    	return null;
    }

	/**
	 * Actually get multiple remote properties. This is called by the
	 *  run(IProgressMonitor monitor) method, which in turn is called by getProperties(...).
	 * <p>
	 * As per IRunnableWithProgress rules:
	 * <ul>
	 *   <li>if the user cancels (monitor.isCanceled()), throw new InterruptedException()
	 *   <li>if something else bad happens, throw new java.lang.reflect.InvocationTargetException(exc);
	 *   <li>do not worry about calling monitor.done() ... caller will do that!
	 * </ul>
	 * YOU MUST OVERRIDE THIS IF YOU SUPPORT PROPERTIES!
	 */
	protected String[] internalGetProperties(IProgressMonitor monitor, Object subject, String[] keys)
         throws java.lang.reflect.InvocationTargetException,
                java.lang.InterruptedException
    {
    	return null;
    }

	/**
	 * Actually set multiple remote properties. This is called by the
	 *  run(IProgressMonitor monitor) method, which in turn is called by setProperties(...).
	 * <p>
	 * As per IRunnableWithProgress rules:
	 * <ul>
	 *   <li>if the user cancels (monitor.isCanceled()), throw new InterruptedException()
	 *   <li>if something else bad happens, throw new java.lang.reflect.InvocationTargetException(exc);
	 *   <li>do not worry about calling monitor.done() ... caller will do that!
	 * </ul>
	 * YOU MUST OVERRIDE THIS IF YOU SUPPORT PROPERTIES!
	 */
	protected Object internalSetProperties(IProgressMonitor monitor, Object subject, String[] keys, String[] values)
         throws java.lang.reflect.InvocationTargetException,
                java.lang.InterruptedException
    {
    	return null;
    }




	 /**
	 * Get the progress monitor dialog for this operation. We try to 
	 *  use one for all phases of a single operation, such as connecting
	 *  and resolving.
	 */
	protected IRunnableContext getRunnableContext(/*Shell rshell*/) {
		if (Display.getCurrent() == null) {
			return new NullRunnableContext();
		}
		// for wizards and dialogs use the specified context that was placed in the registry
		IRunnableContext irc = RSEUIPlugin.getTheSystemRegistry().getRunnableContext();
		if (irc != null) {
			SystemBasePlugin.logInfo("Got runnable context from system registry"); //$NON-NLS-1$
			return irc;
		} else {
			// for other cases, use statusbar
			IWorkbenchWindow win = SystemBasePlugin.getActiveWorkbenchWindow();
			if (win != null) {
				Shell winShell = getActiveWorkbenchShell();
				if (winShell != null && !winShell.isDisposed() && winShell.isVisible()) {
					SystemBasePlugin.logInfo("Using active workbench window as runnable context"); //$NON-NLS-1$
					shell = winShell;
					return win;
//dwd				} else {
//dwd					win = null;
				}
			}
//dwd			if (shell == null || shell.isDisposed() || !shell.isVisible()) {
//dwd				SystemBasePlugin.logInfo("Using progress monitor dialog with given shell as parent");
//dwd				shell = rshell;
//dwd			}
//dwd			IRunnableContext dlg = new ProgressMonitorDialog(rshell);
			IRunnableContext dlg = new ProgressMonitorDialog(shell);
			return dlg;
		}
	}
   
     /**
      * Return the shell for the current operation
      */
     protected Shell getShell()
     {
        return shell;
     }
     
    /**
     * Helper/convenience method. Return shell of active window.
     */ 
	public static Shell getActiveWorkbenchShell() 
	{
		Shell result = null;
		if (PlatformUI.isWorkbenchRunning())
		{
			try
			{
				IWorkbenchWindow window = getActiveWorkbenchWindow();
				if (window != null) 
				{
					result = window.getShell();
				}
			}
			catch (Exception e)
			{
				return null;
			}
		}
		else // workbench has not been loaded yet!
		{
			return null;
		}
		return result;
	}
	/**
	 * Helper/convenience method. Return active window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() 
	{
		return RSEUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
	}
     
	/**
	 * <i><b>Private</b>. Do not override.</i>
	 * @generated This field/method will be replaced during code generation 
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * <i><b>Private</b>. Do not override.</i>
	 * @generated This field/method will be replaced during code generation.
	 */
	public void setName(String newName)
	{
		String oldName = _name;
		if (oldName == null || !oldName.equals(newName))
		{
			_name = newName;
			setDirty(true);
		}
	}

	


	/**
	 * <i><b>Private</b>. Do not override.</i>
	 * @generated This field/method will be replaced during code generation 
	 * Ties this subsystem to its owning subsystemconfiguration, via the
	 * id key string of the configuration.
	 */
	public String getConfigurationId()
	{
		return _subsystemConfigurationId;
	}

	/**
	 * <i><b>Private</b>. Do not override.</i>
	 * @generated This field/method will be replaced during code generation.
	 */
	public void setConfigurationId(String newConfigurationId)
	{
		String oldFactoryId = _subsystemConfigurationId;
		if (oldFactoryId == null || !oldFactoryId.equals(newConfigurationId))
		{
			_subsystemConfigurationId = newConfigurationId;
			setDirty(true);
		}
	}

	/**
	 * <i><b>Private</b>. Do not override.</i>
	 * @generated This field/method will be replaced during code generation 
	 */
	public boolean isHidden()
	{
		return _hidden;
	}

	/**
	 * <i><b>Private</b>. Do not override.</i>
	 * @generated This field/method will be replaced during code generation.
	 */
	public void setHidden(boolean newHidden)
	{
		boolean oldHidden = _hidden;
		if (oldHidden != newHidden)
		{
			_hidden = newHidden;
			setDirty(true);
		}
	}

	/**
	 * <i><b>Private</b>. Do not override.</i>
	 * @generated This field/method will be replaced during code generation 
	 */
	public ISystemFilterPoolReferenceManager getFilterPoolReferenceManager()
	{
		return filterPoolReferenceManager;
	}

	public void setFilterPoolReferenceManager(ISystemFilterPoolReferenceManager newFilterPoolReferenceManager)
	{
		filterPoolReferenceManager = newFilterPoolReferenceManager;
		return;
	}



	
	/**
	 * @return true if this subsystem's properties should take precedence
	 *  over other subsystems that share the same IConnectorService
	 */
	public boolean isPrimarySubSystem()
	{
		return false;
	}
	
	/*
	 * Returns the first subsystem associated with the connection
	 */
	public ISubSystem getPrimarySubSystem()
	{
		ISubSystem firstSS = null;
		ISystemRegistry registry = RSEUIPlugin.getTheSystemRegistry();
		ISubSystem[] sses = registry.getSubSystems(getHost(), false);
		for (int i = 0; i < sses.length; i++)
		{
			ISubSystem ss = sses[i];
			if (ss.getConnectorService() == getConnectorService())
			{
				if (firstSS == null)
				{
					firstSS = ss;
				}
				if (ss.isPrimarySubSystem())
				{
					return ss;
				}
			}
		}
		if (firstSS == null)
		{
			firstSS = this;
		}
		return firstSS;
	}


	
	public Object getTargetForFilter(ISystemFilterReference filterRef)
    {
        return null;
    }
	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.SubSystem#isConnectionError()
	 */
	public boolean isConnectionError()
	{
		return _connectionError;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.rse.core.subsystems.SubSystem#setConnectionError(boolean)
	 */
	public void setConnectionError(boolean error)
	{
		_connectionError = error;
	}
    
    public Object[] getTargetsForFilter(ISystemFilterReference filterRef)
    {
        return null;
    }
    
	/**
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	public boolean contains(ISchedulingRule rule) 
	{
		if (this.equals(rule)) return true;
		if (rule instanceof IResource) return true;
		else return false;
	}
	
	/**
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	public boolean isConflicting(ISchedulingRule rule) 
	{
		if (this == rule) return true;
		else return false;
	}
	
	public boolean commit()
	{
		ISystemProfile profile = getSystemProfile();
		boolean result = profile.commit();
		return result;
	}
	
	public IRSEPersistableContainer getPersistableParent() {
		return _host;
	}
	
	public IRSEPersistableContainer[] getPersistableChildren() {
		ISystemFilterPoolReferenceManager manager = getSystemFilterPoolReferenceManager();
		IRSEPersistableContainer[] result = manager.getReferencedSystemFilterPools();
		return result;
	}
	
}