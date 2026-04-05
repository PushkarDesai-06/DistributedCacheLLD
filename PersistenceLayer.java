public interface PersistenceLayer {
  WritePolicy getWritePolicy();

  Value get(Key key);

  void put(Key key, Value value);

  void flush();

  default void shutdown() {
  }
}