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
package org.livetribe.slp.da;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPError;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.sa.ServiceListener;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Factories;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.PropertiesSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.AbstractServer;
import org.livetribe.slp.spi.Server;
import org.livetribe.slp.spi.ServiceInfoCache;
import org.livetribe.slp.spi.TCPAttrRplyPerformer;
import org.livetribe.slp.spi.TCPSrvAckPerformer;
import org.livetribe.slp.spi.UDPSrvAckPerformer;
import org.livetribe.slp.spi.da.MulticastDAAdvertPerformer;
import org.livetribe.slp.spi.da.TCPSrvRplyPerformer;
import org.livetribe.slp.spi.da.UDPDAAdvertPerformer;
import org.livetribe.slp.spi.da.UDPSrvRplyPerformer;
import org.livetribe.slp.spi.filter.FilterParser;
import org.livetribe.slp.spi.msg.AttrRqst;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.net.NetUtils;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.TCPConnectorServer;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.net.UDPConnectorServer;


/**
 * Implementation of an SLP directory agent standalone server that can be started as a service in a host.
 * <br />
 * Only one instance of this server can be started per each host, as it listens on the SLP TCP port.
 * In SLP, directory agents work as cache of services and allow to reduce the network utilization since
 * both user agents and service agents will prefer a direct tcp connection with the directory agent over the
 * use of multicast.
 *
 * @version $Rev$ $Date$
 */
public class StandardDirectoryAgentServer extends AbstractServer
{
    /**
     * Main method to start this directory agent.
     * <br />
     * It accepts a single program argument, the file path of the configuration file that overrides the
     * defaults for this directory agent
     *
     * @param args the program arguments
     * @throws IOException in case the configuration file cannot be read
     */
    public static void main(String[] args) throws IOException
    {
        Settings settings = null;
        if (args.length > 0) settings = PropertiesSettings.from(new File(args[0]));
        Server server = newInstance(settings);
        server.start();
    }

