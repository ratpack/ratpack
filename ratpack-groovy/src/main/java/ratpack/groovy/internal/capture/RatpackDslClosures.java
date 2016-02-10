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

package ratpack.groovy.internal.capture;

import groovy.lang.Closure;
import ratpack.func.Function;
import ratpack.func.Block;
import ratpack.groovy.Groovy;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.util.Exceptions;
import ratpack.util.internal.Paths2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class RatpackDslClosures {

  private Closure<?> handlers;
  private Closure<?> bindings;
  private Closure<?> serverConfig;

  public RatpackDslClosures() {
    this.handlers = Closure.IDENTITY;
    this.bindings = Closure.IDENTITY;
    this.serverConfig = Closure.IDENTITY;
  }

  public Closure<?> getHandlers() {
    return handlers;
  }

  public Closure<?> getBindings() {
    return bindings;
  }

  public Closure<?> getServerConfig() {
    return serverConfig;
  }

  public void setHandlers(Closure<?> handlers) {
    this.handlers = this.handlers.leftShift(handlers);
  }

  public void setBindings(Closure<?> bindings) {
    this.bindings = this.bindings.leftShift(bindings);
  }

  public void setServerConfig(Closure<?> serverConfig) {
    this.serverConfig = this.serverConfig.leftShift(serverConfig);
  }

  public void include(Path path) {
    Exceptions.uncheck(() -> {
      String script = Paths2.readText(path, StandardCharsets.UTF_8);
      RatpackDslClosures closures = new RatpackDslScriptCapture(false, new String[]{}, RatpackDslBacking::new).apply(path, script);
      this.setServerConfig(closures.getServerConfig());
      this.setBindings(closures.getBindings());
      this.setHandlers(closures.getHandlers());
    });
  }

  public static RatpackDslClosures capture(Function<? super RatpackDslClosures, ? extends Groovy.Ratpack> function, Block action) throws Exception {
    RatpackDslClosures closures = new RatpackDslClosures();
    Groovy.Ratpack receiver = function.apply(closures);
    RatpackScriptBacking.withBacking(closure -> ClosureUtil.configureDelegateFirst(receiver, closure), action);
    return closures;
  }

}
