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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Attributes are key-value pairs that describe a service.
 * The attribute key is called <b>tag</b>, and the attribute value can be non-valued (the tag as no value),
 * single-valued (the tag as only one value) or multi-valued (the tag has more values).
 * Depending on the value(s) of the tag, the attribute has a <b>type<b>; there are four defined types:
 * <ul>
 * <li>boolean, representing boolean values</li>
 * <li>long, representing natural numbers</li>
 * <li>string, representing strings</li>
 * <li>opaque, representing bytes</li>
 * </ul>
 * Attribute values should be homogeneous: the attribute <code>a=1,true,\FF\00</code> is illegal, because it's not
 * clear if the type is long, boolean, or opaque.
 * <br />
 * Attributes can be used by UserAgents during service lookup to select appropriate services that match required
 * conditions.
 * <br />
 * Attributes can be described using a string representation, for example:
 * <pre>
 * String attributesString = "(a=1,2),(b=true),(bytes=\FF\CA\FE\BA\BE),(present),(description=Something Interesting)";
 * Attributes attributes = new Attributes(attributesString);
 * </pre>
 * The example defines 5 attributes:
 * <ul>
 * <li>the first attribute has tag "a", values "1" and "2" and type "long";</li>
 * <li>the second attribute has tag "b", value "true" and type "boolean";</li>
 * <li>the third attribute has tag "bytes", value "\FF\CA\FE\BA\BE" and type "opaque";</li>
 * <li>the fourth attribute has tag "present", no value, and type "presence";</li>
 * <li>the fifth attribute has tag "description", value "Something Interesting" and type "string".</li>
 * </ul>
 * @version $Rev$ $Date$
 */
public class Attributes
{
    private static final char ESCAPE_PREFIX = '\\';
    private static final String OPAQUE_PREFIX = ESCAPE_PREFIX + "FF";
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

    private final Map attributes = new HashMap();

    /**
     * Creates an empty <code>Attributes</code> object.
     */
    public Attributes()
    {
    }

    /**
     * Creates an <code>Attributes</code> object parsing the given <code>attributeList</code> string.
     * @param attributeList The string containing the attributes to parse
     * @throws ServiceLocationException If the parsing fails
     */
    public Attributes(String attributeList) throws ServiceLocationException
    {
        parse(attributeList);
    }

    private Attributes(Attributes copy)
    {
        attributes.putAll(copy.attributes);
    }

    /**
     * Puts a presence tag.
     * @param tag The tag to add
     */
    public void put(String tag)
    {
        attributes.put(tag, new Entry(null, Entry.PRESENCE));
    }

    /**
     * Puts a tag with the specified value.
     * @param tag The tag to add
     * @param value The value of the tag
     */
    public void put(String tag, String value)
    {
        attributes.put(tag, entryFromString(value));
    }

    /**
     * Puts a tag with the specified values
     * @param tag The tag to add
     * @param values The values of the tag
     * @throws ServiceLocationException If the values are not of the same type
     */
    public void put(String tag, String[] values) throws ServiceLocationException
    {
        attributes.put(tag, entryFromStringArray(values));
    }

    /**
     * Returns the <code>Entry</code> for the given tag.
     */
    public Entry getEntry(String tag)
    {
        return (Entry)attributes.get(tag);
    }

    /**
     * Returns the value for the given tag.
     * <br />
     * Depending on the tag type, it returns:
     * <ul>
     * <li>null, if the tag is not present</li>
     * <li>null, if the tag is present and it is a presence tag</li>
     * <li>the tag value, if the tag is present and it is a single valued tag</li>
     * <li>the first tag value, if the tag is present and it is a multi valued tag</li>
     * </ul>
     * @see #isTagPresent(String)
     * @see #getValues(String)
     */
    public Object getValue(String tag)
    {
        Entry entry = getEntry(tag);
        if (entry == null) return null;
        if (entry.isPresenceType()) return null;
        return entry.getValue();
    }

    /**
     * Returns the values for the given tag.
     * <br />
     * Depending on the tag type, it returns:
     * <ul>
     * <li>null, if the tag is not present</li>
     * <li>null, if the tag is present and it is a presence tag</li>
     * <li>the tag value, wrapped in an Object[] of length 1, if the tag is present and it is a single valued tag</li>
     * <li>the tag values, if the tag is present and it is a multi valued tag</li>
     * </ul>
     * @see #isTagPresent(String)
     * @see #getValue(String)
     */
    public Object[] getValues(String tag)
    {
        Entry entry = getEntry(tag);
        if (entry == null) return null;
        if (entry.isPresenceType()) return null;
        return entry.getValues();
    }

    /**
     * Returns true if this <code>Attributes</code> object is empty.
     */
    public boolean isEmpty()
    {
        return attributes.isEmpty();
    }

    /**
     * Returns true if the given tag is present in this <code>Attributes</code> object.
     * @see Entry#isPresenceType()
     */
    public boolean isTagPresent(String tag)
    {
        return attributes.containsKey(tag);
    }

