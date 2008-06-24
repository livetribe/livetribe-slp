/*
 * Copyright 2005-2008 the original author or authors
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

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;

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
 *
 * @version $Rev$ $Date$
 */
public class DAAdvert extends Rply
{
    private static final int ERROR_CODE_BYTES_LENGTH = 2;
    private static final int BOOT_TIME_BYTES_LENGTH = 4;
    private static final int URL_LENGTH_BYTES_LENGTH = 2;
    private static final int SCOPES_LENGTH_BYTES_LENGTH = 2;
    private static final int ATTRIBUTES_LENGTH_BYTES_LENGTH = 2;
    private static final int SPI_LENGTH_BYTES_LENGTH = 2;
    private static final int AUTH_BLOCKS_COUNT_BYTES_LENGTH = 1;

    private int errorCode;
    private int bootTime;
    private String url;
    private Scopes scopes;
    private Attributes attributes;
    private String[] securityParamIndexes;
    private AuthenticationBlock[] authenticationBlocks;

    public byte getMessageType()
    {
        return DA_ADVERT_TYPE;
    }

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] urlBytes = writeString(getURL(), true);
        int urlBytesLength = urlBytes.length;
        byte[] scopesBytes = scopesToBytes(getScopes());
        int scopesBytesLength = scopesBytes.length;
        byte[] attrsBytes = attributesToBytes(getAttributes());
        int attrsBytesLength = attrsBytes.length;
        byte[] securityParamsBytes = writeStringArray(getSecurityParameterIndexes(), true);
        int securityParamsBytesLength = securityParamsBytes.length;
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

        int bodyLength = ERROR_CODE_BYTES_LENGTH + BOOT_TIME_BYTES_LENGTH + URL_LENGTH_BYTES_LENGTH + urlBytesLength + SCOPES_LENGTH_BYTES_LENGTH + scopesBytesLength;
        bodyLength += ATTRIBUTES_LENGTH_BYTES_LENGTH + attrsBytesLength + SPI_LENGTH_BYTES_LENGTH + securityParamsBytesLength + AUTH_BLOCKS_COUNT_BYTES_LENGTH + authBlockBytesSum;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(getErrorCode(), result, offset, ERROR_CODE_BYTES_LENGTH);

        offset += ERROR_CODE_BYTES_LENGTH;
        writeInt(getBootTime(), result, offset, BOOT_TIME_BYTES_LENGTH);

        offset += BOOT_TIME_BYTES_LENGTH;
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
        writeInt(securityParamsBytesLength, result, offset, SPI_LENGTH_BYTES_LENGTH);

        offset += SPI_LENGTH_BYTES_LENGTH;
        System.arraycopy(securityParamsBytes, 0, result, offset, securityParamsBytesLength);

        offset += securityParamsBytesLength;
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
        setErrorCode(readInt(bytes, offset, ERROR_CODE_BYTES_LENGTH));

        offset += ERROR_CODE_BYTES_LENGTH;
        setBootTime(readInt(bytes, offset, BOOT_TIME_BYTES_LENGTH));

        offset += BOOT_TIME_BYTES_LENGTH;
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
        int securityParamsLength = readInt(bytes, offset, SPI_LENGTH_BYTES_LENGTH);

        offset += SPI_LENGTH_BYTES_LENGTH;
        setSecurityParamIndexes(readStringArray(bytes, offset, securityParamsLength, true));

        offset += securityParamsLength;
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

    public int getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
    }

    public int getBootTime()
    {
        return bootTime;
    }

    public void setBootTime(int bootTime)
    {
        this.bootTime = bootTime;
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

    public String[] getSecurityParameterIndexes()
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

    public String toString()
    {
        StringBuilder result = new StringBuilder("[DAAdvert@").append(Integer.toHexString(hashCode()));
        result.append(" responder(");
        String responder = getResponder();
        if (responder != null) result.append(responder);
        result.append(")");
        result.append(" ").append(getErrorCode());
        result.append(" ").append(new Date(TimeUnit.SECONDS.toMillis(getBootTime())));
        result.append(" ").append(getURL());
        result.append(" ").append(getScopes());
        result.append(" {");
        Attributes attrs = getAttributes();
        if (attrs != null) result.append(attrs);
        result.append("}");
        String[] spis = getSecurityParameterIndexes();
        if (spis != null) result.append(" ").append(Arrays.asList(spis));
        result.append("]");
        return result.toString();
    }
}
