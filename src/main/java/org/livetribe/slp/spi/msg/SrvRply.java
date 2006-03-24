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
package org.livetribe.slp.spi.msg;

import org.livetribe.slp.ServiceLocationException;

/**
 * The RFC 2608 SrvRply message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |        Service Location header (function = SrvRply = 2)       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |        Error Code             |        URL Entry count        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       &lt;URL Entry 1&gt;          ...       &lt;URL Entry N&gt;          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * @version $Rev$ $Date$
 */
public class SrvRply extends Rply
{
    private int errorCode;
    private URLEntry[] urlEntries;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        int errorCodeBytesLength = 2;
        int urlEntriesCountBytesLength = 2;
        URLEntry[] urls = getURLEntries();
        int urlEntriesCount = urls == null ? 0 : urls.length;
        byte[][] urlEntriesBytes = new byte[urlEntriesCount][];
        int urlEntriesBytesSum = 0;
        for (int i = 0; i < urlEntriesCount; ++i)
        {
            byte[] bytes = urls[i].serialize();
            urlEntriesBytes[i] = bytes;
            urlEntriesBytesSum += bytes.length;
        }

        byte[] result = new byte[errorCodeBytesLength + urlEntriesCountBytesLength + urlEntriesBytesSum];

        int offset = 0;
        writeInt(getErrorCode(), result, offset, errorCodeBytesLength);

        offset += errorCodeBytesLength;
        writeInt(urlEntriesCount, result, offset, urlEntriesCountBytesLength);

        offset += urlEntriesCountBytesLength;
        for (int i = 0; i < urlEntriesCount; ++i)
        {
            byte[] bytes = urlEntriesBytes[i];
            int length = bytes.length;
            System.arraycopy(bytes, 0, result, offset, length);
            offset += length;
        }

        return result;
    }

    protected void deserializeBody(byte[] bytes) throws ServiceLocationException
    {
        int errorCodeBytesLength = 2;
        int offset = 0;
        setErrorCode(readInt(bytes, offset, errorCodeBytesLength));

        // The message may be truncated if an error occurred (RFC 2608, Chapter 7)
        if (getErrorCode() != 0 && bytes.length == errorCodeBytesLength) return;

        int urlEntryCountBytesLength = 2;
        offset += errorCodeBytesLength;
        int urlEntryCount = readInt(bytes, offset, urlEntryCountBytesLength);

        offset += urlEntryCountBytesLength;
        URLEntry[] urls = new URLEntry[urlEntryCount];
        for (int i = 0; i < urlEntryCount; ++i)
        {
            urls[i] = new URLEntry();
            offset += urls[i].deserialize(bytes, offset);
        }
        setURLEntries(urls);
    }

    public byte getMessageType()
    {
        return SRV_RPLY_TYPE;
    }

    public int getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
    }

    public URLEntry[] getURLEntries()
    {
        return urlEntries;
    }

    public void setURLEntries(URLEntry[] urlEntries)
    {
        this.urlEntries = urlEntries;
    }
}
