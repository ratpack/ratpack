/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.util.internal;

import java.util.*;

public abstract class CollectionUtils {

  @SuppressWarnings("unchecked")
  public static <T> List<T> toList(Iterable<? extends T> source) {
    if (source instanceof Collection) {
      return new ArrayList<T>((Collection<? extends T>) source);
    }

    List<T> list = new LinkedList<T>();
    for (T item : source) {
      list.add(item);
    }
    return list;
  }

  public static <T> List<T> toList(T... things) {
    if (things.length == 0) {
      return Collections.emptyList();
    } else {
      List<T> list = new ArrayList<T>(things.length);
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

}
