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
import org.livetribe.slp.api.sa.ServiceInfo;
import org.livetribe.slp.api.sa.StandardServiceAgent;
import org.livetribe.slp.spi.da.StandardDirectoryAgentManager;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.net.SocketTCPConnector;
import org.livetribe.slp.spi.net.SocketUDPConnector;
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
        uaManager.setUDPConnector(new SocketUDPConnector());
        uaManager.setTCPConnector(new SocketTCPConnector());
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
        daManager.setUDPConnector(new SocketUDPConnector());
        daManager.setTCPConnector(new SocketTCPConnector());
        da.setConfiguration(getDefaultConfiguration());
        da.start();

        try
        {
            InetAddress localhost = InetAddress.getLocalHost();

            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            saManager.setUDPConnector(new SocketUDPConnector());
            saManager.setTCPConnector(new SocketTCPConnector());
            saManager.setConfiguration(getDefaultConfiguration());
            saManager.start();

            try
            {
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", 13);
                String[] scopes = new String[]{"scope1", "scope2"};
                ServiceInfo service = new ServiceInfo(serviceURL, scopes, null, null);
                ServiceAgentInfo info = new ServiceAgentInfo(null, "service:service-agent://127.0.0.1", null, null, Locale.getDefault().getLanguage());
                SrvAck ack = saManager.tcpSrvReg(localhost, service, info, true);

                assertNotNull(ack);
                assertEquals(0, ack.getErrorCode());

                StandardUserAgent ua = new StandardUserAgent();
                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                ua.setUserAgentManager(uaManager);
                uaManager.setUDPConnector(new SocketUDPConnector());
                uaManager.setTCPConnector(new SocketTCPConnector());
                ua.setConfiguration(getDefaultConfiguration());
                ua.start();

                try
                {
                    List serviceURLs = ua.findServices(serviceURL.getServiceType(), scopes, null, null);

                    assertNotNull(serviceURLs);
                    assertEquals(1, serviceURLs.size());
                    ServiceURL foundService = (ServiceURL)serviceURLs.get(0);
                    assertNotNull(foundService);
                    assertEquals(serviceURL, foundService);
                    assertEquals(serviceURL.getLifetime(), foundService.getLifetime());
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
        uaManager.setUDPConnector(new SocketUDPConnector());
        uaManager.setTCPConnector(new SocketTCPConnector());
        ua.setConfiguration(configuration);
        ua.start();

        try
        {
            List das = ua.getCachedDirectoryAgents(ua.getScopes(), null);
            assertNotNull(das);
            assertTrue(das.isEmpty());

            StandardDirectoryAgent da = new StandardDirectoryAgent();
            StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
            da.setDirectoryAgentManager(daManager);
            daManager.setUDPConnector(new SocketUDPConnector());
            daManager.setTCPConnector(new SocketTCPConnector());
            da.setConfiguration(getDefaultConfiguration());
            da.start();

            try
            {
                // Allow unsolicited DAAdvert to arrive and UA to cache it
                sleep(500);

                das = ua.getCachedDirectoryAgents(ua.getScopes(), null);
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
        daManager.setUDPConnector(new SocketUDPConnector());
        daManager.setTCPConnector(new SocketTCPConnector());
        da.setConfiguration(configuration);
        da.start();

        try
        {
            sleep(500);

            StandardUserAgent ua = new StandardUserAgent();
            StandardUserAgentManager uaManager = new StandardUserAgentManager();
            ua.setUserAgentManager(uaManager);
            uaManager.setUDPConnector(new SocketUDPConnector());
            uaManager.setTCPConnector(new SocketTCPConnector());
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

                List das = ua.getCachedDirectoryAgents(ua.getScopes(), null);
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

    public void testSADiscoveryAndFindServicesViaTCP() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardServiceAgent sa = new StandardServiceAgent();
        StandardServiceAgentManager saManager = new StandardServiceAgentManager();
        sa.setServiceAgentManager(saManager);
        SocketTCPConnector unicastConnector = new SocketTCPConnector();
        unicastConnector.setTCPListening(true);
        saManager.setTCPConnector(unicastConnector);
        sa.setConfiguration(configuration);
        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi://host/path", ServiceURL.LIFETIME_DEFAULT);
        String language = Locale.ITALY.getLanguage();
        ServiceInfo service = new ServiceInfo(serviceURL, null, null, language);
        sa.register(service);
        sa.start();

        try
        {
            sleep(500);

            StandardUserAgent ua = new StandardUserAgent();
            ua.setConfiguration(configuration);
            ua.start();

            try
            {
                sleep(500);

                List services = ua.findServices(serviceURL.getServiceType(), null, null, language);
                assertNotNull(services);
                assertEquals(1, services.size());
                assertEquals(serviceURL, services.get(0));
            }
            finally
            {
                ua.stop();
            }
        }
        finally
        {
            sa.stop();
        }
    }

    public void testDiscoveryOfTwoSA() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardServiceAgent sa1 = new StandardServiceAgent();
        sa1.setConfiguration(configuration);
        sa1.setIdentifier("sa1");
        ServiceURL serviceURL1 = new ServiceURL("service:jmx:rmi://host/path", ServiceURL.LIFETIME_DEFAULT);
        String language = Locale.ITALY.getLanguage();
        ServiceInfo service1 = new ServiceInfo(serviceURL1, null, null, language);
        sa1.register(service1);
        sa1.start();

        StandardServiceAgent sa2 = new StandardServiceAgent();
        sa2.setConfiguration(configuration);
        sa2.setIdentifier("sa2");
        ServiceURL serviceURL2 = new ServiceURL("service:jmx:http://host/path", ServiceURL.LIFETIME_DEFAULT);
        ServiceInfo service2 = new ServiceInfo(serviceURL2, null, null, language);
        sa2.register(service2);
        sa2.start();

        try
        {
            sleep(500);

            StandardUserAgent ua = new StandardUserAgent();
            ua.setConfiguration(configuration);
            ua.start();

            try
            {
                sleep(500);

                List sas = ua.findServiceAgents(null, null);
                assertEquals(2, sas.size());
                ServiceAgentInfo sai1 = (ServiceAgentInfo)sas.get(0);
                boolean oneToOne = sa1.getIdentifier().equals(sai1.getIdentifier());
                ServiceAgentInfo sai2 = (ServiceAgentInfo)sas.get(1);
                if (oneToOne)
                {
                    assertEquals(sa1.getIdentifier(), sai1.getIdentifier());
                    assertEquals(sa2.getIdentifier(), sai2.getIdentifier());
                }
                else
                {
                    assertEquals(sa2.getIdentifier(), sai1.getIdentifier());
                    assertEquals(sa1.getIdentifier(), sai2.getIdentifier());
                }
            }
            finally
            {
                ua.stop();
            }
        }
        finally
        {
            sa1.stop();
        }
    }

/*
    public void testSADiscoveryAndFindServicesViaUDPWithOneSA() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardServiceAgent sa = new StandardServiceAgent();
        sa.setConfiguration(configuration);
        Attributes attributes = new Attributes();
        attributes.put(ServiceAgentInfo.SRVRQST_PROTOCOL_TAG, "multicast");
        sa.setAttributes(attributes);
        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi://host/path", ServiceURL.LIFETIME_DEFAULT);
        String language = Locale.ITALY.getLanguage();
        ServiceInfo service = new ServiceInfo(serviceURL, null, null, language);
        sa.register(service);
        sa.start();

        try
        {
            sleep(500);

            StandardUserAgent ua = new StandardUserAgent();
            ua.setConfiguration(configuration);
            ua.start();

            try
            {
                sleep(500);

                List services = ua.findServices(serviceURL.getServiceType(), null, null, language);
                assertNotNull(services);
                assertEquals(1, services.size());
                assertEquals(serviceURL, services.get(0));
            }
            finally
            {
                ua.stop();
            }
        }
        finally
        {
            sa.stop();
        }
    }

    public void testSADiscoveryAndFindServicesViaUDPWithTwoSA() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardServiceAgent sa1 = new StandardServiceAgent();
        sa1.setConfiguration(configuration);
        Attributes attributes = new Attributes();
        attributes.put(ServiceAgentInfo.ID_TAG, "sa1");
        attributes.put(ServiceAgentInfo.SRVRQST_PROTOCOL_TAG, "multicast");
        sa1.setAttributes(attributes);
        ServiceURL serviceURL1 = new ServiceURL("service:jmx:rmi://host/path", ServiceURL.LIFETIME_DEFAULT);
        String language = Locale.ITALY.getLanguage();
        ServiceInfo service1 = new ServiceInfo(serviceURL1, null, null, language);
        sa1.register(service1);
        sa1.start();

        StandardServiceAgent sa2 = new StandardServiceAgent();
        sa2.setConfiguration(configuration);
        attributes = new Attributes();
        attributes.put(ServiceAgentInfo.ID_TAG, "sa2");
        attributes.put(ServiceAgentInfo.SRVRQST_PROTOCOL_TAG, "multicast");
        sa2.setAttributes(attributes);
        ServiceURL serviceURL2 = new ServiceURL("service:jmx:http://host/path", ServiceURL.LIFETIME_DEFAULT);
        ServiceInfo service2 = new ServiceInfo(serviceURL2, null, null, language);
        sa2.register(service2);
        sa2.start();

        try
        {
            sleep(500);

            StandardUserAgent ua = new StandardUserAgent();
            ua.setConfiguration(configuration);
            ua.start();

            try
            {
                sleep(500);

                ServiceType generic = new ServiceType("service:jmx");
                List services = ua.findServices(generic, null, null, language);
                assertNotNull(services);
                assertEquals(2, services.size());
                Set urls = new HashSet();
                urls.add(serviceURL1);
                urls.add(serviceURL2);
                assertEquals(urls, new HashSet(services));
            }
            finally
            {
                ua.stop();
            }
        }
        finally
        {
            sa1.stop();
        }
    }
*/
}
