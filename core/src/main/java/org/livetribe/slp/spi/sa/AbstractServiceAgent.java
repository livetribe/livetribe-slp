/*
 * Copyright 2007-2008 the original author or authors
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
package org.livetribe.slp.spi.sa;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPError;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.da.DirectoryAgentEvent;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.da.DirectoryAgentListener;
import org.livetribe.slp.sa.ServiceListener;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.ADDRESSES_KEY;
import static org.livetribe.slp.settings.Keys.DA_ADDRESSES_KEY;
import static org.livetribe.slp.settings.Keys.LANGUAGE_KEY;
import static org.livetribe.slp.settings.Keys.PORT_KEY;
import static org.livetribe.slp.settings.Keys.SA_ATTRIBUTES_KEY;
import static org.livetribe.slp.settings.Keys.SA_UNICAST_PREFER_TCP;
import static org.livetribe.slp.settings.Keys.SCOPES_KEY;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.AbstractServer;
import org.livetribe.slp.spi.MulticastDASrvRqstPerformer;
import org.livetribe.slp.spi.ServiceInfoCache;
import org.livetribe.slp.spi.UDPSrvAckPerformer;
import org.livetribe.slp.spi.da.DirectoryAgentInfoCache;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.filter.FilterParser;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.net.NetUtils;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.net.UDPConnectorServer;

/**
 * @version $Revision$ $Date$
 */
public abstract class AbstractServiceAgent extends AbstractServer implements DirectoryAgentListener
{
    private final ServiceInfoCache<ServiceInfo> services = new ServiceInfoCache<ServiceInfo>();
    private final DirectoryAgentInfoCache directoryAgents = new DirectoryAgentInfoCache();
    private final MessageListener udpListener = new UDPMessageListener();
    private final Map<String, ServiceAgentInfo> serviceAgents = new HashMap<String, ServiceAgentInfo>();
    private final UDPConnectorServer udpConnectorServer;
    private final MulticastDASrvRqstPerformer multicastDASrvRqst;
    private final UDPSrvAckPerformer udpSrvAck;
    private final UnicastSrvRegPerformer unicastSrvReg;
    private final UnicastSrvDeRegPerformer unicastSrvDeReg;
    private final NotifySrvRegPerformer notifySrvReg;
    private final NotifySrvDeRegPerformer notifySrvDeReg;
    private final UDPSAAdvertPerformer udpSAAdvert;
    private final UDPSrvRplyPerformer udpSrvRply;
    private String[] directoryAgentAddresses = Defaults.get(DA_ADDRESSES_KEY);
    private String[] addresses = Defaults.get(ADDRESSES_KEY);
    private int port = Defaults.get(PORT_KEY);
    private Scopes scopes = Scopes.from(Defaults.get(SCOPES_KEY));
    private Attributes attributes = Attributes.from(Defaults.get(SA_ATTRIBUTES_KEY));
    private String language = Defaults.get(LANGUAGE_KEY);
    private boolean preferTCP = Defaults.get(SA_UNICAST_PREFER_TCP);

    protected AbstractServiceAgent(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, Settings settings)
    {
        this.udpConnectorServer = udpConnectorServer;
        this.multicastDASrvRqst = new MulticastDASrvRqstPerformer(udpConnector, settings);
        this.udpSrvAck = new UDPSrvAckPerformer(udpConnector, settings);
        this.unicastSrvReg = new UnicastSrvRegPerformer(udpConnector, tcpConnector, settings);
        this.unicastSrvDeReg = new UnicastSrvDeRegPerformer(udpConnector, tcpConnector, settings);
        this.notifySrvReg = new NotifySrvRegPerformer(udpConnector, settings);
        this.notifySrvDeReg = new NotifySrvDeRegPerformer(udpConnector, settings);
        this.udpSAAdvert = new UDPSAAdvertPerformer(udpConnector, settings);
        this.udpSrvRply = new UDPSrvRplyPerformer(udpConnector, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(DA_ADDRESSES_KEY)) this.directoryAgentAddresses = settings.get(DA_ADDRESSES_KEY);
        if (settings.containsKey(ADDRESSES_KEY)) this.addresses = settings.get(ADDRESSES_KEY);
        if (settings.containsKey(PORT_KEY)) this.port = settings.get(PORT_KEY);
        if (settings.containsKey(SCOPES_KEY)) this.scopes = Scopes.from(settings.get(SCOPES_KEY));
        if (settings.containsKey(SA_ATTRIBUTES_KEY)) this.attributes = Attributes.from(settings.get(SA_ATTRIBUTES_KEY));
        if (settings.containsKey(LANGUAGE_KEY)) this.language = settings.get(LANGUAGE_KEY);
        if (settings.containsKey(SA_UNICAST_PREFER_TCP)) this.preferTCP = settings.get(SA_UNICAST_PREFER_TCP);
    }

