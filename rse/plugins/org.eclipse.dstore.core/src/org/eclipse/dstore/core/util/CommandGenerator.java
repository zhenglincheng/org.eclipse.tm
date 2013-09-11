/*******************************************************************************
 * Copyright (c) 2002, 2013 IBM Corporation and others.
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
 * David McKnight   (IBM) - [225507][api][breaking] RSE dstore API leaks non-API types
 * David McKnight   (IBM) - [226561] [apidoc] Add API markup to RSE Javadocs where extend / implement is allowed
 * David McKnight   (IBM) - [390037] [dstore] Duplicated items in the System view
 * David McKnight   (IBM) - [396440] [dstore] fix issues with the spiriting mechanism and other memory improvements (phase 1)
 * David McKnight   (IBM) - [416048] [dstore] Using a remote shell does not work on Windows 7/Windows Server 2008r2
 *******************************************************************************/

package org.eclipse.dstore.core.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dstore.core.model.DE;
import org.eclipse.dstore.core.model.DataElement;
import org.eclipse.dstore.core.model.DataStore;
import org.eclipse.dstore.core.model.DataStoreResources;

/**
 * This class is used to generate command object instances from command
 * descriptors and arguments to commands. Command instances are instances of
 * command descriptors. Each command instance contains a set of data arguments
 * and a status object, that represents the current state of a command. After a
 * command instance is created, it is referenced in the command log for the
 * DataStore.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 3.0 moved from non-API to API
 */
public class CommandGenerator
{
    private DataStore _dataStore = null;
    private DataElement _log = null;


	/**
	 * Constructor
	 */
    public CommandGenerator()
    {
    }

	/**
	 * Sets the associated DataStore
	 * @param dataStore the associated DataStore
	 */
    public void setDataStore(DataStore dataStore)
    {
        _dataStore = dataStore;
        _log = _dataStore.getLogRoot();
    }

	/**
	 * This method logs the current command object in the DataStore command log.  For each
	 * logged command, a status object is created and returned.
	 * @param commandObject the commandObject to log
	 * @return the status object of the command
	 */
    public DataElement logCommand(DataElement commandObject)
    {
        try
        {
            // create time and status objects
        	StringBuffer id = new StringBuffer(commandObject.getId());
        	id.append(DataStoreResources.model_status);
            _dataStore.createObject(
                    commandObject,
                   DataStoreResources.model_status,
                   DataStoreResources.model_start,
                    "", //$NON-NLS-1$
                   	id.toString());

            _log.addNestedData(commandObject, false);

        }
        catch (Exception e)
        {
            _dataStore.trace(e);
        }

        return commandObject;
    }

