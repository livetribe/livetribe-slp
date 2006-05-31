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
import org.livetribe.slp.SLPTestSupport;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;

/**
 * @version $Rev$ $Date$
 */
public class SocketUDPConnectorTest extends SLPTestSupport
{
    /**
     * @testng.configuration afterTestMethod="true"
     */
    protected void tearDown() throws Exception
    {
        sleep(500);
    }

    /**
     * @testng.test
     */
    public void testStartStop() throws Exception
    {
        Configuration config = getDefaultConfiguration();
        SocketUDPConnector connector = new SocketUDPConnector();
        connector.setConfiguration(config);
        connector.start();
        assert connector.isRunning();
        connector.stop();
        assert !connector.isRunning();

        // Be sure we can restart it
        connector.start();
        assert connector.isRunning();
        connector.stop();
    }

    /**
     * @testng.test
     */
    public void testClientSendsEmptyMessage() throws Exception
    {
        Configuration config = getDefaultConfiguration();

        SocketUDPConnector connector = new SocketUDPConnector();
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

            assert connector.isRunning();
            MessageEvent event = (MessageEvent)message.get();
            assert event != null;
            assert Arrays.equals(messageBytes, event.getMessageBytes());
        }
        finally
        {
            connector.stop();
        }
    }

    /**
     * @testng.test
     */
    public void testClientSendsMessage() throws Exception
    {
        Configuration config = getDefaultConfiguration();

        SocketUDPConnector connector = new SocketUDPConnector();
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

            byte[] messageBytes = reply.serialize();
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
            packet.setPort(config.getPort());
            packet.setAddress(InetAddress.getByName(config.getMulticastAddress()));

            DatagramSocket client = new DatagramSocket();
            client.send(packet);
            client.close();

            sleep(500);

            assert connector.isRunning();
            MessageEvent event = (MessageEvent)message.get();
            assert event != null;
            assert Arrays.equals(messageBytes, event.getMessageBytes());
        }
        finally
        {
            connector.stop();
        }
    }
}
