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
    private String url;
    private String[] scopes;
    private String[] attributes;
    private AuthenticationBlock[] authenticationBlocks;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        int urlLengthBytesLength = 2;
        byte[] urlBytes = stringToBytes(getURL());
        int urlBytesLength = urlBytes.length;
        int scopesLengthBytesLength = 2;
        byte[] scopesBytes = stringArrayToBytes(getScopes());
        int scopesBytesLength = scopesBytes.length;
        int attrsLengthBytesLength = 2;
        byte[] attrsBytes = stringArrayToBytes(getAttributes());
        int attrsBytesLength = attrsBytes.length;
        int authBlocksCountBytesLength = 1;
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

        int bodyLength = urlLengthBytesLength + urlBytesLength + scopesLengthBytesLength + scopesBytesLength;
        bodyLength += attrsLengthBytesLength + attrsBytesLength + authBlocksCountBytesLength + authBlockBytesSum;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(urlBytesLength, result, offset, urlLengthBytesLength);

        offset += urlLengthBytesLength;
        System.arraycopy(urlBytes, 0, result, offset, urlBytesLength);

        offset += urlBytesLength;
        writeInt(scopesBytesLength, result, offset, scopesLengthBytesLength);

        offset += scopesLengthBytesLength;
        System.arraycopy(scopesBytes, 0, result, offset, scopesBytesLength);

        offset += scopesBytesLength;
        writeInt(attrsBytesLength, result, offset, attrsLengthBytesLength);

        offset += attrsLengthBytesLength;
        System.arraycopy(attrsBytes, 0, result, offset, attrsBytesLength);

        offset += attrsBytesLength;
        writeInt(authBlocksCount, result, offset, authBlocksCountBytesLength);

        offset += authBlocksCountBytesLength;
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
        int urlLengthBytesLength = 2;
        int urlLength = readInt(bytes, offset, urlLengthBytesLength);

        offset += urlLengthBytesLength;
        setURL(readString(bytes, offset, urlLength));

        offset += urlLength;
        int scopesLengthBytesLength = 2;
        int scopesLength = readInt(bytes, offset, scopesLengthBytesLength);

        offset += scopesLengthBytesLength;
        setScopes(readStringArray(bytes, offset, scopesLength));

        offset += scopesLength;
        int attrsLengthBytesLength = 2;
        int attrsLength = readInt(bytes, offset, attrsLengthBytesLength);

        offset += attrsLengthBytesLength;
        setAttributes(readStringArray(bytes, offset, attrsLength));

        offset += attrsLength;
        int authBlocksCountBytesLength = 1;
        int authBlocksCount = readInt(bytes, offset, authBlocksCountBytesLength);

        offset += authBlocksCountBytesLength;
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

    public String[] getScopes()
    {
        return scopes;
    }

    public void setScopes(String[] scopes)
    {
        this.scopes = scopes;
    }

    public String[] getAttributes()
    {
        return attributes;
    }

    public void setAttributes(String[] attributes)
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
