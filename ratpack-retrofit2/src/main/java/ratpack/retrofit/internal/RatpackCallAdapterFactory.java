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
import ratpack.http.client.ReceivedResponse;
import ratpack.retrofit.RatpackRetrofitCallException;
import ratpack.util.Exceptions;
import retrofit2.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RatpackCallAdapterFactory extends CallAdapter.Factory {

  public static final RatpackCallAdapterFactory INSTANCE = new RatpackCallAdapterFactory();

  private RatpackCallAdapterFactory() {

  }

  @Override
  public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
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
  protected CallAdapter<Promise<?>> getCallAdapter(ParameterizedType returnType) {
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
      return new ReceivedResponseCallAdapter(parameterType);
    }
    //Else we're just promising a value
    return new SimpleCallAdapter(parameterType);
  }

  static final class ReceivedResponseCallAdapter implements CallAdapter<Promise<?>> {

    private final Type responseType;

    ReceivedResponseCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public <R> Promise<ReceivedResponse> adapt(Call<R> call) {
      return new RatpackCallFactory.RatpackCall(call.request()).promise();
    }
  }

  static final class ResponseCallAdapter implements CallAdapter<Promise<?>> {
    private final Type responseType;

    ResponseCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public <R> Promise<Response<?>> adapt(Call<R> call) {
      return Promise.async(downstream ->
        call.enqueue(new Callback<R>() {

          @Override
          public void onResponse(Call<R> call, Response<R> response) {
            downstream.success(response);
          }

          @Override
          public void onFailure(Call<R> call, Throwable t) {
            downstream.error(t);
          }
        })
      );
    }
  }

  static final class SimpleCallAdapter implements CallAdapter<Promise<?>> {
    private final Type responseType;

    SimpleCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public <R> Promise<?> adapt(Call<R> call) {
      return Promise.async(downstream ->
        call.enqueue(new Callback<R>() {

          @Override
          public void onResponse(Call<R> call, Response<R> response) {
            if (response.isSuccessful()) {
              downstream.success(response.body());
            } else {
              Exceptions.uncheck(() ->
                downstream.error(RatpackRetrofitCallException.cause(call, response))
              );
            }
          }

          @Override
          public void onFailure(Call<R> call, Throwable t) {
            downstream.error(t);
          }
        })
      );
    }
  }
}
