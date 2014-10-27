package uk.ac.ebi.fgpt.conan.model.context;

import java.util.List;

/**
 * Created by maplesod on 20/10/14.
 */
public interface TaskResult {

    /**
     * The name of this task
     * @return
     */
    String getTaskName();

    /**
     * Whether this task completed successfully or not
     * @return
     */
    boolean isSuccess();

    /**
     * The list of execution results for external processes executed during this task
     * @return
     */
    List<ExecutionResult> getProcessResults();

    /**
     * The total wall clock runtime for the entire task, includes external processes as well as time taken in this process.
     * @return
     */
    long getActualTotalRuntime();

    /**
     * The total wall clock runtime during the execution of external processes in this task
     * @return
     */
    long getTotalExternalRuntime();

    /**
     * The total CPU runtime during the execution of external processes in this task
     * @return
     */
    long getTotalExternalCputime();

    /**
     * The maximum memory usage during the task
     * @return
     */
    int getMaxMemUsage();

    /**
     * A report describing what occured during this task
     * @return
     */
    List<String> getOutput();
}
