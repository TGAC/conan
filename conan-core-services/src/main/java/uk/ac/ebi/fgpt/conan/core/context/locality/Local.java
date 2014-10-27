/**
 * RAMPART - Robust Automatic MultiPle AssembleR Toolkit
 * Copyright (C) 2013  Daniel Mapleson - TGAC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package uk.ac.ebi.fgpt.conan.core.context.locality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fgpt.conan.core.context.DefaultExecutionResult;
import uk.ac.ebi.fgpt.conan.core.process.monitor.InvocationTrackingProcessListener;
import uk.ac.ebi.fgpt.conan.model.context.ExecutionResult;
import uk.ac.ebi.fgpt.conan.model.context.Locality;
import uk.ac.ebi.fgpt.conan.model.context.Scheduler;
import uk.ac.ebi.fgpt.conan.model.context.SchedulerArgs;
import uk.ac.ebi.fgpt.conan.model.monitor.ProcessAdapter;
import uk.ac.ebi.fgpt.conan.model.monitor.ProcessListener;
import uk.ac.ebi.fgpt.conan.service.exception.ProcessExecutionException;
import uk.ac.ebi.fgpt.conan.utils.CommandExecutionException;
import uk.ac.ebi.fgpt.conan.utils.ProcessRunner;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * This environment is used to execute code on the localhost. If the localhost
 * happens to be a single machine and a multi-user environment then it is wise
 * to only submit short single threaded jobs to avoid interfering with other
 * users' processes.
 *
 * @author maplesod
 */
public class Local implements Locality {


    private static Logger log = LoggerFactory.getLogger(Local.class);


    /**
     * No need to establish a connection to the local machine, so this method always returns true.
     *
     * @return true
     */
    @Override
    public boolean establishConnection() {
        return true;
    }

    /**
     * No need to disconnect from the local machine, so this method always returns true.
     *
     * @return true
     */
    @Override
    public boolean disconnect() {
        return true;
    }

    /**
     * This is pretty simple as a Local locality does not contain any state.
     *
     * @return
     */
    @Override
    public Locality copy() {
        return new Local();
    }

    @Override
    public String getDescription() {
        return "localhost";
    }

    @Override
    public ExecutionResult monitoredExecute(String processName, String command, Scheduler scheduler) throws InterruptedException, ProcessExecutionException {

        // TODO, this is a mess... needs rethinking at some point.

        //boolean dispatched = false;
        //boolean recoveryMode = processAdapter.inRecoveryMode();

        //log.debug("In recovery mode? " + recoveryMode);

        // Only dispatch the proc if not in recovery mode... otherwise we will probably have two jobs running
        // simultaneously
        try {
            //if (!recoveryMode) {

                // Create the proc monitor if scheduler requires it
                List<ProcessAdapter> processAdapters = new ArrayList<>();
                if (scheduler.usesFileMonitor()) {
                    SchedulerArgs.JobArrayArgs jaa = scheduler.getArgs().getJobArrayArgs();
                    if (jaa != null) {
                        for(int i = jaa.getMinIndex(); i <= jaa.getMaxIndex(); i += jaa.getStepIndex()) {
                            processAdapters.add(scheduler.createProcessAdapter(i));
                        }
                    }
                    else {
                        processAdapters.add(scheduler.createProcessAdapter());
                    }

                    for(ProcessAdapter pa : processAdapters) {
                        pa.createMonitor();
                    }
                }

                // Execute the proc (this is a scheduled proc so it should run in the background, managed by the scheduler,
                // therefore we call the normal foreground execute method)
                ExecutionResult result = this.execute(processName, command, scheduler);

                if (result.getExitCode() != 0) {
                    throw new ProcessExecutionException(result.getExitCode(), "Process returned non-zero exit code: " + result.getExitCode());
                }

                // Wait for the proc to complete by using the proc monitor
                if (scheduler.usesFileMonitor()) {
                    if (scheduler.getArgs().getJobArrayArgs() != null) {

                        ExecutorService pool = Executors.newFixedThreadPool(processAdapters.size());
                        Set<Future<ExecutionResult>> set = new HashSet<>();
                        SchedulerArgs.JobArrayArgs jaa = scheduler.getArgs().getJobArrayArgs();
                        for(ProcessAdapter pa : processAdapters) {
                            set.add(pool.submit(new ProcessAdaptorCallable(processName, pa)));
                        }

                        int exitCode = 0;
                        int errorCount = 0;
                        String[] message = new String[]{"All jobs in array completed successfully."};
                        for(Future<ExecutionResult> res : set) {
                            try {
                                if (res.get().getExitCode() != 0) {
                                    exitCode = 1;
                                    errorCount++;
                                }
                            }
                            catch (ExecutionException e) {
                                exitCode = 2;
                                errorCount++;
                            }
                        }

                        if (exitCode != 0) {
                            message = new String[] {"Job Array Error: " + errorCount + " out of " + processAdapters.size() + " jobs failed in the array."};
                        }

                        result = new DefaultExecutionResult(processName, exitCode, message, scheduler.getArgs().getMonitorFile());
                    }
                    else {

                        ProcessAdapter pa = processAdapters.get(0);

                        // Override result with info from the wait command.
                        ExecutionResult waitResult = this.waitFor(processName, pa, new InvocationTrackingProcessListener());

                        return new DefaultExecutionResult(
                                processName,
                                waitResult.getExitCode(),
                                pa.getProcessOutput(),
                                pa.getFile(),
                                result.getJobId(),
                                scheduler.getResourceUsageFromMonitorFile(pa.getFile()));
                    }
                }

                return result;
            //}
        } catch (IOException ioe) {
            throw new ProcessExecutionException(-1, ioe);
        } finally {
            // Remove the monitor, even if we are in recovery mode or there was an error.
            //processAdapter.removeMonitor();
        }

        // Hopefully we don't reach here but if we do then just return exit code 1 to signal an error.
        //return 1;
    }

