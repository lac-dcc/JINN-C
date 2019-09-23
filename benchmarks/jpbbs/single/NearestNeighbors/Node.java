import java.util.List;
import java.util.ArrayList;
//
// Node structure. Each node has its boundary (Cube), an array of 3D points and
// an array of children nodes.
class Node {
    // Number of children a node has in the octTree
    final int SIZE = 8;
    // Max number of points allowed within a node, before spliting it
    int MAX_POINTS = 16;
    // Cube representing the axis limits of this node
    Cube boundary;
    // children octants
    Node[] children;
    // flag for disable insertions within current node
    public boolean saturated = false;
    // points stored in this node (octant)
    List<Point> points;
    // parent reference
    public Node parent;
    int count;
    // depth in the OctTree
    int depth;

    public Node(Cube boundary, int d, Node p) {
        this.boundary = boundary;
        points = new ArrayList<Point>(MAX_POINTS);
        children = new Node[SIZE];
        count = 0;
        depth = d;
        parent = p;
    }

    public boolean hasChildren() {
        return (children[0] != null? true : false);
    }

    public Cube getOctant () {
        return boundary;
    }

    public List<Point> getPoints() {
       return points;
    }

    //
    // Distribute points within a node among its children. This method should be
    // be called after reaching a threashold in the node, which forces the creation
    // of its children.
    public void rebalance() {
        // points added to a child will be removed from parent
        List<Point> remove = new ArrayList<Point>();
        for (Point p : points) {
            for (Node child : children) {
                if (child != null) {
                    if (child.coverPoint(p)) {
                        remove.add(p);
                        child.addPoint(p);
                    }
                }
            }
        }
        points.removeAll(remove);
    }

    public boolean coverPoint(Point p) {
        return this.boundary.containsPoint(p);
    }

    public int getDepth() {
        return depth;
    }

    public Node(Cube boundary) {
        this(boundary, 0, null);
    }

    public Node[] getChildrenNodes() {
        if (children[0] == null)
             return null;
        return children;
    }

    public int size() {
        return count;
    }

    public void addPoint(Point p) {
        points.add(p);
        count++;
    }

    public String toString() {
        String buff = "";
        String buff_boundary = "";
        String buff_children = "";
        String spaces = "";
        buff_boundary = boundary.toString();
        int indent = 4;

        for (Point p : points) {
            if (p != null)
                buff += p.toString() + " | ";
        }

        for (Node node : children) {
            if (node != null)
                buff_children += node.toString();
        }

        for (int i = 0; i < depth * indent; i++) {
            spaces += " ";
        }

        return "\n" + spaces + "boundary: " + buff_boundary + "\n" + spaces +
                                             "points: " + buff + buff_children;
    }

    public String toText() {
        String buff = "";
        String buff_boundary = "";
        String buff_children = "";
        String spaces = "";
        int nChildren = (children[0] == null? 0 : 8);
         

        return boundary.toString() + " : Points = " + points.size() + " Children = " + nChildren;
    }
}
