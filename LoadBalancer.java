import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LoadBalancer implements NodeObserver {
  private final ConsistentHashRing ring;
  private final List<CacheNode> nodes;
  private final Set<String> activeNodeIds;

  public LoadBalancer(ConsistentHashRing ring) {
    this.ring = ring;
    this.nodes = new CopyOnWriteArrayList<>();
    this.activeNodeIds = ConcurrentHashMap.newKeySet();
  }

  public void addNode(CacheNode node) {
    if (node == null) {
      return;
    }
    if (!nodes.contains(node)) {
      nodes.add(node);
    }
    if (node.isUp() && activeNodeIds.add(node.getNodeId())) {
      ring.addNode(node);
    }
  }

  public void removeNode(CacheNode node) {
    if (node == null) {
      return;
    }
    nodes.remove(node);
    if (activeNodeIds.remove(node.getNodeId())) {
      ring.removeNode(node);
    }
  }

  public CacheNode route(Key key) {
    CacheNode primary = ring.getNode(key);
    if (primary != null && primary.isUp()) {
      return primary;
    }

    for (CacheNode node : nodes) {
      if (node.isUp()) {
        return node;
      }
    }
    return null;
  }

  @Override
  public void onNodeDown(CacheNode node) {
    if (node != null && activeNodeIds.remove(node.getNodeId())) {
      ring.removeNode(node);
    }
  }

  @Override
  public void onNodeUp(CacheNode node) {
    if (node == null || !node.isUp()) {
      return;
    }
    if (!nodes.contains(node)) {
      nodes.add(node);
    }
    if (activeNodeIds.add(node.getNodeId())) {
      ring.addNode(node);
    }
  }
}