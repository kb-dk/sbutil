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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An extension of CompletionService that supports non-blocking result polling as well as non-terminating wait for
 * all jobs to finish.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class JobController<R> extends ExecutorCompletionService<R> {
    private static final int MAX_IDLE_THREADS = 20;

    private final Executor executor;
    private final AtomicInteger issued = new AtomicInteger(0);
    private final AtomicInteger tasks = new AtomicInteger(0);
    private boolean autoEmpty = false;

    /**
     * Shortcut for {@code JobController(maxConcurrentThreads, false, false)}.
     * @param maxConcurrentThreads the maximum number of active threads.
     */
    public JobController(int maxConcurrentThreads) {
        this(maxConcurrentThreads, false, false);
    }

    /**
     * Shortcut for {@code JobController(maxConcurrentThreads, false, autoEmpty)}.<br/>
     * Constructs a JobController that automatically empties the queue for finished jobs.<br/>
     * Post-finish processing of the results from the jobs are handled by overriding
     * {@link #afterExecute(java.util.concurrent.Future)}.
     * @param maxConcurrentThreads the maximum number of active threads.
     */
    public JobController(int maxConcurrentThreads, boolean autoEmpty) {
        this(maxConcurrentThreads, false, autoEmpty);
    }

    /**
     *
     * @param maxConcurrentThreads the maximum number of active threads.
     * @param daemonThreads if true, exiting the main thread will end JVM execution.
     *                      If false, active threads will finish before exiting the JVM.
     */
    public JobController(int maxConcurrentThreads, final boolean daemonThreads, final boolean autoEmpty) {
        this(new CallbackThreadPoolExecutor(maxConcurrentThreads, daemonThreads));
        ((CallbackThreadPoolExecutor)executor).setCallback(this);
        this.autoEmpty = autoEmpty;
    }

    /**
     * @param executor a ThreadPoolExecutor is recommended as that enables {@link #getActiveCount()}.
     */
    public JobController(Executor executor) {
        super(executor);
        this.executor = executor;
    }

    /**
     * Called each time a task is removed from the controller. Override for special processing.
     * The call takes place outside of task synchronization so long processing will not affect other threads.
     * @param finished the Future for a finished task.
     */
    @SuppressWarnings("UnusedParameters")
    protected void afterExecute(Future<R> finished) {
        // No standard processing
    }

    /**
     * Non-blocking.
     * @return all finished Futures.
     */
    public List<Future<R>> popFinished() {
        List<Future<R>> finished = new ArrayList<Future<R>>();
        Future<R> f;
        while ((f = poll()) != null) {
            finished.add(f);
        }
        return finished;
    }

    /**
     * Non-blocking.
     * @return the result of all finished Futures.
     */
    public List<R> popFinishedResults() throws ExecutionException, InterruptedException {
        List<R> finished = new ArrayList<R>();
        Future<R> f;
        while ((f = poll()) != null) {
            finished.add(f.get());
        }
        return finished;
    }

    /**
     * Blocking. Waits for all running tasks to finish, the returns their Futures.
     * @return all tasks.
     */
    public List<Future<R>> popAll() throws InterruptedException {
        List<Future<R>> finished = new ArrayList<Future<R>>();
        synchronized (tasks) {
            while (tasks.get() > 0) {
                Future<R> future = super.take();
                tasks.decrementAndGet();
                finished.add(future);
            }
        }
        for (Future<R> future: finished) {
            afterExecute(future);
        }
        return finished;
    }

    /**
     * Blocking. Waits at most the given time for all running tasks to finish, the returns their Futures.
     * </p><p>
     * To avoid synchronized callbacks, all running tasks are collected before they are issued to callback.
     * </p>
     * Important: The JobController is not guaranteed to be empty after this call.
     * @return all tasks.
     * @throws java.lang.InterruptedException if interrupted during poll.
     */
    public List<Future<R>> popAll(long timeout, TimeUnit unit) throws InterruptedException {
        List<Future<R>> finished = new ArrayList<Future<R>>();
        synchronized (tasks) {
            while (tasks.get() > 0) {
                Future<R> future = super.poll(timeout, unit);
                if (future == null) {
                    // TODO: Silent ignore of timeout is bad but this is the design of the ExecutorCompletionService
                    break;
                }
                tasks.decrementAndGet();
                finished.add(future);
            }
        }
        for (Future<R> future: finished) {
            afterExecute(future);
        }
        return finished;
    }

    @Override
    public Future<R> submit(Callable<R> task) {
        Future<R> future = super.submit(task);
        tasks.incrementAndGet();
        issued.incrementAndGet();
        return future;
    }

    @Override
    public Future<R> submit(Runnable task, R result) {
        Future<R> future = super.submit(task, result);
        tasks.incrementAndGet();
        issued.incrementAndGet();
        return future;
    }

    // We only synchronize takers and not submitters as we only need to guard against the queue being emptied
    // during popAll.
    @Override
    public Future<R> take() throws InterruptedException {
        Future<R> future;
        synchronized (tasks) {
            future = super.take();
            tasks.decrementAndGet();
        }
        afterExecute(future);
        return future;
    }

    @Override
    public Future<R> poll() {
        Future<R> future;
        synchronized (tasks) {
            future = super.poll();
            if (future != null) {
                tasks.decrementAndGet();
            }
        }
        if (future != null) {
            afterExecute(future);
        }
        return future;
    }

    @Override
    public Future<R> poll(long timeout, TimeUnit unit) throws InterruptedException {
        Future<R> future;
        synchronized (tasks) {
            future = super.poll(timeout, unit);
            if (future != null) {
                tasks.decrementAndGet();
            }
        }
        if (future != null) {
            afterExecute(future);
        }
        return future;
    }

    /**
     * @return the number of jobs where the status has not yet been retrieved.
     */
    public int getTaskCount() {
        return tasks.get();
    }

    /**
     * @return the number of running jobs if the executor is a ThreadPoolExecutor.
     * @throws java.lang.IllegalStateException if the executor is not a ThreadPoolExecutor.
     */
    public int getActiveCount() throws IllegalStateException {
        if (!(executor instanceof ThreadPoolExecutor)) {
            throw new IllegalStateException("The executor was a " + executor.getClass().getCanonicalName()
                                            + " but must be a ThreadPoolExecutor for this call to succeed");
        }
        return ((ThreadPoolExecutor)executor).getActiveCount();
    }

    /**
     * @return the total number of issued tasks.
     */
    public int getIssued() {
        return issued.get();
    }

    @Override
    public String toString() {
        return "JobController(tasks=" + getTaskCount() + ", active=" + getActiveCount()
               + ", issued=" + getIssued() + ", executor=" + executor + ")";
    }

    /**
     * Automatically calls {@link dk.statsbiblioteket.util.JobController#afterExecute(java.util.concurrent.Future)}
     * when a task finishes.
     */
    private static class CallbackThreadPoolExecutor extends ThreadPoolExecutor {
        private JobController callback;

        public CallbackThreadPoolExecutor( int maxConcurrentThreads, final boolean daemonThreads) {
            super(maxConcurrentThreads < MAX_IDLE_THREADS ? maxConcurrentThreads : MAX_IDLE_THREADS,
                  MAX_IDLE_THREADS,
                  10, TimeUnit.MINUTES,
                  new ArrayBlockingQueue<Runnable>(100),
                  new ThreadFactory() {
                      @Override
                      public Thread newThread(Runnable r) {
                          Thread t = new Thread(r);
                          t.setDaemon(daemonThreads);
                          return t;
                      }
                  });
            }
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (callback != null && callback.autoEmpty) {
                // Only poll, not take, as another thread might have already removed the result
                callback.poll();
            }
        }

        public void setCallback(JobController callback) {
            this.callback = callback;
        }
    }

}
