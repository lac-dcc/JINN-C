import java.util.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KNNDriver {
    private static boolean warmup;
    //
    // coordenates for the englobing plane
    static double minX, minY, minZ, maxLen;
    //
    // base point for benchmark
    static Point basePoint;
    //
    // map of points and its k nearest neighbors
    static Map<Point, PriorityQueue<PPair>> neighbors = 
                        new HashMap<Point, PriorityQueue<PPair>>();

    //
    // read points separated by space from input file
    // the file should not end with a blank line
    private static List<Point> readPointsFromFile(File file, int dimensions) {
        List<Point> points = new ArrayList<Point>();
        minX = minY = minZ = (double) Integer.MAX_VALUE;
        maxLen = 0;
        // this factor is to relax the borders of the plane, so we dont have points
        // lying on it.
        int factor = 100;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine(); // read initial PBBS description line
            while((line = reader.readLine()) != null) {
                double x, y, z;
                String[] tokens = line.split(" ");
                x = Double.parseDouble(tokens[0]);
                y = Double.parseDouble(tokens[1]);
                z = (dimensions == 3? Double.parseDouble(tokens[2]) : 0.0);

                if (x < minX) minX = x - Math.abs(x/factor);
                if (y < minY) minY = y - Math.abs(y/factor);
                if (z < minZ) minZ = z - Math.abs(z/factor);

                if (x > maxLen) maxLen = x + Math.abs(x/factor);
                if (y > maxLen) maxLen = y + Math.abs(y/factor);
                if (z > maxLen) maxLen = z + Math.abs(z/factor);

                points.add(new Point(x,y,z));
            }
        } catch (IOException e) {
            // FileNotFoundException
            e.printStackTrace();
        }
        return points;
    }

    public static List<Point> genPointSet(int NP, int dimensions) {
        Random rand = new Random();
        List<Point> localPoints = new ArrayList<Point>();
        minX = minY = minZ = (double) Integer.MAX_VALUE;
        maxLen = 0;
        int factor = 100;
        for (int i = 0; i < NP; i++) {
            double x = (double) (i*(i+5)) % 3000;
            double y = (double) (i*3) % 4006;
            double z = (double) (dimensions == 3? (i*2) % 1503 : 0.0);

            if (x < minX) minX = x - Math.abs(x/factor);
            if (y < minY) minY = y - Math.abs(y/factor);
            if (z < minZ) minZ = z - Math.abs(z/factor);
            if (x > maxLen) maxLen = x + Math.abs(x/factor);
            if (y > maxLen) maxLen = y + Math.abs(y/factor);
            if (z > maxLen) maxLen = z + Math.abs(z/factor);

            localPoints.add(new Point(x,y,z));
        }
        return localPoints;
    }

    private static List<Point> getValueOrReadFile (String arg, int dimensions) {
        List<Point> points;
        try {
            File input = new File(arg);
            if (input.exists()) {
                points = readPointsFromFile(input, dimensions);
            } else {
                int EL = Integer.parseInt(arg);
                points = genPointSet(EL, dimensions);
            }       
            basePoint = new Point(minX, minY, minZ);
            return points; 
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;        
    }

    @AdaptiveMethod
    private static double benchmark(int N, int K, int dimensions, List<Point> points) {
        Point  planeBase   = (warmup? new Point(minX, minY, minZ) : basePoint);
        double planeLength = 2*maxLen;
        Cube boundary      = new Cube(planeBase, planeLength);
        OctTree oct        = new OctTree(boundary, 16);
        //
        // build tree, with at most 16 points per cube
        oct.buildTree(points);
        //
        // initiate KNN search for all points
        // parallel
        long start = EmbeddedSystem.measureNano();
        ExecutorService executor = Executors.newFixedThreadPool(N);
        for (Point p : points) {
            //System.out.println("Running for: " + p);
            KNN ann = new KNN(oct, p, K);
            Runnable worker = new Worker(ann, neighbors);
            executor.execute(worker);
        }
        executor.shutdown();
        while(!executor.isTerminated()) {            
        }
        double time = (EmbeddedSystem.measureNano() - start) / 1e6;
        return time;        
    }


    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Syntax: java KNNDriver <NT> <NK> <ND> <NE | IN>, where:");
            System.err.println("\tNT  [= 8]  Number of threads to use.");
            System.err.println("\tNK  [= 10] Number of neighbors to look for.");
            System.err.println("\tND  [= 3]  Number of dimensions (2 or 3).");
            System.err.println("\tNE         Number of points to generate, OR");
            System.err.println("\t    IN     File containing 3D/2D input points " +
                                    "separated by space (in PBBS format)");
            System.exit(1);
        } else {            
            int NT = Integer.parseInt(args[0]);
            int NK = Integer.parseInt(args[1]);
            int dimensions = Integer.parseInt(args[2]);
            List<Point> pointSet = getValueOrReadFile(args[3], dimensions);

            // warmups
            warmup = true;
            List<Point> w1 = genPointSet(1000, dimensions);            
            benchmark(16, 100, dimensions, w1);
            List<Point> w2 = genPointSet(5000, dimensions);            
            benchmark(8, 200, dimensions, w2);
            List<Point> w3 = genPointSet(10000, dimensions);
            benchmark(1, 300, dimensions, w3);

            // run
            warmup = false;
            EmbeddedSystem.enableEnergyMeasurement();
            double time = benchmark(NT, NK, dimensions, pointSet);
            System.out.printf("%.4f", time);
        }
    }
}
