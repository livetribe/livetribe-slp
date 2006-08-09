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
package org.livetribe.slp.api.da;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPTestSupport;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;
import org.livetribe.slp.spi.sa.StandardServiceAgentManager;
import org.livetribe.slp.spi.ua.StandardUserAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardDirectoryAgentTest extends SLPTestSupport
{
    /**
     * @testng.configuration afterTestMethod="true"
     */
    protected void tearDown() throws Exception
    {
        // Allow ServerSocket to shutdown completely
        sleep(500);
    }

    /**
     * @testng.test
     */
    public void testStartStop() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setPort(getPort());

        assertFalse(da.isRunning());
        da.start();
        assertTrue(da.isRunning());
        da.stop();
        assertFalse(da.isRunning());
        da.start();
        assertTrue(da.isRunning());
        da.stop();
        assertFalse(da.isRunning());
    }

    /**
     * @testng.test
     */
    public void testUnsolicitedDAAdverts() throws Exception
    {
        int port = getPort();

        StandardUserAgentManager ua = new StandardUserAgentManager();
        ua.setPort(port);

        try
        {
            ua.start();

            final AtomicInteger daAdvertCount = new AtomicInteger(0);
            final AtomicReference failure = new AtomicReference(null);
            MessageListener listener = new MessageListener()
            {
                public void handle(MessageEvent event)
                {
                    try
                    {
                        Message message = Message.deserialize(event.getMessageBytes());
                        if (message.getMessageType() == Message.DA_ADVERT_TYPE)
                        {
                            daAdvertCount.incrementAndGet();
                        }
                        else
                        {
                            throw new RuntimeException("Not a DAAdvert " + message);
                        }
                    }
                    catch (Exception x)
                    {
                        failure.set(x);
                    }
                }
            };
            ua.addMessageListener(listener, true);

            sleep(500);

            // Start DA after UA, so that the UA is ready to listen for the UA coming up
            StandardDirectoryAgent da = new StandardDirectoryAgent();
            da.setPort(port);
            int heartBeatPeriod = 1;
            da.setAdvertisementPeriod(heartBeatPeriod);

            try
            {
                da.start();

                int count = 2;
                sleep((heartBeatPeriod * count) * 1000L + 500);

                ua.removeMessageListener(listener, true);

                // There's one more DAAdvert, sent at boot
                assert daAdvertCount.get() == count + 1;
                assert failure.get() == null;
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
    public void testDAAdvertOnShutdown() throws Exception
    {
        int port = getPort();

        StandardUserAgentManager uaManager = new StandardUserAgentManager();
        uaManager.setPort(port);

        try
        {
            uaManager.start();

            StandardDirectoryAgent da = new StandardDirectoryAgent();
            da.setPort(port);
            int heartBeatPeriod = 1;
            da.setAdvertisementPeriod(heartBeatPeriod);
            da.start();

            sleep(500);

            final AtomicInteger daAdvertCount = new AtomicInteger(0);
            final AtomicReference failure = new AtomicReference(null);
            MessageListener listener = new MessageListener()
            {
                public void handle(MessageEvent event)
                {
                    try
                    {
                        Message message = Message.deserialize(event.getMessageBytes());
                        if (message.getMessageType() == Message.DA_ADVERT_TYPE)
                        {
                            if (((DAAdvert)message).getBootTime() == 0)
                            {
                                daAdvertCount.incrementAndGet();
                            }
                            else
                            {
                                throw new RuntimeException("Not a correct DAAdvert " + message);
                            }
                        }
                        else
                        {
                            throw new RuntimeException("Not a correct Message " + message);
                        }
                    }
                    catch (Exception x)
                    {
                        failure.set(x);
                    }
                }
            };
            uaManager.addMessageListener(listener, true);

            da.stop();

            sleep((heartBeatPeriod * 2) * 1000L + 500);

            uaManager.removeMessageListener(listener, true);

            assert daAdvertCount.get() == 1;
            assert failure.get() == null;
        }
        finally
        {
            uaManager.stop();
        }
    }

    /**
     * @testng.test
     */
    public void testDADiscoveryRepliesOnUnicast() throws Exception
    {
        int port = getPort();

        InetAddress localhost = InetAddress.getLocalHost();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setPort(port);

        try
        {
            da.start();
            long afterBoot = System.currentTimeMillis();

            StandardUserAgentManager uaManager = new StandardUserAgentManager();
            uaManager.setPort(port);

            try
            {
                uaManager.start();
                sleep(500);

                DAAdvert[] replies = uaManager.multicastDASrvRqst(Scopes.DEFAULT, null, null, -1);

                assert replies != null;
                assert replies.length == 1;
                DAAdvert reply = replies[0];
                assert reply.getErrorCode() ==0;
                assert reply.getURL().equals("service:directory-agent://" + localhost.getHostAddress());
                assert afterBoot >= reply.getBootTime();
                assert reply.getResponder() != null;
                assert !reply.isMulticast();
            }
            finally
            {
                uaManager.stop();
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
    public void testServiceRegistration() throws Exception
    {
        int port = getPort();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setPort(port);

        try
        {
            da.start();
            InetAddress localhost = InetAddress.getLocalHost();

            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            saManager.setPort(port);

            try
            {
                saManager.start();

                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", 13);
                ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, null);
                ServiceAgentInfo info = new ServiceAgentInfo(null, "service:service-agent://127.0.0.1", null, null, Locale.getDefault().getLanguage());
                SrvAck ack = saManager.tcpSrvReg(localhost, service, info, true);

                assert ack != null;
                assert ack.getErrorCode() == 0;

                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                uaManager.setPort(port);

                try
                {
                    uaManager.start();

                    SrvRply srvRply = uaManager.tcpSrvRqst(localhost, serviceURL.getServiceType(), null, null, null);

                    assert srvRply != null;
                    assert srvRply.getErrorCode() == 0;
                    List urlEntries = srvRply.getURLEntries();
                    assert urlEntries != null;
                    assert urlEntries.size() == 1;
                    URLEntry urlEntry = (URLEntry)urlEntries.get(0);
                    assert serviceURL.getURL().equals(urlEntry.getURL());
                    assert serviceURL.getLifetime() == urlEntry.getLifetime();
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

    /**
     * @testng.test
     */
    public void testServiceUpdate() throws Exception
    {
        int port = getPort();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setPort(port);

        try
        {
            da.start();
            InetAddress localhost = InetAddress.getLocalHost();

            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            saManager.setPort(port);

            try
            {
                saManager.start();

                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_PERMANENT);
                ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());

                ServiceAgentInfo serviceAgent = new ServiceAgentInfo(null, "service:service-agent://127.0.0.1", null, null, null);

                SrvAck ack = saManager.tcpSrvReg(localhost, service, serviceAgent, true);
                assert ack != null;
                assert ack.getErrorCode() == 0;

                // Re-registration with same information must replace service
                ack = saManager.tcpSrvReg(localhost, service, serviceAgent, true);
                assert ack != null;
                assert ack.getErrorCode() == 0;

                // Update with same information must pass
                ack = saManager.tcpSrvReg(localhost, service, serviceAgent, false);
                assert ack != null;
                assert ack.getErrorCode() == 0;

                // Update with different scope must fail
                Scopes wrongScopes = new Scopes(new String[]{"scope"});
                service = new ServiceInfo(serviceURL, wrongScopes, null, null);
                ack = saManager.tcpSrvReg(localhost, service, serviceAgent, false);
                assert ack != null;
                assert ServiceLocationException.SCOPE_NOT_SUPPORTED == ack.getErrorCode();
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
    public void testServiceUpdateOfNonRegisteredService() throws Exception
    {
        int port = getPort();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setPort(port);

        try
        {
            da.start();
            InetAddress localhost = InetAddress.getLocalHost();

            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            saManager.setPort(port);

            try
            {
                saManager.start();

                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_PERMANENT);
                ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
                ServiceAgentInfo info = new ServiceAgentInfo(null, "service:service-agent://127.0.0.1", null, null, null);
                SrvAck ack = saManager.tcpSrvReg(localhost, service, info, false);

                assert ack != null;
                assert ack.getErrorCode() == ServiceLocationException.INVALID_UPDATE;
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
    public void testServiceDeregistration() throws Exception
    {
        int port = getPort();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setPort(port);

        try
        {
            da.start();
            InetAddress localhost = InetAddress.getLocalHost();

            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            saManager.setPort(port);

            try
            {
                saManager.start();

                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_PERMANENT);
                Attributes attributes = new Attributes("(attr=value),tag");
                ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, attributes, Locale.getDefault().getLanguage());

                ServiceAgentInfo serviceAgent = new ServiceAgentInfo(null, "service:service-agent://127.0.0.1", null, null, null);
                SrvAck ack = saManager.tcpSrvReg(localhost, service, serviceAgent, true);
                assert ack != null;
                assert ack.getErrorCode() == 0;

                // Check that deregistration of attributes works
                Attributes attrsModification = new Attributes("tag");
                ServiceInfo update = new ServiceInfo(service.getServiceURL(), service.getScopes(), attrsModification, service.getLanguage());
                ack = saManager.tcpSrvDeReg(localhost, update, serviceAgent);
                assert ack != null;
                assert ack.getErrorCode() == 0;

                // Deregister the service
                ack = saManager.tcpSrvDeReg(localhost, service, serviceAgent);
                assert ack != null;
                assert ack.getErrorCode() == 0;
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
    public void testRegisterServiceInWrongScope() throws Exception
    {
        int port = getPort();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setPort(port);
        da.setScopes(new Scopes(new String[]{"scope1"}));

        try
        {
            da.start();
            InetAddress localhost = InetAddress.getLocalHost();

            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            saManager.setPort(port);

            try
            {
                saManager.start();

                ServiceURL serviceURL = new ServiceURL("service:foo://bar");
                ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
                ServiceAgentInfo serviceAgent = new ServiceAgentInfo(null, "service:service-agent://127.0.0.1", null, null, null);

                SrvAck ack = saManager.tcpSrvReg(localhost, service, serviceAgent, true);
                assert ack != null;
                assert ack.getErrorCode() == ServiceLocationException.SCOPE_NOT_SUPPORTED;
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
    public void testExpiration() throws Exception
    {
        int port = getPort();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setPort(port);

        try
        {
            da.start();
            InetAddress localhost = InetAddress.getLocalHost();

            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            saManager.setPort(port);
            try
            {
                saManager.start();

                int lifetime = 5; // seconds
                ServiceURL serviceURL = new ServiceURL("foo://baz", lifetime);
                ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
                ServiceAgentInfo serviceAgent = new ServiceAgentInfo(null, "service:service-agent://127.0.0.1", null, null, null);

                SrvAck ack = saManager.tcpSrvReg(localhost, service, serviceAgent, true);
                assert ack != null;
                assert ack.getErrorCode() == 0;

                // Wait for the service to expire
                sleep((lifetime + 2) * 1000L);

                assert da.getServices().size() == 0;
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
}
