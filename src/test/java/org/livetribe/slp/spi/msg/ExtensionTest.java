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

import java.util.ArrayList;
import java.util.Collection;

import org.livetribe.slp.Attributes;
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
        original.setHost("1.2.3.4");

        byte[] bytes = original.serialize();
        IdentifierExtension deserialized = (IdentifierExtension)Extension.deserialize(bytes);

        assert original.getIdentifier().equals(deserialized.getIdentifier());
        assert original.getHost().equals(deserialized.getHost());
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
        originalExtension.setHost("1.2.3.4");
        original.addExtension(originalExtension);

        byte[] bytes = original.serialize();
        SrvRqst deserialized = (SrvRqst)Message.deserialize(bytes);
        Collection extensions = deserialized.getExtensions();
        assert extensions != null;
        assert extensions.size() == 1;
        IdentifierExtension deserializedExtension = (IdentifierExtension)extensions.iterator().next();
        assert originalExtension.getIdentifier().equals(deserializedExtension.getIdentifier());
        assert originalExtension.getHost().equals(deserializedExtension.getHost());
    }

    /**
     * @testng.test
     */
    public void testTwoIdentifierExtensions() throws Exception
    {
        SrvRqst srvRqst = new SrvRqst();
        srvRqst.setServiceType(new ServiceType("service:type"));
        IdentifierExtension ext1 = new IdentifierExtension();
        ext1.setIdentifier("id-unique-1");
        ext1.setHost("1.2.3.4");
        srvRqst.addExtension(ext1);
        IdentifierExtension ext2 = new IdentifierExtension();
        ext2.setIdentifier("id2");
        ext2.setHost("255.255.255.255");
        srvRqst.addExtension(ext2);

        byte[] bytes = srvRqst.serialize();
        SrvRqst deserialized = (SrvRqst)Message.deserialize(bytes);
        Collection extensions = deserialized.getExtensions();
        assert extensions != null;
        assert extensions.size() == 2;
        assert new ArrayList(IdentifierExtension.findAll(extensions)).equals(new ArrayList(extensions));
    }

    /**
     * @testng.test
     */
    public void testAttributeListExtension() throws Exception
    {
        AttributeListExtension original = new AttributeListExtension();
        original.setURL("service:foo:bar://baz");
        Attributes originalAttributes = new Attributes("(attr=value)");
        original.setAttributes(originalAttributes);
        // TODO: test auth blocks
//        original.setAuthenticationBlocks();

        byte[] bytes = original.serialize();
        AttributeListExtension deserialized = (AttributeListExtension)Extension.deserialize(bytes);

        assert original.getURL().equals(deserialized.getURL());
        assert originalAttributes.equals(deserialized.getAttributes());
        // TODO: test auth blocks
    }
}
