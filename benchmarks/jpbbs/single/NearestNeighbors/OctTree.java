import java.util.HashSet;
import java.util.Set;
import java.io.*;
import java.util.*;
//
// OctTree implementation for a 3D space on the space x,y,z
public class OctTree {
    // Number of children a node has in the octTree
    final int SIZE = 8;
    // Max number of points allowed within a node, before spliting it
    int MAX_POINTS = 16;
    // Total number of points
    int numPoints = 0;
    // Total number of nodes
    int numNodes = 1;

    boolean debug = false;

    Node root;

    public OctTree(Cube boundary, int MAX_POINTS) {
        this.MAX_POINTS = MAX_POINTS;
        root = new Node(boundary);
    }

    public void buildTree(List<Point> points) {
        for (Point p : points) {
            boolean success = this.insert(p);
            if (!success) {
                System.err.println("Failed to insert: " + p);
            }
        }

        if (debug) {
            System.out.println("OctTree: Size: " + size() + 
                " - Inserted points: " + points.size());
        }
    }

    public int size() {
        return numPoints;
    }

    public boolean insert(Point p) {
        return insert(root, p);
    }

    private boolean insert(Node r, Point p) {
        if (r == null || !r.boundary.containsPoint(p)) {            
            return false;
        }
        if (r.size() < MAX_POINTS && !r.saturated) {
            r.addPoint(p);
            numPoints++;
            return true;
        }
        if (r.children[0] == null)
            subdivide(r);
        for (int i = 0; i < SIZE; i++)
            if (this.insert(r.children[i], p))
                return true;

        return false;
    }

    public Node getRoot() {
        return root;
    }

    private int calcNumNodes (Node node) {
        if (node == null)
            return numNodes;

        int childrenNodes = 0;
        if (node.getPoints().size() > 0)
            numNodes++;

        Node[] children = node.getChildrenNodes();
        if (children != null)
        for (Node c : children)
            calcNumNodes(c);
        
        return numNodes;
    }

    public int nodes() {
        if (numNodes > 1)
            return numNodes;
        numNodes = 0;
        return calcNumNodes(root);
    }

    private void subdivide(Node r) {
        Point min = r.boundary.min;
        double len = r.boundary.length / 2.0;
        int depth = r.getDepth() + 1;
        for (int i = 0; i < SIZE; i++) {
            Point m = new Point(min.x + ((i & 1) > 0 ? len : 0),
                    min.y + ((i & 2) > 0 ? len : 0), min.z + ((i & 4) > 0 ? len : 0));
            r.children[i] = new Node(new Cube(m, len, depth), depth, r);
        }

        // Try to move nodes from current node to its children
        r.saturated = true;
        r.rebalance();
    }

    public Set<Point> queryRange(Cube range) {
        Set<Point> res = new HashSet<Point>();
        queryRange(root, range, res);
        return res;
    }

    private void queryRange(Node r, Cube range, Set<Point> res) {
        if (r == null || !r.boundary.intersects(range))
            return;
        for (int i = 0; i < r.size(); i++) {
            Point p = r.points.get(i);
            if (range.containsPoint(p))
                res.add(p);
        }
        for (int i = 0; i < SIZE; i++)
            queryRange(r.children[i], range, res);
    }


    public String toString() {
        String buff = "";
        Node start = this.root;
        Node[] children = start.getChildrenNodes();
        buff += "root: " + start.toString() + "\n";
        for (Node node : children) {
            if (node != null)
                buff += node.toString();
        }
        return buff;
    }

}
