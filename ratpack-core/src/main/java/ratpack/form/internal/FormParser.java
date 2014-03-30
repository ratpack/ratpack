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

import ratpack.form.Form;
import ratpack.handling.Context;
import ratpack.http.TypedData;
import ratpack.parse.NoOptParserSupport;

public class FormParser extends NoOptParserSupport {

  private FormParser(String contentType) {
    super(contentType);
  }

  @Override
  public <T> T parse(Context context, TypedData requestBody, Class<T> type) throws Exception {
    if (type.equals(Form.class)) {
      return type.cast(FormDecoder.parseForm(context, requestBody));
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
