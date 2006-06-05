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
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.sa.ServiceInfo;
import org.livetribe.slp.spi.StandardAgentManager;
import org.livetribe.slp.spi.msg.AttributeListExtension;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.IdentifierExtension;
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

    public DAAdvert[] multicastDASrvRqst(Scopes scopes, String filter, String language, long timeframe) throws IOException
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

    public void udpSAAdvert(InetSocketAddress address, String identifier, Scopes scopes, Attributes attributes, Integer xid, String language) throws IOException
    {
        SAAdvert advert = new SAAdvert();
        advert.setLanguage(language);
        advert.setXID(xid == null ? generateXID() : xid.intValue());
        advert.setAttributes(attributes);
        advert.setScopes(scopes);
        String host = localhost.getHostAddress();
        advert.setURL("service:service-agent://" + host);
        if (identifier != null)
        {
            IdentifierExtension extension = new IdentifierExtension();
            extension.setIdentifier(identifier);
            extension.setHost(host);
            advert.addExtension(extension);
        }

        byte[] bytes = serializeMessage(advert);

        if (logger.isLoggable(Level.FINEST)) logger.finest("Unicasting " + advert + " to " + address);

        getUDPConnector().unicastSend(null, address, bytes).close();
    }

    public SrvAck tcpSrvReg(InetAddress address, ServiceInfo service, ServiceAgentInfo serviceAgent, boolean freshRegistration) throws IOException
    {
        SrvReg registration = createSrvReg(service, serviceAgent, freshRegistration);

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

    private SrvReg createSrvReg(ServiceInfo service, ServiceAgentInfo serviceAgent, boolean freshRegistration)
    {
        ServiceURL serviceURL = service.getServiceURL();

        URLEntry urlEntry = new URLEntry();
        urlEntry.setLifetime(serviceURL.getLifetime());
        urlEntry.setURL(serviceURL.getURL());

        ServiceType serviceType = service.resolveServiceType();

        SrvReg registration = new SrvReg();
        registration.setURLEntry(urlEntry);
        registration.setServiceType(serviceType);
        registration.setScopes(resolveScopes(service, serviceAgent));
        registration.setAttributes(service.getAttributes());
        registration.setFresh(freshRegistration);
        registration.setXID(generateXID());
        registration.setLanguage(resolveLanguage(service, serviceAgent));

        return registration;
    }

    private String resolveLanguage(ServiceInfo service, ServiceAgentInfo serviceAgent)
    {
        String language = service.getLanguage();
        if (language == null) language = serviceAgent.getLanguage();
        if (language == null) language = Locale.getDefault().getLanguage();
        return language;
    }

    private Scopes resolveScopes(ServiceInfo service, ServiceAgentInfo serviceAgent)
    {
        Scopes scopes = service.getScopes();
        if (scopes == null) scopes = serviceAgent.getScopes();
        if (scopes == null) scopes = Scopes.DEFAULT;
        return scopes;
    }

    public SrvAck tcpSrvDeReg(InetAddress address, ServiceInfo service, ServiceAgentInfo serviceAgent) throws IOException
    {
        SrvDeReg deregistration = createSrvDeReg(service, serviceAgent);

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

    private SrvDeReg createSrvDeReg(ServiceInfo service, ServiceAgentInfo serviceAgent)
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

        return deregistration;
    }

    public void tcpSrvRply(Socket socket, Integer xid, String language, List serviceInfos) throws IOException
    {
        SrvRply srvRply = createSrvRply(xid, language, serviceInfos);
        byte[] bytes = serializeMessage(srvRply);

        if (logger.isLoggable(Level.FINEST))
            logger.finest("TCP unicasting " + srvRply + " to " + socket.getRemoteSocketAddress());

        getTCPConnector().reply(socket, bytes);
    }

    private SrvRply createSrvRply(Integer xid, String language, List serviceInfos)
    {
        SrvRply srvRply = new SrvRply();
        srvRply.setXID(xid == null ? generateXID() : xid.intValue());
        srvRply.setLanguage(language);

        for (int i = 0; i < serviceInfos.size(); ++i)
        {
            ServiceInfo serviceInfo = (ServiceInfo)serviceInfos.get(i);
            ServiceURL serviceURL = serviceInfo.getServiceURL();

            URLEntry urlEntry = new URLEntry();
            urlEntry.setURL(serviceURL.getURL());
            urlEntry.setLifetime(serviceURL.getLifetime());
            srvRply.addURLEntry(urlEntry);

            AttributeListExtension attrs = new AttributeListExtension();
            attrs.setURL(serviceURL.getURL());
            attrs.setAttributes(serviceInfo.getAttributes());
            srvRply.addExtension(attrs);
        }

        // TODO: a SrvRply can have errorCode != 0 ???
        srvRply.setErrorCode(0);
        return srvRply;
    }

    public void multicastSrvRegNotification(ServiceInfo service, ServiceAgentInfo serviceAgent, boolean freshRegistration) throws IOException
    {
        SrvReg registration = createSrvReg(service, serviceAgent, freshRegistration);
        registration.setMulticast(true);

        IdentifierExtension extension = new IdentifierExtension();
        extension.setIdentifier(serviceAgent.getIdentifier());
        extension.setHost(serviceAgent.getHost());
        registration.addExtension(extension);

        byte[] bytes = serializeMessage(registration);
        InetSocketAddress address = new InetSocketAddress(getConfiguration().getMulticastAddress(), getConfiguration().getNotificationPort());
        getUDPConnector().multicastSend(null, address, bytes).close();
    }

    public void multicastSrvDeRegNotification(ServiceInfo service, ServiceAgentInfo serviceAgent) throws IOException
    {
        SrvDeReg deregistration = createSrvDeReg(service, serviceAgent);
        deregistration.setMulticast(true);

        IdentifierExtension extension = new IdentifierExtension();
        extension.setIdentifier(serviceAgent.getIdentifier());
        extension.setHost(serviceAgent.getHost());
        deregistration.addExtension(extension);

        byte[] bytes = serializeMessage(deregistration);
        InetSocketAddress address = new InetSocketAddress(getConfiguration().getMulticastAddress(), getConfiguration().getNotificationPort());
        getUDPConnector().multicastSend(null, address, bytes).close();
    }
}
