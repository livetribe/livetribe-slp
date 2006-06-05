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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.ServiceRegistrationListener;
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
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;

/**
 * @version $Rev$ $Date$
 */
public class StandardDirectoryAgent extends StandardAgent implements DirectoryAgent
{
    private DirectoryAgentManager manager;
    private int heartBeat;
    private InetAddress address;
    private ScheduledExecutorService scheduledExecutorService;
    private long bootTime;
    private InetAddress localhost;
    private MessageListener udpListener;
    private MessageListener tcpListener;
    private final ServiceInfoCache services = new ServiceInfoCache();
    private final List listeners = new LinkedList();
    private final Lock listenersLock = new ReentrantLock();

    public void setDirectoryAgentManager(DirectoryAgentManager manager)
    {
        this.manager = manager;
    }

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        setHeartBeat(configuration.getDAHeartBeatPeriod());
        if (manager != null) manager.setConfiguration(configuration);
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService)
    {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public int getHeartBeat()
    {
        return heartBeat;
    }

    public void setHeartBeat(int heartBeat)
    {
        this.heartBeat = heartBeat;
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

        udpListener = new UDPMessageListener();
        tcpListener = new TCPMessageListener();
        manager.addMessageListener(udpListener, true);
        manager.addMessageListener(tcpListener, false);

        // DirectoryAgents send unsolicited DAAdverts every heartBeat seconds (RFC 2608, 12.2)
        if (scheduledExecutorService == null) scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(new UnsolicitedDAAdvert(), 0L, getHeartBeat() * 1000L, TimeUnit.MILLISECONDS);
    }

    protected DirectoryAgentManager createDirectoryAgentManager()
    {
        return new StandardDirectoryAgentManager();
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

    public void addServiceRegistrationListener(ServiceRegistrationListener listener)
    {
        listenersLock.lock();
        try
        {
            listeners.add(listener);
        }
        finally
        {
            listenersLock.unlock();
        }
    }

    public void removeServiceRegistrationListener(ServiceRegistrationListener listener)
    {
        listenersLock.lock();
        try
        {
            listeners.remove(listener);
        }
        finally
        {
            listenersLock.unlock();
        }
    }

    private void notifyServiceRegistered(SrvReg message)
    {
/*
        listenersLock.lock();
        try
        {
            if (!listeners.isEmpty())
            {
                ServiceRegistrationEvent event = new ServiceRegistrationEvent(message, message.getServiceType(), message.getURLEntry().toServiceURL(), message.getScopes(), message.getAttributes());
                for (int i = 0; i < listeners.size(); ++i)
                {
                    ServiceRegistrationListener listener = (ServiceRegistrationListener)listeners.get(i);
                    try
                    {
                        listener.serviceRegistered(event);
                    }
                    catch (RuntimeException x)
                    {
                        if (logger.isLoggable(Level.INFO))
                            logger.log(Level.INFO, "ServiceRegistrationListener threw exception, ignoring", x);
                    }
                }
            }
        }
        finally
        {
            listenersLock.unlock();
        }
*/
    }

    private void notifyServiceDeregistered(SrvReg message)
    {
/*
        listenersLock.lock();
        try
        {
            if (!listeners.isEmpty())
            {
                ServiceRegistrationEvent event = new ServiceRegistrationEvent(message, message.getServiceType(), message.getURLEntry().toServiceURL(), message.getScopes(), message.getAttributes());
                for (int i = 0; i < listeners.size(); ++i)
                {
                    ServiceRegistrationListener listener = (ServiceRegistrationListener)listeners.get(i);
                    try
                    {
                        listener.serviceDeregistered(event);
                    }
                    catch (RuntimeException x)
                    {
                        if (logger.isLoggable(Level.INFO))
                            logger.log(Level.INFO, "ServiceRegistrationListener threw exception, ignoring", x);
                    }
                }
            }
        }
        finally
        {
            listenersLock.unlock();
        }
*/
    }

    public void registerService(ServiceType serviceType, ServiceURL serviceURL, Scopes scopes, Attributes attributes, String language, boolean notifyListeners) throws ServiceLocationException
    {
        SrvReg message = new SrvReg();
        message.setServiceType(serviceType);
        URLEntry urlEntry = new URLEntry();
        urlEntry.setURL(serviceURL.getURL());
        urlEntry.setLifetime(serviceURL.getLifetime());
        message.setURLEntry(urlEntry);
        message.setScopes(scopes);
        message.setAttributes(attributes);
        message.setLanguage(language);
        int result = registerService(message, notifyListeners);
        if (result != 0) throw new ServiceLocationException(result);
    }

    protected void handleMulticastSrvRqst(SrvRqst message, InetSocketAddress address)
    {
        ServiceType serviceType = message.getServiceType();
        if (serviceType.isAbstractType() || !"directory-agent".equals(serviceType.getPrincipleTypeName()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + message + ": expected service type 'directory-agent', got " + serviceType);
            return;
        }

        if (!getScopes().match(message.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + message + ": no scopes match among DA scopes " + getScopes() + " and message scopes " + message.getScopes());
            return;
        }

        Set prevResponders = message.getPreviousResponders();
        String responder = localhost.getHostAddress();
        if (prevResponders.contains(responder))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + message + ": already contains responder " + responder);
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

    protected void handleTCPSrvReg(SrvReg message, Socket socket)
    {
        // TODO: check that the scopes match

        int errorCode = message.isFresh() ? registerService(message, true) : updateService(message, true);
        try
        {
            manager.tcpSrvAck(socket, new Integer(message.getXID()), message.getLanguage(), errorCode);
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send unicast reply to " + socket, x);
        }
    }

    private int registerService(SrvReg message, boolean notifyListeners)
    {
        // RFC 2608, 7.0
        if (message.getLanguage() == null)
        {
            if (logger.isLoggable(Level.FINE)) logger.fine("Could not register service " + message.getURLEntry().toServiceURL() + ", missing language");
            return ServiceLocationException.INVALID_REGISTRATION;
        }
        if (message.getURLEntry().getLifetime() <= 0)
        {
            if (logger.isLoggable(Level.FINE)) logger.fine("Could not register service " + message.getURLEntry().toServiceURL() + ", invalid lifetime ");
            return ServiceLocationException.INVALID_REGISTRATION;
        }

        services.put(ServiceInfo.from(message));

        // TODO: notify listeners
//        if (notifyListeners) notifyServiceRegistered(message);

        return 0;
    }

    private int updateService(SrvReg message, boolean notifyListeners)
    {
        services.lock();
        try
        {
            ServiceInfo update = ServiceInfo.from(message);
            ServiceInfo existing = services.get(update.getKey());

            // Updating a service that does not exist must fail (RFC 2608, 9.3)
            if (existing == null) return ServiceLocationException.INVALID_UPDATE;

            // Services must be updated keeping the same scopes list (RFC 2608, 9.3)
            if (!existing.getScopes().equals(update.getScopes()))
                return ServiceLocationException.SCOPE_NOT_SUPPORTED;

            services.updateAdd(update);

            return 0;
        }
        finally
        {
            services.unlock();
        }

        // TODO: notify listeners
//        if (notifyListeners) notifyServiceUpdated();
    }

    protected void handleTCPSrvDeReg(SrvDeReg message, Socket socket)
    {
        // TODO: check that the scopes match

        ServiceInfo remove = ServiceInfo.from(message);
        ServiceInfo existing = null;
        ServiceInfo updated = null;

        services.lock();
        try
        {
            existing = services.get(remove.getKey());
            updated = services.remove(remove.getKey());
        }
        finally
        {
            services.unlock();
        }

        // TODO: notify listeners
/*
        if (existing != null)
        {
            if (updated != null)
                notifyServiceUpdated();
            else
                notifyServiceDeregistered();
        }
*/

        int errorCode = 0;
        try
        {
            manager.tcpSrvAck(socket, new Integer(message.getXID()), message.getLanguage(), errorCode);
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send unicast reply to " + socket, x);
        }
    }

    protected void handleTCPSrvRqst(SrvRqst message, Socket socket) throws ServiceLocationException
    {
        if (logger.isLoggable(Level.FINE))
            logger.fine("DirectoryAgent " + this + " queried for services of type " + message.getServiceType());

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

    /**
     * DirectoryAgents listen for multicast UDP messages that may arrive.
     * They are interested in:
     * <ul>
     * <li>SrvRqst, from UAs and SAs that wants to discover DAs; the reply is a DAAdvert</li>
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
