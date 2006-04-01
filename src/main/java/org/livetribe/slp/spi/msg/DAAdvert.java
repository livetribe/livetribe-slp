/*
 * Copyright 2005 the original author or authors
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
import org.livetribe.slp.Attributes;

/**
 * The RFC 2608 DAAdvert message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |        Service Location header (function = DAAdvert = 8)      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          Error Code           |  DA Stateless Boot Timestamp  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |DA Stateless Boot Time,, contd.|         Length of URL         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * \                              URL                              \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Length of [scope-list]    |         [scope-list]          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Length of [attr-list]     |          [attr-list]          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Length of [SLP SPI List]   |     [SLP SPI List] String     \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | # Auth Blocks |         Authentication block (if any)         \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * @version $Rev$ $Date$
 */
public class DAAdvert extends Rply
{
    private int errorCode;
    private long bootTime;
    private String url;
    private String[] scopes;
    private Attributes attributes;
    private String[] securityParamIndexes;
    private AuthenticationBlock[] authenticationBlocks;

    public byte getMessageType()
    {
        return DA_ADVERT_TYPE;
    }

    protected byte[] serializeBody() throws ServiceLocationException
    {
        int errorCodeBytesLength = 2;
        int bootTimeBytesLength = 4;
        int urlLengthBytesLength = 2;
        byte[] urlBytes = stringToBytes(getURL());
        int urlBytesLength = urlBytes.length;
        int scopesLengthBytesLength = 2;
        byte[] scopesBytes = stringArrayToBytes(getScopes());
        int scopesBytesLength = scopesBytes.length;
        int attrsLengthBytesLength = 2;
        byte[] attrsBytes = attributesToBytes(getAttributes());
        int attrsBytesLength = attrsBytes.length;
        int securityParamsLengthBytesLength = 2;
        byte[] securityParamsBytes = stringArrayToBytes(getSecurityParamIndexes());
        int securityParamsBytesLength = securityParamsBytes.length;
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

        int bodyLength = errorCodeBytesLength + bootTimeBytesLength + urlLengthBytesLength + urlBytesLength + scopesLengthBytesLength + scopesBytesLength;
        bodyLength += attrsLengthBytesLength + attrsBytesLength + securityParamsLengthBytesLength + securityParamsBytesLength + authBlocksCountBytesLength + authBlockBytesSum;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(getErrorCode(), result, offset, errorCodeBytesLength);

        offset += errorCodeBytesLength;
        writeInt((int)(getBootTime() / 1000), result, offset, bootTimeBytesLength);

        offset += bootTimeBytesLength;
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
        writeInt(securityParamsBytesLength, result, offset, securityParamsLengthBytesLength);

        offset += securityParamsLengthBytesLength;
        System.arraycopy(securityParamsBytes, 0, result, offset, securityParamsBytesLength);

        offset += securityParamsBytesLength;
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
        int errorCodeBytesLength = 2;
        setErrorCode(readInt(bytes, offset, errorCodeBytesLength));

        offset += errorCodeBytesLength;
        int bootTimeBytesLength = 4;
        setBootTime(readInt(bytes, offset, bootTimeBytesLength) * 1000L);

        offset += bootTimeBytesLength;
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
        setAttributes(new Attributes(readString(bytes, offset, attrsLength)));

        offset += attrsLength;
        int securityParamsLengthBytesLength = 2;
        int securityParamsLength = readInt(bytes, offset, securityParamsLengthBytesLength);

        offset += securityParamsLengthBytesLength;
        setSecurityParamIndexes(readStringArray(bytes, offset, securityParamsLength));

        offset += securityParamsLength;
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

    public int getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
    }

    public long getBootTime()
    {
        return bootTime;
    }

    public void setBootTime(long bootTime)
    {
        // Round the given time
        this.bootTime = (bootTime / 1000L) * 1000L;
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

    public Attributes getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    public String[] getSecurityParamIndexes()
    {
        return securityParamIndexes;
    }

    public void setSecurityParamIndexes(String[] securityParamIndexes)
    {
        this.securityParamIndexes = securityParamIndexes;
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
