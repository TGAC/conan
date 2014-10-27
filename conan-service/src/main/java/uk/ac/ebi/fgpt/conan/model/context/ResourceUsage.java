package uk.ac.ebi.fgpt.conan.model.context;

/**
 * Created by maplesod on 20/10/14.
 */
public class ResourceUsage {

    private int maxMem;
    private long runTime;
    private long cpuTime;

    public ResourceUsage(int maxMem, long runTime, long cpuTime) {
        this.maxMem = maxMem;
        this.runTime = runTime;
        this.cpuTime = cpuTime;
    }

    /**
     * Max memory usage in Mb
     * @return
     */
    public int getMaxMem() {
        return maxMem;
    }

    /**
     * Wall clock runtime in seconds
     * @return
     */
    public long getRunTime() {
        return runTime;
    }

    /**
     * CPU time in seconds
     * @return
     */
    public long getCpuTime() {
        return cpuTime;
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean verbose) {

        return verbose ?
                "MaxMem(MB): " + this.maxMem + "; WallClock(s): " + this.runTime + "; CPUTime(s): " + this.cpuTime :
                this.maxMem + "\t" + this.runTime + "\t" + this.cpuTime;
    }
}
