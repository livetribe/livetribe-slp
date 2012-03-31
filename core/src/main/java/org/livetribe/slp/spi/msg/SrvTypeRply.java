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

import java.util.ArrayList;
import java.util.List;

import org.livetribe.slp.SLPError;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;


/**
 * The RFC 2608 SrvTypeRply message body is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       Service Location header (function = SrvTypeRply = 10)   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Error Code            |   length of [srvtype-list]    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         [srvtype-list]                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 */
public class SrvTypeRply extends Rply
{
    private static final int ERROR_CODE_BYTES_LENGTH = 2;
    private static final int SERVICE_TYPES_LENGTH_BYTES_LENGTH = 2;

    private final List<ServiceType> serviceTypes = new ArrayList<ServiceType>();
    private SLPError error = SLPError.NO_ERROR;

    protected byte[] serializeBody() throws ServiceLocationException
    {
        byte[] serviceTypesBytes = EMPTY_BYTES;
        List<ServiceType> types = getServiceTypes();
        if (types != null)
        {
            String[] serviceTypeStrings = new String[types.size()];
            for (int i = 0; i < types.size(); ++i) serviceTypeStrings[i] = types.get(i).asString();
            serviceTypesBytes = writeStringArray(serviceTypeStrings, true);
        }
        int serviceTypesLength = serviceTypesBytes.length;

        int bodyLength = ERROR_CODE_BYTES_LENGTH + SERVICE_TYPES_LENGTH_BYTES_LENGTH + serviceTypesLength;
        byte[] result = new byte[bodyLength];

        int offset = 0;
        writeInt(getSLPError().getCode(), result, offset, ERROR_CODE_BYTES_LENGTH);

        offset += ERROR_CODE_BYTES_LENGTH;
        writeInt(serviceTypesLength, result, offset, SERVICE_TYPES_LENGTH_BYTES_LENGTH);

        offset += SERVICE_TYPES_LENGTH_BYTES_LENGTH;
        System.arraycopy(serviceTypesBytes, 0, result, offset, serviceTypesLength);

        return result;
    }

    protected void deserializeBody(byte[] bytes) throws ServiceLocationException
    {
        int offset = 0;
        setSLPError(SLPError.from(readInt(bytes, offset, ERROR_CODE_BYTES_LENGTH)));

        // The message may be truncated if an error occurred (RFC 2608, Chapter 7)
        if (getSLPError() != SLPError.NO_ERROR && bytes.length == ERROR_CODE_BYTES_LENGTH) return;

        offset += ERROR_CODE_BYTES_LENGTH;
        int serviceTypesBytes = readInt(bytes, offset, SERVICE_TYPES_LENGTH_BYTES_LENGTH);

        offset += SERVICE_TYPES_LENGTH_BYTES_LENGTH;
        String[] serviceTypeStrings = readStringArray(bytes, offset, serviceTypesBytes, true);
        for (String serviceTypeString : serviceTypeStrings) addServiceType(new ServiceType(serviceTypeString));
    }

    public byte getMessageType()
    {
        return SRV_TYPE_RPLY_TYPE;
    }

    public SLPError getSLPError()
    {
        return error;
    }

    public void setSLPError(SLPError error)
    {
        this.error = error;
    }

    public List<ServiceType> getServiceTypes()
    {
        return serviceTypes;
    }

    public void addServiceType(ServiceType serviceType)
    {
        serviceTypes.add(serviceType);
    }
}
