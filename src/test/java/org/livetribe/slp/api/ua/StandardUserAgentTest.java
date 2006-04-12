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
package org.livetribe.slp.api.ua;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;

import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.SLPAPITestCase;
import org.livetribe.slp.api.da.StandardDirectoryAgent;
import org.livetribe.slp.spi.da.StandardDirectoryAgentManager;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.net.SocketMulticastConnector;
import org.livetribe.slp.spi.net.SocketUnicastConnector;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;
import org.livetribe.slp.spi.sa.StandardServiceAgentManager;
import org.livetribe.slp.spi.ua.StandardUserAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardUserAgentTest extends SLPAPITestCase
{
    protected void tearDown() throws Exception
    {
        sleep(500);
    }

    public void testStartStop() throws Exception
    {
        StandardUserAgent ua = new StandardUserAgent();
        StandardUserAgentManager uaManager = new StandardUserAgentManager();
        uaManager.setMulticastConnector(new SocketMulticastConnector());
        uaManager.setUnicastConnector(new SocketUnicastConnector());
        ua.setUserAgentManager(uaManager);
        ua.setConfiguration(getDefaultConfiguration());

        assertFalse(ua.isRunning());
        ua.start();
        assertTrue(ua.isRunning());
        ua.stop();
        assertFalse(ua.isRunning());
        ua.start();
        assertTrue(ua.isRunning());
        ua.stop();
        assertFalse(ua.isRunning());
    }

    public void testFindServices() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        daManager.setUnicastConnector(new SocketUnicastConnector());
        da.setConfiguration(getDefaultConfiguration());
        da.start();

        try
        {
            InetAddress localhost = InetAddress.getLocalHost();

            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            saManager.setMulticastConnector(new SocketMulticastConnector());
            saManager.setUnicastConnector(new SocketUnicastConnector());
            saManager.setConfiguration(getDefaultConfiguration());
            saManager.start();

            try
            {
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", 13);
                String[] scopes = new String[]{"scope1", "scope2"};
                ServiceAgentInfo info = new ServiceAgentInfo(null, serviceURL, scopes, null, Locale.getDefault().getCountry(), true);
                SrvAck ack = saManager.unicastSrvReg(localhost, info);

                assertNotNull(ack);
                assertEquals(0, ack.getErrorCode());

                StandardUserAgent ua = new StandardUserAgent();
                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                ua.setUserAgentManager(uaManager);
                uaManager.setMulticastConnector(new SocketMulticastConnector());
                uaManager.setUnicastConnector(new SocketUnicastConnector());
                ua.setConfiguration(getDefaultConfiguration());
                ua.start();

                try
                {
                    List serviceURLs = ua.findServices(serviceURL.getServiceType(), scopes, null);

                    assertNotNull(serviceURLs);
                    assertEquals(1, serviceURLs.size());
                    ServiceURL service = (ServiceURL)serviceURLs.get(0);
                    assertNotNull(service);
                    assertEquals(serviceURL, service);
                    assertEquals(serviceURL.getLifetime(), service.getLifetime());
                }
                finally
                {
                    uaManager.stop();
                }
            }
            finally
            {
                saManager.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }

    public void testListenForDAAdverts() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardUserAgent ua = new StandardUserAgent();
        StandardUserAgentManager uaManager = new StandardUserAgentManager();
        ua.setUserAgentManager(uaManager);
        uaManager.setMulticastConnector(new SocketMulticastConnector());
        uaManager.setUnicastConnector(new SocketUnicastConnector());
        ua.setConfiguration(configuration);
        ua.start();

        try
        {
            List das = ua.getCachedDirectoryAgents(ua.getScopes());
            assertNotNull(das);
            assertTrue(das.isEmpty());

            StandardDirectoryAgent da = new StandardDirectoryAgent();
            StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
            da.setDirectoryAgentManager(daManager);
            daManager.setMulticastConnector(new SocketMulticastConnector());
            daManager.setUnicastConnector(new SocketUnicastConnector());
            da.setConfiguration(getDefaultConfiguration());
            da.start();

            try
            {
                // Allow unsolicited DAAdvert to arrive and UA to cache it
                sleep(500);

                das = ua.getCachedDirectoryAgents(ua.getScopes());
                assertNotNull(das);
                assertEquals(1, das.size());
            }
            finally
            {
                da.stop();
            }
        }
        finally
        {
            ua.stop();
        }
    }

    public void testDADiscoveryOnStartup() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        daManager.setUnicastConnector(new SocketUnicastConnector());
        da.setConfiguration(configuration);
        da.start();

        try
        {
            sleep(500);

            StandardUserAgent ua = new StandardUserAgent();
            StandardUserAgentManager uaManager = new StandardUserAgentManager();
            ua.setUserAgentManager(uaManager);
            uaManager.setMulticastConnector(new SocketMulticastConnector());
            uaManager.setUnicastConnector(new SocketUnicastConnector());
            ua.setConfiguration(configuration);
            // Discover the DAs immediately
            ua.setDiscoveryStartWaitBound(0);
            ua.start();

            try
            {
                // The multicast convergence should stop after 2 timeouts, but use 3 to be sure
                long[] timeouts = configuration.getMulticastTimeouts();
                long sleep = timeouts[0] + timeouts[1] + timeouts[2];
                sleep(sleep);

                List das = ua.getCachedDirectoryAgents(ua.getScopes());
                assertNotNull(das);
                assertEquals(1, das.size());
            }
            finally
            {
                ua.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }
}
