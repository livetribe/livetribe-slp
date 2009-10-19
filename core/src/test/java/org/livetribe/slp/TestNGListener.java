/*
 * Copyright 2008-2008 the original author or authors
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

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

/**
 * @version $Revision$ $Date$
 */
public class TestNGListener extends TestListenerAdapter
{
    private static final String EOL = System.getProperty("line.separator");

    @Override
    public void onTestStart(ITestResult testResult)
    {
        StringBuilder message = new StringBuilder(EOL);
        message.append("TEST ").append(testResult.getTestClass().getName());
        message.append(".").append(testResult.getName());
        log(message.toString());
    }

    private void log(String message)
    {
        System.out.println(message);
    }
}
