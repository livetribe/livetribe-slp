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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.IdentifierExtension;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.Rply;
import org.livetribe.slp.spi.msg.Rqst;
import org.livetribe.slp.spi.msg.SAAdvert;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.net.SocketTCPConnector;
import org.livetribe.slp.spi.net.SocketUDPConnector;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * @version $Rev$ $Date$
 */
public abstract class StandardAgentManager implements AgentManager
{
    protected final Logger logger = Logger.getLogger(getClass().getName());

    private Configuration configuration;
    private long multicastMaxWait;
    private long[] multicastTimeouts;
    private int maxTransmissionUnit;
    private volatile boolean running;
    private UDPConnector udpConnector;
    private TCPConnector tcpConnector;
    private final Random random = new Random(System.currentTimeMillis());

    public void setConfiguration(Configuration configuration) throws IOException
    {
        this.configuration = configuration;
        setMulticastMaxWait(configuration.getMulticastMaxWait());
        setMulticastTimeouts(configuration.getMulticastTimeouts());
        setMaxTransmissionUnit(configuration.getMTU());
        if (udpConnector != null) udpConnector.setConfiguration(configuration);
        if (tcpConnector != null) tcpConnector.setConfiguration(configuration);
    }

    protected Configuration getConfiguration()
    {
        return configuration;
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

    public void setUDPConnector(UDPConnector connector)
    {
        this.udpConnector = connector;
    }

    protected UDPConnector getUDPConnector()
    {
        return udpConnector;
    }

    public void setTCPConnector(TCPConnector tcpConnector)
    {
        this.tcpConnector = tcpConnector;
    }

    protected TCPConnector getTCPConnector()
    {
        return tcpConnector;
    }

    public void addMessageListener(MessageListener listener, boolean udp)
    {
        if (udp)
        {
            if (udpConnector != null) udpConnector.addMessageListener(listener);
        }
        else
        {
            if (tcpConnector != null) tcpConnector.addMessageListener(listener);
        }
    }

    public void removeMessageListener(MessageListener listener, boolean udp)
    {
        if (udp)
        {
            if (udpConnector != null) udpConnector.removeMessageListener(listener);
        }
        else
        {
            if (tcpConnector != null) tcpConnector.removeMessageListener(listener);
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

        if (udpConnector == null) udpConnector = createUDPConnector();
        if (tcpConnector == null) tcpConnector = createTCPConnector();

        doStart();

        running = true;

        if (logger.isLoggable(Level.FINE)) logger.fine("AgentManager " + this + " started successfully");
    }

    protected void doStart() throws IOException
    {
        udpConnector.start();
        tcpConnector.start();
    }

    protected UDPConnector createUDPConnector() throws IOException
    {
        SocketUDPConnector connector = new SocketUDPConnector();
        connector.setConfiguration(getConfiguration());
        return connector;
    }

    protected TCPConnector createTCPConnector() throws IOException
    {
        SocketTCPConnector connector = new SocketTCPConnector();
        connector.setConfiguration(getConfiguration());
        return connector;
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

        if (logger.isLoggable(Level.FINE)) logger.fine("AgentManager " + this + " stopped successfully");
    }

    protected void doStop() throws IOException
    {
        if (udpConnector != null) udpConnector.stop();
        if (tcpConnector != null) tcpConnector.stop();
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

    protected DAAdvert[] convergentDASrvRqst(SrvRqst message, long timeframe) throws IOException
    {
        DASrvRqstConverger converger = new DASrvRqstConverger();
        try
        {
            List replies = convergentMulticastSend(message, timeframe, converger);
            return (DAAdvert[])replies.toArray(new DAAdvert[replies.size()]);
        }
        finally
        {
            converger.close();
        }
    }

    protected SAAdvert[] convergentSASrvRqst(SrvRqst message, long timeframe) throws IOException
    {
        SASrvRqstConverger converger = new SASrvRqstConverger();
        try
        {
            List replies = convergentMulticastSend(message, timeframe, converger);
            return (SAAdvert[])replies.toArray(new SAAdvert[replies.size()]);
        }
        finally
        {
            converger.close();
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
     *
     * @return A list of messages in response of the multicast send
     */
    protected List convergentMulticastSend(Rqst message, long timeframe, Converger converger) throws IOException
    {
        long start = System.currentTimeMillis();

        if (timeframe < 0)
        {
            if (logger.isLoggable(Level.FINER))
                logger.finer("Multicast convergence timeframe is negative, using max multicast wait");
            timeframe = getMulticastMaxWait();
        }
        if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence timeframe (ms): " + timeframe);

        long[] timeouts = getMulticastTimeouts();
        if (logger.isLoggable(Level.FINER))
            logger.finer("Multicast convergence timeouts (ms): " + Arrays.toString(timeouts));

        List result = new ArrayList();
        Set previousResponders = new HashSet();
        Set previousResponderIdentifiers = new HashSet();

        udpConnector.accept(converger);

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

            setPreviousResponders(message, previousResponders, previousResponderIdentifiers);
            byte[] messageBytes = serializeMessage(message);

            // Exit if the message bytes cannot fit into the MTU
            if (messageBytes.length > getMaxTransmissionUnit())
            {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Multicast convergence exit, message greater than MTU");
                break;
            }

            if (logger.isLoggable(Level.FINE)) logger.fine("Multicast convergence sending " + message);
            converger.send(udpConnector, messageBytes);

            // Wait for the convergence timeout at timeoutIndex
            converger.lock();
            try
            {
                // Avoid spurious wakeups
                long timeout = timeouts[timeoutIndex];
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Multicast convergence start wait on timeout #" + (timeoutIndex + 1) + " (ms): " + timeout);
                long startWait = System.currentTimeMillis();
                long endWait = startWait;
                while (converger.isEmpty() && endWait - startWait < timeout)
                {
                    converger.await(startWait + timeout - endWait);
                    endWait = System.currentTimeMillis();
                    if (logger.isLoggable(Level.FINEST))
                        logger.finest("Multicast convergence waited (ms): " + (endWait - startWait));
                }
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Multicast convergence stop wait on timeout #" + (timeoutIndex + 1));

                boolean newMessages = false;
                // Messages arrived
                while (!converger.isEmpty())
                {
                    Rply reply = converger.pop();
                    String responder = reply.getResponder();
                    if (logger.isLoggable(Level.FINER))
                        logger.finer("Multicast convergence received reply " + reply + ", responder is " + responder);

                    boolean newResponder = previousResponders.add(responder);
                    boolean newResponderIdentifier = newResponder;
                    IdentifierExtension identifierExtension = IdentifierExtension.findFirst(reply.getExtensions());
                    if (identifierExtension != null) newResponderIdentifier = previousResponderIdentifiers.add(identifierExtension);

                    if (newResponder)
                    {
                        if (logger.isLoggable(Level.FINER))
                            logger.finer("Multicast convergence received a reply from new responder " + responder);
                        result.add(reply);
                        newMessages = true;
                    }
                    else
                    {
                        if (logger.isLoggable(Level.FINER))
                            logger.finer("Multicast convergence received a reply from known responder " + responder);

                        if (identifierExtension != null)
                        {
                            if (newResponderIdentifier)
                            {
                                if (logger.isLoggable(Level.FINER))
                                    logger.finer("Multicast convergence received a reply from new responder with id " + identifierExtension.getIdentifier() + " - " + responder);
                                result.add(reply);
                                newMessages = true;
                            }
                            else
                            {
                                if (logger.isLoggable(Level.FINER))
                                    logger.finer("Multicast convergence received a reply from known responder with id "  + identifierExtension.getIdentifier() + " - " + responder + ", dropping it");
                            }
                        }
                        else
                        {
                                logger.finer("Multicast convergence received a reply from known responder " + responder + ", dropping it");
                        }
                    }
                }

                if (newMessages)
                {
                    if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence replies: " + result);

                    // As extension, exit when the first storm of messages arrive
                    if (timeframe == 0)
                    {
                        if (logger.isLoggable(Level.FINER))
                            logger.finer("Multicast convergence exit, first reply received");
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
                        if (logger.isLoggable(Level.FINER))
                            logger.finer("Multicast convergence exit, two timeouts elapsed");
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
                converger.unlock();
            }
        }

        long end = System.currentTimeMillis();
        if (logger.isLoggable(Level.FINE)) logger.fine("Multicast convergence lasted (ms): " + (end - start));

        return result;
    }

    private void setPreviousResponders(Rqst message, Set previousResponders, Set previousResponderIdentifiers)
    {
        message.setPreviousResponders(previousResponders);
        if (!previousResponderIdentifiers.isEmpty())
        {
            for (Iterator identifiers = previousResponderIdentifiers.iterator(); identifiers.hasNext();)
            {
                IdentifierExtension extension = (IdentifierExtension)identifiers.next();
                message.addExtension(extension);
            }
        }
    }

    private class DASrvRqstConverger extends Converger
    {
        private final InetSocketAddress address;

        public DASrvRqstConverger() throws SocketException
        {
            address = new InetSocketAddress(getConfiguration().getMulticastAddress(), getConfiguration().getPort());
        }

        public void send(UDPConnector connector, byte[] bytes) throws IOException
        {
            connector.multicastSend(getDatagramSocket(), address, bytes);
        }

        public void handle(MessageEvent event)
        {
            InetSocketAddress address = event.getSocketAddress();
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("Convergent DA message listener " + this + " received message " + message);

                switch (message.getMessageType())
                {
                    case Message.DA_ADVERT_TYPE:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("Convergent DA message listener " + this + " received reply message from " + address + ": " + message);
                        ((DAAdvert)message).setResponder(address.getAddress().getHostAddress());
                        add(message);
                        break;
                    default:
                        if (logger.isLoggable(Level.FINEST))
                            logger.finest("Convergent DA message listener " + this + " ignoring message received from " + address + ": " + message);
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Convergent DA message listener " + this + " received bad message from " + address + ", ignoring", x);
            }
        }
    }

    private class SASrvRqstConverger extends Converger
    {
        private final InetSocketAddress address;

        public SASrvRqstConverger() throws SocketException
        {
            address = new InetSocketAddress(getConfiguration().getMulticastAddress(), getConfiguration().getPort());
        }

        public void send(UDPConnector connector, byte[] bytes) throws IOException
        {
            connector.multicastSend(getDatagramSocket(), address, bytes);
        }

        public void handle(MessageEvent event)
        {
            InetSocketAddress address = event.getSocketAddress();
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());

                switch (message.getMessageType())
                {
                    case Message.SA_ADVERT_TYPE:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("Convergent SA message listener " + this + " received reply message from " + address + ": " + message);
                        lock();
                        try
                        {
                            ((SAAdvert)message).setResponder(address.getAddress().getHostAddress());
                            add(message);
                        }
                        finally
                        {
                            unlock();
                        }
                        break;
                    default:
                        if (logger.isLoggable(Level.FINEST))
                            logger.finest("Convergent SA message listener " + this + " ignoring message received from " + address + ": " + message);
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Convergent SA message listener " + this + " received bad message from " + address + ", ignoring", x);
            }
        }
    }
}
