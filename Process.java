final class Process extends Thread
{
    private int id;
    private Sched sc;
    private boolean stop;

    public Process(int id, Sched sc)
    {
        this.id = id;
        this.sc = sc;
        this.stop = false;
    }

    public int getPid()
    {
        return this.id;
    }

    public void stopProcess()
    {
        this.stop = true;
    }

    public void run()
    {
        while (true) {
            this.sc.processWait(this.id);

            if (this.stop) {
                break;
            }

            System.out.println("Process " + this.id);

            this.sc.processFinish(this.id);
        }

        this.sc.processFinish(this.id);
    }
}
