import java.util.*;
//
// Class that performs kNN search using OctTrees as a representation
// of the 3D/2D space.
public class KNN {
    // Octree representing the space    
    OctTree octree;
    // Target point for search
    Point point;
    // Octant where we found the target point
    Node residingOctant;
    // Current searching octant
    Node searchOctant;
    // List of already visited octants to avoid revisiting
    List<Node> visitedOctants;
    // number of neighbors to look for
    int K;
    // neighbors queue ordered by the furthest first
    public PriorityQueue<PPair> nearestNeighbors;
    // initial radius
    double Infinity = Double.MAX_VALUE;            
    double radius = Infinity;
    // debug flag
    boolean debug = false;
    // for statistics
    public int visitedNodes  = 0;
    public int visitedPoints = 0;

    // comparator for priority queue. it inverts the order
    // so we can peek at the furthest point
    Comparator pairCompare = new Comparator<PPair>() {
                @Override
                public int compare(PPair p1, PPair p2) {
                    int diff = p1.distance.compareTo(p2.distance);
                    if (diff == 0) diff = 10;
                    return diff * (-1);
                }
    };

    public KNN (OctTree oct, Point target, int K) {
         this.octree = oct;
         this.point  = target;
         this.residingOctant = null;
         this.searchOctant = null;         
         this.visitedOctants = new ArrayList<Node>();
         this.nearestNeighbors = new PriorityQueue<PPair>(K, pairCompare);
         this.K = K;
    }

    public KNN (OctTree oct, Point target) {
        this(oct, target, 1);
    }    

    public PriorityQueue<PPair> getKNN() {
        return this.nearestNeighbors;
    }

    public Point getStartPoint() {
        return this.point;
    }

    // Find the Octant where the search (target) node is located
    private boolean findInitialOctant() {        
        this.residingOctant = this.octree.getRoot();
        Node[] children = this.residingOctant.getChildrenNodes();
        while (children != null) {
            if (debug) {
                System.out.print("Checking level " + this.residingOctant.getDepth() + " - ");
            }
            boolean stop = true;
            for (Node child : children) {
                if (child != null && child.coverPoint(this.point)) {
                    stop = false;
                    this.residingOctant = child;
                    children = child.getChildrenNodes();
                }
            }
            if (stop) break;
        }
        if (debug) {
            System.out.println("\nFound octant where point " + this.point +
                      " resides. -->" + this.residingOctant.toText());
        }
        this.searchOctant = this.residingOctant;
        return true;
    }

    // Look for neighbors in the initial octant
    private boolean searchInInitialOctant() {
        boolean found = this.checkOctant(this.residingOctant);        
        return found;
    }

    // look for the neighbors in the parent octants
    public boolean recursiveSearch() {   
        // get parent
        while (true) {
            this.searchOctant = this.searchOctant.parent;

            if (debug) {
                System.out.println("\nSearching in oct: " + 
                    (this.searchOctant == null? "Heaven" : this.searchOctant.toText()));
            }

            boolean found = this.checkOctant(this.searchOctant);

            if (debug) {
                if (!found) {
                    System.out.println("\t found nothing in this octant");
                }
            }

            if (!found && this.searchOctant == null)
                break;
        }

        if (debug) {
            System.out.println(nearestNeighbors.size() + " " + K + " - " + visitedNodes + " " + 
                    this.octree.nodes()+ " - visitedOctants: " + visitedOctants.size() + " - r: " + 
                    this.radius);
        }

        return false;
     }


     // Check the current octant and children for closest node inside that octant, if any exist.
     private boolean checkOctant(Node node) {        
        if (node == null || this.visitedOctants.contains(node)) {
            return false;        
        }        
                
        List<Point> points = node.getPoints();
        boolean change = false;
        for (Point p : points) {
            visitedPoints++; // stats
            double distance = dist(p, this.point);
            if (distance > 0.0 && (distance < this.radius || nearestNeighbors.size() < K)) {  
                if (nearestNeighbors.size() >= K) {
                    double farthest = nearestNeighbors.peek().distance;
                    if (distance < farthest) {
                        nearestNeighbors.poll();
                        nearestNeighbors.add(new PPair(p, distance));
                        this.radius = distance + (distance/2);
                    } else {
                        this.radius = farthest + (farthest/2);
                    }                    
                } else {
                    nearestNeighbors.add(new PPair(p, distance));                    
                    this.radius = distance + (distance/2);          
                }

                change = true;
                if (debug) {
                    System.out.println("\t Neighbor point " + p + " found on the octant [" + 
                        node.toText() + "] or in its children");
                }
            }
        }

        if (node.getChildrenNodes() != null) {
            Node[] children = node.getChildrenNodes();
            for (Node child : children) {
                if (distanceTo(child, this.point) <= this.radius && 
                             !this.visitedOctants.contains(child)) {
                    change |= this.checkOctant(child);
                }
            }
        }

        visitedNodes++;
        this.visitedOctants.add(node);
        return change;
    }

     //Distance from ourpoint to octant
    private double distanceTo (Node node, Point point) {
  	    double dmin = 0.0;
  	    if (node == null || point == null)
  	        return Infinity;

  	    Cube octant = node.getOctant();
      	if(point.x < octant.min.x) {
      		dmin += Math.pow(point.x - octant.min.x, 2);
      	} else if (point.x > octant.max.x) {
            dmin += Math.pow(point.x - octant.max.x, 2);
        }

      	if(point.y < octant.min.y){
      		dmin += Math.pow(point.y - octant.min.y, 2);
      	} else if (point.y > octant.max.y) {
            dmin += Math.pow(point.y - octant.max.y, 2);
        }

      	if(point.z < octant.min.z){
      		dmin += Math.pow(point.z - octant.min.z, 2);
      	} else if (point.z > octant.max.z) {
            dmin += Math.pow(point.z - octant.max.z, 2);
        }

        return dmin;
    }

    private double dist(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2) + 
            Math.pow(p1.z - p2.z, 2));
    }

    public void findAllNeighbors () {
        this.findInitialOctant();
        this.searchInInitialOctant();
        this.recursiveSearch();
    }
}