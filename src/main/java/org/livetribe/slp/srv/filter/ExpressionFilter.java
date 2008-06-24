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
package org.livetribe.slp.srv.filter;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Rev$ $Date$
 */
public class ExpressionFilter implements Filter
{
    static final String GE = ">=";
    static final String LE = "<=";
    static final String EQ = "=";
    private static final String ANY = "*";

    private final String lhs;
    private final String operator;
    private final String rhs;

    public ExpressionFilter(String lhs, String operator, String rhs) throws ServiceLocationException
    {
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
        // Check if wildcard comparison is done properly (RFC 2608, 8.1)
        if (!EQ.equals(operator) && rhs.indexOf(ANY) >= 0) throw new ServiceLocationException("Invalid filter " + this + ": wildcard matching is only allowed with operator " + EQ, ServiceLocationException.PARSE_ERROR);
    }

    public boolean matches(Attributes attributes)
    {
        if (attributes == null) return false;

        Attributes.Entry entry = attributes.valueFor(lhs);
        if (entry == null) return false;

        // Check for presence only
        if (entry.isPresenceType() && EQ.equals(operator) && ANY.equals(rhs)) return true;

        return compare(entry, operator, rhs);
    }

    private boolean compare(Attributes.Entry entry, String operator, String compare)
    {
        if (entry.isStringType())
        {
            if (GE.equals(operator))
            {
                String value = (String)entry.getValue();
                return value.compareToIgnoreCase(compare) >= 0;
            }
            else if (LE.equals(operator))
            {
                String value = (String)entry.getValue();
                return value.compareToIgnoreCase(compare) <= 0;
            }
            else if (EQ.equals(operator))
            {
                boolean result = false;
                if (compare.indexOf(ANY) >= 0)
                {
                    // Wildcard comparison
                    String[] parts = compare.split("\\*", 0);
                    Object[] values = entry.getValues();
                    for (Object value : values)
                    {
                        String stringValue = ((String)value).toLowerCase();
                        boolean match = true;
                        int start = 0;
                        for (String part : parts)
                        {
                            part = part.trim().toLowerCase();
                            if (part.length() > 0)
                            {
                                int index = stringValue.indexOf(part, start);
                                match &= index >= 0;
                                start = index + 1;
                            }
                        }
                        result |= match;
                    }
                }
                else
                {
                    // Direct comparison
                    Object[] values = entry.getValues();
                    for (Object value : values)
                    {
                        String stringValue = (String)value;
                        result |= stringValue.compareToIgnoreCase(compare) == 0;
                    }
                }
                return result;
            }
            else
            {
                throw new AssertionError("Invalid operator " + operator);
            }
        }
        else if (entry.isIntegerType())
        {
            try
            {
                int compareInteger = Integer.parseInt(compare);
                if (GE.equals(operator))
                {
                    Object[] values = entry.getValues();
                    boolean result = false;
                    for (Object value : values)
                    {
                        Integer integerValue = (Integer)value;
                        result |= integerValue >= compareInteger;
                    }
                    return result;
                }
                else if (LE.equals(operator))
                {
                    Object[] values = entry.getValues();
                    boolean result = false;
                    for (Object value : values)
                    {
                        Integer integerValue = (Integer)value;
                        result |= integerValue <= compareInteger;
                    }
                    return result;
                }
                else if (EQ.equals(operator))
                {
                    Object[] values = entry.getValues();
                    boolean result = false;
                    for (Object value : values)
                    {
                        Integer integerValue = (Integer)value;
                        result |= integerValue == compareInteger;
                    }
                    return result;
                }
                else
                {
                    throw new AssertionError("Invalid operator " + operator);
                }
            }
            catch (NumberFormatException x)
            {
                return false;
            }
        }
        else if (entry.isBooleanType())
        {
            if (!EQ.equals(operator)) return false;
            if (!"true".equalsIgnoreCase(compare) && !"false".equalsIgnoreCase(compare)) return false;
            Boolean value = (Boolean)entry.getValue();
            return value.equals(Boolean.valueOf(compare));
        }
        else if (entry.isOpaqueType())
        {
            if (!EQ.equals(operator)) return false;
            String value = (String)entry.getValue();
            return compare.equals(value);
        }
        else
        {
            return false;
        }
    }

    public String asString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(lhs).append(operator).append(rhs).append(")");
        return builder.toString();
    }
}
