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
package uk.ac.ebi.fgpt.conan.core.context.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.tgac.rampart.conan.conanx.exec.process.monitor.ProcessAdapter;

public abstract class AbstractScheduler implements Scheduler {

    private static Logger log = LoggerFactory.getLogger(AbstractScheduler.class);

    private String submitCommand;
    private SchedulerArgs args;

    protected AbstractScheduler(String submitCommand, AbstractSchedulerArgs args) {
        this.submitCommand = submitCommand;
        this.args = args;
    }

    protected AbstractScheduler(AbstractScheduler copy) {
        this.submitCommand = copy.getSubmitCommand();
        this.args = copy.getArgs().copy();
    }


    /**
     * Return the command used to submit jobs to this scheduling system
     *
     * @return The submit command for this scheduler
     */
    @Override
    public String getSubmitCommand() {
        return this.submitCommand;
    }

    @Override
    public SchedulerArgs getArgs() {
        return args;
    }

    @Override
    public void setArgs(SchedulerArgs args) {
        this.args = args;
    }

    /**
     * Creates a proc adapter specific to this scheduler.  Automatically, uses the monitor file and interval stored in
     * this object.
     *
     * @return
     */
    @Override
    public ProcessAdapter createTaskAdapter() {
        return createTaskAdapter(this.args.getMonitorFile(), this.args.getMonitorInterval());
    }

}