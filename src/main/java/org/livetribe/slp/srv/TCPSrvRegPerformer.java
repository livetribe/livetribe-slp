/*
 * Copyright 2005-2008 the original author or authors
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

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.msg.SrvAck;
import org.livetribe.slp.srv.msg.SrvReg;
import org.livetribe.slp.srv.msg.URLEntry;
import org.livetribe.slp.srv.net.TCPConnector;

/**
 * @version $Revision$ $Date$
 */
public class TCPSrvRegPerformer
{
    private final TCPConnector tcpConnector;

    public TCPSrvRegPerformer(TCPConnector tcpConnector, Settings settings)
    {
        this.tcpConnector = tcpConnector;
    }

    public SrvAck perform(InetSocketAddress address, ServiceInfo service, boolean update)
    {
        SrvReg srvReg = newSrvReg(service, update);
        byte[] requestBytes = srvReg.serialize();
        byte[] replyBytes = tcpConnector.writeAndRead(address, requestBytes);
        return (SrvAck)Message.deserialize(replyBytes);
    }

    private SrvReg newSrvReg(ServiceInfo service, boolean update)
    {
        ServiceURL serviceURL = service.getServiceURL();
        URLEntry urlEntry = new URLEntry();
        urlEntry.setLifetime(serviceURL.getLifetime());
        urlEntry.setURL(serviceURL.getURL());
        SrvReg srvReg = new SrvReg();
        srvReg.setFresh(!update);
        srvReg.setURLEntry(urlEntry);
        srvReg.setScopes(service.getScopes());
        srvReg.setAttributes(service.getAttributes());
        srvReg.setXID(Message.newXID());
        srvReg.setLanguage(service.getLanguage());
        return srvReg;
    }
}
