import java.util.Random;

public class RandomStrGen {
    static final String AB = "01";
    final private Random rnd; 

    public RandomStrGen(final int seed) {
        rnd = new Random(seed);
    }

    public String gen(final int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }
        return sb.toString();
    }
}
