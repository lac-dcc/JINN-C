import java.util.Random;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/* Note: SampleSort supporting Integers numbers as input */

public class SampleSortDriver {
    private static List<Integer> inputList;    
    private static boolean fromFile = false;
    private static boolean warmup;

    @AdaptiveMethod
    @Input(param="NT")
    @Input(param="list")
    private static double benchmark (int NT, int SS, int[] list) {
        long start = EmbeddedSystem.measureNano();
        int NE = list.length;
        SampleSort.initClass(NT, SS, NE, list);
        Thread[] thread_handles = new Thread[NT];         
        try {
            for (int thread = 0; thread < NT; thread++) {
                thread_handles[thread] = new Thread(new SampleSort(thread));
                thread_handles[thread].start();
            }

            for (Thread t : thread_handles) {
                t.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }     
        long finish = EmbeddedSystem.measureNano();

        double time = (finish - start) / 1e6;
        return time;
    }

    private static int[] buildInput (final int NE) {
        int list[];
        if (!fromFile || warmup) {
            Random random = new Random(0);
            list = new int[NE];
            for (int i = 0; i < NE; i++) {
              list[i] = random.nextInt();
            }
        } else {            
            list = inputList.stream()
                            .mapToInt(Integer::intValue)
                            .toArray();
        }
        return list;
    }

    private static int readInputFile (File file) {        
        inputList = new ArrayList<Integer>();
        int EL = 0;
        fromFile = true;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine(); // reads sequenceInt from PBBS inFile
            while ((line = reader.readLine()) != null) {
                Integer value = Integer.parseInt(line);
                inputList.add(value);
            }
            EL = inputList.size();
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
            }        
        } catch (Exception e) {
            e.printStackTrace();
        }
        return EL;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java SampleSortDriver <NT> <NE | IN>, where: ");
            System.err.println("  NT [=   8] - Number of threads.");
            System.err.println("  NE [= 800] - Number of Elements to Sort, OR ");
            System.err.println("    IN - Input file with numbers to sort (in PBBS format).");
            System.exit(1);
        } else {
            int NT = Integer.parseInt(args[0]);        
            int NE = getValueOrReadFile(args[1]);
            int SS = (NE <= 10 ? 1 : NE / 10);
            warmup = true;
            benchmark(8, 100, buildInput(1000));
            benchmark(4, 80, buildInput(5000));
            benchmark(16, 200, buildInput(10000));

            warmup = false;
            EmbeddedSystem.enableEnergyMeasurement();
            double time = benchmark(NT, SS, buildInput(NE));
            System.out.printf("%.2f", time);
        }
    }
}


