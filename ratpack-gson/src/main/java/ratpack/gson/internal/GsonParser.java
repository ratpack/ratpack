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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import ratpack.api.Nullable;
import ratpack.gson.GsonParseOpts;
import ratpack.handling.Context;
import ratpack.http.TypedData;
import ratpack.parse.Parse;
import ratpack.parse.Parser;
import ratpack.parse.ParserSupport;
import ratpack.util.Types;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GsonParser extends ParserSupport<GsonParseOpts> {

  public static final TypeToken<Parser<GsonParseOpts>> TYPE = Types.intern(new TypeToken<Parser<GsonParseOpts>>() {});
  public static final Parser<GsonParseOpts> INSTANCE = new GsonParser();

  private GsonParser() {
  }

  @Nullable
  @Override
  public <T> T parse(Context context, TypedData body, Parse<T, GsonParseOpts> parse) throws Exception {
    if (!body.getContentType().isJson()) {
      return null;
    }

    GsonParseOpts opts = parse.getOpts().orElse(DefaultGsonParseOpts.INSTANCE);
    TypeToken<T> type = parse.getType();

    Gson gson = opts.getGson().orElseGet(() -> context.get(Gson.class));
    InputStream inputStream = body.getInputStream();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

    return gson.fromJson(bufferedReader, type.getType());
  }

  @Override
  public String toString() {
    return getClass().getName() + " (parses 'application/json' and types ending in '+json')";
  }

}
