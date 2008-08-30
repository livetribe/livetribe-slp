/*
 * Copyright 2008-2008 the original author or authors
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
import java.util.HashSet;
import java.util.Set;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;

/**
 * The RFC 2608 SrvTypeRqst message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      Service Location header (function = SrvTypeRqst = 9)     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      length of [PRList]       |        [PRList] String        \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | length of [Naming Authority]  |  [Naming Authority] String    \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    length of [scope-list]     |     [scope-list] String       \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * @version $Revision$ $Date$
 */
public class SrvTypeRqst extends Rqst
{
    private static final int RESPONDERS_LENGTH_BYTES_LENGTH = 2;
    private static final int NAMING_AUTHORITY_LENGTH_BYTES_LENGTH = 2;
    private static final int SCOPES_LENGTH_BYTES_LENGTH = 2;
    private static final int ANY_NAMING_AUTHORITY_LENGTH = 0xFFFF;

    private boolean anyNamingAuthority;
    private String namingAuthority;
    private Scopes scopes;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        Set<String> responders = getPreviousResponders();
        byte[] previousRespondersBytes = responders == null ? EMPTY_BYTES : writeStringArray(responders.toArray(new String[responders.size()]), true);
        int previousRespondersLength = previousRespondersBytes.length;
        byte[] namingAuthorityBytes;
        int namingAuthorityLength;
        int bodyLength;

        if (isAnyNamingAuthority())
        {
            namingAuthorityBytes = EMPTY_BYTES;
            namingAuthorityLength = ANY_NAMING_AUTHORITY_LENGTH;
            bodyLength = RESPONDERS_LENGTH_BYTES_LENGTH + previousRespondersLength + NAMING_AUTHORITY_LENGTH_BYTES_LENGTH;
        }
        else
        {
            namingAuthorityBytes = writeString(getNamingAuthority(), true);
            namingAuthorityLength = namingAuthorityBytes.length;
            bodyLength = RESPONDERS_LENGTH_BYTES_LENGTH + previousRespondersLength + NAMING_AUTHORITY_LENGTH_BYTES_LENGTH + namingAuthorityLength;
        }

        byte[] scopesBytes = scopesToBytes(getScopes());
        int scopesLength = scopesBytes.length;

        bodyLength += SCOPES_LENGTH_BYTES_LENGTH + scopesLength;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(previousRespondersLength, result, offset, RESPONDERS_LENGTH_BYTES_LENGTH);

        offset += RESPONDERS_LENGTH_BYTES_LENGTH;
        System.arraycopy(previousRespondersBytes, 0, result, offset, previousRespondersLength);

        offset += previousRespondersLength;
        writeInt(namingAuthorityLength, result, offset, NAMING_AUTHORITY_LENGTH_BYTES_LENGTH);

        offset += NAMING_AUTHORITY_LENGTH_BYTES_LENGTH;

        if (!isAnyNamingAuthority())
        {
            System.arraycopy(namingAuthorityBytes, 0, result, offset, namingAuthorityLength);
            offset += namingAuthorityLength;
        }

        writeInt(scopesLength, result, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        System.arraycopy(scopesBytes, 0, result, offset, scopesLength);

        return result;
    }

    protected void deserializeBody(byte[] bytes) throws ServiceLocationException
    {
        int offset = 0;
        int previousRespondersLength = readInt(bytes, offset, RESPONDERS_LENGTH_BYTES_LENGTH);

        offset += RESPONDERS_LENGTH_BYTES_LENGTH;
        setPreviousResponders(new HashSet<String>(Arrays.asList(readStringArray(bytes, offset, previousRespondersLength, true))));

        offset += previousRespondersLength;
        int namingAuthorityLength = readInt(bytes, offset, NAMING_AUTHORITY_LENGTH_BYTES_LENGTH);

        offset += NAMING_AUTHORITY_LENGTH_BYTES_LENGTH;

        if (namingAuthorityLength == ANY_NAMING_AUTHORITY_LENGTH)
        {
            setAnyNamingAuthority(true);
        }
        else
        {
            setNamingAuthority(readString(bytes, offset, namingAuthorityLength, true));
            offset += namingAuthorityLength;
        }

        int scopesLength = readInt(bytes, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        setScopes(Scopes.from(readStringArray(bytes, offset, scopesLength, false)));
    }

    public byte getMessageType()
    {
        return SRV_TYPE_RQST_TYPE;
    }

    public boolean isAnyNamingAuthority()
    {
        return anyNamingAuthority;
    }

    public void setAnyNamingAuthority(boolean anyNamingAuthority)
    {
        this.anyNamingAuthority = anyNamingAuthority;
    }

    public String getNamingAuthority()
    {
        return namingAuthority;
    }

    public void setNamingAuthority(String namingAuthority)
    {
        this.namingAuthority = namingAuthority;
    }

    public Scopes getScopes()
    {
        return scopes;
    }

    public void setScopes(Scopes scopes)
    {
        this.scopes = scopes;
    }

    public boolean isDefaultNamingAuthority()
    {
        return !isAnyNamingAuthority() && getNamingAuthority().length() == 0;
    }
}
