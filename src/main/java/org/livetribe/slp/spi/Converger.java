/*
 * Copyright 2005-2008 the original author or authors
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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.IdentifierExtension;
import org.livetribe.slp.spi.msg.Rply;
import org.livetribe.slp.spi.msg.Rqst;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * Encapsulates the multicast convergence algorithm. <br />
 * SLP uses multicast convergence for discovery.
 * The goal of multicast convergence is to send a number of requests separated by a wait time; interested parties
 * that receive a request send a reply accordingly. Every reply carries (implicitely or explicitely) an indentifier
 * of the interested party. The requests are sent multiple times to increase the chances to reach all interested
 * parties, since a non-reliable protocol (UDP) is used to send the requests.
 * Every time a reply is received, the indentifier of the interested party is added to the next request, and
 * interested parties do not reply to requests that contain their own identifier.
 * When two consecutive wait times elapse, the algorithm exits and returns the non-duplicate replies that have been
 * received (if any).
 *
 * @version $Revision$ $Date$
 */
public abstract class Converger<T extends Rply>
{
    protected final Logger logger = Logger.getLogger(getClass().getName());
    private final UDPConnector udpConnector;
    private int[] multicastTimeouts = Defaults.get(Keys.MULTICAST_TIMEOUTS_KEY);
    private int multicastMaxWait = Defaults.get(Keys.MULTICAST_MAX_WAIT_KEY);
    private int maxTransmissionUnit = Defaults.get(Keys.MAX_TRANSMISSION_UNIT_KEY);

