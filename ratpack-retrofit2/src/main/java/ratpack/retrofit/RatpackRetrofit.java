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

package ratpack.retrofit;

import com.google.common.base.Preconditions;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.retrofit.internal.RatpackCallAdapterFactory;
import ratpack.retrofit.internal.RatpackCallFactory;
import retrofit2.Retrofit;

import java.net.URI;

public class RatpackRetrofit {

  static Builder builder(HttpClient httpClient) {
    return new Builder(httpClient);
  }

  static class Builder {

    private HttpClient httpClient;
    private Action<? super Retrofit.Builder> builderAction = Action.noop();
    private URI uri;

    public Builder(HttpClient httpClient) {
      Preconditions.checkNotNull(httpClient);
      this.httpClient = httpClient;
    }


    Builder configure(Action<? super Retrofit.Builder> builderAction) {
      this.builderAction = builderAction;
      return this;
    }

    Builder uri(URI uri) {
      this.uri = uri;
      return this;
    }

    Retrofit build() throws Exception {
      Retrofit.Builder builder = new Retrofit.Builder()
        .callFactory(new RatpackCallFactory(httpClient))
        .addCallAdapterFactory(RatpackCallAdapterFactory.INSTANCE);
      builder.baseUrl(uri.toString());
      builderAction.execute(builder);
      return builder.build();
    }
  }

}
