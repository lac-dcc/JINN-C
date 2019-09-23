// mined from http://www.ahristov.com/tutorial/geometry-games/convex-hull.html
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;

public class QuickHull
{
  public ArrayList<Point2D.Double> quickHull(ArrayList<Point2D.Double> points)
  {
      ArrayList<Point2D.Double> convexHull = new ArrayList<Point2D.Double>();
      int minPoint = -1, maxPoint = -1;
      double minX = Double.MAX_VALUE;
      double maxX = Double.MIN_VALUE;

      for (int i = 0; i < points.size(); i++)
      {
          if (points.get(i).x < minX)
          {
              minX = points.get(i).x;
              minPoint = i;
          }
          if (points.get(i).x > maxX)
          {
              maxX = points.get(i).x;
              maxPoint = i;
          }
      }

      Point2D.Double A = points.get(minPoint);
      Point2D.Double B = points.get(maxPoint);
      convexHull.add(A);
      convexHull.add(B);
      points.remove(A);
      points.remove(B);

      ArrayList<Point2D.Double> leftSet = new ArrayList<Point2D.Double>();
      ArrayList<Point2D.Double> rightSet = new ArrayList<Point2D.Double>();

      for (int i = 0; i < points.size(); i++)
      {
          Point2D.Double p = points.get(i);
          if (pointLocation(A, B, p) == -1)
              leftSet.add(p);
          else
              rightSet.add(p);
      }
      hullSet(A, B, rightSet, convexHull);
      hullSet(B, A, leftSet, convexHull);

      return convexHull;
  }

  public double distance(Point2D.Double A, Point2D.Double B, Point2D.Double C)
  {
      double ABx = B.x - A.x;
      double ABy = B.y - A.y;
      double num = ABx * (A.y - C.y) - ABy * (A.x - C.x);
      if (num < 0)
          num = -num;
      return num;
  }

  public void hullSet(Point2D.Double A, Point2D.Double B, ArrayList<Point2D.Double> set,
          ArrayList<Point2D.Double> hull)
  {
      int insertPosition = hull.indexOf(B);
      if (set.size() == 0)
          return;
      if (set.size() == 1)
      {
          Point2D.Double p = set.get(0);
          set.remove(p);
          hull.add(insertPosition, p);
          return;
      }
      double dist = (double) Integer.MIN_VALUE;
      int furthestPoint = -1;
      for (int i = 0; i < set.size(); i++)
      {
          Point2D.Double p = set.get(i);
          double distance = distance(A, B, p);
          if (distance > dist)
          {
              dist = distance;
              furthestPoint = i;
          }
      }
      Point2D.Double P = set.get(furthestPoint);
      set.remove(furthestPoint);
      hull.add(insertPosition, P);

      // Determine who's to the left of AP
      ArrayList<Point2D.Double> leftSetAP = new ArrayList<Point2D.Double>();
      for (int i = 0; i < set.size(); i++)
      {
          Point2D.Double M = set.get(i);
          if (pointLocation(A, P, M) == 1)
          {
              leftSetAP.add(M);
          }
      }

      // Determine who's to the left of PB
      ArrayList<Point2D.Double> leftSetPB = new ArrayList<Point2D.Double>();
      for (int i = 0; i < set.size(); i++)
      {
          Point2D.Double M = set.get(i);
          if (pointLocation(P, B, M) == 1)
          {
              leftSetPB.add(M);
          }
      }
      hullSet(A, P, leftSetAP, hull);
      hullSet(P, B, leftSetPB, hull);
  }

  public int pointLocation(Point2D.Double A, Point2D.Double B, Point2D.Double P)
  {
      double cp1 = (B.x - A.x) * (P.y - A.y) - (B.y - A.y) * (P.x - A.x);
      return (cp1 > 0.0) ? 1 : -1;
  }

 public static ArrayList<Point2D.Double> genPointSet(int NP) {
    Random rand = new Random();
    ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>();
    for (int i = 0; i < NP; i++) {
        int x = rand.nextInt(10000);
        int y = rand.nextInt(10000);
        Point2D.Double e = new Point2D.Double(x, y);
        points.add(i, e);
    }
    return points;
 }
}
