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
package org.livetribe.slp.api.da;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.StandardAgent;
import org.livetribe.slp.api.sa.ServiceInfo;
import org.livetribe.slp.spi.ServiceInfoCache;
import org.livetribe.slp.spi.da.DirectoryAgentManager;
import org.livetribe.slp.spi.da.StandardDirectoryAgentManager;
import org.livetribe.slp.spi.filter.FilterParser;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;

/**
 * @version $Rev$ $Date$
 */
public class StandardDirectoryAgent extends StandardAgent implements DirectoryAgent
{
    private Attributes attributes;
    private DirectoryAgentManager manager;
    private int heartBeatPeriod;
    private boolean periodicAdvertisement = true;
    private boolean periodicServiceExpiration = true;
    private int serviceExpirationPeriod = 1;
    private InetAddress address;
    private ScheduledExecutorService scheduledExecutorService;
    private long bootTime;
    private InetAddress localhost;
    private MessageListener udpListener;
    private MessageListener tcpListener;
    private final ServiceInfoCache services = new ServiceInfoCache();

    public void setDirectoryAgentManager(DirectoryAgentManager manager)
    {
        this.manager = manager;
    }

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        setHeartBeatPeriod(configuration.getDAHeartBeatPeriod());
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

    public int getHeartBeatPeriod()
    {
        return heartBeatPeriod;
    }

    public void setHeartBeatPeriod(int heartBeatPeriod)
    {
        this.heartBeatPeriod = heartBeatPeriod;
    }

    public boolean isPeriodicAdvertisementEnabled()
    {
        return periodicAdvertisement;
    }

    public void setPeriodicAdvertisementEnabled(boolean periodicAdvertisement)
    {
        this.periodicAdvertisement = periodicAdvertisement;
    }

    public boolean isPeriodicServiceExpirationEnabled()
    {
        return periodicServiceExpiration;
    }

    public void setPeriodicServiceExpirationEnabled(boolean periodicServiceExpiration)
    {
        this.periodicServiceExpiration = periodicServiceExpiration;
    }

    public int getServiceExpirationPeriod()
    {
        return serviceExpirationPeriod;
    }

    public void setServiceExpirationPeriod(int serviceExpirationPeriod)
    {
        this.serviceExpirationPeriod = serviceExpirationPeriod;
    }

    public InetAddress getInetAddress()
    {
        return address;
    }

    public void setInetAddress(InetAddress address)
    {
        this.address = address;
    }

    public long getBootTime()
    {
        return bootTime;
    }

    public Collection getServices()
    {
        return services.getServices();
    }

    protected void doStart() throws IOException
    {
        bootTime = System.currentTimeMillis();

        InetAddress agentAddr = getInetAddress();
        if (agentAddr == null) agentAddr = InetAddress.getLocalHost();
        if (agentAddr.isLoopbackAddress())
        {
            if (logger.isLoggable(Level.WARNING))
                logger.warning("DirectoryAgent " + this + " starting on loopback address; this is normally wrong, check your hosts configuration");
        }
        localhost = agentAddr;

        if (manager == null)
        {
            manager = createDirectoryAgentManager();
            manager.setConfiguration(getConfiguration());
        }
        manager.start();

        udpListener = new MulticastMessageListener();
        tcpListener = new TCPMessageListener();
        manager.addMessageListener(udpListener, true);
        manager.addMessageListener(tcpListener, false);

        if (scheduledExecutorService == null) scheduledExecutorService = createScheduledExecutorService();

        // DirectoryAgents send unsolicited DAAdverts every heartBeat seconds (RFC 2608, 12.2)
        if (isPeriodicAdvertisementEnabled())
            scheduledExecutorService.scheduleWithFixedDelay(new UnsolicitedDAAdvert(), 0L, getHeartBeatPeriod(), TimeUnit.SECONDS);
        if (isPeriodicServiceExpirationEnabled())
            scheduledExecutorService.scheduleWithFixedDelay(new ServiceExpirer(), 0L, getServiceExpirationPeriod(), TimeUnit.SECONDS);
    }

    protected DirectoryAgentManager createDirectoryAgentManager()
    {
        return new StandardDirectoryAgentManager();
    }

