public final class Response {
  private final Value value;
  private final boolean cacheHit;
  private final String nodeId;
  private final long latencyMs;

  public Response(Value value, boolean cacheHit, String nodeId, long latencyMs) {
    this.value = value;
    this.cacheHit = cacheHit;
    this.nodeId = nodeId;
    this.latencyMs = latencyMs;
  }

  public Value getValue() {
    return value;
  }

  public boolean isCacheHit() {
    return cacheHit;
  }

  public String getNodeId() {
    return nodeId;
  }

  public long getLatencyMs() {
    return latencyMs;
  }
}