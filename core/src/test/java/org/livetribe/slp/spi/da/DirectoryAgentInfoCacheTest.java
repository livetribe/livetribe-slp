/**
 *
 * Copyright 2008-2010 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.spi.da;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.testng.annotations.Test;

/**
 * @version $Revision$ $Date$
 */
public class DirectoryAgentInfoCacheTest
{
    @Test
    public void testAdd()
    {
        DirectoryAgentInfoCache cache = new DirectoryAgentInfoCache();

        Long bootTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        DirectoryAgentInfo info = DirectoryAgentInfo.from("1.2.3.4", Scopes.from("test"), Attributes.from("(a=1)"), "en", bootTime.intValue());
        DirectoryAgentInfo existing = cache.add(info);
        assert existing == null;

        List<DirectoryAgentInfo> infos = cache.match(null, null);
        assert infos.size() == 1;
        DirectoryAgentInfo testInfo = infos.get(0);
        assert testInfo == info;
    }

    @Test
    public void testAddWithSameKey()
    {
        DirectoryAgentInfoCache cache = new DirectoryAgentInfoCache();

        DirectoryAgentInfo info1 = DirectoryAgentInfo.from("1.2.3.4");
        cache.add(info1);

        Long bootTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        DirectoryAgentInfo info2 = DirectoryAgentInfo.from(info1.getHostAddress(), Scopes.from("test"), Attributes.from("(a=1)"), "en", bootTime.intValue());
        DirectoryAgentInfo existing = cache.add(info2);
        assert existing == info1;

        List<DirectoryAgentInfo> infos = cache.match(null, null);
        assert infos.size() == 1;
        DirectoryAgentInfo testInfo = infos.get(0);
        assert testInfo.getAttributes().equals(info2.getAttributes());
        assert testInfo.getBootTime() == info2.getBootTime();
        assert testInfo.getHostAddress().equals(info2.getHostAddress());
        assert testInfo.getKey().equals(info2.getKey());
        assert testInfo.getLanguage().equals(info2.getLanguage());
        assert testInfo.getScopes().equals(info2.getScopes());
        assert testInfo.getURL().equals(info2.getURL());
    }

    @Test
    public void testRemoveAll()
    {
        DirectoryAgentInfoCache cache = new DirectoryAgentInfoCache();

        DirectoryAgentInfo info1 = DirectoryAgentInfo.from("1.2.3.4");
        cache.add(info1);
        DirectoryAgentInfo info2 = DirectoryAgentInfo.from("2.3.4.5");
        cache.add(info2);

        cache.removeAll();
        assert cache.match(null, null).size() == 0;
    }

    // TODO: add more tests
}
