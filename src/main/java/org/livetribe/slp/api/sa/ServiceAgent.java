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

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Agent;
import org.livetribe.slp.spi.sa.ServiceAgentManager;

/**
 * @version $Rev$ $Date$
 */
public interface ServiceAgent extends Agent
{
    public void setServiceAgentManager(ServiceAgentManager manager);

    public void setServiceURL(ServiceURL serviceURL);

    public void setAttributes(Attributes attributes);

    public void setScopes(String[] scopes);

    public void register() throws IOException, ServiceLocationException;

    public void deregister() throws IOException, ServiceLocationException;
}
