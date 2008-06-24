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

import java.util.List;

import org.livetribe.slp.srv.net.UDPConnector;
import org.livetribe.slp.srv.msg.SrvRply;
import org.livetribe.slp.srv.msg.SrvRqst;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.msg.AttributeListExtension;
import org.livetribe.slp.srv.msg.ScopeListExtension;
import org.livetribe.slp.srv.msg.LanguageExtension;
import org.livetribe.slp.srv.filter.Filter;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.settings.Settings;

/**
 * @version $Revision$ $Date$
 */
public class MulticastSrvRqstPerformer
{
    private final SrvRqstConverger converger;

    public MulticastSrvRqstPerformer(UDPConnector udpConnector, Settings settings)
    {
        converger = new SrvRqstConverger(udpConnector, settings);
    }

    public List<SrvRply> perform(ServiceType serviceType, Scopes scopes, Filter filter, String language)
    {
        SrvRqst srvRqst = new SrvRqst();
        srvRqst.setLanguage(language);
        srvRqst.setXID(Message.newXID());
        srvRqst.setServiceType(serviceType);
        srvRqst.setScopes(scopes);
        srvRqst.setFilter(filter == null ? null : filter.asString());
        srvRqst.setMulticast(true);
        // Ask to send the language if it's missing in the request
        if (language == null) srvRqst.addExtension(new LanguageExtension());
        // Ask to send the Attributes as well (RFC 3059)
        srvRqst.addExtension(new AttributeListExtension());
        // Ask to send the Scopes as well
        srvRqst.addExtension(new ScopeListExtension());
        return converger.converge(srvRqst);
    }
}
