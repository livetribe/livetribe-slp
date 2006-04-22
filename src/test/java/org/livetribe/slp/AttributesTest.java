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

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @version $Rev$ $Date$
 */
public class AttributesTest extends SLPTestCase
{
    public void testParsing() throws Exception
    {
        Attributes attributes = new Attributes(null);
        assertTrue(attributes.isEmpty());

        String attributeList = "(a=1,2),foo,(b=1),(separator=\\2c)";
        attributes = new Attributes(attributeList);
        assertTrue(Arrays.equals(new Object[]{new Long(1), new Long(2)}, attributes.getValues("a")));
        assertTrue(attributes.isTagPresent("foo"));
        assertEquals(null, attributes.getValue("foo"));
        assertEquals(new Long(1), attributes.getValue("b"));
        assertEquals(",", attributes.getValue("separator"));

        attributeList = "bar,(a=1,2),foo,(b=1),(separator=\\2c)";
        attributes = new Attributes(attributeList);
        assertTrue(attributes.isTagPresent("bar"));
        assertEquals(null, attributes.getValue("bar"));
        assertTrue(Arrays.equals(new Object[]{new Long(1), new Long(2)}, attributes.getValues("a")));
        assertTrue(attributes.isTagPresent("foo"));
        assertEquals(null, attributes.getValue("foo"));
        assertEquals(new Long(1), attributes.getValue("b"));
        assertEquals(",", attributes.getValue("separator"));

        attributeList = "foo, bar";
        attributes = new Attributes(attributeList);
        assertTrue(attributes.isTagPresent("bar"));
        assertEquals(null, attributes.getValue("bar"));
        assertTrue(attributes.isTagPresent("foo"));
        assertEquals(null, attributes.getValue("foo"));

        attributeList = "foo, bar, ";
        attributes = new Attributes(attributeList);
        assertTrue(attributes.isTagPresent("bar"));
        assertEquals(null, attributes.getValue("bar"));
        assertTrue(attributes.isTagPresent("foo"));
        assertEquals(null, attributes.getValue("foo"));

        attributeList = "foo, (a =1 ), bar, (b = true ) ";
        attributes = new Attributes(attributeList);
        assertTrue(attributes.isTagPresent("foo"));
        assertEquals(null, attributes.getValue("foo"));
        assertEquals(new Long(1), attributes.getValue("a"));
        assertTrue(attributes.isTagPresent("bar"));
        assertEquals(null, attributes.getValue("bar"));
        assertTrue(attributes.isTagPresent("foo"));
        assertEquals(null, attributes.getValue("foo"));
        assertEquals(Boolean.TRUE, attributes.getValue("b"));

        attributeList = "foo, (a =bar ,baz ), (b = \\FF\\00 ) ";
        attributes = new Attributes(attributeList);
        assertTrue(attributes.isTagPresent("foo"));
        assertEquals(null, attributes.getValue("foo"));
        assertTrue(Arrays.equals(new Object[]{"bar", "baz"}, attributes.getValues("a")));
        assertEquals("\\FF\\00", attributes.getValue("b"));
    }

    public void testAsString() throws Exception
    {
        String attributeList = "(a=1,2),foo,(b=1),(separator=\\2c),(d=string),(condition=true),(bytes=\\FF\\00\\01\\0D\\09\\30)";
        Attributes original = new Attributes(attributeList);

        String asString = original.asString();
        Attributes copy = new Attributes(asString);

        assertEquals(original, copy);
    }

    public void testPut() throws Exception
    {
        String attributeList = "(a=1,2),foo,(b=1),(separator=\\2c),(d=string),(condition=true),(bytes=\\FF\\00\\01\\0D\\09\\30)";
        Attributes original = new Attributes(attributeList);

        Attributes copy = new Attributes();
        copy.put("a", new String[]{"1","2"});
        copy.put("foo");
        copy.put("b", "1");
        copy.put("separator", ",");
        copy.put("d", "string");
        copy.put("condition", "true");
        copy.put("bytes", "\\FF\\00\\01\\0D\\09\\30");

        assertEquals(original, copy);
    }
}
