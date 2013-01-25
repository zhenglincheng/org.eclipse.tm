/********************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others. All rights reserved.
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
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 ********************************************************************************/

package org.eclipse.rse.ui.wizards;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.rse.ui.messages.SystemMessageDialog;

/**
 * Wizard for creating a new remote system profile.
 */
public class      SystemNewProfileWizard
  	   extends    AbstractSystemWizard
	   
{	
	
	private SystemNewProfileWizardMainPage mainPage;

    /**
     * Constructor
     */	
	public SystemNewProfileWizard()
	{
		super(SystemResources.RESID_NEWPROFILE_TITLE,
	  	      RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_NEWPROFILEWIZARD_ID));		      
	}
	
	/**
	 * Creates the wizard pages.
	 * This method is an override from the parent Wizard class.
	 */
	public void addPages()
	{
	   try {
	      mainPage = createMainPage();	        
	      addPage(mainPage);
	      //super.addPages();
	   } catch (Exception exc)
	   {
	   	 SystemBasePlugin.logError("New connection: Error in createPages: ",exc); //$NON-NLS-1$
	   }
	}

	/**
	 * Creates the wizard's main page. 
	 * This method is an override from the parent class.
	 */
	protected SystemNewProfileWizardMainPage createMainPage()
	{
	    mainPage = new SystemNewProfileWizardMainPage(this);
	    return mainPage;
	}    
	/**
	 * Completes processing of the wizard. If this
	 * method returns true, the wizard will close;
	 * otherwise, it will stay active.
	 * This method is an override from the parent Wizard class.
	 *
	 * @return whether the wizard finished successfully
	 */
	public boolean performFinish()
	{
		boolean ok = true;
		if (mainPage.performFinish())
		{
            //SystemMessage.showInformationMessage(getShell(),"Finish pressed.");				  	
            ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
            String name = mainPage.getProfileName();
            boolean makeActive = mainPage.getMakeActive();
            try 
            {
                 sr.createSystemProfile(name,makeActive);
            } catch (Exception exc)
            {
               	 String msg = "Exception creating profile "; //$NON-NLS-1$
               	 SystemBasePlugin.logError(msg,exc);
               	 //System.out.println(msg + exc.getMessage() + ": " + exc.getClass().getName());
               	 SystemMessageDialog.displayExceptionMessage(getShell(),exc);
            }
		    return ok;
		}
		else
		  ok = false;
	    return ok;
	}

} // end class