import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;

final class Sched extends Thread
{
    public enum Mode {
        ED,
        FIFO,
        OPTI,
        RM,
        ID
    }

    private class ProcessDef {
        public Condition cond;
        public Process proc;
        public int length;
        public int period;
    }

    private class ScheduledProcess {
        public ProcessDef def;
        public int startPeriod;
        public int elapsed;
        public boolean violated;
    }

    private ReentrantLock lock;
    private Condition cond;

    private Mode mode;
    private int runSteps;
    private ScheduledProcess currentProcess;
    private boolean running;
    private HashMap<Integer, ProcessDef> processes;
    private LinkedList<ScheduledProcess> scheduled;
    private LinkedList<ScheduledProcess> trace;
    private int violations;

    public Sched(Mode mode, int runSteps)
    {
        this.lock = new ReentrantLock();
        this.cond = this.lock.newCondition();

        this.mode = mode;
        this.runSteps = runSteps;
        this.running = true;
        this.processes = new HashMap<Integer, ProcessDef>();
        this.scheduled = new LinkedList<ScheduledProcess>();
        this.trace = new LinkedList<ScheduledProcess>();
    }

    public void addProcess(Process proc, int length, int period)
    {
        ProcessDef sp = new ProcessDef();
        sp.cond = this.lock.newCondition();
        sp.proc = proc;
        sp.length = length;
        sp.period = period;

        this.processes.put(proc.getPid(), sp);
    }

    public void processWait(int id)
    {
        this.lock.lock();

        if (this.currentProcess == null
            || this.currentProcess.def.proc.getPid() != id
            || this.running) {
            try {
                this.processes.get(id).cond.await();
            } catch (InterruptedException e) {
                // TODO:
                e.printStackTrace();
            }
        }
    }

    public void processFinish(int id)
    {
        this.running = true;
        this.cond.signal();
        this.lock.unlock();
    }

    public void run()
    {
        for (ProcessDef def : this.processes.values()) {
            def.proc.start();
        }

        for (int i = 0; i < this.runSteps; i++) {
            this.lock.lock();

            if (!this.running) {
                try {
                    this.cond.await();
                } catch (InterruptedException e) {
                    // TODO:
                    e.printStackTrace();
                }
            }

            // System.out.println(" === step " + i + " ===");
            // process stopped
            if (this.currentProcess != null) {
                this.currentProcess.elapsed++;

                if (i >= this.currentProcess.startPeriod + this.currentProcess.def.period) {
                    if (!this.currentProcess.violated) {
                        this.violations++;
                        this.currentProcess.violated = true;
                    }
                }

                if (this.currentProcess.def.length <= this.currentProcess.elapsed) {
                    // System.out.println("stopped process " + this.currentProcess.def.proc.getPid());
                    this.currentProcess = null;
                }
            }

            ScheduledProcess sp = this.processStep(i);

            // a new process is launched
            if (sp != null) {
                // System.out.println("starting new process " + sp.def.proc.getPid());
                this.currentProcess = sp;
                this.running = false;

                sp.def.cond.signal();
            }

            this.trace.addLast(this.currentProcess);
            this.lock.unlock();
        }

        this.lock.lock();

        for (ProcessDef def : this.processes.values()) {
            def.proc.stopProcess();
            def.cond.signal();
            this.lock.unlock();

            try {
                def.proc.join();
            } catch (InterruptedException e) {
                // TODO:
                e.printStackTrace();
            }
            this.lock.lock();
        }

        this.lock.unlock();
    }

    private ScheduledProcess processStep(int curIter)
    {
        ScheduledProcess next = null;

        for (ProcessDef def : this.processes.values()) {
            // new period for the process
            if (curIter % def.period == 0) {
                // System.out.println("new period for " + def.proc.getPid());

                ScheduledProcess sp = new ScheduledProcess();
                sp.def = def;
                sp.startPeriod = curIter;
                sp.elapsed = 0;

                this.addScheduledProcessToSchedule(sp);
            }
        }

        // no process running
        if (!this.scheduled.isEmpty()) {
            if (this.currentProcess == null) {
                next = this.scheduled.removeFirst();
            } else if (this.mode != Sched.Mode.FIFO) { // no preemption for FIFO
                ScheduledProcess sp = this.currentProcess;
                ScheduledProcess sp2 = this.scheduled.getFirst();

                if (this.compareScheduledProcesses(sp, sp2) < 0) {
                    next = this.scheduled.removeFirst();
                    this.scheduled.addFirst(sp);
                }
            }
        }

        return next;
    }

    private void addScheduledProcessToSchedule(ScheduledProcess sp)
    {
        // A better approach would be to have used an ordered list with
        // a custom ordering function.
        // As I have other things to get done, I'll just put that like
        // this and hope you never read that code (nor understand it as
        // this insertion is O(n)).
        int i = 0;
        for (ScheduledProcess ssp : this.scheduled) {
            if (this.compareScheduledProcesses(sp, ssp) >= 0) {
                break;
            }

            i++;
        }

        this.scheduled.add(i, sp);
    }

    private int compareScheduledProcesses(ScheduledProcess sp, ScheduledProcess sp2)
    {
        switch (this.mode) {
            case FIFO:
                return -1;
            case ED:
                int deadline = sp.startPeriod + sp.def.period;
                int deadline2 = sp2.startPeriod + sp2.def.period;

                return deadline2 - deadline;
            default:
        }

        return 0;
    }

    public void printTrace()
    {
        int i = 0;
        int usedSteps = 0;

        for (ScheduledProcess sp : this.trace) {
            System.out.println(i + " " + (sp != null ? sp.def.proc.getPid() : "-"));
            usedSteps += (sp != null ? 1 : 0);
            i++;
        }

        System.out.println(this.violations + " " + usedSteps);
    }
}
