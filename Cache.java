public class Cache {
  private final LoadBalancer loadBalancer;

  public Cache(LoadBalancer loadBalancer) {
    this.loadBalancer = loadBalancer;
  }

  public Response get(Key key) {
    long start = System.nanoTime();
    CacheNode node = loadBalancer.route(key);
    if (node == null) {
      return new Response(null, false, "NO_NODE", elapsedMs(start));
    }

    Value value = node.get(key);
    boolean cacheHit = value != null;
    return new Response(value, cacheHit, node.getNodeId(), elapsedMs(start));
  }

  public Response put(Key key, Value val) {
    long start = System.nanoTime();
    CacheNode node = loadBalancer.route(key);
    if (node == null) {
      return new Response(null, false, "NO_NODE", elapsedMs(start));
    }

    boolean existed = node.containsKey(key);
    node.put(key, val);
    return new Response(val, existed, node.getNodeId(), elapsedMs(start));
  }

  private long elapsedMs(long startNano) {
    return (System.nanoTime() - startNano) / 1_000_000;
  }
}