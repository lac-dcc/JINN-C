// mined from http://www.ahristov.com/tutorial/geometry-games/convex-hull.html
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;

public class ParallelQuickHull implements Runnable {
  private Point2D.Double tA, tB;
  private ArrayList<Point2D.Double> tSet;
  private static ArrayList<Point2D.Double> hull;
  private int cores;

  public ParallelQuickHull () {}
  public ParallelQuickHull (Point2D.Double A, Point2D.Double B, ArrayList<Point2D.Double> set, int cores) {
    this.tA = A;
    this.tB = B;
    this.tSet = set;
    this.cores = cores;
  }

  public ArrayList<Point2D.Double> quickHull(ArrayList<Point2D.Double> points, int NT)
  {
      hull = new ArrayList<Point2D.Double>();

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
      hull.add(A);
      hull.add(B);
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

      if (NT > 1) {
        ParallelQuickHull pq = new ParallelQuickHull(A, B, rightSet, NT / 2);
        Thread t = new Thread(pq);
        t.start();
        hullSet(B, A, leftSet,  hull, NT / 2);
        try {
          t.join();
        } catch (InterruptedException ex) {}
      } else {
        hullSet(A, B, rightSet,  hull, NT);  
        hullSet(B, A, leftSet,  hull, NT);
      }

      return hull;
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

  @Override
  public void run() {
    hullSet(this.tA, this.tB, this.tSet, hull, this.cores);
  }


  public void hullSet(Point2D.Double A, Point2D.Double B, ArrayList<Point2D.Double> set,
          ArrayList<Point2D.Double> hull, int cores)
  {    
      if (cores <= 1) {
        serialHullSet(A,B,set,hull);
        return;
      }      

      int insertPosition = hull.indexOf(B);
      if (set.size() == 0)
          return;
      if (set.size() == 1)
      {
          Point2D.Double p = set.get(0);
          set.remove(p);
          if (insertPosition > 0) {
            hull.add(insertPosition, p);
          }
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

      synchronized (hull) {
        if (insertPosition > 0) {
          hull.add(insertPosition, P);
        }
      }

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
      ParallelQuickHull pq = new ParallelQuickHull(A, P, leftSetAP, cores/2);
      Thread t = new Thread(pq);
      t.start();

      ParallelQuickHull pq2 = new ParallelQuickHull(P, B, leftSetPB, cores/2);
      Thread t2 = new Thread(pq2);
      t2.start();

//      hullSet(A, P, leftSetAP, hull);
//      hullSet(P, B, leftSetPB, hull);
      try {
        t.join();
        t2.join();
      } catch (InterruptedException ex) {}
  }




  public void serialHullSet(Point2D.Double A, Point2D.Double B, ArrayList<Point2D.Double> set,
          ArrayList<Point2D.Double> hull)
  {
      int insertPosition = hull.indexOf(B);
      if (set.size() == 0)
          return;
      if (set.size() == 1)
      {
          Point2D.Double p = set.get(0);
          set.remove(p);
          if (insertPosition > 0) {
            hull.add(insertPosition, p);
          }
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

      synchronized(hull) {
        if (insertPosition > 0) {
          hull.add(insertPosition, P);
        }
      }

      
      ArrayList<Point2D.Double> leftSetAP = new ArrayList<Point2D.Double>();
      for (int i = 0; i < set.size(); i++)
      {
          Point2D.Double M = set.get(i);
          if (pointLocation(A, P, M) == 1)
          {
              leftSetAP.add(M);
          }
      }
      
      ArrayList<Point2D.Double> leftSetPB = new ArrayList<Point2D.Double>();
      for (int i = 0; i < set.size(); i++)
      {
          Point2D.Double M = set.get(i);
          if (pointLocation(P, B, M) == 1)
          {
              leftSetPB.add(M);
          }
      }

      serialHullSet(A, P, leftSetAP, hull);
      serialHullSet(P, B, leftSetPB, hull);
  }



  public int pointLocation(Point2D.Double A, Point2D.Double B, Point2D.Double P)
  {
      double cp1 = (B.x - A.x) * (P.y - A.y) - (B.y - A.y) * (P.x - A.x);
      return (cp1 > 0) ? 1 : -1;
  }

}
