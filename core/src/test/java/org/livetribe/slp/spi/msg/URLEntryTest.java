/*
 * Copyright 2006-2008 the original author or authors
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

import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class URLEntryTest
{
    @Test
    public void testSerializeDeserialize() throws Exception
    {
        URLEntry original = new URLEntry();
        original.setLifetime(123);
        original.setURL("url1");
        // TODO: test auth blocks
//        original.setAuthenticationBlocks();

        byte[] serialized = original.serialize();
        URLEntry deserialized = new URLEntry();
        int bytesCount = deserialized.deserialize(serialized, 0);

        assert bytesCount == serialized.length;
        assert deserialized.getLifetime() == original.getLifetime();
        assert original.getURL().equals(deserialized.getURL());
        // TODO: test auth blocks
//        assertTrue(Arrays.equals(original.getAuthenticationBlocks(), deserialized.getAuthenticationBlocks()));
    }
}
