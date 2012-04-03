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
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.spi.msg.AttributeListExtension;
import org.livetribe.slp.spi.msg.LanguageExtension;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.ScopeListExtension;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;


/**
 *
 */
public class SrvRplyPerformer
{
    protected SrvRply newSrvRply(Message message, List<? extends ServiceInfo> services)
    {
        return newSrvRply(message, services, Integer.MAX_VALUE);
    }

    protected SrvRply newSrvRply(Message message, List<? extends ServiceInfo> services, int maxLength)
    {
        SrvRply srvRply = newSrvRply(message, SLPError.NO_ERROR);

        SrvRply result = (SrvRply)Message.deserialize(srvRply.serialize());

        for (ServiceInfo service : services)
        {
            ServiceURL serviceURL = service.getServiceURL();

            URLEntry urlEntry = new URLEntry();
            urlEntry.setURL(serviceURL.getURL());
            urlEntry.setLifetime(serviceURL.getLifetime());
            srvRply.addURLEntry(urlEntry);

            // Add language only if it has been requested
            if (LanguageExtension.findFirst(message.getExtensions()) != null)
            {
                LanguageExtension languageExtension = new LanguageExtension();
                languageExtension.setURL(serviceURL.getURL());
                languageExtension.setLanguage(service.getLanguage());
                srvRply.addExtension(languageExtension);
            }

            // Add scopes only if they were requested
            if (ScopeListExtension.findFirst(message.getExtensions()) != null)
            {
                ScopeListExtension scopesExt = new ScopeListExtension();
                scopesExt.setURL(serviceURL.getURL());
                scopesExt.setScopes(service.getScopes());
                srvRply.addExtension(scopesExt);
            }

            // Add attributes only if they were requested
            if (AttributeListExtension.findFirst(message.getExtensions()) != null)
            {
                AttributeListExtension attributesExt = new AttributeListExtension();
                attributesExt.setURL(serviceURL.getURL());
                attributesExt.setAttributes(service.getAttributes());
                srvRply.addExtension(attributesExt);
            }

            byte[] srvRplyBytes = srvRply.serialize();
            if (srvRplyBytes.length > maxLength)
            {
                result.setOverflow(true);
                break;
            }

            result = (SrvRply)Message.deserialize(srvRplyBytes);
        }

        return result;
    }

    protected SrvRply newSrvRply(Message message, SLPError error)
    {
        SrvRply srvRply = new SrvRply();
        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        srvRply.setLanguage(message.getLanguage());
        srvRply.setXID(message.getXID());
        srvRply.setSLPError(error);
        return srvRply;
    }
}
