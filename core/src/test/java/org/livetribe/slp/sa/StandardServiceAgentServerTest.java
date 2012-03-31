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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.livetribe.slp.settings.Keys.LANGUAGE_KEY;
import static org.livetribe.slp.settings.Keys.PORT_KEY;
import static org.livetribe.slp.settings.Keys.SA_UNICAST_PREFER_TCP;
import static org.livetribe.slp.settings.Keys.TCP_CONNECTOR_FACTORY_KEY;
import static org.livetribe.slp.settings.Keys.UA_UNICAST_PREFER_TCP;
import static org.livetribe.slp.settings.Keys.UDP_CONNECTOR_FACTORY_KEY;
import static org.livetribe.slp.settings.Keys.UDP_CONNECTOR_SERVER_FACTORY_KEY;
import org.testng.annotations.Test;

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
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.net.SocketTCPConnectorServer;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.net.UDPConnectorServer;
import org.livetribe.slp.ua.UserAgentClient;


/**
 *
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
    public void testUDPServiceRegistration() throws Exception
    {
        Settings settings = newSettings();
        testServiceRegistration(settings);
    }

    @Test
    public void testTCPServiceRegistration() throws Exception
    {
        Settings settings = newSettings();
        settings.put(SA_UNICAST_PREFER_TCP, true);
        testServiceRegistration(settings);
    }

    private void testServiceRegistration(Settings saSettings)
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        assert sa.getServices().isEmpty();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient registrar = SLP.newServiceAgentClient(saSettings);
            registrar.register(service);

            assert sa.getServices().size() == 1;
            ServiceInfo registered = sa.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getAttributes().equals(service.getAttributes());
            assert registered.isRegistered();
            assert registered.getScopes().equals(service.getScopes());

            // Registration with wrong scopes must fail
            Scopes newScope = Scopes.from("scope");
            ServiceInfo newService = new ServiceInfo(service.getServiceURL(), service.getLanguage(), newScope, service.getAttributes());
            try
            {
                registrar.register(newService);
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getSLPError() == SLPError.SCOPE_NOT_SUPPORTED;
            }

            // Registration with no language must fail
            newService = new ServiceInfo(service.getServiceURL(), null, service.getScopes(), service.getAttributes());
            try
            {
                registrar.register(newService);
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getSLPError() == SLPError.INVALID_REGISTRATION;
            }

            // Registration with invalid lifetime must fail
            int newLifetime = ServiceURL.LIFETIME_NONE;
            ServiceURL newServiceURL = new ServiceURL(service.getServiceURL().getURL(), newLifetime);
            newService = new ServiceInfo(newServiceURL, service.getLanguage(), service.getScopes(), service.getAttributes());
            try
            {
                registrar.register(newService);
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
    public void testUDPServiceUpdate() throws Exception
    {
        Settings settings = newSettings();
        testServiceUpdate(settings);
    }

    @Test
    public void testTCPServiceUpdate() throws Exception
    {
        Settings settings = newSettings();
        settings.put(SA_UNICAST_PREFER_TCP, true);
        testServiceUpdate(settings);
    }

    private void testServiceUpdate(Settings saSettings) throws Exception
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        assert sa.getServices().isEmpty();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=long string),(d=\\FF\\00),e"));
            ServiceAgentClient registrar = SLP.newServiceAgentClient(saSettings);
            registrar.register(service);
            assert sa.getServices().size() == 1;
            ServiceInfo registered = sa.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getScopes().equals(service.getScopes());
            assert registered.getAttributes().equals(service.getAttributes());

            // Update with same information must pass
            registrar.addAttributes(service.getServiceURL(), service.getLanguage(), service.getAttributes());
            assert sa.getServices().size() == 1;
            registered = sa.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getScopes().equals(service.getScopes());
            assert registered.getAttributes().equals(service.getAttributes());

            // Update with different attributes must pass
            Attributes newAttributes = service.getAttributes().union(Attributes.from("(b=false),(f=1)"));
            registrar.addAttributes(service.getServiceURL(), service.getLanguage(), newAttributes);
            assert sa.getServices().size() == 1;
            registered = sa.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getScopes().equals(service.getScopes());
            assert registered.getAttributes().equals(newAttributes);

            // Removing attributes must pass
            newAttributes = Attributes.from("b");
            registrar.removeAttributes(service.getServiceURL(), service.getLanguage(), newAttributes);
            assert sa.getServices().size() == 1;
            registered = sa.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getScopes().equals(service.getScopes());
            assert !registered.getAttributes().containsTag("b");

            // Update with different language must fail
            String newLanguage = Locale.ITALIAN.getLanguage();
            try
            {
                registrar.addAttributes(service.getServiceURL(), newLanguage, service.getAttributes());
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getSLPError() == SLPError.INVALID_UPDATE;
                assert sa.getServices().size() == 1;
            }

            // Update with different ServiceURL must fail
            ServiceURL newServiceURL = new ServiceURL(service.getServiceURL().getURL() + ".new");
            try
            {
                registrar.addAttributes(newServiceURL, service.getLanguage(), service.getAttributes());
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getSLPError() == SLPError.INVALID_UPDATE;
                assert sa.getServices().size() == 1;
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testUDPServiceDeregistration() throws Exception
    {
        Settings settings = newSettings();
        testServiceDeregistration(settings);
    }

    @Test
    public void testTCPServiceDeregistration() throws Exception
    {
        Settings settings = newSettings();
        settings.put(SA_UNICAST_PREFER_TCP, true);
        testServiceDeregistration(settings);
    }

    private void testServiceDeregistration(Settings saSettings) throws Exception
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        assert sa.getServices().isEmpty();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=long string),(d=\\FF\\00),e"));
            ServiceAgentClient registrar = SLP.newServiceAgentClient(saSettings);
            registrar.register(service);
            assert sa.getServices().size() == 1;

            // Deregistering must pass
            registrar.deregister(service.getServiceURL(), service.getLanguage());
            assert sa.getServices().isEmpty();

            // Register again
            registrar.register(service);
            assert sa.getServices().size() == 1;

            // Update with different language must fail
            String newLanguage = Locale.ITALIAN.getLanguage();
            try
            {
                registrar.removeAttributes(service.getServiceURL(), newLanguage, service.getAttributes());
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getSLPError() == SLPError.INVALID_UPDATE;
                assert sa.getServices().size() == 1;
            }

            // Update with different ServiceURL must fail
            ServiceURL newServiceURL = new ServiceURL(service.getServiceURL().getURL() + ".new");
            try
            {
                registrar.removeAttributes(newServiceURL, service.getLanguage(), service.getAttributes());
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getSLPError() == SLPError.INVALID_UPDATE;
                assert sa.getServices().size() == 1;
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testRegisterUpdateDeregisterWithDirectoryAgent() throws Exception
    {
        // Cannot use UDP since both SAS and DAS are listening on UDP and it is likely that
        // the registration goes directly to the DAS and not to the SAS first.
        Settings saSettings = newSettings();
        saSettings.put(SA_UNICAST_PREFER_TCP, true);

        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(saSettings);
        sa.start();
        assert sa.getServices().isEmpty();
        try
        {
            StandardDirectoryAgentServer da = newDirectoryAgent();
            da.start();
            assert da.getServices().isEmpty();
            try
            {
                // Wait for the SAS to get the DAAdvert from DAS, so SAS gets also DAS different TCP port
                Thread.sleep(500);

                ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://daregister");
                ServiceInfo service = new ServiceInfo(serviceURL, Defaults.get(LANGUAGE_KEY), Scopes.DEFAULT, Attributes.NONE);
                ServiceAgentClient client = SLP.newServiceAgentClient(saSettings);
                client.register(service);

                List<ServiceInfo> saServices = sa.getServices();
                assert saServices.size() == 1;
                assert saServices.get(0).getKey().equals(service.getKey());

                List<ServiceInfo> daServices = da.getServices();
                assert daServices.size() == 1;
                assert daServices.get(0).getKey().equals(service.getKey());

                client.deregister(service.getServiceURL(), service.getLanguage());

                saServices = sa.getServices();
                assert saServices.isEmpty();

                daServices = da.getServices();
                assert daServices.isEmpty();
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
    public void testUDPFindServices()
    {
        Settings uaSettings = newSettings();
        testFindServices(uaSettings);
    }

    @Test
    public void testTCPFindServices()
    {
        Settings uaSettings = newSettings();
        uaSettings.put(UA_UNICAST_PREFER_TCP, true);
        testFindServices(uaSettings);
    }

    private void testFindServices(Settings uaSettings)
    {
        StandardServiceAgentServer sa = StandardServiceAgentServer.newInstance(newSettings());
        sa.start();
        assert sa.getServices().isEmpty();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://service");
            String language = Defaults.get(LANGUAGE_KEY);
            ServiceInfo service = new ServiceInfo(serviceURL, language, Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient client = SLP.newServiceAgentClient(newSettings());
            client.register(service);

            UserAgentClient finder = SLP.newUserAgentClient(uaSettings);
            List<ServiceInfo> services = finder.findServices(serviceURL.getServiceType(), null, null, null);
            assert services.size() == 1;
            ServiceInfo service1 = services.get(0);
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
