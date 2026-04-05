import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WriteBack implements PersistenceLayer {
  private final Map<Key, Value> store;
  private final Map<Key, Value> pendingWrites;
  private final ScheduledExecutorService flusher;

  public WriteBack() {
    this(250);
  }

  public WriteBack(long flushIntervalMs) {
    if (flushIntervalMs <= 0) {
      throw new IllegalArgumentException("Flush interval must be > 0");
    }
    this.store = new ConcurrentHashMap<>();
    this.pendingWrites = new ConcurrentHashMap<>();
    this.flusher = Executors.newSingleThreadScheduledExecutor();
    this.flusher.scheduleAtFixedRate(this::flush, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
  }

  @Override
  public WritePolicy getWritePolicy() {
    return WritePolicy.WRITE_BACK;
  }

  @Override
  public Value get(Key key) {
    Value pendingValue = pendingWrites.get(key);
    if (pendingValue != null) {
      return pendingValue;
    }
    return store.get(key);
  }

  @Override
  public void put(Key key, Value value) {
    pendingWrites.put(key, value);
  }

  @Override
  public synchronized void flush() {
    if (pendingWrites.isEmpty()) {
      return;
    }
    store.putAll(pendingWrites);
    pendingWrites.clear();
  }

  @Override
  public void shutdown() {
    flusher.shutdown();
  }
}