/*
 * Copyright 2014 the original author or authors.
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

package ratpack.registry;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

public abstract class PredicateCacheability {

  private static final ImmutableSet<Class<?>> IMPLICITLY_CACHEABLE = ImmutableSet.<Class<?>>of(Predicates.alwaysTrue().getClass(), Predicates.alwaysFalse().getClass(), Predicates.notNull().getClass(), Predicates.isNull().getClass());

  private static final LoadingCache<Class<?>, Boolean> CACHEABLE_PREDICATE_CACHE = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, Boolean>() {
    @Override
    public Boolean load(@SuppressWarnings("NullableProblems") Class<?> key) throws Exception {
      try {
        Method equals = key.getDeclaredMethod("equals", Object.class);
        Class<?> declaringClass = equals.getDeclaringClass();
        return !Proxy.isProxyClass(declaringClass) && !declaringClass.equals(Object.class);
      } catch (NoSuchMethodException e) {
        return false;
      }
    }
  });

  private PredicateCacheability() {
  }

  public static boolean isCacheable(Predicate<?> predicate) {
    Class<?> clazz = predicate.getClass();
    if (IMPLICITLY_CACHEABLE.contains(clazz)) {
      return true;
    }

    try {
      return CACHEABLE_PREDICATE_CACHE.get(clazz);
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  public static class CacheKey<T> {
    public final TypeToken<T> type;
    public final Predicate<? super T> predicate;

    public CacheKey(TypeToken<T> type, Predicate<? super T> predicate) {
      this.type = type;
      this.predicate = predicate;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      CacheKey<?> that = (CacheKey) o;

      return predicate.equals(that.predicate) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
      int result = type.hashCode();
      result = 31 * result + predicate.hashCode();
      return result;
    }
  }
}
