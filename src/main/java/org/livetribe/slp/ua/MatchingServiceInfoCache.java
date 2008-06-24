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
package org.livetribe.slp.ua;

import java.util.Collection;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.sa.ServiceListener;
import org.livetribe.slp.srv.ServiceInfoCache;
import org.livetribe.slp.srv.filter.Filter;
import org.livetribe.slp.srv.filter.FilterParser;

/**
 * @version $Rev: 252 $ $Date: 2006-08-21 09:56:15 +0200 (Mon, 21 Aug 2006) $
 */
public class MatchingServiceInfoCache implements ServiceNotificationListener
{
    private final ServiceInfoCache<ServiceInfo> services = new ServiceInfoCache<ServiceInfo>();
    private final ServiceType serviceType;
    private final String language;
    private final Scopes scopes;
    private final Filter filter;

    public MatchingServiceInfoCache(ServiceType serviceType, String language, Scopes scopes, String filter)
    {
        this.serviceType = serviceType;
        this.language = language;
        this.scopes = scopes;
        this.filter = new FilterParser().parse(filter);
    }

    public void serviceRegistered(ServiceNotificationEvent event)
    {
        ServiceInfo service = event.getService();
        if (event.isUpdate())
            addAttributes(service.getKey(), service.getAttributes());
        else
            put(service);
    }

    public void serviceDeregistered(ServiceNotificationEvent event)
    {
        ServiceInfo service = event.getService();
        if (event.isUpdate())
            removeAttributes(service.getKey(), service.getAttributes());
        else
            remove(service.getKey());
    }

    public void addServiceListener(ServiceListener listener)
    {
        services.addServiceListener(listener);
    }

    public void removeServiceListener(ServiceListener listener)
    {
        services.removeServiceListener(listener);
    }

    public boolean put(ServiceInfo service)
    {
        if (matches(service))
        {
            services.put(service);
            return true;
        }
        return false;
    }

    public void addAll(Collection<ServiceInfo> serviceInfos)
    {
        services.lock();
        try
        {
            for (ServiceInfo serviceInfo : serviceInfos) put(serviceInfo);
        }
        finally
        {
            services.unlock();
        }
    }

    public Collection<ServiceInfo> getServices()
    {
        return services.getServiceInfos();
    }

    private boolean addAttributes(ServiceInfo.Key key, Attributes attributes)
    {
        services.lock();
        try
        {
            ServiceInfo service = services.get(key);
            if (service != null && matches(service))
            {
                services.addAttributes(key, attributes);
                return true;
            }
            return false;
        }
        finally
        {
            services.unlock();
        }
    }

    private boolean removeAttributes(ServiceInfo.Key key, Attributes attributes)
    {
        services.lock();
        try
        {
            ServiceInfo service = services.get(key);
            if (service != null && matches(service))
            {
                services.removeAttributes(key, attributes);
                return true;
            }
            return false;
        }
        finally
        {
            services.unlock();
        }
    }

    public boolean remove(ServiceInfo.Key key)
    {
        return services.remove(key).getPrevious() != null;
    }

    protected boolean matches(ServiceInfo serviceInfo)
    {
        if (serviceType == null || serviceType.matches(serviceInfo.resolveServiceType()))
        {
            if (scopes == null || scopes.match(serviceInfo.getScopes()))
            {
                if (filter == null || filter.matches(serviceInfo.getAttributes()))
                {
                    if (language == null || language.equals(serviceInfo.getLanguage()))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
