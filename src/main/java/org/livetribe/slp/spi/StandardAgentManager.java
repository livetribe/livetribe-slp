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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Condition;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.Rply;
import org.livetribe.slp.spi.msg.Rqst;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.SAAdvert;
import org.livetribe.slp.spi.net.MulticastConnector;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.net.UnicastConnector;
import org.livetribe.slp.spi.net.MessageEvent;

/**
 * @version $Rev$ $Date$
 */
public abstract class StandardAgentManager implements AgentManager
{
    protected Logger logger = Logger.getLogger(getClass().getName());

    private long multicastMaxWait;
    private long[] multicastTimeouts;
    private int maxTransmissionUnit;
    private int port;
    private volatile boolean running;
    private MulticastConnector multicastConnector;
    private UnicastConnector unicastConnector;
    private final Random random = new Random(System.currentTimeMillis());

    public void setConfiguration(Configuration configuration) throws IOException
    {
        setMulticastMaxWait(configuration.getMulticastMaxWait());
        setMulticastTimeouts(configuration.getMulticastTimeouts());
        setMaxTransmissionUnit(configuration.getMTU());
        setPort(configuration.getPort());

        if (multicastConnector != null) multicastConnector.setConfiguration(configuration);
        if (unicastConnector != null) unicastConnector.setConfiguration(configuration);
    }

    public long getMulticastMaxWait()
    {
        return multicastMaxWait;
    }

    public void setMulticastMaxWait(long multicastMaxWait)
    {
        this.multicastMaxWait = multicastMaxWait;
    }

    public long[] getMulticastTimeouts()
    {
        return multicastTimeouts;
    }

    public void setMulticastTimeouts(long[] multicastTimeouts)
    {
        this.multicastTimeouts = multicastTimeouts;
    }

    public int getMaxTransmissionUnit()
    {
        return maxTransmissionUnit;
    }

    public void setMaxTransmissionUnit(int maxTransmissionUnit)
    {
        this.maxTransmissionUnit = maxTransmissionUnit;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setMulticastConnector(MulticastConnector connector)
    {
        this.multicastConnector = connector;
    }

    protected MulticastConnector getMulticastConnector()
    {
        return multicastConnector;
    }

    public void setUnicastConnector(UnicastConnector unicastConnector)
    {
        this.unicastConnector = unicastConnector;
    }

    protected UnicastConnector getUnicastConnector()
    {
        return unicastConnector;
    }

    public void addMessageListener(MessageListener listener, boolean multicast)
    {
        if (multicast)
        {
            if (multicastConnector != null) multicastConnector.addMessageListener(listener);
        }
        else
        {
            if (unicastConnector != null) unicastConnector.addMessageListener(listener);
        }
    }

    public void removeMessageListener(MessageListener listener, boolean multicast)
    {
        if (multicast)
        {
            if (multicastConnector != null) multicastConnector.removeMessageListener(listener);
        }
        else
        {
            if (unicastConnector != null) unicastConnector.removeMessageListener(listener);
        }
    }

    public boolean isRunning()
    {
        return running;
    }

    public void start() throws IOException
    {
        if (isRunning())
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("AgentManager " + this + " is already started");
            return;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("AgentManager " + this + " starting...");

        if (multicastConnector != null) multicastConnector.start();
        if (unicastConnector != null) unicastConnector.start();

        doStart();

        running = true;

        if (logger.isLoggable(Level.FINE)) logger.fine("AgentManager " + this + " started successfully");
    }

    protected void doStart() throws IOException
    {
    }

    public void stop() throws IOException
    {
        if (!isRunning())
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("AgentManager " + this + " is already stopped");
            return;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("AgentManager " + this + " stopping...");

        running = false;

        doStop();

        if (multicastConnector != null) multicastConnector.stop();
        if (unicastConnector != null) unicastConnector.stop();

        if (logger.isLoggable(Level.FINE)) logger.fine("AgentManager " + this + " stopped successfully");
    }

    protected void doStop() throws IOException
    {
    }

    protected int generateXID()
    {
        // XIDs are 2 byte integers
        return random.nextInt(1 << 16);
    }

    protected void closeNoExceptions(Socket socket)
    {
        try
        {
            socket.close();
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "Could not close socket " + socket, x);
        }
    }

    protected byte[] serializeMessage(Message message)
    {
        try
        {
            return message.serialize();
        }
        catch (ServiceLocationException e)
        {
            throw new AssertionError("BUG: could not serialize my own message " + message);
        }
    }

