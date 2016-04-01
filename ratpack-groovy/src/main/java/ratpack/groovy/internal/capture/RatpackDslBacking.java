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
import ratpack.groovy.Groovy;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RatpackDslBacking implements Groovy.Ratpack {

  private final RatpackDslClosures closures;

  public RatpackDslBacking(RatpackDslClosures closures) {
    this.closures = closures;
  }

  @Override
  public void bindings(Closure<?> configurer) {
    closures.setBindings(configurer);
  }

  @Override
  public void handlers(Closure<?> configurer) {
    closures.setHandlers(configurer);
  }

  @Override
  public void serverConfig(Closure<?> configurer) {
    closures.setServerConfig(configurer);
  }

  @Override
  public void include(Path path) {
    closures.include(path);
  }

  @Override
  public void include(String path) {
    include(Paths.get(path));
  }

}
