/*
 * Copyright 2020 the original author or authors.
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

package ratpack.micrometer.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.http.client.HttpClientSpec;
import ratpack.http.client.HttpResponse;
import ratpack.http.client.RequestSpec;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.BiFunction;

public class HttpClientTimingAction implements Action<HttpClientSpec> {
  @Inject
  private MeterRegistry registry;

  @Inject
  private MicrometerMetricsConfig config;

  private BiFunction<RequestSpec, HttpResponse, Tags> clientRequestTags;

  public HttpClientTimingAction() {
    // for post-constructor injection
  }

  public HttpClientTimingAction(MeterRegistry registry, BiFunction<RequestSpec, HttpResponse, Tags> clientRequestTags) {
    this.registry = registry;
    this.clientRequestTags = clientRequestTags;
  }

  @Override
  public void execute(HttpClientSpec httpClientSpec) {
    httpClientSpec.requestIntercept(requestSpec ->
      Execution.currentOpt()
        .ifPresent(execution -> {
          execution.add(requestSpec);
          execution.add(Timer.start(registry));
        })
    );

    httpClientSpec.responseIntercept(responseSpec ->
      Execution.currentOpt()
        .ifPresent(execution -> {
          Optional<Timer.Sample> sample = execution.maybeGet(Timer.Sample.class);
          Optional<RequestSpec> requestSpec = execution.maybeGet(RequestSpec.class);
          BiFunction<RequestSpec, HttpResponse, Tags> tagsFunction = getTagsFunction();
          if (tagsFunction != null && sample.isPresent() && requestSpec.isPresent()) {
            sample.get().stop(registry.timer("http.client.requests",
              tagsFunction.apply(requestSpec.get(), responseSpec)));
          }
        })
    );
  }

  private BiFunction<RequestSpec, HttpResponse, Tags> getTagsFunction() {
    return clientRequestTags == null ? (config == null ? null : config.getClientRequestTags()) : clientRequestTags;
  }
}
