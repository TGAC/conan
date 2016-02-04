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
package uk.ac.ebi.fgpt.conan.core.context.scheduler.slurm;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fgpt.conan.core.context.scheduler.AbstractScheduler;
import uk.ac.ebi.fgpt.conan.core.context.scheduler.pbs.PBSArgs;
import uk.ac.ebi.fgpt.conan.core.context.scheduler.pbs.PBSExitStatus;
import uk.ac.ebi.fgpt.conan.model.context.ExitStatus;
import uk.ac.ebi.fgpt.conan.model.context.ResourceUsage;
import uk.ac.ebi.fgpt.conan.model.context.Scheduler;
import uk.ac.ebi.fgpt.conan.model.monitor.ProcessAdapter;
import uk.ac.ebi.fgpt.conan.util.StringJoiner;
import uk.ac.ebi.fgpt.conan.utils.ProcessRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlurmScheduler extends AbstractScheduler {

    private static Logger log = LoggerFactory.getLogger(SlurmScheduler.class);

    public static final String SBATCH = "sbatch";
    public static final String ARG_SEPARATOR = ":";

    public SlurmScheduler() {
        this(new SlurmArgs());
    }

    public SlurmScheduler(SlurmArgs args) {
        super(SBATCH, args);
    }

    @Override
    public ProcessAdapter createProcessAdapter(File monitorFile, int monitorInterval) {
        return null;
    }

    @Override
    public String createCommand(String internalCommand, boolean isForegroundJob) {

        if (isForegroundJob) {
            String commandPart = "echo -e \"" + internalCommand + "\"";

            // create PBS part
            StringJoiner slurmPartJoiner = new StringJoiner(" ");
            slurmPartJoiner.add("salloc");
            slurmPartJoiner.add(this.getArgs() != null, "", ((SlurmArgs)this.getArgs()).toString());

            String slurmPart = slurmPartJoiner.toString();

            return commandPart + " | " + slurmPart;
        }
        else {
            StringJoiner slurmPartJoiner = new StringJoiner(" ");
            slurmPartJoiner.add("sbatch");
            slurmPartJoiner.add(this.getArgs() != null, "", ((SlurmArgs) this.getArgs()).toString());
            slurmPartJoiner.add("--wrap=\"" + internalCommand + "\"");

            return slurmPartJoiner.toString();
        }


    }

    @Override
    public String createWaitCommand(String waitCondition) {

        // Create command to execute
        StringJoiner sj = new StringJoiner(" ");
        sj.add("srun");
        sj.add("-d" + waitCondition);
        sj.add("-p", this.getArgs().getQueueName());
        sj.add("sleep 1");

        return sj.toString();
    }

    @Override
    public String createKillCommand(String jobId) {
        return "scancel " + jobId;
    }

    @Override
    public String createWaitCondition(ExitStatus.Type exitStatus, String condition) {
        return PBSExitStatus.select(exitStatus).getCommand() + ARG_SEPARATOR + condition;
    }

    @Override
    public String createWaitCondition(ExitStatus.Type exitStatus, List<Integer> jobIds) {

        StringJoiner condition = new StringJoiner(ARG_SEPARATOR);

        for(Integer jobId : jobIds) {
            condition.add(jobId.toString());
        }

        return PBSExitStatus.select(exitStatus).getCommand() + ARG_SEPARATOR + condition.toString();
    }

    @Override
    public Scheduler copy() {
        //TODO Not too nice... shouldn't really use casting here but it will always give the right result.  To tidy up late.
        return new SlurmScheduler(new SlurmArgs((SlurmArgs) this.getArgs()));
    }

    @Override
    public String getName() {
        return "SLURM";
    }

    @Override
    public boolean usesFileMonitor() {
        return false;
    }

    @Override
    public boolean generatesJobIdFromOutput() {
        return false;
    }

    @Override
    public boolean generatesJobIdFromError() {
        return true;
    }


    @Override
    public int extractJobIdFromOutput(String line) {

        String[] parts = line.split(" ");

        if (parts.length == 4) {
            return Integer.parseInt(parts[3]);
        }
        else if (parts.length == 5) {
            return Integer.parseInt(parts[4]);
        }
        else {
            return -1;
        }

//        throw new IllegalArgumentException("Could not extract SLURM job id from: " + line);
    }

    @Override
    public String getJobIndexString() {
        return "${SLURM_ARRAY_TASK_ID}";
    }

    @Override
    public ResourceUsage getResourceUsageFromMonitorFile(File file) throws IOException {
        return null;
    }

    @Override
    public ResourceUsage getResourceUsageFromId(int id) {

        // Run tracejob with ID
        ProcessRunner runner = new ProcessRunner();

        String[] output = null;
        try {
            output = runner.runCommmand("sacct --format=CPUTime,MaxRSS,Elapsed -j " + id);

            if (output.length < 3) {
                throw new IllegalStateException("Could not find any resource content from SLURM job: " + id);
            }

            return this.parseTraceJobOutput(output);
        }
        catch (Exception e) {
            log.error("Encountered problem acquiring SLURM resource usage information for job: " + id, e);
            return null;
        }
    }


    protected ResourceUsage parseTraceJobOutput(String[] traceJobOut) {

        String line = traceJobOut[traceJobOut.length - 1].trim();

        String[] parts = line.split("\\s+");

        if (parts.length == 3) {
            int maxMem = Integer.parseInt(parts[1].substring(0, parts[1].length() - 1)) / 1000;
            long cpuTime = timeToSeconds(parts[0]);
            long wallClock = timeToSeconds(parts[2]);

            return new ResourceUsage(maxMem, wallClock, cpuTime);
        }
        else if (parts.length == 2) {
            long cpuTime = timeToSeconds(parts[0]);
            long wallClock = timeToSeconds(parts[1]);

            return new ResourceUsage(0, wallClock, cpuTime);
        }
        else {
            throw new IllegalStateException("Unexpected results from sacct");
        }
    }

    private long timeToSeconds(String time) {

        String[] parts = time.split(":");

        long seconds = Long.parseLong(parts[2]);
        long minutes = Long.parseLong(parts[1]);
        long hours = Long.parseLong(parts[0]);

        return (((hours * 60) + minutes) * 60) + seconds;
    }

}
