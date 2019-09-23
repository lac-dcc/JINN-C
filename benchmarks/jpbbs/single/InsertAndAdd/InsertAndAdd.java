import java.util.Vector;

public class InsertAndAdd {
  
  private static double benchmark (int NTH, int ISZ, int TRL, int INS) 
                                      throws InterruptedException{
    Vector<Producer> producers = new Vector<Producer>(NTH);
    DataBase db = new DataBase(ISZ, TRL);
    //
    // Create the new threads:
    for (int i = 0; i < NTH; i++) {
      producers.add(new Producer(db, INS));
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
    return time;
  }

  
  public static void main(String args[]) throws InterruptedException {
    if (args.length < 4) {
      System.err.println("Syntax: InsertAndDouble NTH ISZ TRL, where");
      System.err.println(" - NTH: Number of Threads");
      System.err.println(" - ISZ: Initial capacity");
      System.err.println(" - TRL: Change Threshold");
      System.err.println(" - INS: Number of Insertions");
    } else {
      //
      // Read the arguments and store them into constants:
      final int NTH = Integer.parseInt(args[0]);
      final int ISZ = Integer.parseInt(args[1]);
      final int TRL = Integer.parseInt(args[2]);
      final int INS = Integer.parseInt(args[3]);      
      //
      // Benchmark warmup
      benchmark(4, 10, 20000, 1000);
      benchmark(6, 100, 2000, 2000);
      benchmark(8, 1000, 200, 3000);
      benchmark(16, 10000, 20, 4000);
      //
      // Benchmark run
      EmbeddedSystem.enableEnergyMeasurement();
      double time = benchmark(NTH, ISZ, TRL, INS);
      System.out.printf("%.2f", time);
      //System.out.println("Final count = " + db.sum());
    }
  }
}
