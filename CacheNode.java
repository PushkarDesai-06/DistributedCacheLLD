import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class CacheNode {
  private final String nodeId;
  private final Map<Key, Value> cache;
  private final EvictionStrategy eviction;
  private final PersistenceLayer persistence;
  private final List<CacheNode> replicas;
  private volatile boolean isUp;

  public CacheNode(String nodeId, EvictionStrategy eviction, PersistenceLayer persistence) {
    this.nodeId = nodeId;
    this.eviction = eviction;
    this.persistence = persistence;
    this.cache = new HashMap<>();
    this.replicas = new CopyOnWriteArrayList<>();
    this.isUp = true;
  }

  public String getNodeId() {
    return nodeId;
  }

  public boolean isUp() {
    return isUp;
  }

  public synchronized Value get(Key key) {
    if (!isUp) {
      return null;
    }

    Value value = cache.get(key);
    if (value != null) {
      eviction.onGet(key);
      if (eviction.shouldEvict()) {
        eviction.evict(cache);
      }
      return value;
    }

    Value persisted = persistence.get(key);
    if (persisted != null) {
      cache.put(key, persisted);
      eviction.onPut(key, persisted);
      if (eviction.shouldEvict()) {
        eviction.evict(cache);
      }
    }
    return persisted;
  }

  public synchronized void put(Key key, Value value) {
    if (!isUp) {
      return;
    }
    cache.put(key, value);
    eviction.onPut(key, value);
    if (eviction.shouldEvict()) {
      eviction.evict(cache);
    }
    persistence.put(key, value);
    replicateToBackups(key, value);
  }

  public synchronized boolean containsKey(Key key) {
    return cache.containsKey(key);
  }

  public void addReplica(CacheNode replicaNode) {
    if (replicaNode != null && replicaNode != this && !replicas.contains(replicaNode)) {
      replicas.add(replicaNode);
    }
  }

  public void removeReplica(CacheNode replicaNode) {
    replicas.remove(replicaNode);
  }

  public void markDown() {
    isUp = false;
  }

  public void markUp() {
    isUp = true;
  }

  public synchronized void sync() {
    persistence.flush();
  }

  public void shutdown() {
    persistence.shutdown();
  }

  private void replicateToBackups(Key key, Value value) {
    for (CacheNode replica : replicas) {
      replica.putReplica(key, value);
    }
  }

  private synchronized void putReplica(Key key, Value value) {
    if (!isUp) {
      return;
    }
    cache.put(key, value);
    eviction.onPut(key, value);
    if (eviction.shouldEvict()) {
      eviction.evict(cache);
    }
    persistence.put(key, value);
  }
}