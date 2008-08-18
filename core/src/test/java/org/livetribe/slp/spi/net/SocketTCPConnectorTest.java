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

/**
 * @version $Rev$ $Date$
 */
public class SocketTCPConnectorTest
{
//    /**
//     * @testng.configuration afterTestMethod="true"
//     */
//    protected void tearDown() throws Exception
//    {
//        // Allow ServerSocket to shutdown completely
//        sleep(1500);
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testStartStop() throws Exception
//    {
//        SocketTCPConnector connector = new SocketTCPConnector();
//        connector.setPort(getPort());
//
//        connector.start();
//        assertTrue(connector.isRunning());
//        connector.stop();
//        assertFalse(connector.isRunning());
//
//        // Be sure we can restart it
//        connector.start();
//        assertTrue(connector.isRunning());
//        connector.stop();
//        assertFalse(connector.isRunning());
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testClientSendsNothing() throws Exception
//    {
//        SocketTCPConnector connector = new SocketTCPConnector();
//        connector.setPort(getPort());
//        connector.setTCPListening(true);
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
//            Socket client = new Socket((String)null, connector.getPort());
//            client.close();
//
//            sleep(500);
//
//            assertTrue(connector.isRunning());
//            assert message.get() == null;
//
//            // Check that the connector is still able to accept connections
//            client = new Socket((String)null, connector.getPort());
//            client.close();
//        }
//        finally
//        {
//            sleep(500);
//            connector.stop();
//        }
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testClientSendsIncompleteHeader() throws Exception
//    {
//        SocketTCPConnector connector = new SocketTCPConnector();
//        connector.setPort(getPort());
//        connector.setTCPListening(true);
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
//            Socket client = new Socket((String)null, connector.getPort());
//            OutputStream output = client.getOutputStream();
//            byte[] incompleteHeader = new byte[]{2, 1, 0};
//            output.write(incompleteHeader);
//            output.flush();
//            client.close();
//
//            sleep(500);
//
//            assertTrue(connector.isRunning());
//            assert message.get() == null;
//
//            // Check that the connector is still able to accept connections
//            client = new Socket((String)null, connector.getPort());
//            client.close();
//        }
//        finally
//        {
//            sleep(500);
//            connector.stop();
//        }
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testClientSendsIncompleteMessage() throws Exception
//    {
//        SocketTCPConnector connector = new SocketTCPConnector();
//        connector.setPort(getPort());
//        connector.setTCPListening(true);
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
//            Socket client = new Socket((String)null, connector.getPort());
//            OutputStream output = client.getOutputStream();
//
//            SrvRply reply = new SrvRply();
//            URLEntry entry = new URLEntry();
//            entry.setURL("url1");
//            reply.addURLEntry(entry);
//            reply.setResponder("127.0.0.1");
//
//            byte[] messageBytes = reply.serialize();
//            // Write all bytes but the last one
//            output.write(messageBytes, 0, messageBytes.length - 1);
//            output.flush();
//            client.close();
//
//            sleep(500);
//
//            assertTrue(connector.isRunning());
//            assert message.get() == null;
//
//            // Check that the connector is still able to accept connections
//            client = new Socket((String)null, connector.getPort());
//            client.close();
//        }
//        finally
//        {
//            sleep(500);
//            connector.stop();
//        }
//    }
//
//    /**
//     * @testng.test
//     */
//    public void testClientSendsMessage() throws Exception
//    {
//        SocketTCPConnector connector = new SocketTCPConnector();
//        connector.setPort(getPort());
//        connector.setTCPListening(true);
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
//            Socket client = new Socket((String)null, connector.getPort());
//            OutputStream output = client.getOutputStream();
//
//            SrvRply reply = new SrvRply();
//            URLEntry entry = new URLEntry();
//            entry.setURL("url1");
//            reply.addURLEntry(entry);
//            reply.setResponder("127.0.0.1");
//
//            byte[] messageBytes = reply.serialize();
//            output.write(messageBytes, 0, messageBytes.length);
//            output.flush();
//            client.close();
//
//            // Wait for the message to arrive to the listener
//            sleep(500);
//
//            assertTrue(connector.isRunning());
//            assertNotNull(message.get());
//            MessageEvent event = (MessageEvent)message.get();
//            assertTrue(Arrays.equals(messageBytes, event.getMessageBytes()));
//
//            // Check that the connector is still able to accept connections
//            client = new Socket((String)null, connector.getPort());
//            client.close();
//        }
//        finally
//        {
//            sleep(500);
//            connector.stop();
//        }
//    }
}
