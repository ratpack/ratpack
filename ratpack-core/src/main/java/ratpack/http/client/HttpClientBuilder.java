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
package ratpack.http.client;

/**
 * Interface for {@link HttpClient} builders.
 */
public interface HttpClientBuilder {

  /**
   * Configure a {@link HttpClientRequestInterceptor} for the {@link HttpClient}.
   *
   * @param requestInterceptor the request interceptor
   *
   * @return the builder.
   */
  HttpClientBuilder requestInterceptor(HttpClientRequestInterceptor requestInterceptor);

  /**
   * Configure a {@link HttpClientResponseInterceptor} for the {@link HttpClient}.
   *
   * @param responseInterceptor the response interceptor
   *
   * @return the builder.
   */
  HttpClientBuilder responseInterceptor(HttpClientResponseInterceptor responseInterceptor);

  /**
   * Configure a {@link RequestSpecConfigurer} for the {@link HttpClient}.
   *
   * @param requestSpecConfigurer the request configurer
   *
   * @return the builder.
   */
  HttpClientBuilder requestSpecConfigurer(RequestSpecConfigurer requestSpecConfigurer);

  /**
   * Build the {@link HttpClient} instance.
   *
   * @return the http client.
   */
  HttpClient build();
}
