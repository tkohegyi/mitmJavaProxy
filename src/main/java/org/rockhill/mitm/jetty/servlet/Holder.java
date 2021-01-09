//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.rockhill.mitm.jetty.servlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.Registration;
import javax.servlet.ServletContext;

import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Holder
 *
 * Specialization of AbstractHolder for servlet-related classes that
 * have init-params etc
 *
 * @param <T> the type of holder
 */
@ManagedObject("Holder - a container for servlets and the like")
public abstract class Holder<T> extends BaseHolder<T>
{
    private static final Logger LOG = Log.getLogger(Holder.class);

    private final Map<String, String> _initParams = new HashMap<String, String>(3);
    private String _displayName;
    private boolean _asyncSupported;
    private String _name;

    protected Holder(Source source)
    {
        super(source);
        switch (getSource().getOrigin())
        {
            case JAVAX_API:
            case DESCRIPTOR:
            case ANNOTATION:
                _asyncSupported = false;
                break;
            default:
                _asyncSupported = true;
        }
    }

    @ManagedAttribute(value = "Display Name", readonly = true)
    public String getDisplayName()
    {
        return _displayName;
    }

    public String getInitParameter(String param)
    {
        if (_initParams == null)
            return null;
        return _initParams.get(param);
    }

    public Enumeration<String> getInitParameterNames()
    {
        if (_initParams == null)
            return Collections.enumeration(Collections.EMPTY_LIST);
        return Collections.enumeration(_initParams.keySet());
    }

    @ManagedAttribute(value = "Initial Parameters", readonly = true)
    public Map<String, String> getInitParameters()
    {
        return _initParams;
    }

    @ManagedAttribute(value = "Name", readonly = true)
    public String getName()
    {
        return _name;
    }

    @Override
    protected synchronized void setInstance(T instance)
    {
        super.setInstance(instance);
        if (getName() == null)
            setName(String.format("%s@%x", instance.getClass().getName(), instance.hashCode()));
    }

    public void destroyInstance(Object instance)
        throws Exception
    {
    }

    /**
     * @param className The className to set.
     */
    @Override
    public void setClassName(String className)
    {
        super.setClassName(className);
        if (_name == null)
            _name = className + "-" + Integer.toHexString(this.hashCode());
    }

    /**
     * @param held The class to hold
     */
    @Override
    public void setHeldClass(Class<? extends T> held)
    {
        super.setHeldClass(held);
        if (held != null)
        {
            if (_name == null)
                _name = held.getName() + "-" + Integer.toHexString(this.hashCode());
        }
    }

    public void setDisplayName(String name)
    {
        _displayName = name;
    }

    public void setInitParameter(String param, String value)
    {
        _initParams.put(param, value);
    }

    public void setInitParameters(Map<String, String> map)
    {
        _initParams.clear();
        _initParams.putAll(map);
    }

    /**
     * The name is a primary key for the held object.
     * Ensure that the name is set BEFORE adding a Holder
     * (eg ServletHolder or FilterHolder) to a ServletHandler.
     *
     * @param name The name to set.
     */
    public void setName(String name)
    {
        _name = name;
    }

    public void setAsyncSupported(boolean suspendable)
    {
        _asyncSupported = suspendable;
    }

    public boolean isAsyncSupported()
    {
        return _asyncSupported;
    }

    @Override
    public String dump()
    {
        return super.dump();
    }

    @Override
    public String toString()
    {
        return String.format("%s@%x==%s", _name, hashCode(), getClassName());
    }

    protected class HolderConfig
    {

        public ServletContext getServletContext()
        {
            return getServletHandler().getServletContext();
        }

        public String getInitParameter(String param)
        {
            return Holder.this.getInitParameter(param);
        }

        public Enumeration<String> getInitParameterNames()
        {
            return Holder.this.getInitParameterNames();
        }
    }

    protected class HolderRegistration implements Registration.Dynamic
    {
        @Override
        public void setAsyncSupported(boolean isAsyncSupported)
        {
            illegalStateIfContextStarted();
            Holder.this.setAsyncSupported(isAsyncSupported);
        }

        public void setDescription(String description)
        {
            if (LOG.isDebugEnabled())
                LOG.debug(this + " is " + description);
        }

        @Override
        public String getClassName()
        {
            return Holder.this.getClassName();
        }

        @Override
        public String getInitParameter(String name)
        {
            return Holder.this.getInitParameter(name);
        }

        @Override
        public Map<String, String> getInitParameters()
        {
            return Holder.this.getInitParameters();
        }

        @Override
        public String getName()
        {
            return Holder.this.getName();
        }

        @Override
        public boolean setInitParameter(String name, String value)
        {
            illegalStateIfContextStarted();
            if (name == null)
            {
                throw new IllegalArgumentException("init parameter name required");
            }
            if (value == null)
            {
                throw new IllegalArgumentException("non-null value required for init parameter " + name);
            }
            if (Holder.this.getInitParameter(name) != null)
                return false;
            Holder.this.setInitParameter(name, value);
            return true;
        }

        @Override
        public Set<String> setInitParameters(Map<String, String> initParameters)
        {
            illegalStateIfContextStarted();
            Set<String> clash = null;
            for (Map.Entry<String, String> entry : initParameters.entrySet())
            {
                if (entry.getKey() == null)
                {
                    throw new IllegalArgumentException("init parameter name required");
                }
                if (entry.getValue() == null)
                {
                    throw new IllegalArgumentException("non-null value required for init parameter " + entry.getKey());
                }
                if (Holder.this.getInitParameter(entry.getKey()) != null)
                {
                    if (clash == null)
                        clash = new HashSet<String>();
                    clash.add(entry.getKey());
                }
            }
            if (clash != null)
                return clash;
            Holder.this.getInitParameters().putAll(initParameters);
            return Collections.emptySet();
        }
    }
}