    protected ScheduledExecutorService createScheduledExecutorService()
    {
        return Executors.newSingleThreadScheduledExecutor(new ThreadFactory()
        {
            public Thread newThread(Runnable runnable)
            {
                Thread thread = new Thread(runnable, "SLP DirectoryAgent Scheduler");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    protected void doStop() throws IOException
    {
        if (scheduledExecutorService != null)
        {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }

        // DirectoryAgents send a DAAdvert on shutdown with bootTime == 0 (RFC 2608, 12.1)
        manager.multicastDAAdvert(0, getScopes(), null, null, Locale.getDefault().getLanguage());
        manager.removeMessageListener(udpListener, true);
        manager.removeMessageListener(tcpListener, false);
        manager.stop();
    }

    protected void handleMulticastSrvRqst(SrvRqst message, InetSocketAddress address) throws ServiceLocationException
    {
        // Match previous responders
        if (matchPreviousResponders(message)) return;

        // Match scopes
        if (!getScopes().weakMatch(message.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + message + ": no scopes match among DA scopes " + getScopes() + " and message scopes " + message.getScopes());
            return;
        }

        // Check that's a correct multicast request for this DirectoryAgent
        ServiceType serviceType = message.getServiceType();
        if (serviceType.isAbstractType() || !"directory-agent".equals(serviceType.getPrincipleTypeName()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + message + ": expected service type 'directory-agent', got " + serviceType);
            return;
        }

        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        try
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " sending UDP unicast reply to " + address);
            manager.udpDAAdvert(address, getBootTime(), getScopes(), null, new Integer(message.getXID()), message.getLanguage());
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send reply to " + address, x);
        }
    }

    private boolean matchPreviousResponders(SrvRqst message)
    {
        // For now do not support IdentifierExtension, as DA are deployed one per host.

        Set prevResponders = message.getPreviousResponders();
        String responder = localhost.getHostAddress();
        if (prevResponders.contains(responder))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + message + ": already contains responder " + responder);
            return true;
        }
        return false;
    }

    protected void handleTCPSrvReg(SrvReg message, Socket socket)
    {
        ServiceInfo service = ServiceInfo.from(message);
        int errorCode = handleRegistration(service, !message.isFresh());
        try
        {
            manager.tcpSrvAck(socket, new Integer(message.getXID()), message.getLanguage(), errorCode);
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send TCP unicast reply to " + socket, x);
        }
    }

    private int handleRegistration(ServiceInfo service, boolean update)
    {
        // RFC 2608, 7.0
        if (service.getLanguage() == null)
        {
            if (logger.isLoggable(Level.FINE)) logger.fine("Could not register service " + service + ", missing language");
            return ServiceLocationException.INVALID_REGISTRATION;
        }
        if (service.getServiceURL().getLifetime() <= 0)
        {
            if (logger.isLoggable(Level.FINE)) logger.fine("Could not register service " + service + ", invalid lifetime ");
            return ServiceLocationException.INVALID_REGISTRATION;
        }
        if (!getScopes().match(service.getScopes()))
        {
            if (logger.isLoggable(Level.FINE)) logger.fine("Could not register service " + service + ", DirectoryAgent scopes " + getScopes() + " do not match with service scopes " + service.getScopes());
            return ServiceLocationException.SCOPE_NOT_SUPPORTED;
        }

        return update ? updateAddService(service) : registerService(service);
    }

    private int registerService(ServiceInfo service)
    {
        services.put(service);
        return 0;
    }

    private int updateAddService(ServiceInfo service)
    {
        services.lock();
        try
        {
            ServiceInfo existing = services.get(service.getKey());

            // Updating a service that does not exist must fail (RFC 2608, 9.3)
            if (existing == null)
            {
                if (logger.isLoggable(Level.FINE)) logger.fine("Could not update service " + service + ", no existing service found");
                return ServiceLocationException.INVALID_UPDATE;
            }

            // Services must be updated keeping the same scopes list (RFC 2608, 9.3)
            if (!existing.getScopes().equals(service.getScopes()))
            {
                if (logger.isLoggable(Level.FINE)) logger.fine("Could not update service " + service + ", existing scopes " + existing.getScopes() + " do not match update scopes " + service.getScopes());
                return ServiceLocationException.SCOPE_NOT_SUPPORTED;
            }

            services.updateAdd(service);

            return 0;
        }
        finally
        {
            services.unlock();
        }
    }

    protected void handleTCPSrvDeReg(SrvDeReg message, Socket socket)
    {
        ServiceInfo service = ServiceInfo.from(message);
        int errorCode = handleDeregistration(service, service.hasAttributes());
        try
        {
            manager.tcpSrvAck(socket, new Integer(message.getXID()), message.getLanguage(), errorCode);
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send TCP unicast reply to " + socket, x);
        }
    }

