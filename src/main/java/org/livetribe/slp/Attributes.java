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
 * SLP attributes are defined in RFC 2608, 5.0.
 * This class encapsulates operations and parsing of SLP attributes.
 *
 * @version $Rev$ $Date$
 */
public class Attributes
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

    private final Map attributes = new HashMap();

    public Attributes()
    {
    }

    public Attributes(String attributeList) throws ServiceLocationException
    {
        parse(attributeList);
    }

    private Attributes(Attributes copy)
    {
        attributes.putAll(copy.attributes);
    }

    public void put(String tag)
    {
        attributes.put(tag, new Entry(null, Entry.PRESENCE));
    }

    public void put(String tag, String value)
    {
        attributes.put(tag, entryFromString(value));
    }

    public void put(String tag, String[] values) throws ServiceLocationException
    {
        attributes.put(tag, entryFromStringArray(values));
    }

    public Entry getEntry(String tag)
    {
        return (Entry)attributes.get(tag);
    }

    public Object getValue(String tag)
    {
        Entry entry = getEntry(tag);
        if (entry == null) return null;
        return entry.getValue();
    }

    public Object[] getValues(String tag)
    {
        Entry entry = getEntry(tag);
        if (entry == null) return null;
        return entry.getValues();
    }

    public boolean isEmpty()
    {
        return attributes.isEmpty();
    }

    public boolean isTagPresent(String tag)
    {
        return attributes.containsKey(tag);
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
            if (ch != '\\' && ch < reservedChars.length && reservedChars[ch] == ch)
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
                result.append("\\");
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
            if (c == '\\')
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
                    throw new ServiceLocationException("Unknown escaped character \\" + codeString + " at position " + (i + 1) + " of " + value, ServiceLocationException.PARSE_ERROR);
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
        return value.startsWith("\\FF");
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

    public Attributes merge(Attributes that)
    {
        Attributes result = new Attributes(this);
        if (that != null) result.attributes.putAll(that.attributes);
        return result;
    }

    public Attributes unmerge(Attributes that)
    {
        Attributes result = new Attributes(this);
        if (that != null) result.attributes.keySet().removeAll(that.attributes.keySet());
        return result;
    }

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

        public boolean isStringType()
        {
            return type == STRING;
        }

        public boolean isLongType()
        {
            return type == LONG;
        }

        public boolean isBooleanType()
        {
            return type == BOOLEAN;
        }

        public boolean isOpaqueType()
        {
            return type == OPAQUE;
        }

        public boolean isPresenceType()
        {
            return type == PRESENCE;
        }

        public Object getValue()
        {
            if (valueIsArray) return ((Object[])value)[0];
            return value;
        }

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
