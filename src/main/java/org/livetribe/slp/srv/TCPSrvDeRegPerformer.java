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
package org.livetribe.slp.srv;

import java.net.InetSocketAddress;

import org.livetribe.slp.srv.msg.SrvAck;
import org.livetribe.slp.srv.msg.SrvDeReg;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.msg.URLEntry;
import org.livetribe.slp.srv.net.TCPConnector;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.settings.Settings;

/**
 * @version $Revision$ $Date$
 */
public class TCPSrvDeRegPerformer
{
    private final TCPConnector tcpConnector;

    public TCPSrvDeRegPerformer(TCPConnector tcpConnector, Settings settings)
    {
        this.tcpConnector = tcpConnector;
    }

    public SrvAck perform(InetSocketAddress remoteAddress, ServiceInfo service, boolean update)
    {
        SrvDeReg srvDeReg = newSrvDeReg(service, update);
        byte[] requestBytes = srvDeReg.serialize();
        byte[] replyBytes = tcpConnector.writeAndRead(remoteAddress, requestBytes);
        return (SrvAck)Message.deserialize(replyBytes);
    }

    private SrvDeReg newSrvDeReg(ServiceInfo service, boolean update)
    {
        ServiceURL serviceURL = service.getServiceURL();
        URLEntry urlEntry = new URLEntry();
        urlEntry.setLifetime(serviceURL.getLifetime());
        urlEntry.setURL(serviceURL.getURL());
        SrvDeReg srvDeReg = new SrvDeReg();
        srvDeReg.setFresh(!update);
        srvDeReg.setURLEntry(urlEntry);
        srvDeReg.setScopes(service.getScopes());
        srvDeReg.setTags(service.getAttributes());
        srvDeReg.setXID(Message.newXID());
        srvDeReg.setLanguage(service.getLanguage());
        return srvDeReg;
    }
}
