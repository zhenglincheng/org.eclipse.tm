/*******************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [226364][api][breaking] RemoteBaseServerLauncherForm should not implement RemoteServerLauncherConstants.
 * David McKnight   (IBM)        - [235577] [dstore][launcher] Number 0 is accepted as valid port number
 *******************************************************************************/

package org.eclipse.rse.ui.widgets;

import org.eclipse.rse.core.subsystems.IRemoteServerLauncher;
import org.eclipse.rse.core.subsystems.IServerLauncherProperties;
import org.eclipse.rse.core.subsystems.RemoteServerLauncherConstants;
import org.eclipse.rse.core.subsystems.ServerLaunchType;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemWidgetHelpers;
import org.eclipse.rse.ui.messages.ISystemMessageLine;
import org.eclipse.rse.ui.validators.SystemNumericVerifyListener;
import org.eclipse.rse.ui.validators.ValidatorPortInput;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * Comment goes here
 */
public class RemoteServerLauncherForm extends RemoteBaseServerLauncherForm
{

	private Button _radioDaemon, _radioRexec, _radioNone, _checkBoxSSL, _checkBoxRexecSSL, _checkBoxAutoDetect;
	private Text _fieldDaemonPort;
	private Label _labelDaemonPort;

	private Text _fieldRexecPath, _fieldRexecInvocation, _fieldRexecPort;
	private Label _labelRexecPath, _labelRexecInvocation, _labelRexecPort;

	private Composite _daemonControls, _rexecControls, _noneControls;

	private ValidatorPortInput       _daemonPortValidator;
	private ValidatorPortInput       _rexecPortValidator;


	private ServerLaunchType _origlaunchType;
	private String _origPath;
	private String _origInvocation;
	private int _origRexecPort;
	private int _origDaemonPort;
	private boolean _origUseSSL;
	private boolean _origAutoDetect;

	/**
	 * Constructor for EnvironmentVariablesForm.
	 * @param msgLine
	 */
	public RemoteServerLauncherForm(Shell shell, ISystemMessageLine msgLine)
	{
		super(shell, msgLine);
		_daemonPortValidator = new ValidatorPortInput();
		_rexecPortValidator = new ValidatorPortInput();
	}

	public boolean isDirty()
	{
		boolean isDirty = _origlaunchType != getLaunchType() ||
					!_origPath.equals(getServerInstallPath()) ||
					!_origInvocation.equals(getServerInvocation()) ||
					_origRexecPort != getREXECPortAsInt() ||
					_origDaemonPort != getDaemonPortAsInt() ||
					_origUseSSL != getUseSSL() ||
					_origAutoDetect != getAutoDetect();
		return isDirty;
	}

	public void disable()
	{
		_radioDaemon.setEnabled(false);
		_radioRexec.setEnabled(false);
		_radioNone.setEnabled(false);
		_fieldRexecInvocation.setEnabled(false);
		_fieldRexecPath.setEnabled(false);
		_fieldRexecPort.setEnabled(false);
		_fieldDaemonPort.setEnabled(false);
		_checkBoxSSL.setEnabled(false);
		_checkBoxRexecSSL.setEnabled(false);
		_checkBoxAutoDetect.setEnabled(false);
	}



