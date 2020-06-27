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

package ratpack.core.file.internal;

import com.google.common.collect.ImmutableList;
import ratpack.core.file.BaseDirRequiredException;
import ratpack.core.file.FileHandlerSpec;
import ratpack.core.handling.Handler;
import ratpack.core.handling.Handlers;
import ratpack.core.server.ServerConfig;
import ratpack.func.Action;

public class DefaultFileHandlerSpec implements FileHandlerSpec {

  private String path;
  private String dir;
  private ImmutableList<String> indexFiles = ImmutableList.of();

  @Override
  public FileHandlerSpec path(String path) {
    this.path = path;
    return this;
  }

  @Override
  public FileHandlerSpec dir(String dir) {
    this.dir = dir;
    return this;
  }

  @Override
  public FileHandlerSpec indexFiles(String... indexFiles) {
    this.indexFiles = ImmutableList.copyOf(indexFiles);
    return this;
  }

  public static Handler build(ServerConfig serverConfig, Action<? super FileHandlerSpec> config) throws Exception {
    if (!serverConfig.isHasBaseDir()) {
      throw new BaseDirRequiredException("no base dir set for application");
    }
    DefaultFileHandlerSpec spec = new DefaultFileHandlerSpec();
    config.execute(spec);
    Handler handler = new FileHandler(spec.indexFiles, !serverConfig.isDevelopment());
    if (spec.dir != null) {
      handler = Handlers.fileSystem(serverConfig, spec.dir, handler);
    }
    if (spec.path != null) {
      handler = Handlers.prefix(spec.path, handler);
    }
    return handler;
  }
}
