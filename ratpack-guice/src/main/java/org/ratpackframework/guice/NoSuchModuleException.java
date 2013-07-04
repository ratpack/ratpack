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

package org.ratpackframework.guice;

import com.google.inject.Module;

/**
 * Thrown when a module is requested that does not exist.
 */
public class NoSuchModuleException extends IllegalArgumentException {

  private static final long serialVersionUID = 0;

  public NoSuchModuleException(Class<? extends Module> type) {
    super(String.format("No module of type '%s' found", type.getName()));
  }

}
