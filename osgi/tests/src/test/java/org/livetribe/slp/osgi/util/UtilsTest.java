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

import static junit.framework.Assert.assertEquals;
import org.junit.Test;


/**
 * @version $Revision$ $Date$
 */
public class UtilsTest
{
    @Test
    public void test()
    {
        Properties p = new Properties();
        p.put("foo", "bar");
        p.put("car", "cdr");

        assertEquals("This is a bar test", Utils.subst("This is a ${foo} test", p));
        assertEquals("This is a bar cdr test", Utils.subst("This is a ${foo} ${car} ${oops}test", p));
        assertEquals("This is a bar cdr tes${t", Utils.subst("This is a ${foo} ${car} ${oops}tes${t", p));

    }
}
