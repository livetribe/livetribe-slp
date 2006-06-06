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

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPTestSupport;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.ServiceRegistrationEvent;
import org.livetribe.slp.api.ServiceRegistrationListener;
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
public class StandardUserAgentTest extends SLPTestSupport
{
    /**
     * @testng.configuration afterTestMethod="true"
     */
    protected void tearDown() throws Exception
    {
        sleep(500);
    }

    /**
     * @testng.test
     */
    public void testStartStop() throws Exception
    {
        StandardUserAgent ua = new StandardUserAgent();
        StandardUserAgentManager uaManager = new StandardUserAgentManager();
        uaManager.setUDPConnector(new SocketUDPConnector());
        uaManager.setTCPConnector(new SocketTCPConnector());
        ua.setUserAgentManager(uaManager);
        ua.setConfiguration(getDefaultConfiguration());

        assert !ua.isRunning();
        ua.start();
        assert ua.isRunning();
        ua.stop();
        assert !ua.isRunning();
        ua.start();
        assert ua.isRunning();
        ua.stop();
        assert !ua.isRunning();
    }

    /**
     * @testng.test
     */
    public void testFindServices() throws Exception
    {
        Scopes scopes = new Scopes(new String[]{"DEFAULT", "scope1", "scope2"});

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setConfiguration(getDefaultConfiguration());
        da.setScopes(scopes);
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
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///suat1", 13);
                Attributes attributes = new Attributes("(attr=suat1)");
                ServiceInfo service = new ServiceInfo(serviceURL, scopes, attributes, null);
                ServiceAgentInfo info = new ServiceAgentInfo(null, "service:service-agent://127.0.0.1", null, null, Locale.getDefault().getLanguage());
                SrvAck ack = saManager.tcpSrvReg(localhost, service, info, true);

                assert ack != null;
                assert ack.getErrorCode() == 0;

                StandardUserAgent ua = new StandardUserAgent();
                ua.setConfiguration(getDefaultConfiguration());
                ua.start();

                try
                {
                    List serviceInfos = ua.findServices(serviceURL.getServiceType(), scopes, null, null);

                    assert serviceInfos != null;
                    assert serviceInfos.size() == 1;
                    ServiceInfo serviceInfo = (ServiceInfo)serviceInfos.get(0);
                    assert serviceInfo != null;
                    ServiceURL foundService = serviceInfo.getServiceURL();
                    assert foundService != null;
                    assert serviceURL.equals(foundService);
                    assert serviceURL.getLifetime() == foundService.getLifetime();

                    assert serviceInfo.getAttributes() != null;
                    assert serviceInfo.getAttributes().equals(attributes);
                }
                finally
                {
                    ua.stop();
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

    /**
     * @testng.test
     */
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
            assert das != null;
            assert das.isEmpty();

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
                assert das != null;
                assert das.size() == 1;
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

    /**
     * @testng.test
     */
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
                assert das != null;
                assert das.size() == 1;
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

    /**
     * @testng.test
     */
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
        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi://host/suat2", ServiceURL.LIFETIME_DEFAULT);
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

                List serviceInfos = ua.findServices(serviceURL.getServiceType(), null, null, language);
                assert serviceInfos != null;
                assert serviceInfos.size() == 1;
                assert serviceURL.equals(((ServiceInfo)serviceInfos.get(0)).getServiceURL());
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

    /**
     * @testng.test
     */
    public void testDiscoveryOfTwoSA() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardServiceAgent sa1 = new StandardServiceAgent();
        sa1.setConfiguration(configuration);
        sa1.setIdentifier("sa1");
        ServiceURL serviceURL1 = new ServiceURL("service:jmx:rmi://host/suat3", ServiceURL.LIFETIME_DEFAULT);
        String language = Locale.ITALY.getLanguage();
        ServiceInfo service1 = new ServiceInfo(serviceURL1, null, null, language);
        sa1.register(service1);
        sa1.start();

        try
        {
            StandardServiceAgent sa2 = new StandardServiceAgent();
            sa2.setConfiguration(configuration);
            sa2.setIdentifier("sa2");
            ServiceURL serviceURL2 = new ServiceURL("service:jmx:http://host/suat4", ServiceURL.LIFETIME_DEFAULT);
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
                    assert sas.size() == 2;
                    ServiceAgentInfo sai1 = (ServiceAgentInfo)sas.get(0);
                    boolean oneToOne = sa1.getIdentifier().equals(sai1.getIdentifier());
                    ServiceAgentInfo sai2 = (ServiceAgentInfo)sas.get(1);
                    if (oneToOne)
                    {
                        assert sa1.getIdentifier().equals(sai1.getIdentifier());
                        assert sa2.getIdentifier().equals(sai2.getIdentifier());
                    }
                    else
                    {
                        assert sa1.getIdentifier().equals(sai2.getIdentifier());
                        assert sa2.getIdentifier().equals(sai1.getIdentifier());
                    }
                }
                finally
                {
                    ua.stop();
                }
            }
            finally
            {
                sa2.stop();
            }
        }
        finally
        {
            sa1.stop();
        }
    }

    /**
     * @testng.test
     */
    public void testListenForSrvRegSrvDeRegNotifications() throws Exception
    {
        StandardServiceAgent sa = new StandardServiceAgent();
        sa.setIdentifier("sa1");
        sa.setConfiguration(getDefaultConfiguration());
        sa.start();

        try
        {
            StandardUserAgent ua = new StandardUserAgent();
            ua.setConfiguration(getDefaultConfiguration());
            ua.start();

            final AtomicReference registered = new AtomicReference();
            final AtomicReference updated = new AtomicReference();
            final AtomicReference deregistered = new AtomicReference();
            ua.addServiceRegistrationListener(new ServiceRegistrationListener()
            {
                public void serviceRegistered(ServiceRegistrationEvent event)
                {
                    registered.set(event);
                }

                public void serviceUpdated(ServiceRegistrationEvent event)
                {
                    updated.set(event);
                }

                public void serviceDeregistered(ServiceRegistrationEvent event)
                {
                    deregistered.set(event);
                }
            });

            try
            {
                ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz");
                ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
                sa.register(service);

                // Let the event arrive
                sleep(500);

                assert registered.get() != null;
                assert updated.get() == null;
                assert deregistered.get() == null;
                ServiceRegistrationEvent event = (ServiceRegistrationEvent)registered.get();
                assert event.getPreviousServiceInfo() == null;
                assert event.getCurrentServiceInfo().getKey().equals(service.getKey());

                registered.set(null);

                Attributes attributes = new Attributes("(attr=value)");
                service = new ServiceInfo(service.getServiceURL(), service.getScopes(), attributes, service.getLanguage());
                sa.register(service);

                // Let the event arrive
                sleep(500);

                assert registered.get() != null;
                assert deregistered.get() == null;
                event = (ServiceRegistrationEvent)registered.get();
                assert event.getPreviousServiceInfo() == null;
                assert event.getCurrentServiceInfo().getKey().equals(service.getKey());
                assert !event.getCurrentServiceInfo().getAttributes().isEmpty();

                registered.set(null);

                sa.deregister(service);

                // Let the event arrive
                sleep(500);

                assert registered.get() == null;
                assert deregistered.get() != null;
                event = (ServiceRegistrationEvent)deregistered.get();
                assert event.getPreviousServiceInfo() != null;
                assert event.getCurrentServiceInfo() == null;
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
