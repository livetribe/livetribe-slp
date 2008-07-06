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
package org.livetribe.slp.ua;

import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.ua.IUserAgent;

/**
 * The interface of an SLP user agent client.
 * <br />
 * UserAgentClients do not listen for SLP messages but only issue requests and receive replies for the
 * requests they issued.
 * UserAgentClients are used by applications that want to find services via SLP, but do not want
 * to start a {@link UserAgent} in-VM.
 * <br />
 * The preferred way to instantiate a UserAgentClient is the following:
 * <pre>
 * Settings settings = ...
 * UserAgentClient uac = SLP.newUserAgentClient(settings);
 * </pre>
 *
 * @version $Revision$ $Date$
 */
public interface UserAgentClient extends IUserAgent
{
    /**
     * The factory for UserAgentClients.
     * <br />
     * The concrete factory class can be specified in the given settings with the {@link Keys#UA_CLIENT_FACTORY_KEY} key.
     *
     * @see org.livetribe.slp.settings.Factory
     */
    public interface Factory
    {
        /**
         * Creates a new UserAgentClient.
         *
         * @param settings The configuration settings
         * @return a new UserAgentClient
         */
        public UserAgentClient newUserAgentClient(Settings settings);
    }
}
