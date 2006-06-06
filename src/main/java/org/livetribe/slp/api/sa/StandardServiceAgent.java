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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledFuture;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.StandardAgent;
import org.livetribe.slp.spi.ServiceInfoCache;
import org.livetribe.slp.spi.da.DirectoryAgentInfo;
import org.livetribe.slp.spi.da.DirectoryAgentInfoCache;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.filter.FilterParser;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.IdentifierExtension;
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
    private MessageListener udpListener;
    private MessageListener tcpListener;
    private final ServiceInfoCache services = new ServiceInfoCache();
    private final DirectoryAgentInfoCache directoryAgents = new DirectoryAgentInfoCache();
    private String identifier;

    public void setServiceAgentManager(ServiceAgentManager manager)
    {
        this.manager = manager;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
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
        // Sanity checks
        if (service.getServiceURL().getLifetime() == 0) throw new ServiceLocationException("Could not register service, invalid lifetime", ServiceLocationException.INVALID_REGISTRATION);
        if (service.getLanguage() == null) throw new ServiceLocationException("Could not register service, missing language", ServiceLocationException.INVALID_REGISTRATION);
        if (!getScopes().match(service.getScopes())) throw new ServiceLocationException("Could not register service, ServiceAgent scopes do not match with service's", ServiceLocationException.SCOPE_NOT_SUPPORTED);

        SAServiceInfo serviceToRegister = new SAServiceInfo(service);

        services.lock();
        try
        {
            // If the service exists, unschedule its renewal
            SAServiceInfo existing = (SAServiceInfo)services.get(serviceToRegister.getKey());
            if (existing != null) existing.cancelRenewals();

            // Add the service first: if the registration with DA fails, this SA exposes anyway this service.
            services.put(serviceToRegister);
        }
        finally
        {
            services.unlock();
        }

        if (isRunning()) registerService(serviceToRegister);
    }

    public void deregister(ServiceInfo service) throws IOException, ServiceLocationException
    {
        if (service.getLanguage() == null) throw new ServiceLocationException("Could not deregister service, missing language", ServiceLocationException.INVALID_REGISTRATION);
        if (!getScopes().match(service.getScopes())) throw new ServiceLocationException("Could not deregister service, ServiceAgent scopes do not match with service's", ServiceLocationException.SCOPE_NOT_SUPPORTED);

        SAServiceInfo serviceToRemove = (SAServiceInfo)services.remove(service.getKey());

        if (isRunning()) deregisterService(serviceToRemove);
    }

    public Collection getServices()
    {
        return services.getServices();
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

        serviceAgent = new ServiceAgentInfo(getIdentifier(), "service:service-agent://" + localhost, getScopes(), getAttributes(), getLanguage());

        if (manager == null)
        {
            manager = createServiceAgentManager();
            manager.setConfiguration(getConfiguration());
        }
        manager.start();

        udpListener = new UDPMessageListener();
        tcpListener = new TCPMessageListener();
        manager.addMessageListener(udpListener, true);
        manager.addMessageListener(tcpListener, false);

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
        services.clear();

        if (scheduledExecutorService != null)
        {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }

        manager.removeMessageListener(tcpListener, false);
        manager.removeMessageListener(udpListener, true);
        manager.stop();
    }

    private void registerServices() throws IOException, ServiceLocationException
    {
        for (Iterator serviceInfos = getServices().iterator(); serviceInfos.hasNext();)
        {
            SAServiceInfo service = (SAServiceInfo)serviceInfos.next();
            registerService(service);
        }
    }

    private void registerServices(DirectoryAgentInfo da) throws IOException, ServiceLocationException
    {
        List servicesToRegister = services.match(null, da.getScopes(), null, null);
        for (Iterator serviceInfos = servicesToRegister.iterator(); serviceInfos.hasNext();)
        {
            SAServiceInfo service = (SAServiceInfo)serviceInfos.next();
            registerServiceWithDirectoryAgent(service, da);
            scheduleServiceRenewal(service, da);
        }
    }

    private void registerService(SAServiceInfo service) throws IOException, ServiceLocationException
    {
        List das = findDirectoryAgents(service.getScopes());
        if (!das.isEmpty())
        {
            for (int i = 0; i < das.size(); ++i)
            {
                DirectoryAgentInfo directory = (DirectoryAgentInfo)das.get(i);
                registerServiceWithDirectoryAgent(service, directory);
                scheduleServiceRenewal(service, directory);
            }
        }
        else
        {
            // There are no DA deployed on the network: multicast a SrvReg as specified by RFC 3082.
            manager.multicastSrvRegNotification(service, serviceAgent, true);
            // TODO: periodic multicast of SrvReg for service renewal ?
        }
    }

    private void registerServiceWithDirectoryAgent(SAServiceInfo service, DirectoryAgentInfo da) throws IOException, ServiceLocationException
    {
        InetAddress address = InetAddress.getByName(da.getHost());
        SrvAck srvAck = manager.tcpSrvReg(address, service, serviceAgent, true);
        int errorCode = srvAck.getErrorCode();
        if (errorCode != 0)
            throw new ServiceLocationException("Could not register service " + service.getServiceURL() + " to DirectoryAgent " + address, errorCode);
        if (logger.isLoggable(Level.FINE))
            logger.fine("Registered service " + service.getServiceURL() + " to DirectoryAgent " + address);
    }

    private void scheduleServiceRenewal(SAServiceInfo service, DirectoryAgentInfo da)
    {
        long renewalPeriod = calculateRenewalPeriod(service);
        long renewalDelay = calculateRenewalDelay(service);
        if (renewalPeriod > 0)
        {
            ScheduledFuture renewal = scheduledExecutorService.scheduleWithFixedDelay(new RegistrationRenewal(service, da), renewalDelay, renewalPeriod, TimeUnit.MILLISECONDS);
            service.addRenewal(renewal);
        }
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
        for (Iterator serviceInfos = getServices().iterator(); serviceInfos.hasNext();)
        {
            SAServiceInfo service = (SAServiceInfo)serviceInfos.next();
            deregisterService(service);
        }
    }

    private void deregisterService(SAServiceInfo service) throws IOException, ServiceLocationException
    {
        service.cancelRenewals();

        List das = findDirectoryAgents(service.getScopes());
        if (!das.isEmpty())
        {
            for (int i = 0; i < das.size(); ++i)
            {
                DirectoryAgentInfo directory = (DirectoryAgentInfo)das.get(i);
                deregisterServiceWithDirectoryAgent(service, directory);
            }
        }
        else
        {
            // There are no DA deployed on the network: multicast a SrvDeReg as specified by RFC 3082.
            manager.multicastSrvDeRegNotification(service, serviceAgent);
        }
    }

    private void deregisterServiceWithDirectoryAgent(SAServiceInfo service, DirectoryAgentInfo da) throws IOException, ServiceLocationException
    {
        InetAddress address = InetAddress.getByName(da.getHost());
        SrvAck srvAck = manager.tcpSrvDeReg(address, service, serviceAgent);
        int errorCode = srvAck.getErrorCode();
        if (errorCode != 0)
            throw new ServiceLocationException("Could not deregister service " + service.getServiceURL() + " from DirectoryAgent " + address, errorCode);
        if (logger.isLoggable(Level.FINE))
            logger.fine("Deregistered service " + service.getServiceURL() + " from DirectoryAgent " + address);
    }

    protected List findDirectoryAgents(Scopes scopes) throws IOException, ServiceLocationException
    {
        List das = getCachedDirectoryAgents(scopes);
        if (das.isEmpty())
        {
            das = discoverDirectoryAgents(scopes);
            cacheDirectoryAgents(das);
        }
        return das;
    }

    protected List getCachedDirectoryAgents(Scopes scopes)
    {
        return directoryAgents.getByScopes(scopes);
    }

    private boolean cacheDirectoryAgent(DirectoryAgentInfo info)
    {
        return directoryAgents.add(info);
    }

    private void cacheDirectoryAgents(List infos)
    {
        directoryAgents.addAll(infos);
    }

    private void uncacheDirectoryAgent(DirectoryAgentInfo info)
    {
        directoryAgents.remove(info);
    }

    protected List discoverDirectoryAgents(Scopes scopes) throws IOException
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

    protected void handleMulticastSrvRqst(SrvRqst message, InetSocketAddress address) throws ServiceLocationException
    {
        // Match previous responders
        if (matchPreviousResponders(message)) return;

        // Match scopes
        if (!getScopes().weakMatch(message.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + message + ": no scopes match among SA scopes " + getScopes() + " and message scopes " + message.getScopes());
            return;
        }

        // Match filter (see RFC 2608, 8.6)
        Filter filter = new FilterParser().parse(message.getFilter());
        if (!filter.match(getAttributes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + message + ": filter " + filter + " does not match attributes");
        }

        // Check that's a correct multicast request for this ServiceAgent
        ServiceType serviceType = message.getServiceType();
        if (serviceType.isAbstractType() || !"service-agent".equals(serviceType.getPrincipleTypeName()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + message + ": expected service type 'service-agent', got " + serviceType);
        }

        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        try
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " sending UDP unicast reply to " + address);
            manager.udpSAAdvert(address, getIdentifier(), getScopes(), getAttributes(), new Integer(message.getXID()), message.getLanguage());
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "ServiceAgent " + this + " cannot send reply to " + address, x);
        }
    }

    private boolean matchPreviousResponders(SrvRqst message)
    {
        Set prevResponders = message.getPreviousResponders();
        String responder = localhost.getHostAddress();
        if (prevResponders.contains(responder))
        {
            Collection identifierExtensions = IdentifierExtension.findAll(message.getExtensions());
            if (!identifierExtensions.isEmpty())
            {
                for (Iterator identifiers = identifierExtensions.iterator(); identifiers.hasNext();)
                {
                    IdentifierExtension identifierExtension = (IdentifierExtension)identifiers.next();
                    if (identifierExtension.getIdentifier().equals(getIdentifier()))
                    {
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("ServiceAgent " + this + " dropping message " + message + ": already contains responder " + responder + " with id " + getIdentifier());
                        return true;
                    }
                }
            }
            else
            {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("ServiceAgent " + this + " dropping message " + message + ": already contains responder " + responder);
                return true;
            }
        }
        return false;
    }

    protected void handleMulticastDAAdvert(DAAdvert message, InetSocketAddress address)
    {
        if (!getScopes().weakMatch(message.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + message + ": no scopes match among SA scopes " + getScopes() + " and message scopes " + message.getScopes());
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

    protected void handleTCPSrvRqst(SrvRqst message, Socket socket) throws ServiceLocationException
    {
        if (logger.isLoggable(Level.FINE))
            logger.fine("ServiceAgent " + this + " queried via TCP for services of type " + message.getServiceType());

        List matchingServices = matchServices(message.getServiceType(), message.getScopes(), message.getFilter(), message.getLanguage());

        try
        {
            manager.tcpSrvRply(socket, new Integer(message.getXID()), message.getLanguage(), matchingServices);
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " returned " + matchingServices.size() + " services of type " + message.getServiceType());
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "ServiceAgent " + this + " cannot send TCP unicast reply to " + socket, x);
        }
    }

    private List matchServices(ServiceType serviceType, Scopes scopes, String filter, String language) throws ServiceLocationException
    {
        return services.match(serviceType, scopes, new FilterParser().parse(filter), language);
    }

    /**
     * ServiceAgents listen for multicast messages that may arrive.
     * They are interested in:
     * <ul>
     * <li>SrvRqst with service type 'service-agent', from UAs that want to discover ServiceURLs in absence of DAs; the reply is a SAAdvert</li>
     * <li>DAAdverts, from DAs that boot or shutdown; no reply, just update of internal caches</li>
     * </ul>
     */
    private class UDPMessageListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            InetSocketAddress address = event.getSocketAddress();
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("ServiceAgent UDP message listener received message " + message);

                if (!message.isMulticast())
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("ServiceAgent " + this + " dropping message " + message + ": expected multicast flag set");
                    return;
                }

                switch (message.getMessageType())
                {
                    case Message.SRV_RQST_TYPE:
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
    private class TCPMessageListener implements MessageListener
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
                        handleTCPSrvRqst((SrvRqst)message, (Socket)event.getSource());
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
        private final SAServiceInfo service;
        private final DirectoryAgentInfo directory;

        public RegistrationRenewal(SAServiceInfo service, DirectoryAgentInfo directory)
        {
            this.service = service;
            this.directory = directory;
        }

        public void run()
        {
            try
            {
                registerServiceWithDirectoryAgent(service, directory);
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

    private class SAServiceInfo extends ServiceInfo
    {
        private List renewals = new ArrayList();

        private SAServiceInfo(ServiceInfo serviceInfo)
        {
            super(serviceInfo.getServiceType(), serviceInfo.getServiceURL(), serviceInfo.getScopes(), serviceInfo.getAttributes(), serviceInfo.getLanguage());
        }

        protected ServiceInfo clone(ServiceType serviceType, ServiceURL serviceURL, Scopes scopes, Attributes attributes, String language)
        {
            ServiceInfo clone = super.clone(serviceType, serviceURL, scopes, attributes, language);
            SAServiceInfo result = new SAServiceInfo(clone);
            result.renewals.addAll(renewals);
            renewals.clear();
            return result;
        }

        private void cancelRenewals()
        {
            for (int i = 0; i < renewals.size(); ++i)
            {
                ScheduledFuture renewal = (ScheduledFuture)renewals.get(i);
                renewal.cancel(false);
            }
            renewals.clear();
        }

        public void addRenewal(ScheduledFuture renewal)
        {
            renewals.add(renewal);
        }
    }
}
