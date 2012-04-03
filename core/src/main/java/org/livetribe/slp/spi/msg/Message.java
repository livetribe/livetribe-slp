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
package org.livetribe.slp.spi.msg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPError;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;


/**
 * The RFC 2608 message header is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Version    |  Function-ID  |            Length             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Length, contd.|O|F|R|       reserved          |Next Ext Offset|
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Next Extension Offset, contd.|              XID              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      Language Tag Length      |         Language Tag          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 */
public abstract class Message extends BytesBlock
{
    public static final byte SRV_RQST_TYPE = 1;
    public static final byte SRV_RPLY_TYPE = 2;
    public static final byte SRV_REG_TYPE = 3;
    public static final byte SRV_DEREG_TYPE = 4;
    public static final byte SRV_ACK_TYPE = 5;
    public static final byte ATTR_RQST_TYPE = 6;
    public static final byte ATTR_RPLY_TYPE = 7;
    public static final byte DA_ADVERT_TYPE = 8;
    public static final byte SRV_TYPE_RQST_TYPE = 9;
    public static final byte SRV_TYPE_RPLY_TYPE = 10;
    public static final byte SA_ADVERT_TYPE = 11;

    private static final int VERSION_BYTES_LENGTH = 1;
    private static final int MESSAGE_TYPE_BYTES_LENGTH = 1;
    private static final int MESSAGE_LENGTH_BYTES_LENGTH = 3;
    private static final int FLAGS_BYTES_LENGTH = 2;
    private static final int EXTENSION_BYTES_LENGTH = 3;
    private static final int XID_BYTES_LENGTH = 2;
    private static final int LANGUAGE_LENGTH_BYTES_LENGTH = 2;
    private static final byte SLP_VERSION = 2;

    private static final Random random = new Random();

    private boolean overflow;
    private boolean fresh;
    private boolean multicast;
    private int xid;
    private String language;
    private Collection<Extension> extensions = new ArrayList<Extension>();

    protected abstract byte[] serializeBody() throws ServiceLocationException;

    protected abstract void deserializeBody(byte[] bytes) throws ServiceLocationException;

    protected byte[] serializeExtensions(int firstExtensionOffset) throws ServiceLocationException
    {
        if (extensions == null) return EMPTY_BYTES;

        int extensionCount = extensions.size();
        if (extensionCount == 0) return EMPTY_BYTES;

        byte[][] extensionsBytes = new byte[extensionCount][];
        int extensionsLength = 0;
        int i = 0;
        for (Iterator allExtensions = extensions.iterator(); allExtensions.hasNext(); ++i)
        {
            Extension extension = (Extension)allExtensions.next();
            byte[] extensionBytes = extension.serialize();

            // Correct the offset of the next extension
            int nextExtensionOffset = allExtensions.hasNext() ? firstExtensionOffset + extensionsLength + extensionBytes.length : 0;
            writeInt(nextExtensionOffset, extensionBytes, Extension.ID_BYTES_LENGTH, Extension.NEXT_EXTENSION_OFFSET_BYTES_LENGTH);

            extensionsBytes[i] = extensionBytes;
            extensionsLength += extensionBytes.length;
        }

        int offset = 0;
        byte[] result = new byte[extensionsLength];
        for (int j = 0; j < extensionCount; ++j)
        {
            int length = extensionsBytes[j].length;
            System.arraycopy(extensionsBytes[j], 0, result, offset, length);
            offset += length;
        }

        return result;
    }

    protected void deserializeExtensions(byte[] bytes, int firstExtensionOffset) throws ServiceLocationException
    {
        extensions.clear();

        int offset = 0;
        while (offset < bytes.length)
        {
            int initialOffset = offset;

            offset += Extension.ID_BYTES_LENGTH;
            int nextOffset = readInt(bytes, offset, Extension.NEXT_EXTENSION_OFFSET_BYTES_LENGTH);

            offset += Extension.NEXT_EXTENSION_OFFSET_BYTES_LENGTH;
            int extensionLength = nextOffset == 0 ? bytes.length - initialOffset : nextOffset - firstExtensionOffset - initialOffset;
            byte[] extensionBytes = new byte[extensionLength];
            System.arraycopy(bytes, initialOffset, extensionBytes, 0, extensionBytes.length);
            Extension extension = Extension.deserialize(extensionBytes);

            if (extension != null) extensions.add(extension);

            offset = initialOffset + extensionLength;
        }
    }

