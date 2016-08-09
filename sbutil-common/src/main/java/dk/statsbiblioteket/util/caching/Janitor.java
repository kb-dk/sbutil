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
package dk.statsbiblioteket.util.caching;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Background Thread for running clean-up and similar jobs.
 * Multiple jobs can be attached to a single Janitor.
 */
public class Janitor extends Thread {
    private static Log log = LogFactory.getLog(Janitor.class);

    private final long msWaitBetweenSweeps;
    private final boolean subtractJobTimeFromWait;
    private final List<Job> jobs = new ArrayList<Job>();

    private boolean stop = false;

    /**
     * @param msWaitBetweenSweeps the number of milliseconds to wait before doing a new sweep of the jobs.
     * @param subtractJobTimeFromWait if true, the time spend during a sweep is subtracted from wait time.
     */
    public Janitor(long msWaitBetweenSweeps, boolean subtractJobTimeFromWait) {
        this.msWaitBetweenSweeps = msWaitBetweenSweeps;
        this.subtractJobTimeFromWait = subtractJobTimeFromWait;
        this.setName("Janitor_" + System.currentTimeMillis());
        this.setDaemon(true);
    }

    /**
     * Add a job to the Janitor.
     */
    public void attach(Job job) {
        synchronized (jobs) {
            jobs.add(job);
        }
    }

    /**
     * Remove the given job from the Janitor.
     * @param job the job to remove.
     * @return true if job was in the Janitor.
     */
    public boolean detach(Job job) {
        synchronized (jobs) {
            return jobs.remove(job);
        }
    }

    @Override
    public void run() {
        log.debug("Starting " + this);
        out:
        while(!stop) {
            long sweepTime = -System.currentTimeMillis();
            int index = 0;
            while (true) {
                Job job;
                synchronized (jobs) { // Lots of logic to ensure Thread safety and efficient operation
                    if (index >= jobs.size() || (job = jobs.get(index++)) == null) {
                        break;
                    }
                }

                try {
                    job.batch();
                } catch (Exception e) {
                    log.warn("Exception calling batch on job " + job, e);
                }
                if (stop) {
                    break out;
                }
            }
            sweepTime += System.currentTimeMillis();
            long waitTime = msWaitBetweenSweeps - (subtractJobTimeFromWait ? sweepTime : 0);
            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while sleeping " + waitTime + "ms");
                }
            }
        }
        log.debug("Shutting down " + this);
    }

    /**
     * Raises the stop signal that (ultimately) ends the Thread execution.
     */
    public void shutdown() {
        stop = true;
    }

    /**
     * Classes connecting to the Janitor must implement the Job.
     */
    public interface Job {
        void batch();
    }

    @Override
    public String toString() {
        return "Janitor(msWaitBetweenSweeps=" + msWaitBetweenSweeps +
               ", subtractJobTimeFromWait=" + subtractJobTimeFromWait +
               ", stopFlagRaised=" + stop + ", #jobs=" + jobs.size() + ')';
    }
}
