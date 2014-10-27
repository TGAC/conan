package uk.ac.ebi.fgpt.conan.model.context;

import java.io.File;
import java.io.IOException;


public interface ExecutionResult {

    void setName(String name);

    String getName();

    int getExitCode();

    String[] getOutput();

    File getOutputFile();

    int getJobId();

    ResourceUsage getResourceUsage();

    void setResourceUsage(ResourceUsage resourceUsage);

    String getFirstOutputLine();

    void writeOutputToFile(File outputFile) throws IOException;

    String toString();
}
