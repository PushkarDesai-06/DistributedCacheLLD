import java.util.Map;

public interface EvictionStrategy {
  void evict(Map<Key, Value> cache);

  boolean shouldEvict();

  default void onGet(Key key) {
  }

  default void onPut(Key key, Value value) {
  }

  default void onRemove(Key key) {
  }
}