/*
 * Copyright 2008-2008 the original author or authors
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
package org.livetribe.slp.spi;

import java.net.Socket;
import java.util.List;

import org.livetribe.slp.SLPError;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvTypeRply;
import org.livetribe.slp.spi.net.TCPConnector;


/**
 *
 */
public class TCPSrvTypeRplyPerformer extends SrvTypeRplyPerformer
{
    private final TCPConnector tcpConnector;

    public TCPSrvTypeRplyPerformer(TCPConnector tcpConnector, Settings settings)
    {
        this.tcpConnector = tcpConnector;
    }

    public void perform(Socket socket, Message message, List<ServiceType> serviceTypes)
    {
        SrvTypeRply srvTypeRply = newSrvTypeRply(message, serviceTypes);
        byte[] srvTypeRplyBytes = srvTypeRply.serialize();
        tcpConnector.write(socket, srvTypeRplyBytes);
    }

    public void perform(Socket socket, Message message, SLPError error)
    {
        SrvTypeRply srvTypeRply = newSrvTypeRply(message, error);
        byte[] srvTypeRplyBytes = srvTypeRply.serialize();
        tcpConnector.write(socket, srvTypeRplyBytes);
    }
}
