/*
 * Copyright 2007-2008 the original author or authors
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

import java.util.Collection;

import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.IdentifierExtension;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * TODO: RFC 3082, 9.0, says that notification must use the convergence algorithm
 *
 * @version $Revision$ $Date$
 */
public class NotifySrvRegPerformer
{
    private final UDPConnector udpConnector;

    public NotifySrvRegPerformer(UDPConnector udpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
    }

    public void perform(Collection<ServiceAgentInfo> serviceAgents, ServiceInfo service, boolean update)
    {
        for (ServiceAgentInfo serviceAgent : serviceAgents)
        {
            SrvReg srvReg = newSrvReg(serviceAgent, service, update);
            byte[] bytes = srvReg.serialize();
            udpConnector.manycastNotify(serviceAgent.getHost(), bytes);
        }
    }

    private SrvReg newSrvReg(ServiceAgentInfo serviceAgent, ServiceInfo service, boolean update)
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
        srvReg.setMulticast(true);
        if (serviceAgent.hasIdentifier())
        {
            IdentifierExtension extension = new IdentifierExtension(serviceAgent.getHost(), serviceAgent.getIdentifier());
            srvReg.addExtension(extension);
        }
        return srvReg;
    }
}
