/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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
package org.livetribe.slp.osgi.tests;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import org.ops4j.pax.exam.Inject;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;


/**
 * @version $Revision$ $Date$
 */
@RunWith(JUnit4TestRunner.class)
public class SlpBundleActivatorTest
{
    @Inject
    private BundleContext bundleContext = null;

    @Configuration
    public static Option[] configure()
    {
        return options(
                equinox(),
                felix(),
                knopflerfish(),
                provision(
                        mavenBundle().groupId("org.livetribe.slp").artifactId("livetribe-slp-osgi").version(asInProject())
                )
            // vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006" ),
            // this is necessary to let junit runner not timout the remote process before attaching debugger
            // setting timeout to 0 means wait as long as the remote service comes available.
            // starting with version 0.5.0 of PAx Exam this is no longer required as by default the framework tests
            // will not be triggered till the framework is not started
            // waitForFrameworkStartup()
        );
    }

    @Test
    public void testOSGi() throws Exception
    {
        assertThat(bundleContext, is(notNullValue()));

        Bundle bundle = bundleContext.getBundle();
        System.out.println("symbolic name " + bundle.getSymbolicName());
        System.out.println("vendor " + bundleContext.getProperty(Constants.FRAMEWORK_VENDOR));

        for (Bundle b : bundleContext.getBundles())
        {
            System.out.println(b.getSymbolicName() + " " + b.getBundleId());
        }
    }
}
