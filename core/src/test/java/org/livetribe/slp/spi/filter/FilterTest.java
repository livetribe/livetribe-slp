/*
 * Copyright 2006-2010 the original author or authors
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
package org.livetribe.slp.spi.filter;

import org.testng.annotations.Test;

import org.livetribe.slp.Attributes;


/**
 * @version $Rev$ $Date$
 */
public class FilterTest
{
    @Test
    public void testMatchSingleString() throws Exception
    {
        String attributeList = "(a=foo)";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(a=Foo)");
        assert filter.matches(attributes);

        filter = parser.parse("(a >= f)");
        assert filter.matches(attributes);

        filter = parser.parse("(a >= F)");
        assert filter.matches(attributes);

        filter = parser.parse("(a<=g)");
        assert filter.matches(attributes);

        filter = parser.parse("(a=f*o)");
        assert filter.matches(attributes);

        filter = parser.parse("(a=FO*)");
        assert filter.matches(attributes);
    }

    @Test
    public void testMatchWithWhitespace() throws Exception
    {
        String attributeList = "(a=fo  o)";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("( a   = Fo  o )");
        assert filter.matches(attributes);

        filter = parser.parse("(a >= f)");
        assert filter.matches(attributes);

        filter = parser.parse("(a >= F)");
        assert filter.matches(attributes);

        filter = parser.parse("(a<=g)");
        assert filter.matches(attributes);

        filter = parser.parse("(a=f*o)");
        assert filter.matches(attributes);

        filter = parser.parse("(a=FO*)");
        assert filter.matches(attributes);

        filter = parser.parse("(a=fo   o*)");
        assert filter.matches(attributes);

        filter = parser.parse("(a=fo  o*)");
        assert filter.matches(attributes);

        filter = parser.parse("(a=fo o*)");
        assert filter.matches(attributes);
    }

    @Test
    public void testMatchSingleLong() throws Exception
    {
        String attributeList = "(a=14)";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(a=14)");
        assert filter.matches(attributes);

        filter = parser.parse("(a >= 10)");
        assert filter.matches(attributes);

        filter = parser.parse("(a<=17)");
        assert filter.matches(attributes);

        filter = parser.parse("(a=foo)");
        assert !filter.matches(attributes);
    }

    @Test
    public void testMatchSingleBoolean() throws Exception
    {
        String attributeList = "(a=true)";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(a=true)");
        assert filter.matches(attributes);

        filter = parser.parse("(!(a=false))");
        assert filter.matches(attributes);

        filter = parser.parse("(a>=false)");
        assert !filter.matches(attributes);

        filter = parser.parse("(a<=true)");
        assert !filter.matches(attributes);

        filter = parser.parse("(a=foo)");
        assert !filter.matches(attributes);

        filter = parser.parse("(a=10)");
        assert !filter.matches(attributes);
    }

    @Test
    public void testMatchSinglePresence() throws Exception
    {
        String attributeList = "bar";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(bar=*)");
        assert filter.matches(attributes);

        filter = parser.parse("(foo=*)");
        assert !filter.matches(attributes);
    }

    @Test
    public void testMultipleParenthesis() throws Exception
    {
        String attributeList = "(bar=foo)";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(((bar=*)))");
        assert filter.matches(attributes);
    }

    @Test
    public void testMatchMultipleStrings() throws Exception
    {
        String attributeList = "(x=true,2,bar)";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(x=bar)");
        assert filter.matches(attributes);
    }

    @Test
    public void testMatchMultipleLongs() throws Exception
    {
        String attributeList = "(x=1,2,3)";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(x=2)");
        assert filter.matches(attributes);
    }

    @Test
    public void testMatchMultipleBooleans() throws Exception
    {
        String attributeList = "(x=true,false)";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(x=true)");
        assert filter.matches(attributes);
    }

    @Test
    public void testMultipleMatches() throws Exception
    {
        String attributeList = "(a=1,2),(b=false),(name=name1)";
        Attributes attributes = Attributes.from(attributeList);
        FilterParser parser = new FilterParser();

        Filter filter = parser.parse("(&(a=1)(b=false))");
        assert filter.matches(attributes);

        filter = parser.parse("(&(a=1)(b=true))");
        assert !filter.matches(attributes);

        filter = parser.parse("(&(!(b=false))(a=1))");
        assert !filter.matches(attributes);

        filter = parser.parse("(&(!(b=true))(name=name*))");
        assert filter.matches(attributes);

        filter = parser.parse("(|(name=name*)(&(!(b=true))(a=2)))");
        assert filter.matches(attributes);

        filter = parser.parse("(|(name=name*)(&(!(b=true))(a=3)))");
        assert filter.matches(attributes);

        filter = parser.parse("(|(a=3)(&(!(b=true))))");
        assert filter.matches(attributes);
    }

    @Test
    public void testMatchEscapedString()
    {
        FilterParser parser = new FilterParser();

        //Test rfc2254 reserved characters
        Attributes attributes = Attributes.from("(msg=*\\28\\29\\5c)");

        assert parser
                .parse("(msg=\\2a\\28\\29\\5c)")
                .matches(attributes);

        assert parser
                .parse("(msg=*()\\")
                .matches(attributes);

        //Test rfc2608.txt reserved characters ( minus rfc2254 reserved ones )
        assert parser
                .parse("(msg=,<=>==~!)")
                .matches(Attributes.from("(msg=\\2c\\3c\\3d\\3e\\3d\\3d\\7e\\21)"));

        //Generic char
        assert parser
                .parse("(msg=ab\\63)")
                .matches(Attributes.from("(msg=abc)"));

        assert parser
                .parse("(msg=ab\\63*)")
                .matches(Attributes.from("(msg=abcd)"));

        assert parser
                .parse("(msg=\\61b\\63)")
                .matches(Attributes.from("(msg=abc)"));

        //UTF-8 char
        assert parser
                .parse("(msg=\\e1\\ba\\a3)")
                .matches(Attributes.from("(msg=\u1EA3)"));

        //Invalid UTF-8 char
        assert !parser
                .parse("(msg=abc\\FFd)")
                .matches(Attributes.from("(msg=abcd)"));

        //NUL
        assert parser
                .parse("(msg=abc\\00qwe)")
                .matches(Attributes.from("(msg=abc\\00qwe)"));

        //Numeric
        assert parser
                .parse("(msg=12\\33)")
                .matches(Attributes.from("(msg=123)"));

        //Boolean
        assert parser
                .parse("(msg=tr\\55e)")
                .matches(Attributes.from("(msg=true)"));

        //Opaque
        // That will fail !
        /*
            assert parser
                .parse("(msg=\\FF\\01\\02\\03)")
                .matches(Attributes.from("(msg=\\FF\\01\\02\\03)"));
        */

        //Broken escape sequence (will be kept as-is)
        assert parser
                .parse("(msg=\\qq)")
                .matches(Attributes.from("(msg=\\5cqq)"));
    }

    @Test
    public void testToString()
    {
        FilterParser parser = new FilterParser();

        String expression = "";
        Filter filter = parser.parse(expression);
        assert expression.equals(filter.asString());

        expression = "(a=1)";
        filter = parser.parse(expression);
        assert expression.equals(filter.asString());

        expression = "(&(a=1)(b=false))";
        filter = parser.parse(expression);
        assert expression.equals(filter.asString());

        expression = "(&(!(b=false))(a=1))";
        filter = parser.parse(expression);
        assert expression.equals(filter.asString());

        expression = "(|(name=name*)(&(!(b=true))(a=2)))";
        filter = parser.parse(expression);
        assert expression.equals(filter.asString());
    }

}
