import java.util.Random;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class RadixSort {

    private static List<Integer> inputList;    
    private static boolean fromFile = false;
    private static boolean warmup;

    private static double benchmark (final int NT, int[] list) {                
        long startN = EmbeddedSystem.measureNano();
        MultiRadixPara sorter = new MultiRadixPara(NT);
        int[] sorted = sorter.radixMulti(list);
        double time = (EmbeddedSystem.measureNano() - startN) / 1e6;
        return time;
    }

    private static int[] buildInput (final int EL) {
        int[] list;
        if (!fromFile || warmup) {
            Random random = new Random(0);
            list = new int[EL];
            for (int i = 0; i < EL; i++) {
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


    public static void main(String args[]) throws InterruptedException {
        if (args.length < 2) {
            System.err.println("Syntax: java RadixSort <NT> <EL | IN> ");
            System.err.println("NT: number of threads.");
            System.err.println("EL: number of elements to sort, OR");
            System.err.println("  IN: input file with numbers to sort (in the PBBS format).");
            System.exit(1);
        } else {
            final int NT = Integer.parseInt(args[0]);
            final int EL = getValueOrReadFile(args[1]);
            warmup = true;
            benchmark(2,  buildInput(1000));
            benchmark(4,  buildInput(3000));
            benchmark(8,  buildInput(10000));
            benchmark(16, buildInput(80000));

            warmup = false;
            EmbeddedSystem.enableEnergyMeasurement();
            double time = benchmark(NT, buildInput(EL));
            System.out.printf("%.2f", time);
        }
    }
}
