package uk.ac.ebi.fgpt.conan.core.process;

import uk.ac.ebi.fgpt.conan.core.param.ArgValidator;
import uk.ac.ebi.fgpt.conan.core.param.ParameterBuilder;
import uk.ac.ebi.fgpt.conan.model.param.AbstractProcessParams;
import uk.ac.ebi.fgpt.conan.model.param.ConanParameter;

/**
 * Created by maplesod on 13/01/15.
 */
public abstract class TypicalProcessParams extends AbstractProcessParams {

    private ConanParameter outputDir;
    private ConanParameter jobPrefix;
    private ConanParameter threads;
    private ConanParameter runParallel;

    public TypicalProcessParams() {

        this.outputDir = new ParameterBuilder()
                .shortName("o")
                .longName("output_dir")
                .isOptional(false)
                .description("The directory in which to output content")
                .argValidator(ArgValidator.PATH)
                .create();

        this.jobPrefix = new ParameterBuilder()
                .longName("job_prefix")
                .description("The job prefix to apply to all child jobs")
                .argValidator(ArgValidator.DEFAULT)
                .create();

        this.threads = new ParameterBuilder()
                .shortName("t")
                .longName("threads")
                .argValidator(ArgValidator.DIGITS)
                .create();

        this.runParallel = new ParameterBuilder()
                .longName("parallel")
                .isFlag(true)
                .argValidator(ArgValidator.OFF)
                .create();
    }

    public ConanParameter getOutputDir() {
        return outputDir;
    }

    public ConanParameter getJobPrefix() {
        return jobPrefix;
    }

    public ConanParameter getThreads() {
        return threads;
    }

    public ConanParameter getRunParallel() {
        return runParallel;
    }

    @Override
    public ConanParameter[] getConanParametersAsArray() {
        return new ConanParameter[] {
                this.outputDir,
                this.jobPrefix,
                this.threads,
                this.runParallel
        };
    }
}
