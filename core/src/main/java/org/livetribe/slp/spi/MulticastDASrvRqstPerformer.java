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

import java.util.List;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.net.UDPConnector;


/**
 *
 */
public class MulticastDASrvRqstPerformer
{
    private final ServiceType serviceType = new ServiceType("service:directory-agent");
    private final Converger<DAAdvert> converger;

    public MulticastDASrvRqstPerformer(UDPConnector udpConnector, Settings settings)
    {
        converger = new Converger<DAAdvert>(udpConnector, settings);
    }

    public List<DAAdvert> perform(String language, Scopes scopes, Filter filter)
    {
        SrvRqst srvRqst = new SrvRqst();
        srvRqst.setLanguage(language);
        srvRqst.setXID(Message.newXID());
        srvRqst.setServiceType(serviceType);
        srvRqst.setScopes(scopes);
        srvRqst.setFilter(filter == null ? null : filter.asString());
        srvRqst.setMulticast(true);
        return converger.converge(srvRqst);
    }
}
