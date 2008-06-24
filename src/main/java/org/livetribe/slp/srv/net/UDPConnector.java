/*
 * Copyright 2007-2008 the original author or authors
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
package org.livetribe.slp.srv.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.livetribe.slp.settings.Settings;

/**
 * @version $Revision$ $Date$
 */
public interface UDPConnector
{
    /**
     * @return a new DatagramSocket bound to the wildcard address and an ephemeral port.
     */
    public DatagramSocket newDatagramSocket();

    /**
     * Sends the given bytes to the SLP multicast (or broadcast) address and SLP port from the given local address.
     * @param localAddress The local address to send the bytes from
     * @param bytes the bytes to send
     */
    public void manycastSend(String localAddress, byte[] bytes);

    /**
     * Sends the given bytes to the SLP multicast (or broadcast) address and SLP port using the given DatagramSocket.
     * @param socket the DatagramSocket to use to send the bytes
     * @param bytes the bytes to send
     */
    public void manycastSend(DatagramSocket socket, byte[] bytes);

    /**
     * Receives (waiting at most for the given timeout) a DatagramPacket on the given DatagramSocket.
     * @param socket the DatagramSocket to use to receive the DatagramPacket
     * @param timeout the time to wait on receive, in milliseconds
     * @return the DatagramPacket received or null if the timeout elapsed
     */
    public DatagramPacket receive(DatagramSocket socket, int timeout);

    /**
     * Sends the given bytes to the given remote address from the given local address.
     * @param localAddress the local address to send the bytes from
     * @param remoteAddress the remote address to send the bytes to
     * @param bytes the bytes to send
     */
    public void unicastSend(String localAddress, InetSocketAddress remoteAddress, byte[] bytes);

    /**
     * Sends the given bytes to the SLP multicast (or broadcast) address and SLP notification port from the given local address.
     * @param localAddress The local address to send the bytes from
     * @param bytes the bytes to send
     */
    public void manycastNotify(String localAddress, byte[] bytes);

    public interface Factory
    {
        public UDPConnector newUDPConnector(Settings settings);
    }
}
