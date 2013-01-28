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

package org.ratpackframework.templating.internal;

import java.util.Collection;

public class CompositeException extends Exception {

  private final Collection<Exception> exceptions;

  public CompositeException(Collection<Exception> exceptions) {
    this.exceptions = exceptions;
  }

  @Override
  public String getMessage() {
    StringBuilder builder = new StringBuilder("{\n");
    int i = 0;
    for (Exception exception : exceptions) {
      builder.append("  ");
      builder.append(exception.getClass().getName());
      builder.append(": ");
      builder.append(exception.getMessage());
      if (++i < exceptions.size()) {
        builder.append(",\n");
      }
    }
    builder.append("\n}");
    return builder.toString();
  }
}
