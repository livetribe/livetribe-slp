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

import java.io.OutputStream;
import java.net.Socket;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.spi.SLPSPITestCase;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;

/**
 * @version $Rev$ $Date$
 */
public class SocketTCPConnectorTest extends SLPSPITestCase
{
    protected void tearDown() throws Exception
    {
        // Allow ServerSocket to shutdown completely
        sleep(500);
    }

    public void testStartStop() throws Exception
    {
        Configuration config = getDefaultConfiguration();
        SocketTCPConnector connector = new SocketTCPConnector();
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

    public void testClientSendsNothing() throws Exception
    {
        Configuration config = getDefaultConfiguration();

        SocketTCPConnector connector = new SocketTCPConnector();
        connector.setConfiguration(config);
        connector.setTCPListening(true);

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
            Socket client = new Socket((String)null, config.getPort());
            client.close();

            sleep(500);

            assertTrue(connector.isRunning());
            assertNull(message.get());

            // Check that the connector is still able to accept connections
            client = new Socket((String)null, config.getPort());
            client.close();
        }
        finally
        {
            sleep(500);
            connector.stop();
        }
    }

    public void testClientSendsIncompleteHeader() throws Exception
    {
        Configuration config = getDefaultConfiguration();

        SocketTCPConnector connector = new SocketTCPConnector();
        connector.setConfiguration(config);
        connector.setTCPListening(true);

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
            Socket client = new Socket((String)null, config.getPort());
            OutputStream output = client.getOutputStream();
            byte[] incompleteHeader = new byte[]{2, 1, 0};
            output.write(incompleteHeader);
            output.flush();
            client.close();

            sleep(500);

            assertTrue(connector.isRunning());
            assertNull(message.get());

            // Check that the connector is still able to accept connections
            client = new Socket((String)null, config.getPort());
            client.close();
        }
        finally
        {
            sleep(500);
            connector.stop();
        }
    }

    public void testClientSendsIncompleteMessage() throws Exception
    {
        Configuration config = getDefaultConfiguration();

        SocketTCPConnector connector = new SocketTCPConnector();
        connector.setConfiguration(config);
        connector.setTCPListening(true);

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
            Socket client = new Socket((String)null, config.getPort());
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

            sleep(500);

            assertTrue(connector.isRunning());
            assertNull(message.get());

            // Check that the connector is still able to accept connections
            client = new Socket((String)null, config.getPort());
            client.close();
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

        SocketTCPConnector connector = new SocketTCPConnector();
        connector.setConfiguration(config);
        connector.setTCPListening(true);

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
            Socket client = new Socket((String)null, config.getPort());
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

            // Wait for the message to arrive to the listener
            sleep(500);

            assertTrue(connector.isRunning());
            assertNotNull(message.get());
            MessageEvent event = (MessageEvent)message.get();
            assertTrue(Arrays.equals(messageBytes, event.getMessageBytes()));

            // Check that the connector is still able to accept connections
            client = new Socket((String)null, config.getPort());
            client.close();
        }
        finally
        {
            sleep(500);
            connector.stop();
        }
    }
}
