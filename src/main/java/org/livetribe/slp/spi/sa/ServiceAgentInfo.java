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
package org.livetribe.slp.spi.sa;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.msg.IdentifierExtension;
import org.livetribe.slp.spi.msg.SAAdvert;

/**
 * @version $Rev: 163 $ $Date: 2006-06-12 17:14:02 +0200 (Mon, 12 Jun 2006) $
 */
public class ServiceAgentInfo
{
    public static final ServiceType SERVICE_TYPE = new ServiceType("service:service-agent");
    public static final String TCP_PORT_TAG = "tcpPort";

    private final String identifier;
    private final String url;
    private final Scopes scopes;
    private final Attributes attributes;
    private final String language;
    private final String host;
//    private final int port;

    public static ServiceAgentInfo from(SAAdvert saAdvert)
    {
        IdentifierExtension identifierExtension = IdentifierExtension.findFirst(saAdvert.getExtensions());
        String identifier = identifierExtension == null ? null : identifierExtension.getIdentifier();
        return new ServiceAgentInfo(identifier, saAdvert.getURL(), saAdvert.getScopes(), saAdvert.getAttributes(), saAdvert.getLanguage());
    }

    public static ServiceAgentInfo from(String identifier, String address, Scopes scopes, Attributes attributes, String language)
    {
        return new ServiceAgentInfo(identifier, SERVICE_TYPE.asString() + "://" + address, scopes, attributes, language);
    }

    private ServiceAgentInfo(String identifier, String url, Scopes scopes, Attributes attributes, String language)
    {
        this.identifier = identifier;
        this.url = url;
        this.scopes = scopes;
        this.attributes = attributes;
        this.language = language;
        this.host = parseHost(url);
//        this.port = attributes.isTagPresent(TCP_PORT_TAG) ? ((Long)attributes.getValue(TCP_PORT_TAG)).intValue() : Defaults.get(Connector.PORT_KEY);
    }

    private String parseHost(String url)
    {
        String authoritySeparator = "://";
        int index = url.indexOf(authoritySeparator);
        if (index < 0) throw new IllegalArgumentException("ServiceAgent URL is malformed: " + url);
        String host = url.substring(index + authoritySeparator.length());
        if (host.trim().length() == 0) throw new IllegalArgumentException("ServiceAgent URL is malformed: " + url);
        return host;
    }

    public boolean hasIdentifier()
    {
        return getIdentifier() != null;
    }

    public String getIdentifier()
    {
        return identifier;
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

    public String getHost()
    {
        return host;
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
}
