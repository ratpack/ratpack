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

package ratpack.form.internal;

import com.google.common.reflect.TypeToken;
import ratpack.exec.Promise;
import ratpack.form.Form;
import ratpack.handling.Context;
import ratpack.http.TypedData;
import ratpack.parse.NoOptParserSupport;

import static ratpack.util.internal.ImmutableDelegatingMultiValueMap.empty;

public class FormNoOptParser extends NoOptParserSupport {

  private static final TypeToken<Form> FORM_TYPE = TypeToken.of(Form.class);

  private FormNoOptParser(String contentType) {
    super(contentType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Promise<T> parse(Context context, Promise<TypedData> requestBody, TypeToken<T> type) throws Exception {
    if (type.equals(FORM_TYPE)) {
      return (Promise) FormDecoder.parseForm(context, requestBody, empty());
    } else {
      return null;
    }
  }

  public static FormNoOptParser multiPart() {
    return new FormNoOptParser("multipart/form-data");
  }

  public static FormNoOptParser urlEncoded() {
    return new FormNoOptParser("application/x-www-form-urlencoded");
  }

}
