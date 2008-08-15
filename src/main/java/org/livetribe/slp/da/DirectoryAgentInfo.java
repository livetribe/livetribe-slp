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
package org.livetribe.slp.da;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPError;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.msg.DAAdvert;

/**
 * Representation of the information regarding a DirectoryAgent.
 * <br />
 * DirectoryAgents advertise their presence on the network periodically, as well as their bootup and shutdown.
 * ServiceAgents and UserAgents listen for DirectoryAgent advertisements and use the information contained in
 * DirectoryAgentInfo to contact the DirectoryAgent.
 * <br />
 * DirectoryAgentInfos have a {@link #getKey() key} that distinguishes them and that can be used as key
 * in hash structures.
 *
 * @version $Rev: 163 $ $Date: 2006-06-12 17:14:02 +0200 (Mon, 12 Jun 2006) $
 */
public class DirectoryAgentInfo
{
    /**
     * The service type of DirectoryAgents, <code>service:directory-agent</code>.
     */
    public static final ServiceType SERVICE_TYPE = new ServiceType("service:directory-agent");
    /**
     * The Attributes tag for the TCP port a DirectoryAgent listens on.
     */
    public static final String TCP_PORT_TAG = "tcpPort";

    /**
     * Creates a new DirectoryAgentInfo from the given DAAdvert message.
     *
     * @param daAdvert the DAAdvert message from which create the DirectoryAgentInfo
     * @return a new DirectoryAgentInfo from the given DAAdvert message
     */
    public static DirectoryAgentInfo from(DAAdvert daAdvert)
    {
        return new DirectoryAgentInfo(daAdvert.getURL(), daAdvert.getScopes(), daAdvert.getAttributes(), daAdvert.getLanguage(), daAdvert.getBootTime());
    }

    /**
     * Creates a new DirectoryAgentInfo from the given IP address.
     *
     * @param address the IP address of the DirectoryAgent
     * @return a new DirectoryAgentInfo with the given address, any scope, no attributes, no language and no boot time.
     * @see #from(String, Scopes, Attributes, String, int)
     */
    public static DirectoryAgentInfo from(String address)
    {
        return from(address, Scopes.ANY, Attributes.NONE, null, -1);
    }

    /**
     * Creates a new DirectoryAgentInfo from the given arguments.
     *
     * @param address    the IP address of the DirectoryAgent
     * @param scopes     the Scopes of the DirectoryAgent
     * @param attributes the Attributes of the DirectoryAgent
     * @param language   the language of the DirectoryAgent
     * @param bootTime   the boot time, in seconds, of the DirectoryAgent
     * @return a new DirectoryAgentInfo
     * @see #from(String)
     */
    public static DirectoryAgentInfo from(String address, Scopes scopes, Attributes attributes, String language, int bootTime)
    {
        return new DirectoryAgentInfo(SERVICE_TYPE.asString() + "://" + address, scopes, attributes, language, bootTime);
    }

    private final String url;
    private final Scopes scopes;
    private final Attributes attributes;
    private final String language;
    private final int bootTime;
    private final Key key;

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
            throw new ServiceLocationException("DirectoryAgent URL is malformed: " + url, SLPError.PARSE_ERROR);
        String host = url.substring(index + authoritySeparator.length());
        if (host.trim().length() == 0)
            throw new ServiceLocationException("DirectoryAgent URL is malformed: " + url, SLPError.PARSE_ERROR);
        return host;
    }

    /**
     * @return the URL of this DirectoryAgent in the form <code>service:directory-agent://&lt;ip address&gt;</code>
     */
    public String getURL()
    {
        return url;
    }

    /**
     * @return the Scopes of this DirectoryAgent
     */
    public Scopes getScopes()
    {
        return scopes;
    }

    /**
     * @return the Attributes of this DirectoryAgent
     */
    public Attributes getAttributes()
    {
        return attributes;
    }

    /**
     * @return the language of this DirectoryAgent
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * @return the boot time, in seconds since the Unix Epoch, of this DirectoryAgent.
     */
    public int getBootTime()
    {
        return bootTime;
    }

    /**
     * @return the key of this DirectoryAgentInfo
     */
    public Key getKey()
    {
        return key;
    }

    /**
     * @return whether this DirectoryAgent is shutting down.
     */
    public boolean isShuttingDown()
    {
        return getBootTime() == 0;
    }

    /**
     * Matches this DirectoryAgent against the given Scopes.
     *
     * @param scopes The scopes to match
     * @return true if at least one of the given scopes is also a scope of this DirectoryAgentInfo, false otherwise
     * @see Scopes#weakMatch(Scopes)
     */
    public boolean matchScopes(Scopes scopes)
    {
        return getScopes().weakMatch(scopes);
    }

    /**
     * Matches this DirectoryAgent's Attributes against the given LDAPv3 filter.
     *
     * @param filter The LDAPv3 filter to match
     * @return true if this DirectoryAgentInfo's Attributes match the given filter, false otherwise
     */
    public boolean matchFilter(Filter filter)
    {
        return filter == null || filter.matches(getAttributes());
    }

    /**
     * @return the host address of this DirectoryAgent
     */
    public String getHostAddress()
    {
        return key.getHostAddress();
    }

    /**
     * Returns the TCP port this DirectoryAgent is listening on, if this information is available in the Attributes
     * via the {@link #TCP_PORT_TAG dedicated tag}.
     * <br />
     * If the TCP port information is not available in the Attributes, returns the given port.
     *
     * @param defaultPort the port to return if no information on the TCP port is available in the Attributes
     * @return the TCP port this DirectoryAgent is listening on, or the given port
     */
    public int getTCPPort(int defaultPort)
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
     * Encapsulates the DirectoryAgent identity. Object of this class can be used as keys in hash structures.
     * It has no public accessors, since the data is available in its corresponding {@link DirectoryAgentInfo}
     *
     * @see DirectoryAgentInfo#getKey()
     */
    public static class Key
    {
        private final String hostAddress;

        private Key(String hostAddress)
        {
            this.hostAddress = hostAddress;
        }

        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Key that = (Key)obj;
            return hostAddress.equals(that.hostAddress);
        }

        public int hashCode()
        {
            return hostAddress.hashCode();
        }

        private String getHostAddress()
        {
            return hostAddress;
        }
    }
}
