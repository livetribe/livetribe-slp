/*
 * Copyright 2006-2008 the original author or authors
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

import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.msg.SrvReg;


/**
 * Representation of a service, exposed by ServiceAgents and cached by DirectoryAgents.
 * <br />
 * In SLP, services are distinguished by their {@link ServiceURL} and by their language; these two elements
 * form the {@link Key key} of the service. Two service registration with the same key overwrite each other.
 * The {@link Key} object returned by {@link #getKey()} must be used as key in hash structures.
 * <br />
 * {@link Attributes} are not involved in service equality since they can contain
 * locale-specific information (for example: <code>(color=Yellow)</code> with language English,
 * and <code>(color=Giallo)</code> with language Italian.
 * <br />
 * IMPLEMENTATION NOTES:
 * There is an asymmetry in SLP that allows <code>(ServiceURL1, language1)</code>
 * to be registered under two different ServiceTypes (this is allowed by the format of
 * {@link SrvReg}), but not deregistered from one ServiceType only (as {@link SrvDeReg}
 * does not support an additional ServiceType field).
 *
 * @see ServiceURL
 * @see Scopes
 * @see Attributes
 */
public class ServiceInfo
{
    /**
     * Indicates that the service has been deregistered
     *
     * @see #setRegistered(boolean)
     */
    private static final long DEREGISTERED = -1L;
    /**
     * Indicates that it is unknown the registration status of the service
     *
     * @see #setRegistered(boolean)
     */
    private static final long UNREGISTERED = 0L;

    private final Key key;
    private final ServiceType serviceType;
    private final Scopes scopes;
    private final Attributes attributes;
    private volatile long registrationTime;

    /**
     * Creates a <code>ServiceInfo</code> from a SrvReg message.
     *
     * @param srvReg the SrvReg message to convert into a ServiceInfo
     * @return a new ServiceInfo from the given message
     */
    public static ServiceInfo from(SrvReg srvReg)
    {
        return new ServiceInfo(srvReg.getServiceType(), srvReg.getURLEntry().toServiceURL(), srvReg.getLanguage(), srvReg.getScopes(), srvReg.getAttributes());
    }

