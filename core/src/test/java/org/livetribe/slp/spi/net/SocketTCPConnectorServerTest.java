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

import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;
import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class SocketTCPConnectorServerTest
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
        ExecutorService threadPool = Executors.newCachedThreadPool();
        SocketTCPConnectorServer connector = new SocketTCPConnectorServer(threadPool, newSettings());
        connector.start();
        assert connector.isRunning();
        connector.stop();
        assert !connector.isRunning();
    }

    @Test
    public void testClientSendsNothing() throws Exception
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
        ExecutorService threadPool = Executors.newCachedThreadPool();
        SocketTCPConnectorServer connector = new SocketTCPConnectorServer(threadPool, settings);
        connector.addMessageListener(listener);
        connector.start();
        try
        {
            Socket client = new Socket((String)null, port);
            client.close();

            // Wait for message to arrive
            Thread.sleep(500);

            assert connector.isRunning();
            assert message.get() == null;

            // Check that the connector is still able to accept connections
            client = new Socket((String)null, port);
            client.close();
        }
        finally
        {
            connector.stop();
        }
    }

    @Test
    public void testClientSendsIncompleteHeader() throws Exception
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
        ExecutorService threadPool = Executors.newCachedThreadPool();
        SocketTCPConnectorServer connector = new SocketTCPConnectorServer(threadPool, settings);
        connector.addMessageListener(listener);
        connector.start();
        try
        {
            Socket client = new Socket((String)null, port);
            OutputStream output = client.getOutputStream();
            byte[] incompleteHeader = new byte[]{2};
            output.write(incompleteHeader);
            output.flush();
            client.close();

            // Wait for message to arrive
            Thread.sleep(500);

            assert connector.isRunning();
            assert message.get() == null;

            // Check that the connector is still able to accept connections
            client = new Socket((String)null, port);
            client.close();
        }
        finally
        {
            connector.stop();
        }
    }

    @Test
    public void testClientSendsIncompleteMessage() throws Exception
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
        ExecutorService threadPool = Executors.newCachedThreadPool();
        SocketTCPConnectorServer connector = new SocketTCPConnectorServer(threadPool, settings);
        connector.addMessageListener(listener);
        connector.start();
        try
        {
            Socket client = new Socket((String)null, port);
            OutputStream output = client.getOutputStream();

            SrvRply reply = new SrvRply();
            URLEntry entry = new URLEntry();
            entry.setURL("url1");
            reply.addURLEntry(entry);
            reply.setResponder("127.0.0.1");

            byte[] messageBytes = reply.serialize();
            // Write all bytes but the last one
            output.write(messageBytes, 0, messageBytes.length - 1);
            output.flush();
            client.close();

            // Wait for message to arrive
            Thread.sleep(500);

            assert connector.isRunning();
            assert message.get() == null;

            // Check that the connector is still able to accept connections
            client = new Socket((String)null, port);
            client.close();
        }
        finally
        {
            connector.stop();
        }
    }

    @Test
    public void testClientSendsMessage() throws Exception
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
        ExecutorService threadPool = Executors.newCachedThreadPool();
        SocketTCPConnectorServer connector = new SocketTCPConnectorServer(threadPool, settings);
        connector.addMessageListener(listener);
        connector.start();
        try
        {
            Socket client = new Socket((String)null, port);
            OutputStream output = client.getOutputStream();

            SrvRply reply = new SrvRply();
            URLEntry entry = new URLEntry();
            entry.setURL("url1");
            reply.addURLEntry(entry);
            reply.setResponder("127.0.0.1");

            byte[] messageBytes = reply.serialize();
            output.write(messageBytes, 0, messageBytes.length);
            output.flush();
            client.close();

            // Wait for message to arrive
            Thread.sleep(500);

            assert connector.isRunning();
            MessageEvent event = message.get();
            assert event != null;
            Message receivedMessage = event.getMessage();
            assert receivedMessage != null;
            SrvRply receivedRply = (SrvRply)receivedMessage;
            assert receivedRply.getURLEntries().equals(reply.getURLEntries());

            // Check that the connector is still able to accept connections
            client = new Socket((String)null, port);
            client.close();
        }
        finally
        {
            connector.stop();
        }
    }
}
