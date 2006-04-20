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
package org.livetribe.slp.api.sa;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;

/**
 * @version $Rev$ $Date$
 */
public class ServiceInfo
{
    private final ServiceType serviceType;
    private final ServiceURL serviceURL;
    private final String[] scopes;
    private final Attributes attributes;
    private final String language;

    public ServiceInfo(ServiceURL serviceURL, String[] scopes, Attributes attributes, String language)
    {
        this(null, serviceURL, scopes, attributes, language);
    }

    public ServiceInfo(ServiceType serviceType, ServiceURL serviceURL, String[] scopes, Attributes attributes, String language)
    {
        this.serviceType = serviceType;
        this.serviceURL = serviceURL;
        this.scopes = scopes;
        this.attributes = attributes;
        this.language = language;
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
}
