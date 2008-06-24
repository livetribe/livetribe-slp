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
package org.livetribe.slp;

import java.util.concurrent.TimeUnit;

import org.livetribe.slp.srv.msg.SrvDeReg;
import org.livetribe.slp.srv.msg.SrvReg;

/**
 * This class represents a service, exposed by ServiceAgents and cached by DirectoryAgents.
 * <br />
 * In SLP, services are distinguished by their {@link ServiceURL} and by their language; these two elements
 * form the {@link Key key} of the service. Two service registration with the same key overwrite each other.
 * <br />
 * IMPLEMENTATION NOTES:
 * There is an asymmetry in SLP that allows <code>(ServiceURL1, language1)</code>
 * to be registered under two different ServiceTypes (this is allowed by the format of
 * {@link SrvReg}), but not deregistered from one ServiceType only (as {@link SrvDeReg}
 * does not support an additional ServiceType field).
 * <br />
 * {@link Attributes} are not involved in service equality since they can contain
 * locale-specific information (for example: <code>(color=Yellow)</code> with language English,
 * and <code>(color=Giallo)</code> with language Italian.
 *
 * @version $Rev$ $Date$
 * @see ServiceURL
 * @see Attributes
 */
public class ServiceInfo
{
    private final Key key;
    private final ServiceType serviceType;
    private final Scopes scopes;
    private final Attributes attributes;
    private long registrationTime;

    /**
     * Creates a <code>ServiceInfo</code> from a SrvReg message.
     */
    public static ServiceInfo from(SrvReg message)
    {
        return new ServiceInfo(message.getServiceType(), message.getURLEntry().toServiceURL(), message.getLanguage(), message.getScopes(), message.getAttributes());
    }

    /**
     * Creates a <code>ServiceInfo</code> from a SrvDeReg message.
     */
    public static ServiceInfo from(SrvDeReg message)
    {
        return new ServiceInfo(message.getURLEntry().toServiceURL(), message.getLanguage(), message.getScopes(), message.getTags());
    }

    /**
     * Creates a <code>ServiceInfo</code> from a the given arguments.
     *
     * @param serviceURL The service URL of the service
     * @param scopes     The scopes of the service
     * @param attributes The attributes of the service
     * @param language   The language of the service
     */
    public ServiceInfo(ServiceURL serviceURL, String language, Scopes scopes, Attributes attributes)
    {
        this(null, serviceURL, language, scopes, attributes);
    }

    /**
     * Creates a <code>ServiceInfo</code> from the given arguments; the {@link #ServiceInfo(ServiceURL, Scopes, Attributes, String)}
     * constructor should be preferred to this one, as it does not introduce ambiguity between the
     * <code>ServiceType</code> argument and the <code>ServiceType</code> of the <code>ServiceURL</code> argument.
     */
    public ServiceInfo(ServiceType serviceType, ServiceURL serviceURL, String language, Scopes scopes, Attributes attributes)
    {
        this.key = new Key(serviceURL, language);
        this.serviceType = serviceType;
        this.scopes = scopes;
        this.attributes = attributes;
    }

    /**
     * Returns the key of this <code>ServiceInfo</code>.
     */
    public Key getKey()
    {
        return key;
    }

    /**
     * Returns the <code>ServiceType</code> as provided to the constructors; prefer {@link #resolveServiceType()}
     * to get the <code>ServiceType</code> of this <code>ServiceInfo</code>.
     *
     * @see #resolveServiceType()
     */
    public ServiceType getServiceType()
    {
        return serviceType;
    }

    /**
     * Returns the <code>ServiceURL</code> of this <code>ServiceInfo</code>.
     */
    public ServiceURL getServiceURL()
    {
        return getKey().getServiceURL();
    }

    /**
     * Returns the <code>Scopes</code> of this <code>ServiceInfo</code>.
     */
    public Scopes getScopes()
    {
        return scopes;
    }

    /**
     * Returns the <code>Attributes</code> of this <code>ServiceInfo</code>.
     */
    public Attributes getAttributes()
    {
        return attributes;
    }

    /**
     * Returns the language of this <code>ServiceInfo</code>.
     */
    public String getLanguage()
    {
        return getKey().getLanguage();
    }

    /**
     * Returns the <code>ServiceType</code> as provided to the
     * {@link #ServiceInfo(ServiceType, ServiceURL, Scopes, Attributes, String) constructor};
     * if this value is null, returns the <code>ServiceType</code> of the <code>ServiceURL</code>
     * of this <code>ServiceInfo</code>.
     */
    public ServiceType resolveServiceType()
    {
        ServiceType result = getServiceType();
        if (result != null) return result;
        return getServiceURL().getServiceType();
    }

