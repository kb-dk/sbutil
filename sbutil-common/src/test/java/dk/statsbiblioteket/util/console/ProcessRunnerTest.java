/* $Id: NativeRunnerTest.java 39 2008-01-17 14:58:36Z mke $
 * $Revision: 39 $
 * $Date: 2008-01-17 14:58:36 +0000 (Thu, 17 Jan 2008) $
 * $Author: mke $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.util.console;

import dk.statsbiblioteket.util.JobController;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * NativeRunner Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ProcessRunnerTest extends TestCase {
    public ProcessRunnerTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSetParameters() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetProcessOutput() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetProcessError() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetProcessOutputAsString() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetProcessErrorAsString() throws Exception {
        //TODO: Test goes here...
    }

    public void testSimpleCall() throws Exception {
        ProcessRunner runner = new ProcessRunner("true");
        runner.run();
        assertEquals("The execution of true should work fine",
                     0, runner.getReturnCode());

        runner = new ProcessRunner("false");
        runner.run();
        assertEquals("The execution of false should give 1",
                     1, runner.getReturnCode());
    }


    public void testReturnCodes() throws Exception {
        ProcessRunner runner = new ProcessRunner("false");
        runner.run();
        assertFalse("The execution of 'false' should not return 0",
                    runner.getReturnCode() == 0);
    }

    public void testError() throws Exception {
        ProcessRunner runner = new ProcessRunner("bash", "-c", "echo -n foo 1>&2 ; false");
        runner.run();
        assertFalse("The execution of 'false' should not return 0",
                    runner.getReturnCode() == 0);
        assertEquals("The correct error should be printed",
                     "foo", runner.getProcessErrorAsString());
    }

    public void testMuchOutput() throws Exception {
        int MAX_OUT = 1000;
        ProcessRunner runner = new ProcessRunner("bash", "-c", "echo $(yes %| head -n2000)");
        runner.setOutputCollectionByteSize(1000);
        runner.run();
        assertEquals("The execution of 'false' should return 0",
                     0, runner.getReturnCode());
        assertEquals("The length of the output should be as expected",
                     MAX_OUT-1, runner.getProcessOutputAsString().length());
    }

    public void testProblemsNotError() throws Exception {
        ProcessRunner runner = new ProcessRunner("bash", "-c", "echo -n foo 1>&2 ; true");
        runner.run();
        assertEquals("The execution of 'false' should return 0",
                     0, runner.getReturnCode());
        assertEquals("The correct error should be printed",
                     "foo", runner.getProcessErrorAsString());
    }

    public void testVarArgs() throws Exception {
        ProcessRunner runner = new ProcessRunner("sleep", "1");
        runner.run();
        assertEquals("The execution of \"sleep 1\" should work fine",
                     0, runner.getReturnCode());

        runner = new ProcessRunner("true");
        runner.run();
        assertEquals("The execution of \"true\" should work fine",
                     0, runner.getReturnCode());

        runner = new ProcessRunner("echo", "-e", "do\n", "the\n", "mash\n", "potato\n");
        runner.run();
        assertEquals("The execution of \"echo -e do the mash potato\" "
                     + "should work fine",
                     0, runner.getReturnCode());
    }

    public void testTimeout() throws Exception {

        ProcessRunner runner = new ProcessRunner(Arrays.asList("sleep", "2000"));

        runner.setTimeout(100);
        runner.run();
        if (!runner.isTimedOut()) {
            fail("The execution of sleep should time out");
        }
        assertFalse("The process should be marked as failed", runner.getReturnCode() == 0);
    }

    public void testOutputCollection() throws InterruptedException, ExecutionException {
        final int RUNS = 1;
        final int JOBS = 100;
        final String COMMAND = "echo start ; sleep 1 ; echo stop";
        final String EXPECTED = "start\nstop\n";

        JobController<ProcessRunner> controller = new JobController<ProcessRunner>(JOBS);

        Thread.setDefaultUncaughtExceptionHandler(new DebugExceptionHandler());

        for (int r = 0 ; r < RUNS ; r++) {
            for (int i = 0 ; i < JOBS ; i++) {
                ProcessRunner runner = new ProcessRunner(Arrays.asList("bash", "-c", COMMAND));
                try {
                    controller.submit(runner);
                } catch (OutOfMemoryError e) {
                    fail("OutOfMemory (or threads) with return code " + runner.getReturnCode() + " and return output "
                         + runner.getProcessOutputAsString());
                }
            }

            for (int t = 0 ; t < JOBS ; t++) {
                String result = controller.take().get().getProcessOutputAsString();
                assertEquals("The output from run " + r + ", take " + t + " should be as expected", EXPECTED, result);
            }
        }
    }

    private final class DebugExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            System.err.println("Uncaught exception running unit test");
            e.printStackTrace(System.err);
            System.out.println("Continuing...");
        }
    }

    public void testNoTimeout() throws Exception {

        ProcessRunner runner = new ProcessRunner(Arrays.asList("sleep", "1"));

        runner.setTimeout(1100);
        runner.run();
        if (runner.isTimedOut()) {
            fail("The execution of sleep should not have timed out");
        }
        assertTrue("The process should finish normally", runner.getReturnCode() == 0);
    }

    public void testEnvironment() throws Exception {

        Map<String, String> env = new HashMap<String, String>();
        env.put("FLAM", "flim");


        ProcessRunner runner = new ProcessRunner(Arrays.asList("/bin/sh", "-c", "echo $FLAM"));
        runner.setEnviroment(env);
        //Nessesary for the command variable expansion to work correctly

        runner.run();
        assertEquals("The execution of echo should work fine",
                     0, runner.getReturnCode());
        assertEquals("The result of echo should be flim",
                     "flim\n", runner.getProcessOutputAsString());
    }

    public void testNullEnvironment() throws Exception {
        // Make sure we don't throw NPEs on null envs
        ProcessRunner runner = new ProcessRunner(Arrays.asList("/bin/echo",
                                                               "boo"));
    }


    public void testFeedProcess() throws Exception {
        Map<String, String> env = new HashMap<String, String>();
        env.put("FLAM", "flim");

        File f = new File("testfile");
        f.createNewFile();
        f.deleteOnExit();
        InputStream in = new FileInputStream(f);
        Writer out = new FileWriter(f);
        out.write("echo $FLAM");
        out.flush();
        ProcessRunner runner =
                new ProcessRunner(Arrays.asList("/bin/sh"));
        runner.setEnviroment(env);
        runner.setInputStream(in);

        runner.run();

        //Nessesary for the command variable expansion to work correctly

        assertEquals("The execution of echo should work fine",
                     0, runner.getReturnCode());
        assertEquals("The result of echo should be flim",
                     "flim\n", runner.getProcessOutputAsString());

    }

    public void testArgsWithSpaces() throws Exception {
        String dir = "/ /tmp";
        ProcessRunner runner = new ProcessRunner("ls", dir);
        runner.run();

        System.out.println("STDOUT:" + runner.getProcessOutputAsString());
        System.out.println("STDERR:" + runner.getProcessErrorAsString());

        assertTrue("Listing the non-existing directory '" + dir + "' "
                   + "should fail", runner.getReturnCode() != 0);
    }

    public static Test suite() {
        return new TestSuite(ProcessRunnerTest.class);
    }
}