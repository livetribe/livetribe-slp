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
package org.livetribe.slp.sa;

import java.util.UUID;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.ServiceInfoCache;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.net.UDPConnectorServer;
import org.livetribe.slp.spi.sa.AbstractServiceAgent;
import org.livetribe.slp.spi.sa.SAServiceInfo;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;

/**
 * @version $Revision$ $Date$
 */
public class StandardServiceAgent extends AbstractServiceAgent implements ServiceAgent
{
    public static StandardServiceAgent newInstance(Settings settings)
    {
        UDPConnector.Factory udpFactory = org.livetribe.slp.settings.Factory.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        TCPConnector.Factory tcpFactory = org.livetribe.slp.settings.Factory.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        UDPConnectorServer.Factory udpServerFactory = org.livetribe.slp.settings.Factory.newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY);
        return new StandardServiceAgent(udpFactory.newUDPConnector(settings), tcpFactory.newTCPConnector(settings), udpServerFactory.newUDPConnectorServer(settings), settings);
    }

    private final String identifier = UUID.randomUUID().toString();

    public StandardServiceAgent(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer)
    {
        this(udpConnector, tcpConnector, udpConnectorServer, null);
    }

    public StandardServiceAgent(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, Settings settings)
    {
        super(udpConnector, tcpConnector, udpConnectorServer, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
    }

    public void register(ServiceInfo service) throws ServiceLocationException
    {
        register(service, false);
    }

    public void addAttributes(ServiceURL serviceURL, String language, Attributes attributes) throws ServiceLocationException
    {
        ServiceInfo existingService = lookupService(new SAServiceInfo(new ServiceInfo(serviceURL, language, Scopes.NONE, Attributes.NONE)));
        if (existingService == null)
            throw new ServiceLocationException("Could not find service to update", ServiceLocationException.INVALID_UPDATE);
        ServiceInfo service = new ServiceInfo(serviceURL, language, existingService.getScopes(), attributes);
        register(service, true);
    }

    protected void register(ServiceInfo service, boolean update) throws ServiceLocationException
    {
        SAServiceInfo newService = new SAServiceInfo(service);
        ServiceInfoCache.Result<SAServiceInfo> result = cacheService(newService, update);
        if (isRunning()) forwardRegistration(newService, result.getPrevious(), result.getCurrent(), update);
    }

    public void removeAttributes(ServiceURL serviceURL, String language, Attributes attributes) throws ServiceLocationException
    {
        if (attributes.isEmpty())
            throw new ServiceLocationException("No attribute tags to remove", ServiceLocationException.INVALID_UPDATE);
        ServiceInfo existingService = lookupService(new SAServiceInfo(new ServiceInfo(serviceURL, language, Scopes.NONE, Attributes.NONE)));
        if (existingService == null)
            throw new ServiceLocationException("Could not find service to update", ServiceLocationException.INVALID_UPDATE);
        ServiceInfo service = new ServiceInfo(serviceURL, language, existingService.getScopes(), attributes);
        deregister(service, true);
    }

    public void deregister(ServiceURL serviceURL, String language) throws ServiceLocationException
    {
        SAServiceInfo existingService = lookupService(new SAServiceInfo(new ServiceInfo(serviceURL, language, Scopes.NONE, Attributes.NONE)));
        if (existingService == null)
            throw new ServiceLocationException("Could not find service to deregister", ServiceLocationException.INVALID_REGISTRATION);
        ServiceInfo service = new ServiceInfo(serviceURL, language, existingService.getScopes(), Attributes.NONE);
        deregister(service, false);
    }

    protected void deregister(ServiceInfo service, boolean update) throws ServiceLocationException
    {
        SAServiceInfo newService = new SAServiceInfo(service);
        ServiceInfoCache.Result<SAServiceInfo> result = uncacheService(newService, update);
        if (isRunning()) forwardDeregistration(newService, result.getPrevious(), result.getCurrent(), update);
    }

    @Override
    protected void doStart()
    {
        super.doStart();
        forwardRegistrations();
    }

    protected ServiceAgentInfo newServiceAgentInfo(String address, Scopes scopes, Attributes attributes, String language)
    {
        return ServiceAgentInfo.from(identifier, address, scopes, attributes, language);
    }

    public static class Factory implements ServiceAgent.Factory
    {
        public ServiceAgent newServiceAgent(Settings settings)
        {
            return newInstance(settings);
        }
    }
}
