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

package ratpack.config.internal.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ratpack.config.ConfigSource;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;

/**
 * Decorator for a configuration source to apply an error handler.
 */
public class ErrorHandlingConfigSource implements ConfigSource {
  private final ConfigSource delegate;
  private final Action<? super Throwable> errorHandler;

  public ErrorHandlingConfigSource(ConfigSource delegate, Action<? super Throwable> errorHandler) {
    this.delegate = delegate;
    this.errorHandler = errorHandler;
  }

  @Override
  public ObjectNode loadConfigData(ObjectMapper objectMapper, FileSystemBinding fileSystemBinding) throws Exception {
    try {
      return delegate.loadConfigData(objectMapper, fileSystemBinding);
    } catch (Throwable ex) {
      errorHandler.execute(ex);
      return objectMapper.createObjectNode(); // treat the source as if it had no data
    }
  }
}
