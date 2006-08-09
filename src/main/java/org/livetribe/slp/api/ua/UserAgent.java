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

import java.io.IOException;
import java.util.List;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.api.Agent;
import org.livetribe.slp.spi.MessageRegistrationListener;
import org.livetribe.slp.spi.ua.UserAgentManager;

/**
 * @version $Rev$ $Date$
 */
public interface UserAgent extends Agent
{
    public void setUserAgentManager(UserAgentManager manager);

    public List findServices(ServiceType serviceType, Scopes scopes, String filter, String language) throws IOException, ServiceLocationException;

    public void addMessageRegistrationListener(MessageRegistrationListener listener);

    public void removeMessageRegistrationListener(MessageRegistrationListener listener);
}
