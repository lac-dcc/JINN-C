public class Edge {
	private int v1;
	private int v2;
	private double w;
	private boolean weighted = true;

	public Edge (int v1, int v2) {		
		this(v1, v2, 0.0, false);
	}

	public Edge (int v1, int v2, double w) {
		this(v1, v2, w, true);
	}

	public boolean isWeighted () {
		return weighted;
	}

	public Edge (int v1, int v2, double w, boolean weighted) {
		this.v1 = v1;
		this.v2 = v2;
		this.w = w;
		this.weighted = weighted;
	}

	public int getVertex1 () {
		return this.v1;
	}

	public int getVertex2 () {
		return this.v2;
	}

	public double getWeight () {
		return this.w;
	}

	public String toString() {
		return this.v1 + " " + this.v2;
	}
}