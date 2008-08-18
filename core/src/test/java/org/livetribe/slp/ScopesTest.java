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
package org.livetribe.slp;

import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class ScopesTest
{
    @Test
    public void testCaseInsensitivity() throws Exception
    {
        Scopes scopes1 = Scopes.from("SCOPE");
        Scopes scopes2 = Scopes.from("scope");

        assert scopes1.equals(scopes2);
    }

    @Test
    public void testMatch() throws Exception
    {
        Scopes scopes1 = Scopes.DEFAULT;
        Scopes scopes2 = Scopes.from("scope");

        assert !scopes1.match(scopes2);
        assert !scopes2.match(scopes1);

        scopes1 = Scopes.from("scope1", "scope2");
        scopes2 = Scopes.from("scope2");

        assert scopes1.match(scopes2);
        assert !scopes2.match(scopes1);

        // The special none scope matches no scopes, and is matched by all scopes
        scopes1 = Scopes.NONE;
        scopes2 = Scopes.from("scope2");

        assert !scopes1.match(scopes2);
        assert scopes2.match(scopes1);

        // The special any scope matches all scopes, and is matched by no scope
        scopes1 = Scopes.ANY;
        scopes2 = Scopes.from("scope2");

        assert scopes1.match(scopes2);
        assert !scopes2.match(scopes1);
    }

    @Test
    public void testWeakMatch() throws Exception
    {
        Scopes scopes1 = Scopes.DEFAULT;
        Scopes scopes2 = Scopes.from("scope");

        assert !scopes1.weakMatch(scopes2);
        assert !scopes2.weakMatch(scopes1);

        scopes1 = Scopes.from("scope1", "scope2");
        scopes2 = Scopes.from("scope2");

        assert scopes1.weakMatch(scopes2);
        assert scopes2.weakMatch(scopes1);

        // The special empty scope matches no scopes, and will be matched by all scopes
        scopes1 = Scopes.NONE;
        scopes2 = Scopes.from("scope2");

        assert !scopes1.weakMatch(scopes2);
        assert scopes2.weakMatch(scopes1);

        // The special any scope matches all scopes, and is matched by no scope
        scopes1 = Scopes.ANY;
        scopes2 = Scopes.from("scope2");

        assert scopes1.match(scopes2);
        assert !scopes2.match(scopes1);
    }

    @Test
    public void testEscapeUnescape()
    {
        Scopes scopes = Scopes.from("*");
        assert !scopes.equals(Scopes.ANY);

        String[] reserved = new String[]{"!", "(", ")", "*", "+", ",", ";", "<", "=", ">", "\\", "~"};
        scopes = Scopes.from(reserved);
        assert Arrays.equals(scopes.asStringArray(), reserved);
    }
}
