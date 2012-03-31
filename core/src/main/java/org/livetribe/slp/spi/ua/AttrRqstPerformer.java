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

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.spi.msg.AttrRqst;
import org.livetribe.slp.spi.msg.Message;


/**
 *
 */
public class AttrRqstPerformer
{
    protected AttrRqst newAttrRqst(String url, String language, Scopes scopes, Attributes tags)
    {
        AttrRqst attrRqst = new AttrRqst();
        attrRqst.setXID(Message.newXID());
        attrRqst.setLanguage(language);
        attrRqst.setURL(url);
        attrRqst.setScopes(scopes);
        attrRqst.setTags(tags);
        return attrRqst;
    }
}
