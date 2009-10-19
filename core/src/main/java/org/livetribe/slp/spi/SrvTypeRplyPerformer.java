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
package org.livetribe.slp.spi;

import java.util.List;

import org.livetribe.slp.SLPError;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvTypeRply;

/**
 * @version $Revision$ $Date$
 */
public class SrvTypeRplyPerformer
{
    protected SrvTypeRply newSrvTypeRply(Message message, List<ServiceType> serviceTypes)
    {
        return newSrvTypeRply(message, serviceTypes, Integer.MAX_VALUE);
    }

    protected SrvTypeRply newSrvTypeRply(Message message, List<ServiceType> serviceTypes, int maxLength)
    {
        SrvTypeRply srvTypeRply = newSrvTypeRply(message, SLPError.NO_ERROR);
        SrvTypeRply result = (SrvTypeRply)Message.deserialize(srvTypeRply.serialize());
        for (ServiceType serviceType : serviceTypes)
        {
            srvTypeRply.addServiceType(serviceType);

            byte[] srvTypeRplyBytes = srvTypeRply.serialize();
            if (srvTypeRplyBytes.length > maxLength)
            {
                result.setOverflow(true);
                break;
            }

            result = (SrvTypeRply)Message.deserialize(srvTypeRplyBytes);
        }
        return srvTypeRply;
    }

    protected SrvTypeRply newSrvTypeRply(Message message, SLPError error)
    {
        SrvTypeRply srvTypeRply = new SrvTypeRply();
        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        srvTypeRply.setLanguage(message.getLanguage());
        srvTypeRply.setXID(message.getXID());
        srvTypeRply.setSLPError(error);
        return srvTypeRply;
    }
}
