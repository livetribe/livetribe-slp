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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.StandardAgent;
import org.livetribe.slp.spi.da.DirectoryAgentCache;
import org.livetribe.slp.spi.da.DirectoryAgentInfo;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;
import org.livetribe.slp.spi.sa.ServiceAgentManager;
import org.livetribe.slp.spi.sa.StandardServiceAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardServiceAgent extends StandardAgent implements ServiceAgent
{
    private int discoveryStartWaitBound;
    private long discoveryPeriod;
    private ServiceAgentManager manager;
    private final Set services = new HashSet();
    private final Lock servicesLock = new ReentrantLock();
    private MessageListener multicastListener;
    private final DirectoryAgentCache daCache = new DirectoryAgentCache();
    private ScheduledExecutorService scheduledExecutorService;

    public void setServiceAgentManager(ServiceAgentManager manager)
    {
        this.manager = manager;
    }

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        setDiscoveryStartWaitBound(configuration.getDADiscoveryStartWaitBound());
        setDiscoveryPeriod(configuration.getDADiscoveryPeriod());
        if (manager != null) manager.setConfiguration(configuration);
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService)
    {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public int getDiscoveryStartWaitBound()
    {
        return discoveryStartWaitBound;
    }

    /**
     * Sets the bound (in seconds) to the initial random delay this ServiceAgent waits
     * before attempting to discover DirectoryAgents
     */
    public void setDiscoveryStartWaitBound(int discoveryStartWaitBound)
    {
        this.discoveryStartWaitBound = discoveryStartWaitBound;
    }

    public long getDiscoveryPeriod()
    {
        return discoveryPeriod;
    }

    public void setDiscoveryPeriod(long discoveryPeriod)
    {
        this.discoveryPeriod = discoveryPeriod;
    }

    public void register(ServiceInfo service) throws IOException, ServiceLocationException
    {
        if (isRunning()) registerService(service);
        addService(service);
    }

    public void deregister(ServiceInfo service) throws IOException, ServiceLocationException
    {
        if (isRunning()) deregisterService(service);
        removeService(service);
    }

    private boolean addService(ServiceInfo service)
    {
        servicesLock.lock();
        try
        {
            return services.add(service);
        }
        finally
        {
            servicesLock.unlock();
        }
    }

    private boolean removeService(ServiceInfo service)
    {
        servicesLock.lock();
        try
        {
            return services.remove(service);
        }
        finally
        {
            servicesLock.unlock();
        }
    }

    private boolean hasServices()
    {
        servicesLock.lock();
        try
        {
            return !services.isEmpty();
        }
        finally
        {
            servicesLock.unlock();
        }
    }

    protected void doStart() throws Exception
    {
        if (manager == null)
        {
            manager = createServiceAgentManager();
            manager.setConfiguration(getConfiguration());
        }
        manager.start();

        multicastListener = new MulticastListener();
        manager.addMessageListener(multicastListener, true);

        if (scheduledExecutorService == null) scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        long delay = new Random(System.currentTimeMillis()).nextInt(getDiscoveryStartWaitBound() + 1) * 1000L;
        scheduledExecutorService.scheduleWithFixedDelay(new DirectoryAgentDiscovery(), delay, getDiscoveryPeriod() * 1000L, TimeUnit.MILLISECONDS);

        registerServices();
    }

    protected ServiceAgentManager createServiceAgentManager()
    {
        return new StandardServiceAgentManager();
    }

    protected void doStop() throws Exception
    {
        // RFC 2608, 10.6, requires services to deregister when no longer available
        deregisterServices();

        if (scheduledExecutorService != null)
        {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }

        manager.removeMessageListener(multicastListener, true);
        manager.stop();
    }

    private void registerServices() throws IOException, ServiceLocationException
    {
        servicesLock.lock();
        try
        {
            for (Iterator servs = services.iterator(); servs.hasNext();)
            {
                ServiceInfo service = (ServiceInfo)servs.next();
                registerService(service);
            }
        }
        finally
        {
            servicesLock.unlock();
        }
    }

    private void registerServices(DirectoryAgentInfo da) throws IOException, ServiceLocationException
    {
        servicesLock.lock();
        try
        {
            for (Iterator servs = services.iterator(); servs.hasNext();)
            {
                ServiceInfo service = (ServiceInfo)servs.next();
                registerService(service, da);
            }
        }
        finally
        {
            servicesLock.unlock();
        }
    }

    private void registerService(ServiceInfo service) throws IOException, ServiceLocationException
    {
        List das = findDirectoryAgents(service.getScopes());
        for (int i = 0; i < das.size(); ++i)
        {
            DirectoryAgentInfo directory = (DirectoryAgentInfo)das.get(i);
            registerService(service, directory);
        }
    }

    private void registerService(ServiceInfo service, DirectoryAgentInfo da) throws IOException, ServiceLocationException
    {
        ServiceURL su = service.getServiceURL();

        ServiceType st = service.getServiceType();
        if (st == null) st = su.getServiceType();

        String[] sc = service.getScopes();
        if (sc == null) sc = getScopes();

        InetAddress address = InetAddress.getByName(da.getHost());
        ServiceAgentInfo sa = new ServiceAgentInfo(st, su, sc, service.getAttributes(), service.getLanguage(), true);
        SrvAck srvAck = manager.unicastSrvReg(address, sa);
        int errorCode = srvAck.getErrorCode();
        if (errorCode != 0)
            throw new ServiceLocationException("Could not register service " + su + " to DirectoryAgent " + address, errorCode);
        if (logger.isLoggable(Level.FINE)) logger.fine("Registered service " + su + " to DirectoryAgent " + address);

        long renewalPeriod = calculateRenewalPeriod(sa);
        long renewalDelay = calculateRenewalDelay(sa);
        if (renewalPeriod > 0)
            scheduledExecutorService.scheduleWithFixedDelay(new RegistrationRenewal(service, da), renewalDelay, renewalPeriod, TimeUnit.MILLISECONDS);
    }

    private long calculateRenewalPeriod(ServiceAgentInfo sa)
    {
        long lifetime = sa.getServiceURL().getLifetime();
        if (lifetime < ServiceURL.LIFETIME_NONE) lifetime = ServiceURL.LIFETIME_MAXIMUM;
        // Convert from seconds to milliseconds
        lifetime *= 1000L;
        return lifetime;
    }

    private long calculateRenewalDelay(ServiceAgentInfo sa)
    {
        long lifetime = calculateRenewalPeriod(sa);
        // Renew when 75% of the lifetime is elapsed
        return lifetime - (lifetime >> 2);
    }

    private void deregisterServices() throws IOException, ServiceLocationException
    {
        servicesLock.lock();
        for (Iterator servs = services.iterator(); servs.hasNext();)
        {
            ServiceInfo service = (ServiceInfo)servs.next();
            deregisterService(service);
        }
    }

    private void deregisterService(ServiceInfo service) throws IOException, ServiceLocationException
    {
        List das = findDirectoryAgents(service.getScopes());
        for (int i = 0; i < das.size(); ++i)
        {
            DirectoryAgentInfo directory = (DirectoryAgentInfo)das.get(i);
            deregisterService(service, directory);
        }
    }

    private void deregisterService(ServiceInfo service, DirectoryAgentInfo da) throws IOException, ServiceLocationException
    {
        ServiceURL su = service.getServiceURL();

        ServiceType st = service.getServiceType();
        if (st == null) st = su.getServiceType();

        String[] sc = service.getScopes();
        if (sc == null) sc = getScopes();

        InetAddress address = InetAddress.getByName(da.getHost());
        ServiceAgentInfo sa = new ServiceAgentInfo(st, su, sc, null, null, true);

        SrvAck srvAck = manager.unicastSrvDeReg(address, sa);
        int errorCode = srvAck.getErrorCode();
        if (errorCode != 0)
            throw new ServiceLocationException("Could not deregister service " + su + " from DirectoryAgent " + address, errorCode);
        if (logger.isLoggable(Level.FINE))
            logger.fine("Deregistered service " + su + " from DirectoryAgent " + address);
    }

    protected List findDirectoryAgents(String[] scopes) throws IOException, ServiceLocationException
    {
        List das = getCachedDirectoryAgents(scopes);
        if (das.isEmpty())
        {
            das = discoverDirectoryAgents(scopes);
            cacheDirectoryAgents(das);
        }
        return das;
    }

    protected List getCachedDirectoryAgents(String[] scopes)
    {
        return daCache.getByScopes(scopes);
    }

    private boolean cacheDirectoryAgent(DirectoryAgentInfo info)
    {
        return daCache.add(info);
    }

    private void cacheDirectoryAgents(List infos)
    {
        daCache.addAll(infos);
    }

    private void uncacheDirectoryAgent(DirectoryAgentInfo info)
    {
        daCache.remove(info);
    }

    protected List discoverDirectoryAgents(String[] scopes) throws IOException
    {
        List result = new ArrayList();
        DAAdvert[] daAdverts = manager.multicastDASrvRqst(scopes, null, null, -1);
        for (int i = 0; i < daAdverts.length; ++i)
        {
            DAAdvert daAdvert = daAdverts[i];
            DirectoryAgentInfo info = DirectoryAgentInfo.from(daAdvert);
            result.add(info);
        }
        return result;
    }

    protected void handleMulticastDAAdvert(DAAdvert message, InetSocketAddress address)
    {
        List scopesList = Arrays.asList(getScopes());
        List messageScopesList = Arrays.asList(message.getScopes());
        if (!scopesList.contains(DEFAULT_SCOPE) && Collections.disjoint(scopesList, messageScopesList))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + message + ": no scopes match among SA scopes " + scopesList + " and message scopes " + messageScopesList);
            return;
        }

        DirectoryAgentInfo da = DirectoryAgentInfo.from(message);
        if (message.getBootTime() == 0L)
        {
            // DA is shutting down
            uncacheDirectoryAgent(da);
        }
        else
        {
            boolean isNew = cacheDirectoryAgent(da);
            if (isNew)
            {
                try
                {
                    // TODO: RFC 2608, 12.2.2 requires to wait some time before registering
                    registerServices(da);
                }
                catch (IOException x)
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "ServiceAgent " + this + " dropping message " + message + ": could not register service to DA " + da, x);
                }
                catch (ServiceLocationException x)
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "ServiceAgent " + this + " dropping message " + message + ": could not register service to DA " + da, x);
                }
            }
        }
    }

    /**
     * ServiceAgents listen for multicast messages that may arrive.
     * They are interested in:
     * <ul>
     * <li>DAAdverts, from DAs that boot or shutdown</li>
     * </ul>
     */
    private class MulticastListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            InetSocketAddress address = event.getSocketAddress();
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("ServiceAgent multicast message listener received message " + message);

                if (!message.isMulticast())
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("ServiceAgent " + this + " dropping message " + message + ": expected multicast flag set");
                    return;
                }

                switch (message.getMessageType())
                {
                    case Message.DA_ADVERT_TYPE:
                        handleMulticastDAAdvert((DAAdvert)message, address);
                        break;
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("ServiceAgent " + this + " dropping multicast message " + message + ": not handled by ServiceAgents");
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "ServiceAgent " + this + " received bad multicast message from: " + address + ", ignoring", x);
            }
        }
    }

    /**
     * ServiceAgent must refresh their registration with DAs before the lifetime specified
     * in the ServiceURL expires, otherwise the DA does not advertise them anymore.
     */
    private class RegistrationRenewal implements Runnable
    {
        private final ServiceInfo service;
        private final DirectoryAgentInfo directory;

        public RegistrationRenewal(ServiceInfo service, DirectoryAgentInfo directory)
        {
            this.service = service;
            this.directory = directory;
        }

        public void run()
        {
            try
            {
                registerService(service, directory);
            }
            catch (Exception x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Could not renew service registration", x);
            }
        }
    }

    private class DirectoryAgentDiscovery implements Runnable
    {
        public void run()
        {
            try
            {
                List das = discoverDirectoryAgents(getScopes());
                cacheDirectoryAgents(das);
            }
            catch (Exception x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Could not discover DAs", x);
            }
        }
    }
}
