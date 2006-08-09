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
package org.livetribe.slp.api.sa;

import java.io.IOException;

import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.api.Agent;
import org.livetribe.slp.spi.sa.ServiceAgentManager;

/**
 * @version $Rev$ $Date$
 */
public interface ServiceAgent extends Agent
{
    public void setServiceAgentManager(ServiceAgentManager manager);

    /**
     * Registers the given service with directory agents.
     * If this SA is not yet started, the service will be scheduled for registration
     * when the SA is started.
     * @param service The service to register
     * @throws IOException
     * @throws ServiceLocationException
     */
    public void register(ServiceInfo service) throws IOException, ServiceLocationException;

    public void deregister(ServiceInfo service) throws IOException, ServiceLocationException;
}
