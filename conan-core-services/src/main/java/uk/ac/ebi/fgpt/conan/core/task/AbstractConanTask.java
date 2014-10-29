package uk.ac.ebi.fgpt.conan.core.task;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fgpt.conan.core.context.DefaultExecutionContext;
import uk.ac.ebi.fgpt.conan.core.context.DefaultTaskResult;
import uk.ac.ebi.fgpt.conan.core.process.DefaultProcessRun;
import uk.ac.ebi.fgpt.conan.model.ConanPipeline;
import uk.ac.ebi.fgpt.conan.model.ConanProcess;
import uk.ac.ebi.fgpt.conan.model.ConanProcessRun;
import uk.ac.ebi.fgpt.conan.model.ConanTask;
import uk.ac.ebi.fgpt.conan.model.context.ExecutionContext;
import uk.ac.ebi.fgpt.conan.model.context.ExecutionResult;
import uk.ac.ebi.fgpt.conan.model.param.ConanParameter;
import uk.ac.ebi.fgpt.conan.service.exception.ConanParameterException;
import uk.ac.ebi.fgpt.conan.service.exception.ProcessExecutionException;
import uk.ac.ebi.fgpt.conan.service.exception.TaskExecutionException;

import java.io.File;
import java.util.*;

/**
 * An abstract implementation of a {@link uk.ac.ebi.fgpt.conan.model.ConanTask} that contains execute() implementations
 * and some useful protected methods for firing status events.  Concrete implementations should define how to construct
 * and manipulate each task.  This task implementation is designed to support the addition of listeners to report on
 * changes in each task.
 *
 * @author Tony Burdett
 * @date 13-Oct-2010
 */
public abstract class AbstractConanTask<P extends ConanPipeline> implements ConanTask<P> {

    private static Logger log = LoggerFactory.getLogger(AbstractConanTask.class);

    // tracks the current task being executed, always needs to be set
    protected int firstTaskIndex;
    protected int currentExecutionIndex;
    protected String ID;

    // dates for tracking start and end of task
    protected Date creationDate;
    protected Date submissionDate;
    protected Date startDate;
    protected Date completionDate;

    // tracks the processes that are run
    protected List<ConanProcessRun> processRuns;

    // status flags
    protected State currentState;
    protected String statusMessage;
    protected boolean submitted;
    protected boolean paused;

    // listeners
    private Set<ConanTaskListener> listeners;

    protected AbstractConanTask(int firstTaskIndex) {
        // set the index of the task we will start with
        this.firstTaskIndex = 0;
        this.currentExecutionIndex = firstTaskIndex;

        // date of creation
        this.creationDate = new Date();

        // set status tracking
        this.processRuns = new ArrayList<ConanProcessRun>();

        // add listeners and make sure task isn't paused
        this.listeners = new HashSet<ConanTaskListener>();
        this.paused = false;

        // finally set our initial state
        updateCurrentState(State.CREATED);
        updateCurrentStatusMessage("Task created");
    }

    public boolean addConanTaskListener(ConanTaskListener listener) {
        return listeners.add(listener);
    }

    public boolean removeConanTaskListener(ConanTaskListener listener) {
        return listeners.remove(listener);
    }

    public void setId(String ID) {
        this.ID = ID;
    }

    public String getId() {
        return this.ID;
    }

    public ConanProcess getFirstProcess() {
        if (firstTaskIndex < getPipeline().getProcesses().size()) {
            return getPipeline().getProcesses().get(firstTaskIndex);
        } else {
            return null;
        }
    }

    public synchronized ConanProcess getLastProcess() {
        if (currentExecutionIndex > firstTaskIndex && currentExecutionIndex <= getPipeline().getProcesses().size()) {
            return getPipeline().getProcesses().get(currentExecutionIndex - 1);
        } else {
            return null;
        }
    }

    public synchronized ConanProcess getCurrentProcess() {
        if (getCurrentState() == State.RUNNING && currentExecutionIndex < getPipeline().getProcesses().size()) {
            return getPipeline().getProcesses().get(currentExecutionIndex);
        } else {
            return null;
        }
    }

