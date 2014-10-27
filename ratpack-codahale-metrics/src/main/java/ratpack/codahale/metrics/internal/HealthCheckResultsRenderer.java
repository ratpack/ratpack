/*
 * Copyright 2014 the original author or authors.
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

package ratpack.codahale.metrics.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ratpack.codahale.metrics.HealthCheckResults;
import ratpack.func.Action;
import ratpack.handling.ByContentSpec;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.render.RendererSupport;

public class HealthCheckResultsRenderer extends RendererSupport<HealthCheckResults> {

  @Override
  public void render(Context context, HealthCheckResults object) throws Exception {
    context.byContent((new Action<ByContentSpec>() {
      @Override
      public void execute(ByContentSpec byContentSpec) throws Exception {
        byContentSpec.json(new Handler() {
          @Override
          public void handle(Context context) throws Exception {
            ObjectMapper mapper = new ObjectMapper();

            byte[] bytes;
            try {
              bytes = mapper.writeValueAsBytes(object);
            } catch (JsonProcessingException e) {
              context.error(e);
              return;
            }

            context.getResponse().send(bytes);
          }
        });
      }
    }));
  }

}
