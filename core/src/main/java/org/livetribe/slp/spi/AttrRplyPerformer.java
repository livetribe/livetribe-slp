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

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPError;
import org.livetribe.slp.spi.msg.AttrRply;
import org.livetribe.slp.spi.msg.Message;

/**
 * @version $Revision$ $Date$
 */
public class AttrRplyPerformer
{
    protected AttrRply newAttrRply(Message message, Attributes attributes)
    {
        return newAttrRply(message, attributes, Integer.MAX_VALUE);
    }

    protected AttrRply newAttrRply(Message message, Attributes attributes, int maxLength)
    {
        AttrRply attrRply = newAttrRply(message, SLPError.NO_ERROR);
        attrRply.setAttributes(attributes);
        byte[] attrRplyBytes = attrRply.serialize();
        if (attrRplyBytes.length > maxLength)
        {
            attrRply.setAttributes(Attributes.NONE);
            attrRply.setOverflow(true);
        }
        return attrRply;
    }

    protected AttrRply newAttrRply(Message message, SLPError error)
    {
        AttrRply attrRply = new AttrRply();
        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        attrRply.setLanguage(message.getLanguage());
        attrRply.setXID(message.getXID());
        attrRply.setSLPError(error);
        return attrRply;
    }
}
