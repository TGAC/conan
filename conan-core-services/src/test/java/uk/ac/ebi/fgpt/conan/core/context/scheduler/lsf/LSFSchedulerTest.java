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
package uk.ac.ebi.fgpt.conan.core.context.scheduler.lsf;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.fgpt.conan.model.context.ResourceUsage;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * User: maplesod
 * Date: 26/02/13
 * Time: 13:33
 */
public class LSFSchedulerTest {

    private LSFScheduler lsfScheduler;

    @Before
    public void setup() {
        this.lsfScheduler = new LSFScheduler();
    }

    @Test
    public void createCommandTest() {
        String command = this.lsfScheduler.createCommand("sleep 50 2>&1", true);

        assertTrue(command.equals("bsub \"sleep 50 2>&1\""));
    }


    @Test
    public void createWaitCommandTest() {
        String command = this.lsfScheduler.createWaitCommand(
                this.lsfScheduler.createWaitCondition(LSFExitStatus.DONE.getExitStatus(), "WAIT")
        );

        assertTrue(command.equals("bsub -w \"done(WAIT)\" \"sleep 1 2>&1\""));
    }

    @Test
    public void createKillCommandTest() {
        String command = this.lsfScheduler.createKillCommand("KILL");

        assertTrue(command.equals("bkill KILL"));
    }

    @Test
    public void testJobIdExtraction() {

        String testLine = "Job <133704> is submitted to default queue <normal>.";

        int jobId = this.lsfScheduler.extractJobIdFromOutput(testLine);

        assertTrue(jobId == 133704);
    }

    @Test
    public void testParseLsfOutput() throws IOException {

        File lsfFile = FileUtils.toFile(this.getClass().getResource("/lsfoutput.lsf"));

        ResourceUsage ru = this.lsfScheduler.getResourceUsageFromMonitorFile(lsfFile);

        assertTrue(ru.getCpuTime() == 137);
        assertTrue(ru.getMaxMem() == 214);
    }
}
