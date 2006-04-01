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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.StandardAgent;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.spi.da.DirectoryAgentManager;
import org.livetribe.slp.spi.da.StandardDirectoryAgentManager;
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
    private Timer timer;
    private long bootTime;
    private InetAddress localhost;
    private MessageListener multicastListener;
    private MessageListener unicastListener;
    private final Lock servicesLock = new ReentrantLock();
    private final Map services = new HashMap();
    private final List listeners = new LinkedList();
    private final Lock listenersLock = new ReentrantLock();

    public void setDirectoryAgentManager(DirectoryAgentManager manager)
    {
        this.manager = manager;
    }

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        setHeartBeat(configuration.getDAHeartBeat());
        if (manager != null) manager.setConfiguration(configuration);
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

        multicastListener = new MulticastMessageListener();
        unicastListener = new UnicastMessageListener();
        manager.addMessageListener(multicastListener, true);
        manager.addMessageListener(unicastListener, false);

        // DirectoryAgents send unsolicited DAAdverts every heartBeat seconds (RFC 2608, 12.2)
        timer = new Timer(true);
        timer.schedule(new UnsolicitedDAAdvert(), 0L, getHeartBeat() * 1000L);
    }

    protected DirectoryAgentManager createDirectoryAgentManager()
    {
        return new StandardDirectoryAgentManager();
    }

    protected void doStop() throws IOException
    {
        timer.cancel();

        // DirectoryAgents send a DAAdvert on shutdown with bootTime == 0 (RFC 2608, 12.1)
        manager.multicastDAAdvert(0, getScopes(), null, null, Locale.getDefault().getCountry());
        manager.removeMessageListener(multicastListener, true);
        manager.removeMessageListener(unicastListener, false);
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

    public void registerService(ServiceType serviceType, ServiceURL serviceURL, String[] scopes, Attributes attributes, String language) throws ServiceLocationException
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
        int result = registerService(message);
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

        List scopesList = Arrays.asList(getScopes());
        List messageScopes = Arrays.asList(message.getScopes());
        if (!scopesList.contains(DEFAULT_SCOPE) && Collections.disjoint(scopesList, messageScopes))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + message + ": no scopes match among DA scopes " + scopesList + " and message scopes " + messageScopes);
            return;
        }

        String[] prevResponders = message.getPreviousResponders();
        String responder = localhost.getHostAddress();
        if (Arrays.asList(prevResponders).contains(responder))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + message + ": already contains responder " + responder);
            return;
        }

        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        try
        {
            try
            {
                // Avoid that unicast replies are sent to this directory agent
                if (!manager.canReplyOnUnicastTo(address.getAddress())) throw new ConnectException();

                if (logger.isLoggable(Level.FINE))
                    logger.fine("DirectoryAgent " + this + " sending unicast reply to " + address.getAddress());
                manager.unicastDAAdvert(address.getAddress(), getBootTime(), getScopes(), null, new Integer(message.getXID()), message.getLanguage());
            }
            catch (ConnectException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("DirectoryAgent " + this + " could not send unicast reply to " + address.getAddress() + ", trying multicast");
                manager.multicastDAAdvert(getBootTime(), getScopes(), null, new Integer(message.getXID()), message.getLanguage());
            }
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send reply to " + address.getAddress(), x);
        }
    }

    protected void handleUnicastSrvReg(SrvReg message, Socket socket)
    {
        int errorCode = 0;
        if (message.isFresh())
        {
            errorCode = registerService(message);
        }
        else
        {
            errorCode = updateService(message);
        }

        try
        {
            manager.unicastSrvAck(socket, new Integer(message.getXID()), message.getLanguage(), errorCode);
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send unicast reply to " + socket, x);
        }
    }

    private int registerService(SrvReg message)
    {
        int result = 0;
        servicesLock.lock();
        try
        {
            Map serviceURLs = (Map)services.get(message.getServiceType());
            if (serviceURLs == null)
            {
                serviceURLs = new HashMap();
                services.put(message.getServiceType(), serviceURLs);
            }

            Object oldRegistration = serviceURLs.put(message.getURLEntry().getURL(), message);
            if (oldRegistration != null)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Replacing service registration " + oldRegistration + " with new registration " + message);
            }
            else
            {
                if (logger.isLoggable(Level.FINE)) logger.fine("Registering new service " + message);
            }
        }
        finally
        {
            servicesLock.unlock();
        }

        notifyServiceRegistered(message);

        return result;
    }

    private void notifyServiceRegistered(SrvReg message)
    {
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
    }

    private int updateService(SrvReg message)
    {
        servicesLock.lock();
        try
        {
            Map serviceURLs = (Map)services.get(message.getServiceType());
            if (serviceURLs == null)
            {
                serviceURLs = new HashMap();
                services.put(message.getServiceType(), serviceURLs);
            }

            SrvReg registration = (SrvReg)serviceURLs.get(message.getURLEntry().getURL());
            // Updating a service that does not exist must fail (RFC 2608, 9.3)
            if (registration == null) return ServiceLocationException.INVALID_UPDATE;
            // Services must be updated keeping the same scopes list (RFC 2608, 9.3)
            if (!Arrays.equals(registration.getScopes(), message.getScopes()))
                return ServiceLocationException.SCOPE_NOT_SUPPORTED;

            registration.updateAttributes(message.getAttributes());
            return 0;
        }
        finally
        {
            servicesLock.unlock();
        }
    }

    protected void handleUnicastSrvDeReg(SrvDeReg message, Socket socket)
    {
        int errorCode = 0;

        servicesLock.lock();
        try
        {
            for (Iterator servicesIterator = services.entrySet().iterator(); servicesIterator.hasNext();)
            {
                Map.Entry serviceEntry = (Map.Entry)servicesIterator.next();
                Map serviceURLs = (Map)serviceEntry.getValue();

                String serviceURL = message.getURLEntry().getURL();
                SrvReg registration = (SrvReg)serviceURLs.get(serviceURL);
                if (registration != null)
                {
                    Attributes tags = message.getTags();
                    if (tags != null && !tags.isEmpty())
                    {
                        registration.removeAttributes(tags);
                    }
                    else
                    {
                        serviceURLs.remove(serviceURL);
                        if (serviceURLs.isEmpty()) servicesIterator.remove();
                        notifyServiceDeregistered(registration);
                    }
                }
            }
        }
        finally
        {
            servicesLock.unlock();
        }

        try
        {
            manager.unicastSrvAck(socket, new Integer(message.getXID()), message.getLanguage(), errorCode);
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "DirectoryAgent " + this + " cannot send unicast reply to " + socket, x);
        }
    }

    private void notifyServiceDeregistered(SrvReg message)
    {
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
    }

    protected void handleUnicastSrvRqst(SrvRqst message, Socket socket)
    {
        if (logger.isLoggable(Level.FINE))
            logger.fine("DirectoryAgent " + this + " queried for services of type " + message.getServiceType());

        List matchingServices = matchServices(message.getServiceType());
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

    private List matchServices(ServiceType serviceType)
    {
        servicesLock.lock();
        try
        {
            List result = new ArrayList();
            for (Iterator entries = services.entrySet().iterator(); entries.hasNext();)
            {
                Map.Entry entry = (Map.Entry)entries.next();
                ServiceType registeredServiceType = (ServiceType)entry.getKey();
                Map serviceURLs = (Map)entry.getValue();
                if (registeredServiceType.matches(serviceType))
                {
                    for (Iterator urls = serviceURLs.entrySet().iterator(); urls.hasNext();)
                    {
                        Map.Entry urlEntry = (Map.Entry)urls.next();
                        String serviceURL = (String)urlEntry.getKey();
                        SrvReg registeredService = (SrvReg)urlEntry.getValue();
                        result.add(new ServiceURL(serviceURL, registeredService.getURLEntry().getLifetime()));
                    }
                }
            }
            return result;
        }
        finally
        {
            servicesLock.unlock();
        }
    }

    private class UnsolicitedDAAdvert extends TimerTask
    {
        public void run()
        {
            try
            {
                manager.multicastDAAdvert(getBootTime(), getScopes(), null, new Integer(0), Locale.getDefault().getCountry());
            }
            catch (IOException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "DirectoryAgent " + this + " cannot send unsolicited DAAdvert", x);
            }
        }
    }

    /**
     * DirectoryAgents listen for multicast messages that may arrive.
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
                    logger.finest("DirectoryAgent multicast message listener received message " + message);

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
     * <li>SrvRqst, from UAs and SAs that wants register a ServiceURL; the reply is a SrvAck</li>
     * <li>SrvReg, from SAs that wants to register a ServiceURL; the reply is a SrvAck</li>
     * <li>SrvDeReg, from SAs that wants to unregister a ServiceURL; the reply is a SrvAck</li>
     * </ul>
     */
    private class UnicastMessageListener implements MessageListener
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
                        handleUnicastSrvRqst((SrvRqst)message, (Socket)event.getSource());
                        break;

                    case Message.SRV_REG_TYPE:
                        handleUnicastSrvReg((SrvReg)message, (Socket)event.getSource());
                        break;

                    case Message.SRV_DEREG_TYPE:
                        handleUnicastSrvDeReg((SrvDeReg)message, (Socket)event.getSource());
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
