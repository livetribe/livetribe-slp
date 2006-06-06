/*
 * Copyright 2006 the original author or authors
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
package org.livetribe.slp.api.sa;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.msg.SrvReg;

/**
 * Representation of a service, exposed by ServiceAgents and registered in DirectoryAgents.
 * <br />
 * In SLP, services are distinguished by their {@link ServiceURL} and by their language.
 * However, there is an asymmetry in SLP that allows <code>(ServiceURL1, language1)</code>
 * to be registered under two different ServiceTypes (this is allowed by the format of
 * {@link SrvReg}), but not deregistered from one ServiceType only (as {@link SrvDeReg}
 * does not support an additional ServiceType field).
 * <br />
 * {@link Attributes} are not involved in service equality since they can contain
 * locale-specific information (for example: <code>(color=Yellow)</code> with language English,
 * and <code>(color=Giallo)</code> with language Italian.
 * Two service registration with the same ServiceURL and language overwrite each other.
 * @version $Rev$ $Date$
 */
public class ServiceInfo
{
    private final Key key;
    private final ServiceType serviceType;
    private final Scopes scopes;
    private final Attributes attributes;

    public static ServiceInfo from(SrvReg message)
    {
        return new ServiceInfo(message.getServiceType(), message.getURLEntry().toServiceURL(), message.getScopes(), message.getAttributes(), message.getLanguage());
    }

    public static ServiceInfo from(SrvDeReg message)
    {
        return new ServiceInfo(message.getURLEntry().toServiceURL(), message.getScopes(), message.getTags(), message.getLanguage());
    }

    public ServiceInfo(ServiceURL serviceURL, Scopes scopes, Attributes attributes, String language)
    {
        this(null, serviceURL, scopes, attributes, language);
    }

    public ServiceInfo(ServiceType serviceType, ServiceURL serviceURL, Scopes scopes, Attributes attributes, String language)
    {
        this.key = new Key(serviceURL, language);
        this.serviceType = serviceType;
        this.scopes = scopes;
        this.attributes = attributes;
    }

//    public ServiceInfo clone(ServiceInfo other)
//    {
//        return new ServiceInfo(other.getServiceType(), other.getServiceURL(), other.getScopes(), other.getAttributes(), other.getLanguage());
//    }

    public Key getKey()
    {
        return key;
    }

    public ServiceType getServiceType()
    {
        return serviceType;
    }

    public ServiceURL getServiceURL()
    {
        return getKey().getServiceURL();
    }

    public Scopes getScopes()
    {
        return scopes;
    }

    public Attributes getAttributes()
    {
        return attributes;
    }

    public boolean hasAttributes()
    {
        return attributes != null && !attributes.isEmpty();
    }

    public String getLanguage()
    {
        return getKey().getLanguage();
    }

    public ServiceType resolveServiceType()
    {
        ServiceType result = getServiceType();
        if (result != null) return result;
        return getServiceURL().getServiceType();
    }

    public ServiceInfo merge(ServiceInfo that)
    {
        if (!getKey().equals(that.getKey())) return null;
        Attributes thisAttrs = getAttributes();
        Attributes thatAttrs = that.getAttributes();
        Attributes mergedAttrs = null;
        if (thisAttrs == null)
            mergedAttrs = thatAttrs == null ? null : thatAttrs.merge(null);
        else
            mergedAttrs = thisAttrs.merge(thatAttrs);
        return clone(getServiceType(), getServiceURL(), getScopes(), mergedAttrs, getLanguage());
    }

    public ServiceInfo unmerge(ServiceInfo that)
    {
        if (!getKey().equals(that.getKey())) return null;
        Attributes thatAttr = that.getAttributes();
        if (thatAttr == null || thatAttr.isEmpty()) return null;
        Attributes thisAttr = getAttributes();
        if (thisAttr == null) return new ServiceInfo(getServiceType(), getServiceURL(), getScopes(), getAttributes(), getLanguage());
        Attributes mergedAttrs = thisAttr.unmerge(thatAttr);
        return clone(getServiceType(), getServiceURL(), getScopes(), mergedAttrs, getLanguage());
    }

    protected ServiceInfo clone(ServiceType serviceType, ServiceURL serviceURL, Scopes scopes, Attributes attributes, String language)
    {
        return new ServiceInfo(serviceType, serviceURL, scopes, attributes, language);
    }

    /**
     * Services in SLP are identified by ServiceURL and language.
     * There is an asymmetry between SrvReg and SrvDereg,
     * in which SrvReg specify
     */
    public static class Key
    {
        private final ServiceURL serviceURL;
        private final String language;

        public Key(ServiceURL serviceURL, String language)
        {
            this.serviceURL = serviceURL;
            this.language = language;
        }

        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            final Key that = (Key)obj;
            if (!serviceURL.equals(that.serviceURL)) return false;
            return language == null ? that.language == null : language.equals(that.language);
        }

        public int hashCode()
        {
            int result = serviceURL.hashCode();
            if (language != null) result = 29 * result + language.hashCode();
            return result;
        }

        public ServiceURL getServiceURL()
        {
            return serviceURL;
        }

        public String getLanguage()
        {
            return language;
        }
    }
}
