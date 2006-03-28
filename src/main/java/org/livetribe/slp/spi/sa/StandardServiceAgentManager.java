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
import java.net.Socket;
import java.util.Locale;

import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.spi.StandardAgentManager;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.net.UnicastConnector;

/**
 * @version $Rev$ $Date$
 */
public class StandardServiceAgentManager extends StandardAgentManager implements ServiceAgentManager
{
    public DAAdvert[] multicastDASrvRqst(String[] scopes, String filter, long timeframe) throws IOException
    {
        SrvRqst request = createSrvRqst(new ServiceType("service:directory-agent"), scopes, filter);
        request.setMulticast(true);
        UnicastConnector unicast = getUnicastConnector();
        return convergentDASrvRqst(request, timeframe, unicast != null && unicast.isUnicastListening());
    }

    private SrvRqst createSrvRqst(ServiceType serviceType, String[] scopes, String filter)
    {
        SrvRqst request = new SrvRqst();
        request.setLanguage(Locale.getDefault().getCountry());
        request.setXID(generateXID());
        request.setServiceType(serviceType);
        request.setScopes(scopes);
        request.setFilter(filter);
        return request;
    }

    public SrvAck unicastSrvReg(InetAddress address, ServiceAgentInfo info) throws IOException
    {
        ServiceURL serviceURL = info.getServiceURL();

        URLEntry urlEntry = new URLEntry();
        urlEntry.setLifetime(serviceURL.getLifetime());
        urlEntry.setURL(serviceURL.toString());

        ServiceType serviceType = info.getServiceType();
        if (serviceType == null) serviceType = serviceURL.getServiceType();

        SrvReg registration = new SrvReg();
        registration.setURLEntry(urlEntry);
        registration.setServiceType(serviceType);
        registration.setScopes(info.getScopes());
        registration.setAttributes(info.getAttributes());
        registration.setFresh(info.isFresh());
        registration.setXID(generateXID());
        registration.setLanguage(info.getLanguage());

        byte[] requestBytes = serializeMessage(registration);

        UnicastConnector unicast = getUnicastConnector();
        Socket socket = unicast.send(requestBytes, address, false);
        byte[] replyBytes = unicast.receive(socket);
        try
        {
            Message message = Message.deserialize(replyBytes);
            if (message.getMessageType() != Message.SRV_ACK_TYPE) throw new AssertionError("BUG: expected SrvAck upon SrvReg, received instead " + message);
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

    public SrvAck unicastSrvDeReg(InetAddress address, ServiceAgentInfo info) throws IOException
    {
        ServiceURL serviceURL = info.getServiceURL();

        URLEntry urlEntry = new URLEntry();
        urlEntry.setLifetime(serviceURL.getLifetime());
        urlEntry.setURL(serviceURL.toString());

        SrvDeReg deregistration = new SrvDeReg();
        deregistration.setURLEntry(urlEntry);
        deregistration.setScopes(info.getScopes());
        deregistration.setTags(info.getAttributes());
        deregistration.setXID(generateXID());
        deregistration.setLanguage(info.getLanguage());

        byte[] requestBytes = serializeMessage(deregistration);

        UnicastConnector unicast = getUnicastConnector();
        Socket socket = unicast.send(requestBytes, address, false);
        byte[] replyBytes = unicast.receive(socket);
        try
        {
            Message message = Message.deserialize(replyBytes);
            if (message.getMessageType() != Message.SRV_ACK_TYPE) throw new AssertionError("BUG: expected SrvAck upon SrvReg, received instead " + message);
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
}
