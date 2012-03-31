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

import org.livetribe.slp.ServiceLocationException;


/**
 * The RFC 2608 extension is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Extension ID          |       Next Extension Offset   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Offset, contd.|                Extension Data                 \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 */
public abstract class Extension extends BytesBlock
{
    public static final int ATTRIBUTE_LIST_EXTENSION_ID = 0x0002;
    public static final int SCOPE_LIST_EXTENSION_ID = 0x8000;
    public static final int IDENTIFIER_EXTENSION_ID = 0x8001;
    public static final int LANGUAGE_EXTENSION_ID = 0x8002;

    public static final int ID_BYTES_LENGTH = 2;
    public static final int NEXT_EXTENSION_OFFSET_BYTES_LENGTH = 3;

    /**
     * Returns the extension id that identifies this extension.
     */
    public abstract int getId();

    public byte[] serialize() throws ServiceLocationException
    {
        byte[] bodyBytes = serializeBody();
        byte[] result = new byte[ID_BYTES_LENGTH + NEXT_EXTENSION_OFFSET_BYTES_LENGTH + bodyBytes.length];

        int offset = 0;
        writeInt(getId(), result, offset, ID_BYTES_LENGTH);

        // Next extension offset will be corrected by the caller of this method
        offset += ID_BYTES_LENGTH;
        writeInt(0, result, offset, NEXT_EXTENSION_OFFSET_BYTES_LENGTH);

        offset += NEXT_EXTENSION_OFFSET_BYTES_LENGTH;
        System.arraycopy(bodyBytes, 0, result, offset, bodyBytes.length);

        return result;
    }

    protected abstract byte[] serializeBody() throws ServiceLocationException;

    protected abstract void deserializeBody(byte[] bodyBytes) throws ServiceLocationException;

    /**
     * Returns an Extension subclass object obtained deserializing the given bytes, or null
     * if the bytes contain an extension that is not understood.
     *
     * @param extensionBytes The bytes to deserialize
     * @throws ServiceLocationException If the deserialization fails
     */
    public static Extension deserialize(byte[] extensionBytes) throws ServiceLocationException
    {
        int extensionId = readInt(extensionBytes, 0, ID_BYTES_LENGTH);
        Extension extension = createExtension(extensionId);
        if (extension != null)
        {
            byte[] bodyBytes = new byte[extensionBytes.length - ID_BYTES_LENGTH - NEXT_EXTENSION_OFFSET_BYTES_LENGTH];
            System.arraycopy(extensionBytes, ID_BYTES_LENGTH + NEXT_EXTENSION_OFFSET_BYTES_LENGTH, bodyBytes, 0, bodyBytes.length);
            extension.deserializeBody(bodyBytes);
        }
        return extension;
    }

    private static Extension createExtension(int extensionId)
    {
        switch (extensionId)
        {
            case ATTRIBUTE_LIST_EXTENSION_ID:
                return new AttributeListExtension();
            case SCOPE_LIST_EXTENSION_ID:
                return new ScopeListExtension();
            case IDENTIFIER_EXTENSION_ID:
                return new IdentifierExtension();
            case LANGUAGE_EXTENSION_ID:
                return new LanguageExtension();
            default:
                return null;
        }
    }
}
