public final class Value {
  private final Object data;

  public Value(Object data) {
    this.data = data;
  }

  public Object getData() {
    return data;
  }

  @Override
  public String toString() {
    return String.valueOf(data);
  }
}