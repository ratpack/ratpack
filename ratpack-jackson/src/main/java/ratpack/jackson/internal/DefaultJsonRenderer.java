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

package ratpack.jackson.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import ratpack.func.Action;
import ratpack.handling.ByContentSpec;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.jackson.JsonRender;
import ratpack.jackson.JsonRenderer;
import ratpack.render.RendererSupport;

import javax.inject.Inject;

public class DefaultJsonRenderer extends RendererSupport<JsonRender> implements JsonRenderer {

  private final ObjectWriter defaultObjectWriter;

  @Inject
  public DefaultJsonRenderer(ObjectWriter defaultObjectWriter) {
    this.defaultObjectWriter = defaultObjectWriter;
  }

  @Override
  public void render(final Context context, final JsonRender object) throws Exception {
    context.byContent(new Action<ByContentSpec>() {
      @Override
      public void execute(ByContentSpec byContentSpec) throws Exception {
        byContentSpec.json(new Handler() {
          @Override
          public void handle(Context context) {
            ObjectWriter writer = object.getObjectWriter();
            if (writer == null) {
              writer = defaultObjectWriter;
            }

            byte[] bytes;
            try {
              bytes = writer.writeValueAsBytes(object.getObject());
            } catch (JsonProcessingException e) {
              context.error(e);
              return;
            }

            context.getResponse().send(bytes);
          }
        });
      }
    });
  }

}
