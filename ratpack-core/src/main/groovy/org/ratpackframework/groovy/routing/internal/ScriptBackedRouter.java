/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.groovy.routing.internal;

import com.google.inject.Injector;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.ratpackframework.config.LayoutConfig;
import org.ratpackframework.config.RoutingConfig;
import org.ratpackframework.Handler;
import org.ratpackframework.groovy.routing.internal.RoutingBuilderScript;
import org.ratpackframework.routing.ResponseFactory;
import org.ratpackframework.routing.RoutedRequest;
import org.ratpackframework.groovy.ScriptEngine;
import org.ratpackframework.routing.internal.CompositeRoutingHandler;
import org.ratpackframework.routing.internal.RoutingBuilder;

import javax.inject.Inject;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ScriptBackedRouter implements Handler<RoutedRequest> {

  private final Injector injector;
  private final File scriptFile;
  private final ResponseFactory responseFactory;

  private final AtomicLong lastModifiedHolder = new AtomicLong(-1);
  private final AtomicReference<byte[]> contentHolder = new AtomicReference<>();
  private final AtomicReference<Handler<RoutedRequest>> routerHolder = new AtomicReference<>(null);
  private final Lock lock = new ReentrantLock();
  private final RoutingConfig routingConfig;
  private final boolean reloadable;

  @Inject
  public ScriptBackedRouter(Injector injector, ResponseFactory responseFactory, LayoutConfig layoutConfig, RoutingConfig routingConfig) {
    this.injector = injector;
    this.scriptFile = new File(layoutConfig.getBaseDir(), routingConfig.getFile());
    this.responseFactory = responseFactory;
    this.routingConfig = routingConfig;
    this.reloadable = routingConfig.isReloadable();

    if (!reloadable) {
      try {
        refresh();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void handle(final RoutedRequest routedRequest) {
    if (!reloadable) {
      routerHolder.get().handle(routedRequest);
      return;
    }

    if (!scriptFile.exists()) {
      routedRequest.next();
      return;
    }

    try {
      if (refreshNeeded()) {
        refresh();
      }
    } catch (Exception e) {
      routedRequest.getExchange().error(e);
    }

    routerHolder.get().handle(routedRequest);
  }

  private boolean isBytesAreSame() throws IOException {
    byte[] existing = contentHolder.get();
    if (existing == null) {
      return false;
    }

    FileInputStream fileIn = new FileInputStream(scriptFile);
    InputStream in = new BufferedInputStream(fileIn);
    int i = 0;
    int b = in.read();
    while (b != -1 && i < existing.length) {
      if (b != existing[i++]) {
        return false;
      }
    }

    return true;
  }

  private boolean refreshNeeded() throws IOException {
    return (scriptFile.lastModified() != lastModifiedHolder.get()) || !isBytesAreSame();
  }

  private void refresh() throws IOException {
    lock.lock();
    try {
      long lastModifiedTime = scriptFile.lastModified();
      byte[] bytes = ResourceGroovyMethods.getBytes(scriptFile);

      if (lastModifiedTime == lastModifiedHolder.get() && Arrays.equals(bytes, contentHolder.get())) {
        return;
      }

      List<Handler<RoutedRequest>> routers = new LinkedList<>();
      RoutingBuilder routingBuilder = new RoutingBuilder(injector, routers, responseFactory);
      String string;
      try {
        string = new String(bytes, "utf-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
      ScriptEngine<RoutingBuilderScript> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), routingConfig.isStaticallyCompile(), RoutingBuilderScript.class);
      try {
        scriptEngine.run(scriptFile.getName(), string, routingBuilder);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      routerHolder.set(new CompositeRoutingHandler(routers));
      this.lastModifiedHolder.set(lastModifiedTime);
      this.contentHolder.set(bytes);
    } finally {
      lock.unlock();
    }
  }

}
