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
import org.livetribe.slp.da.DirectoryAgentListener;
import org.livetribe.slp.settings.Factories;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.Server;

/**
 * The interface of an SLP service agent non-standalone server that can be started multiple times on a single host.
 * <br />
 * A ServiceAgent is a server in the sense that once started it listens for SLP multicast messages
 * (such as directory agent advertisements or service requests). However, it does not listen on the SLP TCP port,
 * making possible to start any number of ServiceAgents on a single host without the need to setup an external server
 * or without the need to worry about already bound TCP ports (and as such it is non-standalone).
 * <br />
 * ServiceAgents are most useful when embedded in an application that need to expose services via SLP:
 * the application will normally create an instance of ServiceAgent, register one or more services, and then start
 * the ServiceAgent. Other applications in other JVMs on the same host may do exactly the same.
 * In this scenario, the ServiceAgent is in-VM with the application.
 * <br />
 * The preferred way to instantiate a ServiceAgent is the following:
 * <pre>
 * Settings settings = ...
 * ServiceAgent sa = SLP.newServiceAgent(settings);
 * sa.start();
 * </pre>
 *
 * @version $Rev: 200 $ $Date: 2006-08-09 14:17:10 +0200 (Wed, 09 Aug 2006) $
 * @see ServiceAgentClient
 * @see StandardServiceAgentServer
 * @see SLP
 */
public interface ServiceAgent extends IServiceAgent, Server
{
    /**
     * Adds a listener that will be notified of DirectoryAgents birth and death.
     *
     * @param listener the DirectoryAgentListener to add
     * @see #removeDirectoryAgentListener(DirectoryAgentListener)
     */
    public void addDirectoryAgentListener(DirectoryAgentListener listener);

    /**
     * Removes the given DirectoryAgentListener.
     *
     * @param listener the DirectoryAgentListener to remove
     * @see #addDirectoryAgentListener(DirectoryAgentListener)
     */
    public void removeDirectoryAgentListener(DirectoryAgentListener listener);

    /**
     * The factory for ServiceAgents.
     * <br />
     * The concrete factory class can be specified in the given settings with the {@link Keys#SA_FACTORY_KEY} key.
     *
     * @see Factories
     */
    public interface Factory
    {
        /**
         * Creates a new ServiceAgent.
         *
         * @param settings The configuration settings
         * @return a new ServiceAgent
         */
        public ServiceAgent newServiceAgent(Settings settings);
    }
}
