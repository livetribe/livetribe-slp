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
package org.livetribe.slp.sa;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLP;
import org.livetribe.slp.SLPError;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.da.StandardDirectoryAgentServer;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Factories;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.net.SocketTCPConnectorServer;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.net.UDPConnectorServer;
import org.livetribe.slp.ua.UserAgentClient;
import org.testng.annotations.Test;

/**
 * @version $Revision$ $Date$
 */
public class StandardServiceAgentServerTest
{
    private Settings newSettings()
    {
        Settings settings = new MapSettings();
        settings.put(PORT_KEY, 4427);
        return settings;
    }

    private StandardDirectoryAgentServer newDirectoryAgent()
    {
        // Setup a DAS on a different TCP port, so that SAS and DAS can coexist
        // It's a hack, but it's only used in tests where SAS and DAS are needed
        Settings settings = newSettings();
        int daPort = settings.get(PORT_KEY) + 1;
        UDPConnector.Factory udpFactory = Factories.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        TCPConnector.Factory tcpFactory = Factories.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        UDPConnectorServer.Factory udpServerFactory = Factories.newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY);
        ExecutorService threadPool = Executors.newCachedThreadPool();
        SocketTCPConnectorServer tcpConnectorServer = new SocketTCPConnectorServer(threadPool, settings);
        tcpConnectorServer.setPort(daPort);
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        StandardDirectoryAgentServer directoryAgent = new StandardDirectoryAgentServer(udpFactory.newUDPConnector(settings), tcpFactory.newTCPConnector(settings), udpServerFactory.newUDPConnectorServer(settings), tcpConnectorServer, scheduledExecutorService, settings);
        directoryAgent.setPort(daPort);
        return directoryAgent;
    }

    @Test
    public void testRegisterWithWrongScope()
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://wrongScope");
            ServiceInfo service = new ServiceInfo(serviceURL, Defaults.get(LANGUAGE_KEY), Scopes.from("wrong"), Attributes.NONE);
            ServiceAgentClient client = SLP.newServiceAgentClient(newSettings());
            try
            {
                client.register(service);
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getSLPError() == SLPError.SCOPE_NOT_SUPPORTED;
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testRegisterWithNoLanguage()
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://noLanguage");
            ServiceInfo service = new ServiceInfo(serviceURL, null, Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient client = SLP.newServiceAgentClient(newSettings());
            try
            {
                client.register(service);
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getSLPError() == SLPError.INVALID_REGISTRATION;
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testRegisterWithInvalidLifetime()
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://invalidLifetime", 0);
            ServiceInfo service = new ServiceInfo(serviceURL, Defaults.get(LANGUAGE_KEY), Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient client = SLP.newServiceAgentClient(newSettings());
            try
            {
                client.register(service);
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getSLPError() == SLPError.INVALID_REGISTRATION;
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testRegisterDeregisterWithoutDirectoryAgent()
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        try
        {
            List<ServiceInfo> services = sa.getServices();

            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://register");
            ServiceInfo service = new ServiceInfo(serviceURL, Defaults.get(LANGUAGE_KEY), Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient client = SLP.newServiceAgentClient(newSettings());
            client.register(service);

            List<ServiceInfo> services1 = sa.getServices();
            assert services1.size() == services.size() + 1;
            services1.removeAll(services);
            assert services1.size() == 1;
            assert services1.get(0).getKey().equals(service.getKey());

            client.deregister(service.getServiceURL(), service.getLanguage());

            List<ServiceInfo> services2 = sa.getServices();
            assert services2.equals(services);
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testRegisterDeregisterWithDirectoryAgent() throws Exception
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        try
        {
            StandardDirectoryAgentServer da = newDirectoryAgent();
            da.start();
            try
            {
                // Wait for the SAS to get the DAAdvert from DAS, so SAS gets also DAS different TCP port
                Thread.sleep(500);

                List<ServiceInfo> services = da.getServices();

                ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://daregister");
                ServiceInfo service = new ServiceInfo(serviceURL, Defaults.get(LANGUAGE_KEY), Scopes.DEFAULT, Attributes.NONE);
                ServiceAgentClient client = SLP.newServiceAgentClient(newSettings());
                client.register(service);

                List<ServiceInfo> services1 = da.getServices();
                assert services1.size() == services.size() + 1;
                services1.removeAll(services);
                assert services1.size() == 1;
                assert services1.get(0).getKey().equals(service.getKey());

                client.deregister(service.getServiceURL(), service.getLanguage());

                List<ServiceInfo> services2 = sa.getServices();
                assert services2.equals(services);
            }
            finally
            {
                da.stop();
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testFindServices()
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        try
        {
            List<ServiceInfo> services = sa.getServices();

            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://service");
            String language = Defaults.get(LANGUAGE_KEY);
            ServiceInfo service = new ServiceInfo(serviceURL, language, Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient client = SLP.newServiceAgentClient(newSettings());
            client.register(service);

            UserAgentClient finder = SLP.newUserAgentClient(newSettings());
            List<ServiceInfo> services1 = finder.findServices(serviceURL.getServiceType(), null, null, null);
            assert services1.size() == services.size() + 1;
            services1.removeAll(services);
            assert services1.size() == 1;
            ServiceInfo service1 = services1.get(0);
            assert service1.getKey().equals(service.getKey());
            assert service1.getScopes().equals(service.getScopes());
            assert service1.getAttributes().equals(service.getAttributes());
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testListenForDirectoryAgents() throws Exception
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        try
        {
            assert sa.getDirectoryAgents().size() == 0;

            StandardDirectoryAgentServer directoryAgent = newDirectoryAgent();
            directoryAgent.start();

            try
            {
                // Wait for the SAS to get the DAAdvert from DAS
                Thread.sleep(500);

                assert sa.getDirectoryAgents().size() == 1;
            }
            finally
            {
                directoryAgent.stop();
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testServiceRenewalNotPerformed() throws Exception
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        try
        {
            int lifetime = 2; // seconds
            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://dontRenew", lifetime);
            String language = Defaults.get(LANGUAGE_KEY);
            ServiceInfo service = new ServiceInfo(serviceURL, language, Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient client = SLP.newServiceAgentClient(newSettings());
            client.register(service);

            UserAgentClient ua = SLP.newUserAgentClient(newSettings());
            List<ServiceInfo> services = ua.findServices(serviceURL.getServiceType(), null, null, null);
            assert services.size() == 1;

            // Wait for the lifetime to expire
            TimeUnit.SECONDS.sleep(lifetime + 1);

            services = ua.findServices(serviceURL.getServiceType(), null, null, null);
            assert services.size() == 0;
        }
        finally
        {
            sa.stop();
        }
    }
}