    /**
     * Merges the attributes of this <code>ServiceInfo</code> with the attributes of the given <code>ServiceInfo</code>,
     * provided the two <code>ServiceInfo</code>s have the same {@link #getKey() key}.
     *
     * @param that The <code>ServiceInfo</code> to merge with
     * @return A newly created <code>ServiceInfo</code>, result of the merge, or null if this <code>ServiceInfo</code>
     *         and the given <code>ServiceInfo</code> do not have the same {@link #getKey() key}.
     * @see Attributes#merge(Attributes)
     */
    public ServiceInfo merge(Attributes thatAttrs)
    {
        Attributes thisAttrs = getAttributes();
        Attributes mergedAttrs = null;
        if (thisAttrs == null)
            mergedAttrs = thatAttrs == null ? null : thatAttrs.merge(null);
        else
            mergedAttrs = thisAttrs.merge(thatAttrs);
        return clone(getServiceType(), getServiceURL(), getLanguage(), getScopes(), mergedAttrs);
    }

    /**
     * Unmerges the attributes of this <code>ServiceInfo</code> with the attributes of the given <code>ServiceInfo</code>,
     * provided the two <code>ServiceInfo</code>s have the same {@link #getKey() key}.
     *
     * @param that The <code>ServiceInfo</code> to unmerge with
     * @return A newly created <code>ServiceInfo</code>, result of the unmerge, or null if this <code>ServiceInfo</code>
     *         and the given <code>ServiceInfo</code> do not have the same {@link #getKey() key}.
     * @see Attributes#unmerge(Attributes)
     */
    public ServiceInfo unmerge(Attributes thatAttr)
    {
        if (thatAttr == null || thatAttr.isEmpty()) return null;
        Attributes thisAttr = getAttributes();
        if (thisAttr == null) return new ServiceInfo(getServiceType(), getServiceURL(), getLanguage(), getScopes(), getAttributes());
        Attributes mergedAttrs = thisAttr.unmerge(thatAttr);
        return clone(getServiceType(), getServiceURL(), getLanguage(), getScopes(), mergedAttrs);
    }

    /**
     * Returns a new clone of this <code>ServiceInfo</code> with the given arguments.
     * Subclasses may override to clone additional state.
     */
    protected ServiceInfo clone(ServiceType serviceType, ServiceURL serviceURL, String language, Scopes scopes, Attributes attributes)
    {
        ServiceInfo clone = new ServiceInfo(serviceType, serviceURL, language, scopes, attributes);
        clone.setRegistrationTime(getRegistrationTime());
        return clone;
    }

    /**
     * Returns the registration time of this <code>ServiceInfo</code>, in milliseconds since the Unix epoch.
     */
    public long getRegistrationTime()
    {
        return registrationTime;
    }

    /**
     * Set the registration time of this <code>ServiceInfo</code>, in milliseconds since the Unix epoch.
     */
    public void setRegistrationTime(long registrationTime)
    {
        this.registrationTime = registrationTime;
    }

    public boolean expires()
    {
        int lifetime = getServiceURL().getLifetime();
        return lifetime != ServiceURL.LIFETIME_PERMANENT;
    }

    /**
     * Returns true if the <code>ServiceURL</code>'s lifetime is expired, since its registration, as of the specified time.
     *
     * @param time The time, in milliseconds, to check if the lifetime is expired.
     * @see #getRegistrationTime()
     * @see ServiceURL#getLifetime()
     */
    public boolean isExpiredAsOf(long time)
    {
        if (!expires()) return false;
        long lifetimeMillis = TimeUnit.SECONDS.toMillis(getServiceURL().getLifetime());
        return getRegistrationTime() + lifetimeMillis <= time;
    }

    /**
     * Services are identified by their ServiceURL and their language.
     * This class encapsulates the service identity.
     */
    public static class Key
    {
        private final ServiceURL serviceURL;
        private final String language;

        /**
         * Creates a new <code>Key</code> object.
         *
         * @param serviceURL The ServiceURL of the service
         * @param language   The language of the service
         */
        private Key(ServiceURL serviceURL, String language)
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

        /**
         * Returns the ServiceURL of this key.
         */
        private ServiceURL getServiceURL()
        {
            return serviceURL;
        }

        /**
         * Returns the language of this key.
         */
        private String getLanguage()
        {
            return language;
        }
    }
}
