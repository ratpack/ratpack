/*
 * Copyright 2018 the original author or authors.
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

package ratpack.gson.internal;

import com.google.gson.Gson;
import ratpack.gson.GsonParseOpts;

import java.util.Optional;

public class DefaultGsonParseOpts implements GsonParseOpts {

  public static final GsonParseOpts INSTANCE = new DefaultGsonParseOpts(null);

  private final Optional<Gson> gson;

  public DefaultGsonParseOpts(Gson gson) {
    this.gson = Optional.ofNullable(gson);
  }

  @Override
  public Optional<Gson> getGson() {
    return gson;
  }

  @Override
  public String toString() {
    return "GsonParseOpts{json=" + gson.orElse(null) + '}';
  }
}
