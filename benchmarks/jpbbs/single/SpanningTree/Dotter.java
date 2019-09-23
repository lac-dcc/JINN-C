import java.io.*;
import java.util.*;


public class Dotter {
	public static void toDot(String fileName, List<Edge> edges) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			String tab = "";
			writer.write(tab + "digraph G {\n");			
			tab = "\t";
			writer.write(tab + "edge [dir=none]\n");
			for (Edge e : edges) {
				writer.write(tab + e.getVertex1() + " -> " + e.getVertex2() + "\n");
			}
			tab = "";
			writer.write(tab + "}\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}