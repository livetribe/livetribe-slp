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
package org.livetribe.slp.spi;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;


/**
 *
 */
public class AbstractServerTest
{
    private void await(CyclicBarrier barrier)
    {
        try
        {
            barrier.await();
        }
        catch (Exception x)
        {
            throw new AssertionError(x);
        }
    }

    /**
     * Tests that calling concurrently start() results in only one call to doStart().
     */
    @Test
    public void testT1StartT2Start() throws Exception
    {
        ExecutorService pool = Executors.newCachedThreadPool();
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicInteger start = new AtomicInteger(0);

        final AbstractServer server = new AbstractServer()
        {
            protected void doStart()
            {
                logger.finer("Server " + this + " in doStart");
                if (start.compareAndSet(0, 1))
                {
                    // Rendez-vous on barrier1 with thread 'main'
                    await(barrier);
                    // Rendez-vous on barrier2 with thread 'main'
                    await(barrier);
                }
            }

            protected void doStop()
            {
                logger.finer("Server " + this + " in doStop");
            }
        };

        Callable<Boolean> starter = new Callable<Boolean>()
        {
            public Boolean call() throws Exception
            {
                return server.start();
            }
        };

        Future<Boolean> t1 = pool.submit(starter);
        // Rendez-vous on barrier1 with thread 't1'
        barrier.await();
        barrier1:
        {}

        Future<Boolean> t2 = pool.submit(starter);
        assert !t2.get();
        // Rendez-vous on barrier2 with thread 't1'
        barrier.await();
        barrier2:
        {}

        assert t1.get();
    }

    @Test
    public void testT1StartT2StopT2Start() throws Exception
    {
        ExecutorService pool = Executors.newCachedThreadPool();
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicInteger start = new AtomicInteger(0);

        final AbstractServer server = new AbstractServer()
        {
            protected void doStart()
            {
                logger.finer("Server " + this + " in doStart");
                if (start.compareAndSet(0, 1))
                {
                    // Rendez-vous on barrier1 with thread 'main'
                    await(barrier);
                    // Rendez-vous on barrier2 with thread 'main'
                    await(barrier);
                    // Rendez-vous on barrier3 with thread 'main'
                    await(barrier);
                }
            }

            protected void doStop()
            {
                logger.finer("Server " + this + " in doStop");
            }
        };

        Callable<Boolean> starter = new Callable<Boolean>()
        {
            public Boolean call() throws Exception
            {
                return server.start();
            }
        };

        Callable<Boolean> stopper = new Callable<Boolean>()
        {
            public Boolean call() throws Exception
            {
                return server.stop();
            }
        };

        Future<Boolean> t1 = pool.submit(starter);
        // Rendez-vous on barrier1 with thread 't1'
        barrier.await();
        barrier1:
        {}

        Future<Boolean> t2 = pool.submit(stopper);
        assert !t2.get();
        // Rendez-vous on barrier2 with thread 't1'
        barrier.await();
        barrier2:
        {}

        t2 = pool.submit(starter);
        assert !t2.get();
        // Rendez-vous on barrier3 with thread 't1'
        barrier.await();
        barrier3:
        {}

        assert t1.get();
    }
}