	protected void createLauncherControls(Group group)
	{
		// daemon controls
		_radioDaemon =
			SystemWidgetHelpers.createRadioButton(
				group,
				SystemResources.RESID_PROP_SERVERLAUNCHER_RADIO_DAEMON,
				this);
		_radioDaemon.setToolTipText(
			SystemResources.RESID_PROP_SERVERLAUNCHER_RADIO_DAEMON_TOOLTIP);

		_daemonControls = SystemWidgetHelpers.createComposite(group, 1);
		GridLayout dlayout = new GridLayout();
		dlayout.numColumns = 3;
		GridData ddata = new GridData(GridData.FILL_HORIZONTAL);
		ddata.horizontalIndent = 20;

		GridData dd = new GridData();
		dd.widthHint = 30;
		String portRange = " (1-" + ValidatorPortInput.MAXIMUM_PORT_NUMBER + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		_labelDaemonPort =
			SystemWidgetHelpers.createLabel(
				_daemonControls,
				SystemResources.RESID_CONNECTION_DAEMON_PORT_LABEL + portRange);
		_fieldDaemonPort = SystemWidgetHelpers.createTextField(_daemonControls, this);
		_fieldDaemonPort.setToolTipText(SystemResources.RESID_CONNECTION_DAEMON_PORT_TIP);
		_fieldDaemonPort.setLayoutData(dd);
		_fieldDaemonPort.addVerifyListener(new SystemNumericVerifyListener());
		_daemonControls.setLayout(dlayout);
		_daemonControls.setLayoutData(ddata);

		// rexec controls
		_radioRexec =
			SystemWidgetHelpers.createRadioButton(
				group,
				SystemResources.RESID_PROP_SERVERLAUNCHER_RADIO_REXEC,
				this);
		_radioRexec.setToolTipText(
			SystemResources.RESID_PROP_SERVERLAUNCHER_RADIO_REXEC_TOOLTIP);

		_rexecControls = SystemWidgetHelpers.createComposite(group, 1);
		GridLayout layout = new GridLayout();
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalIndent = 20;
		_labelRexecPath =
			SystemWidgetHelpers.createLabel(
				_rexecControls,
				SystemResources.RESID_PROP_SERVERLAUNCHER_PATH);
		_fieldRexecPath = SystemWidgetHelpers.createTextField(_rexecControls, this);
		_fieldRexecPath.setToolTipText(
				SystemResources.RESID_PROP_SERVERLAUNCHER_PATH_TOOLTIP);

		Composite subRexecControls = SystemWidgetHelpers.createComposite(_rexecControls, 1);
		GridLayout l2 = new GridLayout();
		GridData d2 = new GridData(GridData.FILL_HORIZONTAL);
		l2.numColumns = 4;

		_labelRexecInvocation =
			SystemWidgetHelpers.createLabel(
				subRexecControls,
				SystemResources.RESID_PROP_SERVERLAUNCHER_INVOCATION);
		_fieldRexecInvocation = SystemWidgetHelpers.createTextField(subRexecControls, this);
		_fieldRexecInvocation.setToolTipText(
			SystemResources.RESID_PROP_SERVERLAUNCHER_INVOCATION_TOOLTIP);

		GridData d3 = new GridData();
		d3.widthHint = 30;

		_labelRexecPort =
			SystemWidgetHelpers.createLabel(
				subRexecControls,
				SystemResources.RESID_CONNECTION_PORT_LABEL + portRange);
		_fieldRexecPort = SystemWidgetHelpers.createTextField(subRexecControls, this);
		_fieldRexecPort.setToolTipText(SystemResources.RESID_CONNECTION_PORT_TIP);
		_fieldRexecPort.setLayoutData(d3);
		_fieldRexecPort.addVerifyListener(new SystemNumericVerifyListener());

		subRexecControls.setLayout(l2);
		subRexecControls.setLayoutData(d2);

		_checkBoxAutoDetect = SystemWidgetHelpers.createCheckBox(_rexecControls, SystemResources.RESID_SUBSYSTEM_AUTODETECT_LABEL, this);
		_checkBoxAutoDetect.setToolTipText(SystemResources.RESID_SUBSYSTEM_AUTODETECT_TIP);
		_checkBoxRexecSSL = SystemWidgetHelpers.createCheckBox(_rexecControls, SystemResources.RESID_SUBSYSTEM_SSL_LABEL, this);
		_checkBoxRexecSSL.setToolTipText(SystemResources.RESID_SUBSYSTEM_SSL_TIP);

		_rexecControls.setLayout(layout);
		_rexecControls.setLayoutData(data);

		// manual controls
		_radioNone =
			SystemWidgetHelpers.createRadioButton(
				group,
				SystemResources.RESID_PROP_SERVERLAUNCHER_RADIO_NONE,
				this);
		_radioNone.setToolTipText(
			SystemResources.RESID_PROP_SERVERLAUNCHER_RADIO_NONE_TOOLTIP);
		_noneControls = SystemWidgetHelpers.createComposite(group, 1);
		GridLayout nlayout = new GridLayout();
		GridData ndata = new GridData(GridData.FILL_HORIZONTAL);
		ndata.horizontalIndent = 20;
		_checkBoxSSL = SystemWidgetHelpers.createCheckBox(_noneControls, SystemResources.RESID_SUBSYSTEM_SSL_LABEL, this);
		_checkBoxSSL.setToolTipText(SystemResources.RESID_SUBSYSTEM_SSL_TIP);
		_noneControls.setLayout(nlayout);
		_noneControls.setLayoutData(ndata);

		// help
		SystemWidgetHelpers.setHelp(_radioDaemon, RSEUIPlugin.HELPPREFIX + "srln0001"); //$NON-NLS-1$
		SystemWidgetHelpers.setHelp(_radioRexec, RSEUIPlugin.HELPPREFIX + "srln0002"); //$NON-NLS-1$
		SystemWidgetHelpers.setHelp(_radioNone, RSEUIPlugin.HELPPREFIX + "srln0003"); //$NON-NLS-1$
		SystemWidgetHelpers.setHelp(_fieldRexecPath, RSEUIPlugin.HELPPREFIX + "srln0004"); //$NON-NLS-1$
		SystemWidgetHelpers.setHelp(_fieldRexecInvocation, RSEUIPlugin.HELPPREFIX + "srln0005"); //$NON-NLS-1$
	}

