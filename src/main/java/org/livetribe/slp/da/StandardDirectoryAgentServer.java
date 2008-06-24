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
package org.livetribe.slp.da;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.sa.ServiceListener;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.PropertiesSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.AbstractServer;
import org.livetribe.slp.srv.Factories;
import org.livetribe.slp.srv.Server;
import org.livetribe.slp.srv.ServiceInfoCache;
import org.livetribe.slp.srv.TCPSrvAckPerformer;
import org.livetribe.slp.srv.da.DirectoryAgentInfo;
import org.livetribe.slp.srv.da.MulticastDAAdvertPerformer;
import org.livetribe.slp.srv.da.TCPSrvRplyPerformer;
import org.livetribe.slp.srv.da.UDPDAAdvertPerformer;
import org.livetribe.slp.srv.filter.FilterParser;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.msg.SrvAck;
import org.livetribe.slp.srv.msg.SrvDeReg;
import org.livetribe.slp.srv.msg.SrvReg;
import org.livetribe.slp.srv.msg.SrvRqst;
import org.livetribe.slp.srv.net.MessageEvent;
import org.livetribe.slp.srv.net.MessageListener;
import org.livetribe.slp.srv.net.NetUtils;
import org.livetribe.slp.srv.net.TCPConnector;
import org.livetribe.slp.srv.net.TCPConnectorServer;
import org.livetribe.slp.srv.net.UDPConnector;
import org.livetribe.slp.srv.net.UDPConnectorServer;


/**
 * Implementation of an SLP DirectoryAgent.
 *
 * @version $Rev$ $Date$
 */
public class StandardDirectoryAgentServer extends AbstractServer
{
    public static void main(String[] args) throws Exception
    {
        Settings settings = null;
        if (args.length > 0) settings = PropertiesSettings.from(new File(args[0]));
        Server server = newInstance(settings);
        server.start();
    }

