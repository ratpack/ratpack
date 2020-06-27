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

package ratpack.core.file.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ratpack.core.file.BaseDirRequiredException;
import ratpack.config.FileSystemBinding;
import ratpack.core.handling.Context;
import ratpack.core.handling.Handler;
import ratpack.core.handling.internal.ChainHandler;
import ratpack.core.server.ServerConfig;
import ratpack.exec.registry.Registry;

import java.util.concurrent.ExecutionException;

public class FileSystemBindingHandler implements Handler {

  private final String path;
  private final Handler[] handler;

  private final static Cache<FileSystemBinding, Registry> CACHE = Caffeine.newBuilder().build();

  public FileSystemBindingHandler(ServerConfig serverConfig, String path, Handler handler) {
    if (serverConfig.isHasBaseDir()) {
      this.path = path;
      this.handler = ChainHandler.unpack(handler);
    } else {
      throw new BaseDirRequiredException("An application base directory is required to use this handler");
    }

    // TODO - validate the path isn't escaping up with ../
  }

  public void handle(Context context) throws ExecutionException {
    FileSystemBinding parentBinding = context.get(FileSystemBinding.class);
    FileSystemBinding binding = parentBinding.binding(path);
    if (binding == null) {
      context.clientError(404);
    } else {
      context.insert(CACHE.get(binding, FileSystemBindingHandler::registry), handler);
    }
  }

  private static Registry registry(FileSystemBinding binding) {
    return Registry.single(FileSystemBinding.class, binding);
  }
}
