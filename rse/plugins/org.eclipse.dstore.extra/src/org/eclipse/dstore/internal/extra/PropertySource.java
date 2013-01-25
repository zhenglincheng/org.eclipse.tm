/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
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

package org.eclipse.dstore.internal.extra;

import java.util.HashMap;
import java.util.List;

import org.eclipse.dstore.extra.IDataElement;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

public class PropertySource implements IPropertySource
{


  private IDataElement   _dataElement;
  private HashMap        _properties;
  private IPropertyDescriptor[] _descriptors;
  
  public PropertySource(IDataElement element)
  {
    _dataElement = element;
    
    _properties = new HashMap();
    
    IDataElement descriptor = (IDataElement)element.getElementProperty("descriptor"); //$NON-NLS-1$
    
    List attributes = null;
    int attributesSize = 0;
    if (descriptor != null)
	{
	    attributes = descriptor.getAssociated("attributes"); //$NON-NLS-1$
	    attributesSize = attributes.size();
	}
	
	_descriptors = new IPropertyDescriptor[attributesSize + 2];
	_descriptors[0] = new TextPropertyDescriptor("type", "type"); //$NON-NLS-1$ //$NON-NLS-2$
	_descriptors[1] = new TextPropertyDescriptor("name", "name"); //$NON-NLS-1$ //$NON-NLS-2$
	if (attributes != null)
	{
		for (int i = 0; i < attributesSize; i++)
	    {
			IDataElement attribute = (IDataElement)attributes.get(i);
			List types = attribute.getAssociated("attributes"); //$NON-NLS-1$
			    
			String type = null;
			if (types.size() > 0)
			  type = ((IDataElement)types.get(0)).getName();
			else
			  type = "String"; //$NON-NLS-1$
			
			_properties.put(attribute.getName(), type);
			_descriptors[i+2] = new TextPropertyDescriptor(attribute.getName(), attribute.getName());
	    }
	}
    
  }

  public static boolean matches(Class aClass)
  {
    return (aClass == org.eclipse.ui.views.properties.IPropertySource.class);    
  }
  

  public Object getEditableValue()
  {
    return this;
  }

  public IPropertyDescriptor[] getPropertyDescriptors()
  {
    return _descriptors;
  }

  public Object getPropertyValue(Object name)
    {
	return getPropertyValue((String)name);
    }

  public Object getPropertyValue(String name)
  {
      Object result = null;
            
      // find the appropriate attribute
      List attributes = _dataElement.getAssociated("attributes"); //$NON-NLS-1$
      for (int i = 0; i < attributes.size(); i++)
	  {
	      IDataElement attribute = (IDataElement)attributes.get(i);
	      if (attribute.getType().equals(name))
		  {
		      result = attribute.getElementProperty("value"); //$NON-NLS-1$
		  }
	  }
	  
	  if (result == null)
	  {
	  	String type = (String)_properties.get(name);
	  	
	  	if (type != null && type.equals("Integer")) //$NON-NLS-1$
	  	  result = "0"; //$NON-NLS-1$
	  	else if (type != null && type.equals("Float")) //$NON-NLS-1$
	  	  result = "0.0"; //$NON-NLS-1$
	  	else
	  	  result = _dataElement.getElementProperty(name);
	  }
	  
      return result;
  }

  public boolean isPropertySet(Object property)
  {
      return isPropertySet((String)property);
  }

  public boolean isPropertySet(String property)
  {
    return false;
  }

  public void resetPropertyValue(Object property)
    {
    }

  public void resetPropertyValue(String property)
  {
  }

  public void setPropertyValue(Object name, Object value)
    {
	setPropertyValue((String)name, value);
    }

  public void setPropertyValue(String name, Object value)
  {
  }

}
