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
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.annotations.Test;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.mock.BitMatch;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;
import org.livetribe.slp.spi.sa.UDPSrvRplyPerformer;


/**
 * @version $Revision$ $Date$
 */
public class UDPSrvRplyPerformerTest
{
    @Test
    public void testOverflow() throws Exception
    {
        Mockery context = new Mockery();
        final AtomicReference reference = new AtomicReference();
        final UDPConnector connector = context.mock(UDPConnector.class);

        context.checking(new Expectations()
        {{
                oneOf(connector).send(with(aNonNull(String.class)), with(aNonNull(InetSocketAddress.class)), with(new BitMatch(5, (byte) 0x80)));
                will(new CustomAction("Test")
                {
                    @SuppressWarnings({"unchecked"})
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        reference.set(invocation.getParameter(2));
                        return null;
                    }
                });
            }});

        org.livetribe.slp.spi.sa.UDPSrvRplyPerformer usrp = new UDPSrvRplyPerformer(connector, null);

        List<ServiceInfo> list = new ArrayList<ServiceInfo>();
        for (int i = 0; i < 100; i++) list.add(new ServiceInfo(new ServiceURL("test://localhost:" + i), "en", Scopes.ANY, Attributes.NONE));

        usrp.perform(new InetSocketAddress("localhost", 4207),
                     new InetSocketAddress("localhost", 4207),
                     ServiceAgentInfo.from("id", "localhost", Scopes.ANY, Attributes.NONE, "en"),
                     new SrvRply(),
                     list);

        byte[] bytes = (byte[]) reference.get();

        assert bytes.length == Defaults.get(Keys.MAX_TRANSMISSION_UNIT_KEY);

        context.assertIsSatisfied();
    }
}
