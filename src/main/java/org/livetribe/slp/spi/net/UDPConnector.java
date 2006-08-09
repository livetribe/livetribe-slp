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
package org.livetribe.slp.spi.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import org.livetribe.slp.spi.Defaults;

/**
 * @version $Rev$ $Date$
 */
public abstract class UDPConnector extends NetworkConnector
{
    private int multicastTimeToLive = Defaults.MULTICAST_TIME_TO_LIVE;
    private int maxTransmissionUnit = Defaults.MAX_TRANSMISSION_UNIT;
    private InetAddress multicastAddress;
    private int port = Defaults.PORT;

    public void setMulticastTimeToLive(int multicastTimeToLive)
    {
        this.multicastTimeToLive = multicastTimeToLive;
    }

    public int getMulticastTimeToLive()
    {
        return multicastTimeToLive;
    }

    public int getMaxTransmissionUnit()
    {
        return maxTransmissionUnit;
    }

    public void setMaxTransmissionUnit(int maxTransmissionUnit)
    {
        this.maxTransmissionUnit = maxTransmissionUnit;
    }

    public InetAddress getMulticastAddress()
    {
        return multicastAddress;
    }

    public void setMulticastAddress(InetAddress multicastAddress)
    {
        this.multicastAddress = multicastAddress;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    protected void doStart() throws IOException
    {
        if (getMulticastAddress() == null) setMulticastAddress(InetAddress.getByName(Defaults.MULTICAST_ADDRESS));
        super.doStart();
    }

    /**
     * Sends the given bytes to the given address.
     * @param socket The datagram socket to be used to send the bytes, or null if the datagram socket must be created
     * @param address The target address to send the bytes to
     * @param bytes The bytes to send
     * @return The datagram socket passed in, or the newly created one if <code>socket</code> was null
     * @throws IOException In case of communication errors
     */
    public abstract DatagramSocket unicastSend(DatagramSocket socket, InetSocketAddress address, byte[] bytes) throws IOException;

    /**
     * Sends the given bytes to the specified multicast address.
     * @param socket The datagram socket to be used to send the bytes, or null if the datagram socket must be created
     * @param bytes The bytes to send
     * @return The datagram socket passed in, or the newly created one if <code>socket</code> was null
     * @throws IOException In case of communication errors
     */
    public abstract DatagramSocket multicastSend(DatagramSocket socket, InetSocketAddress address, byte[] bytes) throws IOException;

    // Made method public
    public void accept(Runnable executor)
    {
        super.accept(executor);
    }

    public static abstract class Acceptor implements Runnable
    {
        protected final Logger logger = Logger.getLogger(getClass().getName());

        private final UDPConnector udpConnector;

        protected Acceptor(UDPConnector udpConnector)
        {
            this.udpConnector = udpConnector;
        }

        protected int getMaxTransmissionUnit()
        {
            return udpConnector.getMaxTransmissionUnit();
        }

        protected void handle(Runnable executor)
        {
            udpConnector.handle(executor);
        }

        protected InetAddress getMulticastAddress()
        {
            return udpConnector.getMulticastAddress();
        }

        protected int getPort()
        {
            return udpConnector.getPort();
        }
    }
}