    /**
     * Creates a <code>ServiceInfo</code> from a SrvDeReg message.
     *
     * @param srvDeReg the SrvDeReg message to convert into a ServiceInfo
     * @return a new ServiceInfo from the given message
     */
    public static ServiceInfo from(SrvDeReg srvDeReg)
    {
        return new ServiceInfo(srvDeReg.getURLEntry().toServiceURL(), srvDeReg.getLanguage(), srvDeReg.getScopes(), srvDeReg.getTags());
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
     * Creates a <code>ServiceInfo</code> from the given arguments; the {@link #ServiceInfo(ServiceURL, String, Scopes, Attributes)}
     * constructor should be preferred to this one, as it does not introduce ambiguity between the
     * <code>ServiceType</code> argument and the <code>ServiceType</code> of the <code>ServiceURL</code> argument.
     *
     * @param serviceType The service type of the service
     * @param serviceURL  The service URL of the service
     * @param scopes      The scopes of the service
     * @param attributes  The attributes of the service
     * @param language    The language of the service
     */
    public ServiceInfo(ServiceType serviceType, ServiceURL serviceURL, String language, Scopes scopes, Attributes attributes)
    {
        this.key = new Key(serviceURL, language);
        this.serviceType = serviceType;
        this.scopes = scopes;
        this.attributes = attributes;
        registrationTime = UNREGISTERED;
    }

    /**
     * @return the key of this <code>ServiceInfo</code>.
     */
    public Key getKey()
    {
        return key;
    }

    /**
     * Returns the <code>ServiceType</code> as provided to the
     * {@link #ServiceInfo(ServiceType, ServiceURL, String, Scopes, Attributes) constructor};
     * prefer {@link #resolveServiceType()} to get the <code>ServiceType</code> of this <code>ServiceInfo</code>.
     *
     * @return the service type provided to the {@link #ServiceInfo(ServiceType, ServiceURL, String, Scopes, Attributes) constructor}
     * @see #resolveServiceType()
     */
    public ServiceType getServiceType()
    {
        return serviceType;
    }

    /**
     * @return the <code>ServiceURL</code> of this <code>ServiceInfo</code>.
     */
    public ServiceURL getServiceURL()
    {
        return getKey().getServiceURL();
    }

    /**
     * @return the <code>Scopes</code> of this <code>ServiceInfo</code>.
     */
    public Scopes getScopes()
    {
        return scopes;
    }

    /**
     * @return the <code>Attributes</code> of this <code>ServiceInfo</code>.
     */
    public Attributes getAttributes()
    {
        return attributes;
    }

    /**
     * @return the language of this <code>ServiceInfo</code>.
     */
    public String getLanguage()
    {
        return getKey().getLanguage();
    }

    /**
     * Resolves the service type of this service, returning the service type provided in the
     * {@link #ServiceInfo(ServiceType, ServiceURL, String, Scopes, Attributes) constructor} if not null,
     * otherwise the <code>ServiceType</code> of the <code>ServiceURL</code>.
     *
     * @return the service type of this service after resolution
     * @see #getServiceType()
     * @see #getServiceURL()
     */
    public ServiceType resolveServiceType()
    {
        ServiceType result = getServiceType();
        if (result != null) return result;
        return getServiceURL().getServiceType();
    }

    /**
     * Adds the given Attributes to the Attributes of this <code>ServiceInfo</code>, returning a new instance of
     * ServiceInfo containing the sum of the Attributes; the current instance of ServiceInfo is left unaltered.
     *
     * @param thatAttrs The <code>Attributes</code> to add to this instance's Attributes
     * @return A newly created <code>ServiceInfo</code> containing of the sum of the Attributes
     * @see Attributes#union(Attributes)
     */
    public ServiceInfo addAttributes(Attributes thatAttrs)
    {
        Attributes thisAttrs = getAttributes();
        Attributes mergedAttrs = null;
        if (thisAttrs == null)
            mergedAttrs = thatAttrs == null ? null : thatAttrs.union(null);
        else
            mergedAttrs = thisAttrs.union(thatAttrs);
        return clone(getScopes(), mergedAttrs);
    }

    /**
     * Removes the given Attributes from the Attributes of this <code>ServiceInfo</code>, returning a new instance of
     * ServiceInfo containing the difference of the Attributes; the current instance of ServiceInfo is left unaltered.
     * The given Attributes may only contain the tags to remove (values are not considered).
     *
     * @param thatAttrs The <code>Attributes</code> tags to remove from this instance's Attributes
     * @return A newly created <code>ServiceInfo</code> containing the difference of the Attributes
     * @see Attributes#complement(Attributes)
     */
    public ServiceInfo removeAttributes(Attributes thatAttrs)
    {
        Attributes thisAttr = getAttributes();
        Attributes mergedAttrs = null;
        if (thisAttr != null)
            mergedAttrs = thisAttr.complement(thatAttrs);
        return clone(getScopes(), mergedAttrs);
    }

    /**
     * Clones this <code>ServiceInfo</code> using the given arguments.
     * Subclasses may override to clone additional state.
     *
     * @param newScopes     the Scopes that the clone must have
     * @param newAttributes the Attributes that the clone must have
     * @return a new ServiceInfo instance
     */
    protected ServiceInfo clone(Scopes newScopes, Attributes newAttributes)
    {
        ServiceInfo clone = new ServiceInfo(getServiceType(), getServiceURL(), getLanguage(), newScopes, newAttributes);
        clone.registrationTime = registrationTime;
        return clone;
    }

    /**
     * Sets whether this <code>ServiceInfo</code> is registered.
     *
     * @param registered true if this <code>ServiceInfo</code> is registered, false otherwise.
     * @see #isRegistered()
     */
    public void setRegistered(boolean registered)
    {
        registrationTime = registered ? System.currentTimeMillis() : DEREGISTERED;
    }

    /**
     * @return whether this <code>ServiceInfo</code> is registered or not
     * @see #setRegistered(boolean)
     * @see #isExpiredAsOf(long)
     */
    public boolean isRegistered()
    {
        return registrationTime > UNREGISTERED;
    }

    /**
     * @return whether this service expires or not
     * @see ServiceURL#LIFETIME_PERMANENT
     */
    public boolean expires()
    {
        int lifetime = getServiceURL().getLifetime();
        return lifetime != ServiceURL.LIFETIME_PERMANENT;
    }

    /**
     * @param time The time, in milliseconds since the Unix epoch, to check for expiration
     * @return true if the <code>ServiceURL</code>'s lifetime is expired, since its registration, as of the specified time.
     * @see #isRegistered()
     * @see ServiceURL#getLifetime()
     */
    public boolean isExpiredAsOf(long time)
    {
        if (!expires()) return false;
        long lifetimeMillis = TimeUnit.SECONDS.toMillis(getServiceURL().getLifetime());
        return registrationTime + lifetimeMillis <= time;
    }

    /**
     * Encapsulates the service identity.
     * Services are identified by their ServiceURL and their language.
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
         * @return the ServiceURL of this key.
         */
        private ServiceURL getServiceURL()
        {
            return serviceURL;
        }

        /**
         * @return the language of this key.
         */
        private String getLanguage()
        {
            return language;
        }
    }
}