    private static class ProcessAdaptorCallable
            implements Callable<ExecutionResult> {
        private String name;
        private ProcessAdapter pa;
        private ProcessListener pl;
        public ProcessAdaptorCallable(String processName, ProcessAdapter pa) {
            this.name = processName;
            this.pa = pa;
            this.pl = new InvocationTrackingProcessListener();
        }
        public ExecutionResult call() {
            pa.addTaskListener(pl);

            // proc exit value, initialise to -1
            int exitValue = -1;

            // proc monitoring
            try {
                log.debug("Monitoring proc, waiting for completion...");
                exitValue = this.pl.waitFor();
                log.debug("Process completed with exit value: " + exitValue);

                return new DefaultExecutionResult(this.name, exitValue, pa.getProcessOutput(), pa.getFile());
            }
            catch(InterruptedException e) {
                return null;
            }
        }
    }


    @Override
    public ExecutionResult execute(String processName, String command, Scheduler scheduler) throws ProcessExecutionException, InterruptedException {

        String[] output;

        try {
            ProcessRunner runner = new ProcessRunner();
            runner.redirectStderr(true);
            output = runner.runCommmand(command);

        } catch (CommandExecutionException e) {

            String message = "Failed to execute job (exited with exit code: " + e.getExitCode() + ")";

            log.error(message, e);
            ProcessExecutionException pex = new ProcessExecutionException(
                    e.getExitCode(), message, e);
            pex.setProcessOutput(e.getErrorOutput());
            try {
                pex.setProcessExecutionHost(InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e1) {
                log.warn("Unknown host", e1);
            }
            throw pex;
        } catch (IOException e) {
            String message = "Failed to read output stream of native system proc";
            log.error(message);
            log.debug("IOException follows", e);
            throw new ProcessExecutionException(1, message, e);
        }

        int jobId = -1;
        File outputFile = null;

        if (scheduler != null) {

            if (scheduler.generatesJobIdFromOutput()) {
                jobId = scheduler.extractJobIdFromOutput(output[0]);
            }

            if (scheduler.getArgs() != null && scheduler.getArgs().getMonitorFile() != null) {
                outputFile = scheduler.getArgs().getMonitorFile();
            }
        }

        if (jobId != -1)
            log.debug("Job ID detected: " + jobId);

        return new DefaultExecutionResult(processName, 0, output, outputFile, jobId);
    }

    @Override
    public ExecutionResult dispatch(String processName, String command, Scheduler scheduler)
            throws ProcessExecutionException, InterruptedException {

        // Actually for the moment we assume that this is only called for scheduled tasks.  Doesn't make much sense
        // as implemented right now for tasks running on the local host.
        return this.execute(processName, command, scheduler);
    }

    /**
     * Typically, a scheduler will queue a submitted job and let it execute in the background when resources are available.
     * This method allows the user to monitor an output file from the scheduler for this job and will parse each line as
     * it is added to the file.  This way we can monitor job progress and wait for it to complete in this method before
     * starting another job.  Once the job has completed the exit value is returned from this method, otherwise an exception
     * is thrown.
     *
     * @param processName The name of the process to monitor
     * @param processAdapter The ProcessAdapter which monitors process progress job.
     * @param processListener The ProcessListener which maintains process state.
     * @return An exit value describing the completion status of the job.
     * @throws uk.ac.ebi.fgpt.conan.service.exception.ProcessExecutionException
     *
     * @throws InterruptedException
     */
    protected ExecutionResult waitFor(String processName, ProcessAdapter processAdapter, ProcessListener processListener) throws ProcessExecutionException, InterruptedException {

        processAdapter.addTaskListener(processListener);

        // proc exit value, initialise to -1
        int exitValue = -1;

        // proc monitoring
        try {
            log.debug("Monitoring proc, waiting for completion...");
            exitValue = processListener.waitFor();
            log.debug("Process completed with exit value: " + exitValue);

            if (exitValue == 0) {
                return new DefaultExecutionResult(processName, exitValue, processAdapter.getProcessOutput(), processAdapter.getFile());
            } else {
                ProcessExecutionException pex = new ProcessExecutionException(exitValue);
                pex.setProcessOutput(processAdapter.getProcessOutput());
                pex.setProcessExecutionHost(processAdapter.getProcessExecutionHost());
                throw pex;
            }
        } finally {
            // this proc DID start, so only delete output files to cleanup if the proc actually exited,
            // and wasn't e.g. interrupted prior to completion
            /*if (exitValue != -1) {
                log.debug("Deleting " + adapter.getFile().getAbsolutePath());
                ProcessUtils.deleteFiles(adapter.getFile());
            } */
        }
    }
}
