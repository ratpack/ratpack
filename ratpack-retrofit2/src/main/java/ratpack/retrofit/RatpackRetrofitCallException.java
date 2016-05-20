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

package ratpack.retrofit;

import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

/**
 * Exception throw from Retrofit clients when using simple types instead of {@link Response} and the request is not successful.
 */
public class RatpackRetrofitCallException extends Exception {

  public RatpackRetrofitCallException(Call<?> call, String error) {
    super(call.request().url().toString() + ": " + error);
  }

  public static RatpackRetrofitCallException cause(Call<?> call, Response<?> response) throws IOException {
    return new RatpackRetrofitCallException(call, response.errorBody().string());
  }
}
