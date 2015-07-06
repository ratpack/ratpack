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

package ratpack.form.internal;

import com.google.common.reflect.TypeToken;
import ratpack.exec.Promise;
import ratpack.form.Form;
import ratpack.form.FormParseOpts;
import ratpack.handling.Context;
import ratpack.http.TypedData;
import ratpack.parse.Parse;
import ratpack.parse.ParserSupport;
import ratpack.util.MultiValueMap;

import static ratpack.util.internal.ImmutableDelegatingMultiValueMap.empty;

public class FormParser extends ParserSupport<FormParseOpts> {

  private static final TypeToken<Form> FORM_TYPE = TypeToken.of(Form.class);

  private FormParser(String contentType) {
    super(contentType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Promise<T> parse(Context context, Promise<TypedData> requestBody, Parse<T, FormParseOpts> parse) throws Exception {
    if (parse.getType().equals(FORM_TYPE)) {
      MultiValueMap<String, String> base =
        parse.getOpts().isIncludeQueryParams() ? context.getRequest().getQueryParams() : empty();
      return (Promise) FormDecoder.parseForm(context, requestBody, base);
    } else {
      return null;
    }
  }

  public static FormParser multiPart() {
    return new FormParser("multipart/form-data");
  }

  public static FormParser urlEncoded() {
    return new FormParser("application/x-www-form-urlencoded");
  }

}
