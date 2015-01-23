package uk.ac.ebi.fgpt.conan.core.process;

import uk.ac.ebi.fgpt.conan.model.param.ProcessArgs;
import uk.ac.ebi.fgpt.conan.model.param.ProcessParams;
import uk.ac.ebi.fgpt.conan.service.ConanExecutorService;
import uk.ac.ebi.fgpt.conan.service.DefaultExecutorService;

import java.io.File;

/**
 * Created by maplesod on 13/01/15.
 */
public abstract class TypicalProcessArgs extends AbstractProcessArgs {

    protected File outputDir;
    protected String jobPrefix;
    protected int threads;
    protected boolean runParallel;

    public TypicalProcessArgs(TypicalProcessParams params) {
        super(params);

        this.outputDir = null;
        this.jobPrefix = "";
        this.threads = 1;
        this.runParallel = false;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public String getJobPrefix() {
        return jobPrefix;
    }

    public void setJobPrefix(String jobPrefix) {
        this.jobPrefix = jobPrefix;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public boolean isRunParallel() {
        return runParallel;
    }

    public void setRunParallel(boolean runParallel) {
        this.runParallel = runParallel;
    }
}
