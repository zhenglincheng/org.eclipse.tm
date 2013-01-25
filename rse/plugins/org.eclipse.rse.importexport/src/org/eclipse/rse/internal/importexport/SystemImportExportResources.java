/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *  David McKnight   (IBM)        - [223204] [cleanup] fix broken nls strings in files.ui and others
 *******************************************************************************/
package org.eclipse.rse.internal.importexport;

import org.eclipse.osgi.util.NLS;

public class SystemImportExportResources extends NLS {
	private static String BUNDLE_NAME = "org.eclipse.rse.internal.importexport.SystemImportExportResources";//$NON-NLS-1$

	// REMOTE FILE EXPORT WIZARD...
	public static String RESID_FILEEXPORT_TITLE;
	public static String RESID_FILEEXPORT_PAGE1_TITLE;
	public static String RESID_FILEEXPORT_PAGE1_DESCRIPTION;
	public static String RESID_FILEEXPORT_DESTINATION_LABEL;
	public static String RESID_FILEEXPORT_DESTINATION_TOOLTIP;
	public static String RESID_FILEEXPORT_DESTINATION_BROWSE_LABEL;
	public static String RESID_FILEEXPORT_DESTINATION_BROWSE_TOOLTIP;
	public static String RESID_FILEEXPORT_OPTION_OVERWRITE_LABEL;
	public static String RESID_FILEEXPORT_OPTION_OVERWRITE_TOOLTIP;
	public static String RESID_FILEEXPORT_OPTION_CREATEALL_LABEL;
	public static String RESID_FILEEXPORT_OPTION_CREATEALL_TOOLTIP;
	public static String RESID_FILEEXPORT_OPTION_CREATESEL_LABEL;
	public static String RESID_FILEEXPORT_OPTION_CREATESEL_TOOLTIP;
	public static String RESID_FILEEXPORT_OPTION_SETTINGS_LABEL;
	public static String RESID_FILEEXPORT_OPTION_SETTINGS_TOOLTIP;
	public static String RESID_FILEEXPORT_OPTION_SETTINGS_DESCFILE_LABEL;
	public static String RESID_FILEEXPORT_OPTION_SETTINGS_DESCFILE_PATH_TOOLTIP;
	public static String RESID_FILEEXPORT_OPTION_SETTINGS_DESCFILE_BROWSE_LABEL;
	public static String RESID_FILEEXPORT_OPTION_SETTINGS_DESCFILE_BROWSE_TOOLTIP;

	public static String RESID_FILEEXPORT_EXPORTING;
	// REMOTE FILE IMPORT WIZARD...
	public static String RESID_FILEIMPORT_TITLE;
	public static String RESID_FILEIMPORT_PAGE1_TITLE;
	public static String RESID_FILEIMPORT_PAGE1_DESCRIPTION;
	public static String RESID_FILEIMPORT_OPTION_OVERWRITE_LABEL;
	public static String RESID_FILEIMPORT_OPTION_OVERWRITE_TOOLTIP;
	public static String RESID_FILEIMPORT_OPTION_CREATEALL_LABEL;
	public static String RESID_FILEIMPORT_OPTION_CREATEALL_TOOLTIP;
	public static String RESID_FILEIMPORT_OPTION_CREATESEL_LABEL;
	public static String RESID_FILEIMPORT_OPTION_CREATESEL_TOOLTIP;
	public static String RESID_FILEIMPORT_OPTION_SETTINGS_LABEL;
	public static String RESID_FILEIMPORT_OPTION_SETTINGS_TOOLTIP;
	public static String RESID_FILEIMPORT_OPTION_SETTINGS_DESCFILE_LABEL;
	public static String RESID_FILEIMPORT_OPTION_SETTINGS_DESCFILE_PATH_TOOLTIP;
	public static String RESID_FILEIMPORT_OPTION_SETTINGS_DESCFILE_BROWSE_LABEL;
	public static String RESID_FILEIMPORT_OPTION_SETTINGS_DESCFILE_BROWSE_TOOLTIP;
	public static String RESID_FILEIMPORT_SOURCE_LABEL;
	public static String RESID_FILEIMPORT_SOURCE_TOOLTIP;

	public static String RESID_FILEIMPORT_IMPORTING;
	public static String RESID_FILEIMPEXP_BUTTON_SELECTALL_LABEL;
	public static String RESID_FILEIMPEXP_BUTTON_SELECTALL_TOOLTIP;
	public static String RESID_FILEIMPEXP_BUTTON_DESELECTALL_LABEL;
	public static String RESID_FILEIMPEXP_BUTTON_DESELECTALL_TOOLTIP;
	public static String RESID_FILEIMPEXP_BUTTON_SELECTTYPES_LABEL;
	public static String RESID_FILEIMPEXP_BUTTON_SELECTTYPES_TOOLTIP;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, SystemImportExportResources.class);
	}
}
