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

package org.ratpackframework.routing.internal;

import org.ratpackframework.responder.FinalizedResponse;
import org.ratpackframework.routing.Router;
import org.ratpackframework.routing.RouterBuilderScript;
import org.ratpackframework.script.internal.ScriptEngine;
import org.ratpackframework.templating.TemplateRenderer;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.file.FileSystem;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ScriptBackedRouter implements Router {

  private final FileSystem fileSystem;
  private final String scriptFilePath;
  private final String scriptFileName;
  private final TemplateRenderer templateRenderer;

  private final AtomicLong lastModifiedHolder = new AtomicLong(-1);
  private final AtomicReference<byte[]> contentHolder = new AtomicReference<byte[]>();
  private final AtomicReference<Router> routerHolder = new AtomicReference<Router>(null);
  private final Lock lock = new ReentrantLock();

  private boolean staticallyCompile;

  private final boolean reloadable;

  public ScriptBackedRouter(Vertx vertx, File scriptFile, TemplateRenderer templateRenderer, boolean staticallyCompile, boolean reloadable) {
    this.fileSystem = vertx.fileSystem();
    this.scriptFilePath = scriptFile.getAbsolutePath();
    this.scriptFileName = scriptFile.getName();
    this.templateRenderer = templateRenderer;
    this.staticallyCompile = staticallyCompile;
    this.reloadable = reloadable;

    if (!reloadable) {
      refreshSync();
    }
  }

  @Override
  public void handle(final RoutedRequest routedRequest) {
    fileSystem.exists(scriptFilePath, routedRequest.getErrorHandler().asyncHandler(routedRequest.getRequest(), new Handler<Boolean>() {
      @Override
      public void handle(Boolean exists) {
        if (!exists) {
          routedRequest.getNotFoundHandler().handle(routedRequest.getRequest());
          return;
        }

        final Router server = new Router() {
          public void handle(RoutedRequest requestToServe) {
            ScriptBackedRouter.this.routerHolder.get().handle(requestToServe);
          }
        };

        Router refresher = new Router() {
          public void handle(RoutedRequest requestToServe) {
            ScriptBackedRouter.this.lock.lock();
            try {
              refreshSync();
            } catch (Exception e) {
              routedRequest.getFinalizedResponseHandler().handle(new AsyncResult<FinalizedResponse>(e));
              return;
            } finally {
              ScriptBackedRouter.this.lock.unlock();
            }
            server.handle(requestToServe);
          }
        };

        checkForChanges(routedRequest, server, refresher);
      }
    }));
  }

  void checkForChanges(final RoutedRequest routedRequest, final Router noChange, final Router didChange) {
    if (!reloadable) {
      noChange.handle(routedRequest);
      return;
    }

    fileSystem.props(scriptFilePath, routedRequest.getErrorHandler().asyncHandler(routedRequest.getRequest(), new Handler<FileProps>() {
      public void handle(FileProps fileProps) {
        if (fileProps.lastModifiedTime.getTime() != lastModifiedHolder.get()) {
          didChange.handle(routedRequest);
          return;
        }

        fileSystem.readFile(scriptFilePath, routedRequest.getErrorHandler().asyncHandler(routedRequest.getRequest(), new Handler<Buffer>() {
          @Override
          public void handle(Buffer buffer) {
            if (Arrays.equals(buffer.getBytes(), ScriptBackedRouter.this.contentHolder.get())) {
              noChange.handle(routedRequest);
            } else {
              didChange.handle(routedRequest);
            }
          }
        }));
      }
    }));
  }

  private void refreshSync() {
    FileProps fileProps;
    try {
      fileProps = fileSystem.propsSync(scriptFilePath);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    long lastModifiedTime = fileProps.lastModifiedTime.getTime();
    byte[] bytes;
    try {
      bytes = fileSystem.readFileSync(scriptFilePath).getBytes();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (lastModifiedTime == lastModifiedHolder.get() && Arrays.equals(bytes, contentHolder.get())) {
      return;
    }

    List<Router> routers = new LinkedList<Router>();
    String string;
    try {
      string = new String(bytes, "utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    ScriptEngine<RouterBuilderScript> scriptEngine = new ScriptEngine<RouterBuilderScript>(getClass().getClassLoader(), staticallyCompile, RouterBuilderScript.class);
    try {
      scriptEngine.run(scriptFileName, string, routers, templateRenderer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    routerHolder.set(new CompositeRouter(routers));
    this.lastModifiedHolder.set(lastModifiedTime);
    this.contentHolder.set(bytes);
  }

}