    public static StandardDirectoryAgentServer newInstance(Settings settings)
    {
        UDPConnector.Factory udpFactory = Factories.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        TCPConnector.Factory tcpFactory = Factories.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        UDPConnectorServer.Factory udpServerFactory = Factories.newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY);
        TCPConnectorServer.Factory tcpServerFactory = Factories.newInstance(settings, TCP_CONNECTOR_SERVER_FACTORY_KEY);
        return new StandardDirectoryAgentServer(udpFactory.newUDPConnector(settings), tcpFactory.newTCPConnector(settings), udpServerFactory.newUDPConnectorServer(settings), tcpServerFactory.newTCPConnectorServer(settings), settings);
    }

    private final ServiceInfoCache<ServiceInfo> services = new ServiceInfoCache<ServiceInfo>();
    private final MessageListener listener = new DirectoryAgentMessageListener();
    private final Map<String, DirectoryAgentInfo> directoryAgents = new HashMap<String, DirectoryAgentInfo>();
    private final UDPConnectorServer udpConnectorServer;
    private final TCPConnectorServer tcpConnectorServer;
    private final MulticastDAAdvertPerformer multicastDAAdvert;
    private final UDPDAAdvertPerformer udpDAAdvert;
    private final TCPSrvRplyPerformer tcpSrvRply;
    private final TCPSrvAckPerformer tcpSrvAck;
    private ScheduledExecutorService scheduledExecutorService = Defaults.get(SCHEDULED_EXECUTOR_SERVICE_KEY);
    private String[] addresses = Defaults.get(ADDRESSES_KEY);
    private int port = Defaults.get(PORT_KEY);
    private Scopes scopes = Scopes.from(Defaults.get(SCOPES_KEY));
    private Attributes attributes = Attributes.from(Defaults.get(DA_ATTRIBUTES_KEY));
    private String language = Defaults.get(LANGUAGE_KEY);
    private int advertisementPeriod = Defaults.get(DA_ADVERTISEMENT_PERIOD_KEY);
    private int expiredServicesPurgePeriod = Defaults.get(DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY);

    public StandardDirectoryAgentServer(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, TCPConnectorServer tcpConnectorServer, Settings settings)
    {
        this.udpConnectorServer = udpConnectorServer;
        this.tcpConnectorServer = tcpConnectorServer;
        this.multicastDAAdvert = new MulticastDAAdvertPerformer(udpConnector, settings);
        this.udpDAAdvert = new UDPDAAdvertPerformer(udpConnector, settings);
        this.tcpSrvRply = new TCPSrvRplyPerformer(tcpConnector, settings);
        this.tcpSrvAck = new TCPSrvAckPerformer(tcpConnector, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(SCHEDULED_EXECUTOR_SERVICE_KEY)) setScheduledExecutorService(settings.get(SCHEDULED_EXECUTOR_SERVICE_KEY));
        if (settings.containsKey(ADDRESSES_KEY)) setAddresses(settings.get(ADDRESSES_KEY));
        if (settings.containsKey(PORT_KEY)) setPort(settings.get(PORT_KEY));
        if (settings.containsKey(SCOPES_KEY)) setScopes(Scopes.from(settings.get(SCOPES_KEY)));
        if (settings.containsKey(DA_ATTRIBUTES_KEY)) setAttributes(Attributes.from(settings.get(DA_ATTRIBUTES_KEY)));
        if (settings.containsKey(LANGUAGE_KEY)) setLanguage(settings.get(LANGUAGE_KEY));
        if (settings.containsKey(DA_ADVERTISEMENT_PERIOD_KEY)) setAdvertisementPeriod(settings.get(DA_ADVERTISEMENT_PERIOD_KEY));
        if (settings.containsKey(DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY)) setExpiredServicesPurgePeriod(settings.get(DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY));
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService)
    {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public void setAddresses(String[] addresses)
    {
        this.addresses = addresses;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setScopes(Scopes scopes)
    {
        this.scopes = scopes;
    }

    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public void setAdvertisementPeriod(int advertisementPeriod)
    {
        this.advertisementPeriod = advertisementPeriod;
    }

    public void setExpiredServicesPurgePeriod(int expiredServicesPurgePeriod)
    {
        this.expiredServicesPurgePeriod = expiredServicesPurgePeriod;
    }

    public void addServiceListener(ServiceListener listener)
    {
        services.addServiceListener(listener);
    }
    
    public void removeServiceListener(ServiceListener listener)
    {
        services.removeServiceListener(listener);
    }

    public List<ServiceInfo> getServices()
    {
        return matchServices(null, null, null, null);
    }

    protected void doStart()
    {
        // Convert bootTime in seconds, as required by the DAAdvert message
        int bootTime = ((Long)TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())).intValue();
        setAttributes(attributes.merge(Attributes.from("(" + DirectoryAgentInfo.TCP_PORT_TAG + "=" + port + ")")));
        for (int i = 0; i < addresses.length; ++i) addresses[i] = NetUtils.convertWildcardAddress(NetUtils.getByName(addresses[i])).getHostAddress();
        for (String address : addresses)
            directoryAgents.put(address, DirectoryAgentInfo.from(address, scopes, attributes, language, bootTime));

        udpConnectorServer.addMessageListener(listener);
        udpConnectorServer.start();

        tcpConnectorServer.addMessageListener(listener);
        tcpConnectorServer.start();

        if (expiredServicesPurgePeriod > 0)
            scheduledExecutorService.scheduleWithFixedDelay(new ServicesPurger(), expiredServicesPurgePeriod, expiredServicesPurgePeriod, TimeUnit.SECONDS);

        // DirectoryAgents send a DAAdvert on boot (RFC 2608, 12.1)
        multicastDAAdvert.perform(directoryAgents.values(), false);

        // DirectoryAgents send unsolicited DAAdverts every advertisementPeriod seconds (RFC 2608, 12.2)
        if (advertisementPeriod > 0)
            scheduledExecutorService.scheduleWithFixedDelay(new UnsolicitedDAAdvert(), advertisementPeriod, advertisementPeriod, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Shutdown());
    }

    protected void doStop()
    {
        scheduledExecutorService.shutdownNow();

        // DirectoryAgents send a DAAdvert on shutdown (RFC 2608, 12.1)
        multicastDAAdvert.perform(directoryAgents.values(), true);

        tcpConnectorServer.removeMessageListener(listener);
        tcpConnectorServer.stop();

        udpConnectorServer.removeMessageListener(listener);
        udpConnectorServer.stop();
    }

    public List<DirectoryAgentInfo> getDirectoryAgentInfos()
    {
        return new ArrayList<DirectoryAgentInfo>(directoryAgents.values());
    }

    protected void handleMulticastSrvRqst(SrvRqst srvRqst, InetSocketAddress localAddress, InetSocketAddress remoteAddress)
    {
        String address = NetUtils.convertWildcardAddress(localAddress.getAddress()).getHostAddress();
        DirectoryAgentInfo directoryAgent = directoryAgents.get(address);
        if (directoryAgent == null)
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + srvRqst + ": arrived to unknown address " + address);
            return;
        }

        // Match previous responders
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

        // Check that's a correct multicast request for this DirectoryAgent
        ServiceType serviceType = srvRqst.getServiceType();
        if (DirectoryAgentInfo.SERVICE_TYPE.equals(serviceType))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " sending UDP unicast reply to " + remoteAddress);
            udpDAAdvert.perform(remoteAddress, directoryAgent, srvRqst);
        }
        else
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + srvRqst + ": expected service type " + DirectoryAgentInfo.SERVICE_TYPE + ", got instead " + serviceType);
        }
    }

    protected void handleTCPSrvRqst(SrvRqst srvRqst, Socket socket)
    {
        String localAddress = socket.getLocalAddress().getHostAddress();
        DirectoryAgentInfo directoryAgent = directoryAgents.get(localAddress);
        if (directoryAgent == null)
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("DirectoryAgent " + this + " dropping message " + srvRqst + ": arrived to unknown address " + localAddress);
        }

        List<ServiceInfo> matchingServices = matchServices(srvRqst.getServiceType(), srvRqst.getScopes(), srvRqst.getFilter(), srvRqst.getLanguage());
        tcpSrvRply.perform(socket, srvRqst, matchingServices);
        if (logger.isLoggable(Level.FINE))
            logger.fine("DirectoryAgent " + this + " returning " + matchingServices.size() + " services of type " + srvRqst.getServiceType());
    }

    protected List<ServiceInfo> matchServices(ServiceType serviceType, Scopes scopes, String filter, String language)
    {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("DirectoryAgent " + this + " matching ServiceType " + serviceType + ", scopes " + scopes + ", filter " + filter + ", language " + language);
        return services.match(serviceType, language, scopes, new FilterParser().parse(filter));
    }

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
            tcpSrvAck.perform(socket, srvReg, x.getErrorCode());
        }
    }

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
            tcpSrvAck.perform(socket, srvDeReg, x.getErrorCode());
        }
    }

    protected ServiceInfoCache.Result<ServiceInfo> cacheService(ServiceInfo service, boolean update)
    {
        // RFC 2608, 7.0
        if (!scopes.match(service.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Could not register service " + service + ", DirectoryAgent scopes " + scopes + " do not match with service scopes " + service.getScopes());
            throw new ServiceLocationException("Could not register service " + service, ServiceLocationException.SCOPE_NOT_SUPPORTED);
        }

        return update ? services.addAttributes(service.getKey(), service.getAttributes()) : services.put(service);
    }

    protected ServiceInfoCache.Result<ServiceInfo> uncacheService(ServiceInfo service, boolean update)
    {
        // RFC 2608, 7.0
        if (!scopes.match(service.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Could not deregister service " + service + ", DirectoryAgent scopes " + scopes + " do not match with service scopes " + service.getScopes());
            throw new ServiceLocationException("Could not deregister service " + service, ServiceLocationException.SCOPE_NOT_SUPPORTED);
        }

        return update ? services.removeAttributes(service.getKey(), service.getAttributes()) : services.remove(service.getKey());
    }

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
     * DirectoryAgents listen for multicast messages and for tcp messages that may arrive.
     * They are interested in:
     * <ul>
     * <li>Multicast SrvRqst, from UAs and SAs that wants to discover DAs; the reply is a DAAdvert</li>
     * <li>SrvRqst, from UAs and SAs that want to find services; the reply is a SrvRply</li>
     * <li>SrvReg, from SAs that wants to register a service; the reply is a SrvAck</li>
     * <li>SrvDeReg, from SAs that wants to unregister a service; the reply is a SrvAck</li>
     * </ul>
     */
    private class DirectoryAgentMessageListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            Message message = event.getMessage();
            if (logger.isLoggable(Level.FINEST))
                logger.finest("DirectoryAgent server message listener received message " + message);

            if (message.isMulticast())
            {
                InetSocketAddress localAddress = event.getLocalSocketAddress();
                InetSocketAddress remoteAddress = event.getRemoteSocketAddress();
                switch (message.getMessageType())
                {
                    case Message.SRV_RQST_TYPE:
                        handleMulticastSrvRqst((SrvRqst)message, localAddress, remoteAddress);
                        break;
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("DirectoryAgent " + StandardDirectoryAgentServer.this + " dropping multicast message " + message + ": not handled by DirectoryAgents");
                        break;
                }
            }
            else
            {
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
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("DirectoryAgent " + StandardDirectoryAgentServer.this + " dropping tcp message " + message + ": not handled by DirectoryAgents");
                        break;
                }
            }
        }
    }

    private class Shutdown extends Thread
    {
        @Override
        public void run()
        {
            if (StandardDirectoryAgentServer.this.isRunning()) StandardDirectoryAgentServer.this.stop();
        }
    }
}
