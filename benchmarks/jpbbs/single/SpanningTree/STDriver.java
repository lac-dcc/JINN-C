import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;

public class STDriver {
	private static int NV;

	@AdaptiveMethod
	private static double benchmark (List<Edge> edges, int NT) {		
		SpanningTree ST2 = new SpanningTree(edges, NT, NV);
		long start = EmbeddedSystem.measureNano();
		ST2.parallelRun();
		long finish = EmbeddedSystem.measureNano();
		double time = (finish - start) / 1e6;	
		return time;
	}

	private static List<Edge> readEdgesFromFile (String fileName) {
		File file = new File(fileName);
		return readEdgesFromFile(file);
	}	

	private static List<Edge> readEdgesFromFile (File file) {
		List<Edge> edges = new ArrayList<Edge>();
        int vertices = 0;
		try {
			BufferedReader inputBuffer = new BufferedReader(new FileReader(file));
			//
			// We read the first line, which contains the string EdgeArray. We 
			// discard it.
			String line = inputBuffer.readLine();
			while ((line = inputBuffer.readLine()) != null) {				
				// The input format is the ids of vertex that forms an edge
				String[] tokens = line.split(" ");
				int vertex1 = Integer.parseInt(tokens[0]);
				int vertex2 = Integer.parseInt(tokens[1]);
				edges.add( new Edge(vertex1, vertex2) );
                if (vertex1 > vertices) vertices = vertex1;
                if (vertex2 > vertices) vertices = vertex2;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		NV = vertices + 1;
		return edges;
	}

	private static List<Edge> generateEdges (int NE, int NVs) {
		List<Edge> edges = new ArrayList<Edge>();
		Set<String> singular = new HashSet<String>();
		Random random = new Random(0);
		int created = 0;
        int maxInt = 2*NE;
        int i =0;
		while (created < NE) {
            i++;
            if ( i > maxInt ) break;

			int V1 = random.nextInt(NVs);
			int V2 = random.nextInt(NVs);
			String linearized = "" + V1 + "-" + V2;
			if (!singular.contains(linearized)) {
				singular.add(linearized);
				singular.add("" + V2 + "-" + V1);
				edges.add( new Edge(V1, V2));
				created++;
			}
		}
		NV = NVs;
		return edges;
	}


	private static List<Edge> getValueOrReadFile (String arg, String[] args) {
        List<Edge> edges;
        try {
            File input = new File(arg);
            if (input.exists()) {
                edges = readEdgesFromFile(input);
            } else {
            	int NE = Integer.parseInt(arg);
            	int NV = Integer.parseInt(args[2]);
                edges = generateEdges(NE, NV);
            }      
            return edges;  
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

	public static void main (String args[]) {
		if (args.length < 2) {
			System.err.println("Syntax: java STDriver NT <IN | NE NV>, where:");
			System.err.println("   NT =  Number of threads.");
			System.err.println("   IN =  Input file with weighted graph (in PBBS format), OR");
			System.err.println("  <NE =  Number of edges to generate, and ");
			System.err.println("   NV =  Number of vertices to generate >");
			System.exit(1);
		} else {
			int NT = Integer.parseInt(args[0]);			
			//
			// warmup
			List<Edge> w1 = readEdgesFromFile("inputs/text-inputs/stW1k2.txt");
			//List<Edge> w1 = generateEdges(800, 100);
			benchmark(w1, 4);
			List<Edge> w2 = readEdgesFromFile("inputs/text-inputs/stW1k5.txt");
			//List<Edge> w2 = generateEdges(900, 90);
			benchmark(w2, 8);
			List<Edge> w3 = readEdgesFromFile("inputs/text-inputs/stW1k.txt");
			//List<Edge> w3 = generateEdges(1000, 80);
			benchmark(w3, 16);

			//
			// benchmark
			List<Edge> edges = getValueOrReadFile(args[1], args);
            EmbeddedSystem.enableEnergyMeasurement();
			double time = benchmark(edges, NT);
			System.out.printf("%.2f", time);
		}
	}
}
