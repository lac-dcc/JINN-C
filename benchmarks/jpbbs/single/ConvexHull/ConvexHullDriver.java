import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Random;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class ConvexHullDriver {
    private static ArrayList<Point2D.Double> points;    
    private static boolean fromFile = false;
    private static boolean warmup;
    static ParallelQuickHull qh = new ParallelQuickHull();

    @AdaptiveMethod
    private static double benchmark (ArrayList<Point2D.Double> points, int NT) {    
        long start = EmbeddedSystem.measureNano();
        ArrayList<Point2D.Double> p = qh.quickHull(points, NT);
        double time = (EmbeddedSystem.measureNano() - start) / 1e6;
        return time;
    }

    public static ArrayList<Point2D.Double> genPointSet(int NP) {
        Random rand = new Random();
        ArrayList<Point2D.Double> localPoints = new ArrayList<Point2D.Double>();
        for (int i = 0; i < NP; i++) {
            int x = (int) (i*(i+5)) % 3000;
            int y = (int) (i*3) % 4000;
            Point2D.Double e = new Point2D.Double(x, y);
            localPoints.add(i, e);
        }
        return localPoints;
    }

    private static int readInputFile (File file) {                
        int EL = 0;
        fromFile = true;
        points = new ArrayList<Point2D.Double>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine(); // reads pbbs_sequencePoint2d   from PBBS inFile
            int i = 0;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(" ");
                Double x = Double.parseDouble(tokens[0]);
                Double y = Double.parseDouble(tokens[1]);
                Point2D.Double e = new Point2D.Double(x, y);
                points.add(i, e);
                i++;
            }
            EL = points.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return EL;
    }

    private static int getValueOrReadFile (String arg) {
        int EL = 0;
        try {
            File input = new File(arg);
            if (input.exists()) {
                EL = readInputFile(input);
            } else {
                EL = Integer.parseInt(arg);
                points = genPointSet(EL);
            }        
        } catch (Exception e) {
            e.printStackTrace();
        }
        return EL;
    }

    public static void main(String args[]) {
        if (args.length < 2) {
            System.err.println("Syntax: java ConvexHullDriver <NT> <NP | IN>, where:");            
            System.err.println("  NT  [=    8] - Number of threads.");
            System.err.println("  NP  [= 1000] - Number of points, OR");
            System.err.println("     IN - Input file with points (in PBBS format).");
            System.exit(1);
        } else {
            int NT = Integer.parseInt(args[0]);
            int NP = getValueOrReadFile(args[1]);
            warmup = true;
            ArrayList<Point2D.Double> w1 = genPointSet(1000);
            ArrayList<Point2D.Double> w2 = genPointSet(5000); 
            ArrayList<Point2D.Double> w3 = genPointSet(6000); 
            benchmark(w1, 4);
            benchmark(w2, 8);
            benchmark(w3, 16);

            warmup = false;
            EmbeddedSystem.enableEnergyMeasurement();
            double time = benchmark(points, NT);
            System.out.printf("%.2f", time);
            //System.out.println("The points in the Convex hull using Quick Hull are: ");
            // for (int i = 0; i < p.size(); i++)
            //     System.out.println("(" + p.get(i).x + ", " + p.get(i).y + ")");
      }
  }
}
