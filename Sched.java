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
        public int startAt;
    }

    private ReentrantLock lock;
    private Condition cond;

    private Mode mode;
    private int runSteps;
    private ScheduledProcess currentProcess;
    private boolean running;
    private HashMap<Integer, ProcessDef> processes;
    private LinkedList<ScheduledProcess> scheduled;
    private HashMap<Integer, ScheduledProcess> trace;

    public Sched(Mode mode, int runSteps)
    {
        this.lock = new ReentrantLock();
        this.cond = this.lock.newCondition();

        this.mode = mode;
        this.runSteps = runSteps;
        this.running = true;
        this.processes = new HashMap<Integer, ProcessDef>();
        this.scheduled = new LinkedList<ScheduledProcess>();
        this.trace = new HashMap<Integer, ScheduledProcess>();
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

            System.out.println(" === step " + i + " ===");
            // process stopped
            if (this.currentProcess != null) {
                if (this.currentProcess.def.length <= i - this.currentProcess.startAt) {
                    System.out.println("stopped process " + this.currentProcess.def.proc.getPid());
                    this.currentProcess = null;
                }
            }

            ScheduledProcess sp = this.processStep(i);

            // a new process is launched
            if (sp != null) {
                System.out.println("starting new process " + sp.def.proc.getPid());
                this.currentProcess = sp;
                this.trace.put(i, this.currentProcess);
                this.running = false;

                sp.def.cond.signal();
            }

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
                System.out.println("new period for " + def.proc.getPid());

                ScheduledProcess sp = new ScheduledProcess();
                sp.def = def;
                sp.startPeriod = curIter;

                this.scheduled.add(sp);
            }
        }

        // no process running
        if (this.currentProcess == null) {
            if (!this.scheduled.isEmpty()) {
                next = this.scheduled.removeFirst();
                next.startAt = curIter;
            }
        }

        return next;
    }

    public void printTrace()
    {
        int usedSteps = this.runSteps;
        int violations = 0;

        for (int i = 0; i < this.runSteps; i++) {
            if (!this.trace.containsKey(i)) {
                usedSteps--;
                System.out.println(i + " -");

                continue;
            }

            ScheduledProcess p = this.trace.get(i);

            for (int j = 0; j < p.def.length; j++) {
                System.out.println((i + j) + " " + p.def.proc.getPid());
            }

            i += p.def.length - 1;

            if (i >= p.startPeriod + p.def.period) {
                violations++;
            }
        }

        System.out.println(violations + " " + usedSteps);
    }
}
