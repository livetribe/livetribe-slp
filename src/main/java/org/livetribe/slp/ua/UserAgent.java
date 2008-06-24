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

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.Server;
import org.livetribe.slp.srv.ua.IUserAgent;

/**
 * @version $Revision$ $Date$
 */
public interface UserAgent extends IUserAgent, Server
{
    public void addServiceNotificationListener(ServiceNotificationListener listener);

    public void removeServiceNotificationListener(ServiceNotificationListener listener);

    public interface Factory
    {
        public UserAgent newUserAgent(Settings settings);
    }
}
