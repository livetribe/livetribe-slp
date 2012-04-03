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


/**
 *
 */
public class UtilsTest
{
    @Test
    public void test()
    {
        Properties p = new Properties();
        p.put("foo", "bar");
        p.put("car", "cdr");

        assert "This is a bar test".equals(Utils.subst("This is a ${foo} test", p));
        assert "This is a bar cdr test".equals(Utils.subst("This is a ${foo} ${car} ${oops}test", p));
        assert "This is a bar cdr tes${t".equals(Utils.subst("This is a ${foo} ${car} ${oops}tes${t", p));
    }
}
