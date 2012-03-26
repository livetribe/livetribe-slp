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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Attributes are a comma separated list of key-value pairs that describe a service.
 * <br />
 * The attribute key is called <b>tag</b>, and the attribute value can be non-valued (the tag as no value),
 * single-valued (the tag as only one value) or multi-valued (the tag has more values).
 * Depending on the value(s) of the tag, the attribute has a <b>type</b>; there are four defined types:
 * <ul>
 * <li>boolean, representing boolean values</li>
 * <li>integer, representing natural numbers</li>
 * <li>string, representing strings</li>
 * <li>opaque, representing an array of bytes</li>
 * </ul>
 * Attribute values must be homogeneous: the attribute <code>(a=1,true,hello)</code> is illegal, because it's not
 * clear if the type is integer, boolean, or string.
 * <br />
 * Attributes can be used by UserAgents during service lookup to select appropriate services that match required
 * conditions.
 * <br />
 * Attributes can be described using a string representation; a valued or multi-valued attribute must be enclosed in
 * parenthesis, while for non-valued attributes (also called <em>presence attributes</em>) the parenthesis must be
 * omitted, for example:
 * <pre>
 * String attributesString = "(a=1,2),(b=true),(bytes=\FF\CA\FE\BA\BE),present,(description=Something Interesting)";
 * Attributes attributes = Attributes.from(attributesString);
 * </pre>
 * The example defines 5 attributes:
 * <ul>
 * <li>the first attribute has tag "a", values "1" and "2" and type "integer";</li>
 * <li>the second attribute has tag "b", value "true" and type "boolean";</li>
 * <li>the third attribute has tag "bytes", value is an array of four bytes (0xCA, 0xFE, 0xBA, 0xBE) and type "opaque";</li>
 * <li>the fourth attribute has tag "present", no value, and type "presence";</li>
 * <li>the fifth attribute has tag "description", value "Something Interesting" and type "string".</li>
 * </ul>
 *
 * @version $Rev$ $Date$
 */
public class Attributes implements Iterable<String>
{
    private static final char STAR = '*';
    private static final char ESCAPE = '\\';
    private static final String OPAQUE_PREFIX = ESCAPE + "FF";

    public static final Attributes NONE = new Attributes();

    /**
     * Creates an <code>Attributes</code> object parsing the given escaped attributes string.
     * An escaped attributes string is an attribute list as defined in RFC 2608, 5.0, where
     * tags that contain reserved characters are escaped, where string values that contain
     * reserved characters are escaped and where bytes are converted to an opaque (escaped) string.
     *
     * @param escapedAttributesString The string containing the attributes to parse
     * @return a new Attributes instance obtained parsing the given string
     * @throws ServiceLocationException If the parsing fails
     * @see #escapeTag(String)
     * @see #escapeValue(String)
     * @see #bytesToOpaque(byte[])
     */
    public static Attributes from(String escapedAttributesString)
    {
        if (escapedAttributesString == null || escapedAttributesString.trim().length() == 0) return NONE;
        return parseAttributeList(escapedAttributesString);
    }

    /**
     * Creates an <code>Attributes</code> object converting the given string map.
     * The keys and values are not escaped.
     *
     * @param stringMap The string map containing the attributes convert
     * @return a new Attributes instance obtained converting the given string map
     * @throws ServiceLocationException If the conversion fails
     */
    public static Attributes from(Map<String, String> stringMap)
    {
        if (stringMap == null || stringMap.size() == 0) return NONE;

        Attributes result = new Attributes();

        for (Map.Entry<String, String> entry : stringMap.entrySet())
        {
            result.attributes.put(Tag.from(entry.getKey(), false), Value.from(entry.getValue()));
        }

        return result;
    }

    /**
     * Creates an <code>Attributes</code> object parsing the given escaped tags string.
     * An escaped tags string is a tag list as defined in RFC 2608, 9.4, where
     * tags that contains reserved characters are escaped but are allowed to contain the
     * star character '*' that performs character globbing.
     *
     * @param escapedTagsString the string containing the tag list to parse
     * @return a new Attributes instance obtained parsing the given string
     * @throws ServiceLocationException If the parsing fails
     */
    public static Attributes fromTags(String escapedTagsString)
    {
        if (escapedTagsString == null || escapedTagsString.trim().length() == 0) return NONE;
        return parseTagList(escapedTagsString);
    }

