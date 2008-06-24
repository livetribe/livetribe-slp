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
package org.livetribe.slp.srv.net;

import java.net.InetSocketAddress;
import java.util.EventObject;

import org.livetribe.slp.srv.msg.Message;

/**
 * @version $Rev: 46 $ $Date: 2006-03-24 19:10:49 +0100 (Fri, 24 Mar 2006) $
 */
public class MessageEvent extends EventObject
{
    private static final long serialVersionUID = 530747609877668182L;

    private final Message message;
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;

    public MessageEvent(Object source, Message message, InetSocketAddress localAddress, InetSocketAddress remoteAddress)
    {
        super(source);
        this.message = message;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public Message getMessage()
    {
        return message;
    }

    public InetSocketAddress getLocalSocketAddress()
    {
        return localAddress;
    }

    public InetSocketAddress getRemoteSocketAddress()
    {
        return remoteAddress;
    }
}
