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
package org.livetribe.slp.spi;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Condition;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.Rply;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.net.MulticastConnector;

/**
 * @version $Rev$ $Date$
 */
public abstract class Converger extends MulticastConnector.Acceptor implements MessageListener
{
    private final DatagramSocket socket;
    private final List replies = new LinkedList();
    private final Lock lock = new ReentrantLock();
    private final Condition wait = lock.newCondition();

    protected Converger() throws SocketException
    {
        this.socket = new DatagramSocket();
    }

    protected DatagramSocket getDatagramSocket()
    {
        return socket;
    }

    public abstract void send(MulticastConnector connector, byte[] bytes) throws IOException;

    public void close()
    {
        // TODO: we may improve closing by ensuring that if the socket
        // TODO: is sending something, the send completes before the close.
        // TODO: It should be sufficient a flag and a call to signal() in the thread loop below.
        socket.close();
    }

    public void lock()
    {
        lock.lock();
    }

    public void unlock()
    {
        lock.unlock();
    }

    public void await(long time) throws InterruptedException
    {
        wait.await(time, TimeUnit.MILLISECONDS);
    }

    protected void signalAll()
    {
        wait.signalAll();
    }

    public boolean isEmpty()
    {
        lock();
        try
        {
            return replies.isEmpty();
        }
        finally
        {
            unlock();
        }
    }

    protected void add(Message message)
    {
        lock();
        try
        {
            replies.add(message);
            signalAll();
        }
        finally
        {
            unlock();
        }
    }

    public Rply pop()
    {
        lock();
        try
        {
            return (Rply)replies.remove(0);
        }
        finally
        {
            unlock();
        }
    }

    public void run()
    {
        if (logger.isLoggable(Level.FINER)) logger.finer("Converger acceptor thread running for " + socket);

        while (true)
        {
            byte[] buffer = new byte[getMaxTransmissionUnit()];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try
            {
                socket.receive(packet);
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Received datagram packet " + packet + " on socket " + socket + ": " + packet.getLength() + " bytes from " + packet.getSocketAddress());
                // Handle the message event dispatching in this thread.
                // This is necessary to prevent out-of-order replies to the client
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
                MessageEvent event = new MessageEvent(socket, data, (InetSocketAddress)packet.getSocketAddress());
                handle(event);
            }
            catch (SocketException x)
            {
                if (logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST, "Datagram socket " + socket + " has been closed", x);
                break;
            }
            catch (SocketTimeoutException x)
            {
                // Timed out, but the socket is still valid, don't shut down
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("Timeout during receive() on datagram socket " + socket);
            }
            catch (IOException x)
            {
                if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Unexpected IOException", x);
                break;
            }
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("Converger acceptor thread exiting for " + socket);
    }
}
