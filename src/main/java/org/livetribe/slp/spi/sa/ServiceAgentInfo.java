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

import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @version $Rev$ $Date$
 */
public class ServiceAgentInfo
{
    private final ServiceType serviceType;
    private final ServiceURL serviceURL;
    private final List scopes;
    private final List attributes;
    private final String language;
    private final boolean fresh;

    public ServiceAgentInfo(ServiceType serviceType, ServiceURL serviceURL, String[] scopes, String[] attributes, String language, boolean fresh)
    {
        this.serviceType = serviceType;
        this.serviceURL = serviceURL;
        this.scopes = scopes == null ? Collections.emptyList() : Arrays.asList(scopes);
        this.attributes = attributes == null ? Collections.emptyList() : Arrays.asList(attributes);
        this.language = language;
        this.fresh = fresh;
    }

    public ServiceType getServiceType()
    {
        return serviceType;
    }

    public ServiceURL getServiceURL()
    {
        return serviceURL;
    }

    public String[] getScopes()
    {
        return (String[])scopes.toArray(new String[scopes.size()]);
    }

    public String[] getAttributes()
    {
        return (String[])attributes.toArray(new String[attributes.size()]);
    }

    public String getLanguage()
    {
        return language;
    }

    public boolean isFresh()
    {
        return fresh;
    }
}
