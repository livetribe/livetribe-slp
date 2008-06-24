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

import java.io.Serializable;

/**
 * A ServiceURL represents the location of a service.
 * A client given a ServiceURL should be able to contact the remote service that the ServiceURL
 * represents with the information carried by the ServiceURL itself.
 * A ServiceURL is a URI that defines at least scheme and address portion, and that may have the
 * <code>service:</code> scheme.
 * <br />
 * ServiceURLs have a lifetime, used to denote the period of time over which the service is
 * available.
 * @see ServiceType
 * @version $Rev$ $Date$
 */
public class ServiceURL implements Serializable
{
    private static final long serialVersionUID = 2989950945471457682L;

    /**
     * The constant used to denote that the service did not specify a port.
     */
    public static final int NO_PORT = 0;

    /**
     * The constant used to denote that this service URL has no lifetime.
     * It is wrong to advertise services with no lifetime, but this constant can be used to create
     * ServiceURLs used to perform activities different from service registration.
     */
    public static final int LIFETIME_NONE = 0;

    /**
     * The constant used to denote the default lifetime of service URLs (3 hours).
     */
    public static final int LIFETIME_DEFAULT = 10800;

    /**
     * The constant used to denote the maximum value for the lifetime of service URLs.
     */
    public static final int LIFETIME_MAXIMUM = 65535;

    /**
     * The constant used to denote that this service URL has an infinite lifetime.
     */
    public static final int LIFETIME_PERMANENT = -1;

    private static final String SERVICE = "service:";

    private final String url;
    private final int lifetime;
    private ServiceType serviceType;
    private String host;
    private int port;
    private String path;

    /**
     * Creates a <code>ServiceURL</code> parsing the given string, with a {@link #LIFETIME_DEFAULT default lifetime}.
     * @param url The string to be parsed
     */
    public ServiceURL(String url)
    {
        this(url, LIFETIME_DEFAULT);
    }

    /**
     * Creates a <code>ServiceURL</code> parsing the given string, with the specified lifetime, in seconds.
     * @param url The string to be parsed
     * @param lifetime The lifetime, in seconds, for this service URL
     */
    public ServiceURL(String url, int lifetime)
    {
        this.url = url;
        if (lifetime != LIFETIME_PERMANENT && (lifetime < LIFETIME_NONE || lifetime > LIFETIME_MAXIMUM))
            throw new IllegalArgumentException("Lifetime must be between " + LIFETIME_NONE + " and " + LIFETIME_MAXIMUM);
        this.lifetime = lifetime;
        parse(url);
    }

    /**
     * Returns the {@link ServiceType} of this service URL.
     */
    public ServiceType getServiceType()
    {
        return serviceType;
    }

    /**
     * Returns the network layer transport identifier, which is the empty string for the IP transport.
     */
    public String getTransport()
    {
        return "";
    }

    /**
     * Returns the host portion of this service URL.
     */
    public String getHost()
    {
        return host;
    }

    /**
     * Returns the port number of this service URL.
     * @see #NO_PORT
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Returns the path of this service URL.
     */
    public String getURLPath()
    {
        return path;
    }

    /**
     * Returns the lifetime, in seconds, of this service URL.
     */
    public int getLifetime()
    {
        return lifetime;
    }

    /**
     * Returns the string form of this service URL, that can be passed to {@link #ServiceURL(String)} to be parsed.
     */
    public String getURL()
    {
        return url;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final ServiceURL that = (ServiceURL)obj;
        return getURL().equals(that.getURL());
    }

    public int hashCode()
    {
        return getURL().hashCode();
    }

    /**
     * @see #getURL()
     */
    public String toString()
    {
        return getURL();
    }

    private void parse(String serviceURL)
    {
        boolean isServiceURL = false;
        if (serviceURL.startsWith(SERVICE))
        {
            isServiceURL = true;
            serviceURL = serviceURL.substring(SERVICE.length());
        }

        String authorityPrefix = "://";
        int authorityPrefixIndex = serviceURL.indexOf(authorityPrefix);
        serviceType = new ServiceType((isServiceURL ? SERVICE : "") + serviceURL.substring(0, authorityPrefixIndex));

        serviceURL = serviceURL.substring(authorityPrefixIndex + authorityPrefix.length());

        int slash = serviceURL.indexOf('/');
        int colon = serviceURL.indexOf(':');
        if (colon >= 0 && (slash >= 0 && colon < slash || slash < 0))
        {
            if (slash < 0) slash = serviceURL.length();
            host = serviceURL.substring(0, colon);
            port = Integer.parseInt(serviceURL.substring(colon + 1, slash));
            serviceURL = serviceURL.substring(slash);
        }
        else
        {
            if (slash < 0) slash = serviceURL.length();
            host = serviceURL.substring(0, slash);
            port = NO_PORT;
            serviceURL = serviceURL.substring(slash);
        }

        path = serviceURL;
    }
}
