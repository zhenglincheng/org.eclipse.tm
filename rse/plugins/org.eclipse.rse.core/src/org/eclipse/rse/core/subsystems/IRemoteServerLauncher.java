/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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

package org.eclipse.rse.core.subsystems;

/**
 * This is the implementation of {@link IServerLauncherProperties}. It basically allows for numerous types
 * of server connecting, as identified in {@link org.eclipse.rse.core.subsystems.ServerLaunchType}. It
 * also captures the attributes needed to support these.
 * <p> 
 * A server launcher is responsible for starting the server-side code needed for this client subsystem to 
 * access remote resources on the remote system. It starts the server half of the client/server code needed
 * for this subsystem. It is consulted in the default implementation of connect() in IConnectorService, and the
 * manages the properties in the Remote Server Launcher property page.    
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.rse.core.subsystems.IRemoteServerLauncher#getServerLaunchType <em>Server Launch Type</em>}</li>
 * <li>{@link org.eclipse.rse.core.subsystems.IRemoteServerLauncher#getRexecPort <em>Rexec Port</em>}</li>
 * <li>{@link org.eclipse.rse.core.subsystems.IRemoteServerLauncher#getDaemonPort <em>Daemon Port</em>}</li>
 * <li>{@link org.eclipse.rse.core.subsystems.IRemoteServerLauncher#getServerPath <em>Server Path</em>}</li>
 * <li>{@link org.eclipse.rse.core.subsystems.IRemoteServerLauncher#getServerScript <em>Server Script</em>}</li>
 * </ul>
 */
public interface IRemoteServerLauncher extends IServerLauncherProperties {

	/**
	 * Returns the value of the '<em><b>Server Launch Type</b></em>' attribute.
	 * The literals are from the enumeration {@link org.eclipse.rse.core.subsystems.ServerLaunchType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * This is the means by which to start the server-side code, as specified by the user, typically.
	 * It is one of the constants in the enumeration class {@link org.eclipse.rse.core.subsystems.ServerLaunchType}
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Server Launch Type</em>' attribute.
	 * @see org.eclipse.rse.core.subsystems.ServerLaunchType
	 * @see #setServerLaunchType(ServerLaunchType)
	 * @model unsettable="true"
	 * @generated
	 */
	ServerLaunchType getServerLaunchType();

	/**
	 * Sets the value of the '{@link org.eclipse.rse.core.subsystems.IRemoteServerLauncher#getServerLaunchType <em>Server Launch Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * This is the means by which to start the server-side code, as specified by the user, typically.
	 * It is one of the constants in the enumeration class {@link org.eclipse.rse.core.subsystems.ServerLaunchType}
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Server Launch Type</em>' attribute.
	 * @see org.eclipse.rse.core.subsystems.ServerLaunchType
	 * @see #getServerLaunchType()
	 * @generated
	 */
	void setServerLaunchType(ServerLaunchType value);

	/**
	 * Returns the value of the '<em><b>Rexec Port</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Rexec Port</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Rexec Port</em>' attribute.
	 * @see #setRexecPort(int)
	 * @model 
	 * @generated
	 */
	int getRexecPort();

	/**
	 * Set the REXEC port value, as an int
	 */
	public void setRexecPort(int newRexecPort);

	/**
	 * Sets whether or not to auto-detect SSL
	 */
	public void setAutoDetectSSL(boolean auto);

	boolean getAutoDetectSSL();

	int getDaemonPort();

	/**
	 * Set the DAEMON port value, as an int
	 */
	public void setDaemonPort(int newDaemonPort);

	/**
	 * Returns the value of the '<em><b>Server Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * The path where the server lives on the remote system. Used by at least the REXEC server launch type.
	 * Will be null if not set.
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Server Path</em>' attribute.
	 * @see #setServerPath(String)
	 */
	String getServerPath();

	/**
	 * Sets the value of the '{@link org.eclipse.rse.core.subsystems.IRemoteServerLauncher#getServerPath <em>Server Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * Set the path where the server lives on the remote system. Used by at least the REXEC server launch type. 
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Server Path</em>' attribute.
	 * @see #getServerPath()
	 * @generated
	 */
	void setServerPath(String value);

	/**
	 * Returns the value of the '<em><b>Server Script</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * The script to run on the remote system, to start the server code.
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Server Script</em>' attribute.
	 * @see #setServerScript(String)
	 */
	String getServerScript();

	/**
	 * Sets the value of the '{@link org.eclipse.rse.core.subsystems.IRemoteServerLauncher#getServerScript <em>Server Script</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * The script to run on the remote system, to start the server code. 
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Server Script</em>' attribute.
	 * @see #getServerScript()
	 * @generated
	 */
	void setServerScript(String value);

	/**
	 * Call this method to identify specific server launch types that are not to be permitted.
	 * This will disable these types in the property page, effectively preventing the user from
	 * specifying it. Note this is a transient property, so you should call it each time as part 
	 * of restoring your subsystem.
	 * <p>
	 * You normally do not call this! Rather, your subsystem factory class will override
	 * {@link org.eclipse.rse.core.subsystems.SubSystemConfiguration#supportsServerLaunchType(ServerLaunchType)}.
	 * However, this method is needed by ISVs that re-use predefined subsystem factories,
	 * and merely supply their own IConnectorService object via the "systemClass" attribute of the
	 * subsystemConfigurations extension point. They don't call this method directly actually, but
	 * rather {@link AbstractConnectorService#enableServerLaunchType(SubSystem, ServerLaunchType, boolean)},
	 * which in turn calls this.
	 * 
	 * @see org.eclipse.rse.core.subsystems.ServerLaunchType
	 */
	//	public void enableServerLaunchType(ServerLaunchType serverLaunchType, boolean enable);
	/**
	 * This methods returns the enablement state per server launch type.
	 * If {@link #setServerLaunchType(ServerLaunchType)} has not been
	 *  called for this server launch type, then we defer to the subsystem factory's
	 *  method: 
	 * {@link org.eclipse.rse.core.subsystems.ISubSystemConfiguration#supportsServerLaunchType(ServerLaunchType)}.
	 * @see org.eclipse.rse.core.subsystems.ServerLaunchType
	 */
	public boolean isEnabledServerLaunchType(ServerLaunchType serverLaunchType);
}
