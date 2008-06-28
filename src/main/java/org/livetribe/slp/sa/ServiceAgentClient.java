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
package org.livetribe.slp.sa;

import org.livetribe.slp.SLP;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.settings.Settings;

/**
 * The interface of an SLP service agent client, that will connect via TCP on the loopback interface to a
 * {@link StandardServiceAgentServer service agent standalone server} running on the same host.
 * <br />
 * ServiceAgentClient do not listen for SLP messages but only issue requests and receive replies for the
 * requests they issued.
 * ServiceAgentClients are used by applications that want to expose their services via SLP, but do not want
 * to start a {@link ServiceAgent} in-VM. However, such deployment scenario requires the setup of an external
 * service agent standalone server.
 * <br />
 * The preferred way to instantiate a ServiceAgentClient is the following:
 * <pre>
 * Settings settings = ...
 * ServiceAgentClient sac = SLP.newServiceAgentClient(settings);
 * </pre>
 *
 * @version $Revision$ $Date$
 * @see StandardServiceAgentServer
 * @see ServiceAgent
 * @see SLP
 */
public interface ServiceAgentClient extends IServiceAgent
{
    /**
     * The factory for ServiceAgentClients.
     * <br />
     * The concrete factory class can be specified in the given settings with the {@link Keys#SA_CLIENT_FACTORY_KEY} key.
     *
     * @see org.livetribe.slp.settings.Factory
     */
    public interface Factory
    {
        /**
         * Creates a new ServiceAgentClient.
         *
         * @param settings The configuration settings
         * @return a new ServiceAgentClient
         */
        public ServiceAgentClient newServiceAgentClient(Settings settings);
    }
}