    /**
     * Returns a byte array containing the bytes parsed from the given opaque string, except the initial opaque prefix \FF.
     * @param opaqueString The opaque string containing the bytes to parse
     * @throws ServiceLocationException If the parsing fails
     * @see #bytesToOpaque(byte[])
     */
    public static byte[] opaqueToBytes(String opaqueString) throws ServiceLocationException
    {
        if (!opaqueString.startsWith(OPAQUE_PREFIX)) throw new ServiceLocationException("Opaque strings must begin with " + OPAQUE_PREFIX, ServiceLocationException.PARSE_ERROR);
        if (opaqueString.length() % 3 != 0) throw new ServiceLocationException("Opaque strings must be of the form: [\\<HEX><HEX>]+", ServiceLocationException.PARSE_ERROR);

        byte[] result = new byte[(opaqueString.length() - OPAQUE_PREFIX.length()) / 3];
        int position = 0;
        int index = OPAQUE_PREFIX.length();
        while (index < opaqueString.length())
        {
            if (opaqueString.charAt(index) != ESCAPE_PREFIX) throw new ServiceLocationException("Invalid escape sequence at index " + index + " of " + opaqueString, ServiceLocationException.PARSE_ERROR);
            ++index;
            String hexString = opaqueString.substring(index, index + 2);
            result[position] = (byte)(Integer.parseInt(hexString, 16) & 0xFF);
            ++position;
            index += 2;
        }
        return result;
    }

    /**
     * Returns an opaque string containing the escaped sequence of the given bytes, including the initial opaque prefix \FF.
     * @param bytes The bytes to escape into an opaque string
     * @see #opaqueToBytes(String)
     */
    public static String bytesToOpaque(byte[] bytes)
    {
        StringBuffer result = new StringBuffer();
        result.append(OPAQUE_PREFIX);
        for (int i = 0; i < bytes.length; ++i)
        {
            result.append(ESCAPE_PREFIX);
            int code = bytes[i] & 0xFF;
            if (code < 16) result.append("0");
            result.append(Integer.toHexString(code).toUpperCase());
        }
        return result.toString();
    }

    private void parse(String attributeList) throws ServiceLocationException
    {
        if (attributeList == null) return;

        StringBuffer nonPairs = new StringBuffer();
        int start = 0;
        while (start < attributeList.length())
        {
            int open = attributeList.indexOf('(', start);
            if (open < 0)
            {
                String remaining = attributeList.substring(start);
                nonPairs.append(remaining);
                start += remaining.length();
            }
            else
            {
                int close = attributeList.indexOf(')', open);
                if (close < 0)
                    throw new ServiceLocationException("Missing ')' in attribute list " + attributeList, ServiceLocationException.PARSE_ERROR);
                nonPairs.append(attributeList.substring(start, open));
                String pair = attributeList.substring(open, close + 1);
                parseAttribute(pair, attributeList);
                start = close + 1;
            }
        }

        // Only tags, no pairs
        String[] attributes = nonPairs.toString().split(",", 0);
        for (int i = 0; i < attributes.length; ++i)
        {
            String attribute = attributes[i].trim();
            if (attribute.length() > 0) parseAttribute(attribute, attributeList);
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
                throw new ServiceLocationException("Could not parse attributes " + attributeList + ", missing '=' in " + attribute, ServiceLocationException.PARSE_ERROR);

            String escapedTag = pair.substring(0, equals);
            String unescapedTag = checkAndUnescapeTag(escapedTag);

            String value = pair.substring(equals + 1).trim();
            if (value.indexOf(',') >= 0)
            {
                // It's a list of values
                String[] values = value.split(",", -1);
                if (values.length > 0)
                {
                    for (int i = 0; i < values.length; ++i) values[i] = checkAndUnescapeValue(values[i]);
                    put(unescapedTag, values);
                }
            }
            else
            {
                value = checkAndUnescapeValue(value);
                put(unescapedTag, value);
            }
        }
        else
        {
            attribute = checkAndUnescapeTag(attribute);
            put(attribute);
        }
    }

    private String checkAndUnescapeTag(String escapedTag) throws ServiceLocationException
    {
        // Check that the escaped tag does not contain reserved characters
        checkEscaped(escapedTag);

        // Check if the tag is unescapable; wrong escaping will be detected here
        String unescaped = unescape(escapedTag).trim();
        if (unescaped.length() == 0)
            throw new ServiceLocationException("Attribute tag could not be the empty string", ServiceLocationException.PARSE_ERROR);

        // Check disallowed characters in unescaped tag
        if (unescaped.indexOf('\t') >= 0 ||
                unescaped.indexOf('\r') >= 0 ||
                unescaped.indexOf('\n') >= 0 ||
                unescaped.indexOf('*') >= 0 ||
                unescaped.indexOf('_') >= 0)
            throw new ServiceLocationException("Illegal character in attribute tag " + unescaped, ServiceLocationException.PARSE_ERROR);

        return unescaped;
    }

