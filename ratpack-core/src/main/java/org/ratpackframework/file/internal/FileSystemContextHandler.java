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

package org.ratpackframework.file.internal;

import org.ratpackframework.context.Context;
import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

import java.io.File;
import java.util.List;

import static java.util.Collections.singletonList;

public class FileSystemContextHandler implements Handler {

  private final File file;
  private final List<Handler> delegate;
  private final boolean absolute;
  private final FileSystemBinding absoluteContext;

  public FileSystemContextHandler(File file, Handler delegate) {
    this.file = file;
    this.delegate = singletonList(delegate);
    this.absolute = file.isAbsolute();
    this.absoluteContext = new DefaultFileSystemBinding(file.getAbsoluteFile());
  }

  public void handle(Exchange exchange) {
    Context currentContext = exchange.getContext();
    if (absolute) {
      exchange.insert(currentContext.plus(FileSystemBinding.class, absoluteContext), delegate);
    } else {
      FileSystemBinding parentContext = exchange.maybeGet(FileSystemBinding.class);
      if (parentContext == null) {
        exchange.insert(currentContext.plus(FileSystemBinding.class, absoluteContext), delegate);
      } else {
        FileSystemBinding binding = parentContext.binding(file.getPath());
        exchange.insert(currentContext.plus(FileSystemBinding.class, binding), delegate);
      }
    }
  }
}
