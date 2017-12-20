/*
 * ProcessRunner.
 * Copyright (C) 2005-2008  The State and University Library
 * Added to the SBUtils Project by the State and University Library
 * Author Asger Blekinge-Rasmussen
 */
package dk.statsbiblioteket.util.console;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;


/**
 * Native command executor. Based on ProcessBuilder.
 * <ul>
 * <li> Incorporates timeout for spawned processes.
 * <li> Handle automatic collection of bytes from the output and
 * error streams, to ensure that they dont block.
 * <li> Handles automatic feeding of input to the process.
 * <li> Blocking while executing
 * <li> Implements Runnable, to be wrapped in a Thread.
 * </ul>
 *
 *
 * Use the Assessor methods to configure the Enviroment, input, collecting
 * behavoiur, timeout and startingDir.
 * Use the getters to get the output and error streams as strings, along with
 * the return code and if the process timed out.
 *
 * This code is not yet entirely thread safe. Be sure to only call a given
 * processRunner from one thread, and do not reuse it. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "abr")
public class ProcessRunner implements Runnable, Callable<ProcessRunner> {
    protected InputStream processInput = null;
    protected InputStream processOutput = null;
    protected InputStream processError = null;

    /**
     * The threads that polls the output from the commands. When a thread is
     * finished, it removes itself from this list.
     */
    protected final List<Thread> threads =
            Collections.synchronizedList(new LinkedList<Thread>());

    protected final int MAXINITIALBUFFER = 1000000;
    protected final int THREADTIMEOUT = 1000; // Milliseconds
    protected final int POLLING_INTERVAL = 100;//milli

    protected final ProcessBuilder pb;

    //   private final Object locker = new Object();

    protected long timeout = Long.MAX_VALUE;

    protected boolean collect = true;
    protected int maxOutput = 31000;
    protected int maxError = 31000;
    protected int return_code = -2;
    protected boolean timedOut;

    /**
     * Create a new ProcessRunner. Cannot run, until you specify something with
     * the assessor methods.
     */
    public ProcessRunner() {
        pb = new ProcessBuilder();
    }

    /**
     * Create a new ProcessRunner with just this command, with no arguments.
     * Spaces are not allowed in the
     * string
     *
     * @param command the command to run
     */
    public ProcessRunner(String command) {
        this();
        List<String> l = new ArrayList<String>();
        l.add(command);
        setCommand(l);
    }

    /**
     * Create a new ProcessRunner with the given command. Each element in the
     * list should be a command or argument. If the element should not be parsed
     * enclose it in \"'s.
     *
     * @param commands the command to run
     */
    public ProcessRunner(List<String> commands) {
        this();
        setCommand(commands);
    }

    /**
     * Create a new ProcessRunner with the given command and arguments.
     * The first arguments is the command to execute and the remaining
     * are any arguments to pass.
     *
     * @param commands The command and arguments
     */
    public ProcessRunner(String... commands) {
        this(Arrays.asList(commands));
    }

    /**
     * Sets the enviroment that the process should run in. For the the equivalent
     * to the command
     * <pre>
     * export FLIM=flam
     * echo $FLIM
     * </pre>
     * put "FLIM","flam" in the enviroment.
     *
     * @param enviroment The Map containing the mapping in the enviroment.
     */
    public void setEnviroment(Map<String, String> enviroment) {
        if (enviroment != null) {
            Map<String, String> env = pb.environment();
            env.putAll(enviroment);
        }
    }

    /**
     * Set the inputstream, from which the process should read. To be
     * used if you need to give commands to the process, after it has
     * begun.
     *
     * @param processInput to read from.
     */
    public void setInputStream(InputStream processInput) {
        this.processInput = processInput;
    }

    /**
     * The directory to be used as starting dir. If not set, uses the dir of the
     * current process.
     *
     * @param startingDir the starting dir.
     */
    public void setStartingDir(File startingDir) {
        pb.directory(startingDir);
    }


    /**
     * Set the command for this ProcessRunner
     *
     * @param commands the new command.
     */
    public void setCommand(List<String> commands) {
        pb.command(commands);
    }

    /**
     * Set the timeout. Default to Long.MAX_VALUE in millisecs
     *
     * @param timeout the new timeout in millisecs
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Decide if the outputstreams should be collected. Default true, ie, collect
     * the output.
     *
     * @param collect should we collect the output
     */
    public void setCollection(boolean collect) {
        this.collect = collect;
    }

    /**
     * How many bytes should we collect from the ErrorStream. Will block when
     * limit is reached. Default 31000. If set to negative values, will collect until out of memory.
     *
     * @param maxError number of bytes to max collect.
     */
    public void setErrorCollectionByteSize(int maxError) {
        this.maxError = maxError;
    }

    /**
     * How many bytes should we collect from the OutputStream. Will block when
     * limit is reached. Default 31000; If set to negative values, will collect until out of memory.
     *
     * @param maxOutput number of bytes to max collect.
     */

    public void setOutputCollectionByteSize(int maxOutput) {
        this.maxOutput = maxOutput;
    }

    /**
     * The OutputStream will either be the OutputStream directly from the
     * execution of the native commands or a cache with the output of the
     * execution of the native commands
     *
     * @return the output of the native commands.
     */
    public InputStream getProcessOutput() {
        return processOutput;
    }

    /**
     * The OutputStream will either be the error-OutputStream directly from the
     * execution of the native commands  or a cache with the error-output of
     * the execution of the native commands
     *
     * @return the error-output of the native commands.
     */
    public InputStream getProcessError() {
        return processError;
    }

    /**
     * Get the return code of the process. If the process timed out and was
     * killed, the return code will be -1. If the process has not be started at all, it will be -2.
     * But this is not exclusive to this scenario, other programs can also use this return code.
     *
     * @return the return code
     */
    public int getReturnCode() {
        return return_code;
    }

    /**
     * Tells whether the process has timedout. Only valid after the process has
     * been run, of course.
     *
     * @return has the process timed out.
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * Return what was printed on the output channel of a _finished_ process,
     * as a string, including newlines
     *
     * @return the output as a string
     */
    public String getProcessOutputAsString() {
        return getStringContent(getProcessOutput());
    }

    /**
     * Return what was printed on the error channel of a _finished_ process,
     * as a string, including newlines
     *
     * @return the error as a string
     */
    public String getProcessErrorAsString() {
        return getStringContent(getProcessError());
    }


    /**
     * Wait for the polling threads to finish.
     */
    protected void waitForThreads() {
        long endTime = System.currentTimeMillis() + THREADTIMEOUT;
        while (System.currentTimeMillis() < endTime && threads.size() > 0) {
            try {
                Thread.sleep(POLLING_INTERVAL);
            } catch (InterruptedException e) {
                // Ignore, as we are just waiting
            }
        }
    }

    /**
     * Utility Method for reading a stream into a string, for returning
     *
     * @param stream the string to read
     * @return A string with the contents of the stream.
     */
    protected String getStringContent(InputStream stream) {
        if (stream == null) {
            return null;
        }
        BufferedInputStream in = new BufferedInputStream(stream, 1000);
        StringWriter sw = new StringWriter(1000);
        int c;
        try {
            while ((c = in.read()) != -1) {
                sw.append((char) c);
            }
            return sw.toString();
        } catch (IOException e) {
            return "Could not transform content of stream to String";
        }

    }


    /**
     * Run the method, feeding it input, and killing it if the timeout is exceeded.
     * Blocking.
     */
    @Override
    public void run() {
        try {
            Process p = pb.start();

            if (collect) {
                ByteArrayOutputStream pOut = collectProcessOutput(p.getInputStream(), this.maxOutput);
                ByteArrayOutputStream pError = collectProcessOutput(p.getErrorStream(), this.maxError);
                return_code = execute(p);
                waitForThreads();
                processOutput = new ByteArrayInputStream(pOut.toByteArray());
                processError = new ByteArrayInputStream(pError.toByteArray());

            } else {
                processOutput = p.getInputStream();
                processError = p.getErrorStream();
                return_code = execute(p);
            }
        } catch (IOException e) {
            throw new RuntimeException("An io error occurred when running the command", e);
        }
    }

    @Override
    public ProcessRunner call() throws Exception {
        run();
        return this;
    }

    protected int execute(Process p) {
        long startTime = System.currentTimeMillis();
        feedProcess(p, processInput);
        int return_value;

        while (true) {
            //is the thread finished?
            try {
                //then return
                return_value = p.exitValue();
                break;
            } catch (IllegalThreadStateException e) {
                //not finished
            }
            //is the runtime exceeded?
            if (System.currentTimeMillis() - startTime > timeout) {
                //then return
                p.destroy();
                return_value = -1;
                timedOut = true;
                break;
            }
            //else sleep again
            try {
                Thread.sleep(POLLING_INTERVAL);
            } catch (InterruptedException e) {
                //just go on.
            }

        }
        return return_value;

    }


    protected ByteArrayOutputStream collectProcessOutput(
            final InputStream inputStream, final int maxCollect) {
        final ByteArrayOutputStream stream;
        if (maxCollect < 0) {
            stream = new ByteArrayOutputStream();
        } else {
            stream = new ByteArrayOutputStream(Math.min(MAXINITIALBUFFER, maxCollect));
        }

        Thread t = new Thread() {
            public void run() {
                try {
                    InputStream reader = null;
                    OutputStream writer = null;
                    try {
                        reader = new BufferedInputStream(inputStream);
                        writer = new BufferedOutputStream(stream);
                        int c;
                        int counter = 0;
                        while ((c = reader.read()) != -1) {
                            counter++;
                            if (maxCollect < 0 || counter < maxCollect) {
                                writer.write(c);
                            }
                        }
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                        if (writer != null) {
                            writer.close();
                        }
                    }
                } catch (IOException e) {
                    // This seems ugly
                    throw new RuntimeException("Couldn't read output from process.", e);
                }
                threads.remove(this);
            }
        };
        t.setDaemon(true); // Allow the JVM to exit even if t is alive
        threads.add(t);
        t.start();
        return stream;
    }

    protected void feedProcess(final Process process, InputStream processInput) {
        if (processInput == null) {
            // No complaints here - null just means no input
            return;
        }

        final OutputStream pIn = process.getOutputStream();
        final InputStream given = processInput;
        Thread t = new Thread() {
            public void run() {
                try {
                    OutputStream writer = null;
                    try {
                        writer = new BufferedOutputStream(pIn);
                        int c;
                        while ((c = given.read()) != -1) {
                            writer.write(c);
                        }
                    } finally {
                        if (writer != null) {
                            writer.close();
                        }
                        pIn.close();
                    }
                } catch (IOException e) {
                    // This seems ugly
                    throw new RuntimeException("Couldn't write input to " +
                                               "process.", e);
                }
            }
        };

        Thread.UncaughtExceptionHandler u =
                new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(Thread t, Throwable e) {
                        //Might not be the prettiest solution...
                    }
                };
        t.setDaemon(true); // Allow the JVM to exit even if t lives
        t.setUncaughtExceptionHandler(u);
        t.start();

    }
}
