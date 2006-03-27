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

import java.util.Arrays;

import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;

/**
 * The RFC 2608 &lt;URL Entry&gt; is defined as:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Reserved    |          Lifetime             |   URL Length  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |URL len, contd.|            URL (variable length)              \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |# of URL auths |            Auth. blocks (if any)              \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * @version $Rev$ $Date$
 */
public class URLEntry extends BytesBlock
{
    private static final int MAX_LIFETIME = 65535;

    private int lifetime;
    private String url;
    private AuthenticationBlock[] authenticationBlocks;

    public int hashCode()
    {
        int result = lifetime;
        result = 29 * result + url.hashCode();
        if (authenticationBlocks != null)
        {
            result = 29 * result + authenticationBlocks.length;
            for (int i = 0; i < authenticationBlocks.length; ++i)
            {
                result = 29 * result + authenticationBlocks[i].hashCode();
            }
        }
        return result;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final URLEntry other = (URLEntry)obj;
        if (lifetime != other.lifetime) return false;
        if (!url.equals(other.url)) return false;
        return Arrays.equals(authenticationBlocks, other.authenticationBlocks);
    }

    public byte[] serialize() throws ServiceLocationException
    {
        int reservedByteslength = 1;
        int lifeTimeBytesLength = 2;
        int urlLengthBytesLength = 3;
        byte[] urlBytes = stringToBytes(getURL());
        int urlBytesLength = urlBytes.length;
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

        int bodyLength = reservedByteslength + lifeTimeBytesLength + urlLengthBytesLength + urlBytesLength + authBlocksCountBytesLength + authBlockBytesSum;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(0, result, offset, reservedByteslength);

        offset += reservedByteslength;
        writeInt(getLifetime(), result, offset, lifeTimeBytesLength);

        offset += lifeTimeBytesLength;
        writeInt(urlBytesLength, result, offset, urlLengthBytesLength);

        offset += urlLengthBytesLength;
        System.arraycopy(urlBytes, 0, result, offset, urlBytesLength);

        offset += urlBytesLength;
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

    public int deserialize(byte[] bytes, int originalOffset) throws ServiceLocationException
    {
        int offset = originalOffset;
        int reservedByteslength = 1;
        readInt(bytes, offset, reservedByteslength);

        offset += reservedByteslength;
        int lifeTimeBytesLength = 2;
        setLifetime(readInt(bytes, offset, lifeTimeBytesLength));

        offset += lifeTimeBytesLength;
        int urlLengthBytesLength = 3;
        int urlLength = readInt(bytes, offset, urlLengthBytesLength);

        offset += urlLengthBytesLength;
        setURL(readString(bytes, offset, urlLength));

        offset += urlLength;
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

        return offset - originalOffset;
    }

    public int getLifetime()
    {
        return lifetime;
    }

    public void setLifetime(int lifetime)
    {
        if (lifetime > MAX_LIFETIME) throw new IllegalArgumentException("LifeTime value in URLEntry must be between 0 and " + MAX_LIFETIME + " included");
        this.lifetime = lifetime;
    }

    public String getURL()
    {
        return url;
    }

    public void setURL(String url)
    {
        this.url = url;
    }

    public AuthenticationBlock[] getAuthenticationBlocks()
    {
        return authenticationBlocks;
    }

    public void setAuthenticationBlocks(AuthenticationBlock[] authenticationBlocks)
    {
        this.authenticationBlocks = authenticationBlocks;
    }

    public ServiceURL toServiceURL()
    {
        return new ServiceURL(getURL(), getLifetime());
    }
}
