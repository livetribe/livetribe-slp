/**
 *
 * Copyright 2008 (C) The original author or authors
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
package org.livetribe.slp.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;


/**
 * @version $Revision$ $Date$
 */
public class SlpBundleActivatorTest extends AbstractConfigurableBundleCreatorTests
{
    public void testOSGi() throws Exception
    {
        Bundle bundle = bundleContext.getBundle();
        System.err.println("symbolic name " + bundle.getSymbolicName());
        System.err.println("vendor " + bundleContext.getProperty(Constants.FRAMEWORK_VENDOR));

        for (Bundle b : bundleContext.getBundles())
        {
            System.err.println(b.getSymbolicName() + " " + b.getBundleId());
        }
    }

    @Override
    protected String[] getTestBundlesNames()
    {
        return new String[]{"org.livetribe.slp, livetribe-slp-osgi, 2.1.0-SNAPSHOT"};
    }
}
