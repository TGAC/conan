package uk.ac.ebi.fgpt.conan.model.context;

import java.util.List;

/**
 * Created by maplesod on 23/10/14.
 */
public class MultiWaitResult {

    private ExecutionResult waitResult;
    private List<ExecutionResult> dependentResults;

    public MultiWaitResult(ExecutionResult waitResult, List<ExecutionResult> dependentResults) {
        this.waitResult = waitResult;
        this.dependentResults = dependentResults;
    }

    public ExecutionResult getWaitResult() {
        return waitResult;
    }

    public List<ExecutionResult> getDependentResults() {
        return dependentResults;
    }
}
