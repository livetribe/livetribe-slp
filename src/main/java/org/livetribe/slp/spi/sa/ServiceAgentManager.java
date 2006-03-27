/*
 * Copyright 2005 the original author or authors
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
package org.livetribe.slp.spi.sa;

import java.io.IOException;
import java.net.InetAddress;

import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.spi.AgentManager;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.SrvAck;

/**
 * @version $Rev$ $Date$
 */
public interface ServiceAgentManager extends AgentManager
{
    public DAAdvert[] multicastDASrvRqst(String[] scopes, String filter, long timeframe) throws IOException;

    public SrvAck unicastSrvReg(InetAddress address, ServiceURL serviceURL, boolean newService, String[] scopes, String[] attributes) throws IOException;

    public SrvAck unicastSrvDeReg(InetAddress address, ServiceURL serviceURL, String[] scopes, String[] tags) throws IOException;
}
