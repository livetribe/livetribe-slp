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

import org.livetribe.slp.SLPTestSupport;
import org.livetribe.slp.api.Configuration;

/**
 * @version $Rev$ $Date$
 */
public class StandardUserAgentManagerTest extends SLPTestSupport
{
    /**
     * @testng.test
     */
    public void testStartStop() throws Exception
    {
        Configuration configuration = new Configuration();
        configuration.setPort(1427);

        StandardUserAgentManager agent = new StandardUserAgentManager();
        agent.setConfiguration(configuration);

        assertFalse(agent.isRunning());

        agent.start();
        assertTrue(agent.isRunning());

        agent.stop();
        assertFalse(agent.isRunning());

        agent.start();
        assertTrue(agent.isRunning());

        agent.stop();
        assertFalse(agent.isRunning());
    }
}
