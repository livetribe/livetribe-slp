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
package org.livetribe.slp.spi;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Random;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.Rply;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageTooBigException;
import org.livetribe.slp.spi.net.SocketClosedException;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * @version $Rev$ $Date$
 */
public class StandardAgentManagerTest extends SLPSPITestCase
{
    public void testMulticastConvergenceNoReplies() throws Exception
    {
        StandardAgentManager agent = new Agent();
        agent.setUDPConnector(new MCConnector(0, new long[]{20L}));
        agent.setTCPConnector(new UCConnector());
        agent.setConfiguration(getDefaultConfiguration());

        long[] timeouts = new long[]{100L, 200L, 300L, 400L, 500L};
        agent.setMulticastTimeouts(timeouts);
        agent.setMulticastMaxWait(sum(timeouts));

        agent.start();

        try
        {
            SrvRqst rqst = new SrvRqst();
            rqst.setServiceType(new ServiceType("service:test"));

            long start = System.currentTimeMillis();
            Listener converger = new Listener();
            try
            {
                agent.addMessageListener(converger, true);
                List replies = agent.convergentMulticastSend(rqst, -1, converger);
                agent.removeMessageListener(converger, true);
                long end = System.currentTimeMillis();

                assertNotNull(replies);
                assertEquals(0, replies.size());
                long lowerBound = timeouts[0] + timeouts[1];
                long upperBound = lowerBound + timeouts[2];
                long elapsed = end - start;
                assertTrue(elapsed >= lowerBound);
                assertTrue(elapsed < upperBound);
            }
            finally
            {
                converger.close();
            }
        }
        finally
        {
            agent.stop();
        }
    }

    public void testMulticastConvergenceOneReply() throws Exception
    {
        StandardAgentManager agent = new Agent();
        agent.setUDPConnector(new MCConnector(1, new long[]{20L}));
        agent.setTCPConnector(new UCConnector());
        agent.setConfiguration(getDefaultConfiguration());

        long[] timeouts = new long[]{100L, 200L, 300L, 400L, 500L};
        agent.setMulticastTimeouts(timeouts);
        agent.setMulticastMaxWait(sum(timeouts));

        agent.start();

        try
        {
            SrvRqst rqst = new SrvRqst();
            rqst.setServiceType(new ServiceType("service:test"));

            long start = System.currentTimeMillis();
            Listener converger = new Listener();
            try
            {
                agent.addMessageListener(converger, true);
                List replies = agent.convergentMulticastSend(rqst, -1, converger);
                agent.removeMessageListener(converger, true);
                long end = System.currentTimeMillis();

                assertNotNull(replies);
                assertEquals(1, replies.size());
                long lowerBound = timeouts[0] + timeouts[1];
                long upperBound = lowerBound + timeouts[2];
                long elapsed = end - start;
                assertTrue(elapsed >= lowerBound);
                assertTrue(elapsed < upperBound);
            }
            finally
            {
                converger.close();
            }
        }
        finally
        {
            agent.stop();
        }
    }

    public void testMulticastConvergenceOneReplyAfterOneTimeout() throws Exception
    {
        long[] timeouts = new long[]{100L, 200L, 300L, 400L, 500L};

        StandardAgentManager agent = new Agent();
        agent.setUDPConnector(new MCConnector(1, new long[]{timeouts[0] + 20L}));
        agent.setTCPConnector(new UCConnector());
        agent.setConfiguration(getDefaultConfiguration());

        agent.setMulticastTimeouts(timeouts);
        agent.setMulticastMaxWait(sum(timeouts));

        agent.start();

        try
        {
            SrvRqst rqst = new SrvRqst();
            rqst.setServiceType(new ServiceType("service:test"));

            long start = System.currentTimeMillis();
            Listener converger = new Listener();
            try
            {
                agent.addMessageListener(converger, true);
                List replies = agent.convergentMulticastSend(rqst, -1, converger);
                agent.removeMessageListener(converger, true);
                long end = System.currentTimeMillis();

                assertNotNull(replies);
                assertEquals(1, replies.size());
                long lowerBound = timeouts[1] + timeouts[2];
                long upperBound = lowerBound + timeouts[3];
                long elapsed = end - start;
                assertTrue(elapsed >= lowerBound);
                assertTrue(elapsed < upperBound);
            }
            finally
            {
                converger.close();
            }
        }
        finally
        {
            agent.stop();
        }
    }

    public void testMulticastConvergenceTwoReplies() throws Exception
    {
        StandardAgentManager agent = new Agent();
        agent.setUDPConnector(new MCConnector(2, new long[]{20L, 20L}));
        agent.setTCPConnector(new UCConnector());
        agent.setConfiguration(getDefaultConfiguration());

        long[] timeouts = new long[]{100L, 200L, 300L, 400L, 500L};
        agent.setMulticastTimeouts(timeouts);
        agent.setMulticastMaxWait(sum(timeouts));

        agent.start();

        try
        {
            SrvRqst rqst = new SrvRqst();
            rqst.setServiceType(new ServiceType("service:test"));

            long start = System.currentTimeMillis();
            Listener converger = new Listener();
            try
            {
                agent.addMessageListener(converger, true);
                List replies = agent.convergentMulticastSend(rqst, -1, converger);
                agent.removeMessageListener(converger, true);
                long end = System.currentTimeMillis();

                assertNotNull(replies);
                assertEquals(2, replies.size());
                long lowerBound = timeouts[0] + timeouts[1];
                long upperBound = lowerBound + timeouts[2];
                long elapsed = end - start;
                assertTrue(elapsed >= lowerBound);
                assertTrue(elapsed < upperBound);
            }
            finally
            {
                converger.close();
            }
        }
        finally
        {
            agent.stop();
        }
    }

