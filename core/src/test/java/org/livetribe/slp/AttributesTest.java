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
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class AttributesTest
{
    @Test
    public void testParseAttributeList()
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
    public void testParseBadAttributeList()
    {
        // Character '*' is not allowed in tags of an attribute list
        String attributeList = "(foo*=1)";
        try
        {
            Attributes.from(attributeList);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getSLPError() == SLPError.PARSE_ERROR;
        }

        // Character '_' is not allowed in tags of an attribute list
        attributeList = "(foo_bar=1)";
        try
        {
            Attributes.from(attributeList);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getSLPError() == SLPError.PARSE_ERROR;
        }

        // Character '!' is not allowed in values of an attribute list
        attributeList = "(separator=!)";
        try
        {
            Attributes.from(attributeList);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getSLPError() == SLPError.PARSE_ERROR;
        }
    }

    @Test
    public void testParseTagList()
    {
        // Character '*' is allowed in tag list
        String tagList = "foo*,bar";
        Attributes attributes = Attributes.fromTags(tagList);
        assert !attributes.containsTag("foo");
        assert attributes.containsTag("bar");

        tagList = "re\\2at"; // Hex 2a stands for character '*'
        attributes = Attributes.fromTags(tagList);
        assert attributes.containsTag("re*t");
    }

    @Test
    public void testParseBadTagList()
    {
        // Not a tag list
        String tagList = "(foo=1)";
        try
        {
            Attributes.fromTags(tagList);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getSLPError() == SLPError.PARSE_ERROR;
        }

        // Not a tag list
        tagList = "foo,(bar=1)";
        try
        {
            Attributes.fromTags(tagList);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getSLPError() == SLPError.PARSE_ERROR;
        }

        // Tag list with reserved character
        tagList = "foo, b~r";
        try
        {
            Attributes.fromTags(tagList);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getSLPError() == SLPError.PARSE_ERROR;
        }
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
            assert e.getSLPError() == SLPError.PARSE_ERROR;
        }

        try
        {
            Attributes.opaqueToBytes("\\FF00");
            throw new AssertionError();
        }
        catch (ServiceLocationException e)
        {
            assert e.getSLPError() == SLPError.PARSE_ERROR;
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
    public void testTagWithReservedChar()
    {
        String tag = "tag!"; // Character '!' is not allowed in tag, must be escaped
        Integer value = 1;
        Attributes attributes = Attributes.from("(" + Attributes.escapeTag(tag) + "=" + value + ")");
        assert value.equals(attributes.valueFor(tag).getValue());
    }

    @Test
    public void testValueWithReservedChar()
    {
        String tag = "tag";
        String value = "<1>"; // Characters '<' and '>' are reserved in values
        Attributes attributes = Attributes.from("(" + tag + "=" + Attributes.escapeValue(value) + ")");
        assert value.equals(attributes.valueFor(tag).getValue());
    }

    @Test
    public void testNegativeIntegerValue()
    {
        Attributes attributes = Attributes.from("(a=-1,-2)");
        assert attributes.containsTag("a");
        Attributes.Value value = attributes.valueFor("a");
        assert value.isMultiValued();
        assert value.isIntegerType();
        assert Arrays.equals(new Object[]{-1, -2}, value.getValues());

        attributes = Attributes.from("(a=1,-2)");
        assert attributes.containsTag("a");
        value = attributes.valueFor("a");
        assert value.isMultiValued();
        assert value.isIntegerType();
        assert Arrays.equals(new Object[]{1, -2}, value.getValues());
    }

    @Test
    public void testUnion()
    {
        Attributes attributes1 = Attributes.from("(a=1)");
        Attributes attributes2 = Attributes.from("(b=true)");
        Attributes result = attributes1.union(attributes2);
        assert attributes1.getSize() == 1;
        assert attributes1.containsTag("a");
        assert attributes2.getSize() == 1;
        assert attributes2.containsTag("b");
        assert result.getSize() == 2;
        assert result.containsTag("a");
        assert result.containsTag("b");
        assert Integer.valueOf(1).equals(result.valueFor("a").getValue());
        assert Boolean.TRUE.equals(result.valueFor("b").getValue());
    }

    @Test
    public void testIntersect()
    {
        Attributes attributes1 = Attributes.from("(foo=1),(fxo=2),fo,(bar=2)");
        Attributes attributes2 = Attributes.fromTags("f*o");
        Attributes result = attributes1.intersect(attributes2);
        assert attributes1.getSize() == 4;
        assert attributes2.getSize() == 1;
        assert result.getSize() == 3;
        assert !result.containsTag("bar");

        attributes1 = Attributes.from("(a=1),(aa=11),(b=2),(c=3)");
        attributes2 = Attributes.fromTags("a*,c");
        result = attributes1.intersect(attributes2);
        assert attributes1.getSize() == 4;
        assert attributes2.getSize() == 2;
        assert result.getSize() == 3;
        assert !result.containsTag("b");
    }

    @Test
    public void testComplement()
    {
        Attributes attributes1 = Attributes.from("(a=1),(b=1,2),(c=3),cad,(cod=5)");
        Attributes attributes2 = Attributes.fromTags("a");
        Attributes result = attributes1.complement(attributes2);
        assert attributes1.getSize() == 5;
        assert attributes2.getSize() == 1;
        assert result.getSize() == 4;
        assert !result.containsTag("a");

        attributes2 = Attributes.fromTags("cad");
        result = attributes1.complement(attributes2);
        assert attributes1.getSize() == 5;
        assert attributes2.getSize() == 1;
        assert result.getSize() == 4;
        assert !result.containsTag("cad");

        attributes2 = Attributes.fromTags("*a");
        result = attributes1.complement(attributes2);
        assert attributes1.getSize() == 5;
        assert attributes2.getSize() == 1;
        assert result.getSize() == 4;
        assert !result.containsTag("a");

        attributes2 = Attributes.fromTags("*a*");
        result = attributes1.complement(attributes2);
        assert attributes1.getSize() == 5;
        assert attributes2.getSize() == 1;
        assert result.getSize() == 3;
        assert !result.containsTag("a");
        assert !result.containsTag("cad");

        attributes2 = Attributes.fromTags("c*");
        result = attributes1.complement(attributes2);
        assert attributes1.getSize() == 5;
        assert attributes2.getSize() == 1;
        assert result.getSize() == 2;
        assert !result.containsTag("c");
        assert !result.containsTag("cad");
        assert !result.containsTag("cod");

        attributes2 = Attributes.fromTags("c*d");
        result = attributes1.complement(attributes2);
        assert attributes1.getSize() == 5;
        assert attributes2.getSize() == 1;
        assert result.getSize() == 3;
        assert !result.containsTag("cad");
        assert !result.containsTag("cod");

        attributes2 = Attributes.fromTags("*");
        result = attributes1.complement(attributes2);
        assert attributes1.getSize() == 5;
        assert attributes2.getSize() == 1;
        assert result.getSize() == 0;
    }

    @Test
    public void testMerge()
    {
        Attributes attributes1 = Attributes.from("(a=1),  (b=true), (c=string),(d=\\FF\\00), e,   (f=true),(g=true),(h=123456),i");
        Attributes attributes2 = Attributes.from("(a=1,2),(b=false),(c=strong),(d=\\FF\\FF),(e=1), f,      (g=1234),(h=string),j");
        Attributes merged = attributes1.merge(attributes2);

        Attributes.Value aValue = merged.valueFor("a");
        assert aValue.isIntegerType();
        assert aValue.isMultiValued();
        assert Arrays.equals(new Object[]{1, 1, 2}, aValue.getValues());

        Attributes.Value bValue = merged.valueFor("b");
        assert bValue.isBooleanType();
        assert bValue.isMultiValued();
        assert Arrays.equals(new Object[]{true, false}, bValue.getValues());

        Attributes.Value cValue = merged.valueFor("c");
        assert cValue.isStringType();
        assert cValue.isMultiValued();
        assert Arrays.equals(new Object[]{"string", "strong"}, cValue.getValues());

        // No merge for opaque values
        Attributes.Value dValue = merged.valueFor("d");
        assert dValue.equals(attributes2.valueFor("d"));

        // Presence values merge is particular
        Attributes.Value eValue = merged.valueFor("e");
        assert eValue.equals(attributes2.valueFor("e"));
        Attributes.Value fValue = merged.valueFor("f");
        assert fValue.equals(attributes1.valueFor("f"));

        // Merge of boolean and integer yields string
        Attributes.Value gValue = merged.valueFor("g");
        assert gValue.isStringType();
        assert gValue.isMultiValued();
        assert Arrays.equals(new Object[]{"true", "1234"}, gValue.getValues());

        // Merge of integer and string yields string
        Attributes.Value hValue = merged.valueFor("h");
        assert hValue.isStringType();
        assert hValue.isMultiValued();
        assert Arrays.equals(new Object[]{"123456", "string"}, hValue.getValues());

        assert merged.containsTag("i");

        assert merged.containsTag("j");
    }

    @Test
    public void testIteration() throws Exception
    {
        Attributes attributes = Attributes.from("(a=1,2),  (b=true), (file\\5fpath=My Documents),(d=\\FF\\00), e");
        Set<String> tags1 = new HashSet<String>();
        tags1.add("a");
        tags1.add("b");
        tags1.add("file_path");
        tags1.add("d");
        tags1.add("e");
        Set<String> tags2 = new HashSet<String>();
        for (String tag : attributes)
            tags2.add(tag);
        Assert.assertEquals(tags1, tags2);

        for (String tag : attributes)
        {
            Attributes.Value value = attributes.valueFor(tag);
            if ("a".equals(tag))
            {
                Assert.assertTrue(value.isIntegerType());
                Assert.assertTrue(value.isMultiValued());
            }
            else if ("b".equals(tag))
            {
                Assert.assertTrue(value.isBooleanType());
                Assert.assertSame(Boolean.TRUE, value.getValue());
            }
            else if ("file_path".equals(tag))
            {
                Assert.assertTrue(value.isStringType());
                Assert.assertEquals("My Documents", value.getValue());
            }
            else if ("d".equals(tag))
            {
                Assert.assertTrue(value.isOpaqueType());
            }
            else if ("e".equals(tag))
            {
                Assert.assertTrue(value.isPresenceType());
            }
            else
            {
                Assert.fail();
            }
        }
    }
}
