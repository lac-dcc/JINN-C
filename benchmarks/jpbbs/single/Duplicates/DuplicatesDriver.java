import java.util.ArrayList;
import java.util.List;

public class DuplicatesDriver {

    public static List<List<String>> genLists(final int TH, final int EL, final int KS) 
      throws InterruptedException {
      List<List<String>> ans = new ArrayList<List<String>>(TH);
      List<Thread> producers = new ArrayList<Thread>(TH);
      final int elemPerThread = EL / TH;
      //
      // Create the new threads:
      for (int i = 0; i < TH; i++) {
        final int seed = i;
        producers.add(new Thread() {
          public void run() {
            RandomStrGen strGen = new RandomStrGen(seed);
            List<String> rStr = new ArrayList<String>();
            for (int i = 0; i < elemPerThread; i++) {
                rStr.add(strGen.gen(KS));
            }
            synchronized (ans) {
                ans.add(rStr);
            }
          }
        });
      }
      for (Thread t : producers) {
          t.start();
      }
      for (Thread t : producers) {
          t.join();
      }
      return ans;
    }

    public static void printGenStrings(List<List<String>> iLists) {
      for (List<String> list : iLists) {
        for (String str : list) {
          System.out.print(str + ", ");
        }
        System.out.println("");
      }
  }

    static public class TimePair {
      public final double x;
      public final double y;
      public TimePair(double x, double y) {
        this.x = x;
        this.y = y;
      }
    }

    /**
     * This method executes one round of experiments.
     * 
     * @param TH:
     *            the number of threads.
     * @param EL:
     *            the number of elements.
     * @param KS:
     *            the size of the keys. If necessary to check the results of
     *            the computation, you can use these two methods:
     *            <code>printGenStrings(iLists);</code>
     *            <code>fd.dumpHistogram();</code>
     */
    @AdaptiveMethod
    private static TimePair benchmark(final int TH, final int EL, final int KS)
            throws InterruptedException {
      long start1 = EmbeddedSystem.measureNano();
      List<List<String>> iLists = genLists(TH, EL, KS);
      double time1 = (System.nanoTime() - start1) / 1e6;
      
      List<Thread> threads = new ArrayList<Thread>(TH);
      BuildHistogram fd = new BuildHistogram();
      
      long start2 = System.nanoTime();
      for (List<String> list : iLists) {
          threads.add(fd.addWords(list));
      }
      for (Thread t : threads) {
          t.join();
      }
      double time2 = (EmbeddedSystem.measureNano() - start2) / 1e6;
      TimePair tp = new TimePair(time1, time2);
      // printGenStrings(iLists);
      // fd.dumpHistogram();
      return tp;
  }


  public static void main(String args[]) throws InterruptedException {
    if (args.length < 2) {
      System.err.println("Syntax: InsertAndDouble TH EL KS, where");
      System.err.println(" - TH: Number of Threads");
      System.err.println(" - EL: Number of Elements");
      System.err.println(" - KS: Size of Keys");
      System.out.print(-1);
      System.exit(1);
    } else {
      final int TH = Integer.parseInt(args[0]);
      final int EL = Integer.parseInt(args[1]);
      final int KS = Integer.parseInt(args[2]);
      if (TH <= 0 || EL <= 0 || KS <= 0) {
          System.err.println("Error: arguments cannot be negative");
          System.out.print(-1);
          System.exit(1);
      } else {
        //
        // Benchmark warmup
        benchmark(4, 102400, 4);
        benchmark(8, 151200, 4);
        benchmark(8, 205600, 16);
        benchmark(16, 151200, 8);              
        //
        // Benchmark run
        EmbeddedSystem.enableEnergyMeasurement();
        TimePair tp = benchmark(TH, EL, KS);
        //System.out.printf("%.2f, %.2f", tp.x, tp.y);
        System.out.printf("%.2f", tp.x + tp.y);
      }
    }
  }
}