    private String checkAndUnescapeValue(String escaped) throws ServiceLocationException
    {
        escaped = escaped.trim();

        // Special case: the value is opaque
        if (isOpaque(escaped)) return escaped;

        // Check that the escaped value does not contain reserved characters
        checkEscaped(escaped);

        // Check if the value is unescapable; wrong escaping will be detected here
        return unescape(escaped).trim();
    }

    /**
     * Checks that the given escaped string does not contain reserved characters.
     * For example, the string <code>foo,bar</code> will throw a ServiceLocationException (the comma is a reserved character),
     * while the string <code>foo\2cbar<code> will pass the check.
     *
     * @param escaped The string to check, it's assumed that's escaped.
     * @throws ServiceLocationException If a reserved character is contained in the string
     */
    private void checkEscaped(String escaped) throws ServiceLocationException
    {
        for (int i = 0; i < escaped.length(); ++i)
        {
            char ch = escaped.charAt(i);
            // The backslash is a reserved character, but is present in escaped strings, skip it
            if (ch != ESCAPE_PREFIX && ch < reservedChars.length && reservedChars[ch] == ch)
                throw new ServiceLocationException("Illegal character '" + ch + "' in " + escaped, ServiceLocationException.PARSE_ERROR);
        }
    }

    private String escape(String value)
    {
        if (value == null) return null;

        // Do not escape already escaped sequence of bytes
        if (isOpaque(value)) return value;

        StringBuffer result = new StringBuffer();
        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);
            if (c < reservedChars.length && reservedChars[c] == c)
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

    private String unescape(String value) throws ServiceLocationException
    {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);
            if (c == ESCAPE_PREFIX)
            {
                String codeString = value.substring(i + 1, i + 3);
                int code = Integer.parseInt(codeString, 16);
                if (code < reservedChars.length && reservedChars[code] == code)
                {
                    result.append(reservedChars[code]);
                    i += 2;
                }
                else
                {
                    throw new ServiceLocationException("Unknown escaped character " + ESCAPE_PREFIX + codeString + " at position " + (i + 1) + " of " + value, ServiceLocationException.PARSE_ERROR);
                }
            }
            else
            {
                result.append(c);
            }
        }
        return result.toString();
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
     */
    public String asString()
    {
        TreeMap orderedAttributes = new TreeMap(attributes);

        StringBuffer result = new StringBuffer();
        for (Iterator entries = orderedAttributes.entrySet().iterator(); entries.hasNext();)
        {
            Map.Entry mapEntry = (Map.Entry)entries.next();
            String tag = (String)mapEntry.getKey();
            Entry entry = (Entry)mapEntry.getValue();
            if (entry.isPresenceType())
            {
                result.append(escape(tag));
            }
            else
            {
                result.append("(").append(escape(tag)).append("=");
                if (entry.valueIsArray)
                {
                    Object[] values = entry.getValues();
                    for (int i = 0; i < values.length; ++i)
                    {
                        if (i > 0) result.append(",");
                        result.append(escape(String.valueOf(values[i])));
                    }
                }
                else
                {
                    result.append(escape(String.valueOf(entry.getValue())));
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
        if (isOpaque(value)) return new Entry(value, Entry.OPAQUE);

        // Is it a number ?
        if (isNaturalNumber(value)) return new Entry(Long.valueOf(value), Entry.LONG);

        // Is it a boolean ?
        if (isBoolean(value)) return new Entry(Boolean.valueOf(value), Entry.BOOLEAN);

        // Then it's a string
        return new Entry(value, Entry.STRING);
    }

    private boolean isBoolean(String value)
    {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private boolean isNaturalNumber(String value)
    {
        return Pattern.matches("[0-9]+", value);
    }

    private boolean isOpaque(String value)
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
            if (opaquePresent) throw new ServiceLocationException("Attribute values must be homogeneous: considering values to be strings, but one entry is opaque: " + Arrays.asList(values), ServiceLocationException.PARSE_ERROR);

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
     */
    public static class Entry
    {
        private static final int STRING = 1;
        private static final int LONG = 2;
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
         * Returns true if this entry is of type string.
         */
        public boolean isStringType()
        {
            return type == STRING;
        }

        /**
         * Returns true if this entry is of type long (a natural number).
         */
        public boolean isLongType()
        {
            return type == LONG;
        }

        /**
         * Returns true if this entry is of type boolean.
         */
        public boolean isBooleanType()
        {
            return type == BOOLEAN;
        }

        /**
         * Returns true if this entry is of type opaque.
         */
        public boolean isOpaqueType()
        {
            return type == OPAQUE;
        }

        /**
         * Returns true if this entry represent only the presence of a tag with no value.
         */
        public boolean isPresenceType()
        {
            return type == PRESENCE;
        }

        /**
         * Returns the value of this entry (in case it is single valued), or the first value of this entry
         * (in case it is multivalued).
         */
        public Object getValue()
        {
            if (valueIsArray) return ((Object[])value)[0];
            return value;
        }

        /**
         * Returns the values of this entry (in case it is multivalued), or the value of this entry, wrapped in an array
         * of length 1 (in case it is single valued).
         */
        public Object[] getValues()
        {
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
