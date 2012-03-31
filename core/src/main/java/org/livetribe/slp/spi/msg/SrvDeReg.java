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

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
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
 */
public class SrvDeReg extends Message
{
    private static final int SCOPES_LENGTH_BYTES_LENGTH = 2;
    private static final int TAGS_LENGTH_BYTES_LENGTH = 2;

    private Scopes scopes;
    private URLEntry urlEntry;
    private Attributes tags;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] scopesBytes = scopesToBytes(getScopes());
        int scopesLength = scopesBytes.length;
        byte[] urlBytes = getURLEntry().serialize();
        int urlLength = urlBytes.length;
        byte[] tagsBytes = tagsToBytes(getTags());
        int tagsLength = tagsBytes.length;

        int bodyLength = SCOPES_LENGTH_BYTES_LENGTH + scopesLength + urlLength + TAGS_LENGTH_BYTES_LENGTH + tagsLength;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(scopesLength, result, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        System.arraycopy(scopesBytes, 0, result, offset, scopesLength);

        offset += scopesLength;
        System.arraycopy(urlBytes, 0, result, offset, urlLength);

        offset += urlLength;
        writeInt(tagsLength, result, offset, TAGS_LENGTH_BYTES_LENGTH);

        offset += TAGS_LENGTH_BYTES_LENGTH;
        System.arraycopy(tagsBytes, 0, result, offset, tagsLength);

        return result;
    }

    protected void deserializeBody(byte[] bytes) throws ServiceLocationException
    {
        int offset = 0;
        int scopesLength = readInt(bytes, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        setScopes(Scopes.from(readStringArray(bytes, offset, scopesLength, false)));

        offset += scopesLength;
        URLEntry url = new URLEntry();
        offset += url.deserialize(bytes, offset);
        setURLEntry(url);

        int tagsLength = readInt(bytes, offset, TAGS_LENGTH_BYTES_LENGTH);

        offset += TAGS_LENGTH_BYTES_LENGTH;
        setTags(Attributes.fromTags(readString(bytes, offset, tagsLength, false)));
    }

    public byte getMessageType()
    {
        return SRV_DEREG_TYPE;
    }

    public Scopes getScopes()
    {
        return scopes;
    }

    public void setScopes(Scopes scopes)
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

    public Attributes getTags()
    {
        return tags;
    }

    public void setTags(Attributes tags)
    {
        this.tags = tags;
    }

    public boolean isUpdating()
    {
        // SLP mistake here IMO: every message has a fresh flag that can be used to indicate update or not.
        // However, SLP mandates that during deregistration update is identified by the presence of tags.
        boolean updating = !getTags().isEmpty();
//        if (!updating) updating = !isFresh(); // Extension to use the fresh flag
        return updating;
    }
}
