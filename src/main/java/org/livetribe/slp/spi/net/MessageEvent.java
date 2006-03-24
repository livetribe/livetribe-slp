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
package org.livetribe.slp.spi.net;

import java.util.EventObject;
import java.net.InetSocketAddress;

/**
 * @version $Rev$ $Date$
 */
public class MessageEvent extends EventObject
{
    private final byte[] messageBytes;
    private final InetSocketAddress address;

    public MessageEvent(Object source, byte[] messageBytes, InetSocketAddress address)
    {
        super(source);
        this.messageBytes = messageBytes;
        this.address = address;
    }

    public byte[] getMessageBytes()
    {
        return messageBytes;
    }

    public InetSocketAddress getSocketAddress()
    {
        return address;
    }
}
