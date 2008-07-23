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
public class AttributesTest
{
    @Test
    public void testParsing() throws Exception
    {
        Attributes attributes = Attributes.NONE;
        assert attributes.isEmpty();

        String attributeList = "(a=1,2),foo,(b=1),(separator=\\2c)";
        attributes = Attributes.from(attributeList);
        assert Arrays.equals(new Object[]{1, 2}, attributes.valueFor("a").getValues());
        assert attributes.containsTag("foo");
        assert attributes.valueFor("foo").getValue() == null;
        assert Integer.valueOf(1).equals(attributes.valueFor("b").getValue());
        assert ",".equals(attributes.valueFor("separator").getValue());

        attributeList = "bar,(a=1,2),foo,(b=1),(separator=\\2c)";
        attributes = Attributes.from(attributeList);
        assert attributes.containsTag("bar");
        assert attributes.valueFor("bar").getValue() == null;
        assert Arrays.equals(new Object[]{1, 2}, attributes.valueFor("a").getValues());
        assert attributes.containsTag("foo");
        assert attributes.valueFor("foo").getValue() == null;
        assert Integer.valueOf(1).equals(attributes.valueFor("b").getValue());
        assert ",".equals(attributes.valueFor("separator").getValue());

        attributeList = "foo, bar";
        attributes = Attributes.from(attributeList);
        assert attributes.containsTag("bar");
        assert attributes.valueFor("bar").getValue() == null;
        assert attributes.containsTag("foo");
        assert attributes.valueFor("foo").getValue() == null;

        attributeList = "foo, bar, ";
        attributes = Attributes.from(attributeList);
        assert attributes.containsTag("bar");
        assert attributes.valueFor("bar").getValue() == null;
        assert attributes.containsTag("foo");
        assert attributes.valueFor("foo").getValue() == null;

        attributeList = "foo, (a =1 ), bar, (b = true ) ";
        attributes = Attributes.from(attributeList);
        assert attributes.containsTag("foo");
        assert attributes.valueFor("foo").getValue() == null;
        assert Integer.valueOf(1).equals(attributes.valueFor("a").getValue());
        assert attributes.containsTag("bar");
        assert attributes.valueFor("bar").getValue() == null;
        assert attributes.containsTag("foo");
        assert attributes.valueFor("foo").getValue() == null;
        assert Boolean.TRUE.equals(attributes.valueFor("b").getValue());

        attributeList = "foo, (a =bar ,baz ), (b = \\FF\\00 ) ";
        attributes = Attributes.from(attributeList);
        assert attributes.containsTag("foo");
        assert attributes.valueFor("foo").getValue() == null;
        assert Arrays.equals(new Object[]{"bar", "baz"}, attributes.valueFor("a").getValues());
        assert Arrays.equals(new byte[]{(byte)0}, (byte[])attributes.valueFor("b").getValue());
    }

    @Test
    public void testAsString() throws Exception
    {
        String attributeList = "(a=1,2),foo,(b=1),(separator=\\2c),(d=string),(condition=true),(bytes=\\FF\\00\\01\\0D\\09\\30)";
        Attributes original = Attributes.from(attributeList);

        String asString = original.asString();
        Attributes copy = Attributes.from(asString);

        assert original.equals(copy);
    }

    @Test
    public void testValuesAreHomogeneus() throws Exception
    {
        String attributeList = "(a=true,\\FF\\00,string)";
        try
        {
            Attributes.from(attributeList);
            throw new AssertionError();
        }
        catch (ServiceLocationException ignored)
        {
        }
    }

    @Test
    public void testOpaqueConversion() throws Exception
    {
        try
        {
            Attributes.opaqueToBytes(null);
            throw new AssertionError();
        }
        catch (NullPointerException ignored)
        {
        }

        try
        {
            Attributes.opaqueToBytes("BLAH");
            throw new AssertionError();
        }
        catch (ServiceLocationException e)
        {
            assert e.getError() == ServiceLocationException.Error.PARSE_ERROR;
        }

        try
        {
            Attributes.opaqueToBytes("\\FF00");
            throw new AssertionError();
        }
        catch (ServiceLocationException e)
        {
            assert e.getError() == ServiceLocationException.Error.PARSE_ERROR;
        }

        try
        {
            Attributes.bytesToOpaque(null);
            throw new AssertionError();
        }
        catch (NullPointerException ignored)
        {
        }

        byte[] bytes = new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE, (byte)0x07};
        String opaqueString = "\\FF\\CA\\FE\\BA\\BE\\07";

        String bytesToOpaque = Attributes.bytesToOpaque(bytes);
        assert bytesToOpaque != null;
        assert bytesToOpaque.equalsIgnoreCase(opaqueString);

        byte[] opaqueToBytes = Attributes.opaqueToBytes(opaqueString);
        assert opaqueToBytes != null;
        assert opaqueToBytes.length == bytes.length;
        assert Arrays.equals(bytes, opaqueToBytes);
    }

    @Test
    public void testEscapeTagUnescapeTag()
    {
        String tag = "a!(),<=>\\~\t\n\r_*";
        String escapedTag = Attributes.escapeTag(tag);
        String unescapedTag = Attributes.unescapeTag(escapedTag);
        assert unescapedTag.equals(tag);
    }

    @Test
    public void testEscapeValueUnescapeValue()
    {
        String value = "a!(),<=>\\~";
        String escapedValue = Attributes.escapeValue(value);
        String unescapedValue = Attributes.unescapeValue(escapedValue);
        assert unescapedValue.equals(value);
    }

    @Test
    public void testEscapedTag()
    {
        String tag = "tag!"; // Character '!' is not allowed in tag, must be escaped
        Integer value = 1;
        Attributes attributes = Attributes.from("(" + Attributes.escapeTag(tag) + "=" + value + ")");
        assert value.equals(attributes.valueFor(tag).getValue());
    }

    @Test
    public void testEscapedValue()
    {
        String tag = "tag";
        String value = "<1>";
        Attributes attributes = Attributes.from("(" + tag + "=" + Attributes.escapeValue(value) + ")");
        assert value.equals(attributes.valueFor(tag).getValue());
    }

    @Test
    public void testSerializeDeserialize()
    {
//        Attributes attributes = Attributes.from("a=1,b=true,c=long string,d=\\FF\\CA\\FE\\BA\\BE");
    }
}
