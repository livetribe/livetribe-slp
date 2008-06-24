/*
 * Copyright 2007 the original author or authors
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

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.livetribe.slp.ServiceLocationException;

/**
 * The LiveTribe SLP Language Extension is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      Extension ID = 0x8002    |     Next Extension Offset     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Offset, contd.|      Service URL Length       |  Service URL  /
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       Language Length        |            Language            /
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  # of Auths   |    (if present)  Authentication Blocks...     /
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * @version $Revision$ $Date$
 */
public class LanguageExtension extends Extension
{
    private static final int URL_LENGTH_BYTES_LENGTH = 2;
    private static final int LANGUAGE_LENGTH_BYTES_LENGTH = 2;
    private static final int AUTH_BLOCKS_COUNT_BYTES_LENGTH = 1;

    private String url;
    private String language;
    private AuthenticationBlock[] authenticationBlocks;

    public int getId()
    {
        return LANGUAGE_EXTENSION_ID;
    }

    public String getURL()
    {
        return url;
    }

    public void setURL(String url)
    {
        this.url = url;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public AuthenticationBlock[] getAuthenticationBlocks()
    {
        return authenticationBlocks;
    }

    public void setAuthenticationBlocks(AuthenticationBlock[] authenticationBlocks)
    {
        this.authenticationBlocks = authenticationBlocks;
    }

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] urlBytes = writeString(getURL(), true);
        byte[] languageBytes = writeString(getLanguage(), true);
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

        int bodyLength = URL_LENGTH_BYTES_LENGTH + urlBytes.length + LANGUAGE_LENGTH_BYTES_LENGTH + languageBytes.length;
        bodyLength += AUTH_BLOCKS_COUNT_BYTES_LENGTH + authBlockBytesSum;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(urlBytes.length, result, offset, URL_LENGTH_BYTES_LENGTH);

        offset += URL_LENGTH_BYTES_LENGTH;
        System.arraycopy(urlBytes, 0, result, offset, urlBytes.length);

        offset += urlBytes.length;
        writeInt(languageBytes.length, result, offset, LANGUAGE_LENGTH_BYTES_LENGTH);

        offset += LANGUAGE_LENGTH_BYTES_LENGTH;
        System.arraycopy(languageBytes, 0, result, offset, languageBytes.length);

        offset += languageBytes.length;
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
        int urlLength = readInt(bodyBytes, offset, URL_LENGTH_BYTES_LENGTH);

        offset += URL_LENGTH_BYTES_LENGTH;
        setURL(readString(bodyBytes, offset, urlLength, true));

        offset += urlLength;
        int languageLength = readInt(bodyBytes, offset, LANGUAGE_LENGTH_BYTES_LENGTH);

        offset += LANGUAGE_LENGTH_BYTES_LENGTH;
        setLanguage(readString(bodyBytes, offset, languageLength, true));

        offset += languageLength;
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
     * Returns the first LanguageExtension found in the given collection of extensions,
     * or null if the extension collection does not contain a LanguageExtension.
     */
    public static LanguageExtension findFirst(Collection<? extends Extension> extensions)
    {
        for (Extension extension : extensions)
        {
            if (LANGUAGE_EXTENSION_ID == extension.getId())
                return (LanguageExtension)extension;
        }
        return null;
    }

    /**
     * Returns all LanguageExtension found in the given collection of extensions,
     * or an empty list if the extension collection does not contain LanguageExtensions.
     */
    public static List<LanguageExtension> findAll(Collection<? extends Extension> extensions)
    {
        List<LanguageExtension> result = new ArrayList<LanguageExtension>();
        for (Extension extension : extensions)
        {
            if (LANGUAGE_EXTENSION_ID == extension.getId())
                result.add((LanguageExtension)extension);
        }
        return result;
    }
}
