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

package ratpack.codahale.metrics.internal;

import com.codahale.metrics.MetricRegistry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A map of shared, named metric registries
 */
public class SharedMetricRegistries {

  private final ConcurrentMap<String, MetricRegistry> registries = new ConcurrentHashMap<String, MetricRegistry>();

  public void clear() {
    registries.clear();
  }

  public Set<String> names() {
    return registries.keySet();
  }

  public void remove(String key) {
    registries.remove(key);
  }

  public MetricRegistry add(String name, MetricRegistry registry) {
    return registries.putIfAbsent(name, registry);
  }

  public MetricRegistry getOrCreate(String name) {
    final MetricRegistry existing = registries.get(name);
    if (existing == null) {
      final MetricRegistry created = new MetricRegistry();
      final MetricRegistry raced = add(name, created);
      if (raced == null) {
        return created;
      }
      return raced;
    }
    return existing;
  }


}
