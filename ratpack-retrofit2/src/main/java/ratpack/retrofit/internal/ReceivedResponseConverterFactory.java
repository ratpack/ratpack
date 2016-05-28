/*
 * Copyright 2016 the original author or authors.
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

package ratpack.retrofit.internal;

import okhttp3.ResponseBody;
import ratpack.http.client.ReceivedResponse;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ReceivedResponseConverterFactory extends Converter.Factory {

  public static final ReceivedResponseConverterFactory INSTANCE = new ReceivedResponseConverterFactory();

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
    // Do nothing since the underlying HttpClient will handle the request entirely.
    if (type == ReceivedResponse.class) {
      return body -> null;
    }
    return null;
  }
}
