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
import ratpack.form.FormParse;
import ratpack.form.FormParser;
import ratpack.handling.Context;
import ratpack.http.TypedData;
import ratpack.parse.ParserSupport;

public class MultipartFormParser extends ParserSupport<Form, FormParse> implements FormParser {

  public MultipartFormParser() {
    super("multipart/form-data");
  }

  @Override
  public String getContentType() {
    return "multipart/form-data";
  }

  @Override
  public Form parse(Context context, TypedData requestBody, FormParse parse) {
    return FormDecoder.parseForm(context, requestBody);
  }

}
