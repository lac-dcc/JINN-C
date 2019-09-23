import java.util.ArrayList;
import java.io.*;

public class SuffixArrayDriver {
    private static SuffixArray suffixArray = new SuffixArray();

    @AdaptiveMethod
    private static double benchmark(String input, int NT) 
                                    throws InterruptedException {        
        long start = EmbeddedSystem.measureNano();
        ArrayList<Integer> SA = suffixArray.buildSuffixArray(input, NT);
        long end = EmbeddedSystem.measureNano();
        return (end-start) / 1e6;
    }

    private static String read(String inputFile) {
        try {
            File file = new File(inputFile);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            return line;
        } catch (IOException e) {
            System.err.println("Invalid input file: " + inputFile);
            System.exit(2);
        }
        return "";
    }

	public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.println("Syntax: java Driver/SuffixArray NT FILE, where:");
            System.err.println("  NT [= 8] - Number of threads");
            System.err.println("  FILE - Input file with input string");
            System.exit(1);
        } else {
            int NT = Integer.parseInt(args[0]);
            String FILE = args[1];            
            // 
            // reading input string from file
	    	String input = read(FILE);
            benchmark(input.substring((int)input.length()/2), 4);
            benchmark(input.substring(0, (int)input.length()/2), 8);

            EmbeddedSystem.enableEnergyMeasurement();
            double SATime = benchmark(input, NT);
            System.out.printf("%.2f", SATime);
        }
	}
}
