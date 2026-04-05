import java.time.Duration;

public class Main {
  public static void main(String[] args) throws Exception {
    System.out.println("=== Distributed Cache Behavior Test ===");

    ConsistentHashRing ring = new ConsistentHashRing(50);
    LoadBalancer loadBalancer = new LoadBalancer(ring);
    Cache cache = new Cache(loadBalancer);

    CacheNode nodeA = new CacheNode("node-A", new LRUEviction(3), new WriteThrough());
    CacheNode nodeB = new CacheNode("node-B", new LRUEviction(3), new WriteBack(200));
    CacheNode nodeC = new CacheNode("node-C", new LRUEviction(3), new WriteThrough());

    nodeA.addReplica(nodeB);
    nodeB.addReplica(nodeC);

    loadBalancer.addNode(nodeA);
    loadBalancer.addNode(nodeB);
    loadBalancer.addNode(nodeC);

    HealthMonitor monitor = new HealthMonitor(Duration.ofMillis(100));
    monitor.addObserver(loadBalancer);
    monitor.addObserver(new LoggingObserver());
    monitor.registerNode(nodeA);
    monitor.registerNode(nodeB);
    monitor.registerNode(nodeC);
    monitor.startHeartbeat();

    try {
      System.out.println("\n1) Basic put/get through Cache API");
      Key user1 = new StringKey("user:1");
      printRoute(loadBalancer, user1);
      printResponse("PUT user:1", cache.put(user1, new Value("Alice")));
      printResponse("GET user:1", cache.get(user1));

      System.out.println("\n2) Replica sync test");
      Key replicaKey = new StringKey("replica:key");
      nodeA.put(replicaKey, new Value("replicated-value"));
      Value replicaRead = nodeB.get(replicaKey);
      System.out.println("Node-B read after Node-A write (replication expected): " + replicaRead);

      System.out.println("\n3) LRU eviction behavior test");
      CacheNode tinyNode = new CacheNode("tiny-lru", new LRUEviction(2), new WriteThrough());
      Key k1 = new StringKey("k1");
      Key k2 = new StringKey("k2");
      Key k3 = new StringKey("k3");
      tinyNode.put(k1, new Value("v1"));
      tinyNode.put(k2, new Value("v2"));
      tinyNode.get(k1); // Touch k1 so k2 becomes the LRU item.
      tinyNode.put(k3, new Value("v3"));
      System.out.println("tiny-lru in-memory has k1: " + tinyNode.containsKey(k1));
      System.out.println("tiny-lru in-memory has k2: " + tinyNode.containsKey(k2));
      System.out.println("tiny-lru in-memory has k3: " + tinyNode.containsKey(k3));

      System.out.println("\n4) Node-down failover with HealthMonitor");
      Key failoverKey = new StringKey("failover:key");
      CacheNode before = loadBalancer.route(failoverKey);
      System.out.println("Initial route for failover:key -> " + idOf(before));
      if (before != null) {
        before.markDown();
        Thread.sleep(300); // Allow monitor heartbeat to notify observers.
      }
      CacheNode after = loadBalancer.route(failoverKey);
      System.out.println("Route after marking initial node down -> " + idOf(after));
      printResponse("PUT failover:key", cache.put(failoverKey, new Value("fallback-ok")));
      printResponse("GET failover:key", cache.get(failoverKey));

      if (before != null) {
        before.markUp();
        Thread.sleep(300);
      }
    } finally {
      monitor.stopHeartbeat();
      nodeA.sync();
      nodeB.sync();
      nodeC.sync();
      nodeA.shutdown();
      nodeB.shutdown();
      nodeC.shutdown();
      System.out.println("\n=== Test completed ===");
    }
  }

  private static void printRoute(LoadBalancer loadBalancer, Key key) {
    CacheNode node = loadBalancer.route(key);
    System.out.println("Route for key -> " + idOf(node));
  }

  private static String idOf(CacheNode node) {
    return node == null ? "NO_NODE" : node.getNodeId();
  }

  private static void printResponse(String action, Response response) {
    System.out.println(
        action
            + " | node="
            + response.getNodeId()
            + ", hit="
            + response.isCacheHit()
            + ", value="
            + response.getValue()
            + ", latencyMs="
            + response.getLatencyMs());
  }

  private static final class StringKey implements Key {
    private final String raw;

    private StringKey(String raw) {
      this.raw = raw;
    }

    @Override
    public int getHashCode() {
      return raw.hashCode();
    }

    @Override
    public int hashCode() {
      return raw.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      StringKey other = (StringKey) obj;
      return raw.equals(other.raw);
    }

    @Override
    public String toString() {
      return raw;
    }
  }

  private static final class LoggingObserver implements NodeObserver {
    @Override
    public void onNodeDown(CacheNode node) {
      System.out.println("[HealthMonitor] Node DOWN: " + node.getNodeId());
    }

    @Override
    public void onNodeUp(CacheNode node) {
      System.out.println("[HealthMonitor] Node UP: " + node.getNodeId());
    }
  }
}