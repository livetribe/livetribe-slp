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
package org.livetribe.slp.spi.ua;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvTypeRqst;

/**
 * @version $Revision$ $Date$
 */
public abstract class SrvTypeRqstPerformer
{
    protected SrvTypeRqst newSrvTypeRqst(String namingAuthority, Scopes scopes)
    {
        SrvTypeRqst srvTypeRqst = new SrvTypeRqst();
        srvTypeRqst.setXID(Message.newXID());
        srvTypeRqst.setScopes(scopes);
        if (ServiceType.ANY_NAMING_AUTHORITY.equals(namingAuthority))
            srvTypeRqst.setAnyNamingAuthority(true);
        else
            srvTypeRqst.setNamingAuthority(namingAuthority);
        return srvTypeRqst;
    }
}
