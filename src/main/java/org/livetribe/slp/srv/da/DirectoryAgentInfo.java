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
package org.livetribe.slp.srv.da;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.srv.filter.Filter;
import org.livetribe.slp.srv.msg.DAAdvert;

/**
 * A POJO that holds information about an SLP DirectoryAgent.
 * <br />
 *
 * @version $Rev: 163 $ $Date: 2006-06-12 17:14:02 +0200 (Mon, 12 Jun 2006) $
 */
public class DirectoryAgentInfo
{
    public static final ServiceType SERVICE_TYPE = new ServiceType("service:directory-agent");
    public static final String TCP_PORT_TAG = "tcpPort";

    private final String url;
    private final Scopes scopes;
    private final Attributes attributes;
    private final String language;
    private final int bootTime;
    private final Key key;

    public static DirectoryAgentInfo from(DAAdvert daAdvert)
    {
        return new DirectoryAgentInfo(daAdvert.getURL(), daAdvert.getScopes(), daAdvert.getAttributes(), daAdvert.getLanguage(), daAdvert.getBootTime());
    }

    public static DirectoryAgentInfo from(String address)
    {
        return from(address, Scopes.ANY, Attributes.NONE, null, -1);
    }

    public static DirectoryAgentInfo from(String address, Scopes scopes, Attributes attributes, String language, int bootTime)
    {
        return new DirectoryAgentInfo(SERVICE_TYPE.asString() + "://" + address, scopes, attributes, language, bootTime);
    }

    private DirectoryAgentInfo(String url, Scopes scopes, Attributes attributes, String language, int bootTime)
    {
        this.url = url;
        this.scopes = scopes;
        this.attributes = attributes;
        this.language = language;
        this.bootTime = bootTime;
        String host = parseHost(url);
        this.key = new Key(host);
    }

    private String parseHost(String url)
    {
        String authoritySeparator = "://";
        int index = url.indexOf(authoritySeparator);
        if (index < 0)
            throw new ServiceLocationException("DirectoryAgent URL is malformed: " + url, ServiceLocationException.PARSE_ERROR);
        String host = url.substring(index + authoritySeparator.length());
        if (host.trim().length() == 0)
            throw new ServiceLocationException("DirectoryAgent URL is malformed: " + url, ServiceLocationException.PARSE_ERROR);
        return host;
    }

    public String getURL()
    {
        return url;
    }

    public Scopes getScopes()
    {
        return scopes;
    }

    public Attributes getAttributes()
    {
        return attributes;
    }

    public String getLanguage()
    {
        return language;
    }

    public int getBootTime()
    {
        return bootTime;
    }

    public Key getKey()
    {
        return key;
    }

    public boolean isShuttingDown()
    {
        return getBootTime() == 0;
    }

    /**
     * @param scopes The scopes to match
     * @return true if at least one of the given scopes is also a scope of this DirectoryAgentInfo, false otherwise
     */
    public boolean matchScopes(Scopes scopes)
    {
        return getScopes().weakMatch(scopes);
    }

    /**
     * @param filter The filter to match
     * @return true if this DirectoryAgentInfo's attributes match the given filter, false otherwise
     */
    public boolean matchFilter(Filter filter)
    {
        return filter == null || filter.matches(getAttributes());
    }

    public String getHost()
    {
        return key.getHost();
    }

    public int getPort(int defaultPort)
    {
        return attributes.containsTag(TCP_PORT_TAG) ? (Integer)attributes.valueFor(TCP_PORT_TAG).getValue() : defaultPort;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(getURL());
        builder.append(" ").append(attributes);
        return builder.toString();
    }

    /**
     * This class encapsulates the DirectoryAgent identity.
     * It can be used as a key in hash structures.
     * It has no public accessors, since the data is
     * available in its corresponding {@link DirectoryAgentInfo}
     */
    public static class Key
    {
        private final String host;

        private Key(String host)
        {
            this.host = host;
        }

        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Key that = (Key)obj;
            return host.equals(that.host);
        }

        public int hashCode()
        {
            return host.hashCode();
        }

        private String getHost()
        {
            return host;
        }
    }
}
