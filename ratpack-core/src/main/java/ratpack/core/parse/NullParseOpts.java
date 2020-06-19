/*
 * Copyright 2014 the original author or authors.
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

package ratpack.core.parse;

/**
 * A generic parse type that can be used when parsers do not need any extra information from parse objects other than type.
 */
public final class NullParseOpts {

  static final NullParseOpts INSTANCE = new NullParseOpts();

  private NullParseOpts() {
  }

  @Override
  public String toString() {
    return "[no options]";
  }
}
