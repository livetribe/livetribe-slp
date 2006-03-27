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
 * The RFC 2608 SrvDeReg message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Service Location header (function = SrvDeReg = 4)     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Length of [scope-list]     |         [scope-list]          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           URL Entry                           \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      Length of [tag-list]     |            [tag-list]         \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * @version $Rev$ $Date$
 */
public class SrvDeReg extends Message
{
    private String[] scopes;
    private URLEntry urlEntry;
    private String[] tags;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        int scopesLengthBytesLength = 2;
        byte[] scopesBytes = stringArrayToBytes(getScopes());
        int scopesLength = scopesBytes.length;
        byte[] urlBytes = getURLEntry().serialize();
        int urlLength = urlBytes.length;
        int tagsLengthBytesLength = 2;
        byte[] tagsBytes = stringArrayToBytes(getTags());
        int tagsLength = tagsBytes.length;

        int bodyLength = scopesLengthBytesLength + scopesLength + urlLength + tagsLengthBytesLength + tagsLength;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(scopesLength, result, offset, scopesLengthBytesLength);

        offset += scopesLengthBytesLength;
        System.arraycopy(scopesBytes, 0, result, offset, scopesLength);

        offset += scopesLength;
        System.arraycopy(urlBytes, 0, result, offset, urlLength);

        offset += urlLength;
        writeInt(tagsLength, result, offset, tagsLengthBytesLength);

        offset += tagsLengthBytesLength;
        System.arraycopy(tagsBytes, 0, result, offset, tagsLength);

        return result;
    }

    protected void deserializeBody(byte[] bytes) throws ServiceLocationException
    {
        int offset = 0;
        int scopesLengthBytesLength = 2;
        int scopesLength = readInt(bytes, offset, scopesLengthBytesLength);

        offset += scopesLengthBytesLength;
        setScopes(readStringArray(bytes, offset, scopesLength));

        offset += scopesLength;
        URLEntry url = new URLEntry();
        offset += url.deserialize(bytes, offset);
        setURLEntry(url);

        int tagsLengthBytesLength = 2;
        int tagsLength = readInt(bytes, offset, tagsLengthBytesLength);

        offset += tagsLengthBytesLength;
        setTags(readStringArray(bytes, offset, tagsLength));
    }

    public byte getMessageType()
    {
        return SRV_DEREG_TYPE;
    }

    public String[] getScopes()
    {
        return scopes;
    }

    public void setScopes(String[] scopes)
    {
        this.scopes = scopes;
    }

    public URLEntry getURLEntry()
    {
        return urlEntry;
    }

    public void setURLEntry(URLEntry urlEntry)
    {
        this.urlEntry = urlEntry;
    }

    public String[] getTags()
    {
        return tags;
    }

    public void setTags(String[] tags)
    {
        this.tags = tags;
    }
}
