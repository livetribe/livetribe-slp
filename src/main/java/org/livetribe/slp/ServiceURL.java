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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * @version $Rev$ $Date$
 */
public class ServiceURL implements Serializable
{
    public static final int NO_PORT = 0;
    public static final int LIFETIME_NONE = 0;
    public static final int LIFETIME_DEFAULT = 10800;
    public static final int LIFETIME_MAXIMUM = 65535;
    public static final int LIFETIME_PERMANENT = -1;

    private static final String SERVICE = "service:";

    private final String url;
    private final int lifetime;
    private ServiceType serviceType;
    private String host;
    private int port;
    private String path;

    public ServiceURL(String url)
    {
        this(url, LIFETIME_DEFAULT);
    }

    public ServiceURL(String url, int lifetime)
    {
        this.url = url;
        this.lifetime = lifetime;
        parse(url);
    }

    public ServiceType getServiceType()
    {
        return serviceType;
    }

    public String getTransport()
    {
        return "";
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getURLPath()
    {
        return path;
    }

    public int getLifetime()
    {
        return lifetime;
    }

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

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException
    {
        ois.defaultReadObject();
        parse(getURL());
    }

    public String toString()
    {
        return getURL();
    }

    private void parse(String serviceURL)
    {
        if (serviceURL.startsWith(SERVICE)) serviceURL = serviceURL.substring(SERVICE.length());

        String authorityPrefix = "://";
        int authorityPrefixIndex = serviceURL.indexOf(authorityPrefix);
        serviceType = new ServiceType(serviceURL.substring(0, authorityPrefixIndex));

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
