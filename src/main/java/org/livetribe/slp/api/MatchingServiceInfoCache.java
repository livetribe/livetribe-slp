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
package org.livetribe.slp.api;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.MessageRegistrationListener;
import org.livetribe.slp.spi.ServiceInfoCache;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.filter.FilterParser;
import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.util.ConcurrentListeners;

/**
 * @version $Rev$ $Date$
 */
public class MatchingServiceInfoCache implements MessageRegistrationListener
{
    private final ServiceInfoCache services = new ServiceInfoCache();
    private final ServiceType serviceType;
    private final Scopes matchingScopes;
    private final Filter matchingFilter;
    private final String matchingLanguage;
    private final ConcurrentListeners listeners = new ConcurrentListeners();

    public MatchingServiceInfoCache(ServiceType serviceType, Scopes matchingScopes, String matchingFilter, String matchingLanguage) throws ServiceLocationException
    {
        this.serviceType = serviceType;
        this.matchingScopes = matchingScopes;
        this.matchingFilter = new FilterParser().parse(matchingFilter);
        this.matchingLanguage = matchingLanguage;
    }

    public void addServiceRegistrationListener(ServiceRegistrationListener listener)
    {
        listeners.add(listener);
    }

    public void removeServiceRegistrationListener(ServiceRegistrationListener listener)
    {
        listeners.remove(listener);
    }

    public boolean put(ServiceInfo service)
    {
        if (matches(service))
        {
            ServiceInfo result = services.put(service);
            notifyServiceRegistered(result, service);
            return true;
        }
        return false;
    }

    public void putAll(Collection serviceInfos)
    {
        services.lock();
        try
        {
            for (Iterator iterator = serviceInfos.iterator(); iterator.hasNext();)
            {
                ServiceInfo serviceInfo = (ServiceInfo)iterator.next();
                put(serviceInfo);
            }
        }
        finally
        {
            services.unlock();
        }
    }

    public ServiceInfo get(ServiceInfo.Key key)
    {
        return services.get(key);
    }

    public Collection getServiceInfos()
    {
        return services.getServiceInfos();
    }

    public boolean updateAdd(ServiceInfo service)
    {
        if (matches(service))
        {
            ServiceInfo existing, current;
            services.lock();
            try
            {
                existing = services.updateAdd(service);
                current = services.get(service.getKey());
            }
            finally
            {
                services.unlock();
            }
            notifyServiceUpdated(existing, current);
            return true;
        }
        return false;
    }

    public boolean updateRemove(ServiceInfo service)
    {
        if (matches(service))
        {
            ServiceInfo existing, current;
            services.lock();
            try
            {
                existing = services.updateRemove(service);
                current = services.get(service.getKey());
            }
            finally
            {
                services.unlock();
            }
            notifyServiceUpdated(existing, current);
            return true;
        }
        return false;
    }

    public boolean remove(ServiceInfo.Key key)
    {
        ServiceInfo serviceInfo = services.remove(key);
        if (serviceInfo != null)
        {
            notifyServiceDeregistered(serviceInfo, null);
            return true;
        }
        return false;
    }

    public void clear()
    {
        services.clear();
    }

    private boolean matches(ServiceInfo serviceInfo)
    {
        if (serviceType == null || serviceType.matches(serviceInfo.resolveServiceType()))
        {
            if (matchingScopes == null || matchingScopes.match(serviceInfo.getScopes()))
            {
                if (matchingFilter == null || matchingFilter.matches(serviceInfo.getAttributes()))
                {
                    if (matchingLanguage == null || matchingLanguage.equals(serviceInfo.getLanguage()))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected void notifyServiceRegistered(ServiceInfo existing, ServiceInfo current)
    {
        listeners.notify("serviceRegistered", new ServiceRegistrationEvent(this, current, existing));
    }

    protected void notifyServiceUpdated(ServiceInfo existing, ServiceInfo current)
    {
        listeners.notify("serviceUpdated", new ServiceRegistrationEvent(this, current, existing));
    }

    protected void notifyServiceDeregistered(ServiceInfo existing, ServiceInfo current)
    {
        listeners.notify("serviceDeregistered", new ServiceRegistrationEvent(this, current, existing));
    }

    protected void notifyServiceExpired(ServiceInfo existing, ServiceInfo current)
    {
        listeners.notify("serviceExpired", new ServiceRegistrationEvent(this, current, existing));
    }

    public void handleSrvReg(SrvReg srvReg)
    {
        ServiceInfo serviceInfo = ServiceInfo.from(srvReg);
        if (srvReg.isFresh())
            put(serviceInfo);
        else
            updateAdd(serviceInfo);
    }

    public void handleSrvDeReg(SrvDeReg srvDeReg)
    {
        ServiceInfo serviceInfo = ServiceInfo.from(srvDeReg);
        if (serviceInfo.hasAttributes())
            updateRemove(serviceInfo);
        else
            remove(serviceInfo.getKey());
    }

    public void purge()
    {
        List purged = services.purge();
        for (int i = 0; i < purged.size(); ++i)
        {
            ServiceInfo serviceInfo = (ServiceInfo)purged.get(i);
            notifyServiceExpired(serviceInfo, null);
        }
    }
}