	/**
	 * Creates a new command instance object from a command descriptor
	 * @param commandDescriptor the descriptor of the command to create
	 * @return the new command instance
	 */
    public DataElement createCommand(DataElement commandDescriptor)
    {
        if (commandDescriptor != null)
        {
            if (commandDescriptor.getType().equals(DE.T_COMMAND_DESCRIPTOR))
            {
                DataElement commandInstance = _dataStore.createObject(null, commandDescriptor.getName(), commandDescriptor.getValue(), commandDescriptor.getSource());
                commandInstance.setDescriptor(commandDescriptor);
                return commandInstance;
            }
            else
            {
                System.out.println("not cd -> " + commandDescriptor); //$NON-NLS-1$
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    private void clearDeleted(DataElement element)
    {
    	for (int i = 0; i < element.getNestedSize(); i++)
    	{
    		DataElement child = element.get(i);
    		if (child != null && child.isDeleted())
    		{
    			element.removeNestedData(child);
    		}
    	}
    }

	/**
	 * Creates a new command from a command descriptor and it's arguments.
	 *
	 * @param commandDescriptor the command type of the new command
	 * @param arguments the arguments for the command, besides the subject
	 * @param dataObject the subject of the command
	 * @param refArg indicates whether the subject should be represented as a reference or directly
	 * @return the status object of the command
	 */
    public DataElement generateCommand(DataElement commandDescriptor, ArrayList arguments, DataElement dataObject, boolean refArg)
    {
    	//refArg = false;
       	boolean subjectIsCommand = false;
        DataElement commandObject = createCommand(commandDescriptor);
        if (commandObject != null)
        {
        	if (commandObject.getSource().equals("*")){ //$NON-NLS-1$
        		// qualify it if we can
        		DataElement subjectDescriptor = dataObject.getDescriptor();
        		if (subjectDescriptor != null){
        			if (subjectDescriptor.getType().equals(DE.T_COMMAND_DESCRIPTOR)){
        				commandObject.setAttribute(DE.A_SOURCE, dataObject.getSource());
        				subjectIsCommand = true;
        			}
        		}
        	}
        	
        	if (dataObject.isDescriptor()){
        		//System.out.println("using descriptor as command subject! - " + dataObject); //$NON-NLS-1$
        		//System.out.println("Command is "+commandObject.getName()); //$NON-NLS-1$
        		refArg = true;
        	}
        	clearDeleted(dataObject);

            commandObject.setAttribute(DE.A_VALUE, commandDescriptor.getName());

            if ((refArg || subjectIsCommand) && !dataObject.isSpirit())
            {
                _dataStore.createReference(commandObject, dataObject,DataStoreResources.model_contents);
            }
            else
            {
               	dataObject.setPendingTransfer(true);
            	if (dataObject.isSpirit() && _dataStore.isVirtual()){
            		// resurrecting spirited element
            		dataObject.setSpirit(false);            		
            		// clear out old data - otherwise, we can end up with duplicates
            		dataObject.removeNestedData();
            	}   
            	
                 commandObject.addNestedData(dataObject, false);
            }

            if (arguments != null)
            {
                for (int i = 0; i < arguments.size(); i++)
                {
                    DataElement arg = (DataElement)arguments.get(i);
                    if (arg != null)
                    {
                        if (!arg.isUpdated() || arg.isSpirit() || !refArg)
                        {
                            commandObject.addNestedData(arg, false);
                        }
                        else
                        {
                            _dataStore.createReference(commandObject, arg, "argument"); //$NON-NLS-1$
                        }
                    }
                }
            }

            return logCommand(commandObject);
        }
        else
        {
            return null;
        }
    }


	/**
	 * Creates a new command from a command descriptor and it's arguments.
	 *
	 * @param commandDescriptor the command type of the new command
	 * @param arg the arguement for the command, besides the subject
	 * @param dataObject the subject of the command
	 * @param refArg indicates whether the subject should be represented as a reference or directly
	 * @return the status object of the command
	 */
    public DataElement generateCommand(DataElement commandDescriptor, DataElement arg, DataElement dataObject, boolean refArg)
    {
        //refArg = false;
    	boolean subjectIsCommand = false;
        DataElement commandObject = createCommand(commandDescriptor);
        if (commandObject != null)
        {
        	if (commandObject.getSource().equals("*")){ //$NON-NLS-1$
        		// qualify it if we can
        		DataElement subjectDescriptor = dataObject.getDescriptor();
        		if (subjectDescriptor != null){
        			if (subjectDescriptor.getType().equals(DE.T_COMMAND_DESCRIPTOR)){
        				commandObject.setAttribute(DE.A_SOURCE, dataObject.getSource());
        				subjectIsCommand = true;
        			}
        		}
        	}

            commandObject.setAttribute(DE.A_VALUE, commandDescriptor.getName());
            
         	
        	if (dataObject.isDescriptor()){
        		//System.out.println("using descriptor as command subject! - " + dataObject); //$NON-NLS-1$
        		//System.out.println("Command is "+commandObject.getName()); //$NON-NLS-1$
        		refArg = true;
        	}
			clearDeleted(dataObject);
            if ((refArg || subjectIsCommand) && !dataObject.isSpirit())
            {
                _dataStore.createReference(commandObject, dataObject,DataStoreResources.model_contents);
            }
            else
            {
            	dataObject.setPendingTransfer(true);
               	if (dataObject.isSpirit() && _dataStore.isVirtual()){
               		// resurrecting spirited element
            		dataObject.setSpirit(false);            		
            		// clear out old data - otherwise, we can end up with duplicates
            		dataObject.removeNestedData();
            	}   
                commandObject.addNestedData(dataObject, false);
            }

            if (!arg.isUpdated() || arg.isSpirit() || !refArg)
            {
                commandObject.addNestedData(arg, false);
            }
            else
            {
                _dataStore.createReference(commandObject, arg, "argument"); //$NON-NLS-1$
            }


            return logCommand(commandObject);
        }
        else
        {
            return null;
        }
    }

	/**
	 * Creates a new command from a command descriptor and it's arguments.
	 *
	 * @param commandDescriptor the command type of the new command
	 * @param dataObject the subject of the command
	 * @param refArg indicates whether the subject should be represented as a reference or directly
	 * @return the status object of the command
	 */
    public DataElement generateCommand(DataElement commandDescriptor, DataElement dataObject, boolean refArg)
    {
    	//refArg = false;
    	boolean subjectIsCommand = false;
        DataElement commandObject = createCommand(commandDescriptor);
        if (commandObject != null)
        {
        	if (commandObject.getSource().equals("*")){ //$NON-NLS-1$
        		// qualify it if we can
        		DataElement subjectDescriptor = dataObject.getDescriptor();
        		if (subjectDescriptor != null){
        			if (subjectDescriptor.getType().equals(DE.T_COMMAND_DESCRIPTOR)){
        				commandObject.setAttribute(DE.A_SOURCE, dataObject.getSource());
        				subjectIsCommand = true;
        			}
        		}
        	}
        	
            commandObject.setAttribute(DE.A_VALUE, commandDescriptor.getName());

         	
        	if (dataObject.isDescriptor()){
        		//System.out.println("using descriptor as command subject! - " + dataObject); //$NON-NLS-1$
        		//System.out.println("Command is "+commandObject.getName()); //$NON-NLS-1$
        		refArg = true;
        	}
			clearDeleted(dataObject);
            if ((refArg || subjectIsCommand) && !dataObject.isSpirit())
            {
                _dataStore.createReference(commandObject, dataObject,DataStoreResources.model_arguments);
            }
            else
            {
            	dataObject.setPendingTransfer(true);
               	if (dataObject.isSpirit() && _dataStore.isVirtual()){
            		// resurrecting spirited element
            		dataObject.setSpirit(false);            		
            		// clear out old data - otherwise, we can end up with duplicates
            		dataObject.removeNestedData();
            	}   
                commandObject.addNestedData(dataObject, false);
            }

            return logCommand(commandObject);
        }
        else
        {
            return null;
        }
    }

	/**
	 * Creates a response tree for transmitting a set of data from a server to a client.
	 *
	 * @param document the root of the response
	 * @param objects the data contained in the response
	 * @return the response tree root
	 */
    public DataElement generateResponse(DataElement document, ArrayList objects)
    {
        document.addNestedData(objects, false);
        return document;
    }

	/**
	 * Creates a response tree for transmitting a set of data from a server to a client.
	 *
	 * @param responseType the type of data to respond with
	 * @param dataObject the child object in the response tree
	 * @return the response tree root
	 */
    public DataElement generateResponse(String responseType, DataElement dataObject)
    {
        if (dataObject != null)
        {
            DataElement commandObject = _dataStore.createObject(null, "RESPONSE", responseType); //$NON-NLS-1$
            commandObject.addNestedData(dataObject, true);
            return commandObject;
        }
        else
        {
            return null;
        }
    }

	/**
	 * Creates a simple response object of the specified type
	 *
	 * @param responseType the type of data to respond with
	 * @return the response object
	 */
    public DataElement generateResponse(String responseType)
    {
        DataElement commandObject = _dataStore.createObject(null, "RESPONSE", responseType); //$NON-NLS-1$
        return commandObject;
    }
}
