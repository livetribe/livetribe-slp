/*
 * Copyright 2008-2008 the original author or authors
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

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.livetribe.slp.settings.Keys.PORT_KEY;
import static org.livetribe.slp.settings.Keys.SA_UNICAST_PREFER_TCP;
import static org.livetribe.slp.settings.Keys.TCP_CONNECTOR_SERVER_FACTORY_KEY;
import static org.livetribe.slp.settings.Keys.TCP_READ_TIMEOUT_KEY;
import static org.livetribe.slp.settings.Keys.UDP_CONNECTOR_SERVER_FACTORY_KEY;
import org.testng.annotations.Test;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.settings.Factories;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.net.ConnectorServer;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.net.TCPConnectorServer;
import org.livetribe.slp.spi.net.UDPConnectorServer;


/**
 *
 */
public class StandardServiceAgentClientTest
{
    private Settings newSettings()
    {
        Settings settings = new MapSettings();
        settings.put(PORT_KEY, 4427);
        return settings;
    }

    @Test
    public void testUseUDP()
    {
        Settings settings = newSettings();
        UDPConnectorServer.Factory udpServerFactory = Factories.newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY);
        Settings saSettings = newSettings();
        saSettings.put(SA_UNICAST_PREFER_TCP, false);
        testUnicast(udpServerFactory.newUDPConnectorServer(settings), saSettings);
    }

    @Test
    public void testUseTCP()
    {
        Settings settings = newSettings();
        TCPConnectorServer.Factory tcpServerFactory = Factories.newInstance(settings, TCP_CONNECTOR_SERVER_FACTORY_KEY);
        Settings saSettings = newSettings();
        saSettings.put(SA_UNICAST_PREFER_TCP, true);
        saSettings.put(TCP_READ_TIMEOUT_KEY, 500);
        testUnicast(tcpServerFactory.newTCPConnectorServer(settings), saSettings);
    }

    private void testUnicast(ConnectorServer connectorServer, Settings saSettings)
    {
        final AtomicReference<MessageEvent> messages = new AtomicReference<MessageEvent>();
        MessageListener listener = new MessageListener()
        {
            public void handle(MessageEvent event)
            {
                messages.set(event);
            }
        };
        connectorServer.addMessageListener(listener);
        connectorServer.start();
        try
        {
            StandardServiceAgentClient sa = StandardServiceAgentClient.newInstance(saSettings);

            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.NONE);
            try
            {
                sa.register(service);
                assert false;
            }
            catch (ServiceLocationException ignored)
            {
            }
            assert messages.get() != null;

            messages.set(null);
            try
            {
                sa.addAttributes(service.getServiceURL(), service.getLanguage(), Attributes.from("(a=1)"));
                assert false;
            }
            catch (ServiceLocationException ignored)
            {
            }
            assert messages.get() != null;

            messages.set(null);
            try
            {
                sa.addAttributes(service.getServiceURL(), service.getLanguage(), Attributes.fromTags("a"));
                assert false;
            }
            catch (ServiceLocationException ignored)
            {
            }
            assert messages.get() != null;

            messages.set(null);
            try
            {
                sa.deregister(service.getServiceURL(), service.getLanguage());
                assert false;
            }
            catch (ServiceLocationException ignored)
            {
            }
            assert messages.get() != null;
        }
        finally
        {
            connectorServer.stop();
        }
    }
}
