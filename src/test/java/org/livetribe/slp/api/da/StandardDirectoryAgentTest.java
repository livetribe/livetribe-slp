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
import java.util.Locale;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
import org.livetribe.slp.api.SLPAPITestCase;
import org.livetribe.slp.spi.da.StandardDirectoryAgentManager;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.net.SocketMulticastConnector;
import org.livetribe.slp.spi.net.SocketUnicastConnector;
import org.livetribe.slp.spi.sa.StandardServiceAgentManager;
import org.livetribe.slp.spi.ua.StandardUserAgentManager;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Rev$ $Date$
 */
public class StandardDirectoryAgentTest extends SLPAPITestCase
{
    protected void tearDown() throws Exception
    {
        // Allow ServerSocket to shutdown completely
        sleep(500);
    }

    public void testStartStop() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        daManager.setMulticastConnector(new SocketMulticastConnector());
        daManager.setUnicastConnector(new SocketUnicastConnector());
        da.setDirectoryAgentManager(daManager);
        da.setConfiguration(getDefaultConfiguration());

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

    public void testUnsolicitedDAAdverts() throws Exception
    {
        StandardUserAgentManager ua = new StandardUserAgentManager();
        ua.setMulticastConnector(new SocketMulticastConnector());
        ua.setConfiguration(getDefaultConfiguration());
        ua.start();

        try
        {
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
            StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
            daManager.setMulticastConnector(new SocketMulticastConnector());
            daManager.setUnicastConnector(new SocketUnicastConnector());
            da.setDirectoryAgentManager(daManager);
            da.setConfiguration(getDefaultConfiguration());
            int heartBeatPeriod = 1;
            da.setHeartBeat(heartBeatPeriod);
            da.start();

            try
            {
                int count = 2;
                sleep((heartBeatPeriod * count) * 1000L + 500);

                ua.removeMessageListener(listener, true);

                // There's one more DAAdvert, sent at boot
                assertEquals(count + 1, daAdvertCount.get());
                assertNull(failure.get());
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

    public void testDAAdvertOnShutdown() throws Exception
    {
        StandardUserAgentManager uaManager = new StandardUserAgentManager();
        uaManager.setMulticastConnector(new SocketMulticastConnector());
        uaManager.setConfiguration(getDefaultConfiguration());
        uaManager.start();

        try
        {
            StandardDirectoryAgent da = new StandardDirectoryAgent();
            StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
            daManager.setMulticastConnector(new SocketMulticastConnector());
            daManager.setUnicastConnector(new SocketUnicastConnector());
            da.setDirectoryAgentManager(daManager);
            da.setConfiguration(getDefaultConfiguration());
            int heartBeatPeriod = 1;
            da.setHeartBeat(heartBeatPeriod);
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

            assertEquals(1, daAdvertCount.get());
            assertNull(failure.get());
        }
        finally
        {
            uaManager.stop();
        }
    }

    public void testDADiscoveryRepliesOnUnicast() throws Exception
    {
        InetAddress localhost = InetAddress.getLocalHost();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        SocketUnicastConnector daUnicastConnector = new SocketUnicastConnector();
        daManager.setUnicastConnector(daUnicastConnector);
        da.setConfiguration(getDefaultConfiguration());
        // Avoid that replies are sent to the DA listening on unicast
        daUnicastConnector.setUnicastListening(false);
        da.start();

        try
        {
            long afterBoot = System.currentTimeMillis();

            StandardUserAgentManager uaManager = new StandardUserAgentManager();
            uaManager.setMulticastConnector(new SocketMulticastConnector());
            SocketUnicastConnector uaUnicastConnector = new SocketUnicastConnector();
            uaManager.setUnicastConnector(uaUnicastConnector);
            uaManager.setConfiguration(getDefaultConfiguration());
            uaUnicastConnector.setUnicastListening(true);
            uaManager.start();

            try
            {
                sleep(500);

                DAAdvert[] replies = uaManager.multicastDASrvRqst(new String[]{"DEFAULT"}, null, -1);

                assertNotNull(replies);
                assertEquals(1, replies.length);
                DAAdvert reply = replies[0];
                assertEquals(0, reply.getErrorCode());
                assertEquals("service:directory-agent://" + localhost.getHostAddress(), reply.getURL());
                assertTrue(afterBoot >= reply.getBootTime());
                assertNotNull(reply.getResponder());
                assertFalse(reply.isMulticast());
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

    public void testDADiscoveryRepliesOnMulticast() throws Exception
    {
        InetAddress localhost = InetAddress.getLocalHost();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        SocketUnicastConnector daUnicastConnector = new SocketUnicastConnector();
        daManager.setUnicastConnector(daUnicastConnector);
        da.setConfiguration(getDefaultConfiguration());
        // Avoid that replies are sent to the DA listening on unicast
        daUnicastConnector.setUnicastListening(false);
        da.start();

        try
        {
            long afterBoot = System.currentTimeMillis();

            StandardServiceAgentManager sa = new StandardServiceAgentManager();
            sa.setMulticastConnector(new SocketMulticastConnector());
            sa.setConfiguration(getDefaultConfiguration());
            sa.start();

            try
            {
                sleep(500);

                DAAdvert[] replies = sa.multicastDASrvRqst(new String[]{"DEFAULT"}, null, -1);

                assertNotNull(replies);
                assertEquals(1, replies.length);
                DAAdvert reply = replies[0];
                assertEquals(0, reply.getErrorCode());
                assertEquals("service:directory-agent://" + localhost.getHostAddress(), reply.getURL());
                assertTrue(afterBoot >= reply.getBootTime());
                assertNotNull(reply.getResponder());
                assertTrue(reply.isMulticast());
            }
            finally
            {
                sa.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }

    public void testServiceRegistration() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        SocketUnicastConnector daUnicastConnector = new SocketUnicastConnector();
        daManager.setUnicastConnector(daUnicastConnector);
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
                SrvAck ack = saManager.unicastSrvReg(localhost, serviceURL.getServiceType(), serviceURL, true, scopes, null, null);

                assertNotNull(ack);
                assertEquals(0, ack.getErrorCode());

                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                uaManager.setMulticastConnector(new SocketMulticastConnector());
                uaManager.setUnicastConnector(new SocketUnicastConnector());
                uaManager.setConfiguration(getDefaultConfiguration());
                uaManager.start();

                try
                {
                    SrvRply srvRply = uaManager.unicastSrvRqst(localhost, serviceURL.getServiceType(), scopes, null);

                    assertNotNull(srvRply);
                    assertEquals(0, srvRply.getErrorCode());
                    URLEntry[] urlEntries = srvRply.getURLEntries();
                    assertNotNull(urlEntries);
                    assertEquals(1, urlEntries.length);
                    URLEntry urlEntry = urlEntries[0];
                    assertEquals(serviceURL.toString(), urlEntry.getURL());
                    assertEquals(serviceURL.getLifetime(), urlEntry.getLifetime());
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

    public void testServiceUpdate() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        SocketUnicastConnector daUnicastConnector = new SocketUnicastConnector();
        daManager.setUnicastConnector(daUnicastConnector);
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
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_PERMANENT);
                String[] scopes = new String[]{"scope1", "scope2"};

                String language = Locale.getDefault().getCountry();

                SrvAck ack = saManager.unicastSrvReg(localhost, serviceURL.getServiceType(), serviceURL, true, scopes, null, language);
                assertNotNull(ack);
                assertEquals(0, ack.getErrorCode());

                // Re-registration with same information must replace service
                ack = saManager.unicastSrvReg(localhost, serviceURL.getServiceType(), serviceURL, true, scopes, null, language);
                assertNotNull(ack);
                assertEquals(0, ack.getErrorCode());

                // Update with same information must pass
                ack = saManager.unicastSrvReg(localhost, serviceURL.getServiceType(), serviceURL, false, scopes, null, language);
                assertNotNull(ack);
                assertEquals(0, ack.getErrorCode());

                // Update with different scope must fail
                ack = saManager.unicastSrvReg(localhost, serviceURL.getServiceType(), serviceURL, false, new String[]{"scope"}, null, language);
                assertNotNull(ack);
                assertEquals(ServiceLocationException.SCOPE_NOT_SUPPORTED, ack.getErrorCode());
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

    public void testServiceUpdateOfNonRegisteredService() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        SocketUnicastConnector daUnicastConnector = new SocketUnicastConnector();
        daManager.setUnicastConnector(daUnicastConnector);
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
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_PERMANENT);
                SrvAck ack = saManager.unicastSrvReg(localhost, serviceURL.getServiceType(), serviceURL, false, new String[]{"scope1", "scope2"}, null, Locale.getDefault().getCountry());

                assertNotNull(ack);
                assertEquals(ServiceLocationException.INVALID_UPDATE, ack.getErrorCode());
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

    public void testServiceDeregistration() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        SocketUnicastConnector daUnicastConnector = new SocketUnicastConnector();
        daManager.setUnicastConnector(daUnicastConnector);
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
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_PERMANENT);
                String[] scopes = new String[]{"scope1", "scope2"};

                SrvAck ack = saManager.unicastSrvReg(localhost, serviceURL.getServiceType(), serviceURL, true, scopes, null, Locale.getDefault().getCountry());
                assertNotNull(ack);
                assertEquals(0, ack.getErrorCode());

                ack = saManager.unicastSrvDeReg(localhost, serviceURL, scopes, null);
                assertNotNull(ack);
                assertEquals(0, ack.getErrorCode());

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
