import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class BuildHistogram {
  private ConcurrentHashMap<String, LongAdder> words;
    
  public BuildHistogram() {
    words = new ConcurrentHashMap<String, LongAdder>();
  }
  
  public Thread addWords(List<String> list) {
    Thread t = new Thread() {
      public void run() {
        for (String word: list) {
          words.computeIfAbsent(word, k -> new LongAdder()).increment();
        }
      }
    };
    t.start();
    return t;
  }
  
  public void dumpHistogram() {
      Map<String, LongAdder> sortedMap = new TreeMap<String, LongAdder>(words);
      for (Entry<String, LongAdder> entry : sortedMap.entrySet()) {
          System.out.printf("%s: %4d\n", entry.getKey(), entry.getValue().intValue());
      }
  }
}
