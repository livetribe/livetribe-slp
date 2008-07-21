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
package org.livetribe.slp.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.sa.ServiceEvent;
import org.livetribe.slp.sa.ServiceListener;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.util.Listeners;

/**
 * // TODO: RFC 2608 (8.6) suggests that ServiceAgents have an attribute 'service-type'
 * // TODO: whose value is all the service types of services represented by the SA.
 * // TODO: put a getServiceTypes() or something like that to support it
 * A cache for {@link ServiceInfo}s, that provides facilities to store, update, remove and query ServiceInfos.
 *
 * @version $Rev: 252 $ $Date: 2006-08-21 09:56:15 +0200 (Mon, 21 Aug 2006) $
 */
public class ServiceInfoCache<T extends ServiceInfo>
{
    private final Lock lock = new ReentrantLock();
    private final Map<ServiceInfo.Key, ServiceType> keysToServiceTypes = new HashMap<ServiceInfo.Key, ServiceType>();
    private final Map<ServiceInfo.Key, T> keysToServiceInfos = new HashMap<ServiceInfo.Key, T>();
    private final Listeners<ServiceListener> listeners = new Listeners<ServiceListener>();

    /**
     * Locks this cache in order to perform multiple operations atomically.
     *
     * @see #unlock()
     */
    public void lock()
    {
        lock.lock();
    }

    /**
     * Unlocks this cache.
     *
     * @see #lock()
     */
    public void unlock()
    {
        lock.unlock();
    }

    public void addServiceListener(ServiceListener listener)
    {
        listeners.add(listener);
    }

    public void removeServiceListener(ServiceListener listener)
    {
        listeners.remove(listener);
    }

    protected void notifyServiceAdded(T previous, T current)
    {
        ServiceEvent event = new ServiceEvent(this, previous, current);
        for (ServiceListener listener : listeners) listener.serviceAdded(event);
    }

    protected void notifyServiceUpdated(T previous, T current)
    {
        ServiceEvent event = new ServiceEvent(this, previous, current);
        for (ServiceListener listener : listeners) listener.serviceUpdated(event);
    }

    protected void notifyServiceRemoved(T previous, T current)
    {
        ServiceEvent event = new ServiceEvent(this, previous, current);
        for (ServiceListener listener : listeners) listener.serviceRemoved(event);
    }

    protected void check(T service)
    {
        // RFC 2608, 7.0
        if (service.getLanguage() == null)
        {
            throw new ServiceLocationException("Could not register service " + service + ", missing language", ServiceLocationException.INVALID_REGISTRATION);
        }
        int lifetime = service.getServiceURL().getLifetime();
        if (lifetime != ServiceURL.LIFETIME_PERMANENT && (lifetime <= ServiceURL.LIFETIME_NONE || lifetime > ServiceURL.LIFETIME_MAXIMUM))
        {
            throw new ServiceLocationException("Could not register service " + service + ", invalid lifetime " + lifetime, ServiceLocationException.INVALID_REGISTRATION);
        }
    }

