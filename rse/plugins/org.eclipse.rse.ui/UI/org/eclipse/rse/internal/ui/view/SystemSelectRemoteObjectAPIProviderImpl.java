/********************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Martin Oberhuber (Wind River) - [175680] Deprecate obsolete ISystemRegistry methods
 * Tobias Schwarz   (Wind River) - [173267] "empty list" should not be displayed 
 * Martin Oberhuber (Wind River) - [190271] Move ISystemViewInputProvider to Core
 * David Dykstal (IBM) - [224671] [api] org.eclipse.rse.core API leaks non-API types
 * David McKnight   (IBM)        - [225506] [api][breaking] RSE UI leaks non-API types
 ********************************************************************************/

package org.eclipse.rse.internal.ui.view;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.filters.ISystemFilterStringReference;
import org.eclipse.rse.core.filters.SystemFilterUtil;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.internal.model.SystemNewConnectionPromptObject;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.rse.ui.view.ISystemSelectRemoteObjectAPIProvider;
import org.eclipse.rse.ui.view.ISystemSelectRemoteObjectAPIProviderCaller;
import org.eclipse.rse.ui.view.ISystemViewElementAdapter;
import org.eclipse.rse.ui.view.SystemAbstractAPIProvider;
import org.eclipse.rse.ui.view.SystemAdapterHelpers;
import org.eclipse.swt.widgets.Shell;


/**
 * This class is a provider of root nodes to the remote systems tree viewer part.
 * <p>
 * It is used when the contents are used to allow the user to select a remote system object.
 * The tree will begin with the filter pool references or filter references (depending on
 * the user's preferences setting) of the given subsystem. 
 * <p>
 * Alternatively, a filter string can be given and the contents will be the result of resolving 
 * that filter string.
 */
