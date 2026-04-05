import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashRing {
  private final SortedMap<Integer, CacheNode> ring;
  private final int replicas;

  public ConsistentHashRing(int replicas) {
    if (replicas <= 0) {
      throw new IllegalArgumentException("Replicas must be > 0");
    }
    this.replicas = replicas;
    this.ring = new TreeMap<>();
  }

  public synchronized void addNode(CacheNode node) {
    for (int i = 0; i < replicas; i++) {
      ring.put(hash(node.getNodeId() + "#" + i), node);
    }
  }

  public synchronized void removeNode(CacheNode node) {
    for (int i = 0; i < replicas; i++) {
      ring.remove(hash(node.getNodeId() + "#" + i));
    }
  }

  public synchronized CacheNode getNode(Key key) {
    if (ring.isEmpty()) {
      return null;
    }

    int hash = hash(key.getHashCode());
    SortedMap<Integer, CacheNode> tailMap = ring.tailMap(hash);
    Integer selected = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
    return ring.get(selected);
  }

  private int hash(String input) {
    return hash(input.hashCode());
  }

  private int hash(int input) {
    int h = input;
    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
  }
}