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
package org.livetribe.slp;

/**
 * @version $Rev$ $Date$
 */
public class ScopesTest extends SLPTestSupport
{
    /**
     * @testng.test
     */
    public void testCaseInsensitivity() throws Exception
    {
        Scopes scopes1 = new Scopes(new String[]{"SCOPE"});
        Scopes scopes2 = new Scopes(new String[]{"scope"});

        assert scopes1.equals(scopes2);
    }

    /**
     * @testng.test
     */
    public void testMatch() throws Exception
    {
        Scopes scopes1 = Scopes.DEFAULT;
        Scopes scopes2 = new Scopes(new String[]{"scope"});

        assert !scopes1.match(scopes2);
        assert !scopes2.match(scopes1);

        scopes1 = new Scopes(new String[]{"scope1", "scope2"});
        scopes2 = new Scopes(new String[]{"scope2"});

        assert scopes1.match(scopes2);
        assert !scopes2.match(scopes1);

        // The special wildcard scope matches no scopes, and will be matched by all scopes
        scopes1 = Scopes.WILDCARD;
        scopes2 = new Scopes(new String[]{"scope2"});

        assert !scopes1.match(scopes2);
        assert scopes2.match(scopes1);
    }

    /**
     * @testng.test
     */
    public void testWeakMatch() throws Exception
    {
        Scopes scopes1 = Scopes.DEFAULT;
        Scopes scopes2 = new Scopes(new String[]{"scope"});

        assert !scopes1.weakMatch(scopes2);
        assert !scopes2.weakMatch(scopes1);

        scopes1 = new Scopes(new String[]{"scope1", "scope2"});
        scopes2 = new Scopes(new String[]{"scope2"});

        assert scopes1.weakMatch(scopes2);
        assert scopes2.weakMatch(scopes1);

        // The special wildcard scope matches no scopes, and will be matched by all scopes
        scopes1 = Scopes.WILDCARD;
        scopes2 = new Scopes(new String[]{"scope2"});

        assert !scopes1.weakMatch(scopes2);
        assert scopes2.weakMatch(scopes1);
    }
}
