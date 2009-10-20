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
package org.livetribe.slp.ua;

import org.livetribe.slp.sa.ServiceNotificationListener;
import org.livetribe.slp.settings.Factories;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.DirectoryAgentNotifier;
import org.livetribe.slp.spi.Server;
import org.livetribe.slp.spi.ua.IUserAgent;


/**
 * The interface of an SLP user agent non-standalone server that can be started multiple times on a single host.
 * <br />
 * A UserAgent is a server in the sense that once started it listens for SLP multicast messages
 * (such as directory agent advertisements, service registration notifications or service deregistration notifications).
 * <br />
 * UserAgents are most useful when embedded in an application that need to be aware of service registrations
 * or deregistrations in absence of directory agents.
 * In this scenario, the ServiceAgent is in-VM with the application.
 * <br />
 * The preferred way to instantiate a UserAgent is the following:
 * <pre>
 * Settings settings = ...
 * UserAgent ua = SLP.newUserAgent(settings);
 * ua.start();
 * </pre>
 *
 * @version $Revision$ $Date$
 */
public interface UserAgent extends IUserAgent, DirectoryAgentNotifier, Server
{
    /**
     * Adds a listener that will be notified of service registrations and deregistration
     * when no directory agents are deployed in the network.
     *
     * @param listener the ServiceNotificationListener to add
     * @see #removeServiceNotificationListener(ServiceNotificationListener)
     */
    public void addServiceNotificationListener(ServiceNotificationListener listener);

    /**
     * Removes the given ServiceNotificationListener.
     *
     * @param listener the ServiceNotificationListener to remove
     * @see #addServiceNotificationListener(ServiceNotificationListener)
     */
    public void removeServiceNotificationListener(ServiceNotificationListener listener);

    /**
     * The factory for UserAgents.
     * <br />
     * The concrete factory class can be specified in the given settings with the {@link Keys#UA_FACTORY_KEY} key.
     *
     * @see Factories
     */
    public interface Factory
    {
        /**
         * Creates a new UserAgent.
         *
         * @param settings The configuration settings
         * @return a new UserAgent
         */
        public UserAgent newUserAgent(Settings settings);
    }
}