    protected DAAdvert[] convergentDASrvRqst(SrvRqst message, long timeframe, boolean repliesOnUnicast) throws IOException
    {
        ConvergentDAMessageListener listener = new ConvergentDAMessageListener();
        addMessageListener(listener, !repliesOnUnicast);
        try
        {
            List replies = convergentMulticastSend(message, timeframe, listener);
            return (DAAdvert[])replies.toArray(new DAAdvert[replies.size()]);
        }
        finally
        {
            removeMessageListener(listener, !repliesOnUnicast);
        }
    }

    protected SAAdvert[] convergentSASrvRqst(SrvRqst message, long timeframe, boolean repliesOnUnicast) throws IOException
    {
        ConvergentSAMessageListener listener = new ConvergentSAMessageListener();
        addMessageListener(listener, !repliesOnUnicast);
        try
        {
            List replies = convergentMulticastSend(message, timeframe, listener);
            return (SAAdvert[])replies.toArray(new SAAdvert[replies.size()]);
        }
        finally
        {
            removeMessageListener(listener, !repliesOnUnicast);
        }
    }

    /**
     * Implements the multicast convergence algorithm, with the extension of returning
     * after the specified timeframe. If the timeframe is negative, the plain multicast
     * convergence algorithm is used.
     * <br />
     * The multicast convergence algorithm tries to give some sort of reliability to
     * multicast and sends the message via multicast many times at different response timeouts
     * until: a) all responses have been received, or b) no responses have been received
     * and a timeout expires, or c) the number of responders cannot fit into a datagram
     * of the size of the configured MTU.
     * <br />
     * The multicast convergence algorithm works in this way:
     * <ol>
     * <li>Set the initial responder list to be empty</li>
     * <li>Send the message via multicast, with the responder list</li>
     * <li>Wait at most responseTimeout[i] and collect the replies</li>
     * <li>Extract the responders from the replies into the responder list</li>
     * <li>Increase i and go back to point 2</li>
     * </ol>
     * Exit point of the algorithm are when no responses are received (because ServiceAgents
     * and DirectoryAgents drops messages containing their IP in the received responder list),
     * or when a timeout expires.
     * <br />
     * The algorithm is described very briefly in RFC 2608, section 6.3
     * @return A list of messages in response of the multicast send
     */
    protected List convergentMulticastSend(Rqst message, long timeframe, ConvergentMessageListener replies) throws IOException
    {
        long start = System.currentTimeMillis();

        if (timeframe < 0)

        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence timeframe is negative, using max multicast wait");
            timeframe = getMulticastMaxWait();
        }
        if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence timeframe (ms): " + timeframe);

        long[] timeouts = getMulticastTimeouts();
        if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence timeouts (ms): " + Arrays.toString(timeouts));

        List result = new ArrayList();
        Set previousResponders = new HashSet();

