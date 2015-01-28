package uk.ac.ebi.fgpt.conan.core.process;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import uk.ac.ebi.fgpt.conan.model.param.*;
import uk.ac.ebi.fgpt.conan.service.exception.ConanParameterException;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: maplesod
 * Date: 14/01/14
 * Time: 14:56
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractProcessArgs implements ProcessArgs {

    protected ProcessParams params;
    protected String uncheckedArgs;

    public AbstractProcessArgs(ProcessParams params) {

        this.params = params;
        this.uncheckedArgs = null;
    }

    protected abstract void setOptionFromMapEntry(ConanParameter param, String value);

    protected abstract void setArgFromMapEntry(ConanParameter param, String value);

    protected void setStdOutRedirectFromMapEntry(ConanParameter param, String value) {

    }

    protected void setStdErrRedirectFromMapEntry(ConanParameter param, String value) {

    }

    @Override
    public String getUncheckedArgs() {
        return uncheckedArgs;
    }

    public void setUncheckedArgs(String uncheckedArgs) {
        this.uncheckedArgs = uncheckedArgs;
    }

    @Override
    public void setFromArgMap(ParamMap pvp) throws IOException, ConanParameterException {

        for(ParamMapEntry entry : pvp.getOptionList()) {

            if (!entry.getKey().validateParameterValue(entry.getValue())) {
                throw new ConanParameterException("Option parameter invalid: " + entry.getKey().getIdentifier() + " : " + entry.getValue());
            }

            this.setOptionFromMapEntry(entry.getKey(), entry.getValue().trim());
        }

        for(ParamMapEntry entry : pvp.getArgList()) {

            if (!entry.getKey().validateParameterValue(entry.getValue())) {
                throw new ConanParameterException("Argument parameter invalid: " + entry.getKey().getIdentifier() + " : " + entry.getValue());
            }

            this.setArgFromMapEntry(entry.getKey(), entry.getValue().trim());
        }

        ParamMapEntry stdout = pvp.getStdOutRedirection();

        if (stdout != null) {
            if (!stdout.getKey().validateParameterValue(stdout.getValue())) {
                throw new ConanParameterException("Redirect parameter invalid: " + stdout.getKey().getIdentifier() + " : " + stdout.getValue());
            }

            this.setStdOutRedirectFromMapEntry(stdout.getKey(), stdout.getValue().trim());
        }

        ParamMapEntry stderr = pvp.getStdErrRedirection();

        if (stderr != null) {
            if (!stderr.getKey().validateParameterValue(stderr.getValue())) {
                throw new ConanParameterException("Redirect parameter invalid: " + stderr.getKey().getIdentifier() + " : " + stderr.getValue());
            }

            this.setStdErrRedirectFromMapEntry(stderr.getKey(), stderr.getValue().trim());
        }
    }

    @Override
    public void parse(String args) throws IOException {

        if (args == null || args.trim().isEmpty())
            return;

        String[] splitArgs = new String("exe " + args.trim()).split(" ");
        CommandLine cmdLine = null;
        try {
            cmdLine = new PosixParser().parse(this.params.createCommandLineOptions(), splitArgs);
        } catch (ParseException e) {
            throw new IOException(e);
        }

        if (cmdLine == null)
            return;

        this.parseCommandLine(cmdLine);
    }

    protected abstract void parseCommandLine(CommandLine commandLine);
}
