import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthMonitor {
  private final Duration interval;
  private final List<NodeObserver> observers;
  private final List<CacheNode> monitoredNodes;
  private final Map<String, Boolean> knownNodeStates;
  private ScheduledExecutorService heartbeatExecutor;

  public HealthMonitor(Duration interval) {
    if (interval == null || interval.isZero() || interval.isNegative()) {
      throw new IllegalArgumentException("Interval must be a positive duration");
    }
    this.interval = interval;
    this.observers = new CopyOnWriteArrayList<>();
    this.monitoredNodes = new CopyOnWriteArrayList<>();
    this.knownNodeStates = new ConcurrentHashMap<>();
  }

  public void addObserver(NodeObserver observer) {
    if (observer != null) {
      observers.add(observer);
    }
  }

  public void removeObserver(NodeObserver observer) {
    observers.remove(observer);
  }

  public void registerNode(CacheNode node) {
    if (node != null && !monitoredNodes.contains(node)) {
      monitoredNodes.add(node);
      knownNodeStates.put(node.getNodeId(), node.isUp());
    }
  }

  public void unregisterNode(CacheNode node) {
    if (node != null) {
      monitoredNodes.remove(node);
      knownNodeStates.remove(node.getNodeId());
    }
  }

  public synchronized void startHeartbeat() {
    if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
      return;
    }
    heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    heartbeatExecutor.scheduleAtFixedRate(this::pollNodes, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
  }

  public synchronized void stopHeartbeat() {
    if (heartbeatExecutor != null) {
      heartbeatExecutor.shutdown();
      heartbeatExecutor = null;
    }
  }

  private void pollNodes() {
    for (CacheNode node : monitoredNodes) {
      boolean currentState = node.isUp();
      Boolean previousState = knownNodeStates.put(node.getNodeId(), currentState);
      if (previousState == null || previousState == currentState) {
        continue;
      }
      if (currentState) {
        notifyNodeUp(node);
      } else {
        notifyNodeDown(node);
      }
    }
  }

  private void notifyNodeDown(CacheNode node) {
    for (NodeObserver observer : observers) {
      observer.onNodeDown(node);
    }
  }

  private void notifyNodeUp(CacheNode node) {
    for (NodeObserver observer : observers) {
      observer.onNodeUp(node);
    }
  }
}