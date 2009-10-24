/**
 *
 * Copyright 2009 (C) The original author or authors
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
package org.livetribe.slp.osgi.util;

import java.util.Properties;

import org.testng.annotations.Test;

import org.livetribe.slp.settings.Keys;


/**
 * @version $Revision$ $Date$
 */
public class DictionarySettingsTest
{
    @Test
    public void testDictionary()
    {
        Properties p = new Properties();
        p.put(Keys.LANGUAGE_KEY.getKey(), "en");

        DictionarySettings ds = DictionarySettings.from(p);

        assert "en".equals(ds.get(Keys.LANGUAGE_KEY));
    }
}
