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
 * Attribute values should be homogeneous: the attribute <code>(a=1,true,hello)</code> is illegal, because it's not
 * clear if the type is integer, boolean, or string.
 * <br />
 * Attributes can be used by UserAgents during service lookup to select appropriate services that match required
 * conditions.
 * <br />
 * Attributes can be described using a string representation; a valued or multi-valued attribute must be enclosed in
 * parenthesis, while for non-valued attributes (also called <em>presence attribute</em>) the parenthesis must be
 * omitted, for example:
 * <pre>
 * String attributesString = "(a=1,2),(b=true),(bytes=\FF\CA\FE\BA\BE),present,(description=Something Interesting)";
 * Attributes attributes = Attributes.from(attributesString);
 * </pre>
 * The example defines 6 attributes:
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
public class Attributes
{
    private static final char ESCAPE_PREFIX = '\\';
    private static final String OPAQUE_PREFIX = ESCAPE_PREFIX + "FF";
    private static final char[] reservedChars = new char[128];
    private static final char[] reservedTagChars = new char[128];

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
        System.arraycopy(reservedChars, 0, reservedTagChars, 0, reservedChars.length);
        reservedTagChars['\t'] = '\t';
        reservedTagChars['\n'] = '\n';
        reservedTagChars['\r'] = '\r';
        reservedTagChars['_'] = '_';
        reservedTagChars['*'] = '*';
    }

    public static final Attributes NONE = new Attributes("");

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
        return new Attributes(escapedAttributesString);
    }

    /**
     * Maps unescaped tags to the corrispondent value
     */
    private final Map<String, Entry> attributes = new HashMap<String, Entry>();

    private Attributes(String attributeList)
    {
        parse(attributeList);
    }

    private Attributes(Attributes copy)
    {
        attributes.putAll(copy.attributes);
    }

    /**
     * @param unescapedTag the unescaped tag
     * @return the <code>Entry</code> for the given unescaped tag.
     * @see #escapeTag(String)
     */
    public Entry valueFor(String unescapedTag)
    {
        Entry entry = attributes.get(unescapedTag);
        return entry == null ? Entry.NULL_ENTRY : entry;
    }

    /**
     * @return true if this <code>Attributes</code> object is empty.
     */
    public boolean isEmpty()
    {
        return attributes.isEmpty();
    }

    /**
     * @param unescapedTag the unescaped tag
     * @return true if the given unescaped tag is present in this <code>Attributes</code> object.
     * @see Entry#isPresenceType()
     */
    public boolean containsTag(String unescapedTag)
    {
        return attributes.containsKey(unescapedTag);
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
    public static byte[] opaqueToBytes(String opaqueString) throws ServiceLocationException
    {
        if (!opaqueString.startsWith(OPAQUE_PREFIX))
            throw new ServiceLocationException("Opaque strings must begin with " + OPAQUE_PREFIX, ServiceLocationException.Error.PARSE_ERROR);
        if (opaqueString.length() % 3 != 0)
            throw new ServiceLocationException("Opaque strings must be of the form: [\\<HEX><HEX>]+", ServiceLocationException.Error.PARSE_ERROR);

        byte[] result = new byte[(opaqueString.length() - OPAQUE_PREFIX.length()) / 3];
        int position = 0;
        int index = OPAQUE_PREFIX.length();
        while (index < opaqueString.length())
        {
            if (opaqueString.charAt(index) != ESCAPE_PREFIX)
                throw new ServiceLocationException("Invalid escape sequence at index " + index + " of " + opaqueString, ServiceLocationException.Error.PARSE_ERROR);
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
            result.append(ESCAPE_PREFIX);
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
     * @param tag the tag string to escape
     * @return the escaped tag string
     * @see #unescapeTag(String)
     */
    public static String escapeTag(String tag)
    {
        return escape(tag, reservedTagChars);
    }

    private static String escape(String value, char[] reserved)
    {
        value = value.trim();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);
            if (c < reserved.length && reserved[c] == c)
            {
                result.append(ESCAPE_PREFIX);
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
    public static String unescapeTag(String escapedTag) throws ServiceLocationException
    {
        // Check that the escaped tag does not contain reserved characters
        checkEscaped(escapedTag, reservedTagChars);

        // Unescape the tag
        return unescape(escapedTag, reservedTagChars);
    }

    private static String unescape(String value, char[] reserved)
    {
        value = value.trim();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);
            if (c == ESCAPE_PREFIX)
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
                    throw new ServiceLocationException("Unknown escaped character " + ESCAPE_PREFIX + codeString + " at position " + (i + 1) + " of " + value, ServiceLocationException.Error.PARSE_ERROR);
                }
            }
            else
            {
                builder.append(c);
            }
        }
        return builder.toString().trim();
    }

    private static void checkEscaped(String value, char[] reserved) throws ServiceLocationException
    {
        if (value.trim().length() == 0)
            throw new ServiceLocationException("Escaped string could not be the empty string", ServiceLocationException.Error.PARSE_ERROR);
        for (int i = 0; i < value.length(); ++i)
        {
            char ch = value.charAt(i);
            // The backslash is a reserved character, but is present in escaped strings, skip it
            if (ch != ESCAPE_PREFIX && ch < reserved.length && reserved[ch] == ch)
                throw new ServiceLocationException("Illegal character '" + ch + "' in " + value, ServiceLocationException.Error.PARSE_ERROR);
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
        return escape(unescapedValue, reservedChars);
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
        checkEscaped(escapedValue, reservedChars);

        // Unescape the value
        return unescape(escapedValue, reservedChars);
    }

    private void parse(String escapedAttributeList) throws ServiceLocationException
    {
        if (escapedAttributeList == null) return;

        StringBuilder nonPairs = new StringBuilder();
        int start = 0;
        while (start < escapedAttributeList.length())
        {
            int open = escapedAttributeList.indexOf('(', start);
            if (open < 0)
            {
                String remaining = escapedAttributeList.substring(start);
                nonPairs.append(remaining);
                start += remaining.length();
            }
            else
            {
                int close = escapedAttributeList.indexOf(')', open);
                if (close < 0)
                    throw new ServiceLocationException("Missing ')' in attribute list " + escapedAttributeList, ServiceLocationException.Error.PARSE_ERROR);
                nonPairs.append(escapedAttributeList.substring(start, open));
                String pair = escapedAttributeList.substring(open, close + 1);
                parseAttribute(pair, escapedAttributeList);
                start = close + 1;
            }
        }

        // Only tags, no pairs
        String[] attributes = nonPairs.toString().split(",", 0);
        for (String attribute : attributes)
        {
            attribute = attribute.trim();
            if (attribute.length() > 0) parseAttribute(attribute, escapedAttributeList);
        }
    }

    private void parseAttribute(String attribute, String attributeList) throws ServiceLocationException
    {
        if (attribute == null) return;
        attribute = attribute.trim();
        if (attribute.length() == 0) return;

        if (attribute.startsWith("("))
        {
            int closeParenthesis = attribute.indexOf(')');
            String pair = attribute.substring(1, closeParenthesis);

            int equals = pair.indexOf('=');
            if (equals < 0)
                throw new ServiceLocationException("Could not parse attributes " + attributeList + ", missing '=' in " + attribute, ServiceLocationException.Error.PARSE_ERROR);

            String escapedTag = pair.substring(0, equals);
            String unescapedTag = unescapeTag(escapedTag);

            String value = pair.substring(equals + 1).trim();
            if (value.indexOf(',') >= 0)
            {
                // It's a list of values
                String[] values = value.split(",");
                attributes.put(unescapedTag, entryFromStringArray(values));
            }
            else
            {
                attributes.put(unescapedTag, entryFromString(value));
            }
        }
        else
        {
            String unescapedTag = unescapeTag(attribute);
            attributes.put(unescapedTag, new Entry(null, Entry.PRESENCE));
        }
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final Attributes that = (Attributes)obj;
        // Could not compare the attributes Map, since it contains String[] that will make the comparison fail.
        return asString().equals(that.asString());
    }

    public int hashCode()
    {
        return attributes.hashCode();
    }

    /**
     * Returns a string representation of this <code>Attributes</code> object, that can be passed to
     * {@link #Attributes(String)} to be parsed.
     *
     * @return the escaped attribute list string
     */
    public String asString()
    {
        TreeMap<String, Entry> orderedAttributes = new TreeMap<String, Entry>(attributes);
        StringBuilder result = new StringBuilder();
        for (Iterator<Map.Entry<String, Entry>> entries = orderedAttributes.entrySet().iterator(); entries.hasNext();)
        {
            Map.Entry<String, Entry> mapEntry = entries.next();
            String tag = mapEntry.getKey();
            Entry entry = mapEntry.getValue();
            if (entry.isPresenceType())
            {
                result.append(escapeTag(tag));
            }
            else
            {
                result.append("(").append(escapeTag(tag)).append("=");
                if (entry.valueIsArray)
                {
                    Object[] values = entry.getValues();
                    for (int i = 0; i < values.length; ++i)
                    {
                        if (i > 0) result.append(",");
                        if (entry.isOpaqueType())
                            result.append(bytesToOpaque((byte[])values[i]));
                        else
                            result.append(escapeValue(String.valueOf(values[i])));
                    }
                }
                else
                {
                    if (entry.isOpaqueType())
                        result.append(bytesToOpaque((byte[])entry.getValue()));
                    else
                        result.append(escapeValue(String.valueOf(entry.getValue())));
                }
                result.append(")");
            }
            if (entries.hasNext()) result.append(",");
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

    private Entry entryFromString(String value)
    {
        // Is it opaque ?
        if (isOpaque(value)) return new Entry(opaqueToBytes(value), Entry.OPAQUE);

        // Is it a number ?
        if (isInteger(value)) return new Entry(Integer.valueOf(value), Entry.INTEGER);

        // Is it a boolean ?
        if (isBoolean(value)) return new Entry(Boolean.valueOf(value), Entry.BOOLEAN);

        // Then it's a string
        return new Entry(unescapeValue(value), Entry.STRING);
    }

    private boolean isBoolean(String value)
    {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private boolean isInteger(String value)
    {
        return Pattern.matches("[0-9]+", value);
    }

    private static boolean isOpaque(String value)
    {
        return value.startsWith(OPAQUE_PREFIX);
    }

    private Entry entryFromStringArray(String[] values) throws ServiceLocationException
    {
        Entry[] entries = new Entry[values.length];
        boolean homogeneous = true;
        boolean opaquePresent = false;
        for (int i = 0; i < values.length; ++i)
        {
            entries[i] = entryFromString(values[i]);
            homogeneous &= entries[0].getType() == entries[i].getType();
            if (entries[i].isOpaqueType()) opaquePresent = true;
        }

        if (homogeneous)
        {
            Object[] entryValues = new Object[entries.length];
            for (int i = 0; i < entries.length; ++i)
            {
                Entry entry = entries[i];
                entryValues[i] = entry.getValue();
            }
            return new Entry(entryValues, entries[0].getType());
        }
        else
        {
            // It's not homogeneous, and there is one opaque entry: RFC 2608, 5.0 says it's illegal.
            if (opaquePresent)
                throw new ServiceLocationException("Attribute values must be homogeneous: considering values to be strings, but one entry is opaque: " + Arrays.asList(values), ServiceLocationException.Error.PARSE_ERROR);

            Object[] entryValues = new Object[values.length];
            System.arraycopy(values, 0, entryValues, 0, values.length);
            return new Entry(entryValues, Entry.STRING);
        }
    }

    /**
     * Merges the attributes of this <code>Attributes</code> object with the attributes of the given <code>Attributes</code>
     * object into a new <code>Attributes</code> object.
     * If the given <code>Attributes</code> is null, a clone of this <code>Attributes</code> object will be returned.
     * If this <code>Attributes</code> contains a tag that exists in the given <code>Attributes</code>, the merged
     * <code>Attributes</code> will contain the entry from the given <code>Attributes</code> object (overwriting the one
     * from this <code>Attributes</code> object).
     *
     * @param that The <code>Attributes</code> to merge with
     * @return A new <code>Attributes</code> object containing the merged attributes.
     */
    public Attributes merge(Attributes that)
    {
        Attributes result = new Attributes(this);
        if (that != null) result.attributes.putAll(that.attributes);
        return result;
    }

    /**
     * Unmerges the attributes of this <code>Attributes</code> object with the attributes of the given <code>Attributes</code>
     * object into a new <code>Attributes</code> object.
     * If the given <code>Attributes</code> is null, a clone of this <code>Attributes</code> object will be returned.
     * The unmerged <code>Attributes</code> will contain only the attributes present in this <code>Attributes</code> object
     * but not in the given <code>Attributes</code> object.
     *
     * @param that The <code>Attributes</code> to unmerge with
     * @return A new <code>Attributes</code> object containing the unmerged attributes.
     */
    public Attributes unmerge(Attributes that)
    {
        Attributes result = new Attributes(this);
        if (that != null) result.attributes.keySet().removeAll(that.attributes.keySet());
        return result;
    }

    /**
     * Represent the attribute value within the {@link Attributes} class.
     * An <code>Entry</code> encapsulates the attribute value(s) and type.
     * The values are unescaped, so for example opaque strings are stored as byte[].
     */
    public static class Entry
    {
        private static final Entry NULL_ENTRY = new Entry(null, 0);

        private static final int STRING = 1;
        private static final int INTEGER = 2;
        private static final int BOOLEAN = 3;
        private static final int OPAQUE = 4;
        private static final int PRESENCE = 5;

        private final Object value;
        private final int type;
        private final boolean valueIsArray;

        private Entry(Object value, int type)
        {
            this.value = value;
            this.type = type;
            this.valueIsArray = value instanceof Object[];
        }

        private int getType()
        {
            return type;
        }

        /**
         * @return whether this entry is of type string.
         */
        public boolean isStringType()
        {
            return type == STRING;
        }

        /**
         * @return whether this entry is of type integer.
         */
        public boolean isIntegerType()
        {
            return type == INTEGER;
        }

        /**
         * @return whether this entry is of type boolean.
         */
        public boolean isBooleanType()
        {
            return type == BOOLEAN;
        }

        /**
         * @return whether this entry is of type opaque.
         */
        public boolean isOpaqueType()
        {
            return type == OPAQUE;
        }

        /**
         * @return whether this entry represent only the presence of a tag with no value.
         */
        public boolean isPresenceType()
        {
            return type == PRESENCE;
        }

        /**
         * @return the value of this entry (in case it is single valued), or the first value of this entry
         *         (in case it is multivalued).
         */
        public Object getValue()
        {
            if (isPresenceType()) return null;
            if (valueIsArray) return ((Object[])value)[0];
            return value;
        }

        /**
         * @return the values of this entry (in case it is multivalued), or the value of this entry, wrapped in an array
         *         of length 1 (in case it is single valued).
         */
        public Object[] getValues()
        {
            if (isPresenceType()) return null;
            if (valueIsArray) return (Object[])value;
            return new Object[]{value};
        }

        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            final Entry that = (Entry)obj;
            if (type != that.type) return false;
            if (valueIsArray != that.valueIsArray) return false;
            if (valueIsArray)
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
            result = 29 * result + (valueIsArray ? 1 : 0);
            if (valueIsArray)
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
}
