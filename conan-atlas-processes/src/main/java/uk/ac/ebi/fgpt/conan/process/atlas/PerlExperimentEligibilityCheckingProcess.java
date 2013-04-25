package uk.ac.ebi.fgpt.conan.process.atlas;

import net.sourceforge.fluxion.spi.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fgpt.conan.ae.AccessionParameter;
import uk.ac.ebi.fgpt.conan.lsf.LSFProcess;
import uk.ac.ebi.fgpt.conan.model.ConanParameter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Conan process to check experiment for eligibility for
 * the Expression Atlas.
 *
 * @author Amy Tang
 * @date 25-Apr-2013
 */

@ServiceProvider
public class PerlExperimentEligibilityCheckingProcess extends AbstractAE2LSFProcess {
    private final Collection<ConanParameter> parameters;
    private final AccessionParameter accessionParameter;

    private Logger log = LoggerFactory.getLogger(getClass());

    public PerlExperimentEligibilityCheckingProcess() {
        parameters = new ArrayList<ConanParameter>();
        accessionParameter = new AccessionParameter();
        parameters.add(accessionParameter);
//        setQueueName("production");
    }

    protected Logger getLog() {
        return log;
    }


    public String getName() {
        return "atlas eligibility";
    }

    public Collection<ConanParameter> getParameters() {
        return parameters;
    }

    protected String getComponentName() {
        return LSFProcess.UNSPECIFIED_COMPONENT_NAME;
    }

    protected String getCommand(Map<ConanParameter, String> parameters) throws IllegalArgumentException {
        getLog().debug("Executing " + getName() + " with the following parameters: " + parameters.toString());

        // deal with parameters
        AccessionParameter accession = new AccessionParameter();
        accession.setAccession(parameters.get(accessionParameter));
        if (accession.getAccession() == null) {
            throw new IllegalArgumentException("Accession cannot be null");
        }
        else {
            // main command to execute perl script
            
            /* Writing back to productio Subs Tracking database, will implement later
             *  String mainCommand = "cd " + accession.getFile().getParentFile().getAbsolutePath() + "; " +
             *         "perl /ebi/microarray/home/fgpt/sw/lib/perl/Red_Hat/check_atlas_eligiblity.pl -w -a " +
             *       accession.getAccession();
             */
            
            String mainCommand = "cd " + accession.getFile().getParentFile().getAbsolutePath() + "; " +
                    "perl /ebi/microarray/home/fgpt/sw/lib/perl/Red_Hat/check_atlas_eligiblity.pl";

            // path to relevant file
            String filePath = accession.getFile().getAbsolutePath();
            // return command string
            if (accession.isExperiment()) {
                return mainCommand + "-i " + filePath;
            }
        }
    }

    protected String getLSFOutputFilePath(Map<ConanParameter, String> parameters)
            throws IllegalArgumentException {
        // deal with parameters
        AccessionParameter accession = new AccessionParameter();
        accession.setAccession(parameters.get(accessionParameter));
        if (accession.getAccession() == null) {
            throw new IllegalArgumentException("Accession cannot be null");
        }
        else {
            // get the mageFile parent directory
            final File parentDir = accession.getFile().getAbsoluteFile().getParentFile();

            // files to write output to
            final File outputDir = new File(parentDir, ".conan");

            // lsf output file
            return new File(outputDir, "atlas_eligibility.lsfoutput.txt").getAbsolutePath();
        }
    }
}
