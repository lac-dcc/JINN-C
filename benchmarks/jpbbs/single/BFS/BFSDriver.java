import java.util.Arrays;
import java.util.Vector;

public class BFSDriver {

  @AdaptiveMethod
  public double benchmark(final int NT, final int NN)
      throws InterruptedException {
    boolean[] visited = new boolean[NN];
    Arrays.fill(visited, false);
    Graph graph = new Graph(NN, visited, NT);
    Vector<Processor> processors = new Vector<Processor>(NT);
    for (int i = 0; i < NT; i++) {
      processors.add(new Processor(graph, i));
    }
    //
    // Run and time the program:
    long startN = EmbeddedSystem.measureNano();
    for (Processor p : processors) {
      p.start();
    }
    for (Processor p : processors) {
      p.join();
    }
    return (EmbeddedSystem.measureNano() - startN) / 1e6;
  }

  public static void main(String args[]) throws InterruptedException {
    if (args.length < 2) {
      System.err.println("Syntax: Driver NT NN, where");
      System.err.println(" - NT: number of threads");
      System.err.println(" - NN: number of nodes");
      System.out.print(-1);
      System.exit(1);
    } else {
      final int NT = Integer.parseInt(args[0]);
      final int NN = Integer.parseInt(args[1]);
      //
      // New benchmark instance
      BFSDriver bfs = new BFSDriver();
      //
      // Benchmark warmup
      bfs.benchmark(4, 100);
      bfs.benchmark(8, 100);
      bfs.benchmark(5, 120);
      //
      // Benchmark run
      EmbeddedSystem.enableEnergyMeasurement();
      double time = bfs.benchmark(NT, NN);
      System.out.printf("%.2f", time);
    }
  }
}
