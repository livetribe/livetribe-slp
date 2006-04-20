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
package org.livetribe.slp.spi.ua;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.StandardAgentManager;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SAAdvert;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.net.UnicastConnector;

/**
 * An SLP User Agent (UA) can discover SLP ServiceAgents (SAs) and SLP DirectoryAgents(DAs).
 * <br />
 * It caches the DAs discovered, and listens for multicast DAAdverts coming from new DAs
 * or from known DAs that changed (for example, rebooted).
 * UAs also listens for SAAdverts emitted by SAs.
 * <br />
 * UAs have scopes assigned so that they can discover services belonging to those scopes.
 *
 * @version $Rev$ $Date$
 */
public class StandardUserAgentManager extends StandardAgentManager implements UserAgentManager
{
    public DAAdvert[] multicastDASrvRqst(String[] scopes, String filter, String language, long timeframe) throws IOException
    {
        SrvRqst request = createSrvRqst(new ServiceType("service:directory-agent"), scopes, filter, language);
        request.setMulticast(true);
        return convergentDASrvRqst(request, timeframe);
    }

    public SAAdvert[] multicastSASrvRqst(String[] scopes, String filter, String language, int timeframe) throws IOException
    {
        SrvRqst request = createSrvRqst(new ServiceType("service:service-agent"), scopes, filter, language);
        request.setMulticast(true);
        UnicastConnector unicast = getUnicastConnector();
        return convergentSASrvRqst(request, timeframe, unicast != null && unicast.isUnicastListening());
    }

    public SrvRply unicastSrvRqst(InetAddress address, ServiceType serviceType, String[] scopes, String filter, String language) throws IOException
    {
        SrvRqst request = createSrvRqst(serviceType, scopes, filter, language);
        byte[] requestBytes = serializeMessage(request);

        UnicastConnector unicast = getUnicastConnector();
        Socket socket = unicast.send(requestBytes, address, false);
        byte[] replyBytes = unicast.receive(socket);
        try
        {
            Message message = Message.deserialize(replyBytes);

            if (message.getMessageType() != Message.SRV_RPLY_TYPE) throw new AssertionError("BUG: expected SrvRply upon SrvRqst, received instead " + message);

            return (SrvRply)message;
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

    private SrvRqst createSrvRqst(ServiceType serviceType, String[] scopes, String filter, String language)
    {
        SrvRqst request = new SrvRqst();
        request.setLanguage(language);
        request.setXID(generateXID());
        request.setServiceType(serviceType);
        request.setScopes(scopes);
        request.setFilter(filter);
        return request;
    }
}
