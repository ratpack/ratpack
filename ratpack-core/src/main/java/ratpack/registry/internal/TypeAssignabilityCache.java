/*
 * Copyright 2015 the original author or authors.
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

package ratpack.registry.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import ratpack.util.Exceptions;

import java.util.concurrent.ExecutionException;

public abstract class TypeAssignabilityCache {

  private static final LoadingCache<CacheKey, Boolean> CACHE = CacheBuilder.newBuilder()
    .maximumSize(5120) // TODO - make this tuneable
    .build(new CacheLoader<CacheKey, Boolean>() {
      @Override
      public Boolean load(CacheKey key) throws Exception {
        return key.left.isAssignableFrom(key.right);
      }
    });

  private static final class CacheKey {
    final TypeToken<?> left;
    final TypeToken<?> right;

    public CacheKey(TypeToken<?> left, TypeToken<?> right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else {
        CacheKey cacheKey = (CacheKey) o;
        return left.getType() == cacheKey.left.getType()
          && right.getType() == cacheKey.right.getType();
      }
    }

    @Override
    public int hashCode() {
      int result = left.hashCode();
      result = 31 * result + right.hashCode();
      return result;
    }
  }

  public static boolean isAssignableFrom(TypeToken<?> left, TypeToken<?> right) {
    try {
      return CACHE.get(new CacheKey(left, right));
    } catch (ExecutionException e) {
      throw Exceptions.uncheck(e);
    }
  }
}
