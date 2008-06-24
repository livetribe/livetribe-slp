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

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;

/**
 * The RFC 2608 DAAdvert message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |        Service Location header (function = SAAdvert = 11)     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Length of URL         |              URL              \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Length of [scope-list]    |         [scope-list]          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Length of [attr-list]     |          [attr-list]          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | # auth blocks |        authentication block (if any)          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * @version $Rev$ $Date$
 */
public class SAAdvert extends Rply
{
    private static final int URL_LENGTH_BYTES_LENGTH = 2;
    private static final int SCOPES_LENGTH_BYTES_LENGTH = 2;
    private static final int ATTRIBUTES_LENGTH_BYTES_LENGTH = 2;
    private static final int AUTH_BLOCKS_COUNT_BYTES_LENGTH = 1;

    private String url;
    private Scopes scopes;
    private Attributes attributes;
    private AuthenticationBlock[] authenticationBlocks;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] urlBytes = writeString(getURL(), true);
        int urlBytesLength = urlBytes.length;
        byte[] scopesBytes = scopesToBytes(getScopes());
        int scopesBytesLength = scopesBytes.length;
        byte[] attrsBytes = attributesToBytes(getAttributes());
        int attrsBytesLength = attrsBytes.length;
        AuthenticationBlock[] blocks = getAuthenticationBlocks();
        int authBlocksCount = blocks == null ? 0 : blocks.length;
        byte[][] authBlocksBytes = new byte[authBlocksCount][];
        int authBlockBytesSum = 0;
        for (int i = 0; i < authBlocksCount; ++i)
        {
            byte[] bytes = blocks[i].serialize();
            authBlocksBytes[i] = bytes;
            authBlockBytesSum += bytes.length;
        }

        int bodyLength = URL_LENGTH_BYTES_LENGTH + urlBytesLength + SCOPES_LENGTH_BYTES_LENGTH + scopesBytesLength;
        bodyLength += ATTRIBUTES_LENGTH_BYTES_LENGTH + attrsBytesLength + AUTH_BLOCKS_COUNT_BYTES_LENGTH + authBlockBytesSum;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(urlBytesLength, result, offset, URL_LENGTH_BYTES_LENGTH);

        offset += URL_LENGTH_BYTES_LENGTH;
        System.arraycopy(urlBytes, 0, result, offset, urlBytesLength);

        offset += urlBytesLength;
        writeInt(scopesBytesLength, result, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        System.arraycopy(scopesBytes, 0, result, offset, scopesBytesLength);

        offset += scopesBytesLength;
        writeInt(attrsBytesLength, result, offset, ATTRIBUTES_LENGTH_BYTES_LENGTH);

        offset += ATTRIBUTES_LENGTH_BYTES_LENGTH;
        System.arraycopy(attrsBytes, 0, result, offset, attrsBytesLength);

        offset += attrsBytesLength;
        writeInt(authBlocksCount, result, offset, AUTH_BLOCKS_COUNT_BYTES_LENGTH);

        offset += AUTH_BLOCKS_COUNT_BYTES_LENGTH;
        for (int i = 0; i < authBlocksCount; ++i)
        {
            byte[] bytes = authBlocksBytes[i];
            int length = bytes.length;
            System.arraycopy(bytes, 0, result, offset, length);
            offset += length;
        }

        return result;
    }

    protected void deserializeBody(byte[] bytes) throws ServiceLocationException
    {
        int offset = 0;
        int urlLength = readInt(bytes, offset, URL_LENGTH_BYTES_LENGTH);

        offset += URL_LENGTH_BYTES_LENGTH;
        setURL(readString(bytes, offset, urlLength, true));

        offset += urlLength;
        int scopesLength = readInt(bytes, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        setScopes(Scopes.from(readStringArray(bytes, offset, scopesLength, false)));

        offset += scopesLength;
        int attrsLength = readInt(bytes, offset, ATTRIBUTES_LENGTH_BYTES_LENGTH);

        offset += ATTRIBUTES_LENGTH_BYTES_LENGTH;
        setAttributes(Attributes.from(readString(bytes, offset, attrsLength, false)));

        offset += attrsLength;
        int authBlocksCount = readInt(bytes, offset, AUTH_BLOCKS_COUNT_BYTES_LENGTH);

        offset += AUTH_BLOCKS_COUNT_BYTES_LENGTH;
        if (authBlocksCount > 0)
        {
            AuthenticationBlock[] blocks = new AuthenticationBlock[authBlocksCount];
            for (int i = 0; i < authBlocksCount; ++i)
            {
                blocks[i] = new AuthenticationBlock();
                offset += blocks[i].deserialize(bytes, offset);
            }
            setAuthenticationBlocks(blocks);
        }
    }

    public byte getMessageType()
    {
        return SA_ADVERT_TYPE;
    }

    public String getURL()
    {
        return url;
    }

    public void setURL(String url)
    {
        this.url = url;
    }

    public Scopes getScopes()
    {
        return scopes;
    }

    public void setScopes(Scopes scopes)
    {
        this.scopes = scopes;
    }

    public Attributes getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    public AuthenticationBlock[] getAuthenticationBlocks()
    {
        return authenticationBlocks;
    }

    public void setAuthenticationBlocks(AuthenticationBlock[] authenticationBlocks)
    {
        this.authenticationBlocks = authenticationBlocks;
    }
}
