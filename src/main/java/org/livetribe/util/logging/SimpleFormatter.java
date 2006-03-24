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
package org.livetribe.util.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * @version $Rev$ $Date$
 */
public class SimpleFormatter extends Formatter
{
    private final String EOL = System.getProperty("line.separator");

    public String format(LogRecord record)
    {
        StringBuffer result = new StringBuffer();
        result.append(record.getMillis()).append(" ");
        result.append("{").append(record.getThreadID()).append("} ");
//        result.append("[").append(record.getLoggerName()).append("] ");
        result.append(record.getLevel().getLocalizedName()).append(": ");
        result.append(formatMessage(record)).append(EOL);
        Throwable x = record.getThrown();
        if (x != null)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            x.printStackTrace(pw);
            pw.close();
            result.append(sw.toString());
        }
        return result.toString();
    }
}