	protected void initDefaults()
	{
		_radioDaemon.setSelection(true);
		_fieldDaemonPort.setEnabled(_radioDaemon.getSelection());
		_fieldRexecPath.setEnabled(_radioRexec.getSelection());
		_labelRexecPath.setEnabled(_radioRexec.getSelection());
		_fieldRexecInvocation.setEnabled(_radioRexec.getSelection());
		_labelRexecInvocation.setEnabled(_radioRexec.getSelection());
		_labelRexecPort.setEnabled(_radioRexec.getSelection());
		_fieldRexecPort.setEnabled(_radioRexec.getSelection());
		_checkBoxRexecSSL.setEnabled(_radioRexec.getSelection());
		_checkBoxAutoDetect.setEnabled(_radioRexec.getSelection());
		_checkBoxSSL.setEnabled(_radioNone.getSelection());

		_fieldDaemonPort.setText(String.valueOf(RemoteServerLauncherConstants.DEFAULT_DAEMON_PORT));
		_fieldRexecPath.setText(RemoteServerLauncherConstants.DEFAULT_REXEC_PATH);
		_fieldRexecInvocation.setText(RemoteServerLauncherConstants.DEFAULT_REXEC_SCRIPT);
		_fieldRexecPort.setText(String.valueOf(RemoteServerLauncherConstants.DEFAULT_REXEC_PORT));
	}

	/**
	 * Set the initial values for the widgets, based on the server launcher
	 */
	public void initValues(IServerLauncherProperties launcher)
	{
		IRemoteServerLauncher isl = (IRemoteServerLauncher)launcher;

		ServerLaunchType type = isl.getServerLaunchType();
		String path = isl.getServerPath();
		String invocation = isl.getServerScript();
		int rexecport = isl.getRexecPort(); // changed from getPortAsInt via d54335
		int daemonPort = isl.getDaemonPort(); // defect 54335
		boolean useSSL = isl.getConnectorService().isUsingSSL();
		boolean autoDetectSSL = isl.getAutoDetectSSL();

		// find out if daemon can be launched
		boolean allowDaemon = isl.isEnabledServerLaunchType(ServerLaunchType.DAEMON_LITERAL);

		// find out if rexec can be launched
		boolean allowRexec = isl.isEnabledServerLaunchType(ServerLaunchType.REXEC_LITERAL);

		// find out if connect to running server should be allowed
		boolean allowNo = isl.isEnabledServerLaunchType(ServerLaunchType.RUNNING_LITERAL);

		// enable/disable as appropriate
		setDaemonLaunchEnabled(allowDaemon);
		setRexecLaunchEnabled(allowRexec);
		setNoLaunchEnabled(allowNo);

		setLaunchType(type);

		setDaemonPort(daemonPort);
		setServerInstallPath(path);
		setServerInvocation(invocation);
		setREXECPort(rexecport);
		setUseSSL(useSSL);
		setAutoDetect(autoDetectSSL);

		if (!allowDaemon && !allowRexec && !allowNo) {
			disable();
		}


		_origlaunchType = getLaunchType();
		_origPath = getServerInstallPath();
		_origInvocation = getServerInvocation();
		_origRexecPort = getREXECPortAsInt();
		_origDaemonPort = getDaemonPortAsInt();
		_origUseSSL = getUseSSL();
		_origAutoDetect = getAutoDetect();
	}

