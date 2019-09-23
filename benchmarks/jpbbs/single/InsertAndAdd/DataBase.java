import java.util.ArrayList;

public class DataBase {
  private ArrayList<Counter> counters;
  private final int TRL;

  public DataBase(int iSZ, int tRL) {
    counters = new ArrayList<Counter>(iSZ);
    TRL = tRL;
    for (int i = 0; i < iSZ; i++) {
      counters.add(new Counter());
    }
  }

  public void inc(int index) {
    Counter c = counters.get(index);
    synchronized (c) {
      c.n++;
      if (c.n >= TRL) {
        c.n = 0;
        synchronized (counters) {
          Counter nc = new Counter();
          nc.n++;
          counters.add(nc);
        }
      }
    }
  }

  public int size() {
    synchronized (counters) {
      return counters.size();
    }
  }

  public int sum() {
    int sum = 0;
    synchronized(counters) {
      for (int i = 0; i < counters.size(); i++) {
        sum += counters.get(i).n;
      }
    }
    return sum;
  }
}
