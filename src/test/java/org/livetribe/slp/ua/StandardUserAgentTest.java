/*
 * Copyright 2006-2008 the original author or authors
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
package org.livetribe.slp.ua;

/**
 * @version $Rev$ $Date$
 */
public class StandardUserAgentTest
{
//    /**
//     * @testng.configuration afterTestMethod="true"
//     */
//    protected void tearDown() throws Exception
//    {
//        sleep(500);
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testStartStop() throws Exception
//    {
//        int port = getPort();
//
//        StandardUserAgentServer ua = new StandardUserAgentServer();
//        ua.setPort(port);
//
//        assert !ua.isRunning();
//        ua.start();
//        assert ua.isRunning();
//        ua.stop();
//        assert !ua.isRunning();
//        ua.start();
//        assert ua.isRunning();
//        ua.stop();
//        assert !ua.isRunning();
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testFindServices() throws Exception
//    {
//        int port = getPort();
//        Scopes scopes = new Scopes(new String[]{"DEFAULT", "scope1", "scope2"});
//
//        StandardDirectoryAgentServer da = new StandardDirectoryAgentServer();
//        da.setPort(port);
//        da.setScopes(scopes);
//
//        try
//        {
//            da.start();
//
//            InetAddress localhost = InetAddress.getLocalHost();
//
//            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
//            saManager.setPort(port);
//
//            try
//            {
//                saManager.start();
//
//                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///suat1", 13);
//                Attributes attributes = new Attributes("(attr=suat1)");
//                ServiceInfo service = new ServiceInfo(serviceURL, scopes, attributes, null);
//                ServiceAgentInfo info = new ServiceAgentInfo(null, "service:service-agent://127.0.0.1", null, null, Locale.getDefault().getLanguage());
//                SrvAck ack = saManager.tcpSrvReg(localhost, service, info, true);
//
//                assert ack != null;
//                assert ack.getErrorCode() == 0;
//
//                StandardUserAgentServer ua = new StandardUserAgentServer();
//                ua.setPort(port);
//
//                try
//                {
//                    ua.start();
//
//                    List serviceInfos = ua.findServices(serviceURL.getServiceType(), scopes, null, null);
//
//                    assert serviceInfos != null;
//                    assert serviceInfos.size() == 1;
//                    ServiceInfo serviceInfo = (ServiceInfo)serviceInfos.get(0);
//                    assert serviceInfo != null;
//                    ServiceURL foundService = serviceInfo.getServiceURL();
//                    assert foundService != null;
//                    assert serviceURL.equals(foundService);
//                    assert serviceURL.getLifetime() == foundService.getLifetime();
//
//                    assert serviceInfo.getAttributes() != null;
//                    assert serviceInfo.getAttributes().equals(attributes);
//                }
//                finally
//                {
//                    ua.stop();
//                }
//            }
//            finally
//            {
//                saManager.stop();
//            }
//        }
//        finally
//        {
//            da.stop();
//        }
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testListenForDAAdverts() throws Exception
//    {
//        int port = getPort();
//
//        StandardUserAgentServer ua = new StandardUserAgentServer();
//        ua.setPort(port);
//
//        try
//        {
//            ua.start();
//
//            List das = ua.getCachedDirectoryAgents(ua.getScopes(), null);
//            assert das != null;
//            assert das.isEmpty();
//
//            StandardDirectoryAgentServer da = new StandardDirectoryAgentServer();
//            da.setPort(port);
//
//            try
//            {
//                da.start();
//
//                // Allow unsolicited DAAdvert to arrive and UA to cache it
//                sleep(500);
//
//                das = ua.getCachedDirectoryAgents(ua.getScopes(), null);
//                assert das != null;
//                assert das.size() == 1;
//            }
//            finally
//            {
//                da.stop();
//            }
//        }
//        finally
//        {
//            ua.stop();
//        }
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testDADiscoveryOnStartup() throws Exception
//    {
//        int port = getPort();
//
//        StandardDirectoryAgentServer da = new StandardDirectoryAgentServer();
//        da.setPort(port);
//
//        try
//        {
//            da.start();
//            sleep(500);
//
//            StandardUserAgentServer ua = new StandardUserAgentServer();
//            StandardUserAgentManager uaManager = new StandardUserAgentManager();
//            ua.setUserAgentManager(uaManager);
//            ua.setPort(port);
//            // Discover the DAs immediately
//            ua.setDiscoveryInitialWaitBound(0);
//
//            try
//            {
//                ua.start();
//
//                // The multicast convergence should stop after 2 timeouts, but use 3 to be sure
//                long[] timeouts = uaManager.getMulticastTimeouts();
//                long sleep = timeouts[0] + timeouts[1] + timeouts[2];
//                sleep(sleep);
//
//                List das = ua.getCachedDirectoryAgents(ua.getScopes(), null);
//                assert das != null;
//                assert das.size() == 1;
//            }
//            finally
//            {
//                ua.stop();
//            }
//        }
//        finally
//        {
//            da.stop();
//        }
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testSADiscoveryAndFindServicesViaTCP() throws Exception
//    {
//        int port = getPort();
//
//        StandardServiceAgentServer sa = new StandardServiceAgentServer();
//        StandardServiceAgentManager saManager = new StandardServiceAgentManager();
//        saManager.setTCPListening(true);
//        sa.setServiceAgentManager(saManager);
//        sa.setPort(port);
//        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi://host/suat2", ServiceURL.LIFETIME_DEFAULT);
//        String language = Locale.ITALY.getLanguage();
//        ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, language);
//        sa.register(service);
//
//        try
//        {
//            sa.start();
//            sleep(500);
//
//            StandardUserAgentServer ua = new StandardUserAgentServer();
//            ua.setPort(port);
//
//            try
//            {
//                ua.start();
//                sleep(500);
//
//                List serviceInfos = ua.findServices(serviceURL.getServiceType(), null, null, language);
//                assert serviceInfos != null;
//                assert serviceInfos.size() == 1;
//                assert serviceURL.equals(((ServiceInfo)serviceInfos.get(0)).getServiceURL());
//            }
//            finally
//            {
//                ua.stop();
//            }
//        }
//        finally
//        {
//            sa.stop();
//        }
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testDiscoveryOfTwoSA() throws Exception
//    {
//        int port = getPort();
//
//        StandardServiceAgentServer sa1 = new StandardServiceAgentServer();
//        sa1.setPort(port);
//        ServiceURL serviceURL1 = new ServiceURL("service:jmx:rmi://host/suat3", ServiceURL.LIFETIME_DEFAULT);
//        String language = Locale.ITALY.getLanguage();
//        ServiceInfo service1 = new ServiceInfo(serviceURL1, Scopes.DEFAULT, null, language);
//        sa1.register(service1);
//
//        try
//        {
//            sa1.start();
//
//            StandardServiceAgentServer sa2 = new StandardServiceAgentServer();
//            sa2.setPort(port);
//            ServiceURL serviceURL2 = new ServiceURL("service:jmx:http://host/suat4", ServiceURL.LIFETIME_DEFAULT);
//            ServiceInfo service2 = new ServiceInfo(serviceURL2, Scopes.DEFAULT, null, language);
//            sa2.register(service2);
//
//            try
//            {
//                sa2.start();
//                sleep(500);
//
//                StandardUserAgentServer ua = new StandardUserAgentServer();
//                ua.setPort(port);
//
//                try
//                {
//                    ua.start();
//                    sleep(500);
//
//                    List sas = ua.findServiceAgents(null, null);
//                    assert sas.size() == 2;
//                    ServiceAgentInfo sai1 = (ServiceAgentInfo)sas.get(0);
//                    boolean oneToOne = sa1.getIdentifier().equals(sai1.getIdentifier());
//                    ServiceAgentInfo sai2 = (ServiceAgentInfo)sas.get(1);
//                    if (oneToOne)
//                    {
//                        assert sa1.getIdentifier().equals(sai1.getIdentifier());
//                        assert sa2.getIdentifier().equals(sai2.getIdentifier());
//                    }
//                    else
//                    {
//                        assert sa1.getIdentifier().equals(sai2.getIdentifier());
//                        assert sa2.getIdentifier().equals(sai1.getIdentifier());
//                    }
//                }
//                finally
//                {
//                    ua.stop();
//                }
//            }
//            finally
//            {
//                sa2.stop();
//            }
//        }
//        finally
//        {
//            sa1.stop();
//        }
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testListenForSrvRegSrvDeRegNotifications() throws Exception
//    {
//        int port = getPort();
//
//        StandardServiceAgentServer sa = new StandardServiceAgentServer();
//        sa.setPort(port);
//
//        try
//        {
//            sa.start();
//
//            StandardUserAgentServer ua = new StandardUserAgentServer();
//            ua.setPort(port);
//
//            final AtomicReference registered = new AtomicReference();
//            final AtomicReference deregistered = new AtomicReference();
//            ua.addMessageRegistrationListener(new ServiceRegistrationListener()
//            {
//                public void serviceRegistered(SrvReg srvReg)
//                {
//                    registered.set(srvReg);
//                }
//
//                public void serviceDeregistered(SrvDeReg srvDeReg)
//                {
//                    deregistered.set(srvDeReg);
//                }
//            });
//
//            try
//            {
//                ua.start();
//
//                ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz");
//                ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
//                sa.register(service);
//
//                // Let the event arrive
//                sleep(500);
//
//                assert registered.get() != null;
//                assert deregistered.get() == null;
//                SrvReg srvReg = (SrvReg)registered.get();
//                assert srvReg.getURLEntry().toServiceURL().equals(service.getServiceURL());
//                assert srvReg.getLanguage().equals(service.getLanguage());
//
//                registered.set(null);
//
//                Attributes attributes = new Attributes("(attr=value)");
//                service = new ServiceInfo(service.getServiceURL(), service.getScopes(), attributes, service.getLanguage());
//                sa.register(service);
//
//                // Let the event arrive
//                sleep(500);
//
//                assert registered.get() != null;
//                assert deregistered.get() == null;
//                srvReg = (SrvReg)registered.get();
//                assert srvReg.getURLEntry().toServiceURL().equals(service.getServiceURL());
//                assert srvReg.getLanguage().equals(service.getLanguage());
//                assert !srvReg.getAttributes().isEmpty();
//
//                registered.set(null);
//
//                sa.deregister(service);
//
//                // Let the event arrive
//                sleep(500);
//
//                assert registered.get() == null;
//                assert deregistered.get() != null;
//                SrvDeReg srvDeReg = (SrvDeReg)deregistered.get();
//                assert srvDeReg.getURLEntry().toServiceURL().equals(service.getServiceURL());
//                assert srvDeReg.getLanguage().equals(service.getLanguage());
//            }
//            finally
//            {
//                ua.stop();
//            }
//        }
//        finally
//        {
//            sa.stop();
//        }
//    }
}
