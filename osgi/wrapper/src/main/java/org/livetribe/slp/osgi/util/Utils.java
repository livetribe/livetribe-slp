/**
 *
 * Copyright 2009 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.osgi.util;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.ServiceReference;


/**
 * @version $Revision$ $Date$
 */
public class Utils
{
    private final static String CLASS_NAME = Utils.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final static String VARIABLE_PATTERN = "\\$\\{([^}]+)\\}";

    /**
     * Substitute variables surrounded by <code>${</code> and <code>}</code>.
     * The variables' values come from the dicionary that is also passed along.
     * Variables that have no values in the dictionary are removed from the
     * final string result.
     *
     * @param string     the original string w/ variables
     * @param dictionary the dictionary with variables' values
     * @return the orginal string with variables substituted
     */
    public static String subst(String string, Map dictionary)
    {
        LOGGER.entering(CLASS_NAME, "subst", new Object[]{string, dictionary});

        Pattern pattern = Pattern.compile(VARIABLE_PATTERN);
        Matcher matcher = pattern.matcher(string);

        while (matcher.find())
        {
            String value = (String) dictionary.get(matcher.group(1));

            if (value == null) value = "";

            string = string.replace(matcher.group(0), value);
            matcher.reset(string);
        }
        matcher.reset();

        LOGGER.exiting(CLASS_NAME, "subst", string);

        return string;
    }

    /**
     * Convert a {@link ServiceReference}'s attributes into a dictionary.
     *
     * @param reference the service reference to be converted
     * @return a dictionary of the service reference's attributes
     */
    public static Map<String, String> toMap(ServiceReference reference)
    {
        LOGGER.entering(CLASS_NAME, "toMap", reference);

        Map<String, String> result = new Hashtable<String, String>();

        for (String key : reference.getPropertyKeys())
        {
            result.put(key, reference.getProperty(key).toString());
        }

        LOGGER.exiting(CLASS_NAME, "toMap", result);

        return result;
    }

    private Utils() { }
}