public class SystemSelectRemoteObjectAPIProviderImpl 
       extends SystemAbstractAPIProvider
       implements ISystemSelectRemoteObjectAPIProvider
 {


	protected ISubSystem subsystem = null;
	protected String filterString = null;
	protected ISystemViewElementAdapter subsystemAdapter = null;

    // For mode when we want to list the connections ...
	protected boolean  listConnectionsMode = false;	
    protected boolean  showNewConnectionPrompt = false;
	protected boolean  singleConnectionMode = false;	
	protected String   subsystemConfigurationId; 
	protected String   subsystemConfigurationCategory;
	protected String   filterSuffix;
	protected IRSESystemType[] systemTypes;
	protected String   preSelectFilterChild;
	protected Object   preSelectFilterChildObject;
	protected ISystemFilter[] quickFilters;
	protected IHost[] inputConnections;
    protected SystemNewConnectionPromptObject connPrompt = null;
    protected Object[] connPromptAsArray;
    protected ISystemSelectRemoteObjectAPIProviderCaller caller;
    protected boolean multiConnections = false;
	
	/**
	 * Constructor that takes the input needed to drive the list. Specifically,
	 * we need to know what connections to list, and when a connection is expanded,
	 * what subsystems to query for the remote objects.
	 * <p>
	 * This can be done by giving one of two possible pieces of information: 
	 * <ul>
	 *   <li>a subsystem factory Id, which scopes the connections to those containing subsystems 
	 *        owned by this factory, and scopes subsystems to only those from this factory. 
	 *   <li>The subsystem factory Id is usually the right choice, unless you want to include 
	 *        connections and subsystems from multiple subsystem factories, such as is the case 
	 *        for universal files ... there is one base factory but it is subclassed a number of
	 *        times. For this and any other case we also allow scoping by subsystem factory 
	 *        category. All connections from any factory of this category are included, and 
	 *        subsystems of factories from this category are used to populate the list.
	 * </ul>
	 * <p>
	 * You must supply one of these. There is no need to supply both.
	 * <p>
	 * Also, it is often desired to restrict what system types the user can create new connections for.
	 * While this could be deduced from the first two pieces of information, it is safer to ask the
	 * caller to explicitly identify these. If null is passed, then there is no restrictions.
	 * 
	 * @param configId The subsystemConfigurationId to restrict connections and subsystems to
	 *                           An alternative to factoryCategory. Specify only one, pass null for the other.
	 * @param configCategory The subsystem configuration category to restrict connections and subsystems to. 
	 *                           An alternative to factoryId. Specify only one, pass null for the other.
	 * @param showNewConnectionPrompt true if to show "New Connection" prompt, false if not to
	 * @param systemTypes Optional list of system types to restrict the "New Connection" wizard to. Pass null for no restrictions
	 */
	public SystemSelectRemoteObjectAPIProviderImpl(String configId, String configCategory, 
	                                               boolean showNewConnectionPrompt, IRSESystemType[] systemTypes)
	{
		super();
		this.subsystemConfigurationId = configId;
		this.subsystemConfigurationCategory = configCategory;
		this.systemTypes = systemTypes;
		this.showNewConnectionPrompt = showNewConnectionPrompt;
		this.listConnectionsMode = true;
	}	
	
	/**
	 * Set the caller to callback to for some events, such as the expansion of a prompting 
	 *  transient filter.
	 */
	public void setCaller(ISystemSelectRemoteObjectAPIProviderCaller caller)
	{
		this.caller = caller;
	}
	
	/**
	 * Specify whether the user should see the "New Connection..." special connection prompt
	 */
	public void setShowNewConnectionPrompt(boolean show)
	{
		this.showNewConnectionPrompt = show;
	}

	/**
	 * Specify system types to restrict what types of connections
	 * the user can create, and see.
	 * This will override subsystemConfigurationId,if that has been set!
	 * 
     * @param systemTypes An array of system types, or
     *     <code>null</code> to allow all registered valid system types.
     *     A system type is valid if at least one subsystem configuration
     *     is registered against it.
	 */
	public void setSystemTypes(IRSESystemType[] systemTypes)
	{
		this.systemTypes = systemTypes;
	}
	
	/**
	 * Constructor when there is a subsystem 
	 * @param subsystem The subsystem that will resolve the filter string
	 */
	public SystemSelectRemoteObjectAPIProviderImpl(ISubSystem subsystem)
	{
		super();
		setSubSystem(subsystem);
	}
	
	/**
	 * Constructor when there is no subsystem yet
	 * @see #setSubSystem(ISubSystem)
	 */
	public SystemSelectRemoteObjectAPIProviderImpl()
	{
		super();
	}
	
	/** 
	 * Default or Restrict to a specific connection.
	 * If default mode, it is preselected.
	 * If only mode, it is the only connection listed.
	 * @param connection The connection to default or restrict to
	 * @param onlyMode true if this is to be the only connection shown in the list
	 */
	public void setSystemConnection(IHost connection, boolean onlyMode)
	{
		this.inputConnections = new IHost[] {connection};
		this.singleConnectionMode = onlyMode;
		if (onlyMode)
			multiConnections = false;
	}
	
	/**
	 * Change the input subsystem
	 */
	public void setSubSystem(ISubSystem subsystem)
	{
		this.subsystem = subsystem;
		if (subsystem != null)
		  this.subsystemAdapter = getViewAdapter(subsystem);
		else
		  this.subsystemAdapter = null;
	}
	
	/**
	 * Set the filter string to use to resolve the inputs. 
	 * If this is an absolute filter string, it gets turned into a quick filter string,
	 *  so that the user sees it and can expand it. If it is a relative filter string 
	 *  to apply to all expansions, it is used to decorate all filtering as the user drills down.
	 */
	public void setFilterString(String string)
	{
		// WARNING: ENTERING BIG HUGE HACK AREA!
		this.filterString = string;
		filterSuffix = null;
		if (string == null)
		  return;

		if (string.endsWith(",")) //$NON-NLS-1$
		{
		   int idx = string.indexOf('/');
		   if (idx == -1)
		     idx = string.indexOf('\\');
		   if (idx == -1)
		   {
		     filterSuffix = string;
		   }
		}
		
		if (filterSuffix != null)
		  filterString = null;
		
		SystemBasePlugin.logDebugMessage(this.getClass().getName(), "*** FILTER SUFFIX = '" + filterSuffix + "' ***"); //$NON-NLS-1$ //$NON-NLS-2$
	}    

	/**
	 * Set the filters to be exposed to the user. These will be shown to the
	 * user when they expand a connection.
	 */
	public void setQuickFilters(ISystemFilter[] filters)
	{
		this.quickFilters = filters;
	}

	/**
	 * Set child of the first filter to preselect 
	 */
	public void setPreSelectFilterChild(String name)
	{
		this.preSelectFilterChild = name;
	}
	
	/**
	 * Get the name of the item to select when the first filter is expanded.
	 * Called by the filter adapter.
	 */
	public String getPreSelectFilterChild()
	{
		return preSelectFilterChild;
	}
	
	/**
	 * Set actual child object of the first filter to preselect. Called
	 * by the filter adapter once the children are resolved and a match on
	 * the name is found.
	 */
	public void setPreSelectFilterChildObject(Object obj)
	{
		this.preSelectFilterChildObject = obj;
	}
	
	/**
	 * Get the actual object of the item to select when the first filter is expanded.
	 * Called by the GUI form after expansion, so it can select this object
	 */
	public Object getPreSelectFilterChildObject()
	{
		return preSelectFilterChildObject;
	}
	
	/**
	 * Adorn filter string with any relative attributes requested. Eg "/nf" for folders only
	 */
	public String decorateFilterString(Object selectedObject, String inputFilterString)
	{
		// this is a hack explicitly for the universal file system. We want to propogate "type filters"
		// like "/nf" and "class," on down the chain, even though we start by showing the user's filters.
		// When those filters are finally expanded, the filter adapter calls us to do this adornment.

		if (inputFilterString == null)
		  return inputFilterString; 
		else if ((filterSuffix != null) && (inputFilterString.indexOf(filterSuffix)==-1))
		{
		  SystemBasePlugin.logDebugMessage(this.getClass().getName(), "*** INPUT FILTER = '" + inputFilterString + "' ***"); //$NON-NLS-1$ //$NON-NLS-2$
		  String result = inputFilterString;
		  if (filterSuffix.equals(" /nf")) //$NON-NLS-1$
		    result = inputFilterString + filterSuffix;
		  else
		  {
				/** FIXME - can't be coupled with IRemoteFile
		  	RemoteFileFilterString rffs = 
		  	  new RemoteFileFilterString((IRemoteFileSubSystemConfiguration)getSubSystemConfiguration(selectedObject), inputFilterString);
		  	rffs.setFile(filterSuffix);
		  	result = rffs.toString();
		  	*/
			  result = inputFilterString;
		  }
		  SystemBasePlugin.logDebugMessage(this.getClass().getName(), "*** ADORNED FILTER = '" + result + "' ***"); //$NON-NLS-1$ //$NON-NLS-2$
		  return result;
		}
		else
		  return inputFilterString;
	}
	
	/**
	 * For performance reasons, pre-check to see if filter decoration is even necessary...
	 */
	public boolean filtersNeedDecoration(Object selectedObject)
	{
		ISubSystemConfiguration ssf = getSubSystemConfiguration(selectedObject);
		if (ssf == null)
		  return false;
		/** FIXME - can't be coupled with IRemoteFile
		return ((ssf instanceof IRemoteFileSubSystemConfiguration) && (filterSuffix != null));
		*/
		return false;
		
	}
	
	/**
	 * get subsystem factory from filter or filter string
	 */
	private ISubSystemConfiguration getSubSystemConfiguration(Object selectedObject)
	{
        if (selectedObject instanceof ISystemFilterReference)
        {
        	ISubSystem ss = (ISubSystem)((ISystemFilterReference)selectedObject).getProvider();
        	return ss.getSubSystemConfiguration();
        }
        else if (selectedObject instanceof ISystemFilterStringReference)
        {
        	ISubSystem ss = (ISubSystem)((ISystemFilterStringReference)selectedObject).getProvider();
        	return ss.getSubSystemConfiguration();
        }
        else
          return null;
	}

    // ----------------------------------
    // SYSTEMVIEWINPUTPROVIDER METHODS...
    // ----------------------------------
	/**
	 * Return the children objects to consistute the root elements in the system view tree.
	 */
	public Object[] getSystemViewRoots()
	{
        if (listConnectionsMode)
          return getConnections();    

		if (subsystemAdapter == null)
		{
		  return emptyList;
		}
		
        Object[] children = null;

		if (filterString == null)
	 	  children = subsystemAdapter.getChildren((IAdaptable)subsystem, new NullProgressMonitor());
	 	else
	 	{
	 	  children = resolveFilterString(subsystem, filterString);
	 	}
		
		return checkForEmptyList(children, null, true);
	}
	
	/**
	 * Return true if {@link #getSystemViewRoots()} will return a non-empty list
	 */
	public boolean hasSystemViewRoots()
	{
	    if (listConnectionsMode)	
		  return true;
		else
		{
		  boolean hasroots = false;
		  if (subsystemAdapter == null)
		    hasroots = false;
		  else if (filterString != null)
		    hasroots = true;
		  else
		    hasroots = subsystemAdapter.hasChildren((IAdaptable)subsystem);
		  
		  return hasroots;
		}
	}
	
	/**
	 * This method is called by the connection adapter when the user expands
	 *  a connection. This method must return the child objects to show for that
	 *  connection.
	 */
	public Object[] getConnectionChildren(IHost selectedConnection)
	{
		if (!listConnectionsMode)
 		  return null; // not applicable, never get called
 		else
 		{
 			Object[] children = null;
 			ISubSystem[] subsystems = getSubSystems(selectedConnection);
 			ISubSystem subsystem = null;
 			
 			if ((subsystems != null) && (subsystems.length > 0))
 			{
 				subsystem = subsystems[0]; // always just use first. Hopefully never a problem!
 				
 				if (subsystems.length > 1)
 				  SystemBasePlugin.logWarning(this.getClass().getName() + ": More than one subsystem meeting criteria. SSFID = "+subsystemConfigurationId+", SSFCat = "+subsystemConfigurationCategory); //$NON-NLS-1$ //$NON-NLS-2$
 				  
 				if (quickFilters != null)
 				{
 					// DKM - quick filters are only work properly for first subsystem, so for now, I'm only
 					// only going to use them for the initial subsystem
 					//boolean useFilters = false; 		
 								
 					// Phil
					// 50167: re-using the same filter object for every connection causes
					// grief, so we have to clone the filter for each connection.
					if (multiConnections)
					{
						// walk through quick filters, and create a clone for each one
						children = new ISystemFilter[quickFilters.length];
						
						for (int idx=0; idx<quickFilters.length; idx++)
						{  				  	
							ISystemFilter quickFilter = quickFilters[idx];
							children[idx] = SystemFilterUtil.makeSimpleFilter(quickFilter.getName());
							quickFilter.clone((ISystemFilter)children[idx]);
							((ISystemFilter)children[idx]).setSubSystem(subsystem);
						} 				        	 				        	 			        	
					}
								 						
					else
					{		
 				    	// walk through quickFilters and if they are transient, assign current subsystem as parent
 				    	for (int idx=0; idx<quickFilters.length; idx++)
 				    	{  				  	
 				        	if ((quickFilters[idx].isTransient())) 				        
 				        	{	 	
								quickFilters[idx].setSubSystem(subsystem);
 				        	}
 				    	} 				  

	 				  	children = quickFilters;
					} 				    
 				}
		        
		        else if ((filterString == null) || (filterSuffix != null))
		        {
	 	          children = subsystem.getChildren();
		        }
	 	        else
	 	        {
	 	          children = resolveFilterString(subsystem, filterString);
	 	        }
 			}
 			
 			return checkForEmptyList(children, subsystem, true);
 		}
	}
	
	/**
	 * This method is called by the connection adapter when deciding to show a plus-sign
	 * or not beside a connection. Return true if this connection has children to be shown.
	 */
	public boolean hasConnectionChildren(IHost selectedConnection)
	{
		return true;		
	}
    
	/**
	 * Return true if we are listing connections or not, so we know whether we are interested in 
	 *  connection-add events
	 */
	public boolean showingConnections()
	{
		return listConnectionsMode;
	}


    // ----------------------------------
    // OUR OWN METHODS...    
    // ----------------------------------
    
    /**
     * Return the connections appropriate for our subsystem factory ID or category
     *  requirements.
     */
    protected Object[] getConnections()
    {
       if (singleConnectionMode && !showNewConnectionPrompt)
         return inputConnections;
       if ((connPrompt == null) && showNewConnectionPrompt)
       {
         connPrompt = new SystemNewConnectionPromptObject();
         connPromptAsArray = new Object[1];
         connPromptAsArray[0] = connPrompt;
       }
       if ((connPrompt!=null) && (systemTypes != null))
       {
         connPrompt.setSystemTypes(systemTypes);
       }
       
       IHost[] conns = null;
       
       if (singleConnectionMode)
         conns = inputConnections;       
       else if (systemTypes != null)
         conns = sr.getHostsBySystemTypes(systemTypes);
       else if (subsystemConfigurationId != null) {
   	     ISubSystemConfiguration config = sr.getSubSystemConfiguration(subsystemConfigurationId);
         conns = sr.getHostsBySubSystemConfiguration(config);
       }
       else if (subsystemConfigurationCategory != null)
         conns = sr.getHostsBySubSystemConfigurationCategory(subsystemConfigurationCategory);
       else
         conns = sr.getHosts();
       
       Object[] children = null;
       
       if (showNewConnectionPrompt)
       {
         	if ((conns == null) || (conns.length == 0))
          		children = connPromptAsArray;
         	else
         	{
         		multiConnections = (conns.length>1); // 50167pc
         		children = new Object[1+conns.length];
         		children[0] = connPrompt;
         		for (int idx=0; idx<conns.length; idx++)
         	   		children[idx+1] = conns[idx];
         	}
       }
       else
       {
         	children = conns;
			multiConnections = ((conns!=null) && (conns.length>1)); // 50167pc
       }

       return checkForEmptyList(children, null, false);
    }     

	/**
	 * Given a connection, return the subsystem(s) appropriate for the given 
	 * subsystem configuration id or category
	 */
	protected ISubSystem[] getSubSystems(IHost selectedConnection)
	{
		ISubSystem[] subsystems = null;
		if (subsystemConfigurationId != null) {
			ISubSystemConfiguration config = sr.getSubSystemConfiguration(subsystemConfigurationId);
			if (config==null)
				subsystems = new ISubSystem[0];
			else
				subsystems = config.getSubSystems(selectedConnection, true);
		}
		else if (subsystemConfigurationCategory != null) {
		  subsystems = sr.getSubSystemsBySubSystemConfigurationCategory(subsystemConfigurationCategory, selectedConnection);
		}
		else
		  subsystems = sr.getSubSystems(selectedConnection);
		return subsystems;
	}
	
	/**
	 * Return the result of asking a given subsystem to resolve a filter string 
	 */
	protected Object[] resolveFilterString(ISubSystem subsystem, String filterString)
	{
		Object[] children = null;
		try
		{
	 	     children = subsystem.resolveFilterString(filterString, new NullProgressMonitor());	 	     
		} catch (InterruptedException exc)
		{
		     if (cancelledObject == null)
		       children = getCancelledMessageObject();
		} catch (Exception exc)
		{
		     children = getFailedMessageObject();			
		     SystemBasePlugin.logError("Error in SystemTestFilterStringAPIProviderImpl#getSystemViewRoots()",exc); //$NON-NLS-1$
		}
		return children;
	}

    /**
     * Returns the implementation of ISystemViewElement for the given
     * object.  Returns null if the adapter is not defined or the
     * object is not adaptable.
     */
    protected ISystemViewElementAdapter getViewAdapter(Object o) 
    {
    	return SystemAdapterHelpers.getViewAdapter(o);
    }
    
    /**
     * Returns the implementation of ISystemRemoteElement for the given
     * object.  Returns null if this object does not adaptable to this.
     */
    protected ISystemRemoteElementAdapter getRemoteAdapter(Object o) 
    {
    	return SystemAdapterHelpers.getRemoteAdapter(o);
    }
	

    /**
     * Prompt the user to create a new filter as a result of the user expanding a promptable
     * transient filter
     * <p>
     * Simply passes the request on to the caller.
     * <p>
     * NOT SUPPORTED YET!
     *
     * @return the filter created by the user or null if they cancelled the prompting
     */
    public ISystemFilter createFilterByPrompting(ISystemFilter filterPrompt, Shell shell)
           throws Exception
    {
    	if (caller!=null)
    	  return caller.createFilterByPrompting(filterPrompt, shell);
    	else
    	  return null;
    }   
}