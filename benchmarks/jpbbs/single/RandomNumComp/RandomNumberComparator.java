import java.util.Random;
import java.util.Vector;

public class RandomNumberComparator {
  private static double benchmark(final int TH, final int NC)
      throws InterruptedException {
    Vector<Thread> producers = new Vector<Thread>(TH);
    int exps[] = new int[TH];
    int hit0[] = new int[TH];
    int hit1[] = new int[TH];
    int hit2[] = new int[TH];
    int hit3[] = new int[TH];
    int hit4[] = new int[TH];
    //
    // Create the new threads:
    for (int i = 0; i < TH; i++) {
      final int index = i;
      producers.add(new Thread() {
        public void run() {
          Random rand = new Random();
          for (int j = 0; j < NC; j++) {
            final int n = rand.nextInt();
            if ((n & 1) > 0) {
              hit0[index]++;
            }
            if ((n & 2) > 0) {
              hit1[index]++;
            }
            if ((n & 4) > 0) {
              hit2[index]++;
            }
            if ((n & 8) > 0) {
              hit3[index]++;
            }
            if ((n & 16) > 0) {
              hit4[index]++;
            }
            exps[index]++;
          }
        }
      });
    }
    //
    // Run and time the program:
    long startN = EmbeddedSystem.measureNano();
    for (Thread t : producers) {
      t.start();
    }
    //
    // Wait until the threads finish:
    for (Thread t : producers) {
      t.join();
    }
    double time = (EmbeddedSystem.measureNano() - startN) / 1e6;
    //
    // Check results:
/* 
    for (int i = 0; i < exps.length; i++) {
      System.out.printf("%d: %8d/%8d/%8d/%8d/%8d/%8d\n",
          i, hit0[i], hit1[i], hit2[i], hit3[i], hit4[i], exps[i]); 
    }
 */
    return time;
  }

  public static void main(String args[]) throws InterruptedException {
    if (args.length < 2) {
      System.err.println("Syntax: InsertAndDouble TH NC, where");
      System.err.println(" - TH: Number of Threads");
      System.err.println(" - NC: Number of Comparisons");
    } else {
      //
      // Read the arguments and store them into constants:
      final int TH = Integer.parseInt(args[0]);
      final int NC = Integer.parseInt(args[1]);
      if (TH <= 0) {
        System.err.println("Error: TH must be greater than zero");
      } else if (NC < 0) {
        System.err.println("Error: NC must be larger than zero");
      } else {
        benchmark(4, 1000);
        benchmark(8, 10000);
        benchmark(8, 20000);

        EmbeddedSystem.enableEnergyMeasurement();
        double time = benchmark(TH, NC);
        System.out.printf("%.2f", time);
      }
    }
  }
}
