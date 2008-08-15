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
package org.livetribe.slp.spi.sa;

import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.msg.URLEntry;

/**
 * @version $Revision$ $Date$
 */
public class SrvRegPerformer
{
    protected SrvReg newSrvReg(ServiceInfo service, boolean update)
    {
        ServiceURL serviceURL = service.getServiceURL();
        URLEntry urlEntry = new URLEntry();
        urlEntry.setLifetime(serviceURL.getLifetime());
        urlEntry.setURL(serviceURL.getURL());
        SrvReg srvReg = new SrvReg();
        srvReg.setFresh(!update);
        srvReg.setURLEntry(urlEntry);
        srvReg.setServiceType(service.resolveServiceType());
        srvReg.setScopes(service.getScopes());
        srvReg.setAttributes(service.getAttributes());
        srvReg.setXID(Message.newXID());
        srvReg.setLanguage(service.getLanguage());
        return srvReg;
    }
}
