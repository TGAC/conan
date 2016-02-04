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

import uk.ac.ebi.fgpt.conan.model.context.SchedulerArgs;
import uk.ac.ebi.fgpt.conan.util.StringJoiner;

import java.io.File;

public class SlurmArgs extends SchedulerArgs {

    public SlurmArgs() {
        super();
    }

    public SlurmArgs(SlurmArgs args) {
        super(args);
    }


    @Override
    public String toString() {

        return this.toString(true);
    }

    public String toString(boolean isForegroundJob) {

        StringJoiner joiner = new StringJoiner(" ");

        if (this.getJobArrayArgs() == null) {

            if (this.getMonitorFile() != null && !isForegroundJob) {
                joiner.add("-o", new File(this.getMonitorFile().getParentFile(), this.getMonitorFile().getName() + ".stdout"));
                joiner.add("-e", new File(this.getMonitorFile().getParentFile(), this.getMonitorFile().getName() + ".stderr"));
            }
        }
        else {

            JobArrayArgs ja = this.getJobArrayArgs();
            StringBuilder sb = new StringBuilder();
            sb.append(ja.getMinIndex()).append("-").append(ja.getMaxIndex());

            if (ja.getStepIndex() > 1) {
                sb.append(":").append(ja.getStepIndex());
            }

            if (ja.getMaxSimultaneousJobs() > 1) {
                sb.append("%").append(ja.getMaxSimultaneousJobs());
            }

            joiner.add("-a", sb.toString());

            if (this.getMonitorFile() != null && !isForegroundJob) {
                joiner.add("-o", new File(this.getMonitorFile().getParentFile(), this.getMonitorFile().getName() + ".%a.stdout"));
                joiner.add("-e", new File(this.getMonitorFile().getParentFile(), this.getMonitorFile().getName() + ".%a.stderr"));
            }
        }

        joiner.add("-N ", "1");
        joiner.add(this.getThreads() > 1, "-n ", Integer.toString(this.getThreads()));
        joiner.add("-p ", this.getQueueName());
        joiner.add("-J ", this.getJobName());
        joiner.add("-D ", "$(pwd)");
        joiner.add("--profile=all");
        joiner.add(this.getMemoryMB() > 0, "", "--mem=" + Integer.toString(this.getMemoryMB()));
        joiner.add(this.getEstimatedRuntimeMins() > 0, "-t", Integer.toString(this.getEstimatedRuntimeMins()));
        joiner.add(this.getWaitCondition() != null && !this.getWaitCondition().isEmpty(), "-d", this.getWaitCondition());

        joiner.add(this.getExtraArgs());

        return joiner.toString();
    }

    @Override
    public SchedulerArgs copy() {

        SchedulerArgs copy = new SlurmArgs(this);

        return copy;
    }

}
