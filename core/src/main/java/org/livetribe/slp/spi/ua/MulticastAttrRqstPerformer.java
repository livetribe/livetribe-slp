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

import java.util.List;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.Converger;
import org.livetribe.slp.spi.msg.AttrRply;
import org.livetribe.slp.spi.msg.AttrRqst;
import org.livetribe.slp.spi.net.UDPConnector;


/**
 *
 */
public class MulticastAttrRqstPerformer extends AttrRqstPerformer
{
    private final Converger<AttrRply> converger;

    public MulticastAttrRqstPerformer(UDPConnector udpConnector, Settings settings)
    {
        this.converger = new Converger<AttrRply>(udpConnector, settings);
    }

    public List<AttrRply> perform(String url, String language, Scopes scopes, Attributes tags)
    {
        AttrRqst attrRqst = newAttrRqst(url, language, scopes, tags);
        attrRqst.setMulticast(true);
        return converger.converge(attrRqst);
    }
}
