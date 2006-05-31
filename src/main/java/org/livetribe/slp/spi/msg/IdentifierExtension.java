/*
 * Copyright 2006 the original author or authors
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

import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Rev$ $Date$
 */
public class IdentifierExtension extends Extension
{
    private String identifier;

    public int getId()
    {
        return IDENTIFIER_EXTENSION_ID;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    protected byte[] serializeBody() throws ServiceLocationException
    {
        return stringToUTF8Bytes(getIdentifier());
    }

    protected void deserializeBody(byte[] bodyBytes) throws ServiceLocationException
    {
        setIdentifier(utf8BytesToString(bodyBytes, 0, bodyBytes.length));
    }
}
