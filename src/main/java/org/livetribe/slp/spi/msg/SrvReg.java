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
import org.livetribe.slp.ServiceType;

/**
 * The RFC 2608 SrvReg message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Service Location header (function = SrvReg = 3)       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                          <URL-Entry>                          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | length of service type string |        <service-type>         \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     length of <scope-list>    |         <scope-list>          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  length of attr-list string   |          <attr-list>          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |# of AttrAuths |(if present) Attribute Authentication Blocks...\
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * @version $Rev$ $Date$
 */
public class SrvReg extends Message
{
    private URLEntry urlEntry;
    private ServiceType serviceType;
    private String[] scopes;
    private String[] attributes;
    private AuthenticationBlock[] authenticationBlocks;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] urlBytes = getURLEntry().serialize();
        int urlLength = urlBytes.length;
        int serviceTypeLengthBytesLength = 2;
        byte[] serviceTypeBytes = stringToBytes(getServiceType().toString());
        int serviceTypeLength = serviceTypeBytes.length;
        int scopesLengthBytesLength = 2;
        byte[] scopesBytes = stringArrayToBytes(getScopes());
        int scopesLength = scopesBytes.length;
        int attrsLengthBytesLength = 2;
        byte[] attrsBytes = stringArrayToBytes(getAttributes());
        int attrsLength = attrsBytes.length;
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

        int bodyLength = urlLength + serviceTypeLengthBytesLength + serviceTypeLength + scopesLengthBytesLength;
        bodyLength += scopesLength + attrsLengthBytesLength + attrsLength + authBlocksCountBytesLength + authBlockBytesSum;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        System.arraycopy(urlBytes, 0, result, offset, urlLength);

        offset += urlLength;
        writeInt(serviceTypeLength, result, offset, serviceTypeLengthBytesLength);

        offset += serviceTypeLengthBytesLength;
        System.arraycopy(serviceTypeBytes, 0, result, offset, serviceTypeLength);

        offset += serviceTypeLength;
        writeInt(scopesLength, result, offset, scopesLengthBytesLength);

        offset += scopesLengthBytesLength;
        System.arraycopy(scopesBytes, 0, result, offset, scopesLength);

        offset += scopesLength;
        writeInt(attrsLength, result, offset, attrsLengthBytesLength);

        offset += attrsLengthBytesLength;
        System.arraycopy(attrsBytes, 0, result, offset, attrsLength);

        offset += attrsLength;
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

        URLEntry url = new URLEntry();
        offset += url.deserialize(bytes, offset);
        setURLEntry(url);

        int serviceTypeLengthBytesLength = 2;
        int serviceTypeLength = readInt(bytes, offset, serviceTypeLengthBytesLength);

        offset += serviceTypeLengthBytesLength;
        setServiceType(new ServiceType(readString(bytes, offset, serviceTypeLength)));

        offset += serviceTypeLength;
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
        return SRV_REG_TYPE;
    }

    public URLEntry getURLEntry()
    {
        return urlEntry;
    }

    public void setURLEntry(URLEntry urlEntry)
    {
        this.urlEntry = urlEntry;
    }

    public ServiceType getServiceType()
    {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType)
    {
        this.serviceType = serviceType;
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

    public void updateAttributes(String[] newAttributes)
    {
        // TODO
    }

    public void removeAttributes(String[] tags)
    {
        // TODO
    }
}
