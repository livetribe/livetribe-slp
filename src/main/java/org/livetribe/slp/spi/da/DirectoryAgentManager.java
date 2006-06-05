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
package org.livetribe.slp.spi.da;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.spi.AgentManager;

/**
 * @version $Rev$ $Date$
 */
public interface DirectoryAgentManager extends AgentManager
{
    public void multicastDAAdvert(long bootTime, Scopes scopes, Attributes attributes, Integer xid, String language) throws IOException;

    public void udpDAAdvert(InetSocketAddress address, long bootTime, Scopes scopes, Attributes attributes, Integer xid, String language) throws IOException;

    public void tcpSrvAck(Socket socket, Integer xid, String language, int errorCode) throws IOException;

    public void tcpSrvRply(Socket socket, Integer xid, String language, List serviceInfos) throws IOException;
}