    /**
     * Maps the tag to the corrispondent value
     */
    private final Map<Tag, Value> attributes = new HashMap<Tag, Value>();

    private Attributes()
    {
    }

    private Attributes(Attributes copy)
    {
        attributes.putAll(copy.attributes);
    }

    /**
     * @param unescapedTag the unescaped tag
     * @return the <code>Value</code> object for the given unescaped tag.
     * @see #escapeTag(String)
     */
    public Value valueFor(String unescapedTag)
    {
        Value value = attributes.get(Tag.from(escapeTag(unescapedTag), false));
        return value == null ? Value.NULL_VALUE : value;
    }

    /**
     * @return true if this <code>Attributes</code> object is empty.
     */
    public boolean isEmpty()
    {
        return attributes.isEmpty();
    }

    /**
     * @return the number of tags in this <code>Attributes</code> object.
     */
    public int getSize()
    {
        return attributes.size();
    }

    /**
     * @param unescapedTag the unescaped tag
     * @return true if the given unescaped tag is present in this <code>Attributes</code> object.
     * @see Value#isPresenceType()
     */
    public boolean containsTag(String unescapedTag)
    {
        return attributes.containsKey(Tag.from(escapeTag(unescapedTag), false));
    }

    public Iterator<String> iterator()
    {
        return new Iterator<String>()
        {
            private final Iterator<Tag> i = attributes.keySet().iterator();

            public boolean hasNext()
            {
                return i.hasNext();
            }

            public String next()
            {
                Tag tag = i.next();
                return unescapeTag(tag.tag);
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns a byte array containing the bytes parsed from the given opaque string,
     * except the initial opaque prefix \FF.
     * <br />
     * For example the string <code>\FF\CA\FE</code> will be converted into the byte array
     * <code>[0xCA, 0xFE]</code>.
     *
     * @param opaqueString The opaque string containing the bytes to parse
     * @return the byte array obtained from parsing the given string
     * @throws ServiceLocationException If the parsing fails
     * @see #bytesToOpaque(byte[])
     */
    public static byte[] opaqueToBytes(String opaqueString)
    {
        if (!opaqueString.startsWith(OPAQUE_PREFIX))
            throw new ServiceLocationException("Opaque strings must begin with " + OPAQUE_PREFIX, SLPError.PARSE_ERROR);
        if (opaqueString.length() % 3 != 0)
            throw new ServiceLocationException("Opaque strings must be of the form: [\\<HEX><HEX>]+", SLPError.PARSE_ERROR);

        byte[] result = new byte[(opaqueString.length() - OPAQUE_PREFIX.length()) / 3];
        int position = 0;
        int index = OPAQUE_PREFIX.length();
        while (index < opaqueString.length())
        {
            if (opaqueString.charAt(index) != ESCAPE)
                throw new ServiceLocationException("Invalid escape sequence at index " + index + " of " + opaqueString, SLPError.PARSE_ERROR);
            ++index;
            String hexString = opaqueString.substring(index, index + 2);
            result[position] = (byte)(Integer.parseInt(hexString, 16) & 0xFF);
            ++position;
            index += 2;
        }
        return result;
    }

    /**
     * Returns an opaque string containing the escaped sequence of the given bytes,
     * including the initial opaque prefix \FF.
     * <br />
     * For example the byte array <code>[0xCA, 0xFE]</code> will be converted into the string
     * <code>\FF\CA\FE</code>.
     *
     * @param bytes The bytes to escape into an opaque string
     * @return the opaque string obtained escaping the given bytes
     * @see #opaqueToBytes(String)
     */
    public static String bytesToOpaque(byte[] bytes)
    {
        StringBuilder result = new StringBuilder();
        result.append(OPAQUE_PREFIX);
        for (byte aByte : bytes)
        {
            result.append(ESCAPE);
            int code = aByte & 0xFF;
            if (code < 16) result.append("0");
            result.append(Integer.toHexString(code).toUpperCase());
        }
        return result.toString();
    }

    /**
     * Escapes the given tag string following RFC 2608, 5.0.
     * <br />
     * For example, the tag string <code>file_path</code> will be converted into the string
     * <code>file\5fpath</code>, since the character '_' is reserved in tags.
     *
     * @param unescapedTagString the tag string to escape
     * @return the escaped tag string
     * @see #unescapeTag(String)
     */
    public static String escapeTag(String unescapedTagString)
    {
        return escape(unescapedTagString, Tag.reservedChars);
    }

    /**
     * Unescapes the given escaped tag following RFC 2608, 5.0.
     * For example, the escaped tag string <code>file\5fpath</code> will be converted into
     * <code>file_path</code>.
     *
     * @param escapedTag the tag string to unescape
     * @return the unescaped tag
     * @throws ServiceLocationException if the escaping is wrong
     * @see #escapeTag(String)
     */
    public static String unescapeTag(String escapedTag)
    {
        // Check that the escaped tag does not contain reserved characters
        checkEscaped(escapedTag, Tag.reservedChars, false);
        // Unescape the tag
        return unescape(escapedTag, Tag.reservedChars);
    }

    private static String escape(String string, char[] reserved)
    {
        string = string.trim();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < string.length(); ++i)
        {
            char c = string.charAt(i);
            if (c < reserved.length && reserved[c] == c)
            {
                result.append(ESCAPE);
                int code = c & 0xFF;
                if (code < 16) result.append("0");
                result.append(Integer.toHexString(code));
            }
            else
            {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String unescape(String value, char[] reserved)
    {
        value = value.trim();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);
            if (c == ESCAPE)
            {
                String codeString = value.substring(i + 1, i + 3);
                int code = Integer.parseInt(codeString, 16);
                if (code < reserved.length && reserved[code] == code)
                {
                    builder.append(reserved[code]);
                    i += 2;
                }
                else
                {
                    throw new ServiceLocationException("Unknown escaped character " + ESCAPE + codeString + " at position " + (i + 1) + " of " + value, SLPError.PARSE_ERROR);
                }
            }
            else
            {
                builder.append(c);
            }
        }
        return builder.toString().trim();
    }

    private static void checkEscaped(String string, char[] reserved, boolean allowStar) throws ServiceLocationException
    {
        if (string.trim().length() == 0)
            throw new ServiceLocationException("Escaped string could not be the empty string", SLPError.PARSE_ERROR);
        for (int i = 0; i < string.length(); ++i)
        {
            char ch = string.charAt(i);
            // The backslash is a reserved character, but is present in escaped strings, skip it
            if (ch == ESCAPE) continue;
            // Allow globbing in tags
            if (ch == STAR && allowStar) continue;
            if (ch < reserved.length && reserved[ch] == ch)
                throw new ServiceLocationException("Illegal character '" + ch + "' in " + string, SLPError.PARSE_ERROR);
        }
    }

    /**
     * Escapes the given unescaped value string following RFC 2608, 5.0.
     * <br />
     * For example, the string value <code>&lt;A&gt;</code> will be converted into the string
     * <code>\3cA\3e</code>, since the characters '<' and '>' are reserved in values.
     *
     * @param unescapedValue the value string to escape
     * @return the escaped value string
     * @see #unescapeValue(String)
     */
    public static String escapeValue(String unescapedValue)
    {
        return escape(unescapedValue, Value.reservedChars);
    }

    /**
     * Unescapes the given escaped value string following RFC 2608, 5.0.
     * <br />
     * For example, the string value <code>\3cA\3e</code> will be converted into the string
     * <code>&lt;A&gt;</code>.
     *
     * @param escapedValue the value string to unescape
     * @return the unescaped value string
     * @see #escapeValue(String)
     */
    public static String unescapeValue(String escapedValue)
    {
        // Check that the escaped value does not contain reserved characters
        checkEscaped(escapedValue, Value.reservedChars, false);
        // Unescape the value
        return unescape(escapedValue, Value.reservedChars);
    }

    private static Attributes parseAttributeList(String escapedAttributeList) throws ServiceLocationException
    {
        Attributes result = new Attributes();

        StringBuilder tagList = new StringBuilder();
        int start = 0;
        while (start < escapedAttributeList.length())
        {
            int open = escapedAttributeList.indexOf('(', start);
            if (open < 0)
            {
                String remaining = escapedAttributeList.substring(start);
                tagList.append(remaining);
                start += remaining.length();
            }
            else
            {
                int close = escapedAttributeList.indexOf(')', open);
                if (close < 0)
                    throw new ServiceLocationException("Missing ')' in attribute list " + escapedAttributeList, SLPError.PARSE_ERROR);
                tagList.append(escapedAttributeList.substring(start, open));
                String attributeString = escapedAttributeList.substring(open, close + 1);
                Attribute attribute = parseAttribute(attributeString, escapedAttributeList);
                if (attribute != null) result.attributes.put(attribute.tag, attribute.value);
                start = close + 1;
            }
        }

        // Only tags, no pairs
        parseTagList(result, tagList.toString(), false);

        return result;
    }

    private static Attribute parseAttribute(String attribute, String attributeList) throws ServiceLocationException
    {
        attribute = attribute.trim();
        if (attribute.length() == 0) return null;
        if (!attribute.startsWith("("))
            throw new ServiceLocationException("Could not parse attributes " + attributeList + ", missing parenthesis around attribute " + attribute, SLPError.PARSE_ERROR);

        int closeParenthesis = attribute.indexOf(')');
        String pair = attribute.substring(1, closeParenthesis);

        int equals = pair.indexOf('=');
        if (equals < 0)
            throw new ServiceLocationException("Could not parse attributes " + attributeList + ", missing '=' in " + attribute, SLPError.PARSE_ERROR);

        String escapedTag = pair.substring(0, equals).trim();
        Tag tag = Tag.from(escapedTag, false);

        String valueString = pair.substring(equals + 1).trim();
        Value value = Value.from(valueString);
        return new Attribute(tag, value);
    }

    private static Attributes parseTagList(String escapedTagList) throws ServiceLocationException
    {
        Attributes result = new Attributes();
        parseTagList(result, escapedTagList, true);
        return result;
    }

    private static void parseTagList(Attributes attributes, String tagList, boolean allowGlobbing)
    {
        String[] tags = tagList.split(",");
        for (String tag : tags)
        {
            tag = tag.trim();
            if (tag.length() > 0) attributes.attributes.put(Tag.from(tag, allowGlobbing), Value.from(null));
        }
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final Attributes that = (Attributes)obj;
        // Could not compare the attributes Map, since it contains String[] that will make the comparison fail,
        // since native arrays do not override equals: they are different even if they contain the same objects
        return asString().equals(that.asString());
    }

    public int hashCode()
    {
        return asString().hashCode();
    }

    /**
     * Returns a string representation of this <code>Attributes</code> object, that can be passed to
     * {@link #from(String)} to be parsed.
     *
     * @return the escaped attribute list string
     */
    public String asString()
    {
        TreeMap<Tag, Value> orderedAttributes = new TreeMap<Tag, Value>(attributes);
        StringBuilder result = new StringBuilder();
        for (Iterator<Map.Entry<Tag, Value>> entries = orderedAttributes.entrySet().iterator(); entries.hasNext();)
        {
            Map.Entry<Tag, Value> mapEntry = entries.next();
            Tag tag = mapEntry.getKey();
            Value value = mapEntry.getValue();
            if (value.isPresenceType())
            {
                result.append(tag.tag);
            }
            else
            {
                result.append("(").append(tag.tag).append("=");
                if (value.multiValued)
                {
                    Object[] values = value.getValues();
                    for (int i = 0; i < values.length; ++i)
                    {
                        if (i > 0) result.append(",");
                        if (value.isOpaqueType())
                            result.append(bytesToOpaque((byte[])values[i]));
                        else
                            result.append(escapeValue(String.valueOf(values[i])));
                    }
                }
                else
                {
                    if (value.isOpaqueType())
                        result.append(bytesToOpaque((byte[])value.getValue()));
                    else
                        result.append(escapeValue(String.valueOf(value.getValue())));
                }
                result.append(")");
            }
            if (entries.hasNext()) result.append(",");
        }
        return result.toString();
    }

    /**
     * Returns a string representation of the tags of this <code>Attributes</code> object,
     * that can be passed to {@link #fromTags(String)} to be parsed.
     *
     * @return the escaped tags list string
     */
    public String asTagsString()
    {
        TreeMap<Tag, Value> orderedAttributes = new TreeMap<Tag, Value>(attributes);
        StringBuilder result = new StringBuilder();
        for (Iterator<Tag> tags = orderedAttributes.keySet().iterator(); tags.hasNext();)
        {
            Tag tag = tags.next();
            result.append(tag.tag);
            if (tags.hasNext()) result.append(",");
        }
        return result.toString();
    }

    /**
     * @see #asString()
     */
    public String toString()
    {
        return asString();
    }

    /**
     * Adds the attributes of the given <code>Attributes</code> object to this <code>Attributes</code> object,
     * replacing duplicates, returning a new <code>Attributes</code> object.
     * If the given <code>Attributes</code> is null, a clone of this <code>Attributes</code> object will be returned.
     * If this <code>Attributes</code> contains a tag that exists in the given <code>Attributes</code>, the merged
     * <code>Attributes</code> will contain the value from the given <code>Attributes</code> object (overwriting the one
     * from this <code>Attributes</code> object).
     *
     * @param that The <code>Attributes</code> to unite with
     * @return A new <code>Attributes</code> object containing the union of the attributes.
     * @see #complement(Attributes)
     * @see #intersect(Attributes)
     */
    public Attributes union(Attributes that)
    {
        Attributes result = new Attributes(this);
        if (that != null) result.attributes.putAll(that.attributes);
        return result;
    }

    /**
     * Removes the attributes of this <code>Attributes</code> object that are present in the given
     * <code>Attributes</code> object, returning a new <code>Attributes</code> object.
     * The resulting <code>Attributes</code> will contain only the attributes whose tags are present in this
     * <code>Attributes</code> object but not in the given <code>Attributes</code> object.
     * If a tag in the given <code>Attributes</code> object contains the globbing character '*', all attributes in this
     * <code>Attributes</code> object that match will be removed.
     * If the given <code>Attributes</code> is null, a clone of this <code>Attributes</code> object will be returned.
     *
     * @param that The <code>Attributes</code> to complement with
     * @return A new <code>Attributes</code> object containing the complement of the attributes.
     * @see #union(Attributes)
     * @see #intersect(Attributes)
     */
    public Attributes complement(Attributes that)
    {
        Attributes result = new Attributes(this);
        if (that != null)
        {
            for (Tag tagToRemove : that.attributes.keySet())
            {
                for (Iterator<Tag> tags = result.attributes.keySet().iterator(); tags.hasNext();)
                {
                    Tag tag = tags.next();
                    if (tagToRemove.matches(tag)) tags.remove();
                }
            }
        }
        return result;
    }

    /**
     * Retains the attributes of this <code>Attributes</code> object that are present in the given
     * <code>Attributes</code> object, returning a new <code>Attributes</code> object.
     * The resulting <code>Attributes</code> will contain only the attributes whose tags are present in this
     * <code>Attributes</code> object and in the given <code>Attributes</code> object.
     * If a tag in the given <code>Attributes</code> object contains the globbing character '*', all attributes in this
     * <code>Attributes</code> object that match will be retained.
     * If the given <code>Attributes</code> is null or empty, {@link Attributes#NONE} will be returned.
     *
     * @param that The <code>Attributes</code> to intersect with
     * @return A new <code>Attributes</code> object containing the intersection of the attributes.
     * @see #union(Attributes)
     * @see #complement(Attributes)
     */
    public Attributes intersect(Attributes that)
    {
        if (that == null || that.isEmpty()) return NONE;

        Attributes result = new Attributes();

        for (Tag tagToRetain : that.attributes.keySet())
        {
            for (Iterator<Tag> tags = attributes.keySet().iterator(); tags.hasNext();)
            {
                Tag tag = tags.next();
                if (tagToRetain.matches(tag))
                    result.attributes.put(tag, attributes.get(tag));
            }
        }
        return result;
    }

    public Attributes merge(Attributes that)
    {
        Attributes result = union(that);
        Attributes intersection = intersect(that);
        for (Tag tag : intersection.attributes.keySet())
        {
            Value thisValue = attributes.get(tag);
            Value thatValue = that.attributes.get(tag);
            Value mergedValue = thisValue.merge(thatValue);
            result.attributes.put(tag, mergedValue);
        }
        return result;
    }

    private static class Tag implements Comparable<Tag>
    {
        private static final char[] reservedChars = new char[128];

        static
        {
            reservedChars['\t'] = '\t';
            reservedChars['\n'] = '\n';
            reservedChars['\r'] = '\r';
            reservedChars['!'] = '!';
            reservedChars['('] = '(';
            reservedChars[')'] = ')';
            reservedChars['*'] = '*';
            reservedChars[','] = ',';
            reservedChars['<'] = '<';
            reservedChars['='] = '=';
            reservedChars['>'] = '>';
            reservedChars['\\'] = '\\';
            reservedChars['_'] = '_';
            reservedChars['~'] = '~';
        }

        public static Tag from(String escapedTagString, boolean allowGlobbing)
        {
            checkEscaped(escapedTagString, reservedChars, allowGlobbing);
            return new Tag(escapedTagString);
        }

        private final String tag;

        private Tag(String tag)
        {
            this.tag = tag;
        }

        private boolean matches(Tag that)
        {
            // Escape regex characters that are not tag reserved characters
            String regex = tag;
            regex = regex.replace(".", "\\.");
            regex = regex.replace("[", "\\[");
            regex = regex.replace("]", "\\]");
            regex = regex.replace("^", "\\^");
            regex = regex.replace("$", "\\$");
            regex = regex.replace("+", "\\+");
            // Replace globbing '*' character with regexp's '.*'
            regex = regex.replace("*", ".*");
            return that.tag.matches(regex);
        }

        public int compareTo(Tag that)
        {
            return tag.compareTo(that.tag);
        }

        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Tag that = (Tag)obj;
            return tag.equals(that.tag);
        }

        public int hashCode()
        {
            return tag.hashCode();
        }
    }

    /**
     * Represent the attribute value within the {@link Attributes} class.
     * A <code>Value</code> encapsulates the attribute value(s) and type.
     * The values are unescaped, so for example opaque strings are stored as byte[].
     */
    public static class Value
    {
        private static final char[] reservedChars = new char[128];

        static
        {
            reservedChars['!'] = '!';
            reservedChars['('] = '(';
            reservedChars[')'] = ')';
            reservedChars[','] = ',';
            reservedChars['<'] = '<';
            reservedChars['='] = '=';
            reservedChars['>'] = '>';
            reservedChars['\\'] = '\\';
            reservedChars['~'] = '~';
        }

        private static final Value NULL_VALUE = new Value(null, 0);

        private static final int STRING = 1;
        private static final int INTEGER = 2;
        private static final int BOOLEAN = 3;
        private static final int OPAQUE = 4;
        private static final int PRESENCE = 5;

        private final Object value;
        private final int type;
        private final boolean multiValued;

        private static Value from(String valueString)
        {
            if (valueString == null) return new Value(valueString, PRESENCE);
            if (valueString.indexOf(',') >= 0)
                return fromStringArray(valueString.split(","));
            else
                return fromString(valueString);
        }

        private static Value fromStringArray(String[] strings)
        {
            Value[] attributeValues = new Value[strings.length];
            boolean homogeneous = true;
            boolean opaquePresent = false;
            for (int i = 0; i < attributeValues.length; ++i)
            {
                attributeValues[i] = fromString(strings[i]);
                homogeneous &= attributeValues[0].getType() == attributeValues[i].getType();
                if (attributeValues[i].isOpaqueType()) opaquePresent = true;
            }

            if (homogeneous)
            {
                Object[] values = new Object[attributeValues.length];
                for (int i = 0; i < attributeValues.length; ++i)
                {
                    Value value = attributeValues[i];
                    values[i] = value.getValue();
                }
                return new Value(values, attributeValues[0].getType());
            }
            else
            {
                // It's not homogeneous, and there is one opaque value: RFC 2608, 5.0 says it's illegal.
                if (opaquePresent)
                    throw new ServiceLocationException("Attribute values must be homogeneous: considering values to be strings, but one value is opaque: " + Arrays.asList(attributeValues), SLPError.PARSE_ERROR);

                // Not homogeneus, convert everything to string
                Object[] values = new Object[attributeValues.length];
                for (int i = 0; i < attributeValues.length; ++i)
                {
                    Value value = attributeValues[i];
                    values[i] = String.valueOf(value.getValue());
                }
                return new Value(values, Value.STRING);
            }
        }

        private static Value fromString(String value)
        {
            // Is it opaque ?
            if (isOpaque(value)) return new Value(opaqueToBytes(value), Value.OPAQUE);

            // Is it a number ?
            if (isInteger(value)) return new Value(Integer.valueOf(value), Value.INTEGER);

            // Is it a boolean ?
            if (isBoolean(value)) return new Value(Boolean.valueOf(value), Value.BOOLEAN);

            // Then it's a string
            return new Value(unescapeValue(value), Value.STRING);
        }

        private static boolean isBoolean(String value)
        {
            return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
        }

        private static boolean isInteger(String value)
        {
            return Pattern.matches("-?[0-9]+", value);
        }

        private static boolean isOpaque(String value)
        {
            return value.startsWith(OPAQUE_PREFIX);
        }

        private Value(Object value, int type)
        {
            this.value = value;
            this.type = type;
            this.multiValued = value instanceof Object[];
        }

        private int getType()
        {
            return type;
        }

        /**
         * @return whether this value is multi valued.
         * @see #getValues()
         */
        public boolean isMultiValued()
        {
            return multiValued;
        }

        /**
         * @return whether this value is of type string.
         */
        public boolean isStringType()
        {
            return type == STRING;
        }

        /**
         * @return whether this value is of type integer.
         */
        public boolean isIntegerType()
        {
            return type == INTEGER;
        }

        /**
         * @return whether this value is of type boolean.
         */
        public boolean isBooleanType()
        {
            return type == BOOLEAN;
        }

        /**
         * @return whether this value is of type opaque.
         */
        public boolean isOpaqueType()
        {
            return type == OPAQUE;
        }

        /**
         * @return whether this value represent only the presence of a tag with no value.
         */
        public boolean isPresenceType()
        {
            return type == PRESENCE;
        }

        /**
         * @return the value of this <code>Value</code> object (in case it is single valued), or the first value of this entry
         *         (in case it is multivalued).
         */
        public Object getValue()
        {
            if (isPresenceType()) return null;
            if (multiValued) return ((Object[])value)[0];
            return value;
        }

        /**
         * @return the values of this entry (in case it is multivalued), or the value of this entry, wrapped in an array
         *         of length 1 (in case it is single valued).
         */
        public Object[] getValues()
        {
            if (isPresenceType()) return null;
            if (multiValued) return (Object[])value;
            return new Object[]{value};
        }

        private Value merge(Value that)
        {
            if (isPresenceType())
            {
                if (that.isPresenceType())
                    return new Value(null, PRESENCE);
                else
                    return new Value(that.value, that.type);
            }
            else if (isBooleanType())
            {
                if (that.isPresenceType() || that.isOpaqueType())
                    return new Value(value, type);
                else if (that.isBooleanType())
                    return new Value(coalesceArrays(getValues(), that.getValues(), false), BOOLEAN);
                else
                    return new Value(coalesceArrays(getValues(), that.getValues(), true), STRING);
            }
            else if (isIntegerType())
            {
                if (that.isPresenceType() || that.isOpaqueType())
                    return new Value(value, type);
                else if (that.isIntegerType())
                    return new Value(coalesceArrays(getValues(), that.getValues(), false), INTEGER);
                else
                    return new Value(coalesceArrays(getValues(), that.getValues(), true), STRING);
            }
            else if (isOpaqueType())
            {
                if (that.isPresenceType())
                    return new Value(value, type);
                else
                    return new Value(that.value, that.type);
            }
            else
            {
                if (that.isPresenceType() || that.isOpaqueType())
                    return new Value(value, type);
                else
                    return new Value(coalesceArrays(getValues(), that.getValues(), true), STRING);
            }
        }

        private Object[] coalesceArrays(Object[] values1, Object[] values2, boolean convertToString)
        {
            Object[] result = new Object[values1.length + values2.length];
            for (int i = 0; i < values1.length; i++)
            {
                Object value1 = values1[i];
                result[i] = convertToString ? String.valueOf(value1) : value1;
            }
            for (int i = 0; i < values2.length; ++i)
            {
                Object value2 = values2[i];
                result[values1.length + i] = convertToString ? String.valueOf(value2) : value2;
            }
            return result;
        }

        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            final Value that = (Value)obj;
            if (type != that.type) return false;
            if (multiValued != that.multiValued) return false;
            if (multiValued)
            {
                return Arrays.equals((Object[])value, (Object[])that.value);
            }
            else
            {
                return value == null ? that.value == null : value.equals(that.value);
            }
        }

        public int hashCode()
        {
            int result = type;
            result = 29 * result + (multiValued ? 1 : 0);
            if (multiValued)
            {
                result = 29 * result + Arrays.hashCode((Object[])value);
            }
            else
            {
                result = 29 * result + (value == null ? 0 : value.hashCode());
            }
            return result;
        }
    }

    private static class Attribute
    {
        private final Tag tag;
        private final Value value;

        private Attribute(Tag tag, Value value)
        {
            this.tag = tag;
            this.value = value;
        }
    }
}
