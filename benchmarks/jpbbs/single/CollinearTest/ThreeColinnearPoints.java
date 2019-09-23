import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Collections;

public class ThreeColinnearPoints implements Runnable {

	private ArrayList<Point2D.Double> points;
	private int NP, start1, start2, stop;
	private Map<String, ArrayList<Point2D.Double>> equations;

	public ThreeColinnearPoints(Map<String, ArrayList<Point2D.Double>> equations, ArrayList<Point2D.Double> points, int start1, int start2, int stop) {
		this.equations = equations;
		this.points = points;
		this.NP = points.size();
		this.start1 = start1;
		this.start2 = start2;
		this.stop = stop;
	}

	public boolean getAnswer(){
		boolean answer = false;

		for(ArrayList<Point2D.Double> pointsList: equations.values()){
			if(pointsList.size() >= 3){
				answer = true;
				break;
			}
		}

		return answer;
	}

	public void printCollPoints(){
		for(Map.Entry<String, ArrayList<Point2D.Double>> entry: equations.entrySet()){
			if(entry.getValue().size() >= 3){
				System.out.print("Line Equation: " + entry.getKey() + " Collinear Points: ");
				for(Point2D.Double p: entry.getValue()){
					System.out.print("(" + p.getX() + ", " + p.getY() + ") ");
				}
				System.out.println();
			}
		}
	}

	private String findEquationOfLine(Point2D.Double p1, Point2D.Double p2) {

		String equation = "";
		Double a = 0.0, b = 0.0;

		if(p2.getX() == p1.getX()){
			equation = "x = " + p2.getX();
		} else {
			a = (p2.getY() - p1.getY())/(p2.getX() - p1.getX());
			b = p1.getY() - a*p1.getX();
            if (a != null && b != null) {
                try {
    			equation = "y = ";
    			equation += a.toString() + ", ";
    			equation += b.toString();
                } catch (Exception e) {e.printStackTrace(); System.out.println(equation); System.out.println(a); System.out.println(b);};
            }
		}

		return equation;
	}

	@Override
	public void run() {
		String eq;
		Point2D.Double p1, p2;
		ArrayList<Point2D.Double> collinearPoints;
		int count = 0;

		for(int i = start1; i < NP; i++) {
			for(int j = start2; j < NP; j++) {
				if(count == stop)
					break;
				count++;
				p1 = points.get(i);
				p2 = points.get(j);
                if (p1 != null && p2 != null) {
    				eq = findEquationOfLine(p1, p2);
    				////System.out.println("Parallel***** " + eq);

    				synchronized(equations){
    					if(equations.containsKey(eq)) {
    						//there are 3 collinear points
    						////System.out.println("Parallel - Col!!!");
    						ArrayList<Point2D.Double> pointsList = equations.get(eq);
    						if(!pointsList.contains(p1))
    							pointsList.add(p1);
    						if(!pointsList.contains(p2))
    							pointsList.add(p2);

    					} else {
    						collinearPoints = new ArrayList<Point2D.Double>();
    						collinearPoints.add(p1);
    						collinearPoints.add(p2);
    						equations.put(eq, collinearPoints);
    					}
    				}
                }

			}
			if(count == stop)
				break;
		}
	}
}
