package uk.ac.ebi.fgpt.conan.core.context;

import org.apache.commons.io.FileUtils;
import uk.ac.ebi.fgpt.conan.model.context.ExecutionResult;
import uk.ac.ebi.fgpt.conan.model.context.ResourceUsage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: maplesod
 * Date: 17/09/13
 * Time: 14:13
 * To change this template use File | Settings | File Templates.
 */
public class DefaultExecutionResult implements ExecutionResult {

    private String name;
    private int exitCode;
    private String[] output;
    private File outputFile;
    private int jobId;
    private ResourceUsage ru;

    public DefaultExecutionResult(String name, int exitCode) {
        this(name, exitCode, new String[]{}, null);
    }

    public DefaultExecutionResult(String name, int exitCode, String[] output, File outputFile) {
        this(name, exitCode, output, outputFile, -1);
    }

    public DefaultExecutionResult(String name, int exitCode, String[] output, File outputFile, int jobId) {
        this(name, exitCode, output, outputFile, jobId, null);
    }

    public DefaultExecutionResult(String name, int exitCode, String[] output, File outputFile, int jobId, ResourceUsage ru) {
        this.name = name;
        this.exitCode = exitCode;
        this.output = output;
        this.outputFile = outputFile;
        this.jobId = jobId;
        this.ru = ru;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getExitCode() {
        return this.exitCode;
    }

    @Override
    public String[] getOutput() {
        return this.output;
    }

    @Override
    public File getOutputFile() {
        return this.outputFile;
    }

    @Override
    public int getJobId() {
        return this.jobId;
    }

    @Override
    public ResourceUsage getResourceUsage() {
        return ru;
    }

    @Override
    public void setResourceUsage(ResourceUsage resourceUsage) {
        this.ru = resourceUsage;
    }

    public String getFirstOutputLine() {
        return output.length > 0 ? output[0] : null;
    }

    public void writeOutputToFile(File outputFile) throws IOException {
        FileUtils.writeLines(outputFile, Arrays.asList(this.output));
        this.outputFile = outputFile;
    }

    @Override
    public String toString() {
        return this.name + "\t" + this.exitCode + "\t" + this.jobId + "\t" + (this.ru != null ? this.ru.toString() : new ResourceUsage(0, 0, 0).toString());
    }
}
