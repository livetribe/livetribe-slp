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

import java.util.Collection;

import org.livetribe.slp.SLPTestSupport;
import org.livetribe.slp.ServiceType;

/**
 * @version $Rev$ $Date$
 */
public class ExtensionTest extends SLPTestSupport
{
    /**
     * @testng.test
     */
    public void testIdentifierExtension() throws Exception
    {
        IdentifierExtension original = new IdentifierExtension();
        original.setIdentifier("id1");

        byte[] bytes = original.serialize();
        IdentifierExtension deserialized = (IdentifierExtension)Extension.deserialize(bytes);

        assert original.getIdentifier().equals(deserialized.getIdentifier());
    }

    /**
     * @testng.test
     */
    public void testMessageWithIdentifierExtension() throws Exception
    {
        SrvRqst original = new SrvRqst();
        original.setServiceType(new ServiceType("service:type"));
        IdentifierExtension originalExtension = new IdentifierExtension();
        originalExtension.setIdentifier("id1");
        original.addExtension(originalExtension);

        byte[] bytes = original.serialize();
        SrvRqst deserialized = (SrvRqst)Message.deserialize(bytes);
        Collection extensions = deserialized.getExtensions();
        assert extensions != null;
        assert extensions.size() == 1;
        IdentifierExtension deserializedExtension = (IdentifierExtension)extensions.iterator().next();
        assert originalExtension.getIdentifier().equals(deserializedExtension.getIdentifier());
    }
}
