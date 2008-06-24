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

import java.io.Serializable;

/**
 * Services that offer the same functionalities are characterized by the same <code>ServiceType</code>.
 * The <code>ServiceType</code> forms the initial part of a {@link ServiceURL}, and it is used to both
 * characterize the service and to be used to lookup services that offer particular functionalities.
 * <code>ServiceType</code>s are represented in this form:
 * <pre>
 * [service:][&lt;abstract-type&gt;:]&lt;concrete-type&gt;[.&lt;naming-authority&gt;]
 * </pre>
 * The <code>service:</code> string can be omitted, though it is normally used to identify service types
 * exposed by SLP.
 * <br />
 * The <code>abstract-type</code> denotes a type name for a service that can be exposed over a number of
 * different protocols. For example, JMX exposes connector servers (the services) over different
 * protocols, and their service type can be <code>service:jmx:rmi</code> or <code>service:jmx:jmxmp</code>,
 * where <code>rmi</code> and <code>jmxmp</code> are the protocols used for the wire communication.
 * <br />
 * The <code>concrete-type</code> denotes a type name for the protocol used by the service to expose its
 * functionalities.
 * <br />
 * The <code>naming-authority</code> denotes the name of an organization that defined the service type.
 * @see ServiceURL
 * @version $Rev$ $Date$
 */
public class ServiceType implements Serializable
{
    private static final long serialVersionUID = 4196123698679426411L;
    private static final String SERVICE = "service:";

    private final String type;
    private transient boolean isServiceURL;
    private transient boolean isAbstract;
    private transient String abstractName;
    private transient String protocolName;
    private transient String concreteName;
    private transient String namingAuthority;

    /**
     * Creates a <code>ServiceType</code> parsing the given string.
     * @param type The string to be parsed
     */
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

    /**
     * Returns true if this service type does not specify a naming authority.
     */
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
     * For service types of the form <code>[service:]foo</code> returns <code>foo</code>.
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

    /**
     * Returns the string form of this service type, that can be passed to {@link #ServiceType(String)} to be parsed.
     */
    public String asString()
    {
        return type;
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

    @Override
    public String toString()
    {
        return asString();
    }
}
