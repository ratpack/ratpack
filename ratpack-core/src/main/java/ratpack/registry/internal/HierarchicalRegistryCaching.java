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

import ratpack.registry.Registry;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HierarchicalRegistryCaching {

  private static final ReferenceQueue<Registry> REFERENCE_QUEUE = new ReferenceQueue<>();

  private static class CacheKey {
    final WeakReference<Registry> parent;
    final WeakReference<Registry> child;
    final int parentHashCode;
    final int childHashCode;

    public CacheKey(WeakReference<Registry> parent, WeakReference<Registry> child) {
      this.parent = parent;
      this.child = child;
      this.parentHashCode = System.identityHashCode(parent.get());
      this.childHashCode = System.identityHashCode(child.get());
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
      Registry parentRegistry = parent.get();
      if (parentRegistry == null) {
        return false;
      }

      Registry childRegistry = child.get();
      if (childRegistry == null) {
        return false;
      }

      CacheKey other = (CacheKey) o;

      return parentRegistry.equals(other.parent.get()) && childRegistry.equals(other.child.get());
    }

    @Override
    public int hashCode() {
      return 31 * parentHashCode + childHashCode;
    }
  }

  private static final ConcurrentMap<CacheKey, Registry> CACHE = new ConcurrentHashMap<>();

  public static Registry join(Registry parent, Registry child) {
    CacheKey cacheKey = new CacheKey(new WeakReference<>(parent, REFERENCE_QUEUE), new WeakReference<>(child, REFERENCE_QUEUE));
    Registry registry = CACHE.computeIfAbsent(cacheKey, k ->
      CachingRegistry.of(new HierarchicalRegistry(parent, child))
    );

    Reference<? extends Registry> collected = REFERENCE_QUEUE.poll();
    while (collected != null) {
      Iterator<CacheKey> iterator = CACHE.keySet().iterator();
      while (iterator.hasNext()) {
        CacheKey k = iterator.next();
        if (k.parent == collected || k.child == collected) {
          iterator.remove();
        }
      }
      collected = REFERENCE_QUEUE.poll();
    }

    return registry;
  }
}
