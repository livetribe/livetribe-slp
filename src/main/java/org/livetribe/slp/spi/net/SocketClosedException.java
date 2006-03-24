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

/**
 * Thrown when reading an SLP message from an InputStream and the client closes the socket before
 * all message bytes has been sent (the length is in the header of SLP messages).
 *
 * @version $Rev$ $Date$
 */
public class SocketClosedException extends RuntimeException
{
    public SocketClosedException()
    {
    }

    public SocketClosedException(String message)
    {
        super(message);
    }

    public SocketClosedException(Throwable cause)
    {
        super(cause);
    }

    public SocketClosedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
