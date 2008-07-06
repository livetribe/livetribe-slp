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
import java.util.Collection;
import java.util.List;

import org.livetribe.slp.ServiceLocationException;

/**
 * The LiveTribe SLP Identifier Extension is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      Extension ID = 0x8001    |     Next Extension Offset     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Offset, contd.|         Host Length           |  Host Address /
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       Identifier Length       |         Identifier            /
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  # of Auths   |    (if present)  Authentication Blocks...     /
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * @version $Rev$ $Date$
 */
public class IdentifierExtension extends Extension
{
    private static final int HOST_LENGTH_BYTES_LENGTH = 2;
    private static final int IDENTIFIER_LENGTH_BYTES_LENGTH = 2;
    private static final int AUTH_BLOCKS_COUNT_BYTES_LENGTH = 1;

    private String host;
    private String identifier;
    private AuthenticationBlock[] authenticationBlocks;

    IdentifierExtension()
    {
    }

    public IdentifierExtension(String host, String identifier)
    {
        this.host = host;
        this.identifier = identifier;
    }

    public AuthenticationBlock[] getAuthenticationBlocks()
    {
        return authenticationBlocks;
    }

    public void setAuthenticationBlocks(AuthenticationBlock[] authenticationBlocks)
    {
        this.authenticationBlocks = authenticationBlocks;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final IdentifierExtension that = (IdentifierExtension)obj;
        if (!getHost().equals(that.getHost())) return false;
        if (!getIdentifier().equals(that.getIdentifier())) return false;
        return true;
    }

    public int hashCode()
    {
        int result = getHost().hashCode();
        result = 29 * result + getIdentifier().hashCode();
        return result;
    }

    public int getId()
    {
        return IDENTIFIER_EXTENSION_ID;
    }

    public String getHost()
    {
        return host;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] hostBytes = writeString(getHost(), true);
        byte[] identifierBytes = writeString(getIdentifier(), true);
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

        int bodyLength = HOST_LENGTH_BYTES_LENGTH + hostBytes.length + IDENTIFIER_LENGTH_BYTES_LENGTH + identifierBytes.length;
        bodyLength += AUTH_BLOCKS_COUNT_BYTES_LENGTH + authBlockBytesSum;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(hostBytes.length, result, offset, HOST_LENGTH_BYTES_LENGTH);

        offset += HOST_LENGTH_BYTES_LENGTH;
        System.arraycopy(hostBytes, 0, result, offset, hostBytes.length);

        offset += hostBytes.length;
        writeInt(identifierBytes.length, result, offset, IDENTIFIER_LENGTH_BYTES_LENGTH);

        offset += IDENTIFIER_LENGTH_BYTES_LENGTH;
        System.arraycopy(identifierBytes, 0, result, offset, identifierBytes.length);

        offset += identifierBytes.length;
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

    protected void deserializeBody(byte[] bodyBytes) throws ServiceLocationException
    {
        int offset = 0;
        int hostLength = readInt(bodyBytes, offset, HOST_LENGTH_BYTES_LENGTH);

        offset += HOST_LENGTH_BYTES_LENGTH;
        this.host = readString(bodyBytes, offset, hostLength, true);

        offset += hostLength;
        int identifierLength = readInt(bodyBytes, offset, IDENTIFIER_LENGTH_BYTES_LENGTH);

        offset += IDENTIFIER_LENGTH_BYTES_LENGTH;
        this.identifier = readString(bodyBytes, offset, identifierLength, true);

        offset += identifierLength;
        int authBlocksCount = readInt(bodyBytes, offset, AUTH_BLOCKS_COUNT_BYTES_LENGTH);

        offset += AUTH_BLOCKS_COUNT_BYTES_LENGTH;
        if (authBlocksCount > 0)
        {
            AuthenticationBlock[] blocks = new AuthenticationBlock[authBlocksCount];
            for (int i = 0; i < authBlocksCount; ++i)
            {
                blocks[i] = new AuthenticationBlock();
                offset += blocks[i].deserialize(bodyBytes, offset);
            }
            setAuthenticationBlocks(blocks);
        }
    }

    /**
     * Returns the first IdentifierExtension found in the given collection of extensions,
     * or null if no IdentifierExtensions exist in the given collection.
     */
    public static IdentifierExtension findFirst(Collection<? extends Extension> extensions)
    {
        for (Extension extension : extensions)
        {
            if (IDENTIFIER_EXTENSION_ID == extension.getId()) return (IdentifierExtension)extension;
        }
        return null;
    }

    /**
     * Returns all IdentifierExtensions found in the given collection of extensions.
     */
    public static Collection<IdentifierExtension> findAll(Collection<? extends Extension> extensions)
    {
        List<IdentifierExtension> result = new ArrayList<IdentifierExtension>();
        for (Extension extension : extensions)
        {
            if (IDENTIFIER_EXTENSION_ID == extension.getId()) result.add((IdentifierExtension)extension);
        }
        return result;
    }
}
