/**
 * RAMPART - Robust Automatic MultiPle AssembleR Toolkit
 * Copyright (C) 2013  Daniel Mapleson - TGAC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package uk.ac.ebi.fgpt.conan.core.context.scheduler.pbs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fgpt.conan.core.context.scheduler.AbstractScheduler;
import uk.ac.ebi.fgpt.conan.model.context.ExitStatus;
import uk.ac.ebi.fgpt.conan.model.context.ResourceUsage;
import uk.ac.ebi.fgpt.conan.model.context.Scheduler;
import uk.ac.ebi.fgpt.conan.model.monitor.ProcessAdapter;
import uk.ac.ebi.fgpt.conan.util.StringJoiner;
import uk.ac.ebi.fgpt.conan.utils.ProcessRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PBSScheduler extends AbstractScheduler {

    private static Logger log = LoggerFactory.getLogger(PBSScheduler.class);

    public static final String QSUB = "qsub";
    public static final String ARG_SEPARATOR = ":";

    public PBSScheduler() {
        this(new PBSArgs());
    }

    public PBSScheduler(PBSArgs args) {
        super(QSUB, args);
    }

    @Override
    public ProcessAdapter createProcessAdapter(File monitorFile, int monitorInterval) {
        return null;
    }

    @Override
    public String createCommand(String internalCommand, boolean isForegroundJob) {

        // Create command to execute
        String commandPart = "echo \"" + internalCommand + "\"";

        // create PBS part
        StringJoiner pbsPartJoiner = new StringJoiner(" ");
        pbsPartJoiner.add(this.getSubmitCommand());
        pbsPartJoiner.add(this.getArgs() != null, "", ((PBSArgs)this.getArgs()).toString(isForegroundJob));

        String pbsPart = pbsPartJoiner.toString();

        return commandPart + " | " + pbsPart;
    }

    @Override
    public String createWaitCommand(String waitCondition) {

        // Create command to execute
        String commandPart = "echo \"sleep 1 2>&1\"";

        StringJoiner sj = new StringJoiner(" ");
        sj.add("echo \"sleep 1 2>&1\" | ");
        sj.add(this.getSubmitCommand());
        sj.add("-W block=true," + waitCondition);
        sj.add("-q", this.getArgs().getQueueName());
        sj.add("-eo", this.getArgs().getMonitorFile());
        sj.add("-l walltime=1");

        return sj.toString();
    }

    @Override
    public String createKillCommand(String jobId) {
        return "qdel " + jobId;
    }

    @Override
    public String createWaitCondition(ExitStatus.Type exitStatus, String condition) {
        return "depend=" + PBSExitStatus.select(exitStatus).getCommand() + ARG_SEPARATOR + condition;
    }

    @Override
    public String createWaitCondition(ExitStatus.Type exitStatus, List<Integer> jobIds) {

        StringJoiner condition = new StringJoiner(ARG_SEPARATOR);

        for(Integer jobId : jobIds) {
            condition.add(jobId.toString());
        }

        return "depend=" + PBSExitStatus.select(exitStatus).getCommand() + ARG_SEPARATOR + condition.toString();
    }

    @Override
    public Scheduler copy() {
        //TODO Not too nice... shouldn't really use casting here but it will always give the right result.  To tidy up late.
        return new PBSScheduler(new PBSArgs((PBSArgs) this.getArgs()));
    }

    @Override
    public String getName() {
        return "PBS";
    }

    @Override
    public boolean usesFileMonitor() {
        return false;
    }

    @Override
    public boolean generatesJobIdFromOutput() {
        return true;
    }

    @Override
    public int extractJobIdFromOutput(String line) {

        String[] parts = line.split("\\.");

        if (parts.length >= 2) {
            return Integer.parseInt(parts[0]);
        }

        throw new IllegalArgumentException("Could not extract PBS job id from: " + line);
    }

    @Override
    public String getJobIndexString() {
        return "${PBS_ARRAY_INDEX}";
    }

    @Override
    public ResourceUsage getResourceUsageFromMonitorFile(File file) throws IOException {
        return null;
    }

    @Override
    public ResourceUsage getResourceUsageFromId(int id) {

        try {
            // Run tracejob with ID
            ProcessRunner runner = new ProcessRunner();

            int n = 0;
            int nbLines = 0;
            boolean success = false;
            String[] output;

            do {

                output = runner.runCommmand("tracejob " + id);

                int res = this.waitLonger(output);

                if (res == -1) {
                    throw new IllegalStateException("Could not find any content from PBS tracejob command for job: " + id);
                }
                else if (res > 0) {

                    if (res < nbLines) {
                        throw new IllegalStateException("No progress from PBS tracejob command for job: " + id);
                    }

                    log.debug("Could not find enough content from tracejob for job " + id + " yet.  Waiting 5 seconds and trying again.");
                    Thread.sleep(5000);
                }
                else {
                    success = true;
                }

                nbLines = res;
                n++;

            } while(n <= 3 && !success);

            if (!success) {
                throw new IllegalStateException("Could not get any resource usage information from PBS tracejob command for job: " + id);
            }

            return this.parseTraceJobOutput(output);
        }
        catch (Exception e) {
            log.error("Encountered problem acquiring PBS resource usage information for job: " + id, e);
            return null;
        }
    }

    protected int waitLonger(String[] traceJobOut) {
        for(String line : traceJobOut) {

            if (line.length() > 27 && line.contains("resources_used")) {
                return 0;
            }
        }

        if (traceJobOut.length > 5) {
            return traceJobOut.length;
        }

        return -1;
    }

    protected ResourceUsage parseTraceJobOutput(String[] traceJobOut) {

        int maxMem = 0;
        long cpuTime = 0;
        long wallClock = 0;

        for(String line : traceJobOut) {

            if (line.length() > 27 && line.contains("resources_used")) {

                String resInfo = line.substring(26);
                String[] resArr = resInfo.split("\\s+");

                for (String res : resArr) {

                    if (res.startsWith("resources_used")) {
                        String data = res.substring(15);

                        String[] resDataParts = data.split("=");
                        String key = resDataParts[0];
                        String val = resDataParts[1];

                        if (key.equalsIgnoreCase("cput")) {
                            cpuTime = this.timeToSeconds(val);
                        } else if (key.equalsIgnoreCase("mem")) {
                            maxMem = Integer.parseInt(val.substring(0, val.length() - 2)) / 1000;
                        } else if (key.equalsIgnoreCase("walltime")) {
                            wallClock = this.timeToSeconds(val);
                        }
                    }
                }
            }
        }

        return new ResourceUsage(maxMem, wallClock, cpuTime);
    }

    private long timeToSeconds(String time) {

        String[] parts = time.split(":");

        long seconds = Long.parseLong(parts[2]);
        long minutes = Long.parseLong(parts[1]);
        long hours = Long.parseLong(parts[0]);

        return (((hours * 60) + minutes) * 60) + seconds;
    }

}
