/*
 * Copyright 2005 the original author or authors
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
package org.livetribe.slp.spi.sa;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.sa.ServiceInfo;
import org.livetribe.slp.spi.StandardAgentManager;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SAAdvert;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.net.TCPConnector;

/**
 * @version $Rev$ $Date$
 */
public class StandardServiceAgentManager extends StandardAgentManager implements ServiceAgentManager
{
    private InetAddress address;
    private InetAddress localhost;

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
                logger.warning("ServiceAgentManager " + this + " starting on loopback address; this is normally wrong, check your hosts configuration");
        }
        localhost = agentAddr;
    }

    public DAAdvert[] multicastDASrvRqst(String[] scopes, String filter, String language, long timeframe) throws IOException
    {
        SrvRqst request = new SrvRqst();
        request.setLanguage(language);
        request.setXID(generateXID());
        request.setServiceType(new ServiceType("service:directory-agent"));
        request.setScopes(scopes);
        request.setFilter(filter);
        request.setMulticast(true);
        return convergentDASrvRqst(request, timeframe);
    }

    public void udpSAAdvert(InetSocketAddress address, String[] scopes, Attributes attributes, Integer xid, String language) throws IOException
    {
        SAAdvert advert = new SAAdvert();
        advert.setLanguage(language);
        advert.setXID(xid == null ? generateXID() : xid.intValue());
        advert.setAttributes(attributes);
        advert.setScopes(scopes);
        advert.setURL("service:service-agent://" + localhost.getHostAddress());
        byte[] bytes = serializeMessage(advert);

        if (logger.isLoggable(Level.FINEST)) logger.finest("Unicasting " + advert + " to " + address);

        getUDPConnector().unicastSend(null, address, bytes).close();
    }

    public SrvAck tcpSrvReg(InetAddress address, ServiceInfo service, ServiceAgentInfo serviceAgent, boolean freshRegistration) throws IOException
    {
        ServiceURL serviceURL = service.getServiceURL();

        URLEntry urlEntry = new URLEntry();
        urlEntry.setLifetime(serviceURL.getLifetime());
        urlEntry.setURL(serviceURL.getURL());

        ServiceType serviceType = service.getServiceType();
        if (serviceType == null) serviceType = serviceURL.getServiceType();

        SrvReg registration = new SrvReg();
        registration.setURLEntry(urlEntry);
        registration.setServiceType(serviceType);
        registration.setScopes(resolveScopes(service, serviceAgent));
        registration.setAttributes(service.getAttributes());
        registration.setFresh(freshRegistration);
        registration.setXID(generateXID());
        registration.setLanguage(resolveLanguage(service, serviceAgent));

        byte[] requestBytes = serializeMessage(registration);

        TCPConnector connector = getTCPConnector();
        Socket socket = connector.send(requestBytes, address, false);
        byte[] replyBytes = connector.receive(socket);
        try
        {
            Message message = Message.deserialize(replyBytes);
            if (message.getMessageType() != Message.SRV_ACK_TYPE)
                throw new AssertionError("BUG: expected SrvAck upon SrvReg, received instead " + message);
            return (SrvAck)message;
        }
        catch (ServiceLocationException e)
        {
            throw new AssertionError("BUG: could not deserialize message " + replyBytes);
        }
        finally
        {
            closeNoExceptions(socket);
        }
    }

    private String resolveLanguage(ServiceInfo service, ServiceAgentInfo serviceAgent)
    {
        String language = service.getLanguage();
        if (language == null) language = serviceAgent.getLanguage();
        return language;
    }

    private String[] resolveScopes(ServiceInfo service, ServiceAgentInfo serviceAgent)
    {
        String[] scopes = service.getScopes();
        if (scopes == null) scopes = serviceAgent.getScopes();
        return scopes;
    }

    public SrvAck tcpSrvDeReg(InetAddress address, ServiceInfo service, ServiceAgentInfo serviceAgent) throws IOException
    {
        ServiceURL serviceURL = service.getServiceURL();

        URLEntry urlEntry = new URLEntry();
        urlEntry.setLifetime(serviceURL.getLifetime());
        urlEntry.setURL(serviceURL.getURL());

        SrvDeReg deregistration = new SrvDeReg();
        deregistration.setURLEntry(urlEntry);
        deregistration.setScopes(resolveScopes(service, serviceAgent));
        deregistration.setTags(service.getAttributes());
        deregistration.setXID(generateXID());
        deregistration.setLanguage(resolveLanguage(service, serviceAgent));

        byte[] requestBytes = serializeMessage(deregistration);

        TCPConnector connector = getTCPConnector();
        Socket socket = connector.send(requestBytes, address, false);
        byte[] replyBytes = connector.receive(socket);
        try
        {
            Message message = Message.deserialize(replyBytes);
            if (message.getMessageType() != Message.SRV_ACK_TYPE)
                throw new AssertionError("BUG: expected SrvAck upon SrvReg, received instead " + message);
            return (SrvAck)message;
        }
        catch (ServiceLocationException e)
        {
            throw new AssertionError("BUG: could not deserialize message " + replyBytes);
        }
        finally
        {
            closeNoExceptions(socket);
        }
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
