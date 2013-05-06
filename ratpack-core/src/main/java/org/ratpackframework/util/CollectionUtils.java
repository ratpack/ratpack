package org.ratpackframework.util;

import org.ratpackframework.Transformer;

import java.lang.reflect.Array;
import java.util.*;

public abstract class CollectionUtils {

  public static <T> List<T> toList(Iterable<? extends T> source) {
    if (source instanceof Collection) {
      //noinspection unchecked
      return new ArrayList<>((Collection<? extends T>) source);
    }

    List<T> list = new LinkedList<>();
    for (T item : source) {
      list.add(item);
    }
    return list;
  }

  @SafeVarargs
  public static <T> List<T> toList(T... things) {
    if (things.length == 0) {
      return Collections.emptyList();
    } else {
      List<T> list = new ArrayList<>(things.length);
      for (T thing : things) {
        list.add(thing);
      }
      return list;
    }
  }

  public static String join(String separator, Object... objects) {
    return join(separator, objects == null ? null : Arrays.asList(objects));
  }

  public static String join(String separator, Iterable<?> objects) {
    if (separator == null) {
      throw new NullPointerException("The 'separator' cannot be null");
    }
    if (objects == null) {
      throw new NullPointerException("The 'objects' cannot be null");
    }

    boolean first = true;
    StringBuilder string = new StringBuilder();
    for (Object object : objects) {
      if (!first) {
        string.append(separator);
      }
      string.append(object.toString());
      first = false;
    }
    return string.toString();
  }

  public static <R, I> R[] collectArray(I[] list, Class<R> newType, Transformer<? super I, ? extends R> transformer) {
    return collectArray(list, (R[]) Array.newInstance(newType, list.length), transformer);
  }

  public static <R, I> R[] collectArray(I[] list, R[] destination, Transformer<? super I, ? extends R> transformer) {
    assert list.length <= destination.length;
    for (int i = 0; i < list.length; ++i) {
      destination[i] = transformer.transform(list[i]);
    }
    return destination;
  }

  public static <R, I> List<R> collect(List<? extends I> list, Transformer<? super I, ? extends R> transformer) {
    return collect(list, new ArrayList<R>(list.size()), transformer);
  }

  public static <R, I> List<R> collect(I[] list, Transformer<? super I, ? extends R> transformer) {
    return collect(Arrays.asList(list), transformer);
  }

  public static <R, I> Set<R> collect(Set<? extends I> set, Transformer<? super I, ? extends R> transformer) {
    return collect(set, new HashSet<R>(), transformer);
  }

  public static <R, I, C extends Collection<R>> C collect(Iterable<? extends I> source, C destination, Transformer<? super I, ? extends R> transformer) {
    for (I item : source) {
      destination.add(transformer.transform(item));
    }
    return destination;
  }

  private static class UpperCaseTransformer implements Transformer<Object, String> {
    @Override
    public String transform(Object from) {
      return from.toString().toUpperCase();
    }
  }

  public static List<String> toUpperCase(Iterable<?> things) {
    return collect(things, new LinkedList<String>(), new UpperCaseTransformer());
  }

  public static List<String> toUpperCase(List<?> things) {
    return collect(things, new UpperCaseTransformer());
  }

  public static List<String> toUpperCase(Object... things) {
    return collect(things, new UpperCaseTransformer());
  }

}
