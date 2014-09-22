

package ratpack.func;

@FunctionalInterface
public interface Predicate<T> {

  static Predicate<Object> isNull() {
    return o -> o == null;
  }

  boolean apply(T t) throws Exception;

}
