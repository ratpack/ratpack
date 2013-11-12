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

package ratpack.groovy.handling.internal;

import ratpack.api.Nullable;
import ratpack.groovy.handling.GroovyChain;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;
import ratpack.util.Transformer;

import java.util.List;

public class GroovyDslChainActionTransformer implements Transformer<List<Handler>, GroovyChain> {

  private final LaunchConfig launchConfig;
  private final Registry registry;

  public GroovyDslChainActionTransformer(LaunchConfig launchConfig, @Nullable Registry registry) {
    this.launchConfig = launchConfig;
    this.registry = registry;
  }

  public GroovyChain transform(List<Handler> storage) {
    return new DefaultGroovyChain(storage, launchConfig, registry);
  }

}
