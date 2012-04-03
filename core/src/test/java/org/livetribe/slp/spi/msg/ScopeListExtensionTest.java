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

import org.livetribe.slp.Scopes;


/**
 *
 */
public class ScopeListExtensionTest
{
    @Test
    public void testScopeListExtension()
    {
        ScopeListExtension original = new ScopeListExtension();
        original.setURL("service:abstract:concrete://scopeListExtension");
        Scopes scopes = Scopes.from("scope1", "scope2");
        original.setScopes(scopes);
        // TODO: test auth blocks

        byte[] bytes = original.serialize();
        ScopeListExtension deserialized = (ScopeListExtension)Extension.deserialize(bytes);

        assert original.getURL().equals(deserialized.getURL());
        assert original.getScopes().equals(deserialized.getScopes());
        // TODO: test auth blocks
    }
}
