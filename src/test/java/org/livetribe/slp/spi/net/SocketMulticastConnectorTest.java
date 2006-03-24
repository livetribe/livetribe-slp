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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.spi.SLPSPITestCase;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;

/**
 * @version $Rev$ $Date$
 */
public class SocketMulticastConnectorTest extends SLPSPITestCase
{
    public void testStartStop() throws Exception
    {
        Configuration config = getDefaultConfiguration();
        SocketMulticastConnector connector = new SocketMulticastConnector();
        connector.setConfiguration(config);
        connector.start();
        assertTrue(connector.isRunning());
        connector.stop();
        assertFalse(connector.isRunning());

        // Be sure we can restart it
        connector.start();
        assertTrue(connector.isRunning());
        connector.stop();
    }

    public void testClientSendsEmptyMessage() throws Exception
    {
        Configuration config = getDefaultConfiguration();

        SocketMulticastConnector connector = new SocketMulticastConnector();
        connector.setConfiguration(config);

        final AtomicReference message = new AtomicReference(null);
        connector.addMessageListener(new MessageListener()
        {
            public void handle(MessageEvent event)
            {
                message.set(event);
            }
        });

        connector.start();
        sleep(500);

        try
        {
            DatagramSocket client = new DatagramSocket();
            byte[] messageBytes = new byte[0];
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
            packet.setPort(config.getPort());
            packet.setAddress(InetAddress.getByName(config.getMulticastAddress()));
            client.send(packet);
            client.close();

            sleep(500);

            assertTrue(connector.isRunning());
            assertNotNull(message.get());
            MessageEvent event = (MessageEvent)message.get();
            assertTrue(Arrays.equals(messageBytes, event.getMessageBytes()));
        }
        finally
        {
            sleep(500);
            connector.stop();
        }
    }

    public void testClientSendsMessage() throws Exception
    {
        Configuration config = getDefaultConfiguration();

        SocketMulticastConnector connector = new SocketMulticastConnector();
        connector.setConfiguration(config);

        final AtomicReference message = new AtomicReference(null);
        connector.addMessageListener(new MessageListener()
        {
            public void handle(MessageEvent event)
            {
                message.set(event);
            }
        });

        connector.start();
        sleep(500);

        try
        {
            SrvRply reply = new SrvRply();
            URLEntry entry = new URLEntry();
            entry.setURL("url1");
            reply.setURLEntries(new URLEntry[]{entry});
            reply.setResponder("127.0.0.1");

            byte[] messageBytes = reply.serialize();
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
            packet.setPort(config.getPort());
            packet.setAddress(InetAddress.getByName(config.getMulticastAddress()));

            DatagramSocket client = new DatagramSocket();
            client.send(packet);
            client.close();

            sleep(500);

            assertTrue(connector.isRunning());
            assertNotNull(message.get());
            MessageEvent event = (MessageEvent)message.get();
            assertTrue(Arrays.equals(messageBytes, event.getMessageBytes()));
        }
        finally
        {
            sleep(500);
            connector.stop();
        }
    }
}
