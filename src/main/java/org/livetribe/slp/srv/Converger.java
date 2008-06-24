/*
 * Copyright 2005 the original author or authors
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
package org.livetribe.slp.srv;

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

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.srv.msg.IdentifierExtension;
import org.livetribe.slp.srv.msg.Rply;
import org.livetribe.slp.srv.msg.Rqst;
import org.livetribe.slp.srv.net.UDPConnector;

/**
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
            multicastTimeouts = settings.get(Keys.MULTICAST_TIMEOUTS_KEY);
        if (settings.containsKey(Keys.MULTICAST_MAX_WAIT_KEY))
            multicastMaxWait = settings.get(Keys.MULTICAST_MAX_WAIT_KEY);
        if (settings.containsKey(Keys.MAX_TRANSMISSION_UNIT_KEY))
            maxTransmissionUnit = settings.get(Keys.MAX_TRANSMISSION_UNIT_KEY);
    }

    public List<T> converge(Rqst rqst)
    {
        if (logger.isLoggable(Level.FINER)) logger.finer("Multicast convergence max wait (ms): " + multicastMaxWait);

        if (logger.isLoggable(Level.FINER))
            logger.finer("Multicast convergence timeouts (ms): " + Arrays.toString(multicastTimeouts));

        List<T> result = new ArrayList<T>();
        Set<String> previousResponders = new HashSet<String>();
        Set<IdentifierExtension> previousResponderIdentifiers = new HashSet<IdentifierExtension>();

        DatagramSocket datagramSocket = udpConnector.newDatagramSocket();

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
                    logger.finer("Multicast convergence exit, message greater than MTU");
                break;
            }

            // Send and wait for the response up to the timeout at timeoutIndex
            if (logger.isLoggable(Level.FINE)) logger.fine("Multicast convergence sending " + rqst);
            List<T> rplys = multicastSendAndReceive(datagramSocket, rqstBytes, multicastTimeouts[timeoutIndex]);

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
                        logger.finer("Multicast convergence received a reply from new responder " + responder);
                    result.add(rply);
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
                            result.add(rply);
                            newMessages = true;
                        }
                        else
                        {
                            if (logger.isLoggable(Level.FINER))
                                logger.finer("Multicast convergence received a reply from known responder with id " + identifierExtension.getIdentifier() + " - " + responder + ", dropping it");
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

        datagramSocket.close();

        long end = System.currentTimeMillis();
        if (logger.isLoggable(Level.FINE))
            logger.fine("Multicast convergence lasted (ms): " + (end - start) + ", returning " + result.size() + " results");

        return result;
    }

    private List<T> multicastSendAndReceive(DatagramSocket datagramSocket, byte[] rqstBytes, int timeout)
    {
        List<T> result = new ArrayList<T>();

        udpConnector.manycastSend(datagramSocket, rqstBytes);

        DatagramPacket packet = null;
        while ((packet = udpConnector.receive(datagramSocket, timeout)) != null)
        {
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
            T rply = handle(data, (InetSocketAddress)packet.getSocketAddress());
            if (rply != null) result.add(rply);
        }

        return result;
    }

    protected abstract T handle(byte[] rplyBytes, InetSocketAddress address);
}
