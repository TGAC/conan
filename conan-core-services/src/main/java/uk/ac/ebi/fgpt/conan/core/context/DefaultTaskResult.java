package uk.ac.ebi.fgpt.conan.core.context;

import uk.ac.ebi.fgpt.conan.model.context.ExecutionResult;
import uk.ac.ebi.fgpt.conan.model.context.TaskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maplesod on 20/10/14.
 */
public class DefaultTaskResult implements TaskResult {

    private List<ExecutionResult> processResults;
    private long actualTotalRuntime;
    private boolean success;
    private String taskName;

    public DefaultTaskResult(String taskName, boolean success, List<ExecutionResult> processResults, long actualTotalRuntime) {
        this.taskName = taskName;
        this.success = success;
        this.processResults = processResults;
        this.actualTotalRuntime = actualTotalRuntime;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public boolean isAllSubTasksSuccess() {

        for(ExecutionResult res : this.processResults) {
            if (res.getExitCode() != 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<ExecutionResult> getProcessResults() {
        return processResults;
    }

    @Override
    public long getActualTotalRuntime() {
        return actualTotalRuntime;
    }

    @Override
    public long getTotalExternalRuntime() {

        if (this.processResults == null || this.processResults.isEmpty() || this.processResults.get(0).getResourceUsage() == null) {
            return 0L;
        }

        long runtime = 0;

        for(ExecutionResult res : this.processResults) {
            runtime += res.getResourceUsage().getRunTime();
        }

        return runtime;
    }

    @Override
    public long getTotalExternalCputime() {

        if (this.processResults == null || this.processResults.isEmpty() || this.processResults.get(0).getResourceUsage() == null) {
            return 0L;
        }

        long runtime = 0;

        for(ExecutionResult res : this.processResults) {
            runtime += res.getResourceUsage().getCpuTime();
        }

        return runtime;
    }

    @Override
    public int getMaxMemUsage() {

        if (this.processResults == null || this.processResults.isEmpty() || this.processResults.get(0).getResourceUsage() == null) {
            return 0;
        }

        int maxMem = 0;

        for(ExecutionResult res : this.processResults) {
            maxMem = Math.max(maxMem, res.getResourceUsage().getMaxMem());
        }

        return maxMem;
    }

    @Override
    public List<String> getOutput() {

        List<String> lines = new ArrayList<>();

        lines.add("Runtimes and resource usage for task: " + this.taskName);
        lines.add("  Total wall clock runtime (s): " + this.actualTotalRuntime);
        lines.add("  Total CPU time from combined sub-processes (s): " + this.getTotalExternalCputime());
        lines.add("  Max memory usage of uncombined sub-processes (MB): " + this.getMaxMemUsage());
        lines.add("");
        lines.add("Number of sub-processes executed for this task: " + this.processResults.size());
        lines.add("Breakdown of sub-processes:");
        lines.add("Name\tExitCode\tJobID\tMaxMem(MB)\tWallClock(s)\tCPUTime(s)");
        for(ExecutionResult result : this.processResults) {
            lines.add(result.toString());
        }

        return lines;
    }
}
