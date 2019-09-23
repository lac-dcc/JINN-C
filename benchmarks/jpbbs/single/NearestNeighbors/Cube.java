//
// Representation of the octant boundary
public class Cube {
    public Point min, max;
    public double length;
    public int depth;

    public Cube(Point min, double length) {
        this(min, length, 0);
    }

    public Cube(Point min, double length, int depth) {
        this.min = min;
        this.length = length;
        max = new Point(min.x + length, min.y + length, min.z + length);
        this.depth = depth;
    }

    // checks if point P is within this cube
    public boolean containsPoint(Point p) {
        if (p == null)
            return false;
        return min.x <= p.x && p.x <= max.x && min.y <= p.y && p.y <= max.y
                                                    && min.z <= p.z && p.z <= max.z;
    }

    public boolean intersects(Cube r) {
        return !(r.min.x > max.x || r.min.y > max.y || r.max.x < min.x
                            || r.max.y < min.y || r.max.z < min.z || r.min.z > max.z);
    }

    public String toString() {
        return "depth: " + depth +  " min : " + min + " len: " + length;
    }
}
