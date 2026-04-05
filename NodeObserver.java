public interface NodeObserver {
  void onNodeDown(CacheNode node);

  void onNodeUp(CacheNode node);
}