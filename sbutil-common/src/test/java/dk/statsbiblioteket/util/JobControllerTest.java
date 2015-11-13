/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class JobControllerTest extends TestCase {
    private static Log log = LogFactory.getLog(JobControllerTest.class);

    public void testPopFinished() throws Exception {
        final int JOBS = 10;
        final AtomicInteger counter = new AtomicInteger(0);
        JobController<Long> controller = new JobController<Long>(10) {
            @Override
            protected void afterExecute(Future<Long> finished) {
                counter.incrementAndGet();
            }
        };
        for (int i = 0; i < JOBS; i++) {
            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait(10);
            }
            controller.submit(new Shout(50));
        }
        int first = controller.popFinished().size();
        synchronized (Thread.currentThread()) {
            Thread.currentThread().wait(60);
        }
        int second = controller.popFinished().size();

        assertTrue("The first pop should yield some results. Got " + first, first > 0);
        assertTrue("The second pop should yield some results. Got " + second, second > 0);
        log.info("first popFinished: " + first + ", second popFinished: " + second);
        assertEquals("The total pops should be correct", 10, first + second);
        assertEquals("The callback count should be correct", 10, counter.get());
    }

    public void testPopAll() throws Exception {
        final int JOBS = 10;
        final AtomicInteger counter = new AtomicInteger(0);
        JobController<Long> controller = new JobController<Long>(10) {
            @Override
            protected void afterExecute(Future<Long> finished) {
                counter.incrementAndGet();
            }
        };
        for (int i = 0; i < JOBS; i++) {
            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait(10);
            }
            controller.submit(new Shout(50));
        }
        int all = controller.popAll().size();

        assertEquals("The total pops should be correct", 10, all);
        assertEquals("The callback count should be correct", 10, counter.get());
    }

    public void testEmptyPop() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        JobController<Long> controller = new JobController<Long>(10) {
            @Override
            protected void afterExecute(Future<Long> finished) {
                counter.incrementAndGet();
            }
        };
        assertTrue("Popping on empty should return empty list", controller.popAll(10, TimeUnit.MILLISECONDS).isEmpty());
        assertEquals("The callback count should zero on empty controller", 0, counter.get());
    }

    public void testPopTimeout() throws Exception {
        final int JOBS = 10;
        final AtomicInteger counter = new AtomicInteger(0);
        JobController<Long> controller = new JobController<Long>(10) {
            @Override
            protected void afterExecute(Future<Long> finished) {
                counter.incrementAndGet();
            }
        };
        for (int i = 0; i < JOBS; i++) {
            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait(10);
            }
            controller.submit(new Shout(50));
        }
        int allTimeout = controller.popAll(10, TimeUnit.MILLISECONDS).size();
        int allLeft = controller.popAll().size();

        assertTrue("Timeout popAll should be > 0 and < " + JOBS + " but was " + allTimeout,
                   allTimeout > 0 && allTimeout < JOBS);
        assertEquals("The total pops should be correct", 10, allTimeout + allLeft);
        assertEquals("The callback count should be correct", 10, counter.get());
    }

    public void testRemoveCallback() throws Exception {
        final int JOBS = 10;
        final AtomicInteger counter = new AtomicInteger(0);
        JobController<Long> controller = new JobController<Long>(10) {
            @Override
            protected void afterExecute(Future<Long> finished) {
                counter.incrementAndGet();
            }
        };
        for (int i = 0; i < JOBS; i++) {
            controller.submit(new Shout(10));
        }
        synchronized (Thread.currentThread()) {
            Thread.currentThread().wait(100);
        }
        assertEquals("The number of pops should match", JOBS, controller.popAll().size());
        assertEquals("The number of callbacks should match", JOBS, counter.get());
    }

    public void TestAutoEmpty() throws InterruptedException {
        final int JOBS = 10;
        final AtomicInteger counter = new AtomicInteger(0);
        JobController<Long> controller = new JobController<Long>(10, true) {
            @Override
            protected void afterExecute(Future<Long> finished) {
                counter.incrementAndGet();
            }
        };
        for (int i = 0; i < JOBS; i++) {
            controller.submit(new Shout(JOBS / 4));
            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait(JOBS / 10);
            }
        }
        synchronized (Thread.currentThread()) {
            Thread.currentThread().wait(JOBS / 4 + 1);
        }
        assertEquals("The auto removed count should be all the jobs", JOBS, counter.get());
        assertEquals("The JobController should be empty", 0, controller.getTaskCount());
    }

    public void TestAutoEmptyMultiPoll() throws InterruptedException {
        final int JOBS = 10;
        final AtomicInteger counter = new AtomicInteger(0);
        JobController<Long> controller = new JobController<Long>(10, true) {
            @Override
            protected void afterExecute(Future<Long> finished) {
                counter.incrementAndGet();
            }
        };
        for (int i = 0; i < JOBS; i++) {
            controller.submit(new Shout(JOBS / 4));
            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait(JOBS / 10);
            }
        }
        int popped = controller.popAll().size();
        assertEquals("The auto removed count should be all the jobs", JOBS, counter.get());
        assertEquals("The JobController should be empty", 0, controller.getTaskCount());
        assertTrue("The number of explicit popped jobs should be > 0 and < " + JOBS + " but was " + popped,
                   popped > 0 && popped < JOBS);
    }

    private class Shout implements Callable<Long> {
        private final long wait;

        private Shout(long wait) {
            this.wait = wait;
        }

        @Override
        public Long call() throws Exception {
            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait(wait);
            }
            return System.currentTimeMillis();
        }
    }
}
