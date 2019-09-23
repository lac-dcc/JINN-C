import java.util.Vector;
import java.util.HashMap;
import java.util.Map;

public class HashSync {

  private Map<Long, Long> globalMap = new HashMap<Long, Long>();
  private int RAND_LIMIT = 150;

  private double benchmark(
      final int NTH,
      final int INS,
      final int ITR
  ) throws InterruptedException {
    final int wpt = INS/NTH;
    final int elemPerThread = (wpt > 1? wpt : 1);
    Vector<Thread> ts = new Vector<Thread>();

    for (int i = 0; i < NTH; i++) {
      ts.add(new Thread() {
        public void run() {
          for (int ins = 0; ins < elemPerThread; ins++) {
            long key = (this.getId() * (ins + 1)) % RAND_LIMIT;
            for (int work = 0; work < ITR; work++) {
              key = (key + 10007 * (work + 1)) % RAND_LIMIT;
            }

            synchronized(globalMap) {
              long aux = globalMap.getOrDefault(key, 0L);
              aux = aux + 1;
              globalMap.put(key, aux);
            }
          }
        }
      });
    }
    //
    // Start all the threads in the vector:
    long startN = EmbeddedSystem.measureNano();
    for (Thread t : ts) {
      t.start();
    }
    //
    // Wait until the threads finish:
    for (Thread t : ts) {
      t.join();
    }
    return (EmbeddedSystem.measureNano() - startN) / 1e6;
  }

  public static void main(String args[]) throws InterruptedException {
    if (args.length != 3) {
      System.err.println("Syntax: java HashSync NTH INS ITR");
      System.err.println("NTH: number of threads.");
      System.err.println("INS: number of insertions.");
      System.err.println("ITR: number of iterations.");
      System.err.print(-1);
      System.exit(1);
    } else {
      final int NTH = Integer.parseInt(args[0]);
      final int INS = Integer.parseInt(args[1]);
      final int ITR = Integer.parseInt(args[2]);
      //
      // New benchmark instance
      HashSync hashsync = new HashSync();
      //
      // Benchmark warmup
      hashsync.benchmark(4, 10000, 500);
      hashsync.benchmark(8, 100000, 5000);
      hashsync.benchmark(16, 100, 50000);
      //
      // Benchmark run
      EmbeddedSystem.enableEnergyMeasurement();
      double time = hashsync.benchmark(NTH, INS, ITR);
      System.out.printf("%.2f", time);
    }
  }
}