    public synchronized ConanProcess getNextProcess() {
        if (getCurrentState() == State.RUNNING) {
            if ((currentExecutionIndex + 1) < getPipeline().getProcesses().size()) {
                return getPipeline().getProcesses().get(currentExecutionIndex + 1);
            } else {
                return null;
            }
        } else {
            if (currentExecutionIndex < getPipeline().getProcesses().size()) {
                return getPipeline().getProcesses().get(currentExecutionIndex);
            } else {
                return null;
            }
        }
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getSubmissionDate() {
        return submissionDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public List<ConanProcessRun> getConanProcessRuns() {
        return processRuns;
    }

    public List<ConanProcessRun> getConanProcessRunsForProcess(ConanProcess process)
            throws IllegalArgumentException {
        if (getPipeline().getProcesses().contains(process)) {
            List<ConanProcessRun> results = new ArrayList<ConanProcessRun>();
            for (ConanProcessRun run : getConanProcessRuns()) {
                if (run.getProcessName().equals(process.getName())) {
                    results.add(run);
                }
            }
            return results;
        } else {
            throw new IllegalArgumentException("The process '" + process.getName() + "' " +
                    "is not part of the pipeline for task '" + getId() + "'");
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public DefaultTaskResult execute(ExecutionContext executionContext) throws TaskExecutionException, InterruptedException {

        // check the current state for execution
        checkState();

        StopWatch stopWatchTotal = new StopWatch();
        stopWatchTotal.start();

        List<ExecutionResult> results = new ArrayList<>();

        log.info("Executing task '" + getId() + "'");

        try {

            // do processes in order
            while (!isPaused() && getCurrentProcess() != null) {
                if (Thread.interrupted()) {
                    // this thread has been interrupted by a shutdown request, so stop executing
                    throw new InterruptedException();
                }

                StopWatch stopWatchProcess = new StopWatch();
                stopWatchProcess.start();

                String processName = getCurrentProcess().getName();
                String pipelineName = getId();

                // extract only those parameters we need
                Map<ConanParameter, String> nextProcessParams = new HashMap<ConanParameter, String>();
                for (ConanParameter param : getCurrentProcess().getParameters()) {
                    nextProcessParams.put(param, getParameterValues().get(param));
                }

                // increment the execution index and fire an event as we're about to start
                log.debug("Process being executed for task '" + pipelineName + "' is '" +
                        processName + "', supplying parameters: " +
                        nextProcessParams);

                // Notify that event has started
                fireProcessStartedEvent();

                // Ensure there is a valid path for the monitor file if required
                if (executionContext.usingScheduler() && executionContext.getMonitorFile() == null) {

                    String jobName = this.getName() + "_" + this.currentExecutionIndex + "_" + getCurrentProcess().getName();

                    if (executionContext.getJobName() == null || executionContext.getJobName().isEmpty()) {
                        executionContext.getScheduler().getArgs().setJobName(jobName);
                    }

                    executionContext.getScheduler().getArgs().setMonitorFile(new File(jobName + ".log"));
                }

                // now execute
                ExecutionResult result = getCurrentProcess().execute(nextProcessParams, executionContext);
                results.add(result);

                // once finished, update the end date
                fireProcessEndedEvent();

                stopWatchProcess.stop();
                log.debug("Process '" + (processName == null ? "?" : processName) + "' runtime: " + stopWatchProcess.toString());
            }

            stopWatchTotal.stop();

            // finalise task execution
            return new DefaultTaskResult(this.ID, checkExitStatus(), results, stopWatchTotal.getTime() / 1000);
        }
        catch (ConanParameterException e) {
            log.error("Process '" + getCurrentProcess().getName() + "' did not start due to invalid parameters");
            fireProcessFailedEvent(2);
            throw new TaskExecutionException(e);
        }
        catch (ProcessExecutionException e) {
            // log this exception
            log.error("Process '" + getCurrentProcess().getName() + "' failed to execute, " +
                    "exit code: " + e.getExitValue());
            log.error("Execution exception follows", e);
            log.debug("Is this event to abort: " + e.causesAbort() + " Output: " + e.getProcessOutput());
            if (e.causesAbort()) {
                // critical fail, should cause instant abort
                fireProcessFailedEvent(e);
                abort();
            }
            else {
                fireProcessFailedEvent(e);
            }
            throw new TaskExecutionException(e);
        }
        catch (InterruptedException e) {
            // log this exception
            log.error("Executing process '" + getCurrentProcess().getName() + "' was interrupted", e);
            fireProcessInterruptedEvent();
            throw e;
        }
        catch (RuntimeException e) {
            // log this exception
            log.error("An unexpected runtime exception occurred whilst executing task '" + getId() + "'", e);
            log.error("Process '" + getCurrentProcess().getName() + "' failed to execute");
            fireProcessFailedEvent(1);
            throw new TaskExecutionException(e);
        }
        finally {
            // finally, if we have completed or stopped, remove all listeners so this object is dereferenced
            if (getCurrentState() == ConanTask.State.COMPLETED || getCurrentState() == ConanTask.State.ABORTED) {
                setListeners(Collections.<ConanTaskListener>emptySet());
            }

            log.info("Task '" + getId() + "' execution ended.  Runtime: " + stopWatchTotal.toString());
        }
    }

    public DefaultTaskResult execute() throws TaskExecutionException, InterruptedException {
        return this.execute(new DefaultExecutionContext());
    }

    public void submit() {
        // resubmitting does nothing
        if (!isSubmitted()) {
            this.submitted = true;
            fireTaskSubmittedEvent();
        }
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void pause() {
        // set paused flag
        log.debug("Pausing task '" + getName() + "'");
        this.paused = true;
    }

    public boolean isPaused() {
        log.trace("Checking paused status of task '" + getName() + "', " + (paused ? "paused" : "not paused"));
        return paused;
    }

    public void resume() {
        // just reset paused flag
        log.debug("Resuming task '" + getName() + "', no longer paused");
        this.paused = false;
    }

    public void retryLastProcess() {
        // wind execution index back one
        currentExecutionIndex--;
        // and reset paused flag
        log.debug("Retrying task '" + getName() + "', no longer paused");
        this.paused = false;
    }

    public void restart() {
        throw new UnsupportedOperationException("Tasks cannot currently be completely restarted");
    }

    public void abort() {
        fireTaskAbortedEvent();
    }

    protected void checkState() throws TaskExecutionException {
        log.debug("Checking current state of task '" + getId() + "': " + getCurrentState());

        if (getCurrentState() == ConanTask.State.ABORTED) {
            // stopped tasks must never re-execute
            throw new TaskExecutionException("This task has been aborted, so will not execute.");
        }
        else if (getCurrentState().compareTo(State.SUBMITTED) < 0) {
            // this is task hasn't ever been submitted
            throw new TaskExecutionException("Task does not appear to have ever been submitted");
        }
        else if (getCurrentState() == State.RUNNING) {
            // this task was recovered after a shutdown, so we can start executing again
            fireTaskRecoveryEvent();
        }
        else {
            // has been submitted but not running, probably paused or failed, so start this task
            fireTaskStartedEvent();
        }
    }

    protected boolean checkExitStatus() {
        // was this event paused by an intervention?
        if (isPaused()) {
            // we paused, so we didn't complete everything successfully
            fireTaskPausedEvent();
            return false;
        }
        else {
            // we didn't pause, so we either got to the end or were interrupted
            if (Thread.currentThread().isInterrupted()) {
                // we were interrupted, so we didn't complete successfully
                log.warn("Task '" + getId() + "' is terminating following an interrupt request " +
                        "to thread '" + Thread.currentThread().getName() + "'");
                return false;
            }
            else {
                // if we didn't pause this task, we got to the end, so flag it as complete
                fireTaskCompletedEvent();
                return true;
            }
        }
    }

    protected void updateCurrentState(State currentState) {
        this.currentState = currentState;
    }

    protected void updateCurrentStatusMessage(String currentStatusMessage) {
        this.statusMessage = currentStatusMessage;
    }

    protected void fireTaskSubmittedEvent() {
        log.debug("Task '" + getId() + "' submitted");
        updateCurrentState(State.SUBMITTED);
        updateCurrentStatusMessage("Submitted");

        this.submissionDate = new Date();
        ConanTaskEvent event = new ConanTaskEvent(this, getCurrentState(), getFirstProcess(), null);
        for (ConanTaskListener listener : getListeners()) {
            listener.stateChanged(event);
        }

    }

    protected void fireTaskStartedEvent() {
        if (getCurrentState() == State.SUBMITTED) {
            updateCurrentStatusMessage("Started");
            this.startDate = new Date();
        }
        else {
            updateCurrentStatusMessage("Restarted");
        }

        log.debug("Task '" + getId() + "' started");
        updateCurrentState(State.RUNNING);

        ConanTaskEvent event = new ConanTaskEvent(this, getCurrentState(), getFirstProcess(), null);
        for (ConanTaskListener listener : getListeners()) {
            listener.stateChanged(event);
        }
    }

    protected void fireTaskRecoveryEvent() {
        log.debug("Task '" + getId() + "' recovered successfully");
        updateCurrentState(State.RECOVERED);
        updateCurrentStatusMessage("Recovered");

        ConanTaskEvent event = new ConanTaskEvent(
                this, getCurrentState(),
                getNextProcess(),
                processRuns.get(processRuns.size() - 1));
        for (ConanTaskListener listener : getListeners()) {
            listener.stateChanged(event);
        }
    }

    protected void fireTaskPausedEvent() {
        log.debug("Task '" + getId() + "' paused");
        updateCurrentState(State.PAUSED);
        if (getNextProcess() == null) {
            updateCurrentStatusMessage("Paused during the last process");
        }
        else {
            updateCurrentStatusMessage("Paused before '" + getNextProcess().getName() + "'");
        }

        ConanTaskEvent event =
                new ConanTaskEvent(this, getCurrentState(), null, processRuns.get(processRuns.size() - 1));
        for (ConanTaskListener listener : getListeners()) {
            listener.stateChanged(event);
        }
    }

    protected void fireTaskCompletedEvent() {
        log.debug("Task '" + getId() + "' completed");
        updateCurrentState(State.COMPLETED);
        updateCurrentStatusMessage("Complete");

        this.completionDate = new Date();
        ConanTaskEvent event = new ConanTaskEvent(this, getCurrentState(), null, null);
        for (ConanTaskListener listener : getListeners()) {
            listener.stateChanged(event);
        }
        log.debug("Listeners notified of task completion, so will now be deregistered");

        // finally, remove any listeners as this task is complete
        getListeners().clear();
    }

    protected void fireTaskAbortedEvent() {
        log.debug("Task '" + getId() + "' was aborted!");
        updateCurrentState(State.ABORTED);
        if (getLastProcess() == null) {
            updateCurrentStatusMessage("Aborted before the first process started");
        }
        else {
            updateCurrentStatusMessage("Aborted after '" + getLastProcess().getName() + "'");
        }

        this.completionDate = new Date();
        ConanTaskEvent event;
        if (processRuns.isEmpty()) {
            event = new ConanTaskEvent(this, getCurrentState(), null, null);
        }
        else {
            event = new ConanTaskEvent(this, getCurrentState(), null, processRuns.get(processRuns.size() - 1));
        }
        for (ConanTaskListener listener : getListeners()) {
            listener.stateChanged(event);
        }
    }

    protected void fireProcessStartedEvent() {
        log.debug("Task '" + getId() + "' is commencing next process, '" + getCurrentProcess().getName() + "' " +
                "(execution index = " + currentExecutionIndex + ")");
        updateCurrentState(State.RUNNING);

        // create our process run object for the process we're going to execute
        DefaultProcessRun pr = new DefaultProcessRun(getCurrentProcess().getName(), getSubmitter());
        processRuns.add(pr);
        pr.setStartDate(new Date());

        updateCurrentStatusMessage("Doing '" + getCurrentProcess().getName() + "'");
        ConanTaskEvent event = new ConanTaskEvent(this, getCurrentState(), getCurrentProcess(), pr);
        for (ConanTaskListener listener : getListeners()) {
            listener.processStarted(event);
        }
    }

    protected void fireProcessEndedEvent() {
        log.debug("Task '" + getId() + "' finished its current process");
        updateCurrentStatusMessage("Finished '" + getCurrentProcess().getName() + "'");

        // increment the execution index
        currentExecutionIndex++;

        // get the last process run object, and set it's end date
        DefaultProcessRun pr = (DefaultProcessRun) processRuns.get(processRuns.size() - 1);
        pr.setEndDate(new Date());
        pr.setExitValue(0);
        pr.setErrorMessage(null);

        ConanTaskEvent event = new ConanTaskEvent(this, getCurrentState(), getLastProcess(), pr);
        for (ConanTaskListener listener : getListeners()) {
            listener.processEnded(event);
        }
    }

    protected void fireProcessFailedEvent(ProcessExecutionException pex) {
        updateCurrentStatusMessage("Failed at '" + getCurrentProcess().getName() + "'");
        updateCurrentState(State.FAILED);

        // increment the execution index
        currentExecutionIndex++;

        // get the last process run object, and set it's end date
        DefaultProcessRun pr = (DefaultProcessRun) processRuns.get(processRuns.size() - 1);
        pr.setEndDate(new Date());
        pr.setExitValue(pex.getExitValue());
        pr.setErrorMessage(pex.getErrorMessage());

        ConanTaskEvent event =
                new ConanTaskEvent(this, getCurrentState(), getLastProcess(), pr, pex);
        for (ConanTaskListener listener : getListeners()) {
            listener.processFailed(event);
        }

        // log error output
        log.error("Task '" + getId() + "' failed its current process, exit code: " + pex.getExitValue());
        StringBuilder errorContent = new StringBuilder();
        errorContent.append("Output follows...\n");
        for (String s : pex.getProcessOutput()) {
            errorContent.append(s).append("\n");
        }
        log.error(errorContent.toString());
    }

    protected void fireProcessFailedEvent(int exitValue) {
        log.debug("Task '" + getId() + "' failed its current process, exit code: " + exitValue);
        updateCurrentStatusMessage("Failed at '" + getCurrentProcess().getName() + "'");
        updateCurrentState(State.FAILED);

        // increment the execution index
        currentExecutionIndex++;

        // get the last process run object, and set it's end date
        DefaultProcessRun pr = (DefaultProcessRun) processRuns.get(processRuns.size() - 1);
        pr.setEndDate(new Date());
        pr.setExitValue(exitValue);
        pr.setErrorMessage(null);

        ConanTaskEvent event =
                new ConanTaskEvent(this, getCurrentState(), getLastProcess(), pr);
        for (ConanTaskListener listener : getListeners()) {
            listener.processFailed(event);
        }
    }

    protected void fireProcessInterruptedEvent() {
        log.debug("Task '" + getId() + "' was interrupted during '" + getCurrentProcess().getName() + "'");
        updateCurrentStatusMessage("Killed at '" + getCurrentProcess().getName() + "'");
        updateCurrentState(State.FAILED);

        // increment the execution index
        currentExecutionIndex++;

        // get the last process run object, and set it's end date
        DefaultProcessRun pr = (DefaultProcessRun) processRuns.get(processRuns.size() - 1);
        pr.setEndDate(new Date());
        pr.setExitValue(1);
        pr.setErrorMessage(null);

        ConanTaskEvent event =
                new ConanTaskEvent(this, getCurrentState(), getLastProcess(), pr);
        for (ConanTaskListener listener : getListeners()) {
            listener.processFailed(event);
        }
    }


    private Set<ConanTaskListener> getListeners() {
        return listeners;
    }

    private void setListeners(Set<ConanTaskListener> listeners) {
        this.listeners = listeners;
    }
}