    public void addExtension(Extension extension)
    {
        extensions.add(extension);
    }

    public Collection<Extension> getExtensions()
    {
        return Collections.unmodifiableCollection(extensions);
    }

    public abstract byte getMessageType();

    public boolean isMulticast()
    {
        return multicast;
    }

    public void setMulticast(boolean multicast)
    {
        this.multicast = multicast;
    }

    public boolean isOverflow()
    {
        return overflow;
    }

    public void setOverflow(boolean overflow)
    {
        this.overflow = overflow;
    }

    public boolean isFresh()
    {
        return fresh;
    }

    public void setFresh(boolean fresh)
    {
        this.fresh = fresh;
    }

    public int getXID()
    {
        return xid;
    }

    public void setXID(int xid)
    {
        this.xid = xid;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public byte[] serialize() throws ServiceLocationException
    {
        byte[] bodyBytes = serializeBody();

        byte[] languageBytes = writeString(getLanguage(), true);
        int headerLength = VERSION_BYTES_LENGTH + MESSAGE_TYPE_BYTES_LENGTH + MESSAGE_LENGTH_BYTES_LENGTH + FLAGS_BYTES_LENGTH;
        headerLength += EXTENSION_BYTES_LENGTH + XID_BYTES_LENGTH + LANGUAGE_LENGTH_BYTES_LENGTH + languageBytes.length;

        int firstExtensionOffset = headerLength + bodyBytes.length;
        byte[] extensionsBytes = serializeExtensions(firstExtensionOffset);

        byte[] result = new byte[firstExtensionOffset + extensionsBytes.length];

        int offset = 0;
        result[offset] = SLP_VERSION;

        offset += VERSION_BYTES_LENGTH;
        result[offset] = getMessageType();

        offset += MESSAGE_TYPE_BYTES_LENGTH;
        writeInt(result.length, result, offset, MESSAGE_LENGTH_BYTES_LENGTH);

        offset += MESSAGE_LENGTH_BYTES_LENGTH;
        int flags = 0;
        if (isOverflow()) flags |= 0x8000;
        if (isFresh()) flags |= 0x4000;
        if (isMulticast()) flags |= 0x2000;
        writeInt(flags, result, offset, FLAGS_BYTES_LENGTH);

        offset += FLAGS_BYTES_LENGTH;
        if (extensionsBytes.length > 0)
        {
            writeInt(firstExtensionOffset, result, offset, EXTENSION_BYTES_LENGTH);
        }
        else
        {
            writeInt(0, result, offset, EXTENSION_BYTES_LENGTH);
        }

        offset += EXTENSION_BYTES_LENGTH;
        writeInt(getXID(), result, offset, XID_BYTES_LENGTH);

        offset += XID_BYTES_LENGTH;
        writeInt(languageBytes.length, result, offset, LANGUAGE_LENGTH_BYTES_LENGTH);

        offset += LANGUAGE_LENGTH_BYTES_LENGTH;
        System.arraycopy(languageBytes, 0, result, offset, languageBytes.length);

        offset += languageBytes.length;
        System.arraycopy(bodyBytes, 0, result, offset, bodyBytes.length);

        offset += bodyBytes.length;
        System.arraycopy(extensionsBytes, 0, result, offset, extensionsBytes.length);

        return result;
    }

    /**
     * Parses the header of SLP messages, then each message parses its body via {@link #deserializeBody(byte[])}.
     *
     * @throws ServiceLocationException If the bytes cannot be parsed
     */
    public static Message deserialize(byte[] bytes) throws ServiceLocationException
    {
        try
        {
            int offset = 0;
            byte version = bytes[offset];
            if (version != SLP_VERSION)
                throw new ServiceLocationException("Unsupported SLP version " + version + ", only version " + SLP_VERSION + " is supported", SLPError.VERSION_NOT_SUPPORTED);

            offset += VERSION_BYTES_LENGTH;
            byte messageType = bytes[offset];

            offset += MESSAGE_TYPE_BYTES_LENGTH;
            int length = readInt(bytes, offset, MESSAGE_LENGTH_BYTES_LENGTH);
            if (bytes.length != length)
                throw new ServiceLocationException("Expected message length is " + length + ", got instead " + bytes.length, SLPError.PARSE_ERROR);

            offset += MESSAGE_LENGTH_BYTES_LENGTH;
            int flags = readInt(bytes, offset, FLAGS_BYTES_LENGTH);

            offset += FLAGS_BYTES_LENGTH;
            int extensionOffset = readInt(bytes, offset, EXTENSION_BYTES_LENGTH);

            offset += EXTENSION_BYTES_LENGTH;
            int xid = readInt(bytes, offset, XID_BYTES_LENGTH);

            offset += XID_BYTES_LENGTH;
            int languageLength = readInt(bytes, offset, LANGUAGE_LENGTH_BYTES_LENGTH);

            offset += LANGUAGE_LENGTH_BYTES_LENGTH;
            String language = readString(bytes, offset, languageLength, true);

            Message message = createMessage(messageType);
            message.setOverflow((flags & 0x8000) == 0x8000);
            message.setFresh((flags & 0x4000) == 0x4000);
            message.setMulticast((flags & 0x2000) == 0x2000);
            message.setXID(xid);
            message.setLanguage(language);

            offset += languageLength;
            int bodyLength = extensionOffset == 0 ? length - offset : extensionOffset - offset;
            byte[] bodyBytes = new byte[bodyLength];
            System.arraycopy(bytes, offset, bodyBytes, 0, bodyBytes.length);
            message.deserializeBody(bodyBytes);

            if (extensionOffset > 0)
            {
                byte[] extensionsBytes = new byte[length - extensionOffset];
                System.arraycopy(bytes, extensionOffset, extensionsBytes, 0, extensionsBytes.length);
                message.deserializeExtensions(extensionsBytes, extensionOffset);
            }

            return message;
        }
        catch (IndexOutOfBoundsException x)
        {
            throw new ServiceLocationException(x, SLPError.PARSE_ERROR);
        }
    }

    public static int newXID()
    {
        // XIDs are 2 byte integers
        return random.nextInt(1 << 16);
    }

    private static Message createMessage(byte messageType) throws ServiceLocationException
    {
        switch (messageType)
        {
            case SRV_RQST_TYPE:
                return new SrvRqst();
            case SRV_RPLY_TYPE:
                return new SrvRply();
            case SRV_REG_TYPE:
                return new SrvReg();
            case SRV_DEREG_TYPE:
                return new SrvDeReg();
            case SRV_ACK_TYPE:
                return new SrvAck();
            case ATTR_RQST_TYPE:
                return new AttrRqst();
            case ATTR_RPLY_TYPE:
                return new AttrRply();
            case DA_ADVERT_TYPE:
                return new DAAdvert();
            case SRV_TYPE_RQST_TYPE:
                return new SrvTypeRqst();
            case SRV_TYPE_RPLY_TYPE:
                return new SrvTypeRply();
            case SA_ADVERT_TYPE:
                return new SAAdvert();
        }
        throw new ServiceLocationException("Message not supported " + messageType, SLPError.MESSAGE_NOT_SUPPORTED);
    }

    protected static byte[] serviceTypeToBytes(ServiceType serviceType)
    {
        if (serviceType == null) return EMPTY_BYTES;
        return writeString(serviceType.asString(), true);
    }

    protected static byte[] scopesToBytes(Scopes scopes)
    {
        if (scopes == null) return EMPTY_BYTES;
        return writeStringArray(scopes.asStringArray(), false);
    }

    protected static byte[] attributesToBytes(Attributes attributes)
    {
        if (attributes == null) return EMPTY_BYTES;
        return writeString(attributes.asString(), false);
    }

    protected static byte[] tagsToBytes(Attributes attributes)
    {
        if (attributes == null) return EMPTY_BYTES;
        return writeString(attributes.asTagsString(), false);
    }
}
