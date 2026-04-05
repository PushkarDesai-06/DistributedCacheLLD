import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

public class LFUEviction implements EvictionStrategy {
  private static final class FrequencyEntry {
    private final Key key;
    private final int frequency;
    private final long version;

    private FrequencyEntry(Key key, int frequency, long version) {
      this.key = key;
      this.frequency = frequency;
      this.version = version;
    }
  }

  private final int capacity;
  private final Map<Key, Integer> frequencies;
  private final Map<Key, Long> versions;
  private final PriorityQueue<FrequencyEntry> minHeap;
  private final AtomicLong sequence;

  public LFUEviction(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be > 0");
    }
    this.capacity = capacity;
    this.frequencies = new HashMap<>();
    this.versions = new HashMap<>();
    this.minHeap = new PriorityQueue<>((a, b) -> {
      if (a.frequency != b.frequency) {
        return Integer.compare(a.frequency, b.frequency);
      }
      return Long.compare(a.version, b.version);
    });
    this.sequence = new AtomicLong(0);
  }

  @Override
  public synchronized void evict(Map<Key, Value> cache) {
    while (frequencies.size() > capacity && !minHeap.isEmpty()) {
      FrequencyEntry candidate = minHeap.poll();
      Integer currentFrequency = frequencies.get(candidate.key);
      Long currentVersion = versions.get(candidate.key);
      if (currentFrequency == null || currentVersion == null) {
        continue;
      }
      if (currentFrequency == candidate.frequency && currentVersion == candidate.version) {
        frequencies.remove(candidate.key);
        versions.remove(candidate.key);
        cache.remove(candidate.key);
      }
    }
  }

  @Override
  public synchronized boolean shouldEvict() {
    return frequencies.size() > capacity;
  }

  @Override
  public synchronized void onGet(Key key) {
    touch(key);
  }

  @Override
  public synchronized void onPut(Key key, Value value) {
    touch(key);
  }

  @Override
  public synchronized void onRemove(Key key) {
    frequencies.remove(key);
    versions.remove(key);
  }

  private void touch(Key key) {
    int frequency = frequencies.getOrDefault(key, 0) + 1;
    long version = sequence.incrementAndGet();
    frequencies.put(key, frequency);
    versions.put(key, version);
    minHeap.offer(new FrequencyEntry(key, frequency, version));
  }
}