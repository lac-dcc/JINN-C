import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class SerialThreeCollinearPoints {

	private ArrayList<Point2D.Double> points;
	private int NP;
	private Map<String, ArrayList<Point2D.Double>> equations;

	public SerialThreeCollinearPoints(ArrayList<Point2D.Double> points) {
		this.points = points;
		this.NP = points.size();
		this.equations = new HashMap<String, ArrayList<Point2D.Double>>();
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

		String equation;
		double a, b;

		if(p2.getX() == p1.getX()){
			equation = "x = " + p2.getX();
		} else {
			a = (p2.getY() - p1.getY())/(p2.getX() - p1.getX());
			b = p1.getY() - a*p1.getX();
			equation = "y = " + a + ", " + b;
		}

		return equation;
	}

	public boolean collinearTest() {
		String eq;
		Point2D.Double p1, p2;
		ArrayList<Point2D.Double> collinearPoints;
		boolean answer = false;

		for(int i = 0; i < NP; i++) {
			for(int j = i+1; j < NP; j++) {
				////System.out.println("S: " + i + " " + j);
				p1 = points.get(i);
				p2 = points.get(j);

				eq = findEquationOfLine(p1, p2);
				////System.out.println("Serial***** " + eq);

				if(equations.containsKey(eq)) {
					//there are 3 (or more) collinear points
					answer = true;
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

		return answer;
	}
}
