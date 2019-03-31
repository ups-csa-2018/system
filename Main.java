import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import java.util.Vector;
import java.util.Scanner;
import java.io.FileReader;
import java.util.StringTokenizer;

class Main
{
    public static void main(String[] args)
    {
        Sched sc = null;
        Vector<Process> processes = new Vector<Process>();

        Integer expectedTasks = null;
        Scanner in = null;
        try {
            // TODO: change dat shit to get it from parameters or stdin
            in = new Scanner(new FileReader("input-2.txt"));
        } catch (java.io.FileNotFoundException e) {
            // TODO: handle it in a better way
            System.exit(2);
        }

        for (int i = 0; in.hasNextLine(); i++) {
            StringTokenizer tokenizer = new StringTokenizer(in.nextLine());

            if (i == 0) {
                if (tokenizer.countTokens() != 3) {
                    System.err.println("Expected 3 tokens, got " +
                        tokenizer.countTokens());
                    System.exit(1);
                }

                int timesteps = Integer.parseInt((String)tokenizer.nextElement());

                String mode = (String)tokenizer.nextElement();
                Sched.Mode schedMode = null;

                switch (mode) {
                    case "ED":
                        schedMode = Sched.Mode.ED;
                        break;
                    case "FIFO":
                        schedMode = Sched.Mode.FIFO;
                        break;
                    case "RM":
                        schedMode = Sched.Mode.RM;
                        break;
                    case "OPTI":
                        schedMode = Sched.Mode.OPTI;
                        break;
                    case "ID":
                        schedMode = Sched.Mode.ID;
                        break;
                    default:
                        System.err.println("Unknown mode " + mode);
                        System.exit(1);
                }

                expectedTasks = Integer.parseInt((String)tokenizer.nextElement());
                sc = new Sched(schedMode, timesteps);

                continue;
            }

            if (tokenizer.countTokens() != 2) {
                System.err.println("Expected 2 tokens, got " +
                    tokenizer.countTokens());
                System.exit(1);
            }

            Process p = new Process(i - 1, sc);
            processes.add(p);
            sc.addProcess(p,
                Integer.parseInt((String)tokenizer.nextElement()),
                Integer.parseInt((String)tokenizer.nextElement()));
        }

        if (sc == null) {
            System.out.println("Empty sched def.");
            System.exit(1);
        }


        if (expectedTasks != processes.size()) {
            System.out.println("Expected " + expectedTasks + " tasks, got " +
                processes.size());
            System.exit(1);
        }

        sc.start();

        try {
            sc.join();
        } catch (InterruptedException e) {
            // TODO:
            e.printStackTrace();
        }

        sc.printTrace();
    }
}
