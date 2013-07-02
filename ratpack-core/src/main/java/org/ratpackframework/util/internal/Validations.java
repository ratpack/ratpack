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

package org.ratpackframework.util.internal;

import java.util.regex.Matcher;

public abstract class Validations {

  public static void noLeadingForwardSlash(String string, String description) {
    if (string.startsWith("/")) {
      throw new IllegalArgumentException(String.format("%s string (value: %s) should not start with a forward slash", description, string));
    }
  }
}
