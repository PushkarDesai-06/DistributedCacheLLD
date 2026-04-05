import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TTLEviction implements EvictionStrategy {
  private final long ttlMillis;
  private final Map<Key, Long> expiryByKey;

  public TTLEviction(long ttlMillis) {
    if (ttlMillis <= 0) {
      throw new IllegalArgumentException("TTL must be > 0");
    }
    this.ttlMillis = ttlMillis;
    this.expiryByKey = new HashMap<>();
  }

  @Override
  public synchronized void evict(Map<Key, Value> cache) {
    long now = System.currentTimeMillis();
    Iterator<Map.Entry<Key, Long>> iterator = expiryByKey.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Key, Long> entry = iterator.next();
      if (entry.getValue() <= now) {
        cache.remove(entry.getKey());
        iterator.remove();
      }
    }
  }

  @Override
  public synchronized boolean shouldEvict() {
    long now = System.currentTimeMillis();
    for (Long expiry : expiryByKey.values()) {
      if (expiry <= now) {
        return true;
      }
    }
    return false;
  }

  @Override
  public synchronized void onPut(Key key, Value value) {
    expiryByKey.put(key, System.currentTimeMillis() + ttlMillis);
  }

  @Override
  public synchronized void onRemove(Key key) {
    expiryByKey.remove(key);
  }
}