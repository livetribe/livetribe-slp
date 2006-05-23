/*
 * Copyright 2005 the original author or authors
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;


/**
 * The RFC 2608 SrvRqst message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       Service Location header (function = SrvRqst = 1)        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      length of [PRList]       |        [PRList] String        \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   length of [service-type]    |    [service-type] String      \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    length of [scope-list]     |     [scope-list] String       \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  length of predicate string   |  Service Request [predicate]  \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  length of [SLP SPI] string   |       [SLP SPI] String        \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * @version $Rev$ $Date$
 */
public class SrvRqst extends Rqst
{
    private static final int RESPONDERS_LENGTH_BYTES_LENGTH = 2;
    private static final int SERVICE_TYPE_LENGTH_BYTES_LENGTH = 2;
    private static final int SCOPES_LENGTH_BYTES_LENGTH = 2;
    private static final int FILTER_LENGTH_BYTES_LENGTH = 2;
    private static final int SPI_LENGTH_BYTES_LENGTH = 2;

    private ServiceType serviceType;
    private String[] scopes;
    private String filter;
    private String securityParameterIndex;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        Set responders = getPreviousResponders();
        byte[] previousRespondersBytes = responders == null ? EMPTY_BYTES : writeStringArray((String[])responders.toArray(new String[responders.size()]));
        int previousRespondersLength = previousRespondersBytes.length;
        byte[] serviceTypeBytes = writeString(getServiceType().toString());
        int serviceTypeLength = serviceTypeBytes.length;
        byte[] scopesBytes = writeStringArray(getScopes());
        int scopesLength = scopesBytes.length;
        byte[] filterBytes = writeString(getFilter());
        int filterLength = filterBytes.length;
        byte[] securityParameterIndexBytes = writeString(getSecurityParameterIndex());
        int securityParameterIndexLength = securityParameterIndexBytes.length;

        int bodyLength = RESPONDERS_LENGTH_BYTES_LENGTH + previousRespondersLength + SERVICE_TYPE_LENGTH_BYTES_LENGTH + serviceTypeLength;
        bodyLength += SCOPES_LENGTH_BYTES_LENGTH + scopesLength + FILTER_LENGTH_BYTES_LENGTH + filterLength + SPI_LENGTH_BYTES_LENGTH + securityParameterIndexLength;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(previousRespondersLength, result, offset, RESPONDERS_LENGTH_BYTES_LENGTH);

        offset += RESPONDERS_LENGTH_BYTES_LENGTH;
        System.arraycopy(previousRespondersBytes, 0, result, offset, previousRespondersLength);

        offset += previousRespondersLength;
        writeInt(serviceTypeLength, result, offset, SERVICE_TYPE_LENGTH_BYTES_LENGTH);

        offset += SERVICE_TYPE_LENGTH_BYTES_LENGTH;
        System.arraycopy(serviceTypeBytes, 0, result, offset, serviceTypeLength);

        offset += serviceTypeLength;
        writeInt(scopesLength, result, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        System.arraycopy(scopesBytes, 0, result, offset, scopesLength);

        offset += scopesLength;
        writeInt(filterLength, result, offset, FILTER_LENGTH_BYTES_LENGTH);

        offset += FILTER_LENGTH_BYTES_LENGTH;
        System.arraycopy(filterBytes, 0, result, offset, filterLength);

        offset += filterLength;
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
        setPreviousResponders(new HashSet(Arrays.asList(readStringArray(bytes, offset, previousRespondersLength))));

        offset += previousRespondersLength;
        int serviceTypeLength = readInt(bytes, offset, SERVICE_TYPE_LENGTH_BYTES_LENGTH);

        offset += SERVICE_TYPE_LENGTH_BYTES_LENGTH;
        setServiceType(new ServiceType(readString(bytes, offset, serviceTypeLength)));

        offset += serviceTypeLength;
        int scopesLength = readInt(bytes, offset, SCOPES_LENGTH_BYTES_LENGTH);

        offset += SCOPES_LENGTH_BYTES_LENGTH;
        setScopes(readStringArray(bytes, offset, scopesLength));

        offset += scopesLength;
        int filterLength = readInt(bytes, offset, FILTER_LENGTH_BYTES_LENGTH);

        offset += FILTER_LENGTH_BYTES_LENGTH;
        setFilter(readString(bytes, offset, filterLength));

        offset += filterLength;
        int securityParameterIndexLength = readInt(bytes, offset, SPI_LENGTH_BYTES_LENGTH);

        offset += SPI_LENGTH_BYTES_LENGTH;
        setSecurityParameterIndex(readString(bytes, offset, securityParameterIndexLength));
    }

    public byte getMessageType()
    {
        return SRV_RQST_TYPE;
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

    /**
     * An LDAPv3 filter to match service attributes
     */
    public String getFilter()
    {
        return filter;
    }

    public void setFilter(String filter)
    {
        this.filter = filter;
    }

    public String getSecurityParameterIndex()
    {
        return securityParameterIndex;
    }

    public void setSecurityParameterIndex(String securityParameterIndex)
    {
        this.securityParameterIndex = securityParameterIndex;
    }

    public String toString()
    {
        StringBuffer result = new StringBuffer("[SrvRqst@").append(Integer.toHexString(hashCode()));

        result.append(" (");
        Set responders = getPreviousResponders();
        for (Iterator iterator = responders.iterator(); iterator.hasNext();)
        {
            result.append(iterator.next());
            if (iterator.hasNext()) result.append(",");
        }
        result.append(")");

        result.append(" ").append(getServiceType());

        String[] scopes = getScopes();
        if (scopes != null)
        {
            result.append(" ");
            for (int i = 0; i < scopes.length; i++)
            {
                if (i > 0) result.append(",");
                result.append(scopes[i]);
            }
        }

        result.append(" ").append(getFilter());

        result.append(" ").append(getSecurityParameterIndex());

        result.append("]");

        return result.toString();
    }
}
