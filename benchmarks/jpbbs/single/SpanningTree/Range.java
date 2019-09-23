public class Range {
	// inclusive
	public final int start;
	// exclusive
	public final int end;

	public Range (int s, int e) {
		this.start = s;
		this.end = e;
	}

	public String toString () {
		String data = "[" + start + ", " + end + ")";
		return data;
	}
}