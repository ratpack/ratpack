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

package ratpack.file.internal;

import ratpack.file.FileSystemBinding;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.io.File;

public class FileSystemBindingHandler implements Handler {

  private final File file;
  private final Handler handler;
  private final boolean absolute;
  private final FileSystemBinding absoluteBinding;

  public FileSystemBindingHandler(File file, Handler handler) {
    this.file = file;
    this.handler = handler;
    this.absolute = file.isAbsolute();
    this.absoluteBinding = new DefaultFileSystemBinding(file.getAbsoluteFile());
  }

  public void handle(Context context) {
    if (absolute) {
      context.insert(FileSystemBinding.class, absoluteBinding, handler);
    } else {
      FileSystemBinding parentBinding = context.maybeGet(FileSystemBinding.class);
      if (parentBinding == null) {
        context.insert(FileSystemBinding.class, absoluteBinding, handler);
      } else {
        FileSystemBinding binding = parentBinding.binding(file.getPath());
        if (binding != null) {
          context.insert(FileSystemBinding.class, binding, handler);
        }
      }
    }
  }
}
