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
import java.net.Socket;
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
import org.livetribe.slp.Attributes;
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
import org.livetribe.slp.spi.msg.SrvRqst;
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
    private Attributes attributes;
    private String language;
    private int discoveryStartWaitBound;
    private long discoveryPeriod;
    private InetAddress address;
    private InetAddress localhost;
    private ServiceAgentManager manager;
    private ScheduledExecutorService scheduledExecutorService;
    private ServiceAgentInfo serviceAgent;
    private MessageListener multicastListener;
    private MessageListener unicastListener;
    private final Set services = new HashSet();
    private final Lock servicesLock = new ReentrantLock();
    private final DirectoryAgentCache daCache = new DirectoryAgentCache();

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

    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    public Attributes getAttributes()
    {
        return attributes;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getLanguage()
    {
        return language;
    }

    /**
     * Sets the bound (in seconds) to the initial random delay this ServiceAgent waits
     * before attempting to discover DirectoryAgents
     */
    public void setDiscoveryStartWaitBound(int discoveryStartWaitBound)
    {
        this.discoveryStartWaitBound = discoveryStartWaitBound;
    }

    public int getDiscoveryStartWaitBound()
    {
        return discoveryStartWaitBound;
    }

    public void setDiscoveryPeriod(long discoveryPeriod)
    {
        this.discoveryPeriod = discoveryPeriod;
    }

    public long getDiscoveryPeriod()
    {
        return discoveryPeriod;
    }

    public void setInetAddress(InetAddress address)
    {
        this.address = address;
    }

    public InetAddress getInetAddress()
    {
        return address;
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

    protected void doStart() throws Exception
    {
        InetAddress agentAddr = getInetAddress();
        if (agentAddr == null) agentAddr = InetAddress.getLocalHost();
        if (agentAddr.isLoopbackAddress())
        {
            if (logger.isLoggable(Level.WARNING))
                logger.warning("ServiceAgent " + this + " starting on loopback address; this is normally wrong, check your hosts configuration");
        }
        localhost = agentAddr;

        serviceAgent = new ServiceAgentInfo(getAttributes(), getLanguage(), getScopes(), "service:service-agent://" + localhost);

        if (manager == null)
        {
            manager = createServiceAgentManager();
            manager.setConfiguration(getConfiguration());
        }
        manager.start();

        multicastListener = new MulticastListener();
        unicastListener = new UnicastListener();
        manager.addMessageListener(multicastListener, true);
        manager.addMessageListener(unicastListener, false);

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

        manager.removeMessageListener(unicastListener, false);
        manager.removeMessageListener(multicastListener, true);
        manager.stop();
    }

    private void registerServices() throws IOException, ServiceLocationException
    {
        servicesLock.lock();
        try
        {
            for (Iterator serviceInfos = services.iterator(); serviceInfos.hasNext();)
            {
                ServiceInfo service = (ServiceInfo)serviceInfos.next();
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
            for (Iterator serviceInfos = services.iterator(); serviceInfos.hasNext();)
            {
                ServiceInfo service = (ServiceInfo)serviceInfos.next();
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
        InetAddress address = InetAddress.getByName(da.getHost());
        SrvAck srvAck = manager.unicastSrvReg(address, service, serviceAgent, true);
        int errorCode = srvAck.getErrorCode();
        if (errorCode != 0)
            throw new ServiceLocationException("Could not register service " + service.getServiceURL() + " to DirectoryAgent " + address, errorCode);
        if (logger.isLoggable(Level.FINE)) logger.fine("Registered service " + service.getServiceURL() + " to DirectoryAgent " + address);

        long renewalPeriod = calculateRenewalPeriod(service);
        long renewalDelay = calculateRenewalDelay(service);
        if (renewalPeriod > 0)
            scheduledExecutorService.scheduleWithFixedDelay(new RegistrationRenewal(service, da), renewalDelay, renewalPeriod, TimeUnit.MILLISECONDS);
    }

    private long calculateRenewalPeriod(ServiceInfo service)
    {
        long lifetime = service.getServiceURL().getLifetime();
        if (lifetime < ServiceURL.LIFETIME_NONE) lifetime = ServiceURL.LIFETIME_MAXIMUM;
        // Convert from seconds to milliseconds
        lifetime *= 1000L;
        return lifetime;
    }

    private long calculateRenewalDelay(ServiceInfo service)
    {
        long lifetime = calculateRenewalPeriod(service);
        // Renew when 75% of the lifetime is elapsed
        return lifetime - (lifetime >> 2);
    }

    private void deregisterServices() throws IOException, ServiceLocationException
    {
        servicesLock.lock();
        try
        {
            for (Iterator serviceInfos = services.iterator(); serviceInfos.hasNext();)
            {
                ServiceInfo service = (ServiceInfo)serviceInfos.next();
                deregisterService(service);
            }
        }
        finally
        {
            servicesLock.unlock();
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
        InetAddress address = InetAddress.getByName(da.getHost());
        SrvAck srvAck = manager.unicastSrvDeReg(address, service, serviceAgent);
        int errorCode = srvAck.getErrorCode();
        if (errorCode != 0)
            throw new ServiceLocationException("Could not deregister service " + service.getServiceURL() + " from DirectoryAgent " + address, errorCode);
        if (logger.isLoggable(Level.FINE))
            logger.fine("Deregistered service " + service.getServiceURL() + " from DirectoryAgent " + address);
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

    protected void handleMulticastSrvRqst(SrvRqst message, InetSocketAddress address)
    {
        ServiceType serviceType = message.getServiceType();
        if (serviceType.isAbstractType() || !"service-agent".equals(serviceType.getPrincipleTypeName()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + message + ": expected service type 'service-agent', got " + serviceType);
            return;
        }

        List scopesList = Arrays.asList(getScopes());
        List messageScopes = Arrays.asList(message.getScopes());
        if (!scopesList.contains(DEFAULT_SCOPE) && Collections.disjoint(scopesList, messageScopes))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + message + ": no scopes match among SA scopes " + scopesList + " and message scopes " + messageScopes);
            return;
        }

        Set prevResponders = message.getPreviousResponders();
        String responder = localhost.getHostAddress();
        if (prevResponders.contains(responder))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + message + ": already contains responder " + responder);
            return;
        }

        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        try
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " sending UDP unicast reply to " + address);
            manager.unicastSAAdvert(address, getScopes(), null, new Integer(message.getXID()), message.getLanguage());
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "ServiceAgent " + this + " cannot send reply to " + address, x);
        }
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

    protected void handleUnicastSrvRqst(SrvRqst message, Socket socket)
    {
        if (logger.isLoggable(Level.FINE))
            logger.fine("ServiceAgent " + this + " queried for services of type " + message.getServiceType());

        List matchingServices = matchServices(message.getServiceType(), message.getFilter(), message.getScopes(), message.getLanguage());
        ServiceURL[] serviceURLs = (ServiceURL[])matchingServices.toArray(new ServiceURL[matchingServices.size()]);

        try
        {
            manager.unicastSrvRply(socket, new Integer(message.getXID()), message.getLanguage(), serviceURLs);
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " returned " + serviceURLs.length + " services of type " + message.getServiceType());
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send unicast reply to " + socket, x);
        }
    }

    private List matchServices(ServiceType serviceType, String filter, String[] scopes, String language)
    {
        servicesLock.lock();
        try
        {
            List result = new ArrayList();
            for (Iterator serviceInfos = services.iterator(); serviceInfos.hasNext();)
            {
                ServiceInfo serviceInfo = (ServiceInfo)serviceInfos.next();
                ServiceType registeredServiceType = serviceInfo.getServiceType();
                if (registeredServiceType == null) registeredServiceType = serviceInfo.getServiceURL().getServiceType();
                if (registeredServiceType.matches(serviceType))
                {
                    // TODO: match the other parameters
/*
                    if (filter != null)
                    {
                        Attributes registeredAttributes = serviceInfo.getAttributes();
                        if (registeredAttributes != null)
                        {
                            Attributes attributes = new Attributes(filter);
                            if (registeredAttributes.match(attributes))
                            {

                            }
                        }
                    }
*/
                    result.add(serviceInfo.getServiceURL());
                }
            }
            return result;
        }
        finally
        {
            servicesLock.unlock();
        }
    }

    /**
     * ServiceAgents listen for multicast messages that may arrive.
     * They are interested in:
     * <ul>
     * <li>SrvRqst, from UAs that want to discover ServiceURLs in absence of DAs; the reply is a SAAdvert</li>
     * <li>DAAdverts, from DAs that boot or shutdown; no reply, just update of internal caches</li>
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
                    case  Message.SRV_RQST_TYPE:
                        handleMulticastSrvRqst((SrvRqst)message, address);
                        break;
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
     * ServiceAgents listen for unicast messages from UAs.
     * They are interested in:
     * <ul>
     * <li>SrvRqst, from UAs that want find ServiceURLs; the reply is a SrvRply</li>
     * </ul>
     */
    private class UnicastListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("ServiceAgent unicast message listener received message " + message);

                if (message.isMulticast())
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("ServiceAgent " + this + " dropping message " + message + ": expected multicast flag unset");
                    return;
                }

                switch (message.getMessageType())
                {
                    case Message.SRV_RQST_TYPE:
                        handleUnicastSrvRqst((SrvRqst)message, (Socket)event.getSource());
                        break;
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("ServiceAgent " + this + " dropping unicast message " + message + ": not handled by ServiceAgents");
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "ServiceAgent " + this + " received bad unicast message from: " + event.getSocketAddress() + ", ignoring", x);
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
