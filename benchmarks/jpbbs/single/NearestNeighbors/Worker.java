import java.util.*;
public class Worker implements Runnable {
    KNN ann;
    Map<Point, PriorityQueue<PPair>> neighbors;

    public Worker (KNN ann, Map neighbors) {
        this.ann = ann;
        this.neighbors = neighbors;
    }

    @Override
    public void run() {
        ann.findAllNeighbors();
        Point target = ann.getStartPoint();
        PriorityQueue<PPair> knn = ann.getKNN();
        synchronized (neighbors) {
            neighbors.put(target, knn);
        }
    }
}