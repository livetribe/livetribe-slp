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
package org.livetribe.slp.spi;

import java.net.InetSocketAddress;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.msg.AttributeListExtension;
import org.livetribe.slp.spi.msg.LanguageExtension;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.ScopeListExtension;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.net.TCPConnector;

/**
 * @version $Revision$ $Date$
 */
public class TCPSrvRqstPerformer
{
    private final TCPConnector tcpConnector;

    public TCPSrvRqstPerformer(TCPConnector tcpConnector, Settings settings)
    {
        this.tcpConnector = tcpConnector;
    }

    public SrvRply perform(InetSocketAddress address, ServiceType serviceType, String language, Scopes scopes, Filter filter)
    {
        SrvRqst srvRqst = newSrvRqst(serviceType, language, scopes, filter);
        byte[] srvRqstBytes = srvRqst.serialize();
        byte[] srvRplyBytes = tcpConnector.writeAndRead(address, srvRqstBytes);
        return (SrvRply)Message.deserialize(srvRplyBytes);
    }

    private SrvRqst newSrvRqst(ServiceType serviceType, String language, Scopes scopes, Filter filter)
    {
        SrvRqst srvRqst = new SrvRqst();
        srvRqst.setLanguage(language);
        srvRqst.setXID(Message.newXID());
        srvRqst.setServiceType(serviceType);
        srvRqst.setScopes(scopes);
        srvRqst.setFilter(filter == null ? null : filter.asString());
        // Ask to send the language if it's missing in the request
        if (language == null) srvRqst.addExtension(new LanguageExtension());
        // Ask to send the Attributes as well (RFC 3059)
        srvRqst.addExtension(new AttributeListExtension());
        // Ask to send the Scopes as well
        srvRqst.addExtension(new ScopeListExtension());
        return srvRqst;
    }
}
