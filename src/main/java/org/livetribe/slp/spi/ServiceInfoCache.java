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
package org.livetribe.slp.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.filter.Filter;

/**
 * A cache for {@link ServiceInfo}s, that provides facilities to store, update, remove and query.
 *
 * @version $Rev$ $Date$
 */
public class ServiceInfoCache
{
    private final Map keysToServiceTypes = new HashMap();
    private final Map/*<ServiceInfo.Key, ServiceInfo>*/ keysToServices = new HashMap();
    private Lock lock = new ReentrantLock();

    public void lock()
    {
        lock.lock();
    }

    public void unlock()
    {
        lock.unlock();
    }

    /**
     * Adds the given service to this cache replacing an eventually existing entry.
     * @param service The service to cache
     * @return Null if no service already existed, or the replaced service.
     */
    public ServiceInfo put(ServiceInfo service)
    {
        lock();
        try
        {
            ServiceType serviceType = service.resolveServiceType();
            ServiceType existingServiceType = (ServiceType)keysToServiceTypes.get(service.getKey());
            if (existingServiceType != null && !existingServiceType.equals(serviceType))
                throw new IllegalArgumentException("Invalid registration of service " + service.getKey() +
                        ": already registered under service type " + existingServiceType +
                        ", cannot be registered also under service type " + serviceType);
            keysToServiceTypes.put(service.getKey(), serviceType);

            service.setRegistrationTime(System.currentTimeMillis());
            return (ServiceInfo)keysToServices.put(service.getKey(), service);
        }
        finally
        {
            unlock();
        }
    }

    public int getSize()
    {
        lock();
        try
        {
            return keysToServiceTypes.size();
        }
        finally
        {
            unlock();
        }
    }

    /**
     * Returns the service correspondent to the given {@link ServiceInfo.Key}.
     */
    public ServiceInfo get(ServiceInfo.Key key)
    {
        lock();
        try
        {
            return (ServiceInfo)keysToServices.get(key);
        }
        finally
        {
            unlock();
        }
    }

    /**
     * Updates an existing entry with the given service, adding information contained in the given service;
     * if the entry does not exist, does nothing.
     * @param service The service containing the values that update an eventually existing service
     * @return Null if no service already existed, or the existing service prior update.
     */
    public ServiceInfo updateAdd(ServiceInfo service)
    {
        lock();
        try
        {
            ServiceType serviceType = service.resolveServiceType();

            ServiceInfo existing = (ServiceInfo)keysToServices.get(service.getKey());
            if (existing == null) return null;

            ServiceInfo merged = existing.merge(service);
            merged.setRegistrationTime(System.currentTimeMillis());
            keysToServices.put(merged.getKey(), merged);
            return existing;
        }
        finally
        {
            unlock();
        }
    }

    /**
     * Updates an existing entry with the given service, removing information contained in the given service;
     * if the entry does not exist, does nothing.
     * @param service The service containing the values that update an eventually existing service
     * @return Null if no service already existed, or the existing service prior update.
     */
    public ServiceInfo updateRemove(ServiceInfo service)
    {
        lock();
        try
        {
            ServiceType serviceType = service.resolveServiceType();

            ServiceInfo existing = (ServiceInfo)keysToServices.get(service.getKey());
            if (existing == null) return null;

            ServiceInfo merged = existing.unmerge(service);
            merged.setRegistrationTime(System.currentTimeMillis());
            keysToServices.put(merged.getKey(), merged);
            return existing;
        }
        finally
        {
            unlock();
        }
    }

    /**
     * Removes an existing entry with the given {@link ServiceInfo.Key}; if the entry does not exist, does nothing.
     * @return Null if no service existed, or the existing service.
     */
    public ServiceInfo remove(ServiceInfo.Key key)
    {
        lock();
        try
        {
            ServiceType serviceType = (ServiceType)keysToServiceTypes.remove(key);
            if (serviceType == null) return null;

            return (ServiceInfo)keysToServices.remove(key);
        }
        finally
        {
            unlock();
        }
    }

    public List match(ServiceType serviceType, Scopes scopes, Filter filter, String language)
    {
        List result = new ArrayList();
        lock();
        try
        {
            for (Iterator allServiceInfos = keysToServices.values().iterator(); allServiceInfos.hasNext();)
            {
                ServiceInfo serviceInfo = (ServiceInfo)allServiceInfos.next();
                if (matchServiceTypes(serviceInfo.resolveServiceType(), serviceType))
                {
                    if (matchScopes(serviceInfo.getScopes(), scopes))
                    {
                        if (matchAttributes(serviceInfo.getAttributes(), filter))
                        {
                            if (matchLanguage(serviceInfo.getLanguage(), language))
                            {
                                result.add(serviceInfo);
                            }
                        }
                    }
                }
            }
            return result;
        }
        finally
        {
            unlock();
        }
    }

    private boolean matchServiceTypes(ServiceType registered, ServiceType asked)
    {
        if (asked == null) return true;
        return registered.matches(asked);
    }

    private boolean matchScopes(Scopes registered, Scopes asked)
    {
        if (registered == null) return true;
        return registered.match(asked);
    }

    private boolean matchAttributes(Attributes registered, Filter filter)
    {
        if (filter == null) return true;
        return filter.match(registered);
    }

    private boolean matchLanguage(String registered, String asked)
    {
        if (asked == null) return true;
        return asked.equals(registered);
    }

    public Collection getServices()
    {
        lock();
        try
        {
            return new ArrayList(keysToServices.values());
        }
        finally
        {
            unlock();
        }
    }

    public void clear()
    {
        lock();
        try
        {
            keysToServiceTypes.clear();
            keysToServices.clear();
        }
        finally
        {
            unlock();
        }
    }

    /**
     * Purges from this cache entries whose registration time plus their lifetime
     * is less than the current time; that is, entries that should have been renewed
     * but for some reason they have not been.
     * @return The list of purged entries.
     */
    public List purge()
    {
        List result = new ArrayList();
        long now = System.currentTimeMillis();
        lock();
        try
        {
            for (Iterator iterator = getServices().iterator(); iterator.hasNext();)
            {
                ServiceInfo serviceInfo = (ServiceInfo)iterator.next();
                if (serviceInfo.isExpiredAsOf(now))
                {
                    ServiceInfo purged = remove(serviceInfo.getKey());
                    result.add(purged);
                }
            }
            return result;
        }
        finally
        {
            unlock();
        }
    }
}
