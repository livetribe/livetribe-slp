/*
 * Copyright 2007 the original author or authors
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
package org.livetribe.slp.srv.da;

import java.net.Socket;
import java.util.List;

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.srv.msg.AttributeListExtension;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.msg.ScopeListExtension;
import org.livetribe.slp.srv.msg.SrvRply;
import org.livetribe.slp.srv.msg.URLEntry;
import org.livetribe.slp.srv.msg.LanguageExtension;
import org.livetribe.slp.srv.net.TCPConnector;

/**
 * @version $Revision$ $Date$
 */
public class TCPSrvRplyPerformer
{
    private final TCPConnector tcpConnector;

    public TCPSrvRplyPerformer(TCPConnector tcpConnector, Settings settings)
    {
        this.tcpConnector = tcpConnector;
    }

    public void perform(Socket socket, Message message, List<? extends ServiceInfo> services)
    {
        SrvRply srvRply = newSrvRply(message, services);
        byte[] bytes = srvRply.serialize();
        tcpConnector.write(socket, bytes);
    }

    private SrvRply newSrvRply(Message message, List<? extends ServiceInfo> services)
    {
        SrvRply srvRply = new SrvRply();
        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        srvRply.setLanguage(message.getLanguage());
        srvRply.setXID(message.getXID());

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

            // Add attributes only if they were requested
            if (AttributeListExtension.findFirst(message.getExtensions()) != null)
            {
                AttributeListExtension attributesExt = new AttributeListExtension();
                attributesExt.setURL(serviceURL.getURL());
                attributesExt.setAttributes(service.getAttributes());
                srvRply.addExtension(attributesExt);
            }

            // Add scopes only if they were requested
            if (ScopeListExtension.findFirst(message.getExtensions()) != null)
            {
                ScopeListExtension scopesExt = new ScopeListExtension();
                scopesExt.setURL(serviceURL.getURL());
                scopesExt.setScopes(service.getScopes());
                srvRply.addExtension(scopesExt);
            }
        }

        return srvRply;
    }
}
