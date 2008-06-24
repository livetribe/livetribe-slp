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
package org.livetribe.slp;

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.sa.ServiceAgent;
import org.livetribe.slp.sa.ServiceAgentClient;
import org.livetribe.slp.srv.Factories;
import org.livetribe.slp.ua.UserAgent;
import org.livetribe.slp.ua.UserAgentClient;

/**
 * Static factory that adds syntactic sugar to the creation of UserAgents and ServiceAgents.
 *
 * @version $Revision$ $Date$
 */
public class SLP
{
    public static UserAgentClient newUserAgentClient(Settings settings)
    {
        UserAgentClient.Factory factory = Factories.newInstance(settings, Keys.UA_CLIENT_FACTORY_KEY);
        return factory.newUserAgentClient(settings);
    }

    public static UserAgent newUserAgent(Settings settings)
    {
        UserAgent.Factory factory = Factories.newInstance(settings, Keys.UA_FACTORY_KEY);
        return factory.newUserAgent(settings);
    }

    public static ServiceAgentClient newServiceAgentClient(Settings settings)
    {
        ServiceAgentClient.Factory factory = Factories.newInstance(settings, Keys.SA_CLIENT_FACTORY_KEY);
        return factory.newServiceAgentClient(settings);
    }

    public static ServiceAgent newServiceAgent(Settings settings)
    {
        ServiceAgent.Factory factory = Factories.newInstance(settings, Keys.SA_FACTORY_KEY);
        return factory.newServiceAgent(settings);
    }
}
