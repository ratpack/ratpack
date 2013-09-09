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

import com.google.common.collect.ImmutableList;
import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;

import java.io.File;
import java.util.List;

public class FileSystemBindingHandler implements Handler {

  private final File file;
  private final List<Handler> delegate;
  private final boolean absolute;
  private final FileSystemBinding absoluteBinding;

  public FileSystemBindingHandler(File file, ImmutableList<Handler> delegate) {
    this.file = file;
    this.delegate = delegate;
    this.absolute = file.isAbsolute();
    this.absoluteBinding = new DefaultFileSystemBinding(file.getAbsoluteFile());
  }

  public void handle(Context context) {
    if (absolute) {
      context.insert(delegate, FileSystemBinding.class, absoluteBinding);
    } else {
      FileSystemBinding parentBinding = context.maybeGet(FileSystemBinding.class);
      if (parentBinding == null) {
        context.insert(delegate, FileSystemBinding.class, absoluteBinding);
      } else {
        FileSystemBinding binding = parentBinding.binding(file.getPath());
        context.insert(delegate, FileSystemBinding.class, binding);
      }
    }
  }
}
