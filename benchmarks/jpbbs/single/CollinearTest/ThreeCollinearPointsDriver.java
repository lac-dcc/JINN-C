import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

public class ThreeCollinearPointsDriver {

	public static ArrayList<Point2D.Double> generatesPointsSet(int pointsNumber) {

		ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>(pointsNumber);
		Random randomNumber = new Random(System.nanoTime());

		for (int i = 0; i < pointsNumber; i++) {

			double x = (double) randomNumber.nextInt(pointsNumber);
			double y = (double) randomNumber.nextInt(pointsNumber);
			Point2D.Double p = new Point2D.Double(x, y);

			while(points.contains(p)){
				x = (double) randomNumber.nextInt(2*pointsNumber);
				y = (double) randomNumber.nextInt(2*pointsNumber);
				p = new Point2D.Double(x, y);
			}

			points.add(p);
		}

		return points;
	}

	public static void runInParallel(ArrayList<Point2D.Double> points, int NP, int NT) {

		//calculating the number of iterations that will be
		//executed in each thread, that is, the value of variable "stop"

		int iterations, NIT, stop, remainder;

		//total number of iterations of the algorithm that finds if any 3 points are collinear
		iterations = (int) ((Math.pow(NP, 2) - NP) / 2);

		if(NT > iterations)
			NT = iterations;

		remainder = iterations % NT;

		if (remainder == 0)
			NIT = iterations / NT;
		else
			NIT = (int) Math.floor(iterations / NT);

		//initializing the auxiliary array
		//(it represents the number of iterations of the inner loop
		//for each iteration of the outer one)

		int[] aux =  new int[NP];

		for(int i = 0; i < NP; i++) {
			aux[i] = NP-i-1;
		}

		//determining the interval of iterations that each thread will execute

		int index, start1, start2, sum, num_missing_it;
		index = 0;
		Vector<Thread> threads = new Vector<Thread>();
		Map<String, ArrayList<Point2D.Double>> equations = new HashMap<String, ArrayList<Point2D.Double>>();
		ThreeColinnearPoints tcp = new ThreeColinnearPoints(equations, points, -1, -1, -1);

		for (int i = 0; i < NT; i++) {
			stop = NIT;
			if (remainder != 0) {
				stop++;
				remainder--;
			}

			num_missing_it = stop;
			sum = 0;
			start1 = index;
			start2 = index+1;

			if(aux[index] < NP-index-1)
				start2 += NP-index-1-aux[index];

			for(int j = index; j < NP; j++) {
				if(sum < stop) {
					sum += aux[j];
					index = j;
					if(aux[j] >= num_missing_it) {
						aux[j] -= num_missing_it;
						num_missing_it = 0;
					} else {
						num_missing_it -= aux[j];
						aux[j] = 0;
					}
				} else
					break;
			}

			if(sum == stop)
				index++;

			//// System.out.println(start1 + " " + start2);
			tcp = new ThreeColinnearPoints(equations, points, start1, start2, stop);
			threads.add(new Thread((Runnable) tcp));
		}

		for(Thread t: threads){
			t.start();
		}
		try{
			for(Thread t: threads){
			t.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//System.out.println("Parallel Collinear Test");
		//System.out.println("There are 3 (or more) collinear points: " + tcp.getAnswer());

		/*if(tcp.getAnswer()){
			tcp.printCollPoints();
		}*/
	}

	@AdaptiveMethod
	@Input(param="NP")
	@Input(param="NT")
	public static double benchmark(ArrayList<Point2D.Double> points, int NP, int NT) {
		long start = EmbeddedSystem.measureNano();
		runInParallel(points, NP, NT);
		double time = (EmbeddedSystem.measureNano() - start) / 1e6;
		return time;
	}

	public static void main(String args[]) {
		if (args.length < 2) {
			System.err.println("Syntax: java ThreeCollinearPointsDriver <NT> <NP>, where:");
			System.err.println("  NT  [=    8] - Number of threads.");
			System.err.println("  NP  [= 1000] - Number of points");
			System.exit(1);
		} else {
			int threadsNumber = Integer.parseInt(args[0]);
			int pointsNumber = Integer.parseInt(args[1]);
			ArrayList<Point2D.Double> points;
			points = generatesPointsSet(pointsNumber);

			// warmup
			benchmark(generatesPointsSet(100), 100, 1);
			benchmark(generatesPointsSet(800),800, 4);
			benchmark(generatesPointsSet(1000),1000, 8);

			//PARALLEL ALGORITHM
            EmbeddedSystem.enableEnergyMeasurement();
			double time = benchmark(points, pointsNumber, threadsNumber);
			System.out.printf("%.2f", time);

			//SERIAL ALGORITHM
			/*SerialThreeCollinearPoints A = new SerialThreeCollinearPoints(points);
			long start = System.nanoTime();
			boolean answer = A.collinearTest();
			time = (System.nanoTime() - start) / 1e6;
			System.out.println(time); */


			//System.out.println("Serial Collinear Test");
			//System.out.println("There are 3 (or more) collinear points: " + answer);

			/*if(answer){
				A.printCollPoints();
			}*/
		}
	}
}
