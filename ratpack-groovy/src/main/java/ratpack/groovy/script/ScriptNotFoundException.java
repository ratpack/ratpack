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

package ratpack.groovy.script;

import com.google.common.base.Joiner;

public class ScriptNotFoundException extends RuntimeException {

  public ScriptNotFoundException(String path) {
    super("No Ratpack script found at " + path);
  }

  public ScriptNotFoundException(String... paths) {
    super("No Ratpack scripts found for " + Joiner.on(", ").join(paths));
  }
}
