/*
 * Copyright 2006-2008 the original author or authors
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class MulticastSocketUDPConnectorServerTest
{
    private Settings newSettings()
    {
        Settings settings = new MapSettings();
        settings.put(PORT_KEY, 4427);
        return settings;
    }

    @Test
    public void testStartStop() throws Exception
    {
        Settings settings = newSettings();
        Integer port = settings.get(PORT_KEY, Defaults.get(PORT_KEY));
        ExecutorService threadPool = Executors.newCachedThreadPool();
        MulticastSocketUDPConnectorServer connector = new MulticastSocketUDPConnectorServer(threadPool, port, settings);
        connector.start();
        assert connector.isRunning();
        connector.stop();
        assert !connector.isRunning();
    }

    @Test
    public void testClientSendsEmptyMessage() throws Exception
    {
        final AtomicReference<MessageEvent> message = new AtomicReference<MessageEvent>();
        MessageListener listener = new MessageListener()
        {
            public void handle(MessageEvent event)
            {
                message.set(event);
            }
        };
        Settings settings = newSettings();
        Integer port = settings.get(PORT_KEY, Defaults.get(PORT_KEY));
        InetAddress multicastAddress = InetAddress.getByName(settings.get(MULTICAST_ADDRESS_KEY, Defaults.get(MULTICAST_ADDRESS_KEY)));
        ExecutorService threadPool = Executors.newCachedThreadPool();
        MulticastSocketUDPConnectorServer connector = new MulticastSocketUDPConnectorServer(threadPool, port, settings);
        connector.start();
        try
        {
            DatagramSocket client = new DatagramSocket();
            byte[] messageBytes = new byte[0];
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
            packet.setPort(port);
            packet.setAddress(multicastAddress);
            client.send(packet);
            client.close();

            // Wait for message to arrive
            Thread.sleep(500);

            assert connector.isRunning();
            assert message.get() == null;
        }
        finally
        {
            connector.stop();
        }
    }

//    /**
//     * @testng.test
//     */
//    public void testClientSendsMessage() throws Exception
//    {
//        SocketUDPConnector connector = new SocketUDPConnector();
//        connector.setPort(getPort());
//
//        final AtomicReference message = new AtomicReference(null);
//        connector.addMessageListener(new MessageListener()
//        {
//            public void handle(MessageEvent event)
//            {
//                message.set(event);
//            }
//        });
//
//        try
//        {
//            connector.start();
//            sleep(500);
//
//            SrvRply reply = new SrvRply();
//            URLEntry entry = new URLEntry();
//            entry.setURL("url1");
//            reply.addURLEntry(entry);
//
//            byte[] messageBytes = reply.serialize();
//            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
//            packet.setPort(connector.getPort());
//            packet.setAddress(connector.getMulticastAddress());
//
//            DatagramSocket client = new DatagramSocket();
//            client.send(packet);
//            client.close();
//
//            sleep(500);
//
//            assert connector.isRunning();
//            MessageEvent event = (MessageEvent)message.get();
//            assert event != null;
//            assert Arrays.equals(messageBytes, event.getMessageBytes());
//        }
//        finally
//        {
//            connector.stop();
//        }
//    }
}
