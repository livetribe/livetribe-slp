/*
 * Copyright 2007 the original author or authors
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

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLP;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.da.StandardDirectoryAgentServer;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.Factories;
import org.livetribe.slp.srv.da.DirectoryAgentInfo;
import org.livetribe.slp.srv.net.SocketTCPConnectorServer;
import org.livetribe.slp.srv.net.TCPConnector;
import org.livetribe.slp.srv.net.UDPConnector;
import org.livetribe.slp.srv.net.UDPConnectorServer;
import org.livetribe.slp.ua.UserAgentClient;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @version $Revision$ $Date$
 */
public class StandardServiceAgentServerTest
{
    private Settings settings;
    private StandardServiceAgentServer serviceAgentServer;

    @BeforeMethod
    public void initServer()
    {
        Defaults.reload();
        settings = new MapSettings();
        settings.put(PORT_KEY, 4427);
        serviceAgentServer = StandardServiceAgentServer.newInstance(settings);
        serviceAgentServer.start();
    }

    @AfterMethod(alwaysRun = true)
    public void destroyServer() throws InterruptedException
    {
        if (serviceAgentServer != null) serviceAgentServer.stop();
        serviceAgentServer = null;
    }

    @Test
    public void testRegisterWithWrongScope()
    {
        ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://wrongScope");
        ServiceInfo service = new ServiceInfo(serviceURL, Defaults.get(LANGUAGE_KEY), Scopes.from("wrong"), Attributes.NONE);
        ServiceAgentClient client = SLP.newServiceAgentClient(settings);
        try
        {
            client.register(service);
            throw new AssertionError();
        }
        catch (ServiceLocationException x)
        {
            assert x.getErrorCode() == ServiceLocationException.SCOPE_NOT_SUPPORTED;
        }
    }

    @Test
    public void testRegisterWithNoLanguage()
    {
        ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://noLanguage");
        ServiceInfo service = new ServiceInfo(serviceURL, null, Scopes.DEFAULT, Attributes.NONE);
        ServiceAgentClient client = SLP.newServiceAgentClient(settings);
        try
        {
            client.register(service);
            throw new AssertionError();
        }
        catch (ServiceLocationException x)
        {
            assert x.getErrorCode() == ServiceLocationException.INVALID_REGISTRATION;
        }
    }

    @Test
    public void testRegisterWithInvalidLifetime()
    {
        ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://invalidLifetime", 0);
        ServiceInfo service = new ServiceInfo(serviceURL, Defaults.get(LANGUAGE_KEY), Scopes.DEFAULT, Attributes.NONE);
        ServiceAgentClient client = SLP.newServiceAgentClient(settings);
        try
        {
            client.register(service);
            throw new AssertionError();
        }
        catch (ServiceLocationException x)
        {
            assert x.getErrorCode() == ServiceLocationException.INVALID_REGISTRATION;
        }
    }

    @Test
    public void testRegisterDeregisterWithoutDirectoryAgent()
    {
        List<ServiceInfo> services = serviceAgentServer.getServices();

        ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://register");
        ServiceInfo service = new ServiceInfo(serviceURL, Defaults.get(LANGUAGE_KEY), Scopes.DEFAULT, Attributes.NONE);
        ServiceAgentClient client = SLP.newServiceAgentClient(settings);
        client.register(service);

        List<ServiceInfo> services1 = serviceAgentServer.getServices();
        assert services1.size() == services.size() + 1;
        services1.removeAll(services);
        assert services1.size() == 1;
        assert services1.get(0).getKey().equals(service.getKey());

        client.deregister(service.getServiceURL(), service.getLanguage());

        List<ServiceInfo> services2 = serviceAgentServer.getServices();
        assert services2.equals(services);
    }

    @Test
    public void testRegisterDeregisterWithDirectoryAgent() throws Exception
    {
        StandardDirectoryAgentServer directoryAgent = newDirectoryAgent();
        directoryAgent.start();

        try
        {
            // Wait for the SAS to get the DAAdvert from DAS, so SAS gets also DAS different TCP port
            Thread.sleep(500);

            List<ServiceInfo> services = directoryAgent.getServices();

            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://daregister");
            ServiceInfo service = new ServiceInfo(serviceURL, Defaults.get(LANGUAGE_KEY), Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient client = SLP.newServiceAgentClient(settings);
            client.register(service);

            List<ServiceInfo> services1 = directoryAgent.getServices();
            assert services1.size() == services.size() + 1;
            services1.removeAll(services);
            assert services1.size() == 1;
            assert services1.get(0).getKey().equals(service.getKey());

            client.deregister(service.getServiceURL(), service.getLanguage());

            List<ServiceInfo> services2 = serviceAgentServer.getServices();
            assert services2.equals(services);
        }
        finally
        {
            directoryAgent.stop();
        }
    }

    private StandardDirectoryAgentServer newDirectoryAgent()
    {
        // Setup a DAS on a different TCP port, so that SAS and DAS can coexist
        // It's a hack, but it's only used in tests where SAS and DAS are needed
        int daPort = settings.get(PORT_KEY) + 1;
        UDPConnector.Factory udpFactory = Factories.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        TCPConnector.Factory tcpFactory = Factories.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        UDPConnectorServer.Factory udpServerFactory = Factories.newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY);
        SocketTCPConnectorServer tcpConnectorServer = new SocketTCPConnectorServer(settings);
        tcpConnectorServer.setPort(daPort);
        StandardDirectoryAgentServer directoryAgent = new StandardDirectoryAgentServer(udpFactory.newUDPConnector(settings), tcpFactory.newTCPConnector(settings), udpServerFactory.newUDPConnectorServer(settings), tcpConnectorServer, settings);
        directoryAgent.setPort(daPort);
        return directoryAgent;
    }

    @Test
    public void testFindServices()
    {
        List<ServiceInfo> services = serviceAgentServer.getServices();

        ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://service");
        String language = Defaults.get(LANGUAGE_KEY);
        ServiceInfo service = new ServiceInfo(serviceURL, language, Scopes.DEFAULT, Attributes.NONE);
        ServiceAgentClient client = SLP.newServiceAgentClient(settings);
        client.register(service);

        UserAgentClient finder = SLP.newUserAgentClient(settings);
        List<ServiceInfo> services1 = finder.findServices(serviceURL.getServiceType(), null, null, null);
        assert services1.size() == services.size() + 1;
        services1.removeAll(services);
        assert services1.size() == 1;
        ServiceInfo service1 = services1.get(0);
        assert service1.getKey().equals(service.getKey());
        assert service1.getScopes().equals(service.getScopes());
        assert service1.getAttributes().equals(service.getAttributes());
    }

    @Test
    public void testListenForDirectoryAgents() throws Exception
    {
        assert serviceAgentServer.getDirectoryAgents().size() == 0;

        StandardDirectoryAgentServer directoryAgent = newDirectoryAgent();
        directoryAgent.start();

        try
        {
            // Wait for the SAS to get the DAAdvert from DAS
            Thread.sleep(500);

            List<DirectoryAgentInfo> directoryAgentInfos = directoryAgent.getDirectoryAgentInfos();
            assert directoryAgentInfos.size() == 1;

            assert serviceAgentServer.getDirectoryAgents().size() == 1;
            assert serviceAgentServer.getDirectoryAgents().get(0).getKey().equals(directoryAgentInfos.get(0).getKey());
        }
        finally
        {
            directoryAgent.stop();
        }
    }
}