    public String[] getDirectoryAgentAddresses()
    {
        return directoryAgentAddresses;
    }

    public void setDirectoryAgentAddresses(String[] directoryAgentAddresses)
    {
        this.directoryAgentAddresses = directoryAgentAddresses;
    }

    public String[] getAddresses()
    {
        return addresses;
    }

    public void setAddresses(String[] addresses)
    {
        this.addresses = addresses;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public Scopes getScopes()
    {
        return scopes;
    }

    public void setScopes(Scopes scopes)
    {
        this.scopes = scopes;
    }

    public Attributes getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
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
        return new ArrayList<ServiceInfo>(matchServices(null, null, null, null));
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

    protected void doStart()
    {
        for (int i = 0; i < addresses.length; ++i)
            addresses[i] = NetUtils.convertWildcardAddress(NetUtils.getByName(addresses[i])).getHostAddress();
        for (String address : addresses)
            serviceAgents.put(address, newServiceAgentInfo(address, scopes, attributes, language));

        if (directoryAgentAddresses.length > 0)
            for (String daAddress : directoryAgentAddresses) directoryAgents.add(DirectoryAgentInfo.from(daAddress));
        else
            directoryAgents.addAll(discoverDirectoryAgents(scopes, null));
        addDirectoryAgentListener(this);

        udpConnectorServer.addMessageListener(udpListener);
        udpConnectorServer.start();
    }

    protected abstract ServiceAgentInfo newServiceAgentInfo(String address, Scopes scopes, Attributes attributes, String language);

    protected void doStop()
    {
        // RFC 2608, 10.6, requires services to deregister when no longer available
        deregisterServices();

        udpConnectorServer.removeMessageListener(udpListener);
        udpConnectorServer.stop();

        removeDirectoryAgentListener(this);
        directoryAgents.removeAll();
    }

    protected List<DirectoryAgentInfo> discoverDirectoryAgents(Scopes scopes, Filter filter)
    {
        List<DirectoryAgentInfo> result = new ArrayList<DirectoryAgentInfo>();
        List<DAAdvert> daAdverts = multicastDASrvRqst.perform(null, scopes, filter);
        for (DAAdvert daAdvert : daAdverts) result.add(DirectoryAgentInfo.from(daAdvert));
        if (logger.isLoggable(Level.FINE)) logger.fine("ServiceAgent " + this + " discovered DAs: " + result);
        return result;
    }

    protected void forwardRegistration(ServiceInfo givenService, ServiceInfo oldService, ServiceInfo currentService, boolean update)
    {
        List<DirectoryAgentInfo> directoryAgents = this.directoryAgents.match(currentService.getScopes(), null);
        if (!directoryAgents.isEmpty())
        {
            for (DirectoryAgentInfo directoryAgent : directoryAgents)
            {
                registerServiceWithDirectoryAgent(givenService, oldService, currentService, directoryAgent, update);
            }
        }
        else
        {
            // There are no DA with a matching scope deployed on the network: multicast a SrvReg as specified by RFC 3082.
            notifyServiceRegistration(givenService, oldService, currentService, update);
        }
    }

    protected void registerServiceWithDirectoryAgent(ServiceInfo service, ServiceInfo oldService, ServiceInfo currentService, DirectoryAgentInfo directoryAgent, boolean update)
    {
        InetSocketAddress daAddress = resolveDirectoryAgentAddress(directoryAgent);
        SrvAck srvAck = unicastSrvReg.perform(daAddress, preferTCP, service, update);
        SLPError error = srvAck.getSLPError();
        if (error != SLPError.NO_ERROR)
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Could not register service " + service + " to DirectoryAgent " + directoryAgent + ": error " + error);
            throw new ServiceLocationException("Could not register service " + service, error);
        }
        else
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Registered service " + service + " to DirectoryAgent " + directoryAgent);
        }
    }

    protected void notifyServiceRegistration(ServiceInfo service, ServiceInfo oldService, ServiceInfo currentService, boolean update)
    {
        notifySrvReg.perform(serviceAgents.values(), service, update);
        if (logger.isLoggable(Level.FINE)) logger.fine("Sent notification for registration of service " + service);
    }

    protected void notifyServiceDeregistration(ServiceInfo service, ServiceInfo oldService, ServiceInfo currentService, boolean update)
    {
        notifySrvDeReg.perform(serviceAgents.values(), service, update);
        if (logger.isLoggable(Level.FINE)) logger.fine("Sent notification for deregistration of service " + service);
    }

    protected void forwardRegistrations()
    {
        for (ServiceInfo service : services.getServiceInfos()) forwardRegistration(service, null, service, false);
    }

    protected void deregisterServices()
    {
        for (ServiceInfo service : services.getServiceInfos())
        {
            ServiceInfoCache.Result<ServiceInfo> result = uncacheService(service, false);
            ServiceInfo oldService = result.getPrevious();
            ServiceInfo givenService = new ServiceInfo(oldService.getServiceURL(), oldService.getLanguage(), oldService.getScopes(), Attributes.NONE);
            forwardDeregistration(givenService, oldService, null, false);
        }
    }

    protected void forwardDeregistration(ServiceInfo givenService, ServiceInfo oldService, ServiceInfo currentService, boolean update)
    {
        List<DirectoryAgentInfo> directoryAgents = this.directoryAgents.match(oldService.getScopes(), null);
        if (!directoryAgents.isEmpty())
        {
            for (DirectoryAgentInfo directoryAgent : directoryAgents)
            {
                deregisterServiceWithDirectoryAgent(givenService, oldService, currentService, directoryAgent, update);
            }
        }
        else
        {
            // There are no DA with a matching scope deployed on the network: multicast a SrvDeReg as specified by RFC 3082.
            notifyServiceDeregistration(givenService, oldService, currentService, update);
        }
    }

    protected void deregisterServiceWithDirectoryAgent(ServiceInfo service, ServiceInfo oldService, ServiceInfo currentService, DirectoryAgentInfo directoryAgent, boolean update)
    {
        InetSocketAddress daAddress = resolveDirectoryAgentAddress(directoryAgent);
        SrvAck srvAck = unicastSrvDeReg.perform(daAddress, preferTCP, service, update);
        SLPError error = srvAck.getSLPError();
        if (error != SLPError.NO_ERROR)
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Could not deregister service " + service + " from DirectoryAgent " + directoryAgent + ": error " + error);
            throw new ServiceLocationException("Could not deregister service " + service, error);
        }
        else
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Deregistered service " + service + " from DirectoryAgent " + directoryAgent);
        }
    }

    protected InetSocketAddress resolveDirectoryAgentAddress(DirectoryAgentInfo directoryAgent)
    {
        InetAddress daAddress = NetUtils.getByName(directoryAgent.getHostAddress());
        int daPort = directoryAgent.getUnicastPort(preferTCP, port);
        return new InetSocketAddress(daAddress, daPort);
    }

    protected void handleMulticastDAAdvert(DAAdvert daAdvert)
    {
        DirectoryAgentInfo directoryAgent = DirectoryAgentInfo.from(daAdvert);
        directoryAgents.handle(directoryAgent);
    }

    protected void handleUDPSrvRqst(SrvRqst srvRqst, InetSocketAddress localAddress, InetSocketAddress remoteAddress)
    {
        String address = NetUtils.convertWildcardAddress(localAddress.getAddress()).getHostAddress();
        ServiceAgentInfo serviceAgent = serviceAgents.get(address);
        if (serviceAgent == null)
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + srvRqst + ": arrived to unknown address " + address);
            return;
        }

        // Do not reply if we have already replied
        String responder = remoteAddress.getAddress().getHostAddress();
        if (srvRqst.containsResponder(responder))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + srvRqst + ": already contains responder " + responder);
            return;
        }

        // Match scopes
        if (!scopes.weakMatch(srvRqst.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + srvRqst + ": no scopes match among agent scopes " + scopes + " and message scopes " + srvRqst.getScopes());
            return;
        }

        ServiceType serviceType = srvRqst.getServiceType();
        if (ServiceAgentInfo.SERVICE_TYPE.equals(serviceType))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " sending UDP unicast reply to " + remoteAddress);
            udpSAAdvert.perform(localAddress, remoteAddress, serviceAgent, srvRqst);
        }
        else if (DirectoryAgentInfo.SERVICE_TYPE.equals(serviceType))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " ignoring message " + srvRqst + ", service type not handled by ServiceAgents");
        }
        else
        {
            List<ServiceInfo> matchingServices = matchServices(serviceType, srvRqst.getLanguage(), srvRqst.getScopes(), srvRqst.getFilter());
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " returning " + matchingServices.size() + " services of type " + srvRqst.getServiceType());
            udpSrvRply.perform(localAddress, remoteAddress, serviceAgent, srvRqst, matchingServices);
        }
    }

    private List<ServiceInfo> matchServices(ServiceType serviceType, String language, Scopes scopes, String filter) throws ServiceLocationException
    {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("ServiceAgent " + this + " matching ServiceType " + serviceType + ", language " + language + ", scopes " + scopes + ", filter " + filter);
        List<ServiceInfo> result = services.match(serviceType, language, scopes, new FilterParser().parse(filter));
        if (logger.isLoggable(Level.FINEST))
            logger.finest("ServiceAgent " + this + " matched " + result.size() + " services: " + result);
        return result;
    }

    protected void handleUDPSrvReg(SrvReg srvReg, InetSocketAddress localAddress, InetSocketAddress remoteAddress)
    {
        try
        {
            boolean update = srvReg.isUpdating();
            ServiceInfo givenService = ServiceInfo.from(srvReg);
            ServiceInfoCache.Result<ServiceInfo> result = cacheService(givenService, update);
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " registered service " + givenService);
            forwardRegistration(givenService, result.getPrevious(), result.getCurrent(), update);
            udpSrvAck.perform(localAddress, remoteAddress, srvReg, SLPError.NO_ERROR);
        }
        catch (ServiceLocationException x)
        {
            udpSrvAck.perform(localAddress, remoteAddress, srvReg, x.getSLPError());
        }
    }

    protected void handleUDPSrvDeReg(SrvDeReg srvDeReg, InetSocketAddress localAddress, InetSocketAddress remoteAddress)
    {
        try
        {
            boolean update = srvDeReg.isUpdating();
            ServiceInfo givenService = ServiceInfo.from(srvDeReg);
            ServiceInfoCache.Result<ServiceInfo> result = uncacheService(givenService, update);
            forwardDeregistration(givenService, result.getPrevious(), result.getCurrent(), update);
            udpSrvAck.perform(localAddress, remoteAddress, srvDeReg, SLPError.NO_ERROR);
        }
        catch (ServiceLocationException x)
        {
            udpSrvAck.perform(localAddress, remoteAddress, srvDeReg, x.getSLPError());
        }
    }

    public void directoryAgentBorn(DirectoryAgentEvent event)
    {
        DirectoryAgentInfo directoryAgent = event.getDirectoryAgent();
        if (logger.isLoggable(Level.FINEST))
            logger.finest("ServiceAgent " + this + " noticed DirectoryAgent birth: " + directoryAgent);
        // TODO: RFC 2608, 12.2.2 requires to wait some time before registering
        registerServices(directoryAgent);
    }

    private void registerServices(DirectoryAgentInfo directoryAgent)
    {
        for (ServiceInfo service : services.match(null, null, directoryAgent.getScopes(), null))
        {
            registerServiceWithDirectoryAgent(service, null, service, directoryAgent, false);
        }
    }

    public void directoryAgentDied(DirectoryAgentEvent event)
    {
        DirectoryAgentInfo directoryAgent = event.getDirectoryAgent();
        if (logger.isLoggable(Level.FINEST))
            logger.finest("ServiceAgent " + this + " noticed DirectoryAgent death: " + directoryAgent);
    }

    protected ServiceInfoCache.Result<ServiceInfo> cacheService(ServiceInfo service, boolean update)
    {
        // RFC 2608, 7.0
        if (!scopes.match(service.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Could not register service " + service + ", ServiceAgent scopes " + scopes + " do not match with service scopes " + service.getScopes());
            throw new ServiceLocationException("Could not register service " + service, SLPError.SCOPE_NOT_SUPPORTED);
        }

        return update ? services.addAttributes(service.getKey(), service.getAttributes()) : services.put(service);
    }

    protected ServiceInfo lookupService(ServiceInfo service)
    {
        return services.get(service.getKey());
    }

    protected ServiceInfoCache.Result<ServiceInfo> uncacheService(ServiceInfo service, boolean update)
    {
        // RFC 2608, 7.0
        if (!scopes.match(service.getScopes()))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Could not deregister service " + service + ", ServiceAgent scopes " + scopes + " do not match with service scopes " + service.getScopes());
            throw new ServiceLocationException("Could not deregister service " + service, SLPError.SCOPE_NOT_SUPPORTED);
        }

        return update ? services.removeAttributes(service.getKey(), service.getAttributes()) : services.remove(service.getKey());
    }

    /**
     * ServiceAgents listen for udp messages that may arrive.
     */
    private class UDPMessageListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            Message message = event.getMessage();
            if (logger.isLoggable(Level.FINEST))
                logger.finest("ServiceAgent message listener received message " + message);

            InetSocketAddress localAddress = event.getLocalSocketAddress();
            InetSocketAddress remoteAddress = event.getRemoteSocketAddress();
            switch (message.getMessageType())
            {
                case Message.DA_ADVERT_TYPE:
                    if (message.isMulticast())
                        handleMulticastDAAdvert((DAAdvert)message);
                    else if (logger.isLoggable(Level.FINE))
                        logger.fine("ServiceAgent " + this + " dropping message " + message + ": DAAdvert must be multicast");
                    break;
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
                        logger.fine("UserAgent " + this + " dropping multicast message " + message + ": not handled by ServiceAgents");
                    break;
            }
        }
    }
}
