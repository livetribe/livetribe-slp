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
package org.livetribe.slp.spi.sa;

import java.util.List;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import org.livetribe.slp.Attributes;
import org.livetribe.slp.spi.msg.IdentifierExtension;
import org.livetribe.slp.spi.msg.SAAdvert;

/**
 * @version $Rev$ $Date$
 */
public class ServiceAgentInfo
{
    private final String identifier;
    private final String url;
    private final List scopes;
    private final Attributes attributes;
    private final String language;
    private final String host;

    public static ServiceAgentInfo from(SAAdvert saAdvert)
    {
        IdentifierExtension identifierExtension = IdentifierExtension.findFirst(saAdvert.getExtensions());
        String identifier = identifierExtension == null ? null : identifierExtension.getIdentifier();
        return new ServiceAgentInfo(identifier, saAdvert.getURL(), saAdvert.getScopes(), saAdvert.getAttributes(), saAdvert.getLanguage());
    }

    public ServiceAgentInfo(String identifier, String url, String[] scopes, Attributes attributes, String language)
    {
        this.identifier = identifier;
        this.url = url;
        this.scopes = scopes == null ? Collections.emptyList() : Arrays.asList(scopes);
        this.attributes = attributes;
        this.language = language;
        this.host = parseHost(url);
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

    public String getIdentifier()
    {
        return identifier;
    }

    public String[] getScopes()
    {
        return (String[])scopes.toArray(new String[scopes.size()]);
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

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final ServiceAgentInfo that = (ServiceAgentInfo)obj;
        return url.equals(that.url);
    }

    public int hashCode()
    {
        return url.hashCode();
    }
}
