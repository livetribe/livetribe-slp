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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Factories;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.ServiceInfoCache;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.net.UDPConnectorServer;
import org.livetribe.slp.spi.sa.AbstractServiceAgent;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;

/**
 * @version $Revision$ $Date$
 */
public class StandardServiceAgent extends AbstractServiceAgent implements ServiceAgent
{
    public static StandardServiceAgent newInstance(Settings settings)
    {
        UDPConnector.Factory udpFactory = Factories.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        TCPConnector.Factory tcpFactory = Factories.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        UDPConnectorServer.Factory udpServerFactory = Factories.newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY);
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        return new StandardServiceAgent(udpFactory.newUDPConnector(settings), tcpFactory.newTCPConnector(settings), udpServerFactory.newUDPConnectorServer(settings), scheduledExecutorService, settings);
    }

    private final String identifier = UUID.randomUUID().toString();
    private final ScheduledExecutorService scheduledExecutorService;
    private boolean periodicServiceRenewalEnabled = Defaults.get(SA_SERVICE_RENEWAL_ENABLED_KEY);

    public StandardServiceAgent(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, ScheduledExecutorService scheduledExecutorService)
    {
        this(udpConnector, tcpConnector, udpConnectorServer, scheduledExecutorService, null);
    }

    public StandardServiceAgent(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, ScheduledExecutorService scheduledExecutorService, Settings settings)
    {
        super(udpConnector, tcpConnector, udpConnectorServer, settings);
        this.scheduledExecutorService = scheduledExecutorService;
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(SA_SERVICE_RENEWAL_ENABLED_KEY))
            this.periodicServiceRenewalEnabled = settings.get(SA_SERVICE_RENEWAL_ENABLED_KEY);
    }

    public boolean isPeriodicServiceRenewalEnabled()
    {
        return periodicServiceRenewalEnabled;
    }

    public void setPeriodicServiceRenewalEnabled(boolean periodicServiceRenewalEnabled)
    {
        this.periodicServiceRenewalEnabled = periodicServiceRenewalEnabled;
    }

    public void register(ServiceInfo service) throws ServiceLocationException
    {
        register(service, false);
    }

    public void addAttributes(ServiceURL serviceURL, String language, Attributes attributes) throws ServiceLocationException
    {
        ServiceInfo existingService = lookupService(new ServiceInfo(serviceURL, language, Scopes.NONE, Attributes.NONE));
        if (existingService == null)
            throw new ServiceLocationException("Could not find service to update", ServiceLocationException.Error.INVALID_UPDATE);
        ServiceInfo service = new ServiceInfo(serviceURL, language, existingService.getScopes(), attributes);
        register(service, true);
    }

    protected void register(ServiceInfo service, boolean update) throws ServiceLocationException
    {
        ServiceInfoCache.Result<ServiceInfo> result = cacheService(service, update);
        if (logger.isLoggable(Level.FINE))
            logger.fine("Registered service, current: " + result.getCurrent() + ", previous: " + result.getPrevious());
        if (isRunning()) forwardRegistration(service, result.getPrevious(), result.getCurrent(), update);
    }

    public void removeAttributes(ServiceURL serviceURL, String language, Attributes attributes) throws ServiceLocationException
    {
        if (attributes.isEmpty())
            throw new ServiceLocationException("No attribute tags to remove", ServiceLocationException.Error.INVALID_UPDATE);
        ServiceInfo existingService = lookupService(new ServiceInfo(serviceURL, language, Scopes.NONE, Attributes.NONE));
        if (existingService == null)
            throw new ServiceLocationException("Could not find service to update", ServiceLocationException.Error.INVALID_UPDATE);
        ServiceInfo service = new ServiceInfo(serviceURL, language, existingService.getScopes(), attributes);
        deregister(service, true);
    }

    public void deregister(ServiceURL serviceURL, String language) throws ServiceLocationException
    {
        ServiceInfo existingService = lookupService(new ServiceInfo(serviceURL, language, Scopes.NONE, Attributes.NONE));
        if (existingService == null)
            throw new ServiceLocationException("Could not find service to deregister", ServiceLocationException.Error.INVALID_REGISTRATION);
        ServiceInfo service = new ServiceInfo(serviceURL, language, existingService.getScopes(), Attributes.NONE);
        deregister(service, false);
    }

    protected void deregister(ServiceInfo service, boolean update) throws ServiceLocationException
    {
        ServiceInfoCache.Result<ServiceInfo> result = uncacheService(service, update);
        if (logger.isLoggable(Level.FINE))
            logger.fine("Deregistered service, current: " + result.getCurrent() + ", previous: " + result.getPrevious());
        if (isRunning()) forwardDeregistration(service, result.getPrevious(), result.getCurrent(), update);
    }

    @Override
    protected void doStart()
    {
        super.doStart();
        forwardRegistrations();
    }

    @Override
    protected void doStop()
    {
        scheduledExecutorService.shutdownNow();
        super.doStop();
    }

    @Override
    protected void registerServiceWithDirectoryAgent(ServiceInfo service, ServiceInfo oldService, ServiceInfo currentService, DirectoryAgentInfo directoryAgent, boolean update)
    {
        super.registerServiceWithDirectoryAgent(service, oldService, currentService, directoryAgent, update);
        scheduleServiceRenewal(currentService, directoryAgent);
    }

    @Override
    protected void deregisterServiceWithDirectoryAgent(ServiceInfo service, ServiceInfo oldService, ServiceInfo currentService, DirectoryAgentInfo directoryAgent, boolean update)
    {
        super.deregisterServiceWithDirectoryAgent(service, oldService, currentService, directoryAgent, update);
        if (update) scheduleServiceRenewal(currentService, directoryAgent);
    }

    @Override
    protected void notifyServiceRegistration(ServiceInfo service, ServiceInfo oldService, ServiceInfo currentService, boolean update)
    {
        super.notifyServiceRegistration(service, oldService, currentService, update);
        scheduleServiceRenewal(currentService, null);
    }

    @Override
    protected void notifyServiceDeregistration(ServiceInfo service, ServiceInfo oldService, ServiceInfo currentService, boolean update)
    {
        super.notifyServiceDeregistration(service, oldService, currentService, update);
        if (update) scheduleServiceRenewal(currentService, null);
    }

    protected ServiceAgentInfo newServiceAgentInfo(String address, Scopes scopes, Attributes attributes, String language)
    {
        return ServiceAgentInfo.from(identifier, address, scopes, attributes, language);
    }

    protected void scheduleServiceRenewal(ServiceInfo service, DirectoryAgentInfo directoryAgent)
    {
        if (periodicServiceRenewalEnabled && service.expires())
        {
            long renewalPeriod = TimeUnit.SECONDS.toMillis(service.getServiceURL().getLifetime());
            long renewalDelay = renewalPeriod - (renewalPeriod >> 2); // delay is 3/4 of the period
            if (renewalPeriod > 0)
            {
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("Scheduling renewal for " + service + ", period is " + renewalPeriod + " ms");

                // Schedule a one-shot renewal: when it fires it will re-schedule another one-shot renewal
                Runnable renewal = directoryAgent == null ? new MulticastRegistrationRenewal(service) : new DirectoryAgentRegistrationRenewal(service, directoryAgent);
                scheduledExecutorService.schedule(renewal, renewalDelay, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * ServiceAgent must refresh their registration with DAs before the lifetime specified
     * in the ServiceURL expires, otherwise the DA does not advertise them anymore.
     */
    private class DirectoryAgentRegistrationRenewal implements Runnable
    {
        private final ServiceInfo service;
        private final DirectoryAgentInfo directory;

        public DirectoryAgentRegistrationRenewal(ServiceInfo service, DirectoryAgentInfo directory)
        {
            this.service = service;
            this.directory = directory;
        }

        public void run()
        {
            try
            {
                if (service.isRegistered())
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Registration renewal of service " + service + " with DirectoryAgent " + directory);
                    registerServiceWithDirectoryAgent(service, null, service, directory, false);
                    service.setRegistered(true);
                }
                else
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Skipping registration renewal of service " + service + ", it has been deregistered");
                }
            }
            catch (Exception x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Could not renew registration of service " + service + " with DirectoryAgent " + directory, x);
            }
        }
    }

    private class MulticastRegistrationRenewal implements Runnable
    {
        private final ServiceInfo service;

        public MulticastRegistrationRenewal(ServiceInfo service)
        {
            this.service = service;
        }

        public void run()
        {
            try
            {
                if (service.isRegistered())
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Notification renewal of service " + service);
                    notifyServiceRegistration(service, null, service, false);
                    service.setRegistered(true);
                }
                else
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Skipping notification renewal of service " + service + ", it has been deregistered");
                }
            }
            catch (Exception x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Could not notify renewal of service " + service, x);
            }
        }
    }

    public static class Factory implements ServiceAgent.Factory
    {
        public ServiceAgent newServiceAgent(Settings settings)
        {
            return newInstance(settings);
        }
    }
}
