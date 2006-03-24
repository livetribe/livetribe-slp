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
package org.livetribe.slp.api.ua;

import org.livetribe.slp.api.SLPAPITestCase;
import org.livetribe.slp.spi.net.SocketMulticastConnector;
import org.livetribe.slp.spi.net.SocketUnicastConnector;
import org.livetribe.slp.spi.ua.StandardUserAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardUserAgentTest extends SLPAPITestCase
{
    public void testStartStop() throws Exception
    {
        StandardUserAgent ua = new StandardUserAgent();
        StandardUserAgentManager uaManager = new StandardUserAgentManager();
        uaManager.setMulticastConnector(new SocketMulticastConnector());
        uaManager.setUnicastConnector(new SocketUnicastConnector());
        ua.setUserAgentManager(uaManager);
        ua.setConfiguration(getDefaultConfiguration());

        assertFalse(ua.isRunning());
        ua.start();
        assertTrue(ua.isRunning());
        ua.stop();
        assertFalse(ua.isRunning());
        ua.start();
        assertTrue(ua.isRunning());
        ua.stop();
        assertFalse(ua.isRunning());
    }

}
