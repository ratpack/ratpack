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

import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.http.Request;

import java.io.File;

import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

public class FileStaticAssetRequestHandler implements Handler {

  public static final Handler INSTANCE = new FileStaticAssetRequestHandler();

  public void handle(Context context) {
    Request request = context.getRequest();

    FileSystemBinding fileSystemBinding = context.get(FileSystemBinding.class);
    File targetFile = fileSystemBinding.getFile();

    if (targetFile.isHidden() || !targetFile.exists()) {
      context.next();
      return;
    }

    if (!request.getMethod().isGet()) {
      context.clientError(METHOD_NOT_ALLOWED.code());
      return;
    }

    context.render(targetFile);
  }
}
