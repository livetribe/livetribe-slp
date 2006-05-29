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
package org.livetribe.slp.spi.da;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.spi.StandardAgentManager;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.net.TCPConnector;

/**
 * @version $Rev$ $Date$
 */
public class StandardDirectoryAgentManager extends StandardAgentManager implements DirectoryAgentManager
{
    private InetAddress address;
    private InetAddress localhost;

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        // By default, DirectoryAgent listens to TCP also
        TCPConnector connector = getTCPConnector();
        if (connector != null) connector.setTCPListening(true);
    }

    public InetAddress getInetAddress()
    {
        return address;
    }

    public void setInetAddress(InetAddress address)
    {
        this.address = address;
    }

    public void doStart() throws IOException
    {
        super.doStart();

        InetAddress agentAddr = getInetAddress();
        if (agentAddr == null) agentAddr = InetAddress.getLocalHost();
        if (agentAddr.isLoopbackAddress())
        {
            if (logger.isLoggable(Level.WARNING))
                logger.warning("DirectoryAgentManager " + this + " starting on loopback address; this is normally wrong, check your hosts configuration");
        }
        localhost = agentAddr;
    }

    protected TCPConnector createTCPConnector()
    {
        TCPConnector result = super.createTCPConnector();
        // By default, DirectoryAgent listens to TCP also
        result.setTCPListening(true);
        return result;
    }

    public void multicastDAAdvert(long bootTime, String[] scopes, Attributes attributes, Integer xid, String language) throws IOException
    {
        DAAdvert daAdvert = createDAAdvert(bootTime, scopes, attributes, xid, language);
        byte[] bytes = serializeMessage(daAdvert);

        if (logger.isLoggable(Level.FINEST)) logger.finest("Multicasting " + daAdvert);

        getUDPConnector().multicastSend(null, bytes).close();
    }

    public void udpDAAdvert(InetSocketAddress address, long bootTime, String[] scopes, Attributes attributes, Integer xid, String language) throws IOException
    {
        DAAdvert daAdvert = createDAAdvert(bootTime, scopes, attributes, xid, language);
        daAdvert.setMulticast(false);
        byte[] bytes = serializeMessage(daAdvert);

        if (logger.isLoggable(Level.FINEST)) logger.finest("UDP unicasting " + daAdvert + " to " + address);

        getUDPConnector().unicastSend(null, address, bytes).close();
    }

    private DAAdvert createDAAdvert(long bootTime, String[] scopes, Attributes attributes, Integer xid, String language)
    {
        DAAdvert daAdvert = new DAAdvert();
        daAdvert.setLanguage(language);
        daAdvert.setMulticast(true);
        daAdvert.setXID(xid == null ? generateXID() : xid.intValue());
        daAdvert.setBootTime(bootTime);
        daAdvert.setURL("service:directory-agent://" + localhost.getHostAddress());
        daAdvert.setScopes(scopes);
        daAdvert.setAttributes(attributes);
        return daAdvert;
    }

    public void tcpSrvAck(Socket socket, Integer xid, String language, int errorCode) throws IOException
    {
        SrvAck srvAck = new SrvAck();
        srvAck.setXID(xid == null ? generateXID() : xid.intValue());
        srvAck.setLanguage(language);
        srvAck.setErrorCode(errorCode);
        byte[] bytes = serializeMessage(srvAck);

        if (logger.isLoggable(Level.FINEST)) logger.finest("TCP unicasting " + srvAck + " to " + socket.getRemoteSocketAddress());

        getTCPConnector().reply(socket, bytes);
    }

    public void tcpSrvRply(Socket socket, Integer xid, String language, ServiceURL[] serviceURLs) throws IOException
    {
        SrvRply srvRply = new SrvRply();
        srvRply.setXID(xid == null ? generateXID() : xid.intValue());
        srvRply.setLanguage(language);
        // TODO: a SrvRply can have errorCode != 0 ???
        srvRply.setErrorCode(0);
        URLEntry[] entries = new URLEntry[serviceURLs.length];
        for (int i = 0; i < entries.length; ++i)
        {
            ServiceURL serviceURL = serviceURLs[i];
            entries[i] = new URLEntry();
            entries[i].setURL(serviceURL.getURL());
            entries[i].setLifetime(serviceURL.getLifetime());
        }
        srvRply.setURLEntries(entries);
        byte[] bytes = serializeMessage(srvRply);

        if (logger.isLoggable(Level.FINEST)) logger.finest("TCP unicasting " + srvRply + " to " + socket.getRemoteSocketAddress());

        getTCPConnector().reply(socket, bytes);
    }
}
