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

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;


/**
 * The RFC 2608 AttrRqst message body is the following:
 * <pre>
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       Service Location header (function = AttrRqst = 6)       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      length of [PRList]       |        [PRList] String        \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         length of URL         |              URL              \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    length of [scope-list]     |     [scope-list] String       \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  length of [tag-list] string  |       [tag-list] string       \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  length of [SLP SPI] string   |       [SLP SPI] String        \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 */
public class AttrRqst extends Rqst
{
    private static final int RESPONDERS_LENGTH_BYTES_LENGTH = 2;
    private static final int URL_LENGTH_BYTES_LENGTH = 2;
    private static final int SCOPES_LENGTH_BYTES_LENGTH = 2;
    private static final int TAGS_LENGTH_BYTES_LENGTH = 2;
    private static final int SPI_LENGTH_BYTES_LENGTH = 2;

    private String url;
    private Scopes scopes;
    private Attributes tags;
    private String securityParameterIndex;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        Set<String> responders = getPreviousResponders();
        byte[] previousRespondersBytes = responders == null ? EMPTY_BYTES : writeStringArray(responders.toArray(new String[responders.size()]), true);
        int previousRespondersLength = previousRespondersBytes.length;
        byte[] urlBytes = writeString(getURL(), true);
        int urlLength = urlBytes.length;
        byte[] scopesBytes = scopesToBytes(getScopes());
        int scopesLength = scopesBytes.length;
        byte[] tagsBytes = tagsToBytes(getTags());
        int tagsLength = tagsBytes.length;
        byte[] securityParameterIndexBytes = writeString(getSecurityParameterIndex(), true);
        int securityParameterIndexLength = securityParameterIndexBytes.length;

        int bodyLength = RESPONDERS_LENGTH_BYTES_LENGTH + previousRespondersLength + URL_LENGTH_BYTES_LENGTH + urlLength;
        bodyLength += SCOPES_LENGTH_BYTES_LENGTH + scopesLength + TAGS_LENGTH_BYTES_LENGTH + tagsLength + SPI_LENGTH_BYTES_LENGTH + securityParameterIndexLength;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(previousRespondersLength, result, offset, RESPONDERS_LENGTH_BYTES_LENGTH);

        offset += RESPONDERS_LENGTH_BYTES_LENGTH;
        System.arraycopy(previousRespondersBytes, 0, result, offset, previousRespondersLength);

        offset += previousRespondersLength;
        writeInt(urlLength, result, offset, URL_LENGTH_BYTES_LENGTH);

        offset += URL_LENGTH_BYTES_LENGTH;
        System.arraycopy(urlBytes, 0, result, offset, urlLength);

        offset += urlLength;
        writeInt(scopesLength, result, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        System.arraycopy(scopesBytes, 0, result, offset, scopesLength);

        offset += scopesLength;
        writeInt(tagsLength, result, offset, TAGS_LENGTH_BYTES_LENGTH);

        offset += TAGS_LENGTH_BYTES_LENGTH;
        System.arraycopy(tagsBytes, 0, result, offset, tagsLength);

        offset += tagsLength;
        writeInt(securityParameterIndexLength, result, offset, SPI_LENGTH_BYTES_LENGTH);

        offset += SPI_LENGTH_BYTES_LENGTH;
        System.arraycopy(securityParameterIndexBytes, 0, result, offset, securityParameterIndexLength);

        return result;
    }

    protected void deserializeBody(byte[] bytes) throws ServiceLocationException
    {
        int offset = 0;
        int previousRespondersLength = readInt(bytes, offset, RESPONDERS_LENGTH_BYTES_LENGTH);

        offset += RESPONDERS_LENGTH_BYTES_LENGTH;
        setPreviousResponders(new HashSet<String>(Arrays.asList(readStringArray(bytes, offset, previousRespondersLength, true))));

        offset += previousRespondersLength;
        int serviceURLLength = readInt(bytes, offset, URL_LENGTH_BYTES_LENGTH);

        offset += URL_LENGTH_BYTES_LENGTH;
        setURL(readString(bytes, offset, serviceURLLength, true));

        offset += serviceURLLength;
        int scopesLength = readInt(bytes, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        setScopes(Scopes.from(readStringArray(bytes, offset, scopesLength, false)));

        offset += scopesLength;
        int tagsLength = readInt(bytes, offset, TAGS_LENGTH_BYTES_LENGTH);

        offset += TAGS_LENGTH_BYTES_LENGTH;
        setTags(Attributes.fromTags(readString(bytes, offset, tagsLength, false)));

        offset += tagsLength;
        int securityParameterIndexLength = readInt(bytes, offset, SPI_LENGTH_BYTES_LENGTH);

        offset += SPI_LENGTH_BYTES_LENGTH;
        setSecurityParameterIndex(readString(bytes, offset, securityParameterIndexLength, true));
    }

    public byte getMessageType()
    {
        return ATTR_RQST_TYPE;
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

    public Attributes getTags()
    {
        return tags;
    }

    public void setTags(Attributes tags)
    {
        this.tags = tags;
    }

    public String getSecurityParameterIndex()
    {
        return securityParameterIndex;
    }

    public void setSecurityParameterIndex(String securityParameterIndex)
    {
        this.securityParameterIndex = securityParameterIndex;
    }

    public boolean isForServiceType()
    {
        // A ServiceType string cannot be parsed as ServiceURL
        // If the parsing fails, it is a ServiceType, otherwise it is a ServiceURL
        try
        {
            new ServiceURL(getURL());
            return false;
        }
        catch (Exception x)
        {
            return true;
        }
    }
}
