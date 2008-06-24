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
package org.livetribe.slp.srv.msg;

import java.util.Locale;

import org.testng.annotations.Test;

/**
 * @version $Revision$ $Date$
 */
public class LanguageExtensionTest
{
    @Test
    public void testScopeListExtension()
    {
        LanguageExtension original = new LanguageExtension();
        original.setURL("service:abstract:concrete://scopeListExtension");
        original.setLanguage(Locale.ITALIAN.getLanguage());
        // TODO: test auth blocks

        byte[] bytes = original.serialize();
        LanguageExtension deserialized = (LanguageExtension)Extension.deserialize(bytes);

        assert original.getURL().equals(deserialized.getURL());
        assert original.getLanguage().equals(deserialized.getLanguage());
        // TODO: test auth blocks
    }
}
