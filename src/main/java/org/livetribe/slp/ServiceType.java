/*
 * Copyright 2005 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * @version $Rev$ $Date$
 */
public class ServiceType implements Serializable
{
    private static final String SERVICE = "service:";

    private final String type;
    private transient boolean isServiceURL;
    private transient boolean isAbstract;
    private transient String abstractName;
    private transient String protocolName;
    private transient String concreteName;
    private transient String namingAuthority;

    public ServiceType(String type)
    {
        this.type = type;
        parse(type);
    }

    /**
     * Returns true if this service type begins with the string <code>service:</code>.
     */
    public boolean isServiceURL()
    {
        return isServiceURL;
    }

    /**
     * Returns true if this service type is of the form <code>[service:]abstract:concrete</code>.
     */
    public boolean isAbstractType()
    {
        return isAbstract;
    }

    public boolean isNADefault()
    {
        return namingAuthority.length() == 0;
    }

    /**
     * Returns the concrete type name of this service type.
     * For service types of the form <code>[service:]foo:bar</code> returns <code>bar</code>.
     * For service types of the form <code>[service:]foo</code> returns the empty string.
     */
    public String getConcreteTypeName()
    {
        return concreteName;
    }

    /**
     * Returns the protocol type name of this service type.
     * For service types of the form <code>[service:]foo:bar</code> returns <code>foo</code>.
     * For service types of the form <code>[service:]foo</code> returns the <code>foo</code>.
     */
    public String getPrincipleTypeName()
    {
        return protocolName;
    }

    /**
     * Returns the abstract type name of this service type.
     * For service types of the form <code>[service:]foo:bar</code> returns <code>[service:]foo</code>.
     * For service types of the form <code>[service:]foo</code> returns the empty string.
     */
    public String getAbstractTypeName()
    {
        return abstractName;
    }

    /**
     * Returns the naming authority of this service type.
     * For service types of the form <code>[service:]foo:bar.baz</code> returns <code>baz</code>.
     * For service types of the form <code>[service:]foo.baz</code> returns <code>baz</code>.
     * For service types of the form <code>[service:][foo:]bar</code> returns the empty string.
     */
    public String getNamingAuthority()
    {
        return namingAuthority;
    }

    /**
     * Returns true if this service type matches the given service type, false otherwise.
     * <br />
     * If this service types is equal to the given service type, the service types match.
     * <br />
     * The <code>service:</code> prefix is not influent in matching, so
     * <code>service:foo:bar</code> matches <code>foo:bar</code>.
     * <br />
     * <code>service:foo</code> matches <code>service:foo:bar</code>, <code>foo:bar</code> and <code>foo</code>.
     * @param serviceType The service type to match against
     */
    public boolean matches(ServiceType serviceType)
    {
        if (serviceType == null) return false;
        if (equals(serviceType)) return true;

        if (isAbstractType())
        {
            if (serviceType.isAbstractType())
            {
                if (!getPrincipleTypeName().equals(serviceType.getPrincipleTypeName())) return false;
                return getConcreteTypeName().equals(serviceType.getConcreteTypeName());
            }
            else
            {
                return false;
            }
        }
        else
        {
            if (serviceType.isAbstractType())
            {
                return getPrincipleTypeName().equals(serviceType.getPrincipleTypeName());
            }
            else
            {
                return getPrincipleTypeName().equals(serviceType.getPrincipleTypeName());
            }
        }
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final ServiceType that = (ServiceType)obj;
        return type.equals(that.type);
    }

    public int hashCode()
    {
        return type.hashCode();
    }

    public String toString()
    {
        return type;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException
    {
        ois.defaultReadObject();
        parse(type);
    }

    private void parse(String serviceType)
    {
        if (serviceType.startsWith(SERVICE))
        {
            isServiceURL = true;
            serviceType = serviceType.substring(SERVICE.length());
        }

        int colon = serviceType.indexOf(':');
        if (colon >= 0)
        {
            isAbstract = true;
            protocolName = serviceType.substring(0, colon);
            abstractName = (isServiceURL() ? SERVICE : "") + protocolName;
            concreteName = serviceType.substring(colon + 1);
        }
        else
        {
            isAbstract = false;
            protocolName = serviceType;
            abstractName = "";
            concreteName = "";
        }

        String candidateForNamingAuthority = isAbstractType() ? getConcreteTypeName() : getPrincipleTypeName();
        int dot = candidateForNamingAuthority.indexOf(".");
        if (dot >= 0)
        {
            namingAuthority = candidateForNamingAuthority.substring(dot + 1);
            if (isAbstractType())
            {
                concreteName = candidateForNamingAuthority.substring(0, dot);
            }
            else
            {
                protocolName = candidateForNamingAuthority.substring(0, dot);
            }
        }
        else
        {
            namingAuthority = "";
        }
    }
}
