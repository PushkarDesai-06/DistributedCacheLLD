import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WriteThrough implements PersistenceLayer {
  private final Map<Key, Value> store;

  public WriteThrough() {
    this.store = new ConcurrentHashMap<>();
  }

  @Override
  public WritePolicy getWritePolicy() {
    return WritePolicy.WRITE_THROUGH;
  }

  @Override
  public Value get(Key key) {
    return store.get(key);
  }

  @Override
  public void put(Key key, Value value) {
    store.put(key, value);
  }

  @Override
  public void flush() {
    // Write-through persists synchronously on each write.
  }
}