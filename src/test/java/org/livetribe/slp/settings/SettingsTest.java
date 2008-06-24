/*
 * Copyright 2007-2008 the original author or authors
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
package org.livetribe.slp.settings;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.testng.annotations.Test;

/**
 * @version $Revision$ $Date$
 */
public class SettingsTest
{
    @Test
    public void testGetBoolean()
    {
        String property = "test";
        Key<Boolean> key = Key.forType(property, Boolean.class);
        String value = "true";

        Properties properties = new Properties();
        properties.setProperty(property, value);
        Settings settings = new PropertiesSettings(properties);

        assert settings.get(key);
    }

    @Test
    public void testGetClass()
    {
        String property = "test";
        Key<Class<Lock>> key = Key.forClass(property, Lock.class);
        String value = Lock.class.getName();

        Properties properties = new Properties();
        properties.setProperty(property, value);
        Settings settings = new PropertiesSettings(properties);

        assert settings.get(key) == Lock.class;
    }

    @Test
    public void testGetInteger()
    {
        String property = "test";
        Key<Integer> key = Key.forType(property, Integer.class);
        String value = "13";

        Properties properties = new Properties();
        properties.setProperty(property, value);
        Settings settings = new PropertiesSettings(properties);

        assert settings.get(key).equals(Integer.valueOf(value));
    }

    @Test
    public void testGetIntArray()
    {
        String property = "test";
        Key<int[]> key = Key.forType(property, int[].class);
        String value = "1,\f1,\r\n2,\r3,\n5, 8,\t13";

        Properties properties = new Properties();
        properties.setProperty(property, value);
        Settings settings = new PropertiesSettings(properties);

        assert Arrays.equals(settings.get(key), new int[]{1, 1, 2, 3, 5, 8, 13});
    }

    @Test
    public void testGetStringArray()
    {
        String property = "test";
        Key<String[]> key = Key.forType(property, String[].class);
        String value = "A, B ,C";

        Properties properties = new Properties();
        properties.setProperty(property, value);
        Settings settings = new PropertiesSettings(properties);

        assert Arrays.equals(settings.get(key), value.split(","));
    }

    @Test
    public void testGetWithDefault()
    {
        String property = "test";
        Key<String> key = Key.forType(property, String.class);
        String originalValue = "value1";
        String overriddenValue = "value2";

        Properties props = new Properties();
        props.setProperty(property, originalValue);
        Settings original = new PropertiesSettings(props);

        Settings override = new MapSettings();
        override.put(key, overriddenValue);

        assert override.get(key, original.get(key)).equals(overriddenValue);
    }
}
