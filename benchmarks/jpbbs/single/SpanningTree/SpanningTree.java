import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

//
// This is simple implementation of Kruskal Algorithm for ST.
public class SpanningTree {
    // List of edges in original graph
    List<Edge> graphEdges;
    // List of sets of each vertex
    int[] sets;
    // List of edges in ST
    List<Edge> ST;
    // List of Ranges - for parallel version
    List<Range> ranges;
    // Number of vertices
    int NV;
    // Number of threads
    int NT;

    public SpanningTree (List<Edge> edges, int NT, int NV) {
        this.NT = NT;
        this.NV = NV;
        this.sets = new int[NV];
        this.graphEdges = edges;
        this.ranges = new ArrayList<Range>();
        this.ST = new ArrayList<Edge>();
        for (int i = 0; i < NV; i++) this.sets[i] = i;
        splitEdges();
    }

    public List<Edge> getST () {
        return this.ST;
    }

    private void splitEdges () {
        int NE = this.graphEdges.size();
        int step = NE / this.NT;
        int init = 0;       
        for (int i = init; i < NE; i+=step) {
            int next = i + step;
            if (next > NE) next = NE;
            this.ranges.add(new Range(i, next));
        }
    }

    //
    // Simple serial Spanning Tree creation
    public void run () {
        for (Edge e : graphEdges) {
            int setA = find(e.getVertex1());
            int setB = find(e.getVertex2());
            if (setA != setB) {
                this.ST.add(e);
                union(setA, setB);
            }
        }
    }

    //
    // Simple parallel version using splitted ranges
    public void parallelRun () {        
        ExecutorService executor = Executors.newFixedThreadPool(this.NT);
        for (Range r : ranges) {
            Runnable worker = new Worker(r, this.graphEdges, this.ST, 
                                                            this.sets);
            executor.execute(worker);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
    }

    //
    // Finds the set which the vertex belongs to.
    private int find (int vertex) {
        if (this.sets[vertex] != vertex) 
            return find(this.sets[vertex]);
        else
            return vertex;
    }

    //
    // Merges two sets by setting the superset of a set
    private void union (int vertex1, int vertex2) {
        int setA = find(vertex1);
        int setB = find(vertex2);
        this.sets[setB] = setA;        
    }

    //
    // Check if spanning tree is valid (no cicles found) 
    // return true if cicle found, ST is invalid
    public boolean validate () {
        // build temp graph
        Map<Integer, List<Integer>> graph = new HashMap<Integer, List<Integer>>();
        for (Edge e : this.ST) {
            Integer v1 = e.getVertex1();
            Integer v2 = e.getVertex2();
            List<Integer> adjv1 = graph.getOrDefault(v1, new ArrayList<Integer>());
            List<Integer> adjv2 = graph.getOrDefault(v2, new ArrayList<Integer>());
            adjv1.add(v2);
            adjv2.add(v1);
            graph.put(v1, adjv1);
            graph.put(v2, adjv2);
        }

        Boolean[] visited = new Boolean[this.NV];
        for (int i = 0; i < this.NV; i++) 
            visited[i] = false; 

        for (int u = 0; u < this.NV; u++) 
            if (!visited[u]) 
                if (validateHelper(graph, visited, u, -1)) 
                    return true; 

        return false;
    }

    private boolean validateHelper (Map<Integer, List<Integer>> graph, Boolean[] visited,
                                        int v, int parent) {
        visited[v] = true; 
        List<Integer> adjs = graph.get(v); 
        for (Integer i : adjs) {
            if (!visited[i]) { 
                if (validateHelper(graph, visited, i, v))
                    return true; 
            } else if (i != parent) 
                return true; 
        } 
        return false; 
    }

    //
    // prints ST
    public String toString() {
        String data = "Spanning Tree:\n";
        data += "size: " + this.ST.size() +"\n";
        for (Edge e : this.ST) {
            data += e.getVertex1() + " " + e.getVertex2() + "\n";
        }
        return data;
    }
}


class Worker implements Runnable {
    Range r;
    List<Edge> graphEdges;
    List<Edge> ST;
    int[] sets;
    
    public Worker(Range r, List<Edge> edges, List<Edge> tree, int[] sets) {
        this.r = r;
        this.graphEdges = edges;
        this.ST = tree;
        this.sets = sets;
    }
    
    @Override
    public void run () {
        for (int i = this.r.start; i < this.r.end; i++) {
            Edge e = this.graphEdges.get(i);
            int setA = find(e.getVertex1());
            int setB = find(e.getVertex2());
            if (setA != setB) {             
                synchronized (this.sets) {
                    union(setA, setB, e);                   
                }
            }       
        }
    }
    
    //
    // Finds the set which the vertex belongs to.
    private int find (int vertex) {
        if (this.sets[vertex] != vertex) 
            return find(this.sets[vertex]);
        else
            return vertex;
    }
    
    //
    // Merges two sets by setting the superset of a set
    private void union (int vertex1, int vertex2, Edge e) {
        int setA = find(vertex1);
        int setB = find(vertex2);
        if (setA != setB) {
            this.ST.add(e);
            this.sets[setB] = setA;
        }       
    }
}