    private int handleDeregistration(ServiceInfo service, boolean update)
    {
        services.lock();
        try
        {
            ServiceInfo existing = services.get(service.getKey());
            if (existing == null)
            {
                if (logger.isLoggable(Level.FINE)) logger.fine("Could not find service to deregister " + service);
                // If not updating, the service was not present, so it's "removed" already.
                return update ? ServiceLocationException.INVALID_UPDATE : 0;
            }

            if (!service.getScopes().equals(existing.getScopes()))
            {
                if (logger.isLoggable(Level.FINE)) logger.fine("Could not deregister service " + service + ", existing scopes " + existing.getScopes() + " do not match update scopes " + service.getScopes());
                return ServiceLocationException.SCOPE_NOT_SUPPORTED;
            }

            if (update)
            {
                services.updateRemove(service);
            }
            else
            {
                services.remove(service.getKey());
            }

            return 0;
        }
        finally
        {
            services.unlock();
        }
    }

    protected void handleTCPSrvRqst(SrvRqst message, Socket socket) throws ServiceLocationException
    {
        if (logger.isLoggable(Level.FINE))
            logger.fine("DirectoryAgent " + this + " queried via TCP for services of type " + message.getServiceType());

        List matchingServices = matchServices(message.getServiceType(), message.getScopes(), message.getFilter(), message.getLanguage());

        try
        {
            manager.tcpSrvRply(socket, new Integer(message.getXID()), message.getLanguage(), matchingServices);
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " returned " + matchingServices.size() + " services of type " + message.getServiceType());
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send unicast reply to " + socket, x);
        }
    }

    private List matchServices(ServiceType serviceType, Scopes scopes, String filter, String language) throws ServiceLocationException
    {
        return services.match(serviceType, scopes, new FilterParser().parse(filter), language);
    }

    private class UnsolicitedDAAdvert implements Runnable
    {
        public void run()
        {
            try
            {
                manager.multicastDAAdvert(getBootTime(), getScopes(), null, new Integer(0), Locale.getDefault().getLanguage());
            }
            catch (Exception x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "DirectoryAgent " + this + " cannot send unsolicited DAAdvert", x);
            }
        }
    }

    private class ServiceExpirer implements Runnable
    {
        public void run()
        {
            List result = services.purge();
            if (logger.isLoggable(Level.FINE)) logger.fine("DirectoryAgent " + this + " purged " + result.size() + " expired services: " + result);
        }
    }

    /**
     * DirectoryAgents listen for multicast UDP messages that may arrive.
     * They are interested in:
     * <ul>
     * <li>SrvRqst, from UAs and SAs that wants to discover DAs; the reply is a DAAdvert</li>
     * </ul>
     */
    private class MulticastMessageListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            InetSocketAddress address = event.getSocketAddress();
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("DirectoryAgent UDP message listener received message " + message);

                if (!message.isMulticast())
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("DirectoryAgent " + this + " dropping message " + message + ": expected multicast flag set");
                    return;
                }

                switch (message.getMessageType())
                {
                    case Message.SRV_RQST_TYPE:
                        handleMulticastSrvRqst((SrvRqst)message, address);
                        break;
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("DirectoryAgent " + this + " dropping multicast message " + message + ": not handled by DirectoryAgents");
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "DirectoryAgent " + this + " received bad multicast message from: " + address + ", ignoring", x);
            }
        }
    }

    /**
     * DirectoryAgents listen for unicast messages from UAs and SAs.
     * They are interested in:
     * <ul>
     * <li>SrvRqst, from UAs and SAs that want to find ServiceURLs; the reply is a SrvRply</li>
     * <li>SrvReg, from SAs that wants to register a ServiceURL; the reply is a SrvAck</li>
     * <li>SrvDeReg, from SAs that wants to unregister a ServiceURL; the reply is a SrvAck</li>
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
                    logger.finest("DirectoryAgent unicast message listener received message " + message);

                if (message.isMulticast())
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("DirectoryAgent " + this + " dropping message " + message + ": expected multicast flag unset");
                    return;
                }

                switch (message.getMessageType())
                {
                    case Message.SRV_RQST_TYPE:
                        handleTCPSrvRqst((SrvRqst)message, (Socket)event.getSource());
                        break;

                    case Message.SRV_REG_TYPE:
                        handleTCPSrvReg((SrvReg)message, (Socket)event.getSource());
                        break;

                    case Message.SRV_DEREG_TYPE:
                        handleTCPSrvDeReg((SrvDeReg)message, (Socket)event.getSource());
                        break;

                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("DirectoryAgent " + this + " dropping unicast message " + message + ": not handled by DirectoryAgents");
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "DirectoryAgent " + this + " received bad unicast message from: " + event.getSocketAddress() + ", ignoring", x);
            }
        }
    }
}
