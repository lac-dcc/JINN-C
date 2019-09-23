import java.util.Random;

public class Producer extends Thread {

  private Random random;
  private DataBase db;
  private final int INS;

  public Producer(DataBase db, int iNS) {
    this.db = db;
    this.INS = iNS;
    this.random = new Random(0);
  }

  public void run() {
    for (int i = 0; i < INS; i++) {
      int limit = db.size();
      int index = random.nextInt(limit);
      db.inc(index);
    }
  }
}