    public void testMulticastConvergenceFirstReplyTimeoutSecondReply() throws Exception
    {
        long[] timeouts = new long[]{100L, 200L, 300L, 400L, 500L};

        StandardAgentManager agent = new Agent();
        agent.setUDPConnector(new MCConnector(2, new long[]{20L, timeouts[0] + 20L}));
        agent.setTCPConnector(new UCConnector());
        agent.setConfiguration(getDefaultConfiguration());

        agent.setMulticastTimeouts(timeouts);
        agent.setMulticastMaxWait(sum(timeouts));

        agent.start();

        try
        {
            SrvRqst rqst = new SrvRqst();
            rqst.setServiceType(new ServiceType("service:test"));

            long start = System.currentTimeMillis();
            Listener converger = new Listener();
            try
            {
                agent.addMessageListener(converger, true);
                List replies = agent.convergentMulticastSend(rqst, -1, converger);
                agent.removeMessageListener(converger, true);
                long end = System.currentTimeMillis();

                assertNotNull(replies);
                assertEquals(2, replies.size());
                long lowerBound = timeouts[1] + timeouts[2];
                long upperBound = lowerBound + timeouts[3];
                long elapsed = end - start;
                assertTrue(elapsed >= lowerBound);
                assertTrue(elapsed < upperBound);
            }
            finally
            {
                converger.close();
            }
        }
        finally
        {
            agent.stop();
        }
    }

    private long sum(long[] longs)
    {
        long result = 0;
        for (int i = 0; i < longs.length; ++i) result += longs[i];
        return result;
    }

    private class Agent extends StandardAgentManager
    {
    }

    private class MCConnector extends UDPConnector
    {
        private final AtomicInteger sent;
        private final long[] waitTimes;
        private final Random random = new Random(System.currentTimeMillis());

        public MCConnector(int numberOfReplies, long[] waitTimes)
        {
            this.sent = new AtomicInteger(numberOfReplies);
            this.waitTimes = waitTimes;
        }

        protected Runnable[] createAcceptors() throws IOException
        {
            return null;
        }

        protected void destroyAcceptors() throws IOException
        {
        }

        public void accept(Runnable executor)
        {
            // Do nothing, receiving of messages is triggered by send()
        }

        public DatagramSocket unicastSend(DatagramSocket socket, InetSocketAddress address, byte[] bytes) throws IOException
        {
            throw new AssertionError("BUG: this method should not be called");
        }

        public DatagramSocket multicastSend(DatagramSocket socket, byte[] bytes) throws IOException
        {
            if (sent.getAndDecrement() > 0)
            {
                // Use a separate thread so that the send() returns
                // immediately, as it would in a normal situation
                new Thread(new Runnable()
                {
                    public void run()
                    {
                        sleep(waitTimes[waitTimes.length - 1 - sent.get()]);

                        try
                        {
                            SrvRply reply = new SrvRply();
                            byte[] replyData = reply.serialize();
                            MessageEvent event = new MessageEvent(this, replyData, new InetSocketAddress("127.0.0." + (random.nextInt(255) + 1), 427));
                            notifyMessageListeners(event);
                        }
                        catch (ServiceLocationException x)
                        {
                            throw new AssertionError(x);
                        }
                    }
                }).start();
            }
            return socket;
        }
    }

    private class UCConnector extends TCPConnector
    {
        protected Runnable[] createAcceptors() throws IOException
        {
            return null;
        }

        protected void destroyAcceptors() throws IOException
        {
        }

        public byte[] receive(Socket socket) throws MessageTooBigException, SocketClosedException, IOException
        {
            return null;
        }

        public Socket send(byte[] messageBytes, InetAddress address, boolean closeSocket) throws ConnectException, IOException
        {
            return null;
        }

        public void reply(Socket socket, byte[] messageBytes) throws IOException
        {
        }
    }

    private class Listener extends Converger
    {
        public Listener() throws SocketException
        {
        }

        public void send(UDPConnector connector, byte[] bytes) throws IOException
        {
            connector.multicastSend(getDatagramSocket(), bytes);
        }

        public void handle(MessageEvent event)
        {
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());
                lock();
                try
                {
                    ((Rply)message).setResponder(event.getSocketAddress().getAddress().getHostAddress());
                    add(message);
                }
                finally
                {
                    unlock();
                }
            }
            catch (ServiceLocationException e)
            {
                throw new AssertionError();
            }
        }
    }
}
