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

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.Server;
import org.livetribe.slp.srv.sa.IServiceAgent;

/**
 * The SLP ServiceAgent server that handles service registration
 * and deregistration for applications that want to register their services.
 * <p/>
 * TODO: rework javadocs: this interface can be implemented as a inprocess server or standalone
 * <p/>
 * Differently from {@link ServiceAgent}, multiple instances of this
 * inprocess server are allowed for each host.
 * In the typical deployment scenario each JVM that runs an application also
 * starts inprocess one instance of this ServiceAgent server.
 * <p/>
 * The SLP ServiceAgent external server that handles service registration
 * or deregistration through a {@link ServiceAgentClient} of applications
 * that want to register their services.
 * Differently from {@link ServiceAgent}, only one instance of this server
 * is allowed per each host.
 * In the typical deployment scenario, one JVM that runs this ServiceAgent
 * server, and other JVMs run applications that register to the
 * ServiceAgent server their services using {@link ServiceAgentClient}.
 *
 * @version $Rev: 200 $ $Date: 2006-08-09 14:17:10 +0200 (Wed, 09 Aug 2006) $
 */
public interface ServiceAgent extends IServiceAgent, Server
{
    public interface Factory
    {
        public ServiceAgent newServiceAgent(Settings settings);
    }
}