    /**
     * Adds the given service to this cache replacing an eventually existing entry.
     *
     * @param service The service to cache
     * @return a result whose previous is the replaced service and where current is the given service
     */
    public Result<T> put(T service)
    {
        check(service);

        ServiceType serviceType = service.resolveServiceType();
        T previous = null;

        lock();
        try
        {
            ServiceType existingServiceType = keysToServiceTypes.get(service.getKey());
            if (existingServiceType != null && !existingServiceType.equals(serviceType))
                throw new ServiceLocationException("Invalid registration of service " + service.getKey() +
                        ": already registered under service type " + existingServiceType +
                        ", cannot be registered also under service type " + serviceType, ServiceLocationException.INVALID_REGISTRATION);
            keysToServiceTypes.put(service.getKey(), serviceType);
            previous = keysToServiceInfos.put(service.getKey(), service);
            service.setRegistered(true);
            if (previous != null) previous.setRegistered(false);
        }
        finally
        {
            unlock();
        }

        notifyServiceAdded(previous, service);
        return new Result<T>(previous, service);
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
     * @param key the {@link ServiceInfo.Key} identifying the service
     * @return the service correspondent to the given {@link ServiceInfo.Key}.
     */
    public T get(ServiceInfo.Key key)
    {
        lock();
        try
        {
            return keysToServiceInfos.get(key);
        }
        finally
        {
            unlock();
        }
    }

    /**
     * Updates an existing ServiceInfo identified by the given Key, adding the given attributes.
     *
     * @param key        the service's key
     * @param attributes the attributes to add
     * @return a result whose previous is the service prior the update and where current is the current service
     */
    public Result<T> addAttributes(ServiceInfo.Key key, Attributes attributes)
    {
        T previous = null;
        T current = null;

        lock();
        try
        {
            previous = get(key);
            // Updating a service that does not exist must fail (RFC 2608, 9.3)
            if (previous == null)
                throw new ServiceLocationException("Could not find service to update " + key, ServiceLocationException.INVALID_UPDATE);

            current = (T)previous.addAttributes(attributes);
            keysToServiceInfos.put(current.getKey(), current);
            current.setRegistered(true);
            previous.setRegistered(false);
        }
        finally
        {
            unlock();
        }

        notifyServiceUpdated(previous, current);
        return new Result<T>(previous, current);
    }

    /**
     * Updates an existing ServiceInfo identified by the given Key, removing the given attributes.
     *
     * @param key        the service's key
     * @param attributes the attributes to remove
     * @return a result whose previous is the service prior the update and where current is the current service
     */
    public Result<T> removeAttributes(ServiceInfo.Key key, Attributes attributes)
    {
        T previous = null;
        T current = null;

        lock();
        try
        {
            previous = get(key);
            // Updating a service that does not exist must fail (RFC 2608, 9.3)
            if (previous == null)
                throw new ServiceLocationException("Could not find service to update " + key, ServiceLocationException.INVALID_UPDATE);

            current = (T)previous.removeAttributes(attributes);
            keysToServiceInfos.put(current.getKey(), current);
            current.setRegistered(true);
            previous.setRegistered(false);
        }
        finally
        {
            unlock();
        }

        notifyServiceUpdated(previous, current);
        return new Result<T>(previous, current);
    }

    /**
     * Removes an existing entry with the given {@link ServiceInfo.Key}; if the entry does not exist, does nothing.
     *
     * @param key the service's key
     * @return a result whose previous is the existing service and where current is null
     */
    public Result<T> remove(ServiceInfo.Key key)
    {
        T previous = null;

        lock();
        try
        {
            ServiceType serviceType = keysToServiceTypes.remove(key);
            if (serviceType == null) return new Result<T>(null, null);
            previous = keysToServiceInfos.remove(key);
            previous.setRegistered(false);
        }
        finally
        {
            unlock();
        }

        notifyServiceRemoved(previous, null);
        return new Result<T>(previous, null);
    }

    public List<T> match(ServiceType serviceType, String language, Scopes scopes, Filter filter)
    {
        List<T> result = new ArrayList<T>();
        long now = System.currentTimeMillis();
        lock();
        try
        {
            for (T serviceInfo : keysToServiceInfos.values())
            {
                if (serviceInfo.isRegistered() && !serviceInfo.isExpiredAsOf(now))
                {
                    if (matchServiceTypes(serviceInfo.resolveServiceType(), serviceType))
                    {
                        if (matchLanguage(serviceInfo.getLanguage(), language))
                        {
                            if (matchScopes(serviceInfo.getScopes(), scopes))
                            {
                                if (matchAttributes(serviceInfo.getAttributes(), filter))
                                {
                                    result.add(serviceInfo);
                                }
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
        return asked == null || asked.matches(registered);
    }

    private boolean matchScopes(Scopes registered, Scopes asked)
    {
        return asked == null || registered.match(asked);
    }

    private boolean matchAttributes(Attributes registered, Filter filter)
    {
        return filter == null || filter.matches(registered);
    }

    private boolean matchLanguage(String registered, String asked)
    {
        return asked == null || asked.equals(registered);
    }

    public List<T> getServiceInfos()
    {
        lock();
        try
        {
            return new ArrayList<T>(keysToServiceInfos.values());
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
     *
     * @return The list of purged entries.
     */
    public List<T> purge()
    {
        List<T> result = new ArrayList<T>();
        long now = System.currentTimeMillis();
        lock();
        try
        {
            for (T serviceInfo : getServiceInfos())
            {
                if (serviceInfo.isExpiredAsOf(now))
                {
                    T purged = remove(serviceInfo.getKey()).getPrevious();
                    if (purged != null) result.add(purged);
                }
            }
            return result;
        }
        finally
        {
            unlock();
        }
    }

    public static class Result<T>
    {
        private final T previous;
        private final T current;

        public Result(T previous, T current)
        {
            this.previous = previous;
            this.current = current;
        }

        public T getPrevious()
        {
            return previous;
        }

        public T getCurrent()
        {
            return current;
        }
    }
}
