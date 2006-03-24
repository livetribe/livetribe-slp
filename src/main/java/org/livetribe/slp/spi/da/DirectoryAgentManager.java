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
import java.net.InetAddress;
import java.net.Socket;

import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.spi.AgentManager;

/**
 * $Rev$
 */
public interface DirectoryAgentManager extends AgentManager
{
    public void multicastDAAdvert(long bootTime, String[] scopes, String[] attributes, Integer xid, String language) throws IOException;

    public void unicastDAAdvert(InetAddress address, long bootTime, String[] scopes, String[] attributes, Integer xid, String language) throws IOException;

    public void unicastSrvAck(Socket socket, Integer xid, String language, int errorCode) throws IOException;

    public void unicastSrvRply(Socket socket, Integer xid, String language, ServiceURL[] serviceURLs) throws IOException;
}
