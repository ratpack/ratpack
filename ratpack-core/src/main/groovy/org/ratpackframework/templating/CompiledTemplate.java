/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.templating;

import groovy.lang.Writable;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;
import java.io.Writer;

public class CompiledTemplate {

  private final Writable template;

  public CompiledTemplate(Writable template) {
    this.template = template;
  }

  public void render(AsyncResultHandler<Buffer> handler) {
    final Buffer buffer = new Buffer();
    try {
      template.writeTo(new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
          buffer.appendString(new String(cbuf, off, len));
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
      });
    } catch (Throwable e) {
      if (e instanceof Exception) {
        handler.handle(new AsyncResult<Buffer>((Exception) e));
      } else {
        e.printStackTrace();
      }
      return;
    }

    handler.handle(new AsyncResult<Buffer>(buffer));
  }

}
