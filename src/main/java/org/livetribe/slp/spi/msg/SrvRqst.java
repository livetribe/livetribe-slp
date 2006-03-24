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

import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceLocationException;

/**
 * The RFC 2608 SrvRqst message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       Service Location header (function = SrvRqst = 1)        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      length of <PRList>       |        <PRList> String        \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   length of <service-type>    |    <service-type> String      \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    length of <scope-list>     |     <scope-list> String       \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  length of predicate string   |  Service Request <predicate>  \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  length of <SLP SPI> string   |       <SLP SPI> String        \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * $Rev$
 */
public class SrvRqst extends Rqst
{
    private ServiceType serviceType;
    private String[] scopes;
    private String filter;
    private String securityParameterIndex;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] previousRespondersBytes = stringArrayToBytes(getPreviousResponders());
        int previousRespondersLength = previousRespondersBytes.length;
        byte[] serviceTypeBytes = stringToBytes(getServiceType().toString());
        int serviceTypeLength = serviceTypeBytes.length;
        byte[] scopesBytes = stringArrayToBytes(getScopes());
        int scopesLength = scopesBytes.length;
        byte[] filterBytes = stringToBytes(getFilter());
        int filterLength = filterBytes.length;
        byte[] securityParameterIndexBytes = stringToBytes(getSecurityParameterIndex());
        int securityParameterIndexLength = securityParameterIndexBytes.length;

        int lengthBytes = 2;

        byte[] result = new byte[lengthBytes * 5 + previousRespondersLength + serviceTypeLength + scopesLength + filterLength + securityParameterIndexLength];

        int offset = 0;
        writeInt(previousRespondersLength, result, offset, lengthBytes);

        offset += lengthBytes;
        System.arraycopy(previousRespondersBytes, 0, result, offset, previousRespondersLength);

        offset += previousRespondersLength;
        writeInt(serviceTypeLength, result, offset, lengthBytes);

        offset += lengthBytes;
        System.arraycopy(serviceTypeBytes, 0, result, offset, serviceTypeLength);

        offset += serviceTypeLength;
        writeInt(scopesLength, result, offset, lengthBytes);

        offset += lengthBytes;
        System.arraycopy(scopesBytes, 0, result, offset, scopesLength);

        offset += scopesLength;
        writeInt(filterLength, result, offset, lengthBytes);

        offset += lengthBytes;
        System.arraycopy(filterBytes, 0, result, offset, filterLength);

        offset += filterLength;
        writeInt(securityParameterIndexLength, result, offset, lengthBytes);

        offset += lengthBytes;
        System.arraycopy(securityParameterIndexBytes, 0, result, offset, securityParameterIndexLength);

        return result;
    }

    protected void deserializeBody(byte[] bytes) throws ServiceLocationException
    {
        int lengthBytes = 2;

        int offset = 0;
        int previousRespondersLength = readInt(bytes, offset, lengthBytes);

        offset += lengthBytes;
        setPreviousResponders(readStringArray(bytes, offset, previousRespondersLength));

        offset += previousRespondersLength;
        int serviceTypeLength = readInt(bytes, offset, lengthBytes);

        offset += lengthBytes;
        setServiceType(new ServiceType(readString(bytes, offset, serviceTypeLength)));

        offset += serviceTypeLength;
        int scopesLength = readInt(bytes, offset, lengthBytes);

        offset += lengthBytes;
        setScopes(readStringArray(bytes, offset, scopesLength));

        offset += scopesLength;
        int filterLength = readInt(bytes, offset, lengthBytes);

        offset += lengthBytes;
        setFilter(readString(bytes, offset, filterLength));

        offset += filterLength;
        int securityParameterIndexLength = readInt(bytes, offset, lengthBytes);

        offset += lengthBytes;
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
}
