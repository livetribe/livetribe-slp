/*
 * Copyright 2005-2008 the original author or authors
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
package org.livetribe.slp.spi.msg;

import java.io.UnsupportedEncodingException;

import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Rev$ $Date$
 */
public class BytesBlock
{
    private static final char[] reservedChars = new char[128];

    static
    {
        reservedChars['\t'] = '\t';
        reservedChars['\r'] = '\r';
        reservedChars['\n'] = '\n';
        reservedChars['!'] = '!';
        reservedChars['('] = '(';
        reservedChars[')'] = ')';
        reservedChars['*'] = '*';
        reservedChars['+'] = '+';
        reservedChars[','] = ',';
        reservedChars[';'] = ';';
        reservedChars['<'] = '<';
        reservedChars['='] = '=';
        reservedChars['>'] = '>';
        reservedChars['\\'] = '\\';
        reservedChars['~'] = '~';
    }

    protected static final byte[] EMPTY_BYTES = new byte[0];
    protected static final String[] EMPTY_STRINGS = new String[0];

    /**
     * Reads an integer from <code>bytes</code> in network byte order.
     *
     * @param bytes  The bytes from where the integer value is read
     * @param offset The offset in <code>bytes</code> from where to start reading the integer
     * @param length The number of bytes to read
     * @return The integer value read
     */
    protected static int readInt(byte[] bytes, int offset, int length)
    {
        int result = 0;
        for (int i = 0; i < length; ++i)
        {
            result <<= 8;
            result += bytes[offset + i] & 0xFF;
        }
        return result;
    }

    /**
     * Writes an integer value to <code>bytes</code> in network byte order
     *
     * @param value  The integer value to write
     * @param bytes  The bytes where the integer value is written to
     * @param offset The offset in <code>bytes</code>from where to start writing the integer
     * @param length The number of bytes to write
     */
    protected static void writeInt(int value, byte[] bytes, int offset, int length)
    {
        for (int i = length - 1; i >= 0; --i)
        {
            bytes[offset + i] = (byte)(value & 0xFF);
            value >>= 8;
        }
    }

    protected static String readString(byte[] bytes, int offset, int length, boolean unescape) throws ServiceLocationException
    {
        if (length == 0) return null;
        String string = utf8BytesToString(bytes, offset, length);
        return unescape ? unescape(string) : string;
    }

    protected static byte[] writeString(String value, boolean escape) throws ServiceLocationException
    {
        if (value == null || value.length() == 0) return EMPTY_BYTES;
        String string = escape ? escape(value) : value;
        return stringToUTF8Bytes(string);
    }

    protected static String[] readStringArray(byte[] bytes, int offset, int length, boolean unescape) throws ServiceLocationException
    {
        String commaList = utf8BytesToString(bytes, offset, length);
        if (commaList == null) return EMPTY_STRINGS;
        String[] result = commaList.split(",", -1);
        for (int i = 0; i < result.length; ++i) result[i] = unescape(result[i]);
        return result;
    }

    protected static byte[] writeStringArray(String[] value, boolean escape) throws ServiceLocationException
    {
        if (value == null || value.length == 0) return EMPTY_BYTES;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < value.length; ++i)
        {
            if (i > 0) buffer.append(",");
            buffer.append(escape ? escape(value[i]) : value[i]);
        }
        return stringToUTF8Bytes(buffer.toString());
    }

    protected static byte[] stringToUTF8Bytes(String value) throws ServiceLocationException
    {
        try
        {
            return value.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.PARSE_ERROR);
        }
    }

    protected static String utf8BytesToString(byte[] bytes, int offset, int length) throws ServiceLocationException
    {
        if (length == 0) return null;
        try
        {
            return new String(bytes, offset, length, "UTF-8");
        }
        catch (UnsupportedEncodingException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.PARSE_ERROR);
        }
    }

    protected static String escape(String value)
    {
        if (value == null) return null;
        StringBuilder result = new StringBuilder();
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

    protected static String unescape(String value) throws ServiceLocationException
    {
        if (value == null) return null;
        StringBuilder result = new StringBuilder();
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
}
