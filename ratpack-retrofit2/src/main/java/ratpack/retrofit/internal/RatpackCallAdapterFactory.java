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

package ratpack.retrofit.internal;

import com.google.common.reflect.TypeToken;
import ratpack.exec.Promise;
import ratpack.func.Factory;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.retrofit.RatpackRetrofitCallException;
import ratpack.util.Exceptions;
import retrofit2.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RatpackCallAdapterFactory extends CallAdapter.Factory {

  private final Factory<? extends HttpClient> factory;

  private RatpackCallAdapterFactory(Factory<? extends HttpClient> factory) {
    this.factory = factory;
  }

  public static RatpackCallAdapterFactory with(Factory<? extends HttpClient> factory) {
    return new RatpackCallAdapterFactory(factory);
  }

  @Override
  public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    TypeToken<?> rawType = TypeToken.of(returnType);
    if (rawType.getRawType() != Promise.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException("Promise return type must be parameterized"
        + " as Promise<Foo> or Promise<? extends Foo>");
    }
    return getCallAdapter((ParameterizedType) returnType);
  }

  // returnType is the parameterization of Promise
  protected CallAdapter<?, Promise<?>> getCallAdapter(ParameterizedType returnType) {
    Type parameterType = Utils.getSingleParameterUpperBound(returnType);
    TypeToken<?> parameterTypeToken = TypeToken.of(parameterType);
    //Promising a Response type, need the actual value
    if (parameterTypeToken.getRawType() == Response.class) {
      if (!(parameterType instanceof ParameterizedType)) {
        throw new IllegalStateException("Response return type must be parameterized"
          + " as Response<Foo> or Response<? extends Foo>");
      }
      Type responseType = Utils.getSingleParameterUpperBound((ParameterizedType) parameterType);
      return new ResponseCallAdapter(responseType);
    } else if (parameterTypeToken.getRawType() == ReceivedResponse.class) {
      return new ReceivedResponseCallAdapter(parameterType, factory);
    }
    //Else we're just promising a value
    return new SimpleCallAdapter(parameterType);
  }

  static final class ReceivedResponseCallAdapter implements CallAdapter<Object, Promise<?>> {

    private final Type responseType;
    private final ratpack.func.Factory<? extends HttpClient> factory;

    ReceivedResponseCallAdapter(Type responseType, ratpack.func.Factory<? extends HttpClient> factory) {
      this.responseType = responseType;
      this.factory = factory;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public Promise<ReceivedResponse> adapt(Call<Object> call) {
      return ((RatpackCallFactory.RatpackCall) RatpackCallFactory.with(factory).newCall(call.request())).promise();
    }
  }

  static final class ResponseCallAdapter implements CallAdapter<Object, Promise<?>> {
    private final Type responseType;

    ResponseCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public Promise<Response<?>> adapt(Call<Object> call) {
      return Promise.async(downstream ->
        call.enqueue(new Callback<Object>() {

          @Override
          public void onResponse(Call<Object> call, Response<Object> response) {
            downstream.success(response);
          }

          @Override
          public void onFailure(Call<Object> call, Throwable t) {
            downstream.error(t);
          }
        })
      );
    }
  }

  static final class SimpleCallAdapter implements CallAdapter<Object, Promise<?>> {
    private final Type responseType;

    SimpleCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public Promise<?> adapt(Call<Object> call) {
      return Promise.async(downstream ->
        call.enqueue(new Callback<Object>() {

          @Override
          public void onResponse(Call<Object> call, Response<Object> response) {
            if (response.isSuccessful()) {
              downstream.success(response.body());
            } else {
              Exceptions.uncheck(() ->
                downstream.error(RatpackRetrofitCallException.cause(call, response))
              );
            }
          }

          @Override
          public void onFailure(Call<Object> call, Throwable t) {
            downstream.error(t);
          }
        })
      );
    }
  }
}
