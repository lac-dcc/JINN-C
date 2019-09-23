import java.util.Vector;

public class UnbalancedHashSynch {

  static long startN = System.nanoTime();
  static int NTH;
  static int INS;
  static int ITR;
  static int[] globalMap;

  static class Worker extends Thread {
    final int loopLim;
    public Worker(int loopLim) {
      this.loopLim = loopLim;
    }
    public void run() {
      for (int ins = 0; ins < loopLim; ins++) {
        int key = (((int)this.getId()) * (ins + 1)) % INS;
        for (int work = 0; work < ITR; work++) {
          key = (key + 10007 * (work + 1)) % INS;
        }
        synchronized(globalMap) {
          int aux = globalMap[key];
          aux = aux + 1;
          globalMap[key] = aux;
        }
      }
      // double endT = (System.nanoTime() - startN) / 1000000000.0;
      // final long ID = getId();
      // System.out.println("T" + ID + ": " + endT);
    }
  }

  public static void main(String args[]) throws InterruptedException {
    if (args.length != 3) {
      System.err.println("Syntax: java UnbalancedHashSync NTH INS ITR");
      System.err.println("NTH: number of threads.");
      System.err.println("INS: number of insertions.");
      System.err.println("ITR: number of iterations.");
      System.exit(1);
    } else {
      Vector<Thread> ts = new Vector<Thread>();
      NTH = Integer.parseInt(args[0]);
      INS = Integer.parseInt(args[1]);
      ITR = Integer.parseInt(args[2]);
      globalMap = new int[INS];
      //
      // Add the threads into a vector:
      int elemPerThread = INS/NTH;
      if (elemPerThread < 1) {
        throw new IllegalArgumentException("Invalid number of elems/thread.");
      }
      for (int i = 0; i < NTH; i++) {
        final int loopLim = (((NTH/2) * elemPerThread)/(i+1));
        ts.add(new Worker(loopLim));
      }
      for (Thread t : ts) {
        t.start();
      }
      // double timeSeq = (System.nanoTime() - startN) / 1000000000.0;
      // System.out.println("Time Seq: " + timeSeq);
      //
      // Wait until the threads finish:
      for (Thread t : ts) {
        t.join();
      }
      double time = (System.nanoTime() - startN) / 1e6;
      System.out.printf("%.2f", time);
    }
  }
}
