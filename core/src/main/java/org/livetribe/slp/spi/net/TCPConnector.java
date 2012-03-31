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
package org.livetribe.slp.spi.net;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.livetribe.slp.settings.Settings;


/**
 *
 */
public interface TCPConnector
{
    /**
     * Connects a new socket (bound to the wildcard address and an ephemeral port) to the given address,
     * writes the given bytes, reads the reply bytes and closes the socket.
     *
     * @param address the address to connect to
     * @param bytes   the bytes to write
     * @return the reply bytes
     */
    public byte[] writeAndRead(InetSocketAddress address, byte[] bytes);

    /**
     * Writes the given bytes on the given socket.
     *
     * @param socket the socket to write bytes to
     * @param bytes  the bytes to write
     */
    public void write(Socket socket, byte[] bytes);

    public interface Factory
    {
        public TCPConnector newTCPConnector(Settings settings);
    }
}