        int noReplies = 0;
        int timeoutIndex = 0;
        while (timeoutIndex < timeouts.length)
        {
            long now = System.currentTimeMillis();

            // Exit if the timeframe has been exceeded
            if (timeframe > 0 && now > start + timeframe)
            {
                if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence exit, timeframe exceeded");
                break;
            }

            message.setPreviousResponders((String[])previousResponders.toArray(new String[previousResponders.size()]));
            byte[] messageBytes = serializeMessage(message);

            // Exit if the message bytes cannot fit into the MTU
            if (messageBytes.length > getMaxTransmissionUnit())
            {
                if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence exit, message greater than MTU");
                break;
            }

            if (logger.isLoggable(Level.FINE)) logger.fine("Multicast convergence sending " + message);
            multicastConnector.send(messageBytes);

            // Wait for the convergence timeout at timeoutIndex
            replies.lock();
            try
            {
                // Avoid spurious wakeups
                long timeout = timeouts[timeoutIndex];
                if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence start wait on timeout #" + (timeoutIndex + 1) + " (ms): " + timeout);
                long startWait = System.currentTimeMillis();
                long endWait = startWait;
                while (replies.isEmpty() && endWait - startWait < timeout)
                {
                    replies.await(startWait + timeout - endWait);
                    endWait = System.currentTimeMillis();
                    if (logger.isLoggable(Level.FINEST)) logger.finest("Multicast convergence waited (ms): " + (endWait - startWait));
                }
                if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence stop wait on timeout #" + (timeoutIndex + 1));

                boolean newMessages = false;
                if (!replies.isEmpty())
                {
                    // Messages arrived
                    while (!replies.isEmpty())
                    {
                        Rply reply = replies.pop();
                        String responder = reply.getResponder();
                        if (responder != null && responder.length() > 0)
                        {
                            if (previousResponders.add(responder))
                            {
                                result.add(reply);
                                newMessages = true;
                            }
                            else
                            {
                                // Drop the duplicate message, one copy arrived already
                                if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence received a reply from known responder " + responder + ", dropping it");
                            }
                        }
                        else
                        {
                            throw new IllegalStateException("Multicast convergence reply does not contain responder information " + reply);
                        }
                    }
                }

                if (newMessages)
                {
                    if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence replies: " + result);

                    // As extension, exit when the first storm of messages arrive
                    if (timeframe == 0)
                    {
                        if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence exit, first reply received");
                        break;
                    }

                    // Keep the same timeout
                    // Reset the no result counter, in case we had the pattern: [message,] no reply, message
                    noReplies = 0;
                }
                else
                {
                    if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence received no new replies");

                    // Timeout expired or duplicate messages
                    ++timeoutIndex;
                    ++noReplies;

                    // Exit if there are no result for 2 successive timeouts (RFC 2614 section 2.1.5)
                    if (noReplies > 1)
                    {
                        if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence exit, two timeouts elapsed");
                        break;
                    }
                }
            }
            catch (InterruptedException x)
            {
                Thread.currentThread().interrupt();
                break;
            }
            finally
            {
                replies.unlock();
            }
        }

        long end = System.currentTimeMillis();
        if (logger.isLoggable(Level.FINE)) logger.fine("Multicast convergence lasted (ms): " + (end - start));

        return result;
    }

    protected static abstract class ConvergentMessageListener implements MessageListener
    {
        private final List replies = new LinkedList();
        private final Lock lock = new ReentrantLock();
        private final Condition wait = lock.newCondition();

        public void lock()
        {
            lock.lock();
        }

        public void unlock()
        {
            lock.unlock();
        }

        public void await(long time) throws InterruptedException
        {
            wait.await(time, TimeUnit.MILLISECONDS);
        }

        protected void signalAll()
        {
            wait.signalAll();
        }

        public boolean isEmpty()
        {
            return replies.isEmpty();
        }

        protected void add(Message message)
        {
            replies.add(message);
            signalAll();
        }

        public Rply pop()
        {
            return (Rply)replies.remove(0);
        }
    }

    private class ConvergentDAMessageListener extends ConvergentMessageListener
    {
        public void handle(MessageEvent event)
        {
            InetSocketAddress address = event.getSocketAddress();
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());
                if (logger.isLoggable(Level.FINEST)) logger.finest("Convergent DA message listener " + this + " received message " + message);

                switch (message.getMessageType())
                {
                    case Message.DA_ADVERT_TYPE:
                        if (logger.isLoggable(Level.FINE)) logger.fine("Convergent DA message listener " + this + " received reply message from " + address + ": " + message);
                        lock();
                        try
                        {
                            ((DAAdvert)message).setResponder(address.getAddress().getHostAddress());
                            add(message);
                        }
                        finally
                        {
                            unlock();
                        }
                        break;
                    default:
                        if (logger.isLoggable(Level.FINEST)) logger.finest("Convergent DA message listener " + this + " ignoring message received from " + address + ": " + message);
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Convergent DA message listener " + this + " received bad message from " + address + ", ignoring", x);
            }
        }
    }

    private class ConvergentSAMessageListener extends ConvergentMessageListener
    {
        public void handle(MessageEvent event)
        {
            InetSocketAddress address = event.getSocketAddress();
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());

                switch (message.getMessageType())
                {
                    case Message.SRV_RPLY_TYPE:
                        if (logger.isLoggable(Level.FINE)) logger.fine("Convergent SA message listener " + this + " received reply message from " + address + ": " + message);
                        lock();
                        try
                        {
                            ((SrvRply)message).setResponder(address.getAddress().getHostAddress());
                            add(message);
                        }
                        finally
                        {
                            unlock();
                        }
                        break;
                    default:
                        if (logger.isLoggable(Level.FINEST)) logger.finest("Convergent SA message listener " + this + " ignoring message received from " + address + ": " + message);
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Convergent SA message listener " + this + " received bad message from " + address + ", ignoring", x);
            }
        }
    }
}
