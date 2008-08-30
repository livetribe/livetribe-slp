/*
 * Copyright 2006-2008 the original author or authors
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

import java.util.ArrayList;
import java.util.List;

import org.livetribe.slp.SLPError;
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
 * |       [URL Entry 1]          ...       [URL Entry N]          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * @version $Rev$ $Date$
 */
public class SrvRply extends Rply
{
    private static final int ERROR_CODE_BYTES_LENGTH = 2;
    private static final int URL_ENTRIES_COUNT_BYTES_LENGTH = 2;

    private final List<URLEntry> urlEntries = new ArrayList<URLEntry>();
    private SLPError error = SLPError.NO_ERROR;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        List urls = getURLEntries();
        int urlEntriesCount = urls == null ? 0 : urls.size();
        byte[][] urlEntriesBytes = new byte[urlEntriesCount][];
        int urlEntriesBytesSum = 0;
        for (int i = 0; i < urlEntriesCount; ++i)
        {
            byte[] bytes = ((URLEntry)urls.get(i)).serialize();
            urlEntriesBytes[i] = bytes;
            urlEntriesBytesSum += bytes.length;
        }

        byte[] result = new byte[ERROR_CODE_BYTES_LENGTH + URL_ENTRIES_COUNT_BYTES_LENGTH + urlEntriesBytesSum];

        int offset = 0;
        writeInt(getSLPError().getCode(), result, offset, ERROR_CODE_BYTES_LENGTH);

        offset += ERROR_CODE_BYTES_LENGTH;
        writeInt(urlEntriesCount, result, offset, URL_ENTRIES_COUNT_BYTES_LENGTH);

        offset += URL_ENTRIES_COUNT_BYTES_LENGTH;
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
        int offset = 0;
        setSLPError(SLPError.from(readInt(bytes, offset, ERROR_CODE_BYTES_LENGTH)));

        // The message may be truncated if an error occurred (RFC 2608, Chapter 7)
        if (getSLPError() != SLPError.NO_ERROR && bytes.length == ERROR_CODE_BYTES_LENGTH) return;

        offset += ERROR_CODE_BYTES_LENGTH;
        int urlEntryCount = readInt(bytes, offset, URL_ENTRIES_COUNT_BYTES_LENGTH);

        offset += URL_ENTRIES_COUNT_BYTES_LENGTH;
        for (int i = 0; i < urlEntryCount; ++i)
        {
            URLEntry urlEntry = new URLEntry();
            offset += urlEntry.deserialize(bytes, offset);
            addURLEntry(urlEntry);
        }
    }

    public byte getMessageType()
    {
        return SRV_RPLY_TYPE;
    }

    public SLPError getSLPError()
    {
        return error;
    }

    public void setSLPError(SLPError error)
    {
        this.error = error;
    }

    public List<URLEntry> getURLEntries()
    {
        return urlEntries;
    }

    public void addURLEntry(URLEntry urlEntry)
    {
        urlEntries.add(urlEntry);
    }
}