    /**
     * @param settings the configuration settings that override the defaults
     * @return a new instance of this directory agent
     */
    public static StandardDirectoryAgentServer newInstance(Settings settings)
    {
        UDPConnector udpConnector = Factories.<UDPConnector.Factory>newInstance(settings, UDP_CONNECTOR_FACTORY_KEY).newUDPConnector(settings);
        TCPConnector tcpConnector = Factories.<TCPConnector.Factory>newInstance(settings, TCP_CONNECTOR_FACTORY_KEY).newTCPConnector(settings);
        UDPConnectorServer udpConnectorServer = Factories.<UDPConnectorServer.Factory>newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY).newUDPConnectorServer(settings);
        TCPConnectorServer tcpConnectorServer = Factories.<TCPConnectorServer.Factory>newInstance(settings, TCP_CONNECTOR_SERVER_FACTORY_KEY).newTCPConnectorServer(settings);
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        return new StandardDirectoryAgentServer(udpConnector, tcpConnector, udpConnectorServer, tcpConnectorServer, scheduledExecutorService, settings);
    }

    private final ServiceInfoCache<ServiceInfo> services = new ServiceInfoCache<ServiceInfo>();
    private final MessageListener tcpListener = new TCPMessageListener();
    private final MessageListener udpListener = new UDPMessageListener();
    private final Map<String, DirectoryAgentInfo> directoryAgents = new HashMap<String, DirectoryAgentInfo>();
    private final UDPConnectorServer udpConnectorServer;
    private final TCPConnectorServer tcpConnectorServer;
    private final ScheduledExecutorService scheduledExecutorService;
    private final MulticastDAAdvertPerformer multicastDAAdvert;
    private final UDPDAAdvertPerformer udpDAAdvert;
    private final UDPSrvRplyPerformer udpSrvRply;
    private final TCPSrvRplyPerformer tcpSrvRply;
    private final UDPSrvAckPerformer udpSrvAck;
    private final TCPSrvAckPerformer tcpSrvAck;
    private final TCPAttrRplyPerformer tcpAttrRply;
    private String[] addresses = Defaults.get(ADDRESSES_KEY);
    private int port = Defaults.get(PORT_KEY);
    private Scopes scopes = Scopes.from(Defaults.get(SCOPES_KEY));
    private Attributes attributes = Attributes.from(Defaults.get(DA_ATTRIBUTES_KEY));
    private String language = Defaults.get(LANGUAGE_KEY);
    private int advertisementPeriod = Defaults.get(DA_ADVERTISEMENT_PERIOD_KEY);
    private int expiredServicesPurgePeriod = Defaults.get(DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY);

    /**
     * Creates a new StandardDirectoryAgentServer using the default settings
     *
     * @param udpConnector             the connector that handles udp traffic
     * @param tcpConnector             the connector that handles tcp traffic
     * @param udpConnectorServer       the connector that listens for udp traffic
     * @param tcpConnectorServer       the connector that listens for tcp traffic
     * @param scheduledExecutorService the periodic task scheduler for this directory agent
     * @see org.livetribe.slp.settings.Defaults
     */
    public StandardDirectoryAgentServer(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, TCPConnectorServer tcpConnectorServer, ScheduledExecutorService scheduledExecutorService)
    {
        this(udpConnector, tcpConnector, udpConnectorServer, tcpConnectorServer, scheduledExecutorService, null);
    }

    /**
     * Creates a new StandardDirectoryAgentServer
     *
     * @param udpConnector             the connector that handles udp traffic
     * @param tcpConnector             the connector that handles tcp traffic
     * @param udpConnectorServer       the connector that listens for udp traffic
     * @param tcpConnectorServer       the connector that listens for tcp traffic
     * @param scheduledExecutorService the periodic task scheduler for this directory agent
     * @param settings                 the configuration settings that override the defaults
     */
    public StandardDirectoryAgentServer(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, TCPConnectorServer tcpConnectorServer, ScheduledExecutorService scheduledExecutorService, Settings settings)
    {
        this.udpConnectorServer = udpConnectorServer;
        this.tcpConnectorServer = tcpConnectorServer;
        this.scheduledExecutorService = scheduledExecutorService;
        this.multicastDAAdvert = new MulticastDAAdvertPerformer(udpConnector, settings);
        this.udpDAAdvert = new UDPDAAdvertPerformer(udpConnector, settings);
        this.udpSrvRply = new UDPSrvRplyPerformer(udpConnector, settings);
        this.tcpSrvRply = new TCPSrvRplyPerformer(tcpConnector, settings);
        this.udpSrvAck = new UDPSrvAckPerformer(udpConnector, settings);
        this.tcpSrvAck = new TCPSrvAckPerformer(tcpConnector, settings);
        this.tcpAttrRply = new TCPAttrRplyPerformer(tcpConnector, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(ADDRESSES_KEY)) this.addresses = settings.get(ADDRESSES_KEY);
        if (settings.containsKey(PORT_KEY)) this.port = settings.get(PORT_KEY);
        if (settings.containsKey(SCOPES_KEY)) this.scopes = Scopes.from(settings.get(SCOPES_KEY));
        if (settings.containsKey(DA_ATTRIBUTES_KEY)) this.attributes = Attributes.from(settings.get(DA_ATTRIBUTES_KEY));
        if (settings.containsKey(LANGUAGE_KEY)) this.language = settings.get(LANGUAGE_KEY);
        if (settings.containsKey(DA_ADVERTISEMENT_PERIOD_KEY))
            this.advertisementPeriod = settings.get(DA_ADVERTISEMENT_PERIOD_KEY);
        if (settings.containsKey(DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY))
            this.expiredServicesPurgePeriod = settings.get(DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY);
    }

    public String[] getAddresses()
    {
        return addresses;
    }

    /**
     * Sets the interface IP addresses in case of multihomed hosts
     *
     * @param addresses the new interface IP addresses
     */
    public void setAddresses(String[] addresses)
    {
        this.addresses = addresses;
    }

    public int getPort()
    {
        return port;
    }

    /**
     * Sets the SLP port
     *
     * @param port the new SLP port
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    public Scopes getScopes()
    {
        return scopes;
    }

    /**
     * Sets the Scopes of this directory agent
     *
     * @param scopes the new Scopes
     */
    public void setScopes(Scopes scopes)
    {
        this.scopes = scopes;
    }

    public Attributes getAttributes()
    {
        return attributes;
    }

    /**
     * Sets the Attributes of this directory agent
     *
     * @param attributes the new Attributes
     */
    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    public String getLanguage()
    {
        return language;
    }

    /**
     * Set the language of this directory agent
     *
     * @param language the new language
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    public int getAdvertisementPeriod()
    {
        return advertisementPeriod;
    }

    /**
     * Sets the advertisement period, in seconds, between unsolicited DAAdverts
     *
     * @param advertisementPeriod the new advertisement period
     */
    public void setAdvertisementPeriod(int advertisementPeriod)
    {
        this.advertisementPeriod = advertisementPeriod;
    }

    public int getExpiredServicesPurgePeriod()
    {
        return expiredServicesPurgePeriod;
    }

    /**
     * Sets the purge period, in seconds, between purge of expired services
     *
     * @param expiredServicesPurgePeriod the new purge period
     * @see #purgeExpiredServices()
     */
    public void setExpiredServicesPurgePeriod(int expiredServicesPurgePeriod)
    {
        this.expiredServicesPurgePeriod = expiredServicesPurgePeriod;
    }

    /**
     * Adds a service listener that will be notified in case of service addition, update or removal.
     *
     * @param listener the listener to add
     * @see #removeServiceListener(ServiceListener)
     */
    public void addServiceListener(ServiceListener listener)
    {
        services.addServiceListener(listener);
    }

    /**
     * Removes the given service listener.
     *
     * @param listener the listener to remove
     * @see #addServiceListener(ServiceListener)
     */
    public void removeServiceListener(ServiceListener listener)
    {
        services.removeServiceListener(listener);
    }

    /**
     * @return a list of all services present in this directory agent
     */
    public List<ServiceInfo> getServices()
    {
        return matchServices(null, null, null, null);
    }

    protected void doStart()
    {
        // Convert bootTime in seconds, as required by the DAAdvert message
        int bootTime = ((Long)TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())).intValue();
        setAttributes(attributes.union(Attributes.from("(" + DirectoryAgentInfo.TCP_PORT_TAG + "=" + port + ")")));
        for (int i = 0; i < addresses.length; ++i)
            addresses[i] = NetUtils.convertWildcardAddress(NetUtils.getByName(addresses[i])).getHostAddress();
        for (String address : addresses)
            directoryAgents.put(address, DirectoryAgentInfo.from(address, scopes, attributes, language, bootTime));
        // Add loopback address
        String loopbackAddress = NetUtils.getLoopbackAddress().getHostAddress();
        directoryAgents.put(loopbackAddress, DirectoryAgentInfo.from(loopbackAddress, scopes, attributes, language, bootTime));

        udpConnectorServer.addMessageListener(udpListener);
        udpConnectorServer.start();

        tcpConnectorServer.addMessageListener(tcpListener);
        tcpConnectorServer.start();

        if (expiredServicesPurgePeriod > 0)
            scheduledExecutorService.scheduleWithFixedDelay(new ServicesPurger(), expiredServicesPurgePeriod, expiredServicesPurgePeriod, TimeUnit.SECONDS);

        // Directory agent send a DAAdvert on boot (RFC 2608, 12.1)
        if (logger.isLoggable(Level.FINEST))
            logger.finest("DirectoryAgent " + StandardDirectoryAgentServer.this + " sending boot up DAAdverts: " + directoryAgents);
        multicastDAAdvert.perform(directoryAgents.values(), false);

        // Directory agents send unsolicited DAAdverts every advertisementPeriod seconds (RFC 2608, 12.2)
        if (advertisementPeriod > 0)
            scheduledExecutorService.scheduleWithFixedDelay(new UnsolicitedDAAdvert(), advertisementPeriod, advertisementPeriod, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Shutdown());
    }

    protected void doStop()
    {
        scheduledExecutorService.shutdownNow();

        // Directory agents send a DAAdvert on shutdown (RFC 2608, 12.1)
        if (logger.isLoggable(Level.FINEST))
            logger.finest("DirectoryAgent " + StandardDirectoryAgentServer.this + " sending shut down DAAdvert");
        multicastDAAdvert.perform(directoryAgents.values(), true);

        tcpConnectorServer.removeMessageListener(tcpListener);
        tcpConnectorServer.stop();

        udpConnectorServer.removeMessageListener(udpListener);
        udpConnectorServer.stop();
    }

    /**
     * Handles a UDP SrvRqst message arrived to this directory agent.
     * <br />
     * If the SrvRqst messages has the {@link DirectoryAgentInfo#SERVICE_TYPE directory agent service type}, then
     * the reply is a DAAdvert message, otherwise it is a SrvRply message.
     *
     * @param srvRqst       the SrvRqst message to handle
     * @param localAddress  the address on this server on which the message arrived
     * @param remoteAddress the address on the remote client from which the message was sent
     * @see #handleTCPSrvRqst(SrvRqst, Socket)
     * @see #matchServices(ServiceType, Scopes, String, String)
     */
    protected void handleUDPSrvRqst(SrvRqst srvRqst, InetSocketAddress localAddress, InetSocketAddress remoteAddress)
    {
        // Match previous responders in case of multicast request
        String responder = remoteAddress.getAddress().getHostAddress();
        if (srvRqst.containsResponder(responder))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + srvRqst + ": already contains responder " + responder);
            return;
        }

        // Match scopes
        if (!scopes.weakMatch(srvRqst.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + srvRqst + ": no scopes match among DA scopes " + scopes + " and message scopes " + srvRqst.getScopes());
            return;
        }

        ServiceType serviceType = srvRqst.getServiceType();
        if (DirectoryAgentInfo.SERVICE_TYPE.equals(serviceType))
        {
            String address = NetUtils.convertWildcardAddress(localAddress.getAddress()).getHostAddress();
            DirectoryAgentInfo directoryAgent = directoryAgents.get(address);
            if (directoryAgent == null)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("DirectoryAgent " + this + " dropping message " + srvRqst + ": arrived to unknown address " + address);
                return;
            }

            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " sending UDP unicast reply to " + remoteAddress);
            udpDAAdvert.perform(localAddress, remoteAddress, directoryAgent, srvRqst);
        }
        else
        {
            if (srvRqst.isMulticast())
            {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("DirectoryAgent " + this + " dropping message " + srvRqst + ": SrvRqst for service type " + srvRqst.getServiceType() + " must be unicast");
                return;
            }

            List<ServiceInfo> matchingServices = matchServices(srvRqst.getServiceType(), srvRqst.getLanguage(), srvRqst.getScopes(), srvRqst.getFilter());
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " returning " + matchingServices.size() + " services of type " + srvRqst.getServiceType());
            udpSrvRply.perform(localAddress, remoteAddress, srvRqst, matchingServices);
        }
    }

    /**
     * Handles a unicast TCP SrvRqst message arrived to this directory agent.
     * <br />
     * This directory agent will reply with a list of matching services.
     *
     * @param srvRqst the SrvRqst message to handle
     * @param socket  the socket connected to th client where to write the reply
     * @see #handleUDPSrvRqst(SrvRqst, InetSocketAddress, InetSocketAddress)
     * @see #matchServices(ServiceType, Scopes, String, String)
     */
    protected void handleTCPSrvRqst(SrvRqst srvRqst, Socket socket)
    {
        // Match scopes
        if (!scopes.weakMatch(srvRqst.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + srvRqst + ": no scopes match among DA scopes " + scopes + " and message scopes " + srvRqst.getScopes());
            return;
        }

        List<ServiceInfo> matchingServices = matchServices(srvRqst.getServiceType(), srvRqst.getLanguage(), srvRqst.getScopes(), srvRqst.getFilter());
        tcpSrvRply.perform(socket, srvRqst, matchingServices);
        if (logger.isLoggable(Level.FINE))
            logger.fine("DirectoryAgent " + this + " returning " + matchingServices.size() + " services of type " + srvRqst.getServiceType());
    }

    /**
     * Matches the services of this directory agent against the given arguments.
     *
     * @param serviceType the service type to match or null to match any service type
     * @param scopes      the Scopes to match or null to match any Scopes
     * @param filter      the LDAPv3 filter to match the service Attributes against or null to match any Attributes
     * @param language    the language to match or null to match any language
     * @return a list of matching services
     */
    protected List<ServiceInfo> matchServices(ServiceType serviceType, String language, Scopes scopes, String filter)
    {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("DirectoryAgent " + this + " matching ServiceType " + serviceType + ", language " + language + ", scopes " + scopes + ", filter " + filter);
        return services.match(serviceType, language, scopes, new FilterParser().parse(filter));
    }

    /**
     * Handles unicast or multicast UDP SrvReg message arrived to this directory agent.
     * <br />
     * This directory agent will reply with an acknowledge containing the result of the registration.
     *
     * @param srvReg        the SrvReg message to handle
     * @param localAddress  the socket address the message arrived to
     * @param remoteAddress the socket address the message was sent from
     */
    protected void handleUDPSrvReg(SrvReg srvReg, InetSocketAddress localAddress, InetSocketAddress remoteAddress)
    {
        try
        {
            boolean update = srvReg.isUpdating();
            ServiceInfo service = ServiceInfo.from(srvReg);
            cacheService(service, update);
            udpSrvAck.perform(localAddress, remoteAddress, srvReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            udpSrvAck.perform(localAddress, remoteAddress, srvReg, x.getSLPError().getCode());
        }
    }

    /**
     * Handles a unicast TCP SrvReg message arrived to this directory agent.
     * <br />
     * This directory agent will reply with an acknowledge containing the result of the registration.
     *
     * @param srvReg the SrvReg message to handle
     * @param socket the socket connected to th client where to write the reply
     */
    protected void handleTCPSrvReg(SrvReg srvReg, Socket socket)
    {
        try
        {
            boolean update = srvReg.isUpdating();
            ServiceInfo service = ServiceInfo.from(srvReg);
            cacheService(service, update);
            tcpSrvAck.perform(socket, srvReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            tcpSrvAck.perform(socket, srvReg, x.getSLPError().getCode());
        }
    }

    /**
     * Handles unicast or multicast UDP SrvDeReg message arrived to this directory agent.
     * <br />
     * This directory agent will reply with an acknowledge containing the result of the deregistration.
     *
     * @param srvDeReg      the SrvDeReg message to handle
     * @param localAddress  the socket address the message arrived to
     * @param remoteAddress the socket address the message was sent from
     */
    protected void handleUDPSrvDeReg(SrvDeReg srvDeReg, InetSocketAddress localAddress, InetSocketAddress remoteAddress)
    {
        try
        {
            boolean update = srvDeReg.isUpdating();
            ServiceInfo service = ServiceInfo.from(srvDeReg);
            uncacheService(service, update);
            udpSrvAck.perform(localAddress, remoteAddress, srvDeReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            udpSrvAck.perform(localAddress, remoteAddress, srvDeReg, x.getSLPError().getCode());
        }
    }

    /**
     * Handles a unicast TCP SrvDeReg message arrived to this directory agent.
     * <br />
     * This directory agent will reply with an acknowledge containing the result of the deregistration.
     *
     * @param srvDeReg the SrvDeReg message to handle
     * @param socket   the socket connected to the client where to write the reply
     */
    protected void handleTCPSrvDeReg(SrvDeReg srvDeReg, Socket socket)
    {
        try
        {
            boolean update = srvDeReg.isUpdating();
            ServiceInfo service = ServiceInfo.from(srvDeReg);
            uncacheService(service, update);
            tcpSrvAck.perform(socket, srvDeReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            tcpSrvAck.perform(socket, srvDeReg, x.getSLPError().getCode());
        }
    }

    /**
     * Handles a unicast TCP AttrRqst message arrived to this directory agent.
     * <br />
     * This directory agent will reply with an AttrRply containing the result of the attribute request.
     *
     * @param attrRqst the AttrRqst message to handle
     * @param socket   the socket connected to the client where to write the reply
     */
    protected void handleTCPAttrRqst(AttrRqst attrRqst, Socket socket)
    {
        // Match scopes
        if (!scopes.weakMatch(attrRqst.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + attrRqst + ": no scopes match among DA scopes " + scopes + " and message scopes " + attrRqst.getScopes());
            return;
        }

        boolean isForServiceType = attrRqst.isForServiceType();
        ServiceURL serviceURL = null;
        ServiceType serviceType = null;
        if (isForServiceType)
        {
            serviceURL = null;
            serviceType = new ServiceType(attrRqst.getURL());
        }
        else
        {
            serviceURL = new ServiceURL(attrRqst.getURL());
            serviceType = serviceURL.getServiceType();
        }
        List<ServiceInfo> services = matchServices(serviceType, attrRqst.getLanguage(), attrRqst.getScopes(), null);

        Attributes attributes = Attributes.NONE;
        for (ServiceInfo service : services)
        {
            if (isForServiceType)
                attributes = attributes.merge(service.getAttributes());
            else if (service.getServiceURL().equals(serviceURL))
                attributes = attributes.merge(service.getAttributes());
        }
        Attributes tags = attrRqst.getTags();
        if (!tags.isEmpty()) attributes = attributes.intersect(attrRqst.getTags());

        tcpAttrRply.perform(socket, attrRqst, attributes);
        if (logger.isLoggable(Level.FINE))
            logger.fine("DirectoryAgent " + this + " returning attributes for service " + attrRqst.getURL() + ": " + attributes.asString());
    }

    /**
     * Replaces or updates a previously cached service (if any) with the given service.
     *
     * @param service the new service
     * @param update  whether the given service replaces or updates a previously cached service
     * @return a structure containing the previous service (if any) and the current service
     */
    protected ServiceInfoCache.Result<ServiceInfo> cacheService(ServiceInfo service, boolean update)
    {
        // RFC 2608, 7.0
        if (!scopes.match(service.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Could not register service " + service + ", DirectoryAgent scopes " + scopes + " do not match with service scopes " + service.getScopes());
            throw new ServiceLocationException("Could not register service " + service, SLPError.SCOPE_NOT_SUPPORTED);
        }

        if (update)
        {
            ServiceInfoCache.Result<ServiceInfo> result = services.addAttributes(service.getKey(), service.getAttributes());
            if (logger.isLoggable(Level.FINE))
                logger.fine("Added attributes " + service + " to service " + result.getPrevious() + ", result is: " + result.getCurrent());
            return result;
        }
        else
        {
            ServiceInfoCache.Result<ServiceInfo> result = services.put(service);
            if (logger.isLoggable(Level.FINE))
            {
                if (result.getPrevious() == null)
                    logger.fine("Registered service " + service);
                else
                    logger.fine("Replaced service " + result.getPrevious() + " with service " + service);
            }
            return result;
        }
    }

    /**
     * Removes or updates a previously cached service (if any) with the given service.
     *
     * @param service the new service
     * @param update  whether the given service removes or updates a previously cached service
     * @return a structure containing the previous service and the current service (if any)
     */
    protected ServiceInfoCache.Result<ServiceInfo> uncacheService(ServiceInfo service, boolean update)
    {
        // RFC 2608, 7.0
        if (!scopes.match(service.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Could not deregister service " + service + ", DirectoryAgent scopes " + scopes + " do not match with service scopes " + service.getScopes());
            throw new ServiceLocationException("Could not deregister service " + service, SLPError.SCOPE_NOT_SUPPORTED);
        }

        if (update)
        {
            ServiceInfoCache.Result<ServiceInfo> result = services.removeAttributes(service.getKey(), service.getAttributes());
            if (logger.isLoggable(Level.FINE))
                logger.fine("Removed attributes " + service + " from service " + result.getPrevious() + ", result is: " + result.getCurrent());
            return result;
        }
        else
        {
            ServiceInfoCache.Result<ServiceInfo> result = services.remove(service.getKey());
            if (logger.isLoggable(Level.FINE))
                logger.fine("Deregistered service " + result.getPrevious());
            return result;
        }
    }

    /**
     * Purge the expired services from the service cache
     *
     * @return the list of purged services
     * @see ServiceInfoCache#purge()
     */
    protected List<ServiceInfo> purgeExpiredServices()
    {
        return services.purge();
    }

    private class ServicesPurger implements Runnable
    {
        public void run()
        {
            if (logger.isLoggable(Level.FINEST))
                logger.finest("DirectoryAgent " + StandardDirectoryAgentServer.this + " purging expired services");
            List<ServiceInfo> result = purgeExpiredServices();
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + StandardDirectoryAgentServer.this + " purged " + result.size() + " expired services: " + result);
        }
    }

    private class UnsolicitedDAAdvert implements Runnable
    {
        public void run()
        {
            if (logger.isLoggable(Level.FINEST))
                logger.finest("DirectoryAgent " + StandardDirectoryAgentServer.this + " sending unsolicited DAAdvert");
            multicastDAAdvert.perform(directoryAgents.values(), false);
        }
    }

    /**
     * Directory agents listen for tcp messages that may arrive.
     */
    private class TCPMessageListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            Message message = event.getMessage();
            if (logger.isLoggable(Level.FINEST))
                logger.finest("DirectoryAgent server message listener received message " + message);

            Socket socket = (Socket)event.getSource();
            switch (message.getMessageType())
            {
                case Message.SRV_RQST_TYPE:
                    handleTCPSrvRqst((SrvRqst)message, socket);
                    break;
                case Message.SRV_REG_TYPE:
                    handleTCPSrvReg((SrvReg)message, socket);
                    break;
                case Message.SRV_DEREG_TYPE:
                    handleTCPSrvDeReg((SrvDeReg)message, socket);
                    break;
                case Message.ATTR_RQST_TYPE:
                    handleTCPAttrRqst((AttrRqst)message, socket);
                    break;
                default:
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("DirectoryAgent " + StandardDirectoryAgentServer.this + " dropping tcp message " + message + ": not handled by DirectoryAgents");
                    break;
            }
        }
    }

    /**
     * Directory agents listen for udp messages that may arrive.
     */
    private class UDPMessageListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            Message message = event.getMessage();
            if (logger.isLoggable(Level.FINEST))
                logger.finest("DirectoryAgent server message listener received message " + message);

            InetSocketAddress localAddress = event.getLocalSocketAddress();
            InetSocketAddress remoteAddress = event.getRemoteSocketAddress();
            switch (message.getMessageType())
            {
                case Message.SRV_RQST_TYPE:
                    handleUDPSrvRqst((SrvRqst)message, localAddress, remoteAddress);
                    break;
                case Message.SRV_REG_TYPE:
                    handleUDPSrvReg((SrvReg)message, localAddress, remoteAddress);
                    break;
                case Message.SRV_DEREG_TYPE:
                    handleUDPSrvDeReg((SrvDeReg)message, localAddress, remoteAddress);
                    break;
                default:
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("DirectoryAgent " + StandardDirectoryAgentServer.this + " dropping udp message " + message + ": not handled by DirectoryAgents");
                    break;
            }
        }
    }

    private class Shutdown extends Thread
    {
        @Override
        public void run()
        {
            StandardDirectoryAgentServer.this.stop();
        }
    }
}
