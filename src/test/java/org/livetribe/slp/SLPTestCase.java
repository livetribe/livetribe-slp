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

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import junit.framework.TestCase;
import org.livetribe.slp.api.Configuration;

/**
 * @version $Rev$ $Date$
 */
public class SLPTestCase extends TestCase
{
    protected void sleep(long time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
        }
    }

    protected Configuration getDefaultConfiguration() throws IOException
    {
        String EOL = System.getProperty("line.separator");
        StringBuffer configText = new StringBuffer();
        // Linux does not allow non-root users to listen on ports < 1024
        configText.append("org.livetribe.slp.port=1427").append(EOL);
        // Properties files are always encoded in ISO-8859-1
        InputStream stream = new ByteArrayInputStream(configText.toString().getBytes("ISO-8859-1"));
        return new Configuration().configure(stream);
    }
}
