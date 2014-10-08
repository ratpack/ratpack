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

package ratpack.jackson.configuration.internal;

import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.configuration.Discoverable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

/**
 * A subtype resolver which discovers subtypes via
 * {@code META-INF/services/ratpack.configuration.Discoverable}.
 */
public class DiscoverableSubtypeResolver extends StdSubtypeResolver {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoverableSubtypeResolver.class);

  private final ImmutableList<Class<?>> discoveredSubtypes;

  public DiscoverableSubtypeResolver() {
      this(Discoverable.class);
  }

  public DiscoverableSubtypeResolver(Class<?> rootKlass) {
      final ImmutableList.Builder<Class<?>> subtypes = ImmutableList.builder();
      for (Class<?> klass : discoverServices(rootKlass)) {
          for (Class<?> subtype : discoverServices(klass)) {
              subtypes.add(subtype);
              registerSubtypes(subtype);
          }
      }
      this.discoveredSubtypes = subtypes.build();
  }

  public ImmutableList<Class<?>> getDiscoveredSubtypes() {
      return discoveredSubtypes;
  }

  protected List<Class<?>> discoverServices(Class<?> klass) {
      final List<Class<?>> serviceClasses = Lists.newArrayList();
      try {
          final Enumeration<URL> resources = ClassLoader.getSystemResources("META-INF/services/" + klass.getName());
          while (resources.hasMoreElements()) {
              final URL url = resources.nextElement();
              try (InputStream input = url.openStream();
                   InputStreamReader streamReader = new InputStreamReader(input, Charsets.UTF_8);
                   BufferedReader reader = new BufferedReader(streamReader)) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                      try {
                          serviceClasses.add(Class.forName(line.trim()));
                      } catch (ClassNotFoundException e) {
                          LOGGER.info("Unable to load {}", line);
                      }
                  }
              }
          }
      } catch (IOException e) {
          LOGGER.warn("Unable to load META-INF/services/{}", klass.getName(), e);
      }
      return serviceClasses;
  }
}