	/**
	 * Verify page contents on OK.
	 * @return true if all went well, false if error found.
	 */
	public boolean verify()
	{
		SystemMessage msg = null;
		ServerLaunchType launchType = getLaunchType();
		if (launchType == ServerLaunchType.REXEC_LITERAL)
		{
			String port = getREXECPort();
			
			int portAsInt = 0;
			if (port != null && port.length() > 0){
				try {
					portAsInt = Integer.parseInt(port);
				}
				catch (Exception e){
					portAsInt = 0;
				}
			}
						    
			msg = _rexecPortValidator.validate(port);

			// for daemons we don't allow 0
			if (msg == null && portAsInt == 0)
				msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_PORT_NOTVALID);
			
			if (msg == null)
			{
				String path = getServerInstallPath();

				if (path == null || path.length() == 0)
				{
					msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_COMM_PWD_BLANKFIELD);
				}

				if (msg != null)
				{
					_msgLine.setErrorMessage(msg.getLevelOneText());
				}
				else
				{
					_msgLine.clearErrorMessage();
				}
			}
			else
			{
				_msgLine.setErrorMessage(msg);
			}
		}
		else if (launchType == ServerLaunchType.DAEMON_LITERAL)
		{
			String port = getDaemonPort();
				
			int portAsInt = 0;
			if (port != null && port.length() > 0){
				try {
					portAsInt = Integer.parseInt(port);
				}
				catch (Exception e){
					portAsInt = 0;
				}
			}
			
			msg = _daemonPortValidator.validate(port);

			// for daemons we don't allow 0
			if (msg == null && portAsInt == 0)
				msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_PORT_NOTVALID);


			if (msg == null)
			{
				_msgLine.clearErrorMessage();
			}
			else
			{
				_msgLine.setErrorMessage(msg);
			}
		}
		else
		{
			_msgLine.clearErrorMessage();
		}
		notifyVerifyListeners();
		return (msg==null);
	}

	/**
	 * Update the actual values in the server launcher, from the widgets. Called on successful press of OK.
	 * @return true if all went well, false if something failed for some reason.
	 */
	public boolean updateValues(IServerLauncherProperties launcher)
	{
		ServerLaunchType launchType = getLaunchType();
		String path = getServerInstallPath();
		String invocation = getServerInvocation();
		int rexecPort = getREXECPortAsInt();
		int daemonPort = getDaemonPortAsInt();
		boolean useSSL = getUseSSL();
		boolean autoDetect = getAutoDetect();

		IRemoteServerLauncher isl = (IRemoteServerLauncher)launcher;
		isl.setServerLaunchType(launchType);
		isl.setServerPath(path);
		isl.setServerScript(invocation);
		isl.setRexecPort(rexecPort); // changed from setPort via d54335. Phil
		isl.setDaemonPort(daemonPort);
		isl.setAutoDetectSSL(autoDetect);
		isl.getConnectorService().setIsUsingSSL(useSSL);
		try
		{
			isl.getConnectorService().commit();
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}


	public void handleEvent(Event evt)
	{

		boolean useRexec = _radioRexec.getSelection();
		_fieldDaemonPort.setEnabled(_radioDaemon.getSelection());
		_fieldRexecPath.setEnabled(useRexec);
		_labelRexecPath.setEnabled(useRexec);
		_fieldRexecInvocation.setEnabled(useRexec);
		_labelRexecInvocation.setEnabled(useRexec);
		_fieldRexecPort.setEnabled(useRexec);
		_labelRexecPort.setEnabled(useRexec);
		_checkBoxAutoDetect.setEnabled(useRexec);
		_checkBoxRexecSSL.setEnabled(useRexec && !_checkBoxAutoDetect.getSelection());
		_checkBoxSSL.setEnabled(_radioNone.getSelection());

		verify();
	}

	protected ServerLaunchType getLaunchType()
	{
		if (_radioDaemon.getSelection())
			return ServerLaunchType.DAEMON_LITERAL;
		else if (_radioRexec.getSelection())
			return ServerLaunchType.REXEC_LITERAL;
		else if (_radioNone.getSelection())
			return ServerLaunchType.RUNNING_LITERAL;
		else
			return null;
	}

	protected boolean getUseSSL()
	{
		if (_radioRexec.getSelection())
			return _checkBoxRexecSSL.getSelection();
		else return _checkBoxSSL.getSelection();
	}

	protected boolean getAutoDetect()
	{
		if (_radioNone.getSelection()) return false;
		if (_radioRexec.getSelection())
			return _checkBoxAutoDetect.getSelection();
		else return true;
	}

	protected void setUseSSL(boolean use)
	{
		_checkBoxSSL.setSelection(use);
		_checkBoxRexecSSL.setSelection(use);
	}

	protected void setAutoDetect(boolean use)
	{
		_checkBoxAutoDetect.setSelection(use);
	}

	protected void setLaunchType(ServerLaunchType type)
	{
		if (type == ServerLaunchType.DAEMON_LITERAL)
		{
			_radioDaemon.setSelection(true);
			_radioRexec.setSelection(false);
			_radioNone.setSelection(false);
		}
		else if (type == ServerLaunchType.REXEC_LITERAL)
		{
			_radioRexec.setSelection(true);
			_radioDaemon.setSelection(false);
			_radioNone.setSelection(false);
		}
		else if (type == ServerLaunchType.RUNNING_LITERAL)
		{
			_radioNone.setSelection(true);
			_radioRexec.setSelection(false);
			_radioDaemon.setSelection(false);
		}
		else
		{
			_radioNone.setSelection(false);
			_radioRexec.setSelection(false);
			_radioDaemon.setSelection(false);
		}
	}


	/**
	 * Sets whether to enable daemon launch.
	 * @param enable <code>true</code> if daemon launch should be enabled, <code>false</code> otherwise.
	 */
	public void setDaemonLaunchEnabled(boolean enable)
	{
	    //_radioDaemon.setVisible(enable);
	    //_daemonControls.setVisible(enable);
	    //_daemonControls.getLayout().
	    _labelDaemonPort.setEnabled(enable);
	    _fieldDaemonPort.setEnabled(enable);

		_radioDaemon.setEnabled(enable);
	}

	/**
	 * Sets whether to enable rexec launch.
	 * @param enable <code>true</code> if rexec launch should be enabled, <code>false</code> otherwise.
	 */
	public void setRexecLaunchEnabled(boolean enable)
	{
	    /*
	    _radioRexec.setVisible(enable);
	    _rexecControls.setVisible(enable);
	    _labelRexecInvocation.setVisible(enable);
	    _labelRexecPath.setVisible(enable);
	    _labelRexecPort.setVisible(enable);
	    */
	    _fieldRexecInvocation.setEnabled(enable);
	    _fieldRexecPath.setEnabled(enable);
	    _fieldRexecPort.setEnabled(enable);

		_radioRexec.setEnabled(enable);
	}

	public void setHostname(String hostname)
	{
	    _hostName = hostname;
	}

	/**
	 * Set the daemon port widget value
	 * @param port - the port value as a string
	 */
	public void setDaemonPort(String port)
	{
		_fieldDaemonPort.setText(port);
	}

	/**
	 * Set the daemon port widget value
	 * @param port - the port value as an int
	 */
	public void setDaemonPort(int port)
	{
		_fieldDaemonPort.setText(Integer.toString(port));
	}

	/**
	 * Get the Daemon port widget value
	 * @return the widget's current value as an int
	 */
	public int getDaemonPortAsInt()
	{
		int port = 0;
		try {
			port = Integer.parseInt(_fieldDaemonPort.getText().trim());
		} catch (Exception exc) { }
		return port;
	}
	/**
	 * Get the daemon port widget value
	 * @return the widget's current value as a string
	 */
	public String getDaemonPort()
	{
		return _fieldDaemonPort.getText().trim();
	}

	/**
	 * Set the REXEC port's widget value, as a String
	 * @param port - the value to apply to the widget
	 */
	public void setREXECPort(String port)
	{
		_fieldRexecPort.setText(port);
	}
	/**
	 * Set the REXEC port's widget value, given an int port value
	 * @param port - the value to apply to the widget.
	 */
	public void setREXECPort(int port)
	{
		_fieldRexecPort.setText(Integer.toString(port));
	}
	/**
	 * Get the REXEC port widget value
	 * @return the widget's current value as an int
	 */
	public int getREXECPortAsInt()
	{
		int port = 0;
		try {
			port = Integer.parseInt(_fieldRexecPort.getText().trim());
		} catch (Exception exc) { }
		return port;
	}
	/**
	 * Get the REXEC port widget value
	 * @return the widget's current value as a string
	 */
	public String getREXECPort()
	{
		return _fieldRexecPort.getText().trim();
	}


	/**
	 * Sets whether to enable no launch.
	 * @param enable <code>true</code> if no launch should be enabled, <code>false</code> otherwise.
	 */
	public void setNoLaunchEnabled(boolean enable)
	{
		_radioNone.setEnabled(enable);
		_checkBoxSSL.setEnabled(enable);
	}
	/**
	 * Return the current value of the REXEC server install path widget
	 * @return widget value as a string
	 */
	public String getServerInstallPath()
	{
		return _fieldRexecPath.getText().trim();
	}
	/**
	 * Set the REXEC server install path widget's value
	 * @param path - the text to set the widget's value to
	 */
	public void setServerInstallPath(String path)
	{
		_fieldRexecPath.setText(path);
	}
	/**
	 * Return the current value of the REXEC server invocation widget
	 * @return widget value as a string
	 */
	public String getServerInvocation()
	{
		return _fieldRexecInvocation.getText();
	}
	/**
	 * Set the REXEC server invocation widget's value
	 * @param invocation - the text to set the widget's value to
	 */
	public void setServerInvocation(String invocation)
	{
		_fieldRexecInvocation.setText(invocation);
	}
}
