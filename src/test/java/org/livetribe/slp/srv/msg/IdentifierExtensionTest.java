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
package org.livetribe.slp.srv.msg;

import java.util.ArrayList;
import java.util.Collection;

import org.livetribe.slp.ServiceType;
import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class IdentifierExtensionTest
{
    @Test
    public void testIdentifierExtension() throws Exception
    {
        IdentifierExtension original = new IdentifierExtension("1.2.3.4", "id1");

        byte[] bytes = original.serialize();
        IdentifierExtension deserialized = (IdentifierExtension)Extension.deserialize(bytes);
        // TODO: test auth blocks

        assert original.getIdentifier().equals(deserialized.getIdentifier());
        assert original.getHost().equals(deserialized.getHost());
        // TODO: test auth blocks
    }

    @Test
    public void testMessageWithIdentifierExtension() throws Exception
    {
        SrvRqst original = new SrvRqst();
        original.setServiceType(new ServiceType("service:type"));
        IdentifierExtension originalExtension = new IdentifierExtension("1.2.3.4", "id1");
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

    @Test
    public void testTwoIdentifierExtensions() throws Exception
    {
        SrvRqst srvRqst = new SrvRqst();
        srvRqst.setServiceType(new ServiceType("service:type"));
        IdentifierExtension ext1 = new IdentifierExtension("1.2.3.4", "id-unique-1");
        srvRqst.addExtension(ext1);
        IdentifierExtension ext2 = new IdentifierExtension("255.255.255.255", "id2");
        srvRqst.addExtension(ext2);

        byte[] bytes = srvRqst.serialize();
        SrvRqst deserialized = (SrvRqst)Message.deserialize(bytes);
        Collection<Extension> extensions = deserialized.getExtensions();
        assert extensions != null;
        assert extensions.size() == 2;
        // Wrap into ArrayList to be sure that equals() is overridden properly
        assert new ArrayList<Extension>(IdentifierExtension.findAll(extensions)).equals(new ArrayList<Extension>(extensions));
    }
}
