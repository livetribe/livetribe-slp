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
package org.livetribe.slp.spi.da;

import java.util.List;

import org.livetribe.slp.spi.msg.DAAdvert;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @version $Rev$ $Date$
 */
public class DirectoryAgentInfo
{
    private final List attributes;
    private final long bootTime;
    private final String language;
    private final List scopes;
    private final String url;
    private final String host;

    public static DirectoryAgentInfo from(DAAdvert daAdvert)
    {
        return new DirectoryAgentInfo(daAdvert.getAttributes(), daAdvert.getBootTime(), daAdvert.getLanguage(), daAdvert.getScopes(), daAdvert.getURL());
    }

    private DirectoryAgentInfo(String[] attributes, long bootTime, String language, String[] scopes, String url)
    {
        this.attributes = attributes == null ? Collections.emptyList() : Arrays.asList(attributes);
        this.bootTime = bootTime;
        this.language = language;
        this.scopes = scopes == null ? Collections.emptyList() : Arrays.asList(scopes);
        this.url = url;
        this.host = parseHost(url);
    }

    private String parseHost(String url)
    {
        String authoritySeparator = "://";
        int index = url.indexOf(authoritySeparator);
        if (index < 0) throw new IllegalArgumentException("DirectoryAgent URL is malformed: " + url);
        String host = url.substring(index + authoritySeparator.length());
        if (host.trim().length() == 0) throw new IllegalArgumentException("DirectoryAgent URL is malformed: " + url);
        return host;
    }

    public String[] getAttributes()
    {
        return (String[])attributes.toArray(new String[attributes.size()]);
    }

    public long getBootTime()
    {
        return bootTime;
    }

    public String getLanguage()
    {
        return language;
    }

    public String[] getScopes()
    {
        return (String[])scopes.toArray(new String[scopes.size()]);
    }

    public String getUrl()
    {
        return url;
    }

    /**
     * Returns true if at least one of the given scopes is also a scope of this DirectoryAgentInfo
     */
    public boolean matchScopes(List scopesList)
    {
        if (scopesList == null) return false;
        for (int i = 0; i < scopesList.size(); ++i)
        {
            String scope = (String)scopesList.get(i);
            if (scopes.contains(scope)) return true;
        }
        return false;
    }

    public String getHost()
    {
        return host;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final DirectoryAgentInfo that = (DirectoryAgentInfo)obj;
        return url.equals(that.url);
    }

    public int hashCode()
    {
        return url.hashCode();
    }
}