    protected Converger(UDPConnector udpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(Keys.MULTICAST_TIMEOUTS_KEY))
            setMulticastTimeouts(settings.get(Keys.MULTICAST_TIMEOUTS_KEY));
        if (settings.containsKey(Keys.MULTICAST_MAX_WAIT_KEY))
            setMulticastMaxWait(settings.get(Keys.MULTICAST_MAX_WAIT_KEY));
        if (settings.containsKey(Keys.MAX_TRANSMISSION_UNIT_KEY))
            setMaxTransmissionUnit(settings.get(Keys.MAX_TRANSMISSION_UNIT_KEY));
    }

    public void setMulticastTimeouts(int[] multicastTimeouts)
    {
        this.multicastTimeouts = multicastTimeouts;
    }

    public void setMulticastMaxWait(int multicastMaxWait)
    {
        this.multicastMaxWait = multicastMaxWait;
    }

    public void setMaxTransmissionUnit(int maxTransmissionUnit)
    {
        this.maxTransmissionUnit = maxTransmissionUnit;
    }

    /**
     * Callback for the beginning of the convergence algorithm.
     * It normally creates and returns a DatagramSocket that is used to send requests and to receive replies.
     *
     * @return the DatagramSocket to be used to send requests and receive replies
     * @see #convergenceEnd(DatagramSocket)
     */
    protected DatagramSocket convergenceBegin()
    {
        return udpConnector.newDatagramSocket();
    }

    /**
     * Performs the multicast convergence algorithm, sending the given request many times and waiting for replies
     * or for a timeout to elapse.
     *
     * @param rqst the request to send
     * @return a list of replies received
     * @see #convergenceBegin()
     */
    public List<T> converge(Rqst rqst)
    {
        if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence max wait (ms): " + multicastMaxWait);

        if (logger.isLoggable(Level.FINER))
            logger.finer("Multicast convergence timeouts (ms): " + Arrays.toString(multicastTimeouts));

        List<T> result = new ArrayList<T>();
        Set<String> previousResponders = new HashSet<String>();
        Set<IdentifierExtension> previousResponderIdentifiers = new HashSet<IdentifierExtension>();

        DatagramSocket datagramSocket = convergenceBegin();

        long start = System.currentTimeMillis();

        int noReplies = 0;
        int timeoutIndex = 0;
        while (timeoutIndex < multicastTimeouts.length)
        {
            long now = System.currentTimeMillis();

            // Exit if the max wait has been exceeded
            if (multicastMaxWait > 0 && now > start + multicastMaxWait)
            {
                if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence exit, timeframe exceeded");
                break;
            }

            // Set the previous responders
            rqst.setPreviousResponders(previousResponders);
            if (!previousResponderIdentifiers.isEmpty())
            {
                for (IdentifierExtension previousResponderIdentifier : previousResponderIdentifiers)
                {
                    rqst.addExtension(previousResponderIdentifier);
                }
            }

            byte[] rqstBytes = rqst.serialize();

            // Exit if the message bytes cannot fit into the MTU because there are too many previous responders
            if (rqstBytes.length > maxTransmissionUnit)
            {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Multicast convergence exit, message bigger than maxTransmissionUnit");
                break;
            }

            // Send and wait for the response up to the timeout at timeoutIndex
            if (logger.isLoggable(Level.FINE)) logger.fine("Multicast convergence sending " + rqst);
            List<T> rplys = manycastSendAndReceive(datagramSocket, rqstBytes, multicastTimeouts[timeoutIndex]);

            boolean newMessages = false;
            for (T rply : rplys)
            {
                String responder = rply.getResponder();
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Multicast convergence received reply " + rply + ", responder is " + responder);

                boolean newResponder = previousResponders.add(responder);
                boolean newResponderIdentifier = newResponder;
                IdentifierExtension identifierExtension = IdentifierExtension.findFirst(rply.getExtensions());
                if (identifierExtension != null)
                    newResponderIdentifier = previousResponderIdentifiers.add(identifierExtension);

                if (newResponder)
                {
                    if (logger.isLoggable(Level.FINER))
                        logger.finer("Multicast convergence received a reply from new responder " + responder + " (" + (identifierExtension == null ? "" : identifierExtension.getIdentifier()) + ")");
                    result.add(rply);
                    newMessages = true;
                }
                else
                {
                    if (logger.isLoggable(Level.FINER))
                        logger.finer("Multicast convergence received a reply from known responder " + responder + " (" + (identifierExtension == null ? "" : identifierExtension.getIdentifier()) + ")");

                    if (identifierExtension != null)
                    {
                        if (newResponderIdentifier)
                        {
                            if (logger.isLoggable(Level.FINER))
                                logger.finer("Multicast convergence received a reply from new responder " + responder + " (" + identifierExtension.getIdentifier() + ")");
                            result.add(rply);
                            newMessages = true;
                        }
                        else
                        {
                            if (logger.isLoggable(Level.FINER))
                                logger.finer("Multicast convergence received a reply from known responder " + responder + " (" + identifierExtension.getIdentifier() + "), dropping it");
                        }
                    }
                    else
                    {
                        if (logger.isLoggable(Level.FINER))
                            logger.finer("Multicast convergence received a reply from known responder " + responder + ", dropping it");
                    }
                }
            }

            if (newMessages)
            {
                if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence replies: " + result);

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

        convergenceEnd(datagramSocket);

        long end = System.currentTimeMillis();
        if (logger.isLoggable(Level.FINE))
            logger.fine("Multicast convergence lasted (ms): " + (end - start) + ", returning " + result.size() + " results");

        return result;
    }

    /**
     * Sends the given request bytes, then waits at most for the given timeout for replies.
     * For each reply, sets the responder address.
     *
     * @param datagramSocket the socket to use to send and receive
     * @param rqstBytes      the request bytes to send
     * @param timeout        the max time to wait for a reply
     * @return a list of replies received
     */
    protected List<T> manycastSendAndReceive(DatagramSocket datagramSocket, byte[] rqstBytes, int timeout)
    {
        List<T> result = new ArrayList<T>();

        udpConnector.manycastSend(datagramSocket, rqstBytes);

        DatagramPacket packet = null;
        while ((packet = udpConnector.receive(datagramSocket, timeout)) != null)
        {
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
            InetSocketAddress address = (InetSocketAddress)packet.getSocketAddress();
            T rply = convert(data, address);
            if (rply != null)
            {
                rply.setResponder(address.getAddress().getHostAddress());
                result.add(rply);
            }
        }

        return result;
    }

    /**
     * Callback for the end of the multicast convergence algorithm.
     * It normally closes the DatagramSocket used to send requests and receive replies
     *
     * @param datagramSocket the socket to close
     * @see #convergenceBegin()
     */
    protected void convergenceEnd(DatagramSocket datagramSocket)
    {
        datagramSocket.close();
    }

    /**
     * Converts the given reply bytes into a {@link Rply} message.
     * If null is returned, it is like the reply did not arrive.
     *
     * @param rplyBytes the bytes to convert
     * @param address
     * @return a reply message or null if the message bytes are not a proper reply
     */
    protected abstract T convert(byte[] rplyBytes, InetSocketAddress address);
}
