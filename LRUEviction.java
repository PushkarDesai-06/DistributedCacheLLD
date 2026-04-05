import java.util.LinkedHashMap;
import java.util.Map;

public class LRUEviction implements EvictionStrategy {
  private final int capacity;
  private final LinkedHashMap<Key, Boolean> accessOrder;

  public LRUEviction(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be > 0");
    }
    this.capacity = capacity;
    this.accessOrder = new LinkedHashMap<>(16, 0.75f, true);
  }

  @Override
  public synchronized void evict(Map<Key, Value> cache) {
    while (accessOrder.size() > capacity) {
      Key eldest = accessOrder.entrySet().iterator().next().getKey();
      accessOrder.remove(eldest);
      cache.remove(eldest);
    }
  }

  @Override
  public synchronized boolean shouldEvict() {
    return accessOrder.size() > capacity;
  }

  @Override
  public synchronized void onGet(Key key) {
    accessOrder.put(key, Boolean.TRUE);
  }

  @Override
  public synchronized void onPut(Key key, Value value) {
    accessOrder.put(key, Boolean.TRUE);
  }

  @Override
  public synchronized void onRemove(Key key) {
    accessOrder.remove(key);
  }
}