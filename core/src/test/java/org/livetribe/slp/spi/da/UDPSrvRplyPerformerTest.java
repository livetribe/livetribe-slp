/**
 *
 * Copyright 2009 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.spi.da;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.spi.msg.AttributeListExtension;
import org.livetribe.slp.spi.msg.LanguageExtension;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.ScopeListExtension;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.SrvRqst;
import org.livetribe.slp.spi.net.NetUtils;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;
import org.livetribe.slp.spi.sa.UDPSrvRplyPerformer;
import org.testng.annotations.Test;

/**
 * @version $Revision$ $Date$
 */
public class UDPSrvRplyPerformerTest
{
    @Test
    public void testOverflow()
    {
        final AtomicReference<SrvRply> srvRply = new AtomicReference<SrvRply>();
        UDPSrvRplyPerformer performer = new UDPSrvRplyPerformer(null, null)
        {
            @Override
            protected void send(InetSocketAddress localAddress, InetSocketAddress remoteAddress, byte[] bytes)
            {
                srvRply.set((SrvRply)Message.deserialize(bytes));
            }
        };

        String language = Locale.ENGLISH.getLanguage();
        ServiceAgentInfo serviceAgent = ServiceAgentInfo.from(UUID.randomUUID().toString(), NetUtils.getLocalhost().getHostAddress(), Scopes.DEFAULT, Attributes.NONE, language);
        SrvRqst srvRqst = new SrvRqst();
        srvRqst.setXID(Message.newXID());
        srvRqst.setLanguage(language);
        srvRqst.addExtension(new LanguageExtension());
        srvRqst.addExtension(new ScopeListExtension());
        srvRqst.addExtension(new AttributeListExtension());

        // Without extensions, 36 URLEntries fit the srvRply.
        // With the extensions, 6 URLEntries with language, scopes and attributes.
        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/jmxrmi");
        ServiceInfo service = new ServiceInfo(serviceURL, language, Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=string),(d=\\FF\\00),e"));
        List<ServiceInfo> services = new ArrayList<ServiceInfo>();
        int count = 50;
        for (int i = 0; i < count; ++i) services.add(service);
        performer.perform(null, null, serviceAgent, srvRqst, services);

        assert srvRply.get() != null;
        assert srvRply.get().isOverflow();
        assert srvRply.get().getURLEntries().size() < count;
    }
}
