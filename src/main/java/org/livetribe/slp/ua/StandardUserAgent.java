/*
 * Copyright 2005-2008 the original author or authors
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

import java.util.List;
import java.util.logging.Level;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.da.DirectoryAgentListener;
import org.livetribe.slp.sa.ServiceNotificationEvent;
import org.livetribe.slp.sa.ServiceNotificationListener;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Factories;
import org.livetribe.slp.settings.Keys;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.AbstractServer;
import org.livetribe.slp.spi.da.DirectoryAgentInfoCache;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.net.UDPConnectorServer;
import org.livetribe.slp.spi.ua.AbstractUserAgent;
import org.livetribe.util.Listeners;

/**
 * @version $Revision$ $Date$
 */
public class StandardUserAgent extends AbstractUserAgent implements UserAgent
{
    public static StandardUserAgent newInstance(Settings settings)
    {
        UDPConnector.Factory udpFactory = Factories.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        UDPConnector udpConnector = udpFactory.newUDPConnector(settings);
        TCPConnector.Factory tcpFactory = Factories.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        TCPConnector tcpConnector = tcpFactory.newTCPConnector(settings);
        UDPConnectorServer.Factory udpServerFactory = Factories.newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY);
        UDPConnectorServer udpConnectorServer = udpServerFactory.newUDPConnectorServer(settings);
        UDPConnectorServer notificationConnectorServer = udpServerFactory.newNotificationUDPConnectorServer(settings);
        return new StandardUserAgent(udpConnector, tcpConnector, udpConnectorServer, notificationConnectorServer, settings);
    }

    private final UDPConnectorServer udpConnectorServer;
    private final UDPConnectorServer notificationConnectorServer;
    private final UserAgentServer server = new UserAgentServer();
    private final DirectoryAgentInfoCache directoryAgents = new DirectoryAgentInfoCache();
    private final Listeners<ServiceNotificationListener> serviceRegistrationListeners = new Listeners<ServiceNotificationListener>();
    private final MessageListener listener = new UserAgentMessageListener();
    private String[] directoryAgentAddresses = Defaults.get(Keys.DA_ADDRESSES_KEY);

    public StandardUserAgent(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, UDPConnectorServer notificationConnectorServer)
    {
        this(udpConnector, tcpConnector, udpConnectorServer, notificationConnectorServer, null);
    }

    public StandardUserAgent(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, UDPConnectorServer notificationConnectorServer, Settings settings)
    {
        super(udpConnector, tcpConnector, settings);
        this.udpConnectorServer = udpConnectorServer;
        this.notificationConnectorServer = notificationConnectorServer;
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(Keys.DA_ADDRESSES_KEY))
            setDirectoryAgentAddresses(settings.get(Keys.DA_ADDRESSES_KEY));
    }

    public void setDirectoryAgentAddresses(String[] directoryAgentAddresses)
    {
        this.directoryAgentAddresses = directoryAgentAddresses;
    }

    public void start()
    {
        server.start();
    }

    public boolean isRunning()
    {
        return server.isRunning();
    }

    public void stop()
    {
        server.stop();
    }

    protected void doStart()
    {
        udpConnectorServer.addMessageListener(listener);
        udpConnectorServer.start();
        notificationConnectorServer.addMessageListener(listener);
        notificationConnectorServer.start();

        if (directoryAgentAddresses.length > 0)
        {
            for (String daAddress : directoryAgentAddresses) directoryAgents.add(DirectoryAgentInfo.from(daAddress));
        }
        else
        {
            directoryAgents.addAll(discoverDirectoryAgents(null, null));
        }
    }

    protected void doStop()
    {
        directoryAgents.removeAll();

        notificationConnectorServer.stop();
        notificationConnectorServer.removeMessageListener(listener);
        udpConnectorServer.stop();
        udpConnectorServer.removeMessageListener(listener);
    }

    public void addServiceNotificationListener(ServiceNotificationListener listener)
    {
        serviceRegistrationListeners.add(listener);
    }

    public void removeServiceNotificationListener(ServiceNotificationListener listener)
    {
        serviceRegistrationListeners.remove(listener);
    }

    public void addDirectoryAgentListener(DirectoryAgentListener listener)
    {
        directoryAgents.addDirectoryAgentListener(listener);
    }

    public void removeDirectoryAgentListener(DirectoryAgentListener listener)
    {
        directoryAgents.removeDirectoryAgentListener(listener);
    }

    public List<DirectoryAgentInfo> getDirectoryAgents()
    {
        return directoryAgents.match(null, null);
    }

    protected void notifyServiceRegistered(ServiceNotificationEvent event)
    {
        for (ServiceNotificationListener listener : serviceRegistrationListeners) listener.serviceRegistered(event);
    }

    protected void notifyServiceDeregistered(ServiceNotificationEvent event)
    {
        for (ServiceNotificationListener listener : serviceRegistrationListeners) listener.serviceDeregistered(event);
    }

    protected List<DirectoryAgentInfo> findDirectoryAgents(Scopes scopes, Filter filter)
    {
        // This user agent listens DirectoryAgent advertisements so its cache is always up-to-date
        return directoryAgents.match(scopes, filter);
    }

    protected void handleMulticastDAAdvert(DAAdvert daAdvert)
    {
        directoryAgents.handle(DirectoryAgentInfo.from(daAdvert));
    }

    protected void handleMulticastSrvReg(SrvReg srvReg)
    {
        ServiceInfo service = ServiceInfo.from(srvReg);
        ServiceNotificationEvent event = new ServiceNotificationEvent(this, service, srvReg.isUpdating());
        notifyServiceRegistered(event);
    }

    protected void handleMulticastSrvDeReg(SrvDeReg srvDeReg)
    {
        ServiceInfo service = ServiceInfo.from(srvDeReg);
        ServiceNotificationEvent event = new ServiceNotificationEvent(this, service, srvDeReg.isUpdating());
        notifyServiceDeregistered(event);
    }

    private class UserAgentServer extends AbstractServer
    {
        protected void doStart()
        {
            StandardUserAgent.this.doStart();
        }

        protected void doStop()
        {
            StandardUserAgent.this.doStop();
        }
    }

    /**
     * UserAgents listen for multicast messages that may arrive.
     * They are interested in:
     * <ul>
     * <li>Multicast DAAdverts, from DAs (RFC 2608, 3.0)</li>
     * <li>Multicast SrvReg and SrvDeReg, from SAs (RFC 3082, 5.1)</li>
     * </ul>
     */
    private class UserAgentMessageListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            Message message = event.getMessage();
            if (logger.isLoggable(Level.FINEST))
                logger.finest("UserAgent message listener received message " + message);

            if (message.isMulticast())
            {
                switch (message.getMessageType())
                {
                    case Message.DA_ADVERT_TYPE:
                        handleMulticastDAAdvert((DAAdvert)message);
                        break;
                    case Message.SRV_REG_TYPE:
                        handleMulticastSrvReg((SrvReg)message);
                        break;
                    case Message.SRV_DEREG_TYPE:
                        handleMulticastSrvDeReg((SrvDeReg)message);
                        break;
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("UserAgent " + this + " dropping multicast message " + message + ": not handled by UserAgents");
                        break;
                }
            }
            else
            {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("UserAgent " + this + " dropping tcp message " + message + ": not handled by UserAgents");
            }
        }
    }

    public static class Factory implements UserAgent.Factory
    {
        public UserAgent newUserAgent(Settings settings)
        {
            return newInstance(settings);
        }
    }
}
