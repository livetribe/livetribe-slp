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
package org.livetribe.slp.srv.msg;

import org.livetribe.slp.ServiceLocationException;

/**
 * The RFC 2608 SrvAck message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          Service Location header (function = SrvAck = 5)      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          Error Code           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * @version $Rev$ $Date$
 */
public class SrvAck extends Message
{
    public static final int SUCCESS = 0;
    private static final int ERROR_CODE_BYTES_LENGTH = 2;

    private int errorCode;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] result = new byte[ERROR_CODE_BYTES_LENGTH];
        int offset = 0;
        writeInt(getErrorCode(), result, offset, ERROR_CODE_BYTES_LENGTH);
        return result;
    }

    protected void deserializeBody(byte[] bytes) throws ServiceLocationException
    {
        int offset = 0;
        setErrorCode(readInt(bytes, offset, ERROR_CODE_BYTES_LENGTH));
    }

    public byte getMessageType()
    {
        return SRV_ACK_TYPE;
    }

    public int getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
    }
}
