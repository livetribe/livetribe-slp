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
package org.livetribe.slp.spi.da.filter;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.spi.SLPSPITestCase;

/**
 * @version $Rev$ $Date$
 */
public class FilterTest extends SLPSPITestCase
{
    public void testMatchSingleString() throws Exception
    {
        String attributeList = "(a=foo)";
        Attributes attributes = new Attributes(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(a=Foo)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(a >= f)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(a >= F)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(a<=g)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(a=f*o)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(a=FO*)");
        assertTrue(filter.match(attributes));
    }

    public void testMatchSingleLong() throws Exception
    {
        String attributeList = "(a=14)";
        Attributes attributes = new Attributes(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(a=14)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(a >= 10)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(a<=17)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(a=foo)");
        try
        {
            filter.match(attributes);
            fail();
        }
        catch (ServiceLocationException x)
        {
            assertEquals(ServiceLocationException.PARSE_ERROR, x.getErrorCode());
        }
    }

    public void testMatchSingleBoolean() throws Exception
    {
        String attributeList = "(a=true)";
        Attributes attributes = new Attributes(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(a=true)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(!(a=false))");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(a>=false)");
        try
        {
            assertTrue(filter.match(attributes));
            fail();
        }
        catch (ServiceLocationException x)
        {
            assertEquals(ServiceLocationException.PARSE_ERROR, x.getErrorCode());
        }

        filter = parser.parse("(a<=true)");
        try
        {
            assertTrue(filter.match(attributes));
            fail();
        }
        catch (ServiceLocationException x)
        {
            assertEquals(ServiceLocationException.PARSE_ERROR, x.getErrorCode());
        }

        filter = parser.parse("(a=foo)");
        try
        {
            filter.match(attributes);
            fail();
        }
        catch (ServiceLocationException x)
        {
            assertEquals(ServiceLocationException.PARSE_ERROR, x.getErrorCode());
        }

        filter = parser.parse("(a=10)");
        try
        {
            filter.match(attributes);
            fail();
        }
        catch (ServiceLocationException x)
        {
            assertEquals(ServiceLocationException.PARSE_ERROR, x.getErrorCode());
        }
    }

    public void testMatchSinglePresence() throws Exception
    {
        String attributeList = "bar";
        Attributes attributes = new Attributes(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(bar=*)");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(foo=*)");
        assertFalse(filter.match(attributes));
    }

    public void testMultipleParenthesis() throws Exception
    {
        String attributeList = "(bar=foo)";
        Attributes attributes = new Attributes(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(((bar=*)))");
        assertTrue(filter.match(attributes));
    }

    public void testMatchMultipleStrings() throws Exception
    {
        String attributeList = "(x=true,2,bar)";
        Attributes attributes = new Attributes(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(x=bar)");
        assertTrue(filter.match(attributes));
    }

    public void testMatchMultipleLongs() throws Exception
    {
        String attributeList = "(x=1,2,3)";
        Attributes attributes = new Attributes(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(x=2)");
        assertTrue(filter.match(attributes));
    }

    public void testMatchMultipleBooleans() throws Exception
    {
        String attributeList = "(x=true,false)";
        Attributes attributes = new Attributes(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(x=true)");
        assertTrue(filter.match(attributes));
    }

    public void testMultipleMatches() throws Exception
    {
        String attributeList = "(a=1,2),(b=false),(name=name1)";
        Attributes attributes = new Attributes(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(&(a=1)(b=false))");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(&(a=1)(b=true))");
        assertFalse(filter.match(attributes));

        filter = parser.parse("(&(!(b=false))(a=1))");
        assertFalse(filter.match(attributes));

        filter = parser.parse("(&(!(b=true))(name=name*))");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(|(name=name*)(&(!(b=true))(a=2)))");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(|(name=name*)(&(!(b=true))(a=3)))");
        assertTrue(filter.match(attributes));

        filter = parser.parse("(|(a=3)(&(!(b=true))))");
        assertTrue(filter.match(attributes));
    }
}
