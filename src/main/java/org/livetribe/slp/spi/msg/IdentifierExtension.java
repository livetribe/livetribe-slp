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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Rev$ $Date$
 */
public class IdentifierExtension extends Extension
{
    private static final int IDENTIFIER_LENGTH_BYTES_LENGTH = 2;
    private static final int HOST_LENGTH_BYTES_LENGTH = 2;

    private String identifier;
    private String host;

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final IdentifierExtension that = (IdentifierExtension)obj;
        if (!getIdentifier().equals(that.getIdentifier())) return false;
        if (!getHost().equals(that.getHost())) return false;
        return true;
    }

    public int hashCode()
    {
        int result = getIdentifier().hashCode();
        result = 29 * result + getHost().hashCode();
        return result;
    }

    public int getId()
    {
        return IDENTIFIER_EXTENSION_ID;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] identifierBytes = writeString(getIdentifier());
        byte[] hostBytes = writeString(getHost());

        byte[] result = new byte[IDENTIFIER_LENGTH_BYTES_LENGTH + identifierBytes.length + HOST_LENGTH_BYTES_LENGTH + hostBytes.length];

        int offset = 0;
        writeInt(identifierBytes.length, result, offset, IDENTIFIER_LENGTH_BYTES_LENGTH);

        offset += IDENTIFIER_LENGTH_BYTES_LENGTH;
        System.arraycopy(identifierBytes, 0, result, offset, identifierBytes.length);

        offset += identifierBytes.length;
        writeInt(hostBytes.length, result, offset, HOST_LENGTH_BYTES_LENGTH);

        offset += HOST_LENGTH_BYTES_LENGTH;
        System.arraycopy(hostBytes, 0, result, offset, hostBytes.length);

        return result;
    }

    protected void deserializeBody(byte[] bodyBytes) throws ServiceLocationException
    {
        int offset = 0;
        int identifierLength = readInt(bodyBytes, offset, IDENTIFIER_LENGTH_BYTES_LENGTH);

        offset += IDENTIFIER_LENGTH_BYTES_LENGTH;
        setIdentifier(readString(bodyBytes, offset, identifierLength));

        offset += identifierLength;
        int hostLength = readInt(bodyBytes, offset, HOST_LENGTH_BYTES_LENGTH);

        offset += HOST_LENGTH_BYTES_LENGTH;
        setHost(readString(bodyBytes, offset, hostLength));
    }

    /**
     * Returns the first IdentifierExtension found in the given collection of extensions.
     */
    public static IdentifierExtension findFirst(Collection extensions)
    {
        for (Iterator allExtensions = extensions.iterator(); allExtensions.hasNext();)
        {
            Extension extension = (Extension)allExtensions.next();
            if (IDENTIFIER_EXTENSION_ID == extension.getId())
                return (IdentifierExtension)extension;
        }
        return null;
    }

    /**
     * Returns all IdentifierExtensions found in the given collection of extensions.
     */
    public static Collection findAll(Collection extensions)
    {
        List result = new ArrayList();
        for (Iterator allExtensions = extensions.iterator(); allExtensions.hasNext();)
        {
            Extension extension = (Extension)allExtensions.next();
            if (IDENTIFIER_EXTENSION_ID == extension.getId()) result.add(extension);
        }
        return result;
    }
}
