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

import junit.framework.TestCase;

/**
 * @version $Rev$ $Date$
 */
public abstract class SLPTestSupport extends TestCase
{
    protected void sleep(long time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
        }
    }

    protected int getPort()
    {
        // Default SLP port (427) is a reserved port in Unix-like operative systems
        return 1427;
    }

    public static void assertTrue(boolean value)
    {
        assert value;
    }

    public static void assertFalse(boolean value)
    {
        assert !value;
    }

    public static void assertNotNull(Object value)
    {
        assert value != null;
    }

    public static void assertEquals(Object expected, Object value)
    {
        if (expected != value)
        {
            assert expected != null ? expected.equals(value) : value.equals(expected);
        }
    }
}